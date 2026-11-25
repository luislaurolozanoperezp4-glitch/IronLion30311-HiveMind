// Sources: IronLion30311BIOBUZZInicioDeTemporada / branch feature/turrubiates/vision-align / TeamCode/src/test/.../subsystems/vision/HiveStateTrackerTest.java @ a7b8f4c (all tests) — adapted for BIOBUZZ (util.Alliance)
package org.firstinspires.ftc.teamcode.subsystems.vision;

import org.firstinspires.ftc.teamcode.util.Alliance;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Before;
import org.junit.Test;

public class HiveStateTrackerTest {

    private static final long FRAME_MS = 20;

    /** Long enough to confirm a candidate at one update every {@link #FRAME_MS}. */
    private static final long CONFIRM_FEED_MS = HiveStateTracker.CONFIRM_MS + 2 * FRAME_MS;

    private HiveStateTracker tracker;
    private long nowMs;

    @Before
    public void setUp() {
        tracker = new HiveStateTracker(Alliance.RED);
        nowMs = 1000;
    }

    /** Feeds the same verdicts for the given duration, one update every {@link #FRAME_MS}. */
    private void feed(Boolean scoringUpright, Boolean audienceUpright, long durationMs) {
        long endMs = nowMs + durationMs;
        while (nowMs <= endMs) {
            tracker.update(scoringUpright, audienceUpright, nowMs);
            nowMs += FRAME_MS;
        }
    }

    @Test
    public void startsUnknown() {
        assertNull(tracker.getUpwardCell());
        assertEquals(HiveStateTracker.State.UNKNOWN, tracker.getState(nowMs));
    }

    @Test
    public void confirmsUpwardCellAfterConsistentObservations() {
        feed(true, false, CONFIRM_FEED_MS);
        assertEquals(HiveCell.RED_SCORING, tracker.getUpwardCell());
        assertEquals(HiveStateTracker.State.UPWARD_KNOWN, tracker.getState(nowMs));
        assertEquals(0, tracker.getTipCount());
    }

    @Test
    public void doesNotConfirmTooEarly() {
        feed(true, false, HiveStateTracker.CONFIRM_MS - 3 * FRAME_MS);
        assertNull(tracker.getUpwardCell());
    }

    @Test
    public void needsMinimumUpdatesEvenIfTimePasses() {
        tracker.update(true, null, nowMs);
        tracker.update(true, null, nowMs + 10 * CONFIRM_FEED_MS);
        assertNull(tracker.getUpwardCell());
    }

    @Test
    public void singleVerdictIsEnough() {
        // Only the audience cell is visible and it is upside down: the scoring cell is up.
        feed(null, false, CONFIRM_FEED_MS);
        assertEquals(HiveCell.RED_SCORING, tracker.getUpwardCell());
    }

    @Test
    public void countsTipWhenUpwardCellChanges() {
        feed(true, false, CONFIRM_FEED_MS);
        feed(false, true, CONFIRM_FEED_MS);
        assertEquals(HiveCell.RED_AUDIENCE, tracker.getUpwardCell());
        assertEquals(1, tracker.getTipCount());
    }

    @Test
    public void briefGlitchDoesNotChangeConfirmedCell() {
        feed(true, false, CONFIRM_FEED_MS);
        tracker.update(false, true, nowMs);
        nowMs += FRAME_MS;
        feed(true, false, CONFIRM_FEED_MS);
        assertEquals(HiveCell.RED_SCORING, tracker.getUpwardCell());
        assertEquals(0, tracker.getTipCount());
    }

    @Test
    public void contradictoryFramesBreakTheStreak() {
        tracker.update(true, null, nowMs);
        tracker.update(true, null, nowMs + 80);
        tracker.update(true, true, nowMs + 100); // contradictory
        tracker.update(true, null, nowMs + 160);
        tracker.update(true, null, nowMs + 200);
        assertNull(tracker.getUpwardCell());
    }

    @Test
    public void becomesStaleWithoutObservations() {
        feed(true, false, CONFIRM_FEED_MS);
        long afterMs = nowMs + HiveStateTracker.STALE_AFTER_MS + 1;
        assertEquals(HiveStateTracker.State.UPWARD_STALE, tracker.getState(afterMs));
        assertEquals(HiveCell.RED_SCORING, tracker.getUpwardCell());
    }

    @Test
    public void resetForgetsEverything() {
        feed(true, false, CONFIRM_FEED_MS);
        feed(false, true, CONFIRM_FEED_MS);
        tracker.reset();
        assertNull(tracker.getUpwardCell());
        assertEquals(0, tracker.getTipCount());
        assertEquals(HiveStateTracker.State.UNKNOWN, tracker.getState(nowMs));
    }

    @Test
    public void allianceCellsMapCorrectly() {
        HiveStateTracker blue = new HiveStateTracker(Alliance.BLUE);
        for (int i = 0; i <= 10; i++) {
            blue.update(null, true, 1000 + i * FRAME_MS);
        }
        assertEquals(HiveCell.BLUE_AUDIENCE, blue.getUpwardCell());
        assertEquals(Alliance.BLUE, HiveCell.BLUE_AUDIENCE.getAlliance());
        assertEquals(Alliance.RED, HiveCell.RED_SCORING.getAlliance());
    }

    @Test
    public void emptyFramesNeitherCountNorBreakTheStreak() {
        tracker.update(true, null, nowMs);
        tracker.update(null, null, nowMs + 50);
        tracker.update(true, null, nowMs + 100);
        tracker.update(null, null, nowMs + 120);
        tracker.update(true, null, nowMs + 160);
        assertEquals(HiveCell.RED_SCORING, tracker.getUpwardCell());
    }

    @Test
    public void longGapDropsTheStreak() {
        tracker.update(true, null, nowMs);
        long laterMs = nowMs + HiveStateTracker.STREAK_GAP_MS + 1000;
        tracker.update(true, null, laterMs);
        tracker.update(true, null, laterMs + 20);
        assertNull(tracker.getUpwardCell());
    }

    @Test
    public void staleTrackerRecoversWithAFullStreak() {
        feed(true, false, CONFIRM_FEED_MS);
        nowMs += HiveStateTracker.STALE_AFTER_MS + 100;
        tracker.update(true, false, nowMs); // single glitch-like frame
        assertEquals(HiveStateTracker.State.UPWARD_STALE, tracker.getState(nowMs));
        feed(true, false, CONFIRM_FEED_MS);
        assertEquals(HiveStateTracker.State.UPWARD_KNOWN, tracker.getState(nowMs));
        assertEquals(0, tracker.getTipCount());
    }

    @Test
    public void tipAfterStaleIsCounted() {
        feed(true, false, CONFIRM_FEED_MS);
        nowMs += HiveStateTracker.STALE_AFTER_MS + 100;
        feed(false, true, CONFIRM_FEED_MS);
        assertEquals(HiveCell.RED_AUDIENCE, tracker.getUpwardCell());
        assertEquals(1, tracker.getTipCount());
    }

    @Test
    public void staysKnownUpToTheStaleBoundary() {
        feed(true, false, CONFIRM_FEED_MS);
        long lastMs = nowMs - FRAME_MS;
        assertEquals(HiveStateTracker.State.UPWARD_KNOWN,
                tracker.getState(lastMs + HiveStateTracker.STALE_AFTER_MS));
        assertEquals(HiveStateTracker.State.UPWARD_STALE,
                tracker.getState(lastMs + HiveStateTracker.STALE_AFTER_MS + 1));
    }

    @Test
    public void keepingTheNewCellDoesNotCountMoreTips() {
        feed(true, false, CONFIRM_FEED_MS);
        feed(false, true, CONFIRM_FEED_MS);
        feed(false, true, 5 * CONFIRM_FEED_MS);
        assertEquals(1, tracker.getTipCount());
    }

    @Test
    public void selectTargetUsesOnlyASingleUprightCellWhenUnconfirmed() {
        assertEquals(HiveCell.RED_SCORING, tracker.selectTarget(true, null, nowMs));
        assertEquals(HiveCell.RED_AUDIENCE, tracker.selectTarget(false, true, nowMs));
        assertNull(tracker.selectTarget(true, true, nowMs));
        assertNull(tracker.selectTarget(false, false, nowMs));
        assertNull(tracker.selectTarget(null, false, nowMs));
        assertNull(tracker.selectTarget(null, null, nowMs));
    }

    @Test
    public void selectTargetNeverAimsAtACellSeenUpsideDown() {
        feed(true, false, CONFIRM_FEED_MS);
        assertEquals(HiveCell.RED_SCORING, tracker.selectTarget(null, null, nowMs));
        assertEquals(HiveCell.RED_SCORING, tracker.selectTarget(true, false, nowMs));
        assertNull(tracker.selectTarget(false, true, nowMs));
    }
}
