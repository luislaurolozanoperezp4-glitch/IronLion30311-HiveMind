// Sources: IronLion30311BIOBUZZInicioDeTemporada / branch feature/turrubiates/vision-align / TeamCode/.../opmodes/auto/PreloadRouteAuto.java @ a7b8f4c (runCollectRoutine time guards: COLLECT_LATEST_START_SEC, OTHER_CELL_LATEST_START_SEC, shootTimeLeft)
//          IronLion30311BIOBUZZInicioDeTemporada / branch feature/turrubiates/vision-align / TeamCode/.../opmodes/auto/PedroAutoBase.java, PreloadRouteAuto.java @ 1e723f2 (fitted leg time model estimateRouteSec, parkDeadlineSec, parkPower, volleyTimeLeft, hasTimeForOtherCell)
//          IronLion30311BIOBUZZInicioDeTemporada / branch feature/theautonomous / TeamCode/.../MatchClock.java, TimeModel.java @ 125df22 (idea: estimate each phase, leave when the park no longer fits)
//          FTC22526FondyFireTech/Biobuzz_PedroPathing_V3 / branch main / opmodes/auto/LowHiveStartShootPickupShootPark.java (idea: "if the HIVE has not changed by a deadline, go straight to park"; BSD-3-Clause-Clear, idea only, no code copied)
//          — adapted for BIOBUZZ
package org.firstinspires.ftc.teamcode.pathing;

import com.pedropathing.math.Pose;
import com.pedropathing.paths.Path;

import org.firstinspires.ftc.teamcode.config.AutoConstants;
import org.firstinspires.ftc.teamcode.config.DriveConstants;
import org.firstinspires.ftc.teamcode.config.LauncherConstants;
import org.firstinspires.ftc.teamcode.config.MatchConstants;
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
 * <b>Park time budget</b> (vision-align 1e723f2): the robot must be parked
 * {@link AutoConstants#PARK_MARGIN_SECONDS} before the end ({@link #getParkDeadlineSeconds()}).
 * {@link #getVolleyTimeLeft} ends a volley when only the park time is left,
 * {@link #getParkPower} drives the park faster when it is late, and
 * {@link #hasTimeForOtherCell} only sends the robot to the other cell if it can get there, aim,
 * shoot a little and still park in time.
 * <p>
 * The estimates use the leg time model FITTED on the 2026-11-24 runs: a route takes
 * {@code ESTIMATE_ROUTE_FIXED_SECONDS} plus, per leg, {@code ESTIMATE_PER_LEG_SECONDS} +
 * inches / ({@code ESTIMATE_INCHES_PER_SECOND_PER_POWER} x power).
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
    // Park time budget (vision-align 1e723f2)
    // ------------------------------------------------------------------------------------------

    /** @return the match time by which the robot must be parked (30 s minus the park margin) */
    public static double getParkDeadlineSeconds() {
        return MatchConstants.AUTO_SECONDS - AutoConstants.PARK_MARGIN_SECONDS;
    }

    /**
     * Top power for the park legs: the normal {@code AUTO_MAX_POWER} when there is time, more (up
     * to {@link AutoConstants#PARK_MAX_POWER}) when the park would otherwise end after the deadline.
     *
     * @param parkRoute where the robot is, then the park waypoints (the last is PARK)
     * @return the power to use for every park leg
     */
    public double getParkPower(List<Pose> parkRoute) {
        int legs = Math.max(0, parkRoute.size() - 1);
        double timeLeft = getParkDeadlineSeconds() - timer.getElapsedSeconds();
        double driveSeconds = timeLeft - AutoConstants.ESTIMATE_ROUTE_FIXED_SECONDS
                - AutoConstants.ESTIMATE_PER_LEG_SECONDS * legs;
        double needed = driveSeconds <= 0.0 ? AutoConstants.PARK_MAX_POWER
                : routeInches(parkRoute) / (AutoConstants.ESTIMATE_INCHES_PER_SECOND_PER_POWER * driveSeconds);
        return Math.max(DriveConstants.AUTO_MAX_POWER, Math.min(AutoConstants.PARK_MAX_POWER, needed));
    }

    /**
     * Time a volley may last so the park after it (at the fastest park power, plus a settle)
     * still ends by the park deadline. "The park comes first."
     *
     * @param parkRoute where the robot shoots, then the park waypoints
     * @return seconds, at most {@link LauncherConstants#SHOOT_MAX_SECONDS}, never negative
     */
    public double getVolleyTimeLeft(List<Pose> parkRoute) {
        double seconds = getParkDeadlineSeconds() - AutoConstants.SETTLE_SECONDS
                - timer.getElapsedSeconds() - estimateRouteSeconds(AutoConstants.PARK_MAX_POWER, parkRoute);
        return Math.max(0.0, Math.min(LauncherConstants.SHOOT_MAX_SECONDS, seconds));
    }

    /**
     * Whether there is time to drive to the other cell, look, aim, shoot at least
     * {@link AutoConstants#OTHER_CELL_MIN_SHOOT_SECONDS} and park from there by the deadline.
     *
     * @param routeToSpot where the robot is, then the waypoints to the other-cell spot
     * @param parkRoute   the other-cell spot, then the park waypoints from there
     * @return true to go to the other cell, false to park instead
     */
    public boolean hasTimeForOtherCell(List<Pose> routeToSpot, List<Pose> parkRoute) {
        return getOtherCellParkedSeconds(routeToSpot, parkRoute) <= getParkDeadlineSeconds();
    }

    /**
     * @return the match time at which the robot would be parked after a short other-cell volley
     *         (for the log line of the decision)
     */
    public double getOtherCellParkedSeconds(List<Pose> routeToSpot, List<Pose> parkRoute) {
        double arrival = timer.getElapsedSeconds()
                + estimateRouteSeconds(DriveConstants.AUTO_MAX_POWER, routeToSpot) + AutoConstants.SETTLE_SECONDS;
        return arrival + AutoConstants.OTHER_CELL_LOOK_SECONDS + AutoConstants.OTHER_CELL_AIM_SECONDS
                + AutoConstants.OTHER_CELL_MIN_SHOOT_SECONDS
                + estimateRouteSeconds(AutoConstants.PARK_MAX_POWER, parkRoute) + AutoConstants.SETTLE_SECONDS;
    }

    // ------------------------------------------------------------------------------------------
    // Estimates (FITTED leg time model)
    // ------------------------------------------------------------------------------------------

    /**
     * @param distanceInches straight-line distance of one leg
     * @param maxPower       top power of the leg (0 to 1)
     * @return estimated seconds for that leg, its corner/stop overhead included (no route overhead)
     */
    public static double estimateLegSeconds(double distanceInches, double maxPower) {
        return AutoConstants.ESTIMATE_PER_LEG_SECONDS
                + distanceInches / (AutoConstants.ESTIMATE_INCHES_PER_SECOND_PER_POWER * maxPower);
    }

    /**
     * @param distanceInches straight-line distance of one leg
     * @return estimated seconds for that leg at {@code AUTO_MAX_POWER}
     */
    public static double estimateLegSeconds(double distanceInches) {
        return estimateLegSeconds(distanceInches, DriveConstants.AUTO_MAX_POWER);
    }

    /**
     * @param maxPower  top power of every leg
     * @param waypoints the poses the robot drives through, in order (the first is where it is)
     * @return estimated seconds for the whole route (0 if there is no leg)
     */
    public static double estimateRouteSeconds(double maxPower, List<Pose> waypoints) {
        if (waypoints.size() < 2) {
            return 0.0;
        }
        double seconds = AutoConstants.ESTIMATE_ROUTE_FIXED_SECONDS;
        for (int i = 1; i < waypoints.size(); i++) {
            seconds += estimateLegSeconds(OdometryDriver.distance(waypoints.get(i - 1), waypoints.get(i)), maxPower);
        }
        return seconds;
    }

    /**
     * @param waypoints the poses the robot drives through, in order (the first is where it is)
     * @return estimated seconds for the route at {@code AUTO_MAX_POWER}
     */
    public static double estimateLegsSeconds(List<Pose> waypoints) {
        return estimateRouteSeconds(DriveConstants.AUTO_MAX_POWER, waypoints);
    }

    private static double routeInches(List<Pose> waypoints) {
        double inches = 0.0;
        for (int i = 1; i < waypoints.size(); i++) {
            inches += OdometryDriver.distance(waypoints.get(i - 1), waypoints.get(i));
        }
        return inches;
    }

    /**
     * @param paths Pedro paths followed one after the other
     * @return estimated seconds, from the length of each path
     */
    public static double estimatePathsSeconds(List<Path> paths) {
        if (paths.isEmpty()) {
            return 0.0;
        }
        double seconds = AutoConstants.ESTIMATE_ROUTE_FIXED_SECONDS;
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
