// Sources: new for IronLion30311-HiveMind (BIOBUZZ Competition Manual TU10: G304 start rules, LEAVE / PARK, R102 size; wait-TIP route from vision-align @ f26f8d0)
package org.firstinspires.ftc.teamcode.opmodes.auto;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.pedropathing.math.Pose;

import org.firstinspires.ftc.teamcode.config.AutoConstants;
import org.firstinspires.ftc.teamcode.config.FieldConstants;
import org.firstinspires.ftc.teamcode.config.RobotConstants;
import org.firstinspires.ftc.teamcode.pathing.paths.NearRoutePoses;
import org.firstinspires.ftc.teamcode.pathing.paths.WaitTipPaths;
import org.firstinspires.ftc.teamcode.testutil.RuleGeometry;
import org.firstinspires.ftc.teamcode.util.Alliance;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

/**
 * G304 (manual TU10): every start pose the robot can be set up in is fully on its own half,
 * touching the perimeter wall, outside the LOADING ZONE, not touching a FLOWER, and the robot
 * fits the 18 in STARTING CONFIGURATION (R102).
 */
public class StartPositionRulesTest {

    /** A start pose this close to a wall counts as touching it (setup tolerance). */
    private static final double TOUCH_TOLERANCE_INCHES = 0.5;

    private static final List<Pose> FLOWERS = Arrays.asList(FieldConstants.AUDIENCE_WALL_FLOWER,
            FieldConstants.BACK_WALL_FLOWER, FieldConstants.RED_WALL_FLOWER, FieldConstants.BLUE_WALL_FLOWER);

    @Test
    public void robotFitsTheStartingConfiguration() {
        assertTrue("R102: 18 in long", RobotConstants.ROBOT_LENGTH_INCHES <= 18.0);
        assertTrue("R102: 18 in wide", RobotConstants.ROBOT_WIDTH_INCHES <= 18.0);
    }

    @Test
    public void everyStartPoseIsLegal() {
        for (StartPosition start : StartPosition.values()) {
            Pose red = start.getRedPose();
            String name = start.name() + " " + red;
            assertTrue(name + ": G304.A own half (columns A-C)", RuleGeometry.isOnRedHalf(red));
            assertTrue(name + ": inside the FIELD", RuleGeometry.wallGap(red) >= -TOUCH_TOLERANCE_INCHES);
            assertTrue(name + ": G304.C touching the wall", RuleGeometry.wallGap(red) <= TOUCH_TOLERANCE_INCHES);
            assertFalse(name + ": G304.E not in the LOADING ZONE", RuleGeometry.overlapsRedLoadingZone(red));
            for (Pose flower : FLOWERS) {
                assertFalse(name + ": G304.D not touching the FLOWER at " + flower,
                        RuleGeometry.touchesFlower(red, flower));
            }
        }
    }

    @Test
    public void fieldConstantStartsAreTheStartPositions() {
        assertTrue(StartPosition.NEAR.getRedPose() == FieldConstants.NEAR_START);
        assertTrue(StartPosition.FAR.getRedPose() == FieldConstants.FAR_START);
    }

    @Test
    public void blueStartsAreLegalForBlue() {
        // BLUE = RED rotated 180 degrees (D13): the blue half is x > 72, the blue zone is rotated too.
        for (StartPosition start : StartPosition.values()) {
            Pose blue = Alliance.BLUE.toField(start.getRedPose());
            assertTrue(start + " BLUE on the blue half",
                    blue.x() - RuleGeometry.halfX(blue) >= FieldConstants.TERRITORY_LINE_X);
            assertFalse(start + " BLUE not in the blue LOADING ZONE",
                    RuleGeometry.overlapsRedLoadingZone(Alliance.rotate180(blue)));
        }
    }

    // ---------------- LEAVE + PARK at the end of AUTO ----------------

    @Test
    public void waitTipRouteKeepsTheRulesAndLeavesRoomForThePartner() {
        // vision-align FarSideWaitTipAuto @ f26f8d0: back-wall start, park in the upper zone
        assertTrue("touches the back wall (G304.C)", RuleGeometry.wallGap(WaitTipPaths.START) < 1e-9);
        assertParkWithLeave("WaitTipPaths.PARK", WaitTipPaths.PARK);
        double halfLength = RobotConstants.ROBOT_LENGTH_INCHES / 2.0;
        assertTrue("our near park (low edge) and the wait-TIP park (top) do not overlap",
                WaitTipPaths.PARK.y() - halfLength >= NearRoutePoses.PARK.y() + halfLength);
        // The limited aim turn keeps the corners on our side of the territory line (G402)
        double maxTurn = Math.toRadians(AutoConstants.WAIT_TIP_AIM_MAX_TURN_DEGREES);
        for (double turn : new double[]{-maxTurn, maxTurn}) {
            Pose aimed = new Pose(WaitTipPaths.LEAVE.x(), WaitTipPaths.LEAVE.y(), WaitTipPaths.LEAVE.heading() + turn);
            assertFalse(TerritoryGuard.isPastLine(aimed, Alliance.RED));
        }
    }

    @Test
    public void safeParkFromBesideTheZoneLeavesAndParks() {
        Pose leave = AutoSafePark.loadingZoneLeavePose();
        assertTrue("LEAVE: off the wall", RuleGeometry.wallGap(leave) >= FieldConstants.WALL_CLEARANCE_INCHES);
        Pose park = AutoSafePark.loadingZoneParkPose();
        assertParkWithLeave("Safe Park from beside the zone", park);
    }

    /**
     * PARK (10.5.4): at least partly in the LOADING ZONE. LEAVE (assessed at the end of AUTO):
     * not touching the wall; team decision: at least WALL_CLEARANCE_INCHES off it.
     */
    public static void assertParkWithLeave(String name, Pose red) {
        assertTrue(name + " PARK: partly in the LOADING ZONE " + red, RuleGeometry.overlapsRedLoadingZone(red));
        assertTrue(name + " LEAVE: >= " + FieldConstants.WALL_CLEARANCE_INCHES + " in off the wall " + red,
                RuleGeometry.wallGap(red) >= FieldConstants.WALL_CLEARANCE_INCHES - 1e-6);
    }
}
