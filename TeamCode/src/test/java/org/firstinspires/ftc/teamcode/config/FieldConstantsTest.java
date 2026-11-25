// Sources: new for IronLion30311-HiveMind
package org.firstinspires.ftc.teamcode.config;

import static org.junit.Assert.assertTrue;

import com.pedropathing.math.Pose;

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
    public void parkIsInsideRedLoadingZone() {
        Pose park = FieldConstants.RED_PARK;
        assertTrue(park.x() > FieldConstants.RED_LOADING_ZONE_MIN_X
                && park.x() < FieldConstants.RED_LOADING_ZONE_MAX_X);
        assertTrue(park.y() > FieldConstants.RED_LOADING_ZONE_MIN_Y
                && park.y() < FieldConstants.RED_LOADING_ZONE_MAX_Y);
    }
}
