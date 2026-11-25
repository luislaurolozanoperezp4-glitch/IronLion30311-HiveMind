// Sources: new for IronLion30311-HiveMind (hybrid drive switch)
package org.firstinspires.ftc.teamcode.pathing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.pedropathing.math.Pose;

import org.firstinspires.ftc.teamcode.config.HybridDriveConstants;
import org.firstinspires.ftc.teamcode.pathing.paths.NearRoutePoses;
import org.junit.After;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class HybridDriveTest {

    @After
    public void restoreDefaults() {
        HybridDriveConstants.IS_PEDRO_TUNED = false;
        HybridDriveConstants.PEDRO_PRELOAD_PARK = false;
    }

    @Test
    public void usesOdometryDriverEverywhereByDefault() {
        for (DriveRoute route : DriveRoute.values()) {
            assertFalse(route + " must start on the odometry driver", HybridDrive.usesPedro(route));
        }
    }

    @Test
    public void ignoresRouteSwitchUntilPedroIsTuned() {
        HybridDriveConstants.PEDRO_PRELOAD_PARK = true;
        assertFalse(HybridDrive.usesPedro(DriveRoute.PRELOAD_PARK));
        HybridDriveConstants.IS_PEDRO_TUNED = true;
        assertTrue(HybridDrive.usesPedro(DriveRoute.PRELOAD_PARK));
        assertFalse(HybridDrive.usesPedro(DriveRoute.CYCLE_PARK));
    }

    @Test
    public void buildsOneLegPerWaypointAndSkipsZeroLengthLegs() {
        Pose start = NearRoutePoses.SHOOT_SPOT;
        List<Pose> waypoints = Arrays.asList(NearRoutePoses.BACK_OFF_2, NearRoutePoses.BACK_OFF_2,
                NearRoutePoses.WEST_LANE_SOUTH, NearRoutePoses.PRELOAD_LANE_NORTH, NearRoutePoses.PRELOAD_PARK);
        List<Pose[]> legs = HybridDrive.getLegs(start, waypoints);
        assertEquals(4, legs.size());
        Pose[] last = legs.get(legs.size() - 1);
        assertEquals(NearRoutePoses.PRELOAD_PARK.x(), last[1].x(), 1e-9);
        assertEquals(NearRoutePoses.PRELOAD_PARK.y(), last[1].y(), 1e-9);
        assertNotNull(HybridDrive.buildPath(start, waypoints));
    }

    @Test
    public void returnsNoPathWhenAlreadyThere() {
        Pose here = NearRoutePoses.PRELOAD_PARK;
        assertNull(HybridDrive.buildPath(here, Collections.singletonList(here)));
    }

    @Test
    public void givesLongerRoutesMoreTime() {
        Pose start = NearRoutePoses.SHOOT_SPOT;
        double shortRoute = HybridDrive.getTimeoutSeconds(start, Collections.singletonList(NearRoutePoses.BACK_OFF_2));
        double longRoute = HybridDrive.getTimeoutSeconds(start,
                Arrays.asList(NearRoutePoses.WEST_LANE_SOUTH, NearRoutePoses.PRELOAD_LANE_NORTH));
        assertTrue(longRoute > shortRoute);
        assertTrue(shortRoute >= HybridDriveConstants.PEDRO_TIMEOUT_BASE_SECONDS);
    }
}
