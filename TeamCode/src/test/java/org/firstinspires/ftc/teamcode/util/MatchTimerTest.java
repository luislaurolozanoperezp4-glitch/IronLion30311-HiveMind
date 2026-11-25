// Sources: new for IronLion30311-HiveMind
package org.firstinspires.ftc.teamcode.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.firstinspires.ftc.teamcode.config.MatchConstants;
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
        clock.now += 2.5; // 58.5 s left
        assertTrue(timer.canPlaceNectar());
        assertEquals(MatchTimer.Phase.ENDGAME, timer.getPhase());
    }

    /** G410 edges (manual TU10): never before 60 s left, plus the START-press safety margin. */
    @Test
    public void g410UnlockEdges() {
        FakeClock clock = new FakeClock();
        MatchTimer timer = new MatchTimer(clock, false);
        assertFalse("Not before START", timer.canPlaceNectar());
        timer.start();
        double unlockRemaining = MatchConstants.NECTAR_UNLOCK_SECONDS
                - MatchConstants.NECTAR_UNLOCK_MARGIN_SECONDS;
        assertTrue("The margin never unlocks early", unlockRemaining <= MatchConstants.NECTAR_UNLOCK_SECONDS);

        clock.now += MatchConstants.TELEOP_SECONDS - MatchConstants.NECTAR_UNLOCK_SECONDS; // exactly 60 s left
        assertEquals("At 1:00 only a zero margin unlocks", MatchConstants.NECTAR_UNLOCK_MARGIN_SECONDS <= 0.0,
                timer.canPlaceNectar());
        assertTrue("Endgame alerts still start at 60 s", timer.isEndgame());

        clock.now = 100.0 + MatchConstants.TELEOP_SECONDS - unlockRemaining - 0.01; // just before unlock
        assertFalse(timer.canPlaceNectar());
        clock.now += 0.02; // just after unlock
        assertTrue(timer.canPlaceNectar());

        clock.now = 100.0 + MatchConstants.TELEOP_SECONDS; // buzzer
        assertFalse("Locked again at 0:00", timer.canPlaceNectar());
        clock.now += 5.0;
        assertFalse(timer.canPlaceNectar());
    }

    /** G410 applies to TELEOP only: an AUTO timer never allows NECTAR into a FLOWER. */
    @Test
    public void autoNeverAllowsNectar() {
        FakeClock clock = new FakeClock();
        MatchTimer timer = new MatchTimer(clock, true);
        timer.start();
        for (int i = 0; i <= 31; i++) {
            assertFalse(timer.canPlaceNectar());
            clock.now += 1.0;
        }
    }

    /** Manual TU10 sections 10.1 / 10.4: 30 s AUTO, 8 s transition, 2:00 TELEOP. */
    @Test
    public void matchTimingMatchesTheManual() {
        assertEquals(30.0, MatchConstants.AUTO_SECONDS, 1e-9);
        assertEquals(8.0, MatchConstants.TRANSITION_SECONDS, 1e-9);
        assertEquals(120.0, MatchConstants.TELEOP_SECONDS, 1e-9);
        assertEquals(60.0, MatchConstants.NECTAR_UNLOCK_SECONDS, 1e-9);
        assertEquals(4, MatchConstants.MAX_CONTROLLED_ELEMENTS);
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
