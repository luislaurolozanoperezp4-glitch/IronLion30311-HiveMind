// Sources: new for IronLion30311-HiveMind (tests the non-blocking port of vision-align shootPollenPreload @ a7b8f4c)
package org.firstinspires.ftc.teamcode.statemachine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.firstinspires.ftc.teamcode.config.IntakeConstants;
import org.firstinspires.ftc.teamcode.config.LauncherConstants;
import org.firstinspires.ftc.teamcode.testutil.FakeClock;
import org.junit.Test;

public class ShootingSequenceTest {

    private static final double LOOP = 0.02;

    /** A pretend robot: ready when told, counts a shot when told. */
    private static class FakeMechanisms implements ShootingSequence.Mechanisms {
        boolean isReady;
        int shots;
        boolean isFeeding;
        boolean isShaking;
        boolean isStopped;
        double rpm;

        @Override
        public void spinUp(double targetRpm) {
            rpm = targetRpm;
        }

        @Override
        public boolean isReady() {
            return isReady;
        }

        @Override
        public int getShotCount() {
            return shots;
        }

        @Override
        public void setFeed(boolean feeding, boolean shaking) {
            isFeeding = feeding;
            isShaking = shaking;
        }

        @Override
        public void stopAll() {
            isStopped = true;
            isFeeding = false;
        }
    }

    private final FakeClock clock = new FakeClock();
    private final FakeMechanisms robot = new FakeMechanisms();
    private final ShootingSequence sequence = new ShootingSequence(robot, clock);

    private void step() {
        sequence.periodic();
        clock.advance(LOOP);
    }

    @Test
    public void waitsForRpmThenFeedsOneBallPerReadyWindow() {
        sequence.start(2, LauncherConstants.POLLEN_AUTO_RPM, LauncherConstants.SHOOT_MAX_SECONDS);
        assertEquals(LauncherConstants.POLLEN_AUTO_RPM, robot.rpm, 1e-9);
        step();
        assertEquals(ShootingSequence.State.SPIN_UP, sequence.getState());
        assertFalse(robot.isFeeding);

        robot.isReady = true;
        step();
        assertEquals(ShootingSequence.State.FEED_ONE, sequence.getState());
        assertTrue(robot.isFeeding);

        robot.shots = 1;
        robot.isReady = false;
        step();
        assertEquals(ShootingSequence.State.RECOVER, sequence.getState());
        assertFalse(robot.isFeeding);

        robot.isReady = true;
        for (int i = 0; i < 10; i++) {
            step();
        }
        assertTrue(robot.isFeeding);

        robot.shots = 2;
        step();
        assertTrue(sequence.isDone());
        assertTrue(robot.isStopped);
        assertEquals(2, sequence.getShotsFired());
    }

    @Test
    public void shakesTheIntakeWhenNoShotComes() {
        sequence.start(1, LauncherConstants.POLLEN_AUTO_RPM, LauncherConstants.SHOOT_MAX_SECONDS);
        robot.isReady = true;
        double end = clock.now + IntakeConstants.SHAKE_WAIT_SECONDS + 0.1;
        while (clock.now < end && !robot.isShaking) {
            step();
        }
        assertTrue(robot.isShaking);
        assertEquals(1, sequence.getShakeCount());
    }

    @Test
    public void endsWhenTimeRunsOut() {
        sequence.start(4, LauncherConstants.POLLEN_AUTO_RPM, 1.0);
        double end = clock.now + 1.1;
        while (clock.now < end) {
            step();
        }
        assertTrue(sequence.isDone());
        assertEquals(0, sequence.getShotsFired());
    }

    @Test
    public void endsWhenNoOtherShotComes() {
        sequence.start(4, LauncherConstants.POLLEN_AUTO_RPM, 30.0);
        robot.isReady = true;
        step();
        robot.shots = 1;
        double end = clock.now + LauncherConstants.SHOOT_NO_SHOT_END_SECONDS + 0.5;
        while (clock.now < end && !sequence.isDone()) {
            step();
        }
        assertTrue(sequence.isDone());
        assertEquals(1, sequence.getShotsFired());
    }
}
