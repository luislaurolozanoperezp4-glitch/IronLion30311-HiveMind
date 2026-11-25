// Sources: new for IronLion30311-HiveMind (tests for TerritoryGuard, EmergencyPark, GuardedLeaveMove, AutoSafePark and ScoringAuto estimates)
package org.firstinspires.ftc.teamcode.opmodes.auto;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.pedropathing.math.Pose;

import org.firstinspires.ftc.teamcode.config.AutoConstants;
import org.firstinspires.ftc.teamcode.config.DriveConstants;
import org.firstinspires.ftc.teamcode.config.FieldConstants;
import org.firstinspires.ftc.teamcode.config.RobotConstants;
import org.firstinspires.ftc.teamcode.pathing.AutoPlanner;
import org.firstinspires.ftc.teamcode.pathing.paths.NearRoutePoses;
import org.firstinspires.ftc.teamcode.pathing.paths.SafeParkPaths;
import org.firstinspires.ftc.teamcode.testutil.FakeClock;
import org.firstinspires.ftc.teamcode.util.Alliance;
import org.firstinspires.ftc.teamcode.util.MatchTimer;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Safety rules of the competition autos (G402, park from anywhere, encoder LEAVE) and the planner gate. */
public class AutoSafetyTest {

    private static final double EPSILON = 1e-6;
    /** Half the diagonal of the 18 x 18 in robot: any heading fits in this circle. */
    private static final double ROBOT_RADIUS = Math.hypot(RobotConstants.ROBOT_LENGTH_INCHES,
            RobotConstants.ROBOT_WIDTH_INCHES) / 2.0;
    private static final double SAMPLE_INCHES = 0.5;

    /** Every pose a routine can be in when the time guard fires (RED frame). */
    private static final List<Pose> ROUTE_POSES = Arrays.asList(NearRoutePoses.START, NearRoutePoses.SHOOT_SPOT,
            NearRoutePoses.SHOOT_SPOT_FACING_COLLECT, NearRoutePoses.COLLECT_START, NearRoutePoses.COLLECT_END,
            NearRoutePoses.COLLECT_EXIT, NearRoutePoses.SOUTH_WAYPOINT, NearRoutePoses.WEST_LANE_SOUTH,
            NearRoutePoses.WEST_LANE_NORTH, NearRoutePoses.NORTH_WAYPOINT, NearRoutePoses.OTHER_SPOT,
            NearRoutePoses.PARK, FieldConstants.FAR_START, StartPosition.LOADING_ZONE.getRedPose());

    // ---------------- G402 territory guard ----------------

    @Test
    public void territoryGuardAllowsEveryRoutePose() {
        for (Alliance alliance : Alliance.values()) {
            for (Pose red : ROUTE_POSES) {
                assertFalse(alliance + " " + red, TerritoryGuard.isPastLine(alliance.toField(red), alliance));
            }
        }
    }

    @Test
    public void territoryGuardTripsWhenTheSideTouchesTheLine() {
        double limit = FieldConstants.TERRITORY_LINE_X - RobotConstants.ROBOT_WIDTH_INCHES / 2.0;
        Pose red = new Pose(limit + 0.5, 30.0, 0.0);
        assertTrue(TerritoryGuard.isPastLine(red, Alliance.RED));
        assertTrue("BLUE is checked in its rotated frame",
                TerritoryGuard.isPastLine(Alliance.rotate180(red), Alliance.BLUE));
        assertTrue("That spot is red territory for BLUE", TerritoryGuard.isPastLine(red, Alliance.BLUE));
        assertFalse("A BLUE robot deep on the blue side is fine",
                TerritoryGuard.isPastLine(Alliance.rotate180(NearRoutePoses.SHOOT_SPOT), Alliance.BLUE));
    }

    // ---------------- Emergency park ----------------

    @Test
    public void emergencyParkEndsInParkWithTheSameHeading() {
        for (Alliance alliance : Alliance.values()) {
            for (Pose red : ROUTE_POSES) {
                Pose field = alliance.toField(red);
                List<Pose> plan = EmergencyPark.plan(field, alliance);
                Pose last = alliance.toField(plan.get(plan.size() - 1));
                assertEquals(NearRoutePoses.PARK.x(), last.x(), EPSILON);
                assertEquals(NearRoutePoses.PARK.y(), last.y(), EPSILON);
                for (Pose waypoint : plan) {
                    assertEquals("No turn while parking", 0.0,
                            Alliance.normalizeRadians(waypoint.heading() - field.heading()), EPSILON);
                }
            }
        }
    }

    @Test
    public void emergencyParkNeverTouchesTheHiveOrCrossesTheLine() {
        for (Pose red : ROUTE_POSES) {
            List<Pose> legs = new ArrayList<>();
            legs.add(red);
            legs.addAll(EmergencyPark.plan(red, Alliance.RED));
            for (int i = 1; i < legs.size(); i++) {
                Pose a = legs.get(i - 1);
                Pose b = legs.get(i);
                double length = Math.hypot(b.x() - a.x(), b.y() - a.y());
                int samples = Math.max(1, (int) Math.ceil(length / SAMPLE_INCHES));
                for (int s = 0; s <= samples; s++) {
                    double t = (double) s / samples;
                    double x = a.x() + (b.x() - a.x()) * t;
                    double y = a.y() + (b.y() - a.y()) * t;
                    assertTrue("HIVE clearance from " + red + " at (" + x + ", " + y + ")",
                            distanceToHive(x, y) > ROBOT_RADIUS);
                    assertTrue("Territory from " + red, x <= TerritoryGuard.getMaxCenterX());
                }
            }
        }
    }

    @Test
    public void emergencyParkFromTheShootSpotUsesTheWestLane() {
        List<Pose> plan = EmergencyPark.plan(NearRoutePoses.SHOOT_SPOT, Alliance.RED);
        assertEquals(3, plan.size());
        assertEquals(NearRoutePoses.WEST_LANE_SOUTH.x(), plan.get(0).x(), EPSILON);
        assertEquals(NearRoutePoses.WEST_LANE_NORTH.y(), plan.get(1).y(), EPSILON);
    }

    @Test
    public void emergencyParkFromNorthOfTheHiveGoesStraightIn() {
        assertEquals(1, EmergencyPark.plan(NearRoutePoses.OTHER_SPOT, Alliance.RED).size());
        assertEquals(1, EmergencyPark.plan(Alliance.BLUE.toField(NearRoutePoses.OTHER_SPOT), Alliance.BLUE).size());
    }

    private static double distanceToHive(double x, double y) {
        double dx = Math.max(Math.max(FieldConstants.HIVE_MIN_X - x, 0.0), x - FieldConstants.HIVE_MAX_X);
        double dy = Math.max(Math.max(FieldConstants.HIVE_MIN_Y - y, 0.0), y - FieldConstants.HIVE_MAX_Y);
        return Math.hypot(dx, dy);
    }

    // ---------------- Encoder LEAVE ----------------

    /** Wheels that move by hand. */
    private static class FakeWheels implements GuardedLeaveMove.Wheels {
        final int[] ticks = new int[4];
        double power;
        boolean isStopped;

        @Override
        public int[] getWheelPositions() {
            return ticks.clone();
        }

        @Override
        public void drive(double forward) {
            power = forward;
            isStopped = false;
        }

        @Override
        public void stop() {
            power = 0.0;
            isStopped = true;
        }
    }

    @Test
    public void encoderLeaveStopsOnTheMeanAbsoluteTravel() {
        FakeClock clock = new FakeClock();
        FakeWheels wheels = new FakeWheels();
        GuardedLeaveMove move = new GuardedLeaveMove(wheels, clock);
        move.start(AutoConstants.ENCODER_LEAVE_CM, -AutoConstants.ENCODER_LEAVE_POWER, 10.0);
        assertEquals(-AutoConstants.ENCODER_LEAVE_POWER, wheels.power, EPSILON);
        int ticks = (int) Math.ceil(AutoConstants.ENCODER_LEAVE_CM * DriveConstants.ENCODER_TICKS_PER_CM_FORWARD);
        // One encoder counts the wrong way: it must not cancel the others
        wheels.ticks[0] = ticks;
        wheels.ticks[1] = -ticks;
        wheels.ticks[2] = -ticks;
        wheels.ticks[3] = -ticks;
        clock.advance(0.1);
        move.periodic();
        assertTrue(move.isDone());
        assertEquals(GuardedLeaveMove.Result.REACHED, move.getResult());
        assertTrue(wheels.isStopped);
    }

    @Test
    public void encoderLeaveTimesOutWithDeadEncoders() {
        FakeClock clock = new FakeClock();
        FakeWheels wheels = new FakeWheels();
        GuardedLeaveMove move = new GuardedLeaveMove(wheels, clock);
        move.start(AutoConstants.ENCODER_LEAVE_CM, -AutoConstants.ENCODER_LEAVE_POWER,
                AutoConstants.ENCODER_LEAVE_TIMEOUT_SECONDS);
        clock.advance(AutoConstants.ENCODER_LEAVE_TIMEOUT_SECONDS / 2.0);
        move.periodic();
        assertFalse(move.isDone());
        clock.advance(AutoConstants.ENCODER_LEAVE_TIMEOUT_SECONDS);
        move.periodic();
        assertEquals(GuardedLeaveMove.Result.TIMED_OUT, move.getResult());
        assertTrue(wheels.isStopped);
    }

    // ---------------- Safe Park and planner gate ----------------

    @Test
    public void competitionParkEndsLeaveAndPark() {
        // Team decision: every competition park ends >= WALL_CLEARANCE_INCHES off the wall, partly in the zone.
        StartPositionRulesTest.assertParkWithLeave("NearRoutePoses.PARK", NearRoutePoses.PARK);
        StartPositionRulesTest.assertParkWithLeave("NearRoutePoses.OTHER_PARK", NearRoutePoses.OTHER_PARK);
        StartPositionRulesTest.assertParkWithLeave("SafeParkPaths.PARK", SafeParkPaths.PARK);
        StartPositionRulesTest.assertParkWithLeave("FieldConstants.RED_PARK", FieldConstants.RED_PARK);
        StartPositionRulesTest.assertParkWithLeave("AutoSafePark beside-zone park", AutoSafePark.loadingZoneParkPose());
        for (Pose red : ROUTE_POSES) {
            List<Pose> plan = EmergencyPark.plan(red, Alliance.RED);
            StartPositionRulesTest.assertParkWithLeave("EmergencyPark from " + red, plan.get(plan.size() - 1));
        }
    }

    // ---------------- G402 guard with a turned robot ----------------

    @Test
    public void territoryGuardCountsTheCornersOfATurnedRobot() {
        double squareLimit = FieldConstants.TERRITORY_LINE_X - RobotConstants.ROBOT_WIDTH_INCHES / 2.0;
        // 1 in inside the square limit: fine square to the field, past the line at 45 degrees.
        Pose square = new Pose(squareLimit - 1.0, 30.0, Math.toRadians(90.0));
        Pose turned = new Pose(squareLimit - 1.0, 30.0, Math.toRadians(45.0));
        assertFalse(TerritoryGuard.isPastLine(square, Alliance.RED));
        assertTrue("A 45-degree corner reaches 12.7 in", TerritoryGuard.isPastLine(turned, Alliance.RED));
        assertTrue(TerritoryGuard.isPastLine(Alliance.rotate180(turned), Alliance.BLUE));
        assertEquals(ROBOT_RADIUS, TerritoryGuard.getHalfReachX(Math.toRadians(45.0)), 1e-9);
    }

    @Test
    public void plannerCyclesAfterAnEarlyVolleyAndParksAfterALateOne() {
        FakeClock clock = new FakeClock();
        MatchTimer timer = new MatchTimer(clock, true);
        AutoPlanner planner = new AutoPlanner(timer);
        timer.start();
        double cycle = ScoringAuto.estimateNearCycleSeconds();
        double park = ScoringAuto.estimateParkFromShootSideSeconds();
        assertTrue(cycle > 0.0 && park > 0.0);
        // The tested preload volley ends around 6-7 s
        clock.advance(6.0);
        assertEquals(AutoPlanner.Decision.CYCLE_AGAIN, planner.decideNextStep(cycle, park, 0, true));
        // A volley that ends after the 11 s backstop leaves no time for a cycle either
        clock.advance(AutoConstants.COLLECT_LATEST_START_SECONDS - 6.0 + 1.0);
        assertEquals(AutoPlanner.Decision.GO_PARK, planner.decideNextStep(cycle, park, 0, true));
    }
}
