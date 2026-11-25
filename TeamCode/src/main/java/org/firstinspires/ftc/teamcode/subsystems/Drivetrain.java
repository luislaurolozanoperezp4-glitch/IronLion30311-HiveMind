// Sources: IronLion30311BIOBUZZInicioDeTemporada / branch feature/turrubiates/vision-align / TeamCode/.../subsystems/MecanumDrive.java @ a7b8f4c (drive, driveRamped, computeTargets, applyRamp, setPowerLimit, setBrake, slow/fast, getWheelPositions, resetEncoders)
//          IronLion30311BIOBUZZInicioDeTemporada / branch master / TeamCode/.../subsystems/MecanumDrive.java @ 37b7a16 (stop, getAverageForwardCm)
//          — adapted for BIOBUZZ (renamed MecanumDrive -> Drivetrain; ramp via util.SlewRateLimiter; optional field-centric)
package org.firstinspires.ftc.teamcode.subsystems;

import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;

import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.teamcode.config.DriveConstants;
import org.firstinspires.ftc.teamcode.hardware.RobotHardware;
import org.firstinspires.ftc.teamcode.util.MatchTimer;
import org.firstinspires.ftc.teamcode.util.SlewRateLimiter;

import java.util.function.DoubleSupplier;

/**
 * The mecanum drivetrain (four wheels).
 * <p>
 * Features, all from the tested robot:
 * <ul>
 *   <li><b>Ramp</b>: wheel power changes gradually so the robot never jerks. Slowing down uses a
 *   shorter ramp than speeding up, and below {@link DriveConstants#STOP_CUTOFF_POWER} the wheel
 *   is cut to zero so the BRAKE mode stops it.</li>
 *   <li><b>Brake</b> ({@link #setBrake(boolean)}): cuts power instantly and ignores input.</li>
 *   <li><b>Slow / fast mode</b>: scales every command.</li>
 *   <li><b>Battery sag limit</b>: with {@link #setBatteryMonitor(BatteryMonitor)}, power is capped
 *   while the battery sags, so the Control Hub does not brown out.</li>
 *   <li><b>Field-centric</b> (optional): with {@link #setHeadingSupplier(DoubleSupplier)},
 *   {@link #driveFieldCentric(double, double, double)} drives relative to the field.</li>
 * </ul>
 * Each loop: call {@link #drive(double, double, double)} (or another drive method), then
 * {@link #periodic()}. In AUTO, Pedro Pathing drives the motors; do not call the drive methods
 * there (periodic() does nothing until a drive method is called).
 * <p>
 * Directions: forward + = toward the intake, strafe + = right, turn + = clockwise.
 */
public class Drivetrain implements Subsystem {

    /** Wheel order used in every array. */
    public static final int FRONT_LEFT = 0;
    public static final int FRONT_RIGHT = 1;
    public static final int BACK_LEFT = 2;
    public static final int BACK_RIGHT = 3;
    private static final int WHEEL_COUNT = 4;

    private final DcMotorEx[] motors;
    private final SlewRateLimiter[] ramps = new SlewRateLimiter[WHEEL_COUNT];
    private final double[] targetPowers = new double[WHEEL_COUNT];
    private final double[] lastWrittenPowers = new double[WHEEL_COUNT];
    private final MatchTimer.Clock clock;

    private BatteryMonitor batteryMonitor;
    private DoubleSupplier headingSupplier;
    private double headingOffsetRad = 0.0;

    private double speedScale = DriveConstants.FAST_SPEED;
    private double powerLimit = 1.0;
    private boolean isBraking = false;
    private boolean isAlignRamp = false;
    private boolean isCommanded = false;
    private double lastTimeSec = Double.NaN;

    /**
     * @param hardware robot hardware; missing drive motors are skipped
     */
    public Drivetrain(RobotHardware hardware) {
        this(new DcMotorEx[]{hardware.frontLeftDrive, hardware.frontRightDrive,
                hardware.backLeftDrive, hardware.backRightDrive}, MatchTimer.systemClock());
    }

    /**
     * For tests and special setups.
     *
     * @param motors four motors in {@code [FL, FR, BL, BR]} order (entries may be null)
     * @param clock  time source
     */
    public Drivetrain(DcMotorEx[] motors, MatchTimer.Clock clock) {
        this.motors = motors;
        this.clock = clock;
        for (int i = 0; i < WHEEL_COUNT; i++) {
            ramps[i] = new SlewRateLimiter(DriveConstants.ACCEL_RAMP_SECONDS,
                    DriveConstants.DECEL_RAMP_SECONDS);
        }
    }

    /**
     * Drives relative to the robot, with the speed mode and the normal ramps.
     *
     * @param forward -1 to 1, + = forward
     * @param strafe  -1 to 1, + = right
     * @param turn    -1 to 1, + = clockwise
     */
    public void drive(double forward, double strafe, double turn) {
        isAlignRamp = false;
        setTargets(forward, strafe, turn, speedScale);
    }

    /**
     * Drives for closed-loop controllers (HIVE alignment): full speed scale, ignoring slow mode,
     * with the short {@link DriveConstants#ALIGN_RAMP_SECONDS} ramp so the controller stays quick.
     *
     * @param forward -1 to 1, + = forward
     * @param strafe  -1 to 1, + = right
     * @param turn    -1 to 1, + = clockwise
     */
    public void driveRamped(double forward, double strafe, double turn) {
        isAlignRamp = true;
        setTargets(forward, strafe, turn, DriveConstants.FAST_SPEED);
    }

    /**
     * Drives relative to the field (pushing the stick "away from the driver" always moves the
     * robot the same way on the field). Needs {@link #setHeadingSupplier(DoubleSupplier)};
     * without it, this is the same as {@link #drive(double, double, double)}.
     *
     * @param forward -1 to 1, + = away from the driver
     * @param strafe  -1 to 1, + = to the driver's right
     * @param turn    -1 to 1, + = clockwise
     */
    public void driveFieldCentric(double forward, double strafe, double turn) {
        if (headingSupplier == null) {
            drive(forward, strafe, turn);
            return;
        }
        double heading = headingSupplier.getAsDouble() - headingOffsetRad;
        double[] robot = fieldToRobot(forward, strafe, heading);
        drive(robot[0], robot[1], turn);
    }

    /**
     * @param supplier robot heading in radians, counter-clockwise positive (for example from
     *                 the Pedro follower or the Pinpoint), or null to turn field-centric off
     */
    public void setHeadingSupplier(DoubleSupplier supplier) {
        headingSupplier = supplier;
    }

    /** Makes the robot's current heading the "away from the driver" direction. */
    public void resetFieldHeading() {
        if (headingSupplier != null) {
            headingOffsetRad = headingSupplier.getAsDouble();
        }
    }

    /** @param monitor battery monitor whose sag limit caps the drive power (or null) */
    public void setBatteryMonitor(BatteryMonitor monitor) {
        batteryMonitor = monitor;
    }

    /**
     * Caps every drive command on top of the speed mode. The ramp still applies.
     *
     * @param limit maximum power scale, 0 to 1 (1 = no limit)
     */
    public void setPowerLimit(double limit) {
        powerLimit = Math.max(0.0, Math.min(1.0, limit));
    }

    /**
     * Turns the brake on or off. While on, power is cut instantly (no ramp) and drive input is
     * ignored. When released, the wheels ramp up from zero.
     *
     * @param enabled true to stop the wheels
     */
    public void setBrake(boolean enabled) {
        if (enabled == isBraking) {
            return;
        }
        isBraking = enabled;
        if (enabled) {
            cutPower();
        }
    }

    /** Limits the drivetrain to {@link DriveConstants#SLOW_SPEED}. */
    public void setSlowMode() {
        speedScale = DriveConstants.SLOW_SPEED;
    }

    /** Allows {@link DriveConstants#FAST_SPEED}. */
    public void setFastMode() {
        speedScale = DriveConstants.FAST_SPEED;
    }

    /** @return true while the brake is on */
    public boolean isBraking() {
        return isBraking;
    }

    /** @return true in slow mode */
    public boolean isSlowMode() {
        return speedScale == DriveConstants.SLOW_SPEED;
    }

    /** @return true if all four motors are configured */
    public boolean isConnected() {
        for (DcMotorEx motor : motors) {
            if (motor == null) {
                return false;
            }
        }
        return true;
    }

    /** Moves the wheel powers one ramp step toward the targets. Call once per loop. */
    @Override
    public void periodic() {
        double nowSec = clock.nowSeconds();
        double dtSec = Double.isNaN(lastTimeSec) ? 0.0
                : Math.min(nowSec - lastTimeSec, DriveConstants.RAMP_MAX_DT_SECONDS);
        lastTimeSec = nowSec;
        if (!isCommanded || isBraking) {
            return;
        }
        double accel = isAlignRamp ? DriveConstants.ALIGN_RAMP_SECONDS : DriveConstants.ACCEL_RAMP_SECONDS;
        double decel = isAlignRamp ? DriveConstants.ALIGN_RAMP_SECONDS : DriveConstants.DECEL_RAMP_SECONDS;
        for (int i = 0; i < WHEEL_COUNT; i++) {
            ramps[i].setRampSeconds(accel, decel);
            double power = ramps[i].calculate(targetPowers[i], dtSec);
            if (targetPowers[i] == 0.0 && Math.abs(power) <= DriveConstants.STOP_CUTOFF_POWER) {
                ramps[i].reset(0.0);
                power = 0.0;
            }
            writePower(i, power);
        }
    }

    /** Stops all four wheels immediately, skipping the ramp. */
    @Override
    public void stop() {
        isCommanded = false;
        cutPower();
    }

    /** @return wheel encoder ticks in {@code [FL, FR, BL, BR]} order (0 for a missing motor) */
    public int[] getWheelPositions() {
        int[] positions = new int[WHEEL_COUNT];
        for (int i = 0; i < WHEEL_COUNT; i++) {
            positions[i] = motors[i] == null ? 0 : motors[i].getCurrentPosition();
        }
        return positions;
    }

    /** @return average forward travel of the four wheels since the last reset, in cm */
    public double getAverageForwardCm() {
        int[] ticks = getWheelPositions();
        double average = (ticks[FRONT_LEFT] + ticks[FRONT_RIGHT] + ticks[BACK_LEFT]
                + ticks[BACK_RIGHT]) / (double) WHEEL_COUNT;
        return average / DriveConstants.ENCODER_TICKS_PER_CM_FORWARD;
    }

    /** Resets all wheel encoders to zero and goes back to open-loop power. */
    public void resetEncoders() {
        for (DcMotorEx motor : motors) {
            if (motor != null) {
                motor.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
                motor.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
            }
        }
    }

    /** @return the power last written to each wheel, {@code [FL, FR, BL, BR]} */
    public double[] getWheelPowers() {
        return lastWrittenPowers.clone();
    }

    @Override
    public void addTelemetry(Telemetry t) {
        t.addData("Drive", "%s%s%s", isSlowMode() ? "SLOW" : "FAST",
                isBraking ? " | BRAKE" : "", getEffectiveLimit() < 1.0 ? " | SAG LIMIT" : "");
        if (!isConnected()) {
            t.addLine("Drive: motor missing");
        }
    }

    /**
     * Standard mecanum mixing, normalized so no wheel goes above 1, then scaled.
     *
     * @param forward + = forward
     * @param strafe  + = right
     * @param turn    + = clockwise
     * @param scale   largest power (speed mode times limits)
     * @return wheel powers in {@code [FL, FR, BL, BR]} order
     */
    public static double[] mix(double forward, double strafe, double turn, double scale) {
        double denominator = Math.max(Math.abs(forward) + Math.abs(strafe) + Math.abs(turn), 1.0);
        double k = scale / denominator;
        return new double[]{
                (forward + strafe + turn) * k,
                (forward - strafe - turn) * k,
                (forward - strafe + turn) * k,
                (forward + strafe - turn) * k
        };
    }

    /**
     * Rotates a field-relative command into the robot frame.
     *
     * @param forward    + = away from the driver
     * @param strafe     + = driver's right
     * @param headingRad robot heading, counter-clockwise positive, 0 = facing away from the driver
     * @return {@code [robotForward, robotStrafe]}
     */
    public static double[] fieldToRobot(double forward, double strafe, double headingRad) {
        double cos = Math.cos(headingRad);
        double sin = Math.sin(headingRad);
        return new double[]{forward * cos - strafe * sin, forward * sin + strafe * cos};
    }

    private void setTargets(double forward, double strafe, double turn, double scale) {
        isCommanded = true;
        if (isBraking) {
            return;
        }
        double[] powers = mix(forward, strafe, turn, scale * getEffectiveLimit());
        System.arraycopy(powers, 0, targetPowers, 0, WHEEL_COUNT);
    }

    private double getEffectiveLimit() {
        double sagLimit = batteryMonitor == null ? 1.0 : batteryMonitor.getDrivePowerLimit();
        return Math.min(powerLimit, sagLimit);
    }

    private void cutPower() {
        for (int i = 0; i < WHEEL_COUNT; i++) {
            targetPowers[i] = 0.0;
            ramps[i].reset(0.0);
            writePower(i, 0.0);
        }
    }

    private void writePower(int wheel, double power) {
        if (power == lastWrittenPowers[wheel]) {
            return;
        }
        lastWrittenPowers[wheel] = power;
        if (motors[wheel] != null) {
            motors[wheel].setPower(power);
        }
    }
}
