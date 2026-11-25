// Sources: IronLion30311BIOBUZZInicioDeTemporada / branch feature/turrubiates/vision-align / TeamCode/.../opmodes/auto/PreloadRouteAuto.java @ a7b8f4c (leg distances, time limits)
//          IronLion30311BIOBUZZInicioDeTemporada / branch feature/turrubiates/vision-align / TeamCode/.../opmodes/auto/PedroAutoBase.java @ a7b8f4c (PATH_TIMEOUT_SEC)
//          IronLion30311BIOBUZZInicioDeTemporada / branch feature/theautonomous / TeamCode/.../TimeModel.java @ 125df22 (idea: per-step time estimates)
//          — adapted for BIOBUZZ
package org.firstinspires.ftc.teamcode.config;

import com.acmerobotics.dashboard.config.Config;

/**
 * Autonomous route geometry and time budget.
 * <p>
 * The leg distances are in centimeters because that is how they were measured and tested on the
 * field (vision-align {@code PreloadRouteAuto}). They are {@code final} on purpose: the path
 * classes in {@code pathing.paths} build their poses from them, and {@code PathsTest} checks
 * those poses against the rules. Change one only after a field test, then run the tests.
 * <p>
 * The time estimates feed {@code pathing.AutoPlanner}, which decides "one more cycle" or
 * "go park". They are best guesses: tune them from the pose logs.
 */
@Config
public final class AutoConstants {

    private AutoConstants() {
    }

    // ---------------- Pedro path following ----------------

    /** Safety timeout per Pedro path, so a stuck robot never blocks the routine. In use on VA. */
    public static double PATH_TIMEOUT_SECONDS = 8.0;

    // ---------------- Near route legs (TESTED in the 4/4 preload run, 2026-11-23) ----------------

    /** LEAVE: back-off from the start before shooting. 5 cm was not clearly enough. TESTED. */
    public static final double LEAVE_BACK_CM = 10.0;
    /** Back-off after shooting, before the park legs. TESTED. */
    public static final double SECOND_BACK_CM = 5.0;
    /** First move to the right (field -x), toward the west lane. TESTED. */
    public static final double RIGHT_1_CM = 60.0;
    /** Long move back (field +y) up the west lane. 240 ended ~10 cm too far. TESTED. */
    public static final double LONG_BACK_CM = 230.0;
    /** Last move right into the LOADING ZONE. 40 and 50 fell short. TESTED. */
    public static final double RIGHT_2_CM = 60.0;

    // ---------------- Near collect / other cell legs (FITTED from logged runs 2026-11-24) ----------------

    /** Turn before collecting, degrees clockwise (front from south to west). FITTED. */
    public static final double COLLECT_TURN_DEGREES = 90.0;
    /** Distance driven toward the ball row with the intake off. FITTED. */
    public static final double COLLECT_START_CM = 60.0;
    /** Where collecting ends, from the shooting spot (intake on from COLLECT_START_CM). FITTED. */
    public static final double COLLECT_END_CM = 120.0;
    /** Top power while driving over the balls. 0.3 was too fast, 0.15 risks stalling. FITTED. */
    public static double COLLECT_POWER = 0.2;
    /** Top power driving back to the shooting spot. FITTED. */
    public static double RETURN_POWER = 0.6;
    /** Waypoint south-west of the HIVE: RIGHT_1_CM right and this far back from the start. FITTED. */
    public static final double SOUTH_WAYPOINT_BACK_CM = 80.0;
    /** Waypoint north-west of the HIVE: RIGHT_1_CM right and this far back from the start. FITTED. */
    public static final double NORTH_WAYPOINT_BACK_CM = 257.0;
    /** Exit waypoint after collecting (away from the west wall before turning). FITTED. */
    public static final double COLLECT_EXIT_BACK_CM = 53.0;
    public static final double COLLECT_EXIT_RIGHT_CM = 84.0;
    /** The other-cell spot stops this much short of the exact mirror, to clear the back-wall FLOWER. FITTED. */
    public static final double OTHER_SPOT_SHORTEN_CM = 15.0;
    /** Robot center to the audience wall at the near start (half an 18 in robot). TESTED. */
    public static final double START_WALL_DISTANCE_CM = 22.86;
    /** Field length between the inner wall faces (144 in). Decision D19. */
    public static final double FIELD_LENGTH_CM = 365.76;

    // ---------------- Hard time limits (TESTED backstops from VA PreloadRouteAuto) ----------------

    /** Collecting is skipped if the first volley ends after this match time. */
    public static double COLLECT_LATEST_START_SECONDS = 11.0;
    /** Going to the other cell is skipped if it would start after this match time. */
    public static double OTHER_CELL_LATEST_START_SECONDS = 15.0;
    /** A volley at our scoring cell ends by this match time (the park takes ~6.3 s). */
    public static double LATEST_SHOOT_END_SECONDS = 22.0;
    /** A volley at the other cell ends by this match time (~3 s park from that side). */
    public static double OTHER_CELL_SHOOT_END_SECONDS = 26.5;
    /** Pause between odometry legs so the robot settles. TESTED. */
    public static double SETTLE_SECONDS = 0.2;

    // ---------------- Time estimates for AutoPlanner ----------------

    /**
     * Average driving speed of the odometry driver over a leg, including the ramp. FITTED to the
     * ~6.3 s measured for the 140 in near park legs. TODO: TUNE from the pose logs.
     */
    public static double CRUISE_INCHES_PER_SECOND = 25.0;
    /** Fixed time per leg (start, stop, settle). TODO: TUNE */
    public static double LEG_OVERHEAD_SECONDS = 0.2;
    /** Average in-place turn speed. TODO: TUNE */
    public static double TURN_DEGREES_PER_SECOND = 120.0;
    /** Flywheel spin-up before the first shot of a volley. TODO: TUNE */
    public static double SPIN_UP_SECONDS = 1.0;
    /** Time per ball in a volley. Shots came ~1 s apart in the good runs. FITTED. */
    public static double FEED_SECONDS_PER_BALL = 1.0;
    /** Most collect-and-shoot cycles after the preload (one 4-ball row per side). INFERRED. */
    public static int MAX_CYCLES = 1;
}
