// Sources: IronLion30311BIOBUZZInicioDeTemporada / branch feature/turrubiates/vision-align / TeamCode/.../opmodes/auto/PreloadRouteAuto.java @ a7b8f4c (runCollectRoutine, returnAndCheckTip, parkFromShootSide; PreloadCollectAuto.isCollecting) — adapted for BIOBUZZ
package org.firstinspires.ftc.teamcode.pathing.paths;

import com.pedropathing.math.Pose;
import com.pedropathing.paths.Path;

import org.firstinspires.ftc.teamcode.util.Alliance;

import java.util.Arrays;
import java.util.List;

/**
 * "Preload + Collect + Park", the shooting-side part (old name {@code PreloadCollectAuto}).
 * Tuned from logged runs on 2026-11-23/24, no full-success run yet.
 * <p>
 * After the preload volley at {@link NearRoutePoses#SHOOT_SPOT} the robot turns 90 degrees
 * clockwise (front to the red wall), drives 60 cm, then collects the ball row slowly to 120 cm,
 * drives back, turns back and shoots again; then it parks through the south-west waypoint.
 * The routes to the other HIVE cell are in {@link OtherCellPaths}.
 * <p>
 * The in-place turns at the shooting spot are not paths (use {@code OdometryDriver.turnTo}).
 */
public class PreloadCollectPaths extends RoutePaths {

    /**
     * @param alliance alliance to build the paths for
     */
    public PreloadCollectPaths(Alliance alliance) {
        super(alliance);
    }

    /** @return the waypoints of the collect row (pass-through, then the slow intake leg) */
    public List<Pose> getCollectWaypoints() {
        return Arrays.asList(toField(NearRoutePoses.COLLECT_START), toField(NearRoutePoses.COLLECT_END));
    }

    /** @return the waypoints of the park from the shooting side (pass-through, then PARK) */
    public List<Pose> getParkWaypoints() {
        return Arrays.asList(toField(NearRoutePoses.SOUTH_WAYPOINT), toField(NearRoutePoses.PARK));
    }

    /**
     * @return the collect row: to the balls (intake off), then over them (intake on, slow);
     *         heading 180 degrees (front to the red wall)
     */
    public Path collectRow() {
        return chain(
                lineConstant(NearRoutePoses.SHOOT_SPOT_FACING_COLLECT, NearRoutePoses.COLLECT_START,
                        NearRoutePoses.COLLECT_START),
                lineConstant(NearRoutePoses.COLLECT_START, NearRoutePoses.COLLECT_END,
                        NearRoutePoses.COLLECT_END));
    }

    /** @return straight copy of {@link #collectRow()} (the two legs are collinear: same geometry) */
    public Path collectRowStraight() {
        return lineConstant(NearRoutePoses.SHOOT_SPOT_FACING_COLLECT, NearRoutePoses.COLLECT_END,
                NearRoutePoses.COLLECT_END);
    }

    /** @return back from the end of the row to the shooting spot, still facing the red wall */
    public Path returnToShootSpot() {
        return lineConstant(NearRoutePoses.COLLECT_END, NearRoutePoses.SHOOT_SPOT_FACING_COLLECT,
                NearRoutePoses.SHOOT_SPOT_FACING_COLLECT);
    }

    /** @return straight copy of {@link #returnToShootSpot()} (already one line) */
    public Path returnToShootSpotStraight() {
        return lineConstant(NearRoutePoses.COLLECT_END, NearRoutePoses.SHOOT_SPOT_FACING_COLLECT,
                NearRoutePoses.SHOOT_SPOT_FACING_COLLECT);
    }

    /** @return park from the shooting spot: diagonal to the south-west waypoint, then to PARK */
    public Path parkFromShootSide() {
        return chain(
                lineConstant(NearRoutePoses.SHOOT_SPOT, NearRoutePoses.SOUTH_WAYPOINT,
                        NearRoutePoses.SOUTH_WAYPOINT),
                lineConstant(NearRoutePoses.SOUTH_WAYPOINT, NearRoutePoses.PARK, NearRoutePoses.PARK));
    }

    /**
     * Straight copy of {@link #parkFromShootSide()}: one diagonal from the shooting spot to PARK.
     * <p>
     * DO NOT USE IN COMPETITION: the robot's corner enters the HIVE frame box (G417 risk).
     *
     * @return the straight copy
     * @deprecated cuts through the HIVE box; kept only for comparison
     */
    @Deprecated
    public Path parkFromShootSideStraight() {
        return lineConstant(NearRoutePoses.SHOOT_SPOT, NearRoutePoses.PARK, NearRoutePoses.PARK);
    }
}
