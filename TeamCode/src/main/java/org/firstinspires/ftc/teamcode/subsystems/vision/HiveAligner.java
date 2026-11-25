// Sources: IronLion30311BIOBUZZInicioDeTemporada / branch feature/turrubiates/vision-align / TeamCode/.../subsystems/vision/HiveAlignController.java @ a7b8f4c (reset, update, alignTo, evaluate, computeAxisOutput, rear-facing signs)
//          — adapted for BIOBUZZ (renamed HiveAlignController -> HiveAligner; gains in VisionConstants; disabled by default)
package org.firstinspires.ftc.teamcode.subsystems.vision;

import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.teamcode.config.VisionConstants;
import org.firstinspires.ftc.teamcode.subsystems.Drivetrain;
import org.firstinspires.ftc.teamcode.subsystems.Subsystem;
import org.firstinspires.ftc.teamcode.util.MatchTimer;

/**
 * Drives the robot to the shooting spot in front of a HIVE cell using the Limelight
 * (forward, strafe and yaw, each with a PD controller plus a small push to beat friction).
 * <p>
 * <b>Disabled by default</b> ({@link VisionConstants#HIVE_ALIGN_ENABLED} = false): it overshot
 * on the field (commit 21900d5). While disabled, {@link #alignTo(HiveCell)} never moves the
 * robot and returns false, so the driver aims by hand. The setpoint (1.03 m / 0.08 m) is
 * TESTED; the gains are not.
 * <p>
 * The launchers and the Limelight face the rear, so camera errors are flipped into chassis
 * commands (the three {@code _SIGN} constants).
 * <p>
 * Call {@link #alignTo(HiveCell)} every loop while aligning (after {@code Vision.periodic()}),
 * and {@link #reset()} when the driver lets go.
 */
public class HiveAligner implements Subsystem {

    /** Camera faces the rear: a HIVE farther than the target means drive backward. */
    private static final double FORWARD_SIGN = -1.0;
    /** Camera faces the rear: a target to the right in the image is on the robot's left. */
    private static final double STRAFE_SIGN = -1.0;
    /** Camera faces the rear: a positive wall yaw means turn counter-clockwise. */
    private static final double YAW_SIGN = -1.0;
    /** Largest loop time used for the derivative. */
    private static final double MAX_DT_SECONDS = 0.1;
    /** Shorter loop times than this are treated as "no time passed". */
    private static final double MIN_DT_SECONDS = 1e-4;

    private final HiveVision vision;
    private final Drivetrain drivetrain;
    private final MatchTimer.Clock clock;

    private double previousForwardError = 0.0;
    private double previousStrafeError = 0.0;
    private double previousYawError = 0.0;
    private boolean isFirstIteration = true;
    private double lastTimeSec = Double.NaN;

    private double forwardCommand = 0.0;
    private double strafeCommand = 0.0;
    private double turnCommand = 0.0;

    private HiveCell lastAlignToCell = null;
    private boolean isAligned = false;
    private HiveClusterPose activeTarget = null;

    /**
     * @param vision     the HIVE vision ({@code vision.getHiveVision()})
     * @param drivetrain the drivetrain to move (TeleOp); may be null to only compute commands
     */
    public HiveAligner(HiveVision vision, Drivetrain drivetrain) {
        this.vision = vision;
        this.drivetrain = drivetrain;
        this.clock = MatchTimer.systemClock();
    }

    /** @return true if auto-align is switched on in {@link VisionConstants} */
    public boolean isEnabled() {
        return VisionConstants.HIVE_ALIGN_ENABLED;
    }

    /** Forgets the derivative history and the target. Call when the driver lets go. */
    public void reset() {
        previousForwardError = 0.0;
        previousStrafeError = 0.0;
        previousYawError = 0.0;
        isFirstIteration = true;
        forwardCommand = 0.0;
        strafeCommand = 0.0;
        turnCommand = 0.0;
        isAligned = false;
        activeTarget = null;
        lastAlignToCell = null;
    }

    /**
     * Drives one control step toward exactly this cell. {@code null} means "no valid target"
     * (for example our upward cell is unknown): the robot holds still instead of aiming at a
     * wrong cell. Does nothing while disabled.
     *
     * @param targetCell the cell to align with, or null
     * @return true if the target is visible and all three axes are within tolerance
     */
    public boolean alignTo(HiveCell targetCell) {
        if (!isEnabled() || targetCell == null) {
            reset();
            lastTimeSec = clock.nowSeconds();
            if (isEnabled()) {
                drive(0.0, 0.0, 0.0);
            }
            return false;
        }
        if (targetCell != lastAlignToCell) {
            // New cell (for example after a TIP): restart the derivative.
            isFirstIteration = true;
            lastAlignToCell = targetCell;
        }
        boolean isOnTarget = evaluate(targetCell);
        drive(forwardCommand, strafeCommand, turnCommand);
        return isOnTarget;
    }

    /**
     * Computes the commands for this cell WITHOUT moving the robot (use it to check the signs
     * on the robot before letting it drive). Works even while disabled.
     *
     * @param targetCell the cell, or null for the best visible cell
     * @return true if the target is visible and all three axes are within tolerance
     */
    public boolean evaluate(HiveCell targetCell) {
        double nowSec = clock.nowSeconds();
        double dtSec = Double.isNaN(lastTimeSec) ? 0.0 : Math.min(nowSec - lastTimeSec, MAX_DT_SECONDS);
        lastTimeSec = nowSec;

        activeTarget = vision.getPose(targetCell);
        if (activeTarget == null) {
            forwardCommand = 0.0;
            strafeCommand = 0.0;
            turnCommand = 0.0;
            isFirstIteration = true;
            isAligned = false;
            return false;
        }

        double forwardError = (activeTarget.forwardMeters - VisionConstants.ALIGN_TARGET_FORWARD_METERS) * FORWARD_SIGN;
        double strafeError = (activeTarget.strafeMeters - VisionConstants.ALIGN_TARGET_STRAFE_METERS) * STRAFE_SIGN;
        double yawError = activeTarget.yawDeg * YAW_SIGN;

        boolean isForwardAligned = Math.abs(forwardError) <= VisionConstants.ALIGN_FORWARD_TOLERANCE_METERS;
        boolean isStrafeAligned = Math.abs(strafeError) <= VisionConstants.ALIGN_STRAFE_TOLERANCE_METERS;
        boolean isYawAligned = Math.abs(yawError) <= VisionConstants.ALIGN_YAW_TOLERANCE_DEGREES;
        isAligned = isForwardAligned && isStrafeAligned && isYawAligned;

        if (isFirstIteration || dtSec <= MIN_DT_SECONDS) {
            previousForwardError = forwardError;
            previousStrafeError = strafeError;
            previousYawError = yawError;
            isFirstIteration = false;
        }

        forwardCommand = isForwardAligned ? 0.0 : computeAxisOutput(forwardError,
                previousForwardError, dtSec, VisionConstants.ALIGN_FORWARD_KP,
                VisionConstants.ALIGN_FORWARD_KD, VisionConstants.ALIGN_TRANSLATION_FEEDFORWARD,
                VisionConstants.ALIGN_FORWARD_MAX);
        strafeCommand = isStrafeAligned ? 0.0 : computeAxisOutput(strafeError,
                previousStrafeError, dtSec, VisionConstants.ALIGN_STRAFE_KP,
                VisionConstants.ALIGN_STRAFE_KD, VisionConstants.ALIGN_TRANSLATION_FEEDFORWARD,
                VisionConstants.ALIGN_STRAFE_MAX);
        turnCommand = isYawAligned ? 0.0 : computeAxisOutput(yawError, previousYawError, dtSec,
                VisionConstants.ALIGN_YAW_KP, VisionConstants.ALIGN_YAW_KD,
                VisionConstants.ALIGN_YAW_FEEDFORWARD, VisionConstants.ALIGN_YAW_MAX);

        previousForwardError = forwardError;
        previousStrafeError = strafeError;
        previousYawError = yawError;
        return isAligned;
    }

    /** @return true if the target is visible and all three axes are within tolerance */
    public boolean isAligned() {
        return isAligned;
    }

    /** @return the cluster pose being tracked, or null */
    public HiveClusterPose getActiveTarget() {
        return activeTarget;
    }

    /** @return latest forward command, -1 to 1 (+ = forward, toward the intake) */
    public double getForwardCommand() {
        return forwardCommand;
    }

    /** @return latest strafe command, -1 to 1 (+ = right) */
    public double getStrafeCommand() {
        return strafeCommand;
    }

    /** @return latest turn command, -1 to 1 (+ = clockwise) */
    public double getTurnCommand() {
        return turnCommand;
    }

    /** Nothing to do each loop: {@link #alignTo(HiveCell)} is called by the OpMode. */
    @Override
    public void periodic() {
    }

    /** Forgets the target (the drivetrain stops itself). */
    @Override
    public void stop() {
        reset();
    }

    @Override
    public void addTelemetry(Telemetry t) {
        t.addData("Align", "%s%s", isEnabled() ? "ON" : "OFF (disabled)",
                isAligned ? " ALIGNED" : "");
    }

    /**
     * PD + static-friction feedforward for one axis, limited to {@code maxPower}.
     *
     * @return the axis command
     */
    static double computeAxisOutput(double error, double previousError, double dtSec,
                                    double kp, double kd, double kStatic, double maxPower) {
        double derivative = dtSec > MIN_DT_SECONDS ? (error - previousError) / dtSec : 0.0;
        double output = kp * error + kd * derivative + Math.copySign(kStatic, error);
        return Math.max(-maxPower, Math.min(maxPower, output));
    }

    private void drive(double forward, double strafe, double turn) {
        if (drivetrain != null) {
            drivetrain.driveRamped(forward, strafe, turn);
        }
    }
}
