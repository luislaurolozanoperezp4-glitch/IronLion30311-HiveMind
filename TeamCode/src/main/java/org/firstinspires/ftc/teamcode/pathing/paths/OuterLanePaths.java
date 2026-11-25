// Sources: IronLion30311BIOBUZZInicioDeTemporada / branch feature/autonomos-beta / TeamCode/.../opmodes/auto/OuterLaneAuto.java @ 206fa84 (buildPoses, backOffWall, toNearCorner, backToStartColumn, toShootPose, toOuterWall, toPark)
//          IronLion30311BIOBUZZInicioDeTemporada / branch feature/autonomos-beta / TeamCode/.../opmodes/base/PedroAutoBase.java @ 206fa84 (PRELOAD_HEADING_DEG -95)
//          — adapted for BIOBUZZ (the beta "blue = left side" poses are RED geometry, docs/AUDIT_PEDRO.md section 5.2)
package org.firstinspires.ftc.teamcode.pathing.paths;

import com.pedropathing.math.Pose;
import com.pedropathing.paths.Path;

import org.firstinspires.ftc.teamcode.util.Alliance;

/**
 * "Outer Lane" (beta, {@code @Disabled} there, never field-tested): back off and shoot the
 * preload, collect in the near corner, come back and shoot, then drive up along the red wall,
 * west of the HIVE, into the LOADING ZONE.
 * <p>
 * Stays clear of the HIVE. Candidate for a "Near Outer Lane" auto after a field test. Its park
 * ends at y 118.67, 0.8 in inside the LOADING ZONE edge (marginal). The poses come from the
 * Pedro visualizer export (four decimals kept exactly).
 */
public class OuterLanePaths extends RoutePaths {

    /** Start: rear against the audience wall, front up the field. */
    public static final Pose START = pose(56.0, 8.0, 90.0);
    /** Preload shot after backing off (heading -95: launchers face the HIVE). */
    public static final Pose BACK_OFF = pose(56.0449, 14.1062, -95.0);
    /** Near corner pick-up. */
    public static final Pose NEAR_CORNER = pose(9.1714, 9.1132, -173.9198);
    /** Back at the start column. */
    public static final Pose START_COLUMN = pose(54.8148, 16.8376, -170.3946);
    /** Second shot. */
    public static final Pose SHOOT_POSE = pose(54.8518, 20.287, -97.0);
    /** Along the red wall, west of the HIVE. */
    public static final Pose OUTER_WALL = pose(12.6814, 79.1872, 125.6013);
    /** PARK in the LOADING ZONE. */
    public static final Pose PARK = pose(10.0773, 118.6701, 93.7734);

    /**
     * @param alliance alliance to build the paths for
     */
    public OuterLanePaths(Alliance alliance) {
        super(alliance);
    }

    /** @return the start pose for this alliance */
    public Pose getStartPose() {
        return toField(START);
    }

    /** @return O1: off the wall to the preload shot, turning to the shot heading */
    public Path backOffWall() {
        return lineLinear(START, BACK_OFF);
    }

    /** @return straight copy of {@link #backOffWall()} (already one line) */
    public Path backOffWallStraight() {
        return lineLinear(START, BACK_OFF);
    }

    /** @return O2: to the near corner, intake first */
    public Path toNearCorner() {
        return lineTangent(BACK_OFF, NEAR_CORNER);
    }

    /** @return straight copy of {@link #toNearCorner()} (already one line) */
    public Path toNearCornerStraight() {
        return lineTangent(BACK_OFF, NEAR_CORNER);
    }

    /** @return O3: back to the start column, rear first */
    public Path backToStartColumn() {
        return lineReverseTangent(NEAR_CORNER, START_COLUMN);
    }

    /** @return straight copy of {@link #backToStartColumn()} (already one line) */
    public Path backToStartColumnStraight() {
        return lineReverseTangent(NEAR_CORNER, START_COLUMN);
    }

    /** @return O4: short move to the second shot, turning to aim */
    public Path toShootPose() {
        return lineLinear(START_COLUMN, SHOOT_POSE);
    }

    /** @return straight copy of {@link #toShootPose()} (already one line) */
    public Path toShootPoseStraight() {
        return lineLinear(START_COLUMN, SHOOT_POSE);
    }

    /** @return O5: diagonal to the red wall, west of the HIVE */
    public Path toOuterWall() {
        return lineTangent(SHOOT_POSE, OUTER_WALL);
    }

    /** @return straight copy of {@link #toOuterWall()} (already one line) */
    public Path toOuterWallStraight() {
        return lineTangent(SHOOT_POSE, OUTER_WALL);
    }

    /** @return O6: up the red wall into the LOADING ZONE */
    public Path toPark() {
        return lineTangent(OUTER_WALL, PARK);
    }

    /** @return straight copy of {@link #toPark()} (already one line) */
    public Path toParkStraight() {
        return lineTangent(OUTER_WALL, PARK);
    }
}
