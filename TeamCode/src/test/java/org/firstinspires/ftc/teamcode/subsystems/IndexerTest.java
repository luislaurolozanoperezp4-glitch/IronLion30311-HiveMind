// Sources: new for IronLion30311-HiveMind (tests the jam/unjam state machine ported from vision-align Indexer @ a7b8f4c)
package org.firstinspires.ftc.teamcode.subsystems;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.firstinspires.ftc.teamcode.config.IndexerConstants;
import org.firstinspires.ftc.teamcode.testutil.FakeClock;
import org.junit.Test;

public class IndexerTest {

    private static final double LOOP = 0.02;
    private static final double LOW_AMPS = 1.0;
    private static final double HIGH_AMPS = 8.0;

    private final FakeClock clock = new FakeClock();
    private double amps = LOW_AMPS;
    private final Indexer indexer = new Indexer(null, () -> amps, clock);

    private void run(double seconds) {
        double end = clock.now + seconds;
        while (clock.now < end) {
            indexer.periodic();
            clock.advance(LOOP);
        }
    }

    @Test
    public void pausesWhileTheLauncherIsNotReady() {
        indexer.feed();
        indexer.setPaused(true);
        run(0.1);
        assertEquals(Indexer.State.PAUSED, indexer.getState());
        indexer.setPaused(false);
        run(0.1);
        assertEquals(Indexer.State.FEEDING, indexer.getState());
    }

    @Test
    public void highCurrentStartsAnUnjam() {
        indexer.feed();
        run(0.1);
        amps = HIGH_AMPS;
        run(IndexerConstants.JAM_TIME_SECONDS + 0.1);
        assertEquals(Indexer.State.UNJAMMING, indexer.getState());
        assertEquals(1, indexer.getTotalUnjams());
        assertTrue(indexer.getPower() <= 0.0);
    }

    @Test
    public void stopsForGoodAfterTooManyJamsAndClearsOnRelease() {
        indexer.feed();
        amps = HIGH_AMPS;
        run(10.0);
        assertEquals(Indexer.State.JAMMED, indexer.getState());
        assertEquals(IndexerConstants.MAX_UNJAM_ATTEMPTS, indexer.getTotalUnjams());
        assertEquals(1, indexer.getTotalJamStops());

        indexer.idle();
        run(0.1);
        assertEquals(Indexer.State.IDLE, indexer.getState());
    }
}
