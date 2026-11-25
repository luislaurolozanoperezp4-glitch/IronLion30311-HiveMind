// Sources: IronLion30311BIOBUZZInicioDeTemporada / branch feature/turrubiates/vision-align / TeamCode/.../opmodes/auto/PreloadRouteAuto.java @ a7b8f4c (runCollectRoutine time guards: COLLECT_LATEST_START_SEC, OTHER_CELL_LATEST_START_SEC, shootTimeLeft)
//          IronLion30311BIOBUZZInicioDeTemporada / branch feature/theautonomous / TeamCode/.../MatchClock.java, TimeModel.java @ 125df22 (idea: estimate each phase, leave when the park no longer fits)
//          FTC22526FondyFireTech/Biobuzz_PedroPathing_V3 / branch main / opmodes/auto/LowHiveStartShootPickupShootPark.java (idea: "if the HIVE has not changed by a deadline, go straight to park"; BSD-3-Clause-Clear, idea only, no code copied)
//          — adapted for BIOBUZZ
package org.firstinspires.ftc.teamcode.pathing;

import com.pedropathing.math.Pose;
import com.pedropathing.paths.Path;

import org.firstinspires.ftc.teamcode.config.AutoConstants;
import org.firstinspires.ftc.teamcode.util.MatchTimer;

import java.util.List;

/**
 * Decides, from the time left in AUTO, whether the robot has time for one more stop (collect and
 * shoot) or must head to PARK now. Pure logic, no hardware, so JUnit can test it with a fake clock.
 * <p>
 * Rule (after each scoring step):
 * <pre>
 *   cycle + park after the cycle fits ({@link MatchTimer#hasTimeFor}, which keeps the park
 *   reserve)  AND  cycles done &lt; MAX_CYCLES  AND  balls are available   -&gt;  CYCLE_AGAIN
 *   otherwise                                                              -&gt;  GO_PARK
 * </pre>
 * During any step, {@link #mustParkNow(double)} says when to abort the step and park.
 * The fixed deadlines tested on the field (for example "collect only if it starts before 11 s")
 * stay as a backstop through {@link #isBeforeDeadline(double)}.
 * <p>
 * The estimate helpers turn distances and turns into seconds with the values in
 * {@link AutoConstants} ({@code TODO: TUNE} from the pose logs).
 */
public class AutoPlanner {

    /** What to do next. */
    public enum Decision {
        /** There is time: run one more collect-and-shoot cycle. */
        CYCLE_AGAIN,
        /** Not enough time (or nothing left to do): drive to the LOADING ZONE now. */
        GO_PARK
    }

    private final MatchTimer timer;

    /**
     * @param timer the AUTO match timer, started when START is pressed
     */
    public AutoPlanner(MatchTimer timer) {
        this.timer = timer;
    }

    /**
     * Decides whether to run another cycle or go park.
     *
     * @param cycleSeconds          estimated time of the next cycle (drive + collect + shoot)
     * @param parkAfterCycleSeconds estimated time to park from where that cycle ends
     * @param cyclesDone            cycles already run after the preload
     * @param hasBallsAvailable     true if there are balls left to collect
     * @return {@link Decision#CYCLE_AGAIN} only if the cycle and the park after it both fit
     */
    public Decision decideNextStep(double cycleSeconds, double parkAfterCycleSeconds,
                                   int cyclesDone, boolean hasBallsAvailable) {
        if (!hasBallsAvailable || cyclesDone >= AutoConstants.MAX_CYCLES) {
            return Decision.GO_PARK;
        }
        return timer.hasTimeFor(cycleSeconds + parkAfterCycleSeconds)
                ? Decision.CYCLE_AGAIN : Decision.GO_PARK;
    }

    /**
     * Call during any step: true when the park from here no longer fits with the reserve, so the
     * step must stop (mechanisms off) and the robot must park.
     *
     * @param parkSeconds estimated time to park from the current pose
     * @return true if the robot must start parking now
     */
    public boolean mustParkNow(double parkSeconds) {
        return !timer.hasTimeFor(parkSeconds);
    }

    /**
     * Fixed deadline check (TESTED backstops such as {@link AutoConstants#COLLECT_LATEST_START_SECONDS}).
     *
     * @param latestMatchSeconds latest match time, seconds since START, at which the step may begin
     * @return true if the match clock has not passed it
     */
    public boolean isBeforeDeadline(double latestMatchSeconds) {
        return timer.getElapsedSeconds() <= latestMatchSeconds;
    }

    /**
     * Time left for a volley that must end by a deadline, capped at {@code maxSeconds}.
     *
     * @param maxSeconds         longest volley allowed
     * @param latestEndSeconds   match time by which the volley must end
     * @return seconds the volley may take (0 if the deadline already passed)
     */
    public double getShootTimeLeft(double maxSeconds, double latestEndSeconds) {
        return Math.max(0.0, Math.min(maxSeconds, latestEndSeconds - timer.getElapsedSeconds()));
    }

    // ------------------------------------------------------------------------------------------
    // Estimates
    // ------------------------------------------------------------------------------------------

    /**
     * @param distanceInches straight-line distance of one leg
     * @return estimated seconds for that leg, overhead included
     */
    public static double estimateLegSeconds(double distanceInches) {
        return distanceInches / AutoConstants.CRUISE_INCHES_PER_SECOND
                + AutoConstants.LEG_OVERHEAD_SECONDS;
    }

    /**
     * @param waypoints the poses the robot drives through, in order (the first is where it is)
     * @return estimated seconds for all the legs between them
     */
    public static double estimateLegsSeconds(List<Pose> waypoints) {
        double seconds = 0.0;
        for (int i = 1; i < waypoints.size(); i++) {
            seconds += estimateLegSeconds(OdometryDriver.distance(waypoints.get(i - 1), waypoints.get(i)));
        }
        return seconds;
    }

    /**
     * @param paths Pedro paths followed one after the other
     * @return estimated seconds, from the length of each path
     */
    public static double estimatePathsSeconds(List<Path> paths) {
        double seconds = 0.0;
        for (Path path : paths) {
            seconds += estimateLegSeconds(path.curve.length());
        }
        return seconds;
    }

    /**
     * @param degrees size of an in-place turn (sign ignored)
     * @return estimated seconds for the turn
     */
    public static double estimateTurnSeconds(double degrees) {
        return Math.abs(degrees) / AutoConstants.TURN_DEGREES_PER_SECOND;
    }

    /**
     * @param balls balls in the volley
     * @return estimated seconds for spin-up and feeding
     */
    public static double estimateShootSeconds(int balls) {
        return AutoConstants.SPIN_UP_SECONDS + balls * AutoConstants.FEED_SECONDS_PER_BALL;
    }
}
