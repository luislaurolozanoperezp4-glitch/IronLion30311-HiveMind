// Sources: IronLion30311BIOBUZZInicioDeTemporada / branch feature/autonomos-beta / TeamCode/.../opmodes/auto/CenterLaneAuto.java @ 206fa84 (buildPoses, offTheWall, intoCorner, upTheWall, underTheStructure, sidewaysToPark)
//          IronLion30311BIOBUZZInicioDeTemporada / branch feature/autonomos-beta / TeamCode/.../Constants.java @ 206fa84 (Constants.CenterLane poses)
//          — adapted for BIOBUZZ (the beta "blue = left side" poses are RED geometry, docs/AUDIT_PEDRO.md section 5.2)
package org.firstinspires.ftc.teamcode.pathing.paths;

import com.pedropathing.math.Pose;
import com.pedropathing.paths.Path;

import org.firstinspires.ftc.teamcode.util.Alliance;

/**
 * "Center Lane" (beta, never field-tested): shoot one tile off the wall, sweep the audience wall
 * into the corner while turning, sweep up the red wall, cross straight up UNDER the HIVE ending
 * aimed, shoot again and slide into the LOADING ZONE. No in-place turns.
 * <p>
 * ARCHIVE: the crossing under the HIVE ({@link #underTheStructure()}) is not allowed in
 * competition until decision D22 is answered. Note: Pedro's {@code linear} heading takes the
 * short way, so {@link #intoCorner()} turns from -96 to 180 degrees clockwise (84 degrees).
 */
public class CenterLanePaths extends RoutePaths {

    private static final double BETA_ROBOT_WIDTH_INCHES = 17.83;
    private static final double BETA_ROBOT_LENGTH_INCHES = 16.5354;

    /** Start: front against the audience wall, 0.25 in gap. */
    public static final Pose START = pose(56.0, BETA_ROBOT_LENGTH_INCHES / 2.0 + 0.25, -90.0);
    /** Shot 1, one tile off the wall. */
    public static final Pose SHOOT_1 = pose(56.0, 24.0, -96.0);
    /** Corner, front 1.5 in from the red wall. */
    public static final Pose CORNER = pose(BETA_ROBOT_LENGTH_INCHES / 2.0 + 1.5, 12.0, 180.0);
    /** End of the sweep up the red wall. */
    public static final Pose WALL_SWEEP_END = pose(BETA_ROBOT_LENGTH_INCHES / 2.0 + 1.5, 30.0, 180.0);
    /** Start of the crossing, south of the HIVE, facing up the field. */
    public static final Pose CROSS_BOTTOM = pose(60.0, 40.0, 90.0);
    /** Shot 2 north of the HIVE, already aimed. */
    public static final Pose SHOOT_2 = pose(60.0, 120.0, 90.0);
    /** PARK in the LOADING ZONE. */
    public static final Pose PARK = pose(BETA_ROBOT_WIDTH_INCHES / 2.0 + 1.0, 108.0, 90.0);

    /**
     * @param alliance alliance to build the paths for
     */
    public CenterLanePaths(Alliance alliance) {
        super(alliance);
    }

    /** @return the start pose for this alliance */
    public Pose getStartPose() {
        return toField(START);
    }

    /** @return C1: off the wall, easing to the shot-1 heading */
    public Path offTheWall() {
        return lineLinear(START, SHOOT_1);
    }

    /** @return straight copy of {@link #offTheWall()} (already one line) */
    public Path offTheWallStraight() {
        return lineLinear(START, SHOOT_1);
    }

    /** @return C2: diagonal into the corner, turning to face the red wall */
    public Path intoCorner() {
        return lineLinear(SHOOT_1, CORNER);
    }

    /** @return straight copy of {@link #intoCorner()} (already one line) */
    public Path intoCornerStraight() {
        return lineLinear(SHOOT_1, CORNER);
    }

    /** @return C3: slide up the red wall, intake to the wall */
    public Path upTheWall() {
        return lineConstant(CORNER, WALL_SWEEP_END, CORNER);
    }

    /** @return straight copy of {@link #upTheWall()} (already one line) */
    public Path upTheWallStraight() {
        return lineConstant(CORNER, WALL_SWEEP_END, CORNER);
    }

    /**
     * C4: away from the wall turning to face up the field, then straight up under the HIVE.
     * <p>
     * DO NOT USE IN COMPETITION: drives under the HIVE (x = 60, y 40 to 120), decision D22.
     *
     * @return the original two-line chain
     * @deprecated drives under the HIVE; kept as the original geometry
     */
    @Deprecated
    public Path underTheStructure() {
        return chain(
                lineLinear(WALL_SWEEP_END, CROSS_BOTTOM),
                lineConstant(CROSS_BOTTOM, SHOOT_2, SHOOT_2));
    }

    /**
     * Straight copy of {@link #underTheStructure()}: red wall to the shot-2 spot in one diagonal.
     * <p>
     * DO NOT USE IN COMPETITION: cuts through the HIVE.
     *
     * @return the straight copy
     * @deprecated cuts through the HIVE box; kept only for comparison
     */
    @Deprecated
    public Path underTheStructureStraight() {
        return lineLinear(WALL_SWEEP_END, SHOOT_2);
    }

    /** @return C5: slide into the LOADING ZONE */
    public Path sidewaysToPark() {
        return lineConstant(SHOOT_2, PARK, SHOOT_2);
    }

    /** @return straight copy of {@link #sidewaysToPark()} (already one line) */
    public Path sidewaysToParkStraight() {
        return lineConstant(SHOOT_2, PARK, SHOOT_2);
    }
}
