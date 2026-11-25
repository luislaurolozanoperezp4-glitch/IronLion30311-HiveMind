// Sources: new for IronLion30311-HiveMind
package org.firstinspires.ftc.teamcode.util;

import static org.junit.Assert.assertEquals;

import org.firstinspires.ftc.teamcode.testutil.FakeClock;
import org.junit.Test;

public class PidfControllerTest {

    private static final double EPS = 1e-9;

    @Test
    public void proportionalAndFeedforward() {
        FakeClock clock = new FakeClock();
        PidfController pid = new PidfController(2.0, 0.0, 0.0, 0.5, clock);
        // 2 * (10 - 4) + 0.5 * 10
        assertEquals(17.0, pid.calculate(10.0, 4.0), EPS);
    }

    @Test
    public void integralIsLimited() {
        FakeClock clock = new FakeClock();
        PidfController pid = new PidfController(0.0, 1.0, 0.0, 0.0, clock);
        pid.setIntegralLimit(0.2);
        pid.calculate(1.0, 0.0);
        for (int i = 0; i < 20; i++) {
            clock.advance(0.05);
            pid.calculate(1.0, 0.0);
        }
        assertEquals(0.2, pid.calculate(1.0, 0.0), EPS);
    }

    @Test
    public void derivativeUsesMeasurementSoSetpointJumpHasNoKick() {
        FakeClock clock = new FakeClock();
        PidfController pid = new PidfController(0.0, 0.0, 1.0, 0.0, clock);
        pid.calculate(0.0, 0.0);
        clock.advance(0.05);
        assertEquals(0.0, pid.calculate(100.0, 0.0), EPS);
        clock.advance(0.05);
        // The measurement rose 1 in 0.05 s, so D = -20
        assertEquals(-20.0, pid.calculate(100.0, 1.0), EPS);
    }

    @Test
    public void outputLimitAndStaticFeedforward() {
        FakeClock clock = new FakeClock();
        PidfController pid = new PidfController(1.0, 0.0, 0.0, 0.0, clock);
        pid.setStaticFeedforward(0.05);
        pid.setOutputLimit(0.5);
        assertEquals(-0.15, pid.calculate(0.0, 0.1), EPS);
        assertEquals(0.5, pid.calculate(10.0, 0.0), EPS);
    }
}
