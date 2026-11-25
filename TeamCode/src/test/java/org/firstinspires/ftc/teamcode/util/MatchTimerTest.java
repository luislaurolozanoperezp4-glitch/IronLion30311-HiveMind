// Sources: new for IronLion30311-HiveMind
package org.firstinspires.ftc.teamcode.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class MatchTimerTest {

    /** A clock the test moves by hand. */
    private static class FakeClock implements MatchTimer.Clock {
        double now = 100.0;

        @Override
        public double nowSeconds() {
            return now;
        }
    }

    @Test
    public void blocksNectarBeforeLastMinute() {
        FakeClock clock = new FakeClock();
        MatchTimer timer = new MatchTimer(clock, false);
        timer.start();
        clock.now += 59.0; // 61 s left
        assertFalse(timer.canPlaceNectar());
        clock.now += 1.5; // 59.5 s left
        assertTrue(timer.canPlaceNectar());
        assertEquals(MatchTimer.Phase.ENDGAME, timer.getPhase());
    }

    @Test
    public void keepsParkReserveInAuto() {
        FakeClock clock = new FakeClock();
        MatchTimer timer = new MatchTimer(clock, true);
        timer.start();
        clock.now += 20.0; // 10 s left, 4 s reserve
        assertTrue(timer.hasTimeFor(6.0));
        assertFalse(timer.hasTimeFor(6.5));
    }

    @Test
    public void reportsOverAtBuzzer() {
        FakeClock clock = new FakeClock();
        MatchTimer timer = new MatchTimer(clock, true);
        assertEquals(MatchTimer.Phase.NOT_STARTED, timer.getPhase());
        timer.start();
        clock.now += 31.0;
        assertEquals(MatchTimer.Phase.OVER, timer.getPhase());
        assertEquals(0.0, timer.getRemainingSeconds(), 1e-9);
    }
}
