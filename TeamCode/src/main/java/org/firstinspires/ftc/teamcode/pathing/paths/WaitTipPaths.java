// Sources: IronLion30311BIOBUZZInicioDeTemporada / branch feature/turrubiates/vision-align / TeamCode/.../opmodes/auto/FarSideWaitTipAuto.java @ f26f8d0 (buildPoses: start, leaveSpot, parkSpot) — adapted for BIOBUZZ (144 in field, FLOWER keep-out, park 2 in off the wall)
package org.firstinspires.ftc.teamcode.pathing.paths;

import com.pedropathing.math.Pose;
import com.pedropathing.paths.Path;

import org.firstinspires.ftc.teamcode.config.AutoConstants;
import org.firstinspires.ftc.teamcode.config.FieldConstants;
import org.firstinspires.ftc.teamcode.config.RobotConstants;
import org.firstinspires.ftc.teamcode.util.Alliance;

/**
 * "Wait TIP" (vision-align {@code FarSideWaitTipAuto}, UNTESTED): from the back wall next to the
 * back-wall FLOWER, back off 10 cm (LEAVE), wait for the partner's TIP, shoot, then one diagonal
 * into the UPPER part of the red LOADING ZONE without turning (our partner parks at the low edge).
 * <p>
 * Poses (RED, inches, degrees):
 * <pre>
 * START   (61.5, 135.0,  90)  front against the back wall, launchers toward the HIVE
 * LEAVE   (61.5, 131.1,  90)  10 cm back
 * PARK    (11.1, 121.0,  90)  robot y 112-130 overlaps the zone (96.5-119.5) by 7.5 in, 2.1 in off the wall
 * </pre>
 * Differences from the source: the source start (59.36, 132.06) assumed a 141.5 in field and left
 * ~0.3 in to the FLOWER; here the robot touches the 144 in back wall (decision D19) and keeps the
 * FLOWER keep-out radius, and the PARK is 2.1 in off the red wall (team decision) instead of 0.5.
 */
public class WaitTipPaths extends RoutePaths {

    private static final double CM_PER_INCH = 2.54;

    /** Start: front (intake) against the back wall, right of the back-wall FLOWER. */
    public static final Pose START = pose(AutoConstants.WAIT_TIP_START_X, FieldConstants.FIELD_SIZE_INCHES
            - RobotConstants.ROBOT_LENGTH_INCHES / 2.0, AutoConstants.WAIT_TIP_HEADING_DEG);
    /** LEAVE: {@code LEAVE_BACK_CM} back (toward the HIVE), same heading. */
    public static final Pose LEAVE = pose(START.x(), START.y() - AutoConstants.LEAVE_BACK_CM / CM_PER_INCH,
            AutoConstants.WAIT_TIP_HEADING_DEG);
    /** PARK in the upper part of the LOADING ZONE, 2 in off the red wall, no turn. */
    public static final Pose PARK = pose(NearRoutePoses.PARK.x(), AutoConstants.WAIT_TIP_PARK_Y,
            AutoConstants.WAIT_TIP_HEADING_DEG);

    /**
     * @param alliance alliance to build the paths for
     */
    public WaitTipPaths(Alliance alliance) {
        super(alliance);
    }

    /** @return LEAVE: 10 cm straight back off the back wall */
    public Path leaveWall() {
        return lineConstant(START, LEAVE, START);
    }

    /** @return straight copy of {@link #leaveWall()} (already one line) */
    public Path leaveWallStraight() {
        return lineConstant(START, LEAVE, START);
    }

    /** @return one diagonal into the upper LOADING ZONE, north of the HIVE, no turn */
    public Path park() {
        return lineConstant(LEAVE, PARK, PARK);
    }

    /** @return straight copy of {@link #park()} (already one line) */
    public Path parkStraight() {
        return lineConstant(LEAVE, PARK, PARK);
    }
}
