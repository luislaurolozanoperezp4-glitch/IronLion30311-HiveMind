// Sources: new for IronLion30311-HiveMind (gamepad thresholds used by util.GamepadEx)
//          IronLion30311BIOBUZZInicioDeTemporada / branch feature/turrubiates/vision-align / TeamCode/.../opmodes/teleop/MainTeleOp.java @ a7b8f4c (brake, jam and battery rumbles)
//          — adapted for BIOBUZZ
package org.firstinspires.ftc.teamcode.config;

import com.acmerobotics.dashboard.config.Config;

/** Gamepad feel and TeleOp options: thresholds, rumbles, telemetry and match log. */
@Config
public final class ControlConstants {

    private ControlConstants() {
    }

    /** A trigger pulled past this (0..1) counts as a pressed button. TODO: TUNE */
    public static double TRIGGER_PRESS_THRESHOLD = 0.5;
    /** Length of a short rumble, in milliseconds. TODO: TUNE */
    public static int SHORT_RUMBLE_MILLIS = 200;
    /** Length of a long rumble (for example the endgame warning), in milliseconds. TODO: TUNE */
    public static int LONG_RUMBLE_MILLIS = 600;

    // ---------------- TeleOp rumbles (values from the tested MainTeleOp) ----------------

    /** Rumble while the driver moves the sticks with the brake on, in milliseconds. */
    public static int BRAKE_RUMBLE_MILLIS = 150;
    /** Strength (0..1) of the brake warning rumble. */
    public static double BRAKE_RUMBLE_POWER = 0.3;
    /** gamepad2 rumble while the indexer is stopped on a jam, in milliseconds. */
    public static int JAM_RUMBLE_MILLIS = 300;
    /** Rumble on both gamepads when the battery turns low, in milliseconds. */
    public static int LOW_BATTERY_RUMBLE_MILLIS = 1000;
    /** Blips at the first endgame warning ({@link MatchConstants#FIRST_ENDGAME_WARNING_SECONDS}). TODO: TUNE */
    public static int FIRST_ENDGAME_WARNING_BLIPS = 3;

    // ---------------- TeleOp options ----------------

    /** Start TeleOp with the short COMPETITION telemetry (gamepad2 BACK switches to DEBUG). */
    public static boolean START_IN_COMPETITION_TELEMETRY = true;
    /** Start TeleOp field-centric (false = robot-centric, as tested; gamepad1 BACK toggles). */
    public static boolean START_FIELD_CENTRIC = false;
    /** Read the Pinpoint in TeleOp (pose on telemetry, field-centric heading). */
    public static boolean USE_ODOMETRY_IN_TELEOP = true;
    /** Write the TeleOp match CSV log under /sdcard/FIRST/match_logs. */
    public static boolean MATCH_LOG_ENABLED = true;
    /** Time between two match log rows, in seconds. TODO: TUNE */
    public static double MATCH_LOG_INTERVAL_SECONDS = 0.1;
    /** Time between two match log flushes to the file, in seconds. TODO: TUNE */
    public static double MATCH_LOG_FLUSH_SECONDS = 1.0;
    /** Longest auto-shoot volley (gamepad2 A), spin-up included, in seconds. TODO: TUNE */
    public static double AUTO_SHOOT_MAX_SECONDS = 8.0;
}
