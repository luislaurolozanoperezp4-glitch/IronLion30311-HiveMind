// Sources: IronLion30311BIOBUZZInicioDeTemporada / branch feature/turrubiates/vision-align / TeamCode/.../opmodes/auto/PreloadRouteAuto.java @ a7b8f4c (runCollectRoutine TIP branches, shootAtOtherCellAndPark) — adapted for BIOBUZZ
package org.firstinspires.ftc.teamcode.pathing.paths;

import com.pedropathing.math.Pose;
import com.pedropathing.paths.Path;

import org.firstinspires.ftc.teamcode.util.Alliance;

import java.util.Arrays;
import java.util.List;

/**
 * Routes to the "other" HIVE cell, used after a TIP (our scoring cell went down), from
 * {@code PreloadCollectAuto}. They go around the HIVE through the lane west of it and shoot
 * from {@link NearRoutePoses#OTHER_SPOT} north of the HIVE, then park.
 * <p>
 * The other-cell spot is 15 cm short of the exact mirror to clear the back-wall FLOWER (FITTED in
 * the Pedro visualizer).
 */
public class OtherCellPaths extends RoutePaths {

    /**
     * @param alliance alliance to build the paths for
     */
    public OtherCellPaths(Alliance alliance) {
        super(alliance);
    }

    /** @return waypoints from the end of the ball row to the other cell, for the odometry driver */
    public List<Pose> getFromCollectWaypoints() {
        return Arrays.asList(toField(NearRoutePoses.COLLECT_EXIT), toField(NearRoutePoses.NORTH_WAYPOINT),
                toField(NearRoutePoses.OTHER_SPOT));
    }

    /** @return waypoints from the shooting spot to the other cell, for the odometry driver */
    public List<Pose> getFromShootSpotWaypoints() {
        return Arrays.asList(toField(NearRoutePoses.SOUTH_WAYPOINT_TURNED),
                toField(NearRoutePoses.NORTH_WAYPOINT), toField(NearRoutePoses.OTHER_SPOT));
    }

    /**
     * @return from the end of the ball row: away from the red wall (still facing it), turn north
     *         on the way to the north-west waypoint, then to the other-cell spot
     */
    public Path fromCollect() {
        return chain(
                lineConstant(NearRoutePoses.COLLECT_END, NearRoutePoses.COLLECT_EXIT, NearRoutePoses.COLLECT_EXIT),
                lineLinear(NearRoutePoses.COLLECT_EXIT, NearRoutePoses.NORTH_WAYPOINT),
                lineConstant(NearRoutePoses.NORTH_WAYPOINT, NearRoutePoses.OTHER_SPOT, NearRoutePoses.OTHER_SPOT));
    }

    /**
     * Straight copy of {@link #fromCollect()}: one diagonal from the end of the row to the spot.
     * <p>
     * DO NOT USE IN COMPETITION: it cuts through the HIVE box and turns against the red wall.
     *
     * @return the straight copy
     * @deprecated cuts through the HIVE box; kept only for comparison
     */
    @Deprecated
    public Path fromCollectStraight() {
        return lineLinear(NearRoutePoses.COLLECT_END, NearRoutePoses.OTHER_SPOT);
    }

    /**
     * @return from the shooting spot: to the south-west waypoint turning 180 degrees, up the
     *         west lane, then to the other-cell spot
     */
    public Path fromShootSpot() {
        return chain(
                lineLinear(NearRoutePoses.SHOOT_SPOT, NearRoutePoses.SOUTH_WAYPOINT_TURNED),
                lineConstant(NearRoutePoses.SOUTH_WAYPOINT_TURNED, NearRoutePoses.NORTH_WAYPOINT,
                        NearRoutePoses.NORTH_WAYPOINT),
                lineConstant(NearRoutePoses.NORTH_WAYPOINT, NearRoutePoses.OTHER_SPOT, NearRoutePoses.OTHER_SPOT));
    }

    /**
     * Straight copy of {@link #fromShootSpot()}: straight north through x = 57.
     * <p>
     * DO NOT USE IN COMPETITION: it drives straight through the HIVE.
     *
     * @return the straight copy
     * @deprecated cuts through the HIVE box; kept only for comparison
     */
    @Deprecated
    public Path fromShootSpotStraight() {
        return lineLinear(NearRoutePoses.SHOOT_SPOT, NearRoutePoses.OTHER_SPOT);
    }

    /** @return from the other-cell spot to PARK in one diagonal north of the HIVE */
    public Path toPark() {
        return lineConstant(NearRoutePoses.OTHER_SPOT, NearRoutePoses.OTHER_PARK, NearRoutePoses.OTHER_PARK);
    }

    /** @return straight copy of {@link #toPark()} (already one line) */
    public Path toParkStraight() {
        return lineConstant(NearRoutePoses.OTHER_SPOT, NearRoutePoses.OTHER_PARK, NearRoutePoses.OTHER_PARK);
    }
}
