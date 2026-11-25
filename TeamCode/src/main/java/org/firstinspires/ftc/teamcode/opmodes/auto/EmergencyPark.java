// Sources: IronLion30311BIOBUZZInicioDeTemporada / branch feature/turrubiates/vision-align / TeamCode/.../opmodes/auto/PreloadRouteAuto.java @ a7b8f4c (west-lane park legs right1, longBack, right2; collectExit "do not turn at the wall")
//          — adapted for BIOBUZZ (park from any pose, used when time runs out mid-step; PARK heading squared to the walls since the angled other-cell heading of vision-align 4fd2293)
package org.firstinspires.ftc.teamcode.opmodes.auto;

import com.pedropathing.math.Pose;

import org.firstinspires.ftc.teamcode.config.AutoConstants;
import org.firstinspires.ftc.teamcode.config.FieldConstants;
import org.firstinspires.ftc.teamcode.pathing.AutoPlanner;
import org.firstinspires.ftc.teamcode.pathing.paths.NearRoutePoses;
import org.firstinspires.ftc.teamcode.util.Alliance;

import java.util.ArrayList;
import java.util.List;

/**
 * Plans a park from wherever the robot is, for when the routine must stop a step and go park
 * now (time is short). It reuses the TESTED west lane (x 33.4, west of the HIVE):
 * <ul>
 *     <li>North of the HIVE: straight to PARK (the tested other-cell park diagonal).</li>
 *     <li>Otherwise: sideways into the west lane, up the lane, then into PARK.</li>
 * </ul>
 * There is no in-place turn, so a robot stopped next to a wall cannot sweep its corners into it.
 * The heading is kept on the way and only squared to the nearest wall direction on the last leg
 * (an angled 18 in robot, for example at the 115 deg other-cell heading, would touch the red wall
 * at PARK and lose LEAVE). Pure logic: tested by {@code AutoSafetyTest}.
 */
public final class EmergencyPark {

    private EmergencyPark() {
    }

    /**
     * @param fieldPose robot pose on the real field
     * @param alliance  our alliance
     * @return field waypoints to drive through in order; the last one is PARK
     */
    public static List<Pose> plan(Pose fieldPose, Alliance alliance) {
        Pose red = alliance.toField(fieldPose);
        double heading = red.heading();
        double laneX = NearRoutePoses.WEST_LANE_SOUTH.x();
        double laneSouthY = NearRoutePoses.WEST_LANE_SOUTH.y();
        double laneNorthY = NearRoutePoses.WEST_LANE_NORTH.y();
        List<Pose> redWaypoints = new ArrayList<>();
        boolean isNorthOfHive = red.y() >= FieldConstants.HIVE_MAX_Y;
        if (!isNorthOfHive) {
            if (Math.abs(red.x() - laneX) > AutoConstants.EMERGENCY_PARK_LANE_TOLERANCE_INCHES) {
                double entryY = Math.max(laneSouthY, Math.min(laneNorthY, red.y()));
                redWaypoints.add(new Pose(laneX, entryY, heading));
            }
            redWaypoints.add(new Pose(laneX, laneNorthY, heading));
        }
        redWaypoints.add(new Pose(NearRoutePoses.PARK.x(), NearRoutePoses.PARK.y(), squareHeading(heading)));

        List<Pose> fieldWaypoints = new ArrayList<>();
        for (Pose pose : redWaypoints) {
            fieldWaypoints.add(alliance.toField(pose));
        }
        return fieldWaypoints;
    }

    /**
     * @param headingRadians any heading
     * @return the nearest of 0, 90, 180 and 270 degrees (radians): robot sides parallel to the walls
     */
    static double squareHeading(double headingRadians) {
        double quarterTurn = Math.PI / 2.0;
        return Math.round(headingRadians / quarterTurn) * quarterTurn;
    }

    /**
     * @param fieldPose robot pose on the real field
     * @param alliance  our alliance
     * @return estimated seconds to park from here with {@link #plan}
     */
    public static double estimateSeconds(Pose fieldPose, Alliance alliance) {
        List<Pose> legs = new ArrayList<>();
        legs.add(fieldPose);
        legs.addAll(plan(fieldPose, alliance));
        return AutoPlanner.estimateLegsSeconds(legs);
    }
}
