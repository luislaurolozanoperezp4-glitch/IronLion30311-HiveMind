// Sources: new for IronLion30311-HiveMind
package org.firstinspires.ftc.teamcode.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import com.pedropathing.math.Pose;

import org.junit.Test;

public class AllianceTest {

    private static final double EPS = 1e-9;

    @Test
    public void keepsRedPoseUnchanged() {
        Pose p = new Pose(10, 20, 0.5);
        assertSame(p, Alliance.RED.toField(p));
    }

    @Test
    public void rotatesBluePoseAroundCenter() {
        Pose blue = Alliance.BLUE.toField(new Pose(10, 20, 0.0));
        assertEquals(134.0, blue.x(), EPS);
        assertEquals(124.0, blue.y(), EPS);
        assertEquals(Math.PI, Math.abs(blue.heading()), EPS);
    }

    @Test
    public void returnsSamePoseAfterTwoRotations() {
        Pose p = new Pose(57, 9, Math.toRadians(-90));
        Pose back = Alliance.rotate180(Alliance.rotate180(p));
        assertEquals(p.x(), back.x(), EPS);
        assertEquals(p.y(), back.y(), EPS);
        assertEquals(p.heading(), back.heading(), EPS);
    }

    @Test
    public void keepsAnglesInRange() {
        assertEquals(Math.PI / 2, Alliance.normalizeRadians(Math.PI / 2 + 4 * Math.PI), EPS);
        assertEquals(-Math.PI / 2, Alliance.normalizeRadians(3 * Math.PI / 2), EPS);
    }
}
