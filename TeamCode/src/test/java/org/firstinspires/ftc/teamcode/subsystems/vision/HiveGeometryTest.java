// Sources: IronLion30311BIOBUZZInicioDeTemporada / branch feature/turrubiates/vision-align / TeamCode/src/test/.../subsystems/vision/HiveGeometryTest.java @ a7b8f4c (all tests) — adapted for BIOBUZZ (util.Alliance)
package org.firstinspires.ftc.teamcode.subsystems.vision;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class HiveGeometryTest {

    private static final double TOLERANCE = 1e-6;

    private static double[][] emptyTags() {
        return new double[HiveCell.MAX_TAG_ID + 1][];
    }

    @Test
    public void orientationIsAveragedPerCluster() {
        double[][] tags = emptyTags();
        // {strafe, forward, yaw, is3D, roll, imageUp, rawYaw}
        tags[31] = new double[]{-0.06, 1.0, 0.0, 1.0, 170.0, -80.0, 5.0};
        tags[32] = new double[]{0.06, 1.0, 0.0, 1.0, -170.0, -100.0, -5.0};

        HiveClusterPose pose = HiveGeometry.reconstructRawCellPose(HiveCell.RED_SCORING, tags);

        assertNotNull(pose);
        assertEquals(180.0, Math.abs(pose.rollDeg), TOLERANCE);
        assertEquals(-90.0, pose.imageUpDeg, TOLERANCE);
        assertEquals(0.0, pose.rawYawDeg, TOLERANCE);
        assertEquals(2, pose.visibleTagCount);
    }

    @Test
    public void legacyFourElementArraysGiveUnknownOrientation() {
        double[][] tags = emptyTags();
        tags[34] = new double[]{0.0, 1.2, 0.0, 0.0};

        HiveClusterPose pose = HiveGeometry.reconstructRawCellPose(HiveCell.RED_AUDIENCE, tags);

        assertNotNull(pose);
        assertTrue(Double.isNaN(pose.rollDeg));
        assertTrue(Double.isNaN(pose.imageUpDeg));
        assertTrue(Double.isNaN(pose.rawYawDeg));
    }

    @Test
    public void unknownOrientationOnOneTagDoesNotHideTheOther() {
        double[][] tags = emptyTags();
        tags[38] = new double[]{0.0, 1.0, 0.0, 0.0, Double.NaN, Double.NaN, Double.NaN};
        tags[39] = new double[]{0.11, 1.0, 0.0, 0.0, Double.NaN, 95.0, Double.NaN};

        HiveClusterPose pose = HiveGeometry.reconstructRawCellPose(HiveCell.BLUE_AUDIENCE, tags);

        assertNotNull(pose);
        assertEquals(95.0, pose.imageUpDeg, TOLERANCE);
        assertTrue(Double.isNaN(pose.rollDeg));
    }
}
