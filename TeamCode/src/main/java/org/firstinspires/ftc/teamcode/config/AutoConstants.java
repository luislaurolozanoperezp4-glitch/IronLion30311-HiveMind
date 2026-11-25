// Sources: IronLion30311BIOBUZZInicioDeTemporada / branch feature/turrubiates/vision-align / TeamCode/.../opmodes/auto/PreloadRouteAuto.java @ a7b8f4c (leg distances, time limits)
//          IronLion30311BIOBUZZInicioDeTemporada / branch feature/turrubiates/vision-align / TeamCode/.../opmodes/auto/PedroAutoBase.java @ a7b8f4c (PATH_TIMEOUT_SEC)
//          IronLion30311BIOBUZZInicioDeTemporada / branch feature/turrubiates/vision-align / TeamCode/.../opmodes/auto/PreloadRouteAuto.java @ 4fd2293 (south-wall collect line, angled other-cell spot, Limelight aim) @ 1e723f2 (PARK_BACK_CM, OTHER_CELL_AIM_SEC, OTHER_CELL_MIN_SHOOT_SEC)
//          IronLion30311BIOBUZZInicioDeTemporada / branch feature/turrubiates/vision-align / TeamCode/.../opmodes/auto/PedroAutoBase.java @ 1e723f2 (PARK_MARGIN_SEC, PARK_MAX_POWER, fitted leg time model) @ f26f8d0 (AIM_* limits)
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
    /**
     * PARK depth: the robot center this far back (field +y) from the near start, so PARK is at
     * field y ~95.4, the LOW edge of the red LOADING ZONE (y 96.5-119.5). Vision-align 1e723f2
     * (team strategy): a PARK counts with part of the robot in the zone and both alliance robots
     * must fit, so ours stays low and the partner parks above (the far wait-TIP auto parks at the
     * top). The robot (y 86.4-104.4) still overlaps the zone by ~8 in. The TESTED 4/4 run parked
     * at 245 cm (y 105.5, back 230 up the lane). TODO: TUNE (re-test on the robot)
     */
    public static final double PARK_BACK_CM = 219.5;
    /**
     * Long move back (field +y) up the west lane, ending level with PARK. Derived from
     * {@link #PARK_BACK_CM} (204.5 cm). TESTED value was 230 (240 ended ~10 cm too far).
     */
    public static final double LONG_BACK_CM = PARK_BACK_CM - LEAVE_BACK_CM - SECOND_BACK_CM;
    /**
     * Long move back up the west lane for the PRELOAD + PARK route only (no collect cycle).
     * TESTED in the 4/4 run (2026-11-23) and still used by the team's current code
     * ({@code master:PreloadRouteAuto LONG_BACK_CM = 230}): PARK ends at field y ~105.5, inside the
     * LOADING ZONE. The lower {@link #PARK_BACK_CM} park (1e723f2) is used only after a collect
     * cycle or from the other cell. Review finding #1.
     */
    public static final double PRELOAD_LONG_BACK_CM = 230.0;
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
    /**
     * The 4 POLLEN of the row lie against the audience wall at field x ~1-13 in (game manual), not
     * 10 cm off it as in the tests. The robot collects along the wall: its center this far back
     * from the start line, i.e. its side ~1 cm off the wall (it starts against it). Vision-align
     * 4fd2293. TODO: TUNE (not yet run on the robot)
     */
    public static final double COLLECT_LINE_BACK_CM = 1.0;
    /**
     * Where the slow, intake-on part starts, right (field -x) of the start: 71 cm = field x ~29 in,
     * the intake ~7 in before the first ball. Vision-align 4fd2293 (was 60, FITTED for the test
     * row). TODO: TUNE
     */
    public static final double COLLECT_START_CM = 71.0;
    /** Where collecting ends, right of the start: the intake at the red wall. FITTED. */
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
    /**
     * Spot to shoot at our AUDIENCE cell from the north side, facing it at an angle: field
     * ~(46.8, 118.4) in, heading 115 deg (the team's Pedro visualizer edit, vision-align 4fd2293).
     * It clears the back-wall FLOWER and is shorter than going straight in front of the cell
     * (the old spot was the mirrored shooting spot (57, 125.2), 15 cm shortened). Given as back
     * and right of the near start, plus the turn from the start heading. TODO: TUNE
     */
    public static final double OTHER_SPOT_BACK_CM = 278.0;
    public static final double OTHER_SPOT_RIGHT_CM = 26.0;
    public static final double OTHER_SPOT_TURN_DEGREES = 205.0;

    // ---------------- Hard time limits (TESTED backstops from VA PreloadRouteAuto) ----------------

    /** Collecting is skipped if the first volley ends after this match time. */
    public static double COLLECT_LATEST_START_SECONDS = 11.0;
    /** The PRELOAD volley at our scoring cell ends by this match time (the park takes ~6.3 s). */
    public static double LATEST_SHOOT_END_SECONDS = 22.0;
    /** Pause between odometry legs so the robot settles. TESTED. */
    public static double SETTLE_SECONDS = 0.2;

    // ---------------- Time model for AutoPlanner (FITTED, vision-align 1e723f2) ----------------

    /**
     * Leg time model fitted on the 2026-11-24 runs (a 70 in park leg in 2.1 s, a 35 + 70 in park
     * in 3.6 s at power 0.85): route seconds = ESTIMATE_ROUTE_FIXED_SECONDS + per leg
     * (ESTIMATE_PER_LEG_SECONDS + inches / (this * power)). FITTED. It also reproduces the
     * ~6.3 s of the TESTED near park legs (with the settles).
     */
    public static double ESTIMATE_INCHES_PER_SECOND_PER_POWER = 70.0;
    /** Fixed seconds per route (accelerating, slowing down). FITTED. */
    public static double ESTIMATE_ROUTE_FIXED_SECONDS = 0.75;
    /** Fixed seconds per leg (corners, stops). FITTED. */
    public static double ESTIMATE_PER_LEG_SECONDS = 0.55;
    /**
     * Park time budget: the robot must be parked this long before the 30 s end (the team asked
     * for 1-2 s). Volleys end and the park speeds up to keep it. Vision-align 1e723f2. The
     * emergency time guard keeps its own, larger reserve (MatchConstants.AUTO_PARK_RESERVE_SECONDS).
     */
    public static double PARK_MARGIN_SECONDS = 1.5;
    /** Top power a late park may use (normal legs use AUTO_MAX_POWER). TODO: TUNE (never run at 1.0) */
    public static double PARK_MAX_POWER = 1.0;
    /** Average in-place turn speed. TODO: TUNE */
    public static double TURN_DEGREES_PER_SECOND = 120.0;
    /** Flywheel spin-up before the first shot of a volley. TODO: TUNE */
    public static double SPIN_UP_SECONDS = 1.0;
    /** Time per ball in a volley. Shots came ~1 s apart in the good runs. FITTED. */
    public static double FEED_SECONDS_PER_BALL = 1.0;
    /** Most collect-and-shoot cycles after the preload (one 4-ball row per side). INFERRED. */
    public static int MAX_CYCLES = 1;

    // ---------------- Wait-TIP auto (vision-align FarSideWaitTipAuto @ f26f8d0, UNTESTED) ----------------

    /**
     * Start x (center) at the back wall, right of the back-wall FLOWER. The source used 59.36
     * (~0.3 in from the FLOWER); 61.5 keeps the 4 in FLOWER keep-out (G304.D check) and leaves
     * 1.5 in to the territory line (G304.A). TODO: TUNE (place it on the field)
     */
    public static final double WAIT_TIP_START_X = 61.5;
    /** Heading of the whole routine: front to the back wall, launchers toward the HIVE. */
    public static final double WAIT_TIP_HEADING_DEG = 90.0;
    /**
     * PARK y in the UPPER part of the LOADING ZONE (robot y 112-130, 7.5 in inside the zone), so
     * the partner fits at the zone's low edge. Source: 121. TODO: TUNE
     */
    public static final double WAIT_TIP_PARK_Y = 121.0;
    /** POLLEN pre-spin while waiting for the TIP (a moderate idle). Source value. TODO: TUNE */
    public static double WAIT_TIP_PRESPIN_RPM = 2000.0;
    /** The cell counts as up and still after its tags are seen this long without moving. TODO: TUNE */
    public static double WAIT_TIP_STABLE_SECONDS = 0.8;
    /** ...with the camera-to-cell distance changing less than this, cm. TODO: TUNE */
    public static double WAIT_TIP_STABLE_MAX_MOVE_CM = 3.0;
    /** Time kept for aiming after the TIP. Source value. TODO: TUNE */
    public static double WAIT_TIP_AIM_SECONDS = 2.0;
    /** Shortest volley worth waiting for; the wait gives up before it no longer fits. TODO: TUNE */
    public static double WAIT_TIP_MIN_SHOOT_SECONDS = 3.0;
    /** A volley shorter than this is skipped (straight to park). Source: 1.0. */
    public static double WAIT_TIP_MIN_VOLLEY_SECONDS = 1.0;
    /**
     * Largest aim turn here: at x 61.5 a robot turned more than ~9.6 deg reaches past the
     * territory line (x 72) and the G402 guard would stop the drive for the rest of AUTO.
     */
    public static double WAIT_TIP_AIM_MAX_TURN_DEGREES = 9.0;

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
    /**
     * The other cell is only tried if, by the time model, the robot can get there, look, aim
     * (this long), shoot for at least {@link #OTHER_CELL_MIN_SHOOT_SECONDS} and still park by the
     * park deadline. Vision-align 1e723f2. TODO: TUNE
     */
    public static double OTHER_CELL_AIM_SECONDS = 2.0;
    /** Shortest other-cell volley worth going for (1-2 balls). Vision-align 1e723f2. TODO: TUNE */
    public static double OTHER_CELL_MIN_SHOOT_SECONDS = 2.0;

    // ---------------- Limelight aim at a cell (vision-align 4fd2293 / f26f8d0, UNTESTED) ----------------

    /**
     * Aim reference when it was not measured at the first volley: bearing and camera-to-cell
     * distance of our scoring cell seen from the shooting spot (the volley that scores).
     */
    public static double DEFAULT_SHOOT_BEARING_DEGREES = 10.8;
    public static double DEFAULT_SHOOT_DISTANCE_METERS = 1.22;
    /** Largest aim turn, in degrees. TODO: TUNE */
    public static double AIM_MAX_TURN_DEGREES = 25.0;
    /**
     * Sign of the aim turn: -1 = a cell right of the crosshair needs a clockwise turn (the camera
     * is on the rear). TODO: TUNE (flip it if the robot turns away from the cell)
     */
    public static double AIM_TURN_SIGN = -1.0;
    /** Smaller aim turns are skipped, degrees. */
    public static double AIM_MIN_TURN_DEGREES = 1.5;
    /** Largest aim move toward the cell / away from it (toward the FLOWER), cm. TODO: TUNE */
    public static double AIM_MAX_TOWARD_CM = 25.0;
    public static double AIM_MAX_AWAY_CM = 8.0;
    /** Smaller aim moves are skipped, cm. */
    public static double AIM_MIN_MOVE_CM = 3.0;
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
     * Aim turns and aim moves are shortened so the robot stays at least this far inside our
     * territory, turn overshoot included, and never trips the G402 guard (review finding #2).
     */
    public static double AIM_TERRITORY_SAFETY_INCHES = 1.0;

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
