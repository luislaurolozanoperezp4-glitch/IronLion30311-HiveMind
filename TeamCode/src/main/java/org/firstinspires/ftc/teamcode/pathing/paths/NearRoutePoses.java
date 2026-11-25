// Sources: IronLion30311BIOBUZZInicioDeTemporada / branch feature/turrubiates/vision-align / TeamCode/.../opmodes/auto/PreloadRouteAuto.java @ a7b8f4c (buildPoses, relative) @ 4fd2293 (collect along the wall, angled other-cell spot) @ 1e723f2 (park at the zone's low edge) — adapted for BIOBUZZ (park kept 2.1 in off the wall)
package org.firstinspires.ftc.teamcode.pathing.paths;

import com.pedropathing.math.Pose;

import org.firstinspires.ftc.teamcode.config.AutoConstants;
import org.firstinspires.ftc.teamcode.config.FieldConstants;

/**
 * The RED poses of the TESTED near routine (vision-align {@code PreloadRouteAuto} and
 * {@code PreloadCollectAuto}), computed exactly as the original did: every pose is "back" and
 * "right" of the start in the start's robot frame, in centimeters from {@link AutoConstants}.
 * <p>
 * The robot starts at {@link FieldConstants#NEAR_START} (57, 9, -90 deg): front (intake) against
 * the audience wall, launchers toward the HIVE. "Back" is field +y and "right" is field -x.
 * <p>
 * Pose values (inches, degrees), for reference:
 * <pre>
 * SHOOT_SPOT              (57.000, 12.937,  -90)   LEAVE 10 cm, shoot here (TESTED)
 * BACK_OFF_2              (57.000, 14.906,  -90)
 * WEST_LANE_SOUTH (right1)(33.378, 14.906,  -90)
 * WEST_LANE_NORTH (long)  (33.378, 95.417,  -90)   TESTED at y 105.457 (long back 230)
 * PARK (right2)           (11.134, 95.417,  -90)   zone's low edge, 2.1 in off the wall
 * COLLECT_START           (29.047,  9.394,  180)   along the audience wall (4fd2293)
 * COLLECT_END             ( 9.756,  9.394,  180)
 * COLLECT_EXIT            (23.929, 29.866,  180)
 * SOUTH_WAYPOINT          (33.378, 40.496,  -90)
 * NORTH_WAYPOINT          (33.378,110.181,  115)
 * OTHER_SPOT              (46.764,118.449,  115)   angled spot facing the audience cell
 * OTHER_PARK              (11.134, 95.417,   90)   square to the wall again (see OTHER_PARK)
 * </pre>
 */
public final class NearRoutePoses {

    private NearRoutePoses() {
    }

    private static final double CM_PER_INCH = 2.54;

    /** Heading of the near start, degrees. TESTED. */
    public static final double START_HEADING_DEG = -90.0;
    /** Heading while collecting the row (front to the red wall), degrees. */
    public static final double COLLECT_HEADING_DEG = START_HEADING_DEG - AutoConstants.COLLECT_TURN_DEGREES;
    /** Heading at the other cell (launchers face our AUDIENCE cell from the north-west), degrees. */
    public static final double OTHER_HEADING_DEG = START_HEADING_DEG + AutoConstants.OTHER_SPOT_TURN_DEGREES;

    private static final double BACK_AFTER_LEAVE = AutoConstants.LEAVE_BACK_CM;
    private static final double BACK_AFTER_SECOND = BACK_AFTER_LEAVE + AutoConstants.SECOND_BACK_CM;
    private static final double BACK_AFTER_LONG = BACK_AFTER_SECOND + AutoConstants.LONG_BACK_CM;

    /** Near start. TESTED. */
    public static final Pose START = FieldConstants.NEAR_START;
    /** After the 10 cm LEAVE: the preload shooting spot. TESTED. */
    public static final Pose SHOOT_SPOT = relative(BACK_AFTER_LEAVE, 0.0, START_HEADING_DEG);
    /** Shooting spot after the turn toward the ball row. */
    public static final Pose SHOOT_SPOT_FACING_COLLECT = relative(BACK_AFTER_LEAVE, 0.0, COLLECT_HEADING_DEG);
    /** Where the intake turns on, on the way to the ball row (side ~1 cm off the audience wall). */
    public static final Pose COLLECT_START = relative(AutoConstants.COLLECT_LINE_BACK_CM,
            AutoConstants.COLLECT_START_CM, COLLECT_HEADING_DEG);
    /** End of the ball row (less than 1 in from the red wall: do not turn here). */
    public static final Pose COLLECT_END = relative(AutoConstants.COLLECT_LINE_BACK_CM,
            AutoConstants.COLLECT_END_CM, COLLECT_HEADING_DEG);
    /** Second back-off before the park legs. TESTED. */
    public static final Pose BACK_OFF_2 = relative(BACK_AFTER_SECOND, 0.0, START_HEADING_DEG);
    /** South end of the west lane (the original "right1"). TESTED. */
    public static final Pose WEST_LANE_SOUTH = relative(BACK_AFTER_SECOND, AutoConstants.RIGHT_1_CM,
            START_HEADING_DEG);
    /** North end of the west lane (the original "longBack"). TESTED. */
    public static final Pose WEST_LANE_NORTH = relative(BACK_AFTER_LONG, AutoConstants.RIGHT_1_CM,
            START_HEADING_DEG);
    /** PARK in the red LOADING ZONE (the original "right2"), 2 in off the wall for LEAVE. */
    public static final Pose PARK = relative(BACK_AFTER_LONG,
            AutoConstants.RIGHT_1_CM + AutoConstants.RIGHT_2_CM, START_HEADING_DEG);
    /** Pass-through waypoint south-west of the HIVE. */
    public static final Pose SOUTH_WAYPOINT = relative(AutoConstants.SOUTH_WAYPOINT_BACK_CM,
            AutoConstants.RIGHT_1_CM, START_HEADING_DEG);
    /** Same position as {@link #SOUTH_WAYPOINT}, turned to the other-cell heading. */
    public static final Pose SOUTH_WAYPOINT_TURNED = relative(AutoConstants.SOUTH_WAYPOINT_BACK_CM,
            AutoConstants.RIGHT_1_CM, OTHER_HEADING_DEG);
    /** Pass-through waypoint north-west of the HIVE. */
    public static final Pose NORTH_WAYPOINT = relative(AutoConstants.NORTH_WAYPOINT_BACK_CM,
            AutoConstants.RIGHT_1_CM, OTHER_HEADING_DEG);
    /** Exit waypoint after collecting, away from the red wall before turning. */
    public static final Pose COLLECT_EXIT = relative(AutoConstants.COLLECT_EXIT_BACK_CM,
            AutoConstants.COLLECT_EXIT_RIGHT_CM, COLLECT_HEADING_DEG);
    /** Angled spot facing our AUDIENCE cell from the north, clear of the back-wall FLOWER. */
    public static final Pose OTHER_SPOT = relative(AutoConstants.OTHER_SPOT_BACK_CM,
            AutoConstants.OTHER_SPOT_RIGHT_CM, OTHER_HEADING_DEG);
    /**
     * Heading of the PARK reached from the other cell: square to the red wall (launchers north),
     * the TESTED heading of that park leg. Vision-align 4fd2293 kept the angled 115 deg here, but
     * a turned 18 in robot reaches ~12 in sideways, so at x 11.1 a corner would touch the wall
     * and LEAVE would fail (team decision: every park ends at least 2 in off the wall).
     */
    public static final double OTHER_PARK_HEADING_DEG = START_HEADING_DEG + 180.0;
    /** PARK position reached from the other cell (square to the wall, see {@link #OTHER_PARK_HEADING_DEG}). */
    public static final Pose OTHER_PARK = relative(BACK_AFTER_LONG,
            AutoConstants.RIGHT_1_CM + AutoConstants.RIGHT_2_CM, OTHER_PARK_HEADING_DEG);

    /**
     * Pose {@code backCm} behind and {@code rightCm} to the right of the near start, in the
     * start's robot frame (the original {@code relative()} with forward = -back).
     *
     * @param backCm     centimeters back from the start (toward the HIVE)
     * @param rightCm    centimeters to the robot's right at the start (toward the red wall)
     * @param headingDeg field heading of the result, degrees
     * @return the RED field pose
     */
    public static Pose relative(double backCm, double rightCm, double headingDeg) {
        double heading = Math.toRadians(START_HEADING_DEG);
        double forwardInches = -backCm / CM_PER_INCH;
        double rightInches = rightCm / CM_PER_INCH;
        double x = START.x() + forwardInches * Math.cos(heading) + rightInches * Math.sin(heading);
        double y = START.y() + forwardInches * Math.sin(heading) - rightInches * Math.cos(heading);
        return new Pose(x, y, Math.toRadians(headingDeg));
    }
}
