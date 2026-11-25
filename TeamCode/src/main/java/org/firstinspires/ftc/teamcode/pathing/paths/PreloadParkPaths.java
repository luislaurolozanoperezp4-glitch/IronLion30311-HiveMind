// Sources: IronLion30311BIOBUZZInicioDeTemporada / branch feature/turrubiates/vision-align / TeamCode/.../opmodes/auto/PreloadRouteAuto.java @ a7b8f4c (runRoutine park legs: shootSpot, backOff2, right1, longBack, right2) — adapted for BIOBUZZ
package org.firstinspires.ftc.teamcode.pathing.paths;

import com.pedropathing.math.Pose;
import com.pedropathing.paths.Path;

import org.firstinspires.ftc.teamcode.util.Alliance;

import java.util.Arrays;
import java.util.List;

/**
 * "Preload + Park": the TESTED primary route (4/4 preload shots and parked in ~12-14 s,
 * vision-align, 2026-11-23). Old name: {@code PreloadRouteAuto} ("Preload + Park (Odometry)").
 * <p>
 * LEAVE 10 cm and shoot at {@link NearRoutePoses#SHOOT_SPOT}, then park through the lane west of
 * the HIVE: back 5 cm, right 60 cm, back 230 cm, right 56.5 cm ({@code AutoConstants.RIGHT_2_CM};
 * the tested run used 60 cm, shortened so the robot ends 2 in off the wall for LEAVE).
 * The heading stays at -90 degrees
 * the whole way (the robot slides sideways and backwards, never turns).
 * <p>
 * On the field this route is driven leg by leg with {@code pathing.OdometryDriver} (use
 * {@link #getParkWaypoints()}). The Pedro paths below are the same legs for when the follower is
 * tuned (decision D6).
 */
public class PreloadParkPaths extends RoutePaths {

    /**
     * @param alliance alliance to build the paths for
     */
    public PreloadParkPaths(Alliance alliance) {
        super(alliance);
    }

    /** @return the start pose for this alliance */
    public Pose getStartPose() {
        return toField(NearRoutePoses.START);
    }

    /** @return the waypoints of the LEAVE leg, for the odometry driver */
    public List<Pose> getLeaveWaypoints() {
        return Arrays.asList(toField(NearRoutePoses.SHOOT_SPOT));
    }

    /** @return the waypoints of the park legs in order, for the odometry driver */
    public List<Pose> getParkWaypoints() {
        return Arrays.asList(toField(NearRoutePoses.BACK_OFF_2), toField(NearRoutePoses.WEST_LANE_SOUTH),
                toField(NearRoutePoses.PRELOAD_LANE_NORTH), toField(NearRoutePoses.PRELOAD_PARK));
    }

    /** @return LEAVE: start to the shooting spot, 10 cm off the wall */
    public Path leaveWall() {
        return lineConstant(NearRoutePoses.START, NearRoutePoses.SHOOT_SPOT, NearRoutePoses.SHOOT_SPOT);
    }

    /** @return straight copy of {@link #leaveWall()} (already one line: same geometry) */
    public Path leaveWallStraight() {
        return lineConstant(NearRoutePoses.START, NearRoutePoses.SHOOT_SPOT, NearRoutePoses.SHOOT_SPOT);
    }

    /** @return the four park legs as one chain, shooting spot to the LOADING ZONE */
    public Path park() {
        return chain(
                lineConstant(NearRoutePoses.SHOOT_SPOT, NearRoutePoses.BACK_OFF_2, NearRoutePoses.BACK_OFF_2),
                lineConstant(NearRoutePoses.BACK_OFF_2, NearRoutePoses.WEST_LANE_SOUTH,
                        NearRoutePoses.WEST_LANE_SOUTH),
                lineConstant(NearRoutePoses.WEST_LANE_SOUTH, NearRoutePoses.PRELOAD_LANE_NORTH,
                        NearRoutePoses.PRELOAD_LANE_NORTH),
                lineConstant(NearRoutePoses.PRELOAD_LANE_NORTH, NearRoutePoses.PRELOAD_PARK,
                        NearRoutePoses.PRELOAD_PARK));
    }

    /**
     * Straight copy of {@link #park()}: one diagonal from the shooting spot to PARK.
     * <p>
     * DO NOT USE IN COMPETITION: the robot's corner enters the HIVE frame box (G417 risk).
     *
     * @return the straight copy
     * @deprecated cuts through the HIVE box; kept only for comparison
     */
    @Deprecated
    public Path parkStraight() {
        return lineConstant(NearRoutePoses.SHOOT_SPOT, NearRoutePoses.PRELOAD_PARK, NearRoutePoses.PRELOAD_PARK);
    }
}
