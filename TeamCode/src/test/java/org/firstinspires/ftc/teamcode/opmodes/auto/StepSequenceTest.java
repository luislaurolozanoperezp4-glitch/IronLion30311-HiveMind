// Sources: new for IronLion30311-HiveMind (tests for opmodes/auto/StepSequence)
package org.firstinspires.ftc.teamcode.opmodes.auto;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.firstinspires.ftc.teamcode.testutil.FakeClock;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Step ordering, decisions and the abort-to-park of {@link StepSequence}, with a fake clock. */
public class StepSequenceTest {

    private final FakeClock clock = new FakeClock();
    private final List<String> events = new ArrayList<>();

    /** A step that lasts a fixed time on the fake clock and records what happens to it. */
    private class TimedStep extends AutoStep {
        private final double seconds;
        private double startSec = Double.NaN;

        TimedStep(String name, double seconds) {
            super(name);
            this.seconds = seconds;
        }

        @Override
        public void start() {
            startSec = clock.now;
            events.add("start " + getName());
        }

        @Override
        public boolean isDone() {
            return !Double.isNaN(startSec) && clock.now - startSec >= seconds;
        }

        @Override
        public void cancel() {
            events.add("cancel " + getName());
        }
    }

    /** Runs loops of 0.05 s until the sequence is finished or the time is up. */
    private void run(StepSequence sequence, double maxSeconds) {
        double end = clock.now + maxSeconds;
        while (!sequence.isFinished() && clock.now < end) {
            sequence.periodic();
            clock.advance(0.05);
        }
    }

    @Test
    public void stepsRunInOrder() {
        StepSequence sequence = new StepSequence();
        sequence.add(new TimedStep("A", 0.2), new TimedStep("B", 0.1), new TimedStep("C", 0.0));
        run(sequence, 5.0);
        assertTrue(sequence.isFinished());
        assertEquals(Arrays.asList("start A", "start B", "start C"), events);
        assertEquals(3, sequence.getStepsFinished());
    }

    @Test
    public void decisionInsertsStepsBeforeTheRestOfThePlan() {
        final StepSequence sequence = new StepSequence();
        sequence.add(new TimedStep("Volley", 0.1),
                AutoStep.instant("Decide", new Runnable() {
                    @Override
                    public void run() {
                        sequence.addNext(new TimedStep("Collect", 0.1), new TimedStep("Shoot again", 0.1));
                    }
                }),
                new TimedStep("Park", 0.1));
        run(sequence, 5.0);
        assertEquals(Arrays.asList("start Volley", "start Collect", "start Shoot again", "start Park"), events);
    }

    @Test
    public void instantStepsChainInOneLoop() {
        StepSequence sequence = new StepSequence();
        Runnable nothing = new Runnable() {
            @Override
            public void run() {
            }
        };
        sequence.add(AutoStep.instant("D1", nothing), AutoStep.instant("D2", nothing), new TimedStep("Leg", 1.0));
        sequence.periodic();
        assertEquals("Leg", sequence.getCurrentName());
    }

    @Test
    public void instantStepsAreCappedPerLoop() {
        StepSequence sequence = new StepSequence();
        Runnable nothing = new Runnable() {
            @Override
            public void run() {
            }
        };
        for (int i = 0; i < StepSequence.MAX_STEPS_STARTED_PER_LOOP + 2; i++) {
            sequence.add(AutoStep.instant("D" + i, nothing));
        }
        sequence.periodic();
        assertFalse("A loop never runs an unbounded number of steps", sequence.isFinished());
        sequence.periodic();
        assertTrue(sequence.isFinished());
    }

    @Test
    public void abortCancelsTheCurrentStepAndDropsThePlan() {
        StepSequence sequence = new StepSequence();
        final List<String> logLines = new ArrayList<>();
        sequence.setListener(new StepSequence.Listener() {
            @Override
            public void onLogLine(String line) {
                logLines.add(line);
            }
        });
        sequence.add(new TimedStep("Collect", 10.0), new TimedStep("Shoot", 1.0));
        sequence.periodic();
        clock.advance(1.0);
        sequence.periodic();
        sequence.abortAndReplace(new ArrayList<AutoStep>(Arrays.<AutoStep>asList(new TimedStep("Park", 0.1))));
        run(sequence, 5.0);
        assertEquals(Arrays.asList("start Collect", "cancel Collect", "start Park"), events);
        assertTrue(logLines.contains("Collect: CANCELLED"));
        assertTrue(sequence.isFinished());
    }

    @Test
    public void emptySequenceIsFinished() {
        StepSequence sequence = new StepSequence();
        assertTrue(sequence.isFinished());
        sequence.periodic();
        assertEquals("Finished", sequence.getCurrentName());
    }
}
