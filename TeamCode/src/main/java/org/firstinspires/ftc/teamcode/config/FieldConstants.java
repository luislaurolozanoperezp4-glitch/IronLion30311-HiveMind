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
 * to convert them. Decision D13 (team must confirm): the field is 180-degree rotationally
 * symmetric, so BLUE = RED rotated 180 degrees around the field center, NOT mirrored.
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

    /** Minimum gap to the wall at the end of AUTO so LEAVE counts. */
    public static final double WALL_CLEARANCE_INCHES = 2.0;

    // ---------------- HIVE (center structure) ----------------
    public static final double HIVE_MIN_X = 47.3;
    public static final double HIVE_MAX_X = 96.7;
    public static final double HIVE_MIN_Y = 52.5;
    public static final double HIVE_MAX_Y = 91.5;

    /** Red HIVE cell centers. Which AprilTag IDs belong to which cell: decision D20. */
    public static final Pose RED_SOUTH_CELL = new Pose(59.25, 57.4, 0.0);
    public static final Pose RED_NORTH_CELL = new Pose(59.25, 86.6, 0.0);

    // ---------------- RED LOADING ZONE (PARK) ----------------
    public static final double RED_LOADING_ZONE_MIN_X = 0.0;
    public static final double RED_LOADING_ZONE_MAX_X = 11.0;
    public static final double RED_LOADING_ZONE_MIN_Y = 96.5;
    public static final double RED_LOADING_ZONE_MAX_Y = 119.5;

    // ---------------- RED start poses ----------------
    /** TESTED in the 4/4 preload run: against the audience wall, facing down (-90 degrees). */
    public static final Pose NEAR_START = new Pose(57.0, 9.0, Math.toRadians(-90.0));
    /** Against the red side wall, tile A6. Decision D21: confirm "far" and legality (G304). */
    public static final Pose FAR_START = new Pose(8.96, 131.19, Math.toRadians(90.0));

    // ---------------- RED scoring spots ----------------
    /** TESTED: shoot the first cell from here. */
    public static final Pose NEAR_SHOOT = new Pose(57.0, 12.9, Math.toRadians(-90.0));
    /** FITTED: the second ("other") cell shooting spot. TODO: TUNE */
    public static final Pose OTHER_SHOOT = new Pose(57.0, 125.2, Math.toRadians(90.0));

    /** Center of the RED LOADING ZONE, used as the default PARK target. */
    public static final Pose RED_PARK = new Pose(
            (RED_LOADING_ZONE_MIN_X + RED_LOADING_ZONE_MAX_X) / 2.0 + WALL_CLEARANCE_INCHES,
            (RED_LOADING_ZONE_MIN_Y + RED_LOADING_ZONE_MAX_Y) / 2.0,
            Math.toRadians(90.0));
}
