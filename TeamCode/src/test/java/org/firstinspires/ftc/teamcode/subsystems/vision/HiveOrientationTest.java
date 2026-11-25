// Sources: IronLion30311BIOBUZZInicioDeTemporada / branch feature/turrubiates/vision-align / TeamCode/src/test/.../subsystems/vision/HiveOrientationTest.java @ a7b8f4c (all tests) — adapted for BIOBUZZ (util.Alliance)
package org.firstinspires.ftc.teamcode.subsystems.vision;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class HiveOrientationTest {

    private static final double TOLERANCE_DEG = 1e-6;

    @Test
    public void wrapDeg_mapsAnglesIntoHalfOpenRange() {
        assertEquals(0.0, HiveOrientation.wrapDeg(360.0), TOLERANCE_DEG);
        assertEquals(-180.0, HiveOrientation.wrapDeg(180.0), TOLERANCE_DEG);
        assertEquals(-170.0, HiveOrientation.wrapDeg(190.0), TOLERANCE_DEG);
        assertEquals(170.0, HiveOrientation.wrapDeg(-190.0), TOLERANCE_DEG);
        assertEquals(-90.0, HiveOrientation.wrapDeg(-450.0), TOLERANCE_DEG);
    }

    @Test
    public void circularMeanDeg_staysCorrectAcrossTheSeam() {
        // Arithmetic mean of 170 and -170 would be 0 (wrong); the circular mean is 180.
        double mean = HiveOrientation.circularMeanDeg(170.0, -170.0);
        assertEquals(180.0, Math.abs(mean), TOLERANCE_DEG);
    }

    @Test
    public void circularMeanDeg_ignoresNaN() {
        assertEquals(10.0, HiveOrientation.circularMeanDeg(Double.NaN, 10.0), TOLERANCE_DEG);
    }

    @Test
    public void circularMeanDeg_returnsNaNWhenEmptyOrCancelling() {
        assertTrue(Double.isNaN(HiveOrientation.circularMeanDeg()));
        assertTrue(Double.isNaN(HiveOrientation.circularMeanDeg(Double.NaN)));
        assertTrue(Double.isNaN(HiveOrientation.circularMeanDeg(0.0, 180.0)));
    }

    @Test
    public void cornerVectorAngleDeg_usesImageAxes() {
        // Image y grows downward: a vector pointing up in the image has a negative dy.
        assertEquals(-90.0, HiveOrientation.cornerVectorAngleDeg(10, 20, 10, 5), TOLERANCE_DEG);
        assertEquals(0.0, HiveOrientation.cornerVectorAngleDeg(0, 0, 5, 0), TOLERANCE_DEG);
        assertTrue(Double.isNaN(HiveOrientation.cornerVectorAngleDeg(3, 3, 3, 3)));
    }

    @Test
    public void isUpright_usesNinetyDegreeWindowAroundReference() {
        double reference = -90.0;
        assertTrue(HiveOrientation.isUpright(-90.0, reference));
        assertTrue(HiveOrientation.isUpright(-10.0, reference));
        assertTrue(HiveOrientation.isUpright(-170.0, reference));
        assertFalse(HiveOrientation.isUpright(90.0, reference));
        assertFalse(HiveOrientation.isUpright(5.0, reference));
        assertFalse(HiveOrientation.isUpright(175.0, reference));
    }

    @Test
    public void wrapDeg_neverReturnsPlus180() {
        assertEquals(-180.0, HiveOrientation.wrapDeg(-180.00000000000003), TOLERANCE_DEG);
        assertTrue(HiveOrientation.wrapDeg(-180.00000000000003) < 180.0);
        assertTrue(Double.isNaN(HiveOrientation.wrapDeg(Double.NaN)));
    }

    @Test
    public void circularMeanDeg_rejectsTagsThatDisagree() {
        // 0 and 170 degrees point almost opposite ways: no trustworthy cluster orientation.
        assertTrue(Double.isNaN(HiveOrientation.circularMeanDeg(0.0, 170.0)));
        // 0 and 60 degrees agree well enough.
        assertEquals(30.0, HiveOrientation.circularMeanDeg(0.0, 60.0), TOLERANCE_DEG);
    }

    @Test
    public void isUpright_windowBoundaryIsExclusive() {
        assertFalse(HiveOrientation.isUpright(90.0, 0.0));
        assertFalse(HiveOrientation.isUpright(-90.0, 0.0));
        assertTrue(HiveOrientation.isUpright(89.99, 0.0));
    }

    @Test
    public void isUpright_isFalseWhenUnknown() {
        assertFalse(HiveOrientation.isUpright(Double.NaN, 0.0));
    }
}
