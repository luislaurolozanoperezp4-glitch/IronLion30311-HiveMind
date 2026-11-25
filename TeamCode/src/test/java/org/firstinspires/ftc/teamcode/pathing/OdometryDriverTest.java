// Sources: new for IronLion30311-HiveMind (simulated tests of the ported vision-align odometry driver)
package org.firstinspires.ftc.teamcode.pathing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.pedropathing.math.Pose;

import org.firstinspires.ftc.teamcode.pathing.paths.NearRoutePoses;
import org.firstinspires.ftc.teamcode.util.MatchTimer;
import org.junit.Test;

/**
 * Drives {@link OdometryDriver} against a simple simulated mecanum robot (power maps straight to
 * speed) to check the step API, the guards and the sign conventions.
 */
public class OdometryDriverTest {

    private static final double LOOP_SECONDS = 0.02;
    private static final double MAX_SPEED_INCHES = 50.0;
    private static final double MAX_TURN_RADIANS = 4.0;

    /** Fake clock moved by the simulation. */
    private static class FakeClock implements MatchTimer.Clock {
        double now = 0.0;

        @Override
        public double nowSeconds() {
            return now;
        }
    }

    /** Integrates the robot-centric commands into a field pose. */
    private static class SimRobot implements OdometryDriver.DriveBase {
        Pose pose;
        double forward;
        double strafe;
        double turn;

        SimRobot(Pose start) {
            pose = start;
        }

        @Override
        public Pose getPose() {
            return pose;
        }

        @Override
        public void setPose(Pose newPose) {
            pose = newPose;
        }

        @Override
        public void drive(double forwardPower, double strafePower, double turnPower) {
            forward = forwardPower;
            strafe = strafePower;
            turn = turnPower;
        }

        void step(double dt) {
            double h = pose.heading();
            double vx = (forward * Math.cos(h) + strafe * Math.sin(h)) * MAX_SPEED_INCHES;
            double vy = (forward * Math.sin(h) - strafe * Math.cos(h)) * MAX_SPEED_INCHES;
            pose = new Pose(pose.x() + vx * dt, pose.y() + vy * dt, h - turn * MAX_TURN_RADIANS * dt);
        }
    }

    private final FakeClock clock = new FakeClock();

    private void run(OdometryDriver driver, SimRobot robot, double maxSeconds) {
        double end = clock.now + maxSeconds;
        driver.periodic();
        while (driver.isBusy() && clock.now < end) {
            robot.step(LOOP_SECONDS);
            clock.now += LOOP_SECONDS;
            driver.periodic();
        }
    }

    @Test
    public void drivesTheTestedLeaveLeg() {
        SimRobot robot = new SimRobot(NearRoutePoses.START);
        OdometryDriver driver = new OdometryDriver(robot, clock);
        assertEquals(OdometryDriver.StartCheck.OK, driver.checkStartPose(NearRoutePoses.START));
        driver.driveTo(NearRoutePoses.SHOOT_SPOT);
        assertTrue(driver.isBusy());
        run(driver, robot, 5.0);
        assertTrue(driver.isDone());
        assertTrue(driver.getResult() == OdometryDriver.Result.REACHED
                || driver.getResult() == OdometryDriver.Result.PASSED);
        assertEquals(0.0, OdometryDriver.distance(robot.pose, NearRoutePoses.SHOOT_SPOT), 1.0);
    }

    @Test
    public void drivesThroughAWaypointAndCarriesSpeed() {
        SimRobot robot = new SimRobot(NearRoutePoses.BACK_OFF_2);
        OdometryDriver driver = new OdometryDriver(robot, clock);
        driver.armPoseJumpGuard();
        driver.driveThrough("West lane", NearRoutePoses.WEST_LANE_SOUTH);
        run(driver, robot, 5.0);
        assertEquals(OdometryDriver.Result.PASSED_THROUGH, driver.getResult());
        driver.driveTo("Up the lane", NearRoutePoses.WEST_LANE_NORTH, 0.85);
        run(driver, robot, 10.0);
        assertFalse(driver.isOdometryLost());
        assertEquals(0.0, OdometryDriver.distance(robot.pose, NearRoutePoses.WEST_LANE_NORTH), 2.0);
    }

    @Test
    public void turnsClockwiseToTheCollectHeading() {
        SimRobot robot = new SimRobot(NearRoutePoses.SHOOT_SPOT);
        OdometryDriver driver = new OdometryDriver(robot, clock);
        driver.turnTo("Turn", Math.toRadians(NearRoutePoses.COLLECT_HEADING_DEG));
        driver.periodic();
        assertTrue("from -90 to 180 the short way is clockwise (+ turn)", robot.turn > 0.0);
        run(driver, robot, 3.0);
        assertEquals(OdometryDriver.Result.REACHED, driver.getResult());
        assertEquals(0.0, Math.toDegrees(OdometryDriver.angleError(Math.PI, robot.pose.heading())), 2.5);
    }

    @Test
    public void stopsForGoodWhenThePoseJumps() {
        SimRobot robot = new SimRobot(NearRoutePoses.START);
        OdometryDriver driver = new OdometryDriver(robot, clock);
        driver.checkStartPose(NearRoutePoses.START);
        driver.driveTo(NearRoutePoses.PARK);
        driver.periodic();
        robot.pose = new Pose(0.0, 0.0, 0.0); // Pinpoint reboot
        driver.periodic();
        assertTrue(driver.isOdometryLost());
        assertEquals(OdometryDriver.Result.ODOMETRY_LOST, driver.getResult());
        assertEquals(0.0, robot.forward, 0.0);
        driver.driveTo(NearRoutePoses.PARK);
        driver.periodic();
        assertEquals(0.0, robot.forward, 0.0);
        assertEquals(0.0, robot.strafe, 0.0);
    }

    @Test
    public void rezeroesASmallStartErrorAndRefusesALargeOne() {
        SimRobot moved = new SimRobot(new Pose(57.0, 15.0, Math.toRadians(-90.0)));
        OdometryDriver driver = new OdometryDriver(moved, clock);
        assertEquals(OdometryDriver.StartCheck.REZEROED, driver.checkStartPose(NearRoutePoses.START));
        assertEquals(0.0, OdometryDriver.distance(moved.pose, NearRoutePoses.START), 1e-9);

        SimRobot broken = new SimRobot(new Pose(200.0, 9.0, Math.toRadians(-90.0)));
        OdometryDriver faulty = new OdometryDriver(broken, clock);
        assertEquals(OdometryDriver.StartCheck.FAULT, faulty.checkStartPose(NearRoutePoses.START));
        assertTrue(faulty.isOdometryLost());
    }

    @Test
    public void timesOutWhenBlocked() {
        SimRobot robot = new SimRobot(NearRoutePoses.START) {
            @Override
            void step(double dt) {
                // wheels spin, robot does not move
            }
        };
        OdometryDriver driver = new OdometryDriver(robot, clock);
        driver.driveTo(NearRoutePoses.SHOOT_SPOT);
        run(driver, robot, 10.0);
        assertEquals(OdometryDriver.Result.TIMED_OUT, driver.getResult());
    }

    @Test
    public void holdsForTheGivenTime() {
        SimRobot robot = new SimRobot(NearRoutePoses.SHOOT_SPOT);
        OdometryDriver driver = new OdometryDriver(robot, clock);
        driver.turnTo("Anchor", NearRoutePoses.SHOOT_SPOT.heading());
        run(driver, robot, 1.0);
        robot.pose = new Pose(robot.pose.x() + 2.0, robot.pose.y(), robot.pose.heading()); // pushed
        driver.hold(1.0);
        run(driver, robot, 2.0);
        assertEquals(OdometryDriver.Result.REACHED, driver.getResult());
        assertEquals(0.0, OdometryDriver.distance(robot.pose, NearRoutePoses.SHOOT_SPOT), 0.6);
    }
}
