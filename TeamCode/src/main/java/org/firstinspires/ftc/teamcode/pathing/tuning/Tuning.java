// Sources: IronLion30311BIOBUZZInicioDeTemporada / branch feature/turrubiates/vision-align / TeamCode/.../subsystems/pathing/tuning/Tuning.java @ a7b8f4c (mecanumTuner, pinpointTuner, foresightTuner, tests) — adapted for BIOBUZZ
package org.firstinspires.ftc.teamcode.pathing.tuning;

import com.pedropathing.algorithm.Foresight;
import com.pedropathing.revhub.drivetrains.Mecanum;
import com.pedropathing.revhub.localizers.PinpointLocalizer;
import com.pedropathing.tuning.autotune.Procedure;
import com.pedropathing.tuning.autotune.Tuner;

import org.firstinspires.ftc.teamcode.pathing.PedroConstants;

/**
 * Pedro Pathing AutoTune procedures for this robot.
 * <p>
 * With the Robot Controller running, open http://192.168.43.1:10158 in a browser connected to
 * the robot Wi-Fi and run the procedures in this order. After each one, copy the Java code it
 * prints into the matching config in {@link PedroConstants}, rebuild and install:
 * <ol>
 *     <li><b>Mecanum Procedure</b>: each drive motor's direction ({@code DRIVETRAIN_CONFIG}).</li>
 *     <li><b>Pinpoint Procedure</b>: pod directions and offsets ({@code LOCALIZER_CONFIG}).
 *     Push the robot forward, then left, then spin it 180 degrees counter-clockwise.</li>
 *     <li><b>Tests &gt; Odometry / Localization</b>: forward must increase x, left must increase y.</li>
 *     <li><b>Foresight Procedure</b>: velocities, braking and gains ({@code FOLLOWER_CONFIG}).
 *     The robot drives by itself: leave 2-3 tiles free in front and to its left.</li>
 *     <li><b>Tests &gt; Line / Curved / Hold</b>: checks the tuned follower. Then remove the
 *     {@code MAX_PATH_SPEED} cap.</li>
 * </ol>
 * The procedures use the current configs, so re-running Tests after pasting new values checks
 * exactly what the autonomous uses. See https://pedropathing.com/docs/pathing/tuning.
 * <p>
 * The procedure classes in this package are vendored unchanged from the Pedro Pathing Quickstart
 * (BSD-3-Clause-Clear): update them from there, do not edit them here.
 */
public final class Tuning {

    private Tuning() {
    }

    /** @return the Mecanum motor-direction procedure */
    @Tuner
    public static Procedure mecanumTuner() {
        return new MecanumTuner();
    }

    /** @return the Pinpoint pods procedure */
    @Tuner
    public static Procedure pinpointTuner() {
        return new PinpointTuner();
    }

    /** @return the Foresight follower procedure, using the current localizer and drivetrain */
    @Tuner
    public static Procedure foresightTuner() {
        return new ForesightTuner(
                hardwareMap -> new PinpointLocalizer(hardwareMap, PedroConstants.LOCALIZER_CONFIG),
                hardwareMap -> new Mecanum(hardwareMap, PedroConstants.DRIVETRAIN_CONFIG));
    }

    /** @return the test procedures (localization, line, curve, hold) with the current configs */
    @Tuner
    public static Procedure tests() {
        return new Tests(
                hardwareMap -> new Mecanum(hardwareMap, PedroConstants.DRIVETRAIN_CONFIG),
                hardwareMap -> new PinpointLocalizer(hardwareMap, PedroConstants.LOCALIZER_CONFIG),
                () -> new Foresight(PedroConstants.FOLLOWER_CONFIG));
    }
}
