// Sources: new for IronLion30311-HiveMind (G408 opponent NECTAR, ball counting from vision-align BallIntakeCounter)
package org.firstinspires.ftc.teamcode.subsystems;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.firstinspires.ftc.teamcode.config.IntakeConstants;
import org.firstinspires.ftc.teamcode.config.VisionConstants;
import org.firstinspires.ftc.teamcode.testutil.FakeClock;
import org.firstinspires.ftc.teamcode.util.Alliance;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ColorSensingTest {

    private static final double NEAR = 0.8;
    private static final double FAR = -0.5;

    private static List<ColorSensing.Ball> frame(BallColor color, double yOffset) {
        List<ColorSensing.Ball> balls = new ArrayList<>();
        balls.add(new ColorSensing.Ball(color, 0.0, yOffset, 1000));
        return balls;
    }

    @Test
    public void rejectsOpponentNectar() {
        FakeClock clock = new FakeClock();
        ColorSensing sensing = new ColorSensing(null, clock);
        sensing.updateFromBalls(frame(BallColor.BLUE_NECTAR, NEAR));
        assertTrue(sensing.isOpponentNectarAtIntake(Alliance.RED));
        assertFalse(sensing.isOpponentNectarAtIntake(Alliance.BLUE));
        assertTrue(sensing.isNectarAtIntake());

        final ColorSensing seen = sensing;
        Intake intake = new Intake(null, clock);
        intake.setRejectDetector(() -> seen.isOpponentNectarAtIntake(Alliance.RED));
        intake.collect();
        intake.periodic();
        assertEquals(Intake.State.REJECTING, intake.getState());

        sensing.updateFromBalls(Collections.<ColorSensing.Ball>emptyList());
        clock.advance(IntakeConstants.REJECT_EJECT_SECONDS + 0.01);
        intake.periodic();
        assertEquals(Intake.State.COLLECTING, intake.getState());
        assertEquals(1, intake.getRejectCount());
    }

    @Test
    public void farNectarIsNotAtTheIntake() {
        ColorSensing sensing = new ColorSensing(null, new FakeClock());
        sensing.updateFromBalls(frame(BallColor.RED_NECTAR, FAR));
        assertFalse(sensing.isNectarAtIntake());
        assertFalse(sensing.isOpponentNectarAtIntake(Alliance.BLUE));
        assertEquals(BallColor.RED_NECTAR, sensing.getClosestNectar().color);
    }

    @Test
    public void countsABallThatGoesIntoTheIntake() {
        FakeClock clock = new FakeClock();
        ColorSensing sensing = new ColorSensing(null, clock);
        sensing.countWhileIntaking(true);
        sensing.updateFromBalls(frame(BallColor.POLLEN, NEAR));
        clock.advance(0.05);
        sensing.updateFromBalls(Collections.<ColorSensing.Ball>emptyList());
        clock.advance(VisionConstants.BALL_LOST_CONFIRM_SECONDS + 0.01);
        sensing.updateFromBalls(Collections.<ColorSensing.Ball>emptyList());
        assertEquals(1, sensing.getTotalBalls());
        assertEquals(1, sensing.getBallCount(BallColor.POLLEN));
    }

    @Test
    public void doesNotCountWhileTheIntakeIsOff() {
        FakeClock clock = new FakeClock();
        ColorSensing sensing = new ColorSensing(null, clock);
        sensing.updateFromBalls(frame(BallColor.POLLEN, NEAR));
        clock.advance(1.0);
        sensing.updateFromBalls(Collections.<ColorSensing.Ball>emptyList());
        assertEquals(0, sensing.getTotalBalls());
    }

    @Test
    public void unknownIdsAndTinyBlocksAreIgnored() {
        assertNull(ColorSensing.toBall(9, 160, 120, 40, 40));
        assertNull(ColorSensing.toBall(VisionConstants.HUSKY_POLLEN_ID, 160, 120, 2, 2));
        ColorSensing.Ball ball = ColorSensing.toBall(VisionConstants.HUSKY_POLLEN_ID, 320, 240, 20, 20);
        assertEquals(1.0, ball.xOffset, 1e-9);
        assertEquals(1.0, ball.yOffset, 1e-9);
    }
}
