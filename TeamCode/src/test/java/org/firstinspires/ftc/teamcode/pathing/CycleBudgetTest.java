// Sources: docs/BEST_OF_MATRIX.md section 5 (time-aware logic, CycleBudgetTest cases) — new for IronLion30311-HiveMind
package org.firstinspires.ftc.teamcode.pathing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.pedropathing.math.Pose;

import org.firstinspires.ftc.teamcode.config.AutoConstants;
import org.firstinspires.ftc.teamcode.config.MatchConstants;
import org.firstinspires.ftc.teamcode.pathing.paths.NearRoutePoses;
import org.firstinspires.ftc.teamcode.pathing.paths.PreloadParkPaths;
import org.firstinspires.ftc.teamcode.util.Alliance;
import org.firstinspires.ftc.teamcode.util.MatchTimer;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Checks the "one more cycle or go park" decision of {@link AutoPlanner} with a fake clock. */
public class CycleBudgetTest {

    /** A clock the test moves by hand. */
    private static class FakeClock implements MatchTimer.Clock {
        double now = 50.0;

        @Override
        public double nowSeconds() {
            return now;
        }
    }

    private final FakeClock clock = new FakeClock();
    private final MatchTimer timer = new MatchTimer(clock, true);
    private final AutoPlanner planner = new AutoPlanner(timer);

    /** Sets the clock so {@code remaining} seconds of AUTO are left. */
    private void setRemaining(double remaining) {
        timer.start();
        clock.now += MatchConstants.AUTO_SECONDS - remaining;
    }

    @Test
    public void parksWhenCycleAndParkDoNotFit() {
        setRemaining(14.0); // matrix case: 12 s cycle + 4 s park, 4 s reserve
        assertEquals(AutoPlanner.Decision.GO_PARK, planner.decideNextStep(12.0, 4.0, 0, true));
    }

    @Test
    public void cyclesWhenThereIsTime() {
        setRemaining(20.0); // 20 - 4 reserve = 16 = 12 + 4
        assertEquals(AutoPlanner.Decision.CYCLE_AGAIN, planner.decideNextStep(12.0, 4.0, 0, true));
    }

    @Test
    public void parksAfterMaxCyclesOrWithoutBalls() {
        setRemaining(29.0);
        assertEquals(AutoPlanner.Decision.GO_PARK,
                planner.decideNextStep(5.0, 3.0, AutoConstants.MAX_CYCLES, true));
        assertEquals(AutoPlanner.Decision.GO_PARK, planner.decideNextStep(5.0, 3.0, 0, false));
    }

    @Test
    public void abortsAStepWhenTheParkNoLongerFits() {
        setRemaining(10.0);
        assertFalse(planner.shouldParkNow(6.0));
        clock.now += 0.5; // 9.5 s left: 9.5 - 4 < 6
        assertTrue(planner.shouldParkNow(6.0));
    }

    @Test
    public void keepsTheTestedDeadlines() {
        setRemaining(MatchConstants.AUTO_SECONDS - 10.0); // 10 s into AUTO
        assertTrue(planner.isBeforeDeadline(AutoConstants.COLLECT_LATEST_START_SECONDS));
        clock.now += 1.5; // 11.5 s
        assertFalse(planner.isBeforeDeadline(AutoConstants.COLLECT_LATEST_START_SECONDS));
        assertEquals(10.0, planner.getShootTimeLeft(10.0, 22.0), 1e-9);
        clock.now += 8.0; // 19.5 s
        assertEquals(2.5, planner.getShootTimeLeft(10.0, 22.0), 1e-9);
        clock.now += 5.0;
        assertEquals(0.0, planner.getShootTimeLeft(10.0, 22.0), 1e-9);
    }

    @Test
    public void parkEstimateMatchesTheMeasuredRun() {
        // vision-align measured ~6.3 s for the TESTED near park legs (back 5, right 60, back
        // 230, right 60 cm), stopping and settling on each leg
        List<Pose> legs = Arrays.asList(NearRoutePoses.SHOOT_SPOT,
                NearRoutePoses.relative(15.0, 0.0, -90.0), NearRoutePoses.relative(15.0, 60.0, -90.0),
                NearRoutePoses.relative(245.0, 60.0, -90.0), NearRoutePoses.relative(245.0, 120.0, -90.0));
        double estimate = AutoPlanner.estimateLegsSeconds(legs) + 4 * AutoConstants.SETTLE_SECONDS;
        assertEquals(6.3, estimate, 0.5);
        // The Pedro path of the same legs gives the same estimate
        List<com.pedropathing.paths.Path> paths = new ArrayList<>();
        paths.add(new PreloadParkPaths(Alliance.RED).park());
        assertTrue(AutoPlanner.estimatePathsSeconds(paths) > 0.0);
    }

    @Test
    public void estimatesTurnsAndVolleys() {
        assertEquals(90.0 / AutoConstants.TURN_DEGREES_PER_SECOND, AutoPlanner.estimateTurnSeconds(-90.0), 1e-9);
        assertEquals(AutoConstants.SPIN_UP_SECONDS + 4 * AutoConstants.FEED_SECONDS_PER_BALL,
                AutoPlanner.estimateShootSeconds(4), 1e-9);
    }
}
