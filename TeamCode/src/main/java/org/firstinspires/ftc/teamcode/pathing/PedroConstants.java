// Sources: IronLion30311BIOBUZZInicioDeTemporada / branch feature/turrubiates/vision-align / TeamCode/.../subsystems/pathing/PedroFollowerFactory.java @ a7b8f4c (DRIVETRAIN_CONFIG, LOCALIZER_CONFIG, FOLLOWER_CONFIG, create) — adapted for BIOBUZZ
package org.firstinspires.ftc.teamcode.pathing;

import com.pedropathing.algorithm.Foresight;
import com.pedropathing.algorithm.ForesightConfig;
import com.pedropathing.controllers.Controller;
import com.pedropathing.follower.Follower;
import com.pedropathing.math.Matrix;
import com.pedropathing.math.Vector2D;
import com.pedropathing.revhub.drivetrains.Mecanum;
import com.pedropathing.revhub.drivetrains.MecanumConfig;
import com.pedropathing.revhub.localizers.PinpointConfig;
import com.pedropathing.revhub.localizers.PinpointLocalizer;
import com.qualcomm.hardware.gobilda.GoBildaPinpointDriver;
import com.qualcomm.robotcore.hardware.HardwareMap;

import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;
import org.firstinspires.ftc.teamcode.config.DriveConstants;
import org.firstinspires.ftc.teamcode.config.RobotConstants;

/**
 * Pedro Pathing 3 setup for the robot: mecanum drivetrain, goBILDA Pinpoint localizer (two
 * 4-bar pods + IMU) and the Foresight path-following algorithm, plus {@link #createFollower}.
 * <p>
 * The old {@code PedroFollowerFactory} is merged into this class (one place for every Pedro
 * value and the factory that uses them). The configs are public so the AutoTune procedures in
 * {@code pathing.tuning} test exactly what the autonomous uses.
 * <p>
 * Device names come from {@link RobotConstants}. Pedro has no fallback to the legacy names, so
 * the Control Hub configuration must use the current names (decision D2).
 * <p>
 * Status: drivetrain directions and brake mode are TESTED; the Pinpoint pod directions are
 * TESTED and the pod offsets FITTED (decision D4). The Foresight gains are placeholders that
 * were never tuned (decision D6): run AutoTune and paste its output into
 * {@link #FOLLOWER_CONFIG} before following paths at full speed.
 */
public final class PedroConstants {

    private PedroConstants() {
    }

    /**
     * X (forward) pod offset from the center of rotation, inches (goBILDA convention: left of
     * center is positive). FITTED from an in-place 92 degree turn on 2026-11-23 (residual 0.08 in).
     * TODO: TUNE (confirm with a tape measure, decision D4).
     */
    public static final double X_POD_OFFSET_INCHES = -6.79;

    /**
     * Y (strafe) pod offset from the center of rotation, inches (forward of center is positive).
     * FITTED, same run as {@link #X_POD_OFFSET_INCHES}. TODO: TUNE (decision D4).
     */
    public static final double Y_POD_OFFSET_INCHES = -0.72;

    /**
     * Safety cap on path speed (0 to 1) while the Foresight gains are untuned. Remove it (set it
     * to 1.0) only after the AutoTune Foresight procedure and the Line/Curve/Hold tests pass.
     */
    public static final double MAX_PATH_SPEED = 0.6;

    /** Mecanum drivetrain: same names and directions as RobotHardware. TESTED. */
    public static final MecanumConfig DRIVETRAIN_CONFIG = new MecanumConfig(c -> {
        c.frontLeftName.set(RobotConstants.FRONT_LEFT_DRIVE);
        c.frontRightName.set(RobotConstants.FRONT_RIGHT_DRIVE);
        c.backLeftName.set(RobotConstants.BACK_LEFT_DRIVE);
        c.backRightName.set(RobotConstants.BACK_RIGHT_DRIVE);
        c.frontLeftDirection.set(DriveConstants.FRONT_LEFT_DIRECTION);
        c.backLeftDirection.set(DriveConstants.BACK_LEFT_DIRECTION);
        c.frontRightDirection.set(DriveConstants.FRONT_RIGHT_DIRECTION);
        c.backRightDirection.set(DriveConstants.BACK_RIGHT_DIRECTION);
        // BRAKE when the odometry driver commands 0 through follower.manual(): with FLOAT the
        // robot rolled ~12 cm past the end of a 240 cm leg. TESTED.
        c.manualBrakeMode.set(DriveConstants.ZERO_POWER_BRAKE);
    });

    /** Pinpoint localizer, in inches to match the Pedro visualizer. */
    public static final PinpointConfig LOCALIZER_CONFIG = new PinpointConfig(c -> {
        c.name.set(RobotConstants.PINPOINT);
        c.podType.set(GoBildaPinpointDriver.GoBildaOdometryPods.goBILDA_4_BAR_POD);
        c.xPodDirection.set(GoBildaPinpointDriver.EncoderDirection.FORWARD);
        // REVERSED: with FORWARD the robot strafed right while the Pinpoint read left
        // (test 2026-11-23, checked against the wheel encoders). TESTED.
        c.yPodDirection.set(GoBildaPinpointDriver.EncoderDirection.REVERSED);
        c.xPodOffset.set(X_POD_OFFSET_INCHES);
        c.yPodOffset.set(Y_POD_OFFSET_INCHES);
        c.offsetUnits.set(DistanceUnit.INCH);
        c.globalDistanceUnit.set(DistanceUnit.INCH);
    });

    /**
     * Foresight follower gains. Every value is a conservative placeholder, identical on every
     * old branch and never tuned. TODO: TUNE (replace with the code printed by the Foresight
     * procedure, decision D6).
     */
    public static final ForesightConfig FOLLOWER_CONFIG = new ForesightConfig(c -> {
        // TODO: TUNE
        c.forwardTranslational.set(Controller.piecewise(Controller.proportional(0.03))
                .put(2.5, Controller.proportional(0.1)));
        // TODO: TUNE
        c.strafeTranslational.set(Controller.piecewise(Controller.proportional(0.03))
                .put(2.5, Controller.proportional(0.1)));

        c.coast.set(Controller.proportionalFeedforward(0.01)); // TODO: TUNE
        c.brake.set(Controller.proportionalFeedforward(0.01)); // TODO: TUNE

        c.headingFeedback.set(Controller.proportional(1.0)); // TODO: TUNE
        c.headingBrakeCoefficients.set(Vector2D.cartesian(0.1, 0.01)); // TODO: TUNE

        c.linearBrakeCoefficients.set(Matrix.diag(0.05, 0.05)); // TODO: TUNE
        c.quadraticBrakeCoefficients.set(Matrix.diag(0.001, 0.001)); // TODO: TUNE

        c.maxAchievableForwardVelocity.set(60.0); // TODO: TUNE (in/s)
        c.maxAchievableStrafeVelocity.set(45.0); // TODO: TUNE (in/s)
        c.naturalForwardDeceleration.set(40.0); // TODO: TUNE (in/s^2)
        c.naturalStrafeDeceleration.set(40.0); // TODO: TUNE (in/s^2)

        // Safety cap while untuned; see MAX_PATH_SPEED.
        c.maxPathSpeed.set(MAX_PATH_SPEED);
    });

    /**
     * Creates a new follower bound to this OpMode's hardware map. Call it once in INIT.
     * <p>
     * The follower owns the Pinpoint and the four drive motors while it runs: do not drive the
     * same motors from a Drivetrain subsystem in the same OpMode.
     *
     * @param hardwareMap the hardware map provided by the OpMode
     * @return a ready-to-use Pedro Pathing follower (pose starts at (0, 0, 0): call setPose)
     */
    public static Follower createFollower(HardwareMap hardwareMap) {
        return new Follower(
                new PinpointLocalizer(hardwareMap, LOCALIZER_CONFIG),
                new Mecanum(hardwareMap, DRIVETRAIN_CONFIG),
                new Foresight(FOLLOWER_CONFIG));
    }
}
