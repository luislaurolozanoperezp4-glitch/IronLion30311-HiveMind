// Sources: IronLion30311BIOBUZZInicioDeTemporada / branch feature/mar/duopodriveNcam / TeamCode/... field map @ 2d92fba (HIVE box, tiles)
//          IronLion30311BIOBUZZInicioDeTemporada / branch feature/turrubiates/vision-align / TeamCode/.../opmodes/auto/PreloadRouteAuto.java @ a7b8f4c (near start, TESTED)
//          IronLion30311BIOBUZZInicioDeTemporada / branch feature/autonomos-beta / TeamCode/.../opmodes/auto/ParkAuto.java @ 206fa84 (far start)
//          docs/AUDIT_PEDRO.md section 5.1 (zones, FLOWERS)
//          — adapted for BIOBUZZ
package org.firstinspires.ftc.teamcode.config;

import com.pedropathing.math.Pose;

/**
 * BIOBUZZ field positions in the Pedro Pathing frame.
 * <p>
 * Frame: inches; origin = red alliance / audience corner; +x to the right, +y toward the back
 * wall; heading in radians, counter-clockwise from +x.
 * <p>
 * All poses are written for the RED alliance. The BLUE alliance uses {@code util.Alliance}
 * to convert them. Decision D13, VERIFIED against the Competition Manual TU10 (Figures 9-2,
 * 9-5 and 10-2): the field is 180-degree rotationally symmetric, so BLUE = RED rotated 180
 * degrees around the field center, NOT mirrored. The red ALLIANCE AREA is on the left seen
 * from the audience (section 9.5), so the origin corner is tile A1 and the red half is
 * columns A-C (x &lt; 72).
 * <p>
 * <b>Start poses</b> (G304, manual TU10): the robot must be fully on its own half, touching the
 * FIELD perimeter wall, not in the LOADING ZONE, not touching a FLOWER, and inside the 18 in
 * STARTING CONFIGURATION (R102). The manual has no "near" or "far" start: these are team names
 * (decision D21). NEAR = against the audience wall. FAR = the team's definition "the wall
 * farthest from our LOADING ZONE"; {@link #FAR_START} is the pose the current routines use.
 * {@code StartPositionRulesTest} checks every start pose against G304.
 * <p>
 * Every pose here except {@link #NEAR_START} is TO VERIFY on the real field.
 */
public final class FieldConstants {

    private FieldConstants() {
    }

    /** Field side length. Alternative 141.5 (inner dimension in Pedro docs). Decision D19. */
    public static final double FIELD_SIZE_INCHES = 144.0;
    public static final double FIELD_CENTER_INCHES = FIELD_SIZE_INCHES / 2.0;
    public static final double TILE_INCHES = 24.0;

    /**
     * G402: in AUTO the RED robot must stay where {@code x < TERRITORY_LINE_X}, minus half the
     * robot width. TO VERIFY against the manual (decision D13).
     */
    public static final double TERRITORY_LINE_X = 72.0;

    /**
     * Minimum gap to the wall at the end of AUTO so LEAVE counts (LEAVE = no longer touching the
     * perimeter wall, assessed at the end of AUTO, manual TU10 10.5 / 10.5.4). Team decision:
     * every competition park ends at least this far from the wall.
     */
    public static final double WALL_CLEARANCE_INCHES = 2.0;

    // ---------------- HIVE (center structure) ----------------
    public static final double HIVE_MIN_X = 47.3;
    public static final double HIVE_MAX_X = 96.7;
    public static final double HIVE_MIN_Y = 52.5;
    public static final double HIVE_MAX_Y = 91.5;

    /**
     * Red HIVE cell centers. AUDIENCE = the CELL on the audience side (AprilTags 34-37), BACK =
     * the CELL opposite the audience (30-33), manual TU10 section 9.9 (decision D20, confirmed).
     */
    public static final Pose RED_AUDIENCE_CELL = new Pose(59.25, 57.4, 0.0);
    public static final Pose RED_BACK_CELL = new Pose(59.25, 86.6, 0.0);

    // ---------------- FLOWERS (4, on the perimeter walls; absolute field positions) ----------------
    /** FLOWER centers from docs/AUDIT_PEDRO.md 5.1, matching manual Figure 9-2. TO VERIFY */
    public static final Pose AUDIENCE_WALL_FLOWER = new Pose(96.0, 1.9, 0.0);
    public static final Pose BACK_WALL_FLOWER = new Pose(48.0, 142.1, 0.0);
    public static final Pose RED_WALL_FLOWER = new Pose(1.9, 48.0, 0.0);
    public static final Pose BLUE_WALL_FLOWER = new Pose(142.1, 96.0, 0.0);
    /** Keep-out radius around a FLOWER center for start poses (G304.D). TODO: TUNE from the CAD */
    public static final double FLOWER_KEEP_OUT_RADIUS_INCHES = 4.0;

    // ---------------- RED LOADING ZONE (PARK) ----------------
    /** 23 in wide x 11 in deep against the red wall, tape included (manual TU10 9.3). */
    public static final double RED_LOADING_ZONE_MIN_X = 0.0;
    public static final double RED_LOADING_ZONE_MAX_X = 11.0;
    public static final double RED_LOADING_ZONE_MIN_Y = 96.5;
    public static final double RED_LOADING_ZONE_MAX_Y = 119.5;

    // ---------------- RED start poses ----------------
    /** TESTED in the 4/4 preload run: against the audience wall, facing down (-90 degrees). */
    public static final Pose NEAR_START = new Pose(57.0, 9.0, Math.toRadians(-90.0));
    /**
     * FAR start (team definition, see the class Javadoc): against the red side wall, tile A6,
     * facing the back wall. G304-legal (checked by {@code StartPositionRulesTest}).
     */
    public static final Pose FAR_START = new Pose(8.96, 131.19, Math.toRadians(90.0));

    // ---------------- RED scoring spots ----------------
    /** TESTED: shoot the first cell from here. */
    public static final Pose NEAR_SHOOT = new Pose(57.0, 12.9, Math.toRadians(-90.0));
    /**
     * FITTED: the second ("other") cell shooting spot, angled toward our AUDIENCE cell
     * (vision-align 4fd2293; same as {@code NearRoutePoses.OTHER_SPOT}). TODO: TUNE
     */
    public static final Pose OTHER_SHOOT = new Pose(46.764, 118.449, Math.toRadians(115.0));

    /**
     * Default PARK target: robot side {@link #WALL_CLEARANCE_INCHES} off the red wall (LEAVE)
     * while still partly inside the LOADING ZONE (PARK only needs part of the robot inside).
     */
    public static final Pose RED_PARK = new Pose(
            RED_LOADING_ZONE_MIN_X + RobotConstants.ROBOT_WIDTH_INCHES / 2.0 + WALL_CLEARANCE_INCHES,
            (RED_LOADING_ZONE_MIN_Y + RED_LOADING_ZONE_MAX_Y) / 2.0,
            Math.toRadians(90.0));
}
