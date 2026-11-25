// Sources: new for IronLion30311-HiveMind (QA tests for pathing/AutoPlanner deadlines and estimates; park time budget from vision-align @ 1e723f2)
package org.firstinspires.ftc.teamcode.pathing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.pedropathing.math.Pose;

import org.firstinspires.ftc.teamcode.config.AutoConstants;
import org.firstinspires.ftc.teamcode.config.DriveConstants;
import org.firstinspires.ftc.teamcode.config.LauncherConstants;
import org.firstinspires.ftc.teamcode.testutil.FakeClock;
import org.firstinspires.ftc.teamcode.util.MatchTimer;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Fixed deadlines, volley time and leg estimates of {@link AutoPlanner}. */
public class AutoPlannerTest {

    private static final double EPS = 1e-9;

    private final FakeClock clock = new FakeClock();
    private final MatchTimer timer = new MatchTimer(clock, true);
    private final AutoPlanner planner = new AutoPlanner(timer);

    @Test
    public void deadlineIncludesTheExactSecond() {
        timer.start();
        clock.advance(11.0);
        assertTrue(planner.isBeforeDeadline(11.0));
        clock.advance(0.01);
        assertFalse(planner.isBeforeDeadline(11.0));
    }

    @Test
    public void shootTimeIsCappedAndNeverNegative() {
        timer.start();
        clock.advance(20.0);
        assertEquals(3.0, planner.getShootTimeLeft(3.0, 25.0), EPS);
        assertEquals(1.5, planner.getShootTimeLeft(3.0, 21.5), EPS);
        assertEquals(0.0, planner.getShootTimeLeft(3.0, 19.0), EPS);
    }

    @Test
    public void noLegsTakeNoTime() {
        assertEquals(0.0, AutoPlanner.estimateLegsSeconds(Collections.<Pose>emptyList()), EPS);
        assertEquals(0.0, AutoPlanner.estimateLegsSeconds(
                Collections.singletonList(new Pose(10.0, 10.0, 0.0))), EPS);
    }

    @Test
    public void legsAddUpWithOverheadEach() {
        double legs = AutoPlanner.estimateLegsSeconds(Arrays.asList(
                new Pose(0.0, 0.0, 0.0), new Pose(30.0, 40.0, 0.0), new Pose(30.0, 0.0, 0.0)));
        double expected = AutoConstants.ESTIMATE_ROUTE_FIXED_SECONDS
                + 2 * AutoConstants.ESTIMATE_PER_LEG_SECONDS
                + (50.0 + 40.0) / (AutoConstants.ESTIMATE_INCHES_PER_SECOND_PER_POWER * DriveConstants.AUTO_MAX_POWER);
        assertEquals(expected, legs, EPS);
    }

    @Test
    public void fittedModelMatchesTheLoggedRuns() {
        // vision-align 1e723f2: a 35 + 70 in park took 3.6 s at power 0.85
        double seconds = AutoPlanner.estimateRouteSeconds(0.85, Arrays.asList(
                new Pose(0.0, 0.0, 0.0), new Pose(35.0, 0.0, 0.0), new Pose(35.0, 70.0, 0.0)));
        assertEquals(3.6, seconds, 0.1);
    }

    @Test
    public void parkPowerIsNormalEarlyAndFasterWhenLate() {
        timer.start();
        List<Pose> park = Arrays.asList(new Pose(0.0, 0.0, 0.0), new Pose(0.0, 100.0, 0.0));
        clock.advance(10.0);
        assertEquals(DriveConstants.AUTO_MAX_POWER, planner.getParkPower(park), EPS);
        clock.advance(16.5); // 26.5 s: 2 s to the 28.5 s deadline for 100 in
        double late = planner.getParkPower(park);
        assertTrue(late > DriveConstants.AUTO_MAX_POWER);
        assertTrue(late <= AutoConstants.PARK_MAX_POWER);
        clock.advance(5.0); // past the deadline
        assertEquals(AutoConstants.PARK_MAX_POWER, planner.getParkPower(park), EPS);
    }

    @Test
    public void volleyEndsWhenOnlyTheParkTimeIsLeft() {
        timer.start();
        List<Pose> park = Arrays.asList(new Pose(0.0, 0.0, 0.0), new Pose(0.0, 70.0, 0.0));
        double parkSeconds = AutoPlanner.estimateRouteSeconds(AutoConstants.PARK_MAX_POWER, park);
        clock.advance(20.0);
        double expected = AutoPlanner.getParkDeadlineSeconds() - AutoConstants.SETTLE_SECONDS - 20.0 - parkSeconds;
        assertEquals(Math.min(LauncherConstants.SHOOT_MAX_SECONDS, expected), planner.getVolleyTimeLeft(park), EPS);
        clock.advance(10.0);
        assertEquals(0.0, planner.getVolleyTimeLeft(park), EPS);
    }

    @Test
    public void otherCellOnlyWhenAimShootAndParkStillFit() {
        timer.start();
        List<Pose> toSpot = Arrays.asList(new Pose(33.0, 15.0, 0.0), new Pose(33.0, 110.0, 0.0),
                new Pose(46.8, 118.4, 0.0));
        List<Pose> park = Arrays.asList(new Pose(46.8, 118.4, 0.0), new Pose(11.1, 95.4, 0.0));
        clock.advance(12.0);
        assertTrue(planner.hasTimeForOtherCell(toSpot, park));
        clock.advance(10.0); // 22 s: too late
        assertFalse(planner.hasTimeForOtherCell(toSpot, park));
    }

    @Test
    public void turnEstimateIgnoresDirection() {
        assertEquals(AutoPlanner.estimateTurnSeconds(90.0), AutoPlanner.estimateTurnSeconds(-90.0), EPS);
        assertEquals(0.0, AutoPlanner.estimateTurnSeconds(0.0), EPS);
    }

    @Test
    public void emptyVolleyStillNeedsSpinUp() {
        assertEquals(AutoConstants.SPIN_UP_SECONDS, AutoPlanner.estimateShootSeconds(0), EPS);
    }
}
