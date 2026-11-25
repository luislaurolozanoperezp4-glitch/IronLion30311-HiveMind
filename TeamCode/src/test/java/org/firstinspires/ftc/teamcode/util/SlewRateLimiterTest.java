// Sources: new for IronLion30311-HiveMind
package org.firstinspires.ftc.teamcode.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class SlewRateLimiterTest {

    private static final double EPS = 1e-9;

    @Test
    public void rampsUpByOneStepPerCall() {
        SlewRateLimiter ramp = new SlewRateLimiter(0.5, 0.25);
        assertEquals(0.1, ramp.calculate(1.0, 0.05), EPS);
        assertEquals(0.2, ramp.calculate(1.0, 0.05), EPS);
    }

    @Test
    public void slowsDownWithTheDecelRamp() {
        SlewRateLimiter ramp = new SlewRateLimiter(0.5, 0.25);
        ramp.reset(1.0);
        assertEquals(0.8, ramp.calculate(0.0, 0.05), EPS);
    }

    @Test
    public void reversalStopsAtZeroFirst() {
        SlewRateLimiter ramp = new SlewRateLimiter(1.0, 1.0);
        ramp.reset(0.05);
        assertEquals(0.0, ramp.calculate(-1.0, 0.1), EPS);
        assertEquals(-0.1, ramp.calculate(-1.0, 0.1), EPS);
    }

    @Test
    public void neverOvershootsTheTarget() {
        SlewRateLimiter ramp = new SlewRateLimiter(0.1);
        assertEquals(0.3, ramp.calculate(0.3, 1.0), EPS);
    }

    @Test
    public void zeroRampJumpsStraightToTarget() {
        SlewRateLimiter ramp = new SlewRateLimiter(0.0, 0.0);
        assertEquals(0.7, ramp.calculate(0.7, 0.01), EPS);
    }
}
