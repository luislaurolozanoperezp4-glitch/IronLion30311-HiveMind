// Sources: IronLion30311BIOBUZZInicioDeTemporada / branch feature/turrubiates/vision-align / TeamCode/.../opmodes/auto/PreloadRouteAuto.java @ a7b8f4c (waypoints northWaypoint, southWaypoint, otherSpot, shootSpot)
//          IronLion30311BIOBUZZInicioDeTemporada / branch feature/autonomos-beta / TeamCode/.../opmodes/auto/ParkAuto.java @ 206fa84 (far start pose)
//          docs/BEST_OF_MATRIX.md section 4 row 23 (new route) — adapted for BIOBUZZ
package org.firstinspires.ftc.teamcode.pathing.paths;

import com.pedropathing.math.Pose;
import com.pedropathing.paths.Path;

import org.firstinspires.ftc.teamcode.config.FieldConstants;
import org.firstinspires.ftc.teamcode.util.Alliance;

/**
 * NEW, UNTESTED route for a "Far Cycle" auto (decision D21: what "far start" means). Built only
 * from waypoints already driven on the field by the near routine.
 * <p>
 * From the far start against the red wall, drive to the other-cell spot north of the HIVE and
 * shoot there; or, if that cell is down, go down the west lane to the near shooting spot. Park
 * with {@link OtherCellPaths#toPark()} or {@link PreloadCollectPaths#parkFromShootSide()}.
 */
public class FarCyclePaths extends RoutePaths {

    /** Far start, against the red wall, tile A6 (decision D21). */
    public static final Pose FAR_START = FieldConstants.FAR_START;

    /**
     * @param alliance alliance to build the paths for
     */
    public FarCyclePaths(Alliance alliance) {
        super(alliance);
    }

    /** @return the start pose for this alliance */
    public Pose getStartPose() {
        return toField(FAR_START);
    }

    /** @return far start to the north-west waypoint, then to the other-cell spot (heading 90) */
    public Path toOtherSpot() {
        return chain(
                lineConstant(FAR_START, NearRoutePoses.NORTH_WAYPOINT, NearRoutePoses.NORTH_WAYPOINT),
                lineConstant(NearRoutePoses.NORTH_WAYPOINT, NearRoutePoses.OTHER_SPOT, NearRoutePoses.OTHER_SPOT));
    }

    /** @return straight copy of {@link #toOtherSpot()}: one short diagonal north of the HIVE */
    public Path toOtherSpotStraight() {
        return lineConstant(FAR_START, NearRoutePoses.OTHER_SPOT, NearRoutePoses.OTHER_SPOT);
    }

    /**
     * @return other-cell spot down the west lane to the near shooting spot, turning 180 degrees
     *         on the way (the reverse of {@link OtherCellPaths#fromShootSpot()})
     */
    public Path otherSpotToShootSpot() {
        return chain(
                lineConstant(NearRoutePoses.OTHER_SPOT, NearRoutePoses.NORTH_WAYPOINT, NearRoutePoses.NORTH_WAYPOINT),
                lineLinear(NearRoutePoses.NORTH_WAYPOINT, NearRoutePoses.SOUTH_WAYPOINT),
                lineConstant(NearRoutePoses.SOUTH_WAYPOINT, NearRoutePoses.SHOOT_SPOT, NearRoutePoses.SHOOT_SPOT));
    }

    /**
     * Straight copy of {@link #otherSpotToShootSpot()}: straight south through x = 57.
     * <p>
     * DO NOT USE IN COMPETITION: it drives straight through the HIVE.
     *
     * @return the straight copy
     * @deprecated cuts through the HIVE box; kept only for comparison
     */
    @Deprecated
    public Path otherSpotToShootSpotStraight() {
        return lineLinear(NearRoutePoses.OTHER_SPOT, NearRoutePoses.SHOOT_SPOT);
    }
}
