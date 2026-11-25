// Sources: new for IronLion30311-HiveMind (QA tests for pathing/AutoPlanner deadlines and estimates)
package org.firstinspires.ftc.teamcode.pathing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.pedropathing.math.Pose;

import org.firstinspires.ftc.teamcode.config.AutoConstants;
import org.firstinspires.ftc.teamcode.testutil.FakeClock;
import org.firstinspires.ftc.teamcode.util.MatchTimer;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

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
        double expected = (50.0 + 40.0) / AutoConstants.CRUISE_INCHES_PER_SECOND
                + 2 * AutoConstants.LEG_OVERHEAD_SECONDS;
        assertEquals(expected, legs, EPS);
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
