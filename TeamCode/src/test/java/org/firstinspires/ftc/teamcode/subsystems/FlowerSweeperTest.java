// Sources: new for IronLion30311-HiveMind (tests the FlowerSweeper ported from vision-align @ 344254b and its lowering gate; replaces FlowerArmTest)
package org.firstinspires.ftc.teamcode.subsystems;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.firstinspires.ftc.teamcode.config.FlowerSweeperConstants;
import org.firstinspires.ftc.teamcode.testutil.FakeClock;
import org.firstinspires.ftc.teamcode.util.MatchTimer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/** Slowed-down travel, mirrored positions, first command and the lowering gate. */
public class FlowerSweeperTest {

    private static final double LOOP_SECONDS = 0.02;

    private final FakeClock clock = new FakeClock();
    private double leftUp;
    private double leftDown;
    private double rightUp;
    private double rightDown;

    @Before
    public void setPositions() {
        leftUp = FlowerSweeperConstants.LEFT_UP_POSITION;
        leftDown = FlowerSweeperConstants.LEFT_DOWN_POSITION;
        rightUp = FlowerSweeperConstants.RIGHT_UP_POSITION;
        rightDown = FlowerSweeperConstants.RIGHT_DOWN_POSITION;
        // Mirrored servos: the right one turns the other way
        FlowerSweeperConstants.LEFT_UP_POSITION = 0.2;
        FlowerSweeperConstants.LEFT_DOWN_POSITION = 0.6;
        FlowerSweeperConstants.RIGHT_UP_POSITION = 0.8;
        FlowerSweeperConstants.RIGHT_DOWN_POSITION = 0.4;
    }

    @After
    public void restorePositions() {
        FlowerSweeperConstants.LEFT_UP_POSITION = leftUp;
        FlowerSweeperConstants.LEFT_DOWN_POSITION = leftDown;
        FlowerSweeperConstants.RIGHT_UP_POSITION = rightUp;
        FlowerSweeperConstants.RIGHT_DOWN_POSITION = rightDown;
    }

    private void run(FlowerSweeper sweeper, double seconds) {
        double end = clock.now + seconds;
        while (clock.now < end) {
            sweeper.periodic();
            clock.advance(LOOP_SECONDS);
        }
    }

    @Test
    public void nothingMovesBeforeTheFirstCommand() {
        FlowerSweeper sweeper = new FlowerSweeper(null, null, clock);
        run(sweeper, 1.0);
        assertEquals(FlowerSweeper.State.NOT_SET, sweeper.getState());
        assertFalse(sweeper.isConnected());
    }

    @Test
    public void movesAreSlowedAndMirrored() {
        FlowerSweeper sweeper = new FlowerSweeper(null, null, clock);
        sweeper.up();
        assertEquals("the first command goes straight there", FlowerSweeper.State.UP, sweeper.getState());
        assertTrue(sweeper.down());
        run(sweeper, FlowerSweeperConstants.MOVE_SECONDS / 2.0);
        assertEquals(FlowerSweeper.State.MOVING_DOWN, sweeper.getState());
        // Halfway: both servos are halfway between their own UP and DOWN
        assertEquals(0.4, sweeper.getLeftPosition(), 0.05);
        assertEquals(0.6, sweeper.getRightPosition(), 0.05);
        run(sweeper, FlowerSweeperConstants.MOVE_SECONDS);
        assertTrue(sweeper.isDown());
        assertEquals(0.6, sweeper.getLeftPosition(), 1e-9);
        assertEquals(0.4, sweeper.getRightPosition(), 1e-9);
        assertTrue(sweeper.toggle());
        run(sweeper, FlowerSweeperConstants.MOVE_SECONDS + LOOP_SECONDS);
        assertEquals(FlowerSweeper.State.UP, sweeper.getState());
    }

    @Test
    public void loweringWaitsForTheGateButRaisingNeverDoes() {
        FakeClock matchClock = new FakeClock();
        MatchTimer timer = new MatchTimer(matchClock, false);
        timer.start();
        FlowerSweeper sweeper = new FlowerSweeper(null, null, clock);
        sweeper.setDownGate(timer::canPlaceNectar);

        assertFalse(sweeper.down());
        assertFalse(sweeper.toggle());
        assertEquals(2, sweeper.getRefusedCount());
        sweeper.up();
        assertEquals(FlowerSweeper.State.UP, sweeper.getState());

        matchClock.advance(62.0); // 58 s left: past the G410 unlock and its START-press margin
        assertTrue(sweeper.toggle());
        assertEquals(FlowerSweeper.State.MOVING_DOWN, sweeper.getState());
    }
}
