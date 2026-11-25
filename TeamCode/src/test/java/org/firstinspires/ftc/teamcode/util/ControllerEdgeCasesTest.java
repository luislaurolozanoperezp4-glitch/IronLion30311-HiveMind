// Sources: new for IronLion30311-HiveMind (QA edge-case tests for util/PidfController and util/SlewRateLimiter)
package org.firstinspires.ftc.teamcode.util;

import static org.junit.Assert.assertEquals;

import org.firstinspires.ftc.teamcode.testutil.FakeClock;
import org.junit.Test;

/** Edge cases of {@link PidfController} and {@link SlewRateLimiter} not covered elsewhere. */
public class ControllerEdgeCasesTest {

    private static final double EPS = 1e-9;

    private final FakeClock clock = new FakeClock();

    @Test
    public void pidFirstCallHasNoIntegralOrDerivative() {
        PidfController pid = new PidfController(0.0, 1.0, 1.0, 0.0, clock);
        assertEquals(0.0, pid.calculate(5.0, 0.0), EPS);
    }

    @Test
    public void pidLongPauseIsTreatedAsAShortStep() {
        PidfController pid = new PidfController(0.0, 1.0, 0.0, 0.0, clock);
        pid.calculate(1.0, 0.0);
        clock.advance(10.0); // OpMode paused: only 0.1 s may count
        assertEquals(0.1, pid.calculate(1.0, 0.0), EPS);
    }

    @Test
    public void pidTinyTimeStepAddsNothing() {
        PidfController pid = new PidfController(0.0, 1.0, 1.0, 0.0, clock);
        pid.calculate(1.0, 0.0);
        clock.advance(1e-6);
        assertEquals(0.0, pid.calculate(1.0, 0.5), EPS);
    }

    @Test
    public void pidResetForgetsIntegralAndDerivativeHistory() {
        PidfController pid = new PidfController(0.0, 1.0, 1.0, 0.0, clock);
        pid.calculate(1.0, 0.0);
        clock.advance(0.05);
        pid.calculate(1.0, 0.0);
        pid.reset();
        assertEquals(0.0, pid.getLastError(), EPS);
        clock.advance(0.05);
        // Like a first call again: no I, and no D kick from the old measurement.
        assertEquals(0.0, pid.calculate(1.0, 0.8), EPS);
    }

    @Test
    public void pidStaticFeedforwardIsZeroAtTheTargetAndFollowsTheErrorSign() {
        PidfController pid = new PidfController(0.0, 0.0, 0.0, 0.0, clock);
        pid.setStaticFeedforward(0.1);
        assertEquals(0.0, pid.calculate(2.0, 2.0), EPS);
        assertEquals(-0.1, pid.calculate(1.0, 2.0), EPS);
        assertEquals(0.1, pid.calculate(3.0, 2.0), EPS);
    }

    @Test
    public void pidNegativeLimitsAreUsedAsTheirSize() {
        PidfController pid = new PidfController(10.0, 0.0, 0.0, 0.0, clock);
        pid.setOutputLimit(-1.0);
        assertEquals(1.0, pid.calculate(5.0, 0.0), EPS);
        assertEquals(-1.0, pid.calculate(-5.0, 0.0), EPS);
    }

    @Test
    public void pidSetGainsTakesEffectOnTheNextCall() {
        PidfController pid = new PidfController(1.0, 0.0, 0.0, 0.0, clock);
        assertEquals(2.0, pid.calculate(2.0, 0.0), EPS);
        pid.setGains(3.0, 0.0, 0.0, 0.0);
        assertEquals(6.0, pid.calculate(2.0, 0.0), EPS);
    }

    @Test
    public void slewNegativeDtDoesNotMoveTheOutput() {
        SlewRateLimiter ramp = new SlewRateLimiter(1.0);
        ramp.reset(0.5);
        assertEquals(0.5, ramp.calculate(1.0, -0.1), EPS);
    }

    @Test
    public void slewNegativeRampTimesMeanNoRamp() {
        SlewRateLimiter ramp = new SlewRateLimiter(-1.0, -1.0);
        assertEquals(0.8, ramp.calculate(0.8, 0.01), EPS);
    }

    @Test
    public void slewZeroRampReversalStillStopsAtZeroFirst() {
        SlewRateLimiter ramp = new SlewRateLimiter(0.0, 0.0);
        ramp.reset(1.0);
        assertEquals(0.0, ramp.calculate(-1.0, 0.01), EPS);
        assertEquals(-1.0, ramp.calculate(-1.0, 0.01), EPS);
    }

    @Test
    public void slewRampsDownBackwardsWithTheDecelTime() {
        SlewRateLimiter ramp = new SlewRateLimiter(1.0, 0.5);
        ramp.reset(-1.0);
        assertEquals(-0.8, ramp.calculate(0.0, 0.1), EPS);
        assertEquals(-0.8, ramp.getValue(), EPS);
    }

    @Test
    public void slewSetRampSecondsChangesTheStep() {
        SlewRateLimiter ramp = new SlewRateLimiter(1.0);
        ramp.setRampSeconds(0.5, 0.5);
        assertEquals(0.2, ramp.calculate(1.0, 0.1), EPS);
    }
}
