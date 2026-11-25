// Sources: new for IronLion30311-HiveMind (StateMachine; TipWatch ported from vision-align watchForTip @ a7b8f4c; CollectSequence from collectBalls)
package org.firstinspires.ftc.teamcode.statemachine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.firstinspires.ftc.teamcode.subsystems.BallColor;
import org.firstinspires.ftc.teamcode.subsystems.ColorSensing;
import org.firstinspires.ftc.teamcode.subsystems.vision.HiveCell;
import org.firstinspires.ftc.teamcode.testutil.FakeClock;
import org.junit.Test;

public class StateMachineAndTipWatchTest {

    private enum Step { A, B }

    private final FakeClock clock = new FakeClock();

    @Test
    public void stateMachineTracksTimeInState() {
        StateMachine<Step> machine = new StateMachine<>(Step.A, clock);
        clock.advance(1.0);
        machine.setState(Step.A); // same state: timer keeps running
        assertEquals(1.0, machine.getTimeInStateSec(), 1e-9);
        machine.setState(Step.B);
        assertTrue(machine.isIn(Step.B));
        assertEquals(Step.A, machine.getPreviousState());
        assertEquals(0.0, machine.getTimeInStateSec(), 1e-9);
    }

    /** A cell view the test controls. */
    private static class FakeView implements TipWatch.CellView {
        boolean isInView;
        boolean hasSeen;

        @Override
        public boolean isCellInView(HiveCell cell) {
            return isInView;
        }

        @Override
        public boolean hasSeenCell(HiveCell cell) {
            return hasSeen;
        }
    }

    @Test
    public void tipIsSeenWhenTheCellStaysGone() {
        FakeView view = new FakeView();
        view.hasSeen = true;
        TipWatch watch = new TipWatch(view, clock);
        watch.start(HiveCell.RED_SCORING, 4.0, 0.8);
        // Time without tags before the watch does not count
        watch.periodic();
        assertFalse(watch.isDone());
        clock.advance(0.9);
        watch.periodic();
        assertTrue(watch.isCellDown());
    }

    @Test
    public void cellStillUpWhenItStaysInView() {
        FakeView view = new FakeView();
        view.hasSeen = true;
        view.isInView = true;
        TipWatch watch = new TipWatch(view, clock);
        watch.start(HiveCell.RED_SCORING, 1.0, 0.8);
        for (int i = 0; i < 60; i++) {
            watch.periodic();
            clock.advance(0.02);
        }
        assertTrue(watch.isCellStillUp());
    }

    @Test
    public void neverSeenIsUnknown() {
        TipWatch watch = new TipWatch(new FakeView(), clock);
        watch.start(HiveCell.RED_SCORING, 1.0, 0.8);
        clock.advance(1.1);
        watch.periodic();
        assertTrue(watch.isUnknown());
    }

    @Test
    public void collectStopsWhenFull() {
        final boolean[] canCollect = {true};
        final boolean[] intakeOn = {false};
        CollectSequence collect = new CollectSequence(new CollectSequence.Inputs() {
            @Override
            public ColorSensing.Ball getClosestBall(BallColor... colors) {
                return new ColorSensing.Ball(BallColor.POLLEN, 0.5, 0.0, 500);
            }

            @Override
            public int getTotalBalls() {
                return 0;
            }

            @Override
            public boolean canCollectMore() {
                return canCollect[0];
            }

            @Override
            public void setIntake(boolean isOn) {
                intakeOn[0] = isOn;
            }
        }, clock);
        collect.start(5.0);
        collect.periodic();
        assertTrue(intakeOn[0]);
        assertTrue(collect.getForwardCommand() > 0.0);
        assertTrue(collect.getTurnCommand() > 0.0);

        canCollect[0] = false;
        collect.periodic();
        assertTrue(collect.isDone());
        assertEquals(CollectSequence.Result.FULL, collect.getResult());
        assertFalse(intakeOn[0]);
    }
}
