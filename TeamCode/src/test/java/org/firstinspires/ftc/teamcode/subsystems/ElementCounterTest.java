// Sources: new for IronLion30311-HiveMind (G407 element limit)
package org.firstinspires.ftc.teamcode.subsystems;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.firstinspires.ftc.teamcode.config.MatchConstants;
import org.firstinspires.ftc.teamcode.testutil.FakeClock;
import org.junit.Test;

public class ElementCounterTest {

    @Test
    public void rejectsFifthElement() {
        ElementCounter counter = new ElementCounter(null, null);
        for (int i = 0; i < MatchConstants.MAX_CONTROLLED_ELEMENTS; i++) {
            assertTrue(counter.recordCollected());
        }
        assertFalse(counter.canCollectMore());
        assertFalse(counter.recordCollected());
        assertEquals(MatchConstants.MAX_CONTROLLED_ELEMENTS, counter.getCount());
        assertEquals(1, counter.getRefusedCount());
    }

    @Test
    public void shotsFreeRoomAgain() {
        ElementCounter counter = new ElementCounter(null, null);
        counter.setCount(MatchConstants.MAX_CONTROLLED_ELEMENTS);
        counter.update(0, 1);
        assertTrue(counter.canCollectMore());
        assertEquals(MatchConstants.MAX_CONTROLLED_ELEMENTS - 1, counter.getCount());
    }

    @Test
    public void updateCountsNewPickupsOnlyOnce() {
        ElementCounter counter = new ElementCounter(null, null);
        counter.update(2, 0);
        counter.update(2, 0);
        assertEquals(2, counter.getCount());
    }

    @Test
    public void intakeRefusesToCollectWhenFull() {
        FakeClock clock = new FakeClock();
        ElementCounter counter = new ElementCounter(null, null);
        counter.setCount(MatchConstants.MAX_CONTROLLED_ELEMENTS);
        Intake intake = new Intake(null, clock);
        intake.setCollectGate(counter::canCollectMore);
        intake.collect();
        intake.periodic();
        assertEquals(Intake.State.FULL, intake.getState());
        assertEquals(0.0, intake.getPower(), 1e-9);

        counter.recordLaunched();
        intake.periodic();
        assertEquals(Intake.State.COLLECTING, intake.getState());
    }

    @Test
    public void pushingToIndexerIgnoresTheGate() {
        ElementCounter counter = new ElementCounter(null, null);
        counter.setCount(MatchConstants.MAX_CONTROLLED_ELEMENTS);
        Intake intake = new Intake(null, new FakeClock());
        intake.setCollectGate(counter::canCollectMore);
        intake.pushToIndexer();
        intake.periodic();
        assertEquals(Intake.State.PUSHING, intake.getState());
    }
}
