// Sources: IronLion30311BIOBUZZInicioDeTemporada / branch feature/turrubiates/vision-align / TeamCode/.../opmodes/auto/PreloadRouteAuto.java @ a7b8f4c (leg distances, time limits)
//          IronLion30311BIOBUZZInicioDeTemporada / branch feature/turrubiates/vision-align / TeamCode/.../opmodes/auto/PedroAutoBase.java @ a7b8f4c (PATH_TIMEOUT_SEC)
//          IronLion30311BIOBUZZInicioDeTemporada / branch feature/theautonomous / TeamCode/.../TimeModel.java @ 125df22 (idea: per-step time estimates)
//          IronLion30311BIOBUZZInicioDeTemporada / branch feature/turrubiates/auto-leave-park / TeamCode/.../opmodes/auto/LeaveParkAuto.java @ e455142 (encoder LEAVE values)
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
    /**
     * Last move right into the LOADING ZONE. 40 and 50 fell short; 60 was TESTED but ended the
     * robot ~0.8 in from the red wall, so LEAVE was at risk. Team decision (rules agent,
     * 2026-11-25): 56.5 ends the robot side ~2.1 in off the wall (FieldConstants
     * WALL_CLEARANCE_INCHES) and still partly in the zone. TODO: TUNE (re-test on the robot)
     */
    public static final double RIGHT_2_CM = 56.5;

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

    // ---------------- Competition OpModes (opmodes/auto) ----------------

    /** Longest start delay the drive team can pick in INIT (alliance-partner coordination). */
    public static double START_DELAY_MAX_SECONDS = 10.0;
    /** Start delay change per D-pad press in INIT. */
    public static double START_DELAY_STEP_SECONDS = 0.5;

    /** Balls on the near collection row (the second volley). TESTED (VA COLLECT_BALLS). */
    public static int COLLECT_BALLS = 4;
    /** Early TIP watch after the preload volley, longest. TESTED (VA EARLY_TIP_WATCH_SEC). */
    public static double EARLY_TIP_WATCH_SECONDS = 4.0;
    /** Early TIP watch: tags gone this long = cell down. TESTED (VA EARLY_TIP_GONE_SEC). */
    public static double EARLY_TIP_GONE_SECONDS = 0.8;
    /** TIP check back at the shooting spot: look this long for our cell. TESTED (VA TIP_CHECK_SEC). */
    public static double TIP_CHECK_SECONDS = 0.6;
    /** The TIP check is trusted only this close to the shooting spot. TESTED (VA). */
    public static double TIP_CHECK_POSITION_TOLERANCE_CM = 10.0;
    public static double TIP_CHECK_HEADING_TOLERANCE_DEGREES = 8.0;
    /** How long to look for the other cell's tags at the other-cell spot. TESTED (VA). */
    public static double OTHER_CELL_LOOK_SECONDS = 0.8;
    /** POLLEN RPM at the other cell (2800 shot short on 2026-11-24). TODO: TUNE */
    public static double OTHER_CELL_POLLEN_RPM = 2950.0;
    /** The other-cell volley needs at least this long before its deadline to start. (VA: 1.0) */
    public static double OTHER_CELL_MIN_SHOOT_SECONDS = 1.0;
    /** Largest Limelight distance correction at the other cell (never away from the cell). TESTED (VA). */
    public static double VISION_APPROACH_MAX_CM = 25.0;
    /** Smaller vision corrections are skipped. (VA: 3 cm) */
    public static double VISION_APPROACH_MIN_CM = 3.0;
    /** Camera-to-cell distance to aim for when it was not measured at the first volley. (VA) */
    public static double DEFAULT_SHOOT_DISTANCE_METERS = 1.20;
    /**
     * AutoPlanner: a cycle is only started if at least a short volley (spin-up + this many balls)
     * fits; the volley itself is cut by its deadline. TODO: TUNE
     */
    public static int CYCLE_MIN_PLANNED_BALLS = 1;

    /**
     * G402 runtime guard: the drive stops for good if the robot center (in the RED frame) goes
     * past {@code TERRITORY_LINE_X - half the robot width + this margin}. Every competition
     * route keeps the center at x 57 or less (edge 66 in), so the guard only trips on a fault.
     */
    public static double TERRITORY_GUARD_MARGIN_INCHES = 0.0;

    /**
     * Emergency park (time ran out mid-step): a robot this close to the west lane (x 33.4) is
     * already in it and drives straight up the lane. TODO: TUNE
     */
    public static double EMERGENCY_PARK_LANE_TOLERANCE_INCHES = 2.0;

    /**
     * "Beside the loading zone" start (Safe Park): front (intake) against the red wall (heading
     * 180), the whole robot just behind the LOADING ZONE tape (y > 119.5), because G304.E forbids
     * starting in the LOADING ZONE (manual TU10). Was y 108 (inside the zone: illegal).
     * TODO: TUNE (measure on the field)
     */
    public static final double LOADING_ZONE_START_X = 9.0;
    public static final double LOADING_ZONE_START_Y = 129.5;
    public static final double LOADING_ZONE_START_HEADING_DEG = 180.0;
    /** LEAVE from the beside-the-zone start: back off the wall. (ALP: 10 cm) */
    public static final double LOADING_ZONE_LEAVE_CM = 10.0;

    /** Encoder-only LEAVE (Safe Park Encoders), from ALP LeaveParkAuto. */
    public static double ENCODER_LEAVE_CM = 10.0;
    /** Constant power of the encoder LEAVE, backwards (ALP: 0.30). */
    public static double ENCODER_LEAVE_POWER = 0.30;
    /** Hard timeout of the encoder LEAVE, so a dead encoder cannot drive far (ALP: 0.6 s). */
    public static double ENCODER_LEAVE_TIMEOUT_SECONDS = 0.6;
}
