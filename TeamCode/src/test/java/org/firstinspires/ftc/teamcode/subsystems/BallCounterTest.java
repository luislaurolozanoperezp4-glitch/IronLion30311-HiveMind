// Sources: new for IronLion30311-HiveMind (QA tests for subsystems/BallCounter)
package org.firstinspires.ftc.teamcode.subsystems;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import org.firstinspires.ftc.teamcode.config.VisionConstants;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Counting picked-up balls from HuskyLens frames with {@link BallCounter}. */
public class BallCounterTest {

    private static final double NEAR_Y = VisionConstants.BALL_NEAR_Y_OFFSET + 0.1;
    private static final double FAR_Y = NEAR_Y - VisionConstants.BALL_NEXT_JUMP - 0.1;
    private static final List<ColorSensing.Ball> NONE = Collections.emptyList();

    private final BallCounter counter = new BallCounter();
    private double now = 10.0;

    private static List<ColorSensing.Ball> frame(ColorSensing.Ball... balls) {
        return Arrays.asList(balls);
    }

    private static ColorSensing.Ball ball(BallColor color, double y) {
        return new ColorSensing.Ball(color, 0.0, y, 500);
    }

    private void step(List<ColorSensing.Ball> balls, boolean isIntaking, double dtSec) {
        now += dtSec;
        counter.update(balls, isIntaking, now);
    }

    @Test
    public void nearBallThatDisappearsIsCountedAfterTheConfirmTime() {
        step(frame(ball(BallColor.POLLEN, NEAR_Y)), true, 0.02);
        step(NONE, true, 0.02);
        assertEquals(0, counter.getTotal());
        step(NONE, true, VisionConstants.BALL_LOST_CONFIRM_SECONDS);
        assertEquals(1, counter.getCount(BallColor.POLLEN));
        assertEquals(BallColor.POLLEN, counter.getLastPicked());
    }

    @Test
    public void farBallThatDisappearsIsNotCounted() {
        step(frame(ball(BallColor.POLLEN, VisionConstants.BALL_NEAR_Y_OFFSET - 0.2)), true, 0.02);
        step(NONE, true, 1.0);
        assertEquals(0, counter.getTotal());
    }

    @Test
    public void nothingCountsWhileTheIntakeIsOff() {
        step(frame(ball(BallColor.RED_NECTAR, NEAR_Y)), false, 0.02);
        step(NONE, false, 1.0);
        assertEquals(0, counter.getTotal());
    }

    @Test
    public void stoppingTheIntakeForgetsTheNearBall() {
        step(frame(ball(BallColor.RED_NECTAR, NEAR_Y)), true, 0.02);
        step(NONE, false, 0.02);
        step(NONE, true, 1.0);
        assertEquals(0, counter.getTotal());
    }

    @Test
    public void nextBallJumpingUpTheImageCountsTheNearOne() {
        step(frame(ball(BallColor.BLUE_NECTAR, NEAR_Y)), true, 0.02);
        step(frame(ball(BallColor.POLLEN, FAR_Y)), true, 0.02);
        assertEquals(1, counter.getCount(BallColor.BLUE_NECTAR));
        assertEquals(0, counter.getCount(BallColor.POLLEN));
    }

    @Test
    public void twoPickupsCloserThanTheMinimumIntervalCountOnce() {
        step(frame(ball(BallColor.POLLEN, NEAR_Y)), true, 0.02);
        step(frame(ball(BallColor.POLLEN, FAR_Y)), true, 0.02); // first pickup
        step(frame(ball(BallColor.POLLEN, NEAR_Y)), true, 0.02);
        step(frame(ball(BallColor.POLLEN, FAR_Y)), true, 0.02); // too soon: ignored
        assertEquals(1, counter.getTotal());
    }

    @Test
    public void picksTheBallLowestInTheImageAsNearest() {
        ColorSensing.Ball low = ball(BallColor.POLLEN, 0.9);
        step(frame(ball(BallColor.RED_NECTAR, -0.5), low, ball(BallColor.BLUE_NECTAR, 0.1)),
                true, 0.02);
        assertSame(low, counter.getNearest());
        step(NONE, true, 0.02);
        assertNull(counter.getNearest());
    }

    @Test
    public void resetClearsCountsAndLastPicked() {
        step(frame(ball(BallColor.POLLEN, NEAR_Y)), true, 0.02);
        step(NONE, true, 1.0);
        counter.reset();
        assertEquals(0, counter.getTotal());
        assertNull(counter.getLastPicked());
        assertEquals("0 (pollen 0, red nectar 0, blue nectar 0)", counter.getSummary());
    }

    @Test
    public void summaryListsEveryColor() {
        step(frame(ball(BallColor.RED_NECTAR, NEAR_Y)), true, 0.02);
        step(NONE, true, 1.0);
        assertEquals("1 (pollen 0, red nectar 1, blue nectar 0)", counter.getSummary());
    }
}
