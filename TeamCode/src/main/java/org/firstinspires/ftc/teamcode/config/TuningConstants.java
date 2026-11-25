// Sources: IronLion30311BIOBUZZInicioDeTemporada / branch feature/turrubiates/vision-align / TeamCode/.../opmodes/teleop/DriveWheelTest.java @ a7b8f4c (TEST_POWER 0.3)
//          IronLion30311BIOBUZZInicioDeTemporada / branch feature/turrubiates/vision-align / TeamCode/.../opmodes/calibration/HiveStateProbe.java @ a7b8f4c (DRIVE_SCALE 0.3, NO_TAG_INTERVAL_SEC 0.1)
//          — adapted for BIOBUZZ
package org.firstinspires.ftc.teamcode.config;

import com.acmerobotics.dashboard.config.Config;

/** Values used only by the OpModes in {@code opmodes/tuning} (group "Tuning"). */
@Config
public final class TuningConstants {

    private TuningConstants() {
    }

    // ---------------- Tune Launcher PIDF ----------------

    /** Big target change on D-pad right/left, in RPM. TODO: TUNE */
    public static double LAUNCHER_BIG_RPM_STEP = 500.0;
    /** Change of KP per bumper tap. TODO: TUNE */
    public static double LAUNCHER_KP_STEP = 1.0;
    /** Change of KI per bumper tap. TODO: TUNE */
    public static double LAUNCHER_KI_STEP = 0.1;
    /** Change of KD per bumper tap. TODO: TUNE */
    public static double LAUNCHER_KD_STEP = 0.5;
    /** Change of KF (at 12 V) per bumper tap. TODO: TUNE */
    public static double LAUNCHER_KF_STEP = 0.5;
    /** Time between two launcher CSV rows, in seconds. TODO: TUNE */
    public static double LAUNCHER_LOG_INTERVAL_SECONDS = 0.02;

    // ---------------- Drive and wheel tests ----------------

    /** Power of one wheel in Test Drive Wheels (low so the robot stays on the floor). */
    public static double WHEEL_TEST_POWER = 0.3;
    /** Encoder speed above which a wheel counts as turning, in ticks per second. TODO: TUNE */
    public static double WHEEL_MOVING_TICKS_PER_SECOND = 20.0;
    /** Drive power scale while recording in Tune Hive Vision. */
    public static double VISION_DRIVE_SCALE = 0.3;
    /** Interval of the "no HIVE tag visible" rows in Tune Hive Vision, in seconds. */
    public static double VISION_NO_TAG_LOG_INTERVAL_SECONDS = 0.1;

    // ---------------- Indexer / intake test ----------------

    /** Time between two current CSV rows in Test Indexer Intake, in seconds. TODO: TUNE */
    public static double CURRENT_LOG_INTERVAL_SECONDS = 0.05;
}
