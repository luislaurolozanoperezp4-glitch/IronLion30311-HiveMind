// Sources: new for IronLion30311-HiveMind
package org.firstinspires.ftc.teamcode.config;

import static org.junit.Assert.assertTrue;

import com.pedropathing.math.Pose;

import org.firstinspires.ftc.teamcode.testutil.RuleGeometry;

import org.junit.Test;

/** Sanity checks that the RED field poses respect the AUTO territory rule (G402). */
public class FieldConstantsTest {

    private static final double HALF_ROBOT_WIDTH = RobotConstants.ROBOT_WIDTH_INCHES / 2.0;

    private static void assertOnRedSide(String name, Pose pose) {
        assertTrue(name + " must stay on the red half (x + half robot < territory line)",
                pose.x() + HALF_ROBOT_WIDTH <= FieldConstants.TERRITORY_LINE_X);
    }

    @Test
    public void redPosesStayOnRedHalf() {
        assertOnRedSide("NEAR_START", FieldConstants.NEAR_START);
        assertOnRedSide("FAR_START", FieldConstants.FAR_START);
        assertOnRedSide("NEAR_SHOOT", FieldConstants.NEAR_SHOOT);
        assertOnRedSide("OTHER_SHOOT", FieldConstants.OTHER_SHOOT);
        assertOnRedSide("RED_PARK", FieldConstants.RED_PARK);
    }

    @Test
    public void parkIsPartlyInRedLoadingZoneAndOffTheWall() {
        Pose park = FieldConstants.RED_PARK;
        assertTrue("PARK: partly in the zone", RuleGeometry.overlapsRedLoadingZone(park));
        assertTrue("LEAVE: off the wall", RuleGeometry.wallGap(park) >= FieldConstants.WALL_CLEARANCE_INCHES - 1e-9);
    }

    /** Manual TU10 9.3: LOADING ZONE ~23 in wide x 11 in deep, against the red wall. */
    @Test
    public void loadingZoneMatchesTheManual() {
        assertTrue(Math.abs(FieldConstants.RED_LOADING_ZONE_MAX_X - FieldConstants.RED_LOADING_ZONE_MIN_X - 11.0) < 1e-9);
        assertTrue(Math.abs(FieldConstants.RED_LOADING_ZONE_MAX_Y - FieldConstants.RED_LOADING_ZONE_MIN_Y - 23.0) < 1e-9);
        assertTrue(FieldConstants.RED_LOADING_ZONE_MIN_X == 0.0);
        assertTrue("Red side is columns A-C", FieldConstants.TERRITORY_LINE_X == 3 * FieldConstants.TILE_INCHES);
    }
}
