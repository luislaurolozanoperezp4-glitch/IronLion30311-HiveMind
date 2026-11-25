// Sources: new for IronLion30311-HiveMind (QA tests for statemachine/CollectSequence and StateMachine)
package org.firstinspires.ftc.teamcode.statemachine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.firstinspires.ftc.teamcode.config.IntakeConstants;
import org.firstinspires.ftc.teamcode.subsystems.BallColor;
import org.firstinspires.ftc.teamcode.subsystems.ColorSensing;
import org.firstinspires.ftc.teamcode.testutil.FakeClock;
import org.junit.Test;

/** Non-blocking ball collection ({@link CollectSequence}) and {@link StateMachine} timers. */
public class CollectSequenceTest {

    private static final double EPS = 1e-9;

    /** Robot stand-in the test controls. */
    private static class FakeInputs implements CollectSequence.Inputs {
        ColorSensing.Ball ball;
        int totalBalls;
        boolean canCollectMore = true;
        boolean isIntakeOn;
        BallColor[] lastWanted;

        @Override
        public ColorSensing.Ball getClosestBall(BallColor... colors) {
            lastWanted = colors;
            return ball;
        }

        @Override
        public int getTotalBalls() {
            return totalBalls;
        }

        @Override
        public boolean canCollectMore() {
            return canCollectMore;
        }

        @Override
        public void setIntake(boolean isOn) {
            isIntakeOn = isOn;
        }
    }

    private enum Step { A, B }

    private final FakeClock clock = new FakeClock();
    private final FakeInputs inputs = new FakeInputs();
    private final CollectSequence sequence = new CollectSequence(inputs, clock);

    @Test
    public void doesNothingBeforeStart() {
        sequence.periodic();
        assertEquals(CollectSequence.State.IDLE, sequence.getState());
        assertFalse(sequence.isRunning());
        assertFalse(sequence.isDone());
        assertFalse(inputs.isIntakeOn);
    }

    @Test
    public void approachesAVisibleBallWithTheIntakeOn() {
        inputs.ball = new ColorSensing.Ball(BallColor.POLLEN, 0.2, 0.0, 400);
        sequence.start(5.0, BallColor.POLLEN);
        sequence.periodic();
        assertEquals(CollectSequence.State.APPROACHING, sequence.getState());
        assertTrue(inputs.isIntakeOn);
        assertEquals(IntakeConstants.COLLECT_APPROACH_POWER, sequence.getForwardCommand(), EPS);
        assertEquals(IntakeConstants.COLLECT_TURN_KP * 0.2, sequence.getTurnCommand(), EPS);
        assertEquals(BallColor.POLLEN, inputs.lastWanted[0]);
    }

    @Test
    public void turnCommandIsLimited() {
        inputs.ball = new ColorSensing.Ball(BallColor.POLLEN, -100.0, 0.0, 400);
        sequence.start(5.0);
        sequence.periodic();
        assertEquals(-IntakeConstants.COLLECT_MAX_TURN, sequence.getTurnCommand(), EPS);
    }

    @Test
    public void keepsDrivingBrieflyAfterTheBallGoesUnderTheCamera() {
        inputs.ball = new ColorSensing.Ball(BallColor.POLLEN, 0.0, 0.0, 400);
        sequence.start(5.0);
        sequence.periodic();
        inputs.ball = null;
        clock.advance(IntakeConstants.COLLECT_BALL_LOST_DRIVE_SECONDS / 2);
        sequence.periodic();
        assertEquals(CollectSequence.State.INTO_INTAKE, sequence.getState());
        assertEquals(0.0, sequence.getTurnCommand(), EPS);
        assertTrue(sequence.getForwardCommand() > 0.0);

        clock.advance(IntakeConstants.COLLECT_BALL_LOST_DRIVE_SECONDS);
        sequence.periodic();
        assertTrue(sequence.isDone());
        assertEquals(CollectSequence.Result.NO_BALL_IN_VIEW, sequence.getResult());
        assertFalse(inputs.isIntakeOn);
        assertEquals(0.0, sequence.getForwardCommand(), EPS);
    }

    @Test
    public void endsAtOnceWhenNoBallWasEverSeen() {
        sequence.start(5.0);
        sequence.periodic();
        assertTrue(sequence.isDone());
        assertEquals(CollectSequence.Result.NO_BALL_IN_VIEW, sequence.getResult());
    }

    @Test
    public void endsWhenTimeIsUp() {
        inputs.ball = new ColorSensing.Ball(BallColor.POLLEN, 0.0, 0.0, 400);
        sequence.start(2.0);
        sequence.periodic();
        clock.advance(2.0);
        sequence.periodic();
        assertEquals(CollectSequence.Result.TIME_UP, sequence.getResult());
        assertFalse(inputs.isIntakeOn);
    }

    @Test
    public void cancelStopsARunningSequenceButNotAnIdleOne() {
        sequence.cancel();
        assertEquals(CollectSequence.Result.NONE, sequence.getResult());
        assertFalse(sequence.isDone());

        inputs.ball = new ColorSensing.Ball(BallColor.POLLEN, 0.0, 0.0, 400);
        sequence.start(5.0);
        sequence.periodic();
        sequence.cancel();
        assertEquals(CollectSequence.Result.CANCELLED, sequence.getResult());
        assertFalse(inputs.isIntakeOn);
    }

    @Test
    public void countsBallsFromStartAndCanRestart() {
        inputs.totalBalls = 3;
        inputs.ball = new ColorSensing.Ball(BallColor.POLLEN, 0.0, 0.0, 400);
        sequence.start(5.0);
        inputs.totalBalls = 5;
        assertEquals(2, sequence.getBallsCollected());

        sequence.cancel();
        sequence.start(5.0);
        assertTrue(sequence.isRunning());
        assertEquals(CollectSequence.Result.NONE, sequence.getResult());
        assertEquals(0, sequence.getBallsCollected());
    }

    @Test
    public void setStateToTheSameStepKeepsItsTimer() {
        StateMachine<Step> machine = new StateMachine<>(Step.A, clock);
        clock.advance(1.0);
        machine.setState(Step.A);
        assertEquals(1.0, machine.getTimeInStateSec(), EPS);
        machine.restartState(Step.A);
        assertEquals(0.0, machine.getTimeInStateSec(), EPS);
        assertEquals(Step.A, machine.getPreviousState());
    }

    @Test
    public void remembersThePreviousStep() {
        StateMachine<Step> machine = new StateMachine<>(Step.A, clock);
        machine.setState(Step.B);
        assertTrue(machine.isIn(Step.B));
        assertEquals(Step.A, machine.getPreviousState());
    }
}
