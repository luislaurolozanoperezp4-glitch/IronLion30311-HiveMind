// Sources: IronLion30311BIOBUZZInicioDeTemporada / branch feature/autonomos-beta / TeamCode/.../opmodes/auto/TickAuto.java @ 206fa84 (buildPoses, offTheWall, intoCorner, sidewaysToShootSpot, sidewaysToPark)
//          IronLion30311BIOBUZZInicioDeTemporada / branch feature/autonomos-beta / TeamCode/.../Constants.java @ 206fa84 (Constants.Tick poses)
//          — adapted for BIOBUZZ (the beta "blue = left side" poses are RED geometry, docs/AUDIT_PEDRO.md section 5.2)
package org.firstinspires.ftc.teamcode.pathing.paths;

import com.pedropathing.math.Pose;
import com.pedropathing.paths.Path;

import org.firstinspires.ftc.teamcode.util.Alliance;

/**
 * "Tick" (beta, never field-tested): one tile off the audience wall and shoot, turn 95 degrees,
 * drive into the near corner collecting, slide straight up UNDER the HIVE, turn 90 degrees,
 * shoot again and slide into the LOADING ZONE.
 * <p>
 * ARCHIVE: the crossing under the HIVE ({@link #sidewaysToShootSpot()}) is not allowed in
 * competition until decision D22 (can the robot pass under the HIVE, G417?) is answered.
 * <p>
 * Original geometry kept exactly. Robot size in the beta code: 17.83 in wide, 16.54 in long.
 */
public class TickPaths extends RoutePaths {

    private static final double BETA_ROBOT_WIDTH_INCHES = 17.83;
    private static final double BETA_ROBOT_LENGTH_INCHES = 16.5354;

    /** Start: front against the audience wall, 0.25 in gap. */
    public static final Pose START = pose(60.0, BETA_ROBOT_LENGTH_INCHES / 2.0 + 0.25, -90.0);
    /** Shot 1, one tile off the wall. */
    public static final Pose SHOOT_1 = pose(60.0, 24.0, -85.0);
    /** Shot 1 spot after the 95 degree turn (front to the red wall). */
    public static final Pose SHOOT_1_TURNED = pose(60.0, 24.0, 180.0);
    /** Corner pick-up, front 1.5 in from the red wall. */
    public static final Pose CORNER = pose(BETA_ROBOT_LENGTH_INCHES / 2.0 + 1.5, 16.0, 180.0);
    /** Start of the crossing, south of the HIVE. */
    public static final Pose CROSS_BOTTOM = pose(60.0, 30.0, 180.0);
    /** Shot 2 spot north of the HIVE, before the 90 degree turn. */
    public static final Pose SHOOT_2_APPROACH = pose(60.0, 120.0, 180.0);
    /** Shot 2 spot after the turn (launchers face the HIVE). */
    public static final Pose SHOOT_2 = pose(60.0, 120.0, 90.0);
    /** PARK in the LOADING ZONE (edge 1 in from the red wall). */
    public static final Pose PARK = pose(BETA_ROBOT_WIDTH_INCHES / 2.0 + 1.0, 108.0, 90.0);

    /**
     * @param alliance alliance to build the paths for
     */
    public TickPaths(Alliance alliance) {
        super(alliance);
    }

    /** @return the start pose for this alliance */
    public Pose getStartPose() {
        return toField(START);
    }

    /** @return T1: off the wall (rear first), easing to the shot-1 heading */
    public Path offTheWall() {
        return lineLinear(START, SHOOT_1);
    }

    /** @return straight copy of {@link #offTheWall()} (already one line) */
    public Path offTheWallStraight() {
        return lineLinear(START, SHOOT_1);
    }

    /** @return T2: into the near corner, intake first */
    public Path intoCorner() {
        return lineConstant(SHOOT_1_TURNED, CORNER, CORNER);
    }

    /** @return straight copy of {@link #intoCorner()} (already one line) */
    public Path intoCornerStraight() {
        return lineConstant(SHOOT_1_TURNED, CORNER, CORNER);
    }

    /**
     * T3: back out of the corner to south of the HIVE, then slide straight up under it.
     * <p>
     * DO NOT USE IN COMPETITION: drives under the HIVE (x = 60, y 30 to 120), decision D22.
     *
     * @return the original two-line chain
     * @deprecated drives under the HIVE; kept as the original geometry
     */
    @Deprecated
    public Path sidewaysToShootSpot() {
        return chain(
                lineConstant(CORNER, CROSS_BOTTOM, CORNER),
                lineConstant(CROSS_BOTTOM, SHOOT_2_APPROACH, CORNER));
    }

    /**
     * Straight copy of {@link #sidewaysToShootSpot()}: corner to the shot-2 spot in one diagonal.
     * <p>
     * DO NOT USE IN COMPETITION: cuts diagonally through the HIVE.
     *
     * @return the straight copy
     * @deprecated cuts through the HIVE box; kept only for comparison
     */
    @Deprecated
    public Path sidewaysToShootSpotStraight() {
        return lineConstant(CORNER, SHOOT_2_APPROACH, CORNER);
    }

    /** @return T4: slide into the LOADING ZONE */
    public Path sidewaysToPark() {
        return lineConstant(SHOOT_2, PARK, SHOOT_2);
    }

    /** @return straight copy of {@link #sidewaysToPark()} (already one line) */
    public Path sidewaysToParkStraight() {
        return lineConstant(SHOOT_2, PARK, SHOOT_2);
    }
}
