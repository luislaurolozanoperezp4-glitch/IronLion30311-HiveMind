// Sources: IronLion30311BIOBUZZInicioDeTemporada / branch feature/turrubiates/vision-align / TeamCode/.../subsystems/vision/HiveVision.java, HiveGeometry.java, HiveAlignController.java @ a7b8f4c
//          IronLion30311BIOBUZZInicioDeTemporada / branch feature/turrubiates/vision-align / TeamCode/.../subsystems/vision/BallVision.java @ a7b8f4c (HuskyLens IDs)
//          IronLion30311BIOBUZZInicioDeTemporada / branch feature/mar/duopodriveNcam / TeamCode/... @ 2d92fba (alternative tag spacing)
//          — adapted for BIOBUZZ
package org.firstinspires.ftc.teamcode.config;

import com.acmerobotics.dashboard.config.Config;

/** Camera values: Limelight 3A (HIVE AprilTags, rear) and HuskyLens (ball color, front). */
@Config
public final class VisionConstants {

    private VisionConstants() {
    }

    // ---------------- Limelight 3A ----------------
    public static int LIMELIGHT_PIPELINE = 0;
    public static int LIMELIGHT_POLL_RATE_HZ = 100;
    /** Smoothing for tag readings (0 = frozen, 1 = no smoothing). */
    public static double TAG_SMOOTHING = 0.45;
    public static long TAG_STALE_MILLIS = 200;
    public static double TAG_DROPOUT_HOLD_SECONDS = 0.15;
    /** Camera tilt up from horizontal. TODO: TUNE (decision D17). */
    public static double CAMERA_PITCH_DEGREES = 40.0;
    /** Distance between HIVE tags. Alternative 0.0825 m. Decision D17. */
    public static double TAG_SPACING_METERS = 0.1143;
    /** HIVE AprilTag IDs (TESTED in HiveStateProbe). Cell mapping: decision D20. */
    public static final int FIRST_HIVE_TAG_ID = 30;
    public static final int LAST_HIVE_TAG_ID = 45;

    // ---------------- HIVE auto-align (disabled: it overshot, commit 21900d5) ----------------
    public static boolean HIVE_ALIGN_ENABLED = false;
    /** Setpoint TESTED 2026-11-20. Gains below: TODO: TUNE. */
    public static double ALIGN_TARGET_FORWARD_METERS = 1.03;
    public static double ALIGN_TARGET_STRAFE_METERS = 0.08;
    public static double ALIGN_FORWARD_TOLERANCE_METERS = 0.03;
    public static double ALIGN_STRAFE_TOLERANCE_METERS = 0.025;
    public static double ALIGN_YAW_TOLERANCE_DEGREES = 2.5;
    public static double ALIGN_FORWARD_KP = 1.45;
    public static double ALIGN_FORWARD_KD = 0.14;
    public static double ALIGN_FORWARD_MAX = 0.60;
    public static double ALIGN_STRAFE_KP = 1.75;
    public static double ALIGN_STRAFE_KD = 0.16;
    public static double ALIGN_STRAFE_MAX = 0.70;
    public static double ALIGN_YAW_KP = 0.022;
    public static double ALIGN_YAW_KD = 0.0025;
    public static double ALIGN_YAW_MAX = 0.45;
    public static double ALIGN_TRANSLATION_FEEDFORWARD = 0.06;
    public static double ALIGN_YAW_FEEDFORWARD = 0.05;

    // ---------------- HuskyLens (color recognition) ----------------
    /** Learned color IDs. Retrain the HuskyLens to match these (decision D11). */
    public static int HUSKY_POLLEN_ID = 1;
    public static int HUSKY_RED_NECTAR_ID = 2;
    public static int HUSKY_BLUE_NECTAR_ID = 3;
    /** Ignore color blocks smaller than this (far away or noise). TODO: TUNE */
    public static int HUSKY_MIN_BLOCK_AREA_PIXELS = 150;
    public static double HUSKY_READ_INTERVAL_SECONDS = 0.05;
    /** Turn assist toward a seen ball in TeleOp. TODO: TUNE */
    public static double BALL_ASSIST_KP = 0.35;
    public static double BALL_ASSIST_MAX_TURN = 0.3;
    /** HuskyLens image size in pixels (fixed by the camera). */
    public static final int HUSKY_IMAGE_WIDTH_PIXELS = 320;
    public static final int HUSKY_IMAGE_HEIGHT_PIXELS = 240;

    // ---------------- Ball counting (BallIntakeCounter on vision-align, in use) ----------------
    /** A ball this far down the image (-1 top .. +1 bottom) is about to enter the intake. */
    public static double BALL_NEAR_Y_OFFSET = 0.35;
    /** The near ball must stay out of view this long to count as picked up. */
    public static double BALL_LOST_CONFIRM_SECONDS = 0.15;
    /** If the nearest ball jumps up the image by more than this, the near one went in. */
    public static double BALL_NEXT_JUMP = 0.4;
    /** Two pickups closer than this are the same ball seen twice. */
    public static double BALL_MIN_PICKUP_INTERVAL_SECONDS = 0.25;
    /**
     * A NECTAR ball at least this far down the image counts as "at the intake" (G408 intake stop
     * and the NECTAR alert). TODO: TUNE
     */
    public static double INTAKE_ZONE_Y_OFFSET = 0.35;
}
