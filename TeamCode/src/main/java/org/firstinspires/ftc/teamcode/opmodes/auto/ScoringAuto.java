// Sources: IronLion30311BIOBUZZInicioDeTemporada / branch feature/turrubiates/vision-align / TeamCode/.../opmodes/auto/PreloadRouteAuto.java @ a7b8f4c (runRoutine, runCollectRoutine, returnAndCheckTip, parkFromShootSide, shootAtOtherCellAndPark, shootTimeLeft) @ 4fd2293 (bearing + distance reference, aimAtCell) @ 1e723f2 (volleyTimeLeft, hasTimeForOtherCell, parkPower)
//          IronLion30311BIOBUZZInicioDeTemporada / branch feature/turrubiates/vision-align / TeamCode/.../opmodes/auto/PedroAutoBase.java @ f26f8d0 (aimAtCell, bearingDeg)
//          docs/BEST_OF_MATRIX.md section 5 (time-aware cycle loop; Far Cycle from the other-cell spot)
//          — adapted for BIOBUZZ (non-blocking steps; AutoPlanner decides "cycle again" or "go park")
package org.firstinspires.ftc.teamcode.opmodes.auto;

import com.pedropathing.math.Pose;

import org.firstinspires.ftc.teamcode.config.AutoConstants;
import org.firstinspires.ftc.teamcode.config.DriveConstants;
import org.firstinspires.ftc.teamcode.config.LauncherConstants;
import org.firstinspires.ftc.teamcode.config.MatchConstants;
import org.firstinspires.ftc.teamcode.pathing.AutoPlanner;
import org.firstinspires.ftc.teamcode.pathing.DriveRoute;
import org.firstinspires.ftc.teamcode.pathing.paths.NearRoutePoses;
import org.firstinspires.ftc.teamcode.pathing.paths.OtherCellPaths;
import org.firstinspires.ftc.teamcode.pathing.paths.PreloadCollectPaths;
import org.firstinspires.ftc.teamcode.pathing.paths.PreloadParkPaths;
import org.firstinspires.ftc.teamcode.subsystems.vision.HiveCell;
import org.firstinspires.ftc.teamcode.subsystems.vision.HiveClusterPose;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.function.DoubleSupplier;

/**
 * The scoring routine of the competition autos: the TESTED near route (vision-align
 * {@code PreloadRouteAuto} / {@code PreloadCollectAuto}) plus a time-aware choice after every
 * volley.
 * <p>
 * <b>NEAR start</b> (57, 9, -90 deg), TESTED 4/4 preload:
 * <ol>
 *     <li>LEAVE 10 cm, measure our scoring cell's bearing and distance (the aim reference), shoot
 *     the 4 preloaded POLLEN at 2950 RPM (ends by 22 s).</li>
 *     <li>Decision ({@link AutoPlanner}): CYCLE_AGAIN if a collect cycle plus the park after it
 *     still fit (with the 4 s park reserve) and the volley ended before 11 s (TESTED backstop);
 *     otherwise GO_PARK with the near park legs (back 5, right 60, back 204.5, right 56.5 cm: the
 *     TESTED legs with the long leg ending at the zone's low edge and the last leg kept 2 in off
 *     the wall).</li>
 *     <li>Cycle: early TIP watch on our scoring cell (up to 4 s, shorter if time is short), turn
 *     90 deg, collect the ball row along the audience wall slowly, then
 *     <ul>
 *         <li>TIP seen: around the HIVE to the angled other-cell spot, if the time model says the
 *         robot can still aim, shoot a little and park; aim with the Limelight, shoot, park;</li>
 *         <li>no TIP: back to the shooting spot, re-center, TIP check; up = shoot here and ask
 *         the planner again (it parks: {@code MAX_CYCLES} = 1); down = other cell (same time
 *         check).</li>
 *     </ul></li>
 * </ol>
 * Volleys after the preload end when only the park time is left, and the park legs speed up
 * when late (vision-align 1e723f2, {@link AutoPlanner#getVolleyTimeLeft},
 * {@link AutoPlanner#getParkPower}).
 * <p>
 * <b>FAR start</b> (8.96, 131.19, 90 deg), <b>UNTESTED</b> (decision D21): drive to the
 * other-cell spot north of the HIVE; if that cell is seen, aim and shoot the preload there;
 * otherwise go down the west lane to the near shooting spot and shoot there. Then the same
 * decisions.
 * <p>
 * The time guard in {@link AutoBase} parks the robot from anywhere if a step runs long.
 */
public abstract class ScoringAuto extends AutoBase {

    private int cyclesDone;
    /** Bearing of our scoring cell at the first volley (the one that scores), degrees. */
    private double shootBearingDegrees = Double.NaN;
    /** Camera-to-scoring-cell distance at the first volley, meters. */
    private double shootDistanceMeters = Double.NaN;

    @Override
    protected void buildRoutine(StepSequence sequence) {
        if (getStartPosition() == StartPosition.FAR) {
            buildFarOpening(sequence);
        } else {
            buildNearOpening(sequence);
        }
    }

    @Override
    protected String describeRoute() {
        if (getStartPosition() == StartPosition.FAR) {
            return "FAR (UNTESTED): other cell or west lane -> shoot 4 -> planner: cycle or park";
        }
        return "NEAR (TESTED 4/4): LEAVE -> shoot 4 -> planner: collect cycle or park";
    }

    // ------------------------------------------------------------------------------------------
    // Openings
    // ------------------------------------------------------------------------------------------

    private void buildNearOpening(StepSequence sequence) {
        sequence.add(
                driveTo("LEAVE back " + AutoConstants.LEAVE_BACK_CM + " cm", field(NearRoutePoses.SHOOT_SPOT), Mode.IDLE),
                settle(AutoConstants.SETTLE_SECONDS, Mode.IDLE),
                AutoStep.instant("Measure HIVE bearing and distance", new Runnable() {
                    @Override
                    public void run() {
                        HiveClusterPose scoring = getCellPose(HiveCell.getBackCell(getAlliance()));
                        if (scoring != null) {
                            shootBearingDegrees = bearingDegrees(scoring);
                            shootDistanceMeters = distanceMeters(scoring);
                        }
                    }
                }),
                shoot("Preload volley", LauncherConstants.PRELOAD_BALLS, LauncherConstants.POLLEN_AUTO_RPM,
                        shootTimeLeft(AutoConstants.LATEST_SHOOT_END_SECONDS)),
                AutoStep.instant("Decide after volley", new Runnable() {
                    @Override
                    public void run() {
                        decideAfterShootSpotVolley();
                    }
                }));
    }

    private void buildFarOpening(StepSequence sequence) {
        setSpinUpRpm(AutoConstants.OTHER_CELL_POLLEN_RPM);
        sequence.addAll(routeLegs(DriveRoute.OTHER_CELL_TRANSIT, "To other cell",
                Arrays.asList(field(NearRoutePoses.NORTH_WAYPOINT), field(NearRoutePoses.OTHER_SPOT)), Mode.SPIN_UP));
        sequence.add(
                settle(AutoConstants.SETTLE_SECONDS, Mode.SPIN_UP),
                lookForCell("Other cell", HiveCell.getAudienceCell(getAlliance()), AutoConstants.OTHER_CELL_LOOK_SECONDS),
                AutoStep.instant("Decide at other cell", new Runnable() {
                    @Override
                    public void run() {
                        if (canShootAtOtherCell()) {
                            getSequence().addNext(otherCellVolleySteps(LauncherConstants.PRELOAD_BALLS));
                        } else {
                            getSequence().note("Other cell not seen: shooting from the near spot");
                            getSequence().addNext(otherSpotToShootSpotSteps(LauncherConstants.PRELOAD_BALLS));
                        }
                    }
                }));
    }

    // ------------------------------------------------------------------------------------------
    // Decisions
    // ------------------------------------------------------------------------------------------

    /** After a volley at the near shooting spot: one more collect cycle, or park. */
    private void decideAfterShootSpotVolley() {
        double cycleSeconds = estimateNearCycleSeconds();
        double parkSeconds = estimateParkFromShootSideSeconds();
        AutoPlanner.Decision decision = getPlanner().decideNextStep(cycleSeconds, parkSeconds, cyclesDone, true);
        boolean isBeforeDeadline = getPlanner().isBeforeDeadline(AutoConstants.COLLECT_LATEST_START_SECONDS);
        boolean isCycling = decision == AutoPlanner.Decision.CYCLE_AGAIN && isBeforeDeadline;
        getSequence().note(String.format(Locale.US,
                "Planner at %.1f s: %s (cycle ~%.1f s + park ~%.1f s, cycles done %d, before %.0f s: %s)",
                getMatchTimer().getElapsedSeconds(), isCycling ? "CYCLE_AGAIN" : "GO_PARK",
                cycleSeconds, parkSeconds, cyclesDone, AutoConstants.COLLECT_LATEST_START_SECONDS, isBeforeDeadline));
        if (isCycling) {
            getSequence().addNext(nearCycleSteps(cycleSeconds, parkSeconds));
        } else if (cyclesDone == 0) {
            // The TESTED park legs, straight from the preload volley
            getSequence().addNext(timedParkSteps(DriveRoute.PRELOAD_PARK, "Park",
                    new PreloadParkPaths(getAlliance()).getParkWaypoints(), true));
        } else {
            getSequence().addNext(timedParkSteps(DriveRoute.CYCLE_PARK, "Park",
                    new PreloadCollectPaths(getAlliance()).getParkWaypoints(), false));
        }
    }

    /** After the collect row: other cell (TIP seen early, if it fits in time), or back to the shooting spot. */
    private void decideAfterCollect() {
        cyclesDone++;
        getSequence().note("Collect: " + getBallSummary());
        if (!wasTipDown()) {
            getSequence().addNext(returnAndTipCheckSteps());
            return;
        }
        List<Pose> waypoints = new OtherCellPaths(getAlliance()).getFromCollectWaypoints();
        if (hasTimeForOtherCell(waypoints)) {
            List<AutoStep> steps = new ArrayList<>();
            // Away from the red wall first (no turn there), then turning north on the way
            steps.addAll(routeLegs(DriveRoute.OTHER_CELL_TRANSIT, "To other cell",
                    Arrays.asList(waypoints.get(0), waypoints.get(1), field(NearRoutePoses.OTHER_SPOT)), Mode.SPIN_UP));
            steps.addAll(otherCellSteps(AutoConstants.COLLECT_BALLS));
            getSequence().addNext(steps);
        } else {
            // Too late for the other cell. VA drove the south-west diagonal from the wall here;
            // the emergency park keeps the heading instead (no turn next to the wall).
            getSequence().addNext(timedParkSteps(DriveRoute.EMERGENCY_PARK, "Park",
                    EmergencyPark.plan(getPose(), getAlliance()), false));
        }
    }

    /** Back on the shooting spot: the TIP check decides shoot here or go to the other cell. */
    private void decideTipCheck() {
        boolean isOnSpot = isNear(field(NearRoutePoses.SHOOT_SPOT), AutoConstants.TIP_CHECK_POSITION_TOLERANCE_CM,
                AutoConstants.TIP_CHECK_HEADING_TOLERANCE_DEGREES);
        if (!isOnSpot) {
            getSequence().note("TIP check SKIPPED: not on the shoot spot, shooting anyway");
            getSequence().addNext(shootSpotVolleySteps("Row volley", AutoConstants.COLLECT_BALLS));
            return;
        }
        getSequence().addNext(
                lookForCell("TIP check", HiveCell.getBackCell(getAlliance()), AutoConstants.TIP_CHECK_SECONDS),
                AutoStep.instant("Decide after TIP check", new Runnable() {
                    @Override
                    public void run() {
                        List<Pose> waypoints = new OtherCellPaths(getAlliance()).getFromShootSpotWaypoints();
                        if (wasCellSeen()) {
                            getSequence().addNext(shootSpotVolleySteps("Row volley", AutoConstants.COLLECT_BALLS));
                        } else if (hasTimeForOtherCell(waypoints)) {
                            List<AutoStep> steps = new ArrayList<>();
                            // Turning to the other-cell heading on the way, south-west of the HIVE
                            steps.addAll(routeLegs(DriveRoute.OTHER_CELL_TRANSIT, "To other cell",
                                    Arrays.asList(waypoints.get(0), waypoints.get(1),
                                            field(NearRoutePoses.OTHER_SPOT)), Mode.SPIN_UP));
                            steps.addAll(otherCellSteps(AutoConstants.COLLECT_BALLS));
                            getSequence().addNext(steps);
                        } else {
                            getSequence().addNext(timedParkSteps(DriveRoute.CYCLE_PARK, "Park",
                                    new PreloadCollectPaths(getAlliance()).getParkWaypoints(), false));
                        }
                    }
                }));
    }

    /** At the other-cell spot after looking: aim and shoot there, or park. */
    private void decideAtOtherCell(int balls) {
        if (canShootAtOtherCell()) {
            getSequence().addNext(otherCellVolleySteps(balls));
        } else {
            getSequence().addNext(parkFromOtherSideSteps());
        }
    }

    /** After a volley at the other cell: one more cycle (down the west lane), or park. */
    private void decideAfterOtherVolley() {
        double cycleSeconds = estimateOtherSpotToShootSpotSeconds() + estimateNearCycleSeconds();
        double parkSeconds = estimateParkFromShootSideSeconds();
        AutoPlanner.Decision decision = getPlanner().decideNextStep(cycleSeconds, parkSeconds, cyclesDone, true);
        boolean isBeforeDeadline = getPlanner().isBeforeDeadline(AutoConstants.COLLECT_LATEST_START_SECONDS);
        boolean isCycling = decision == AutoPlanner.Decision.CYCLE_AGAIN && isBeforeDeadline;
        getSequence().note(String.format(Locale.US, "Planner at %.1f s (other cell): %s (cycle ~%.1f s + park ~%.1f s)",
                getMatchTimer().getElapsedSeconds(), isCycling ? "CYCLE_AGAIN" : "GO_PARK", cycleSeconds, parkSeconds));
        if (isCycling) {
            List<AutoStep> steps = otherSpotLegsToShootSpot(Mode.IDLE);
            steps.addAll(nearCycleSteps(estimateNearCycleSeconds(), parkSeconds));
            getSequence().addNext(steps);
        } else {
            getSequence().addNext(parkFromOtherSideSteps());
        }
    }

    // ------------------------------------------------------------------------------------------
    // Step groups
    // ------------------------------------------------------------------------------------------

    /** Collect cycle from the shooting spot (heading -90): TIP watch, turn, collect, decide. */
    private List<AutoStep> nearCycleSteps(final double cycleSeconds, final double parkSeconds) {
        List<Pose> collect = new PreloadCollectPaths(getAlliance()).getCollectWaypoints();
        return new ArrayList<>(Arrays.asList(
                watchForTip("Early TIP watch", HiveCell.getBackCell(getAlliance()), new DoubleSupplier() {
                    @Override
                    public double getAsDouble() {
                        // Only the slack left after the cycle and the park (the watch is optional)
                        double slack = getMatchTimer().getRemainingSeconds() - MatchConstants.AUTO_PARK_RESERVE_SECONDS
                                - cycleSeconds - parkSeconds;
                        return Math.max(0.0, Math.min(AutoConstants.EARLY_TIP_WATCH_SECONDS, slack));
                    }
                }, AutoConstants.EARLY_TIP_GONE_SECONDS),
                turnTo("Turn " + AutoConstants.COLLECT_TURN_DEGREES + " deg CW",
                        fieldHeading(NearRoutePoses.COLLECT_HEADING_DEG), Mode.IDLE),
                driveThrough("To balls " + AutoConstants.COLLECT_START_CM + " cm", collect.get(0), Mode.IDLE),
                driveTo("Collect to " + AutoConstants.COLLECT_END_CM + " cm", collect.get(1),
                        AutoConstants.COLLECT_POWER, Mode.INTAKE),
                // Intake still on while settling, so the last ball goes in and gets counted
                settle(AutoConstants.SETTLE_SECONDS, Mode.INTAKE),
                AutoStep.instant("Decide after collect", new Runnable() {
                    @Override
                    public void run() {
                        decideAfterCollect();
                    }
                })));
    }

    /** Back to the shooting spot spinning up, turn back, re-center, then the TIP check. */
    private List<AutoStep> returnAndTipCheckSteps() {
        setSpinUpRpm(LauncherConstants.POLLEN_AUTO_RPM);
        return new ArrayList<>(Arrays.asList(
                driveTo("Back to shoot spot", field(NearRoutePoses.SHOOT_SPOT_FACING_COLLECT),
                        AutoConstants.RETURN_POWER, Mode.SPIN_UP),
                turnTo("Turn back", fieldHeading(NearRoutePoses.START_HEADING_DEG), Mode.SPIN_UP),
                // The turn moved the robot 8-14 cm off the spot in every run: drive back onto it
                driveTo("Re-center on shoot spot", field(NearRoutePoses.SHOOT_SPOT), Mode.SPIN_UP),
                settle(AutoConstants.SETTLE_SECONDS, Mode.SPIN_UP),
                AutoStep.instant("TIP check", new Runnable() {
                    @Override
                    public void run() {
                        decideTipCheck();
                    }
                })));
    }

    /** A volley at the near shooting spot (ends when only the park time is left), then the planner decides again. */
    private List<AutoStep> shootSpotVolleySteps(String name, int balls) {
        return new ArrayList<>(Arrays.asList(
                shoot(name, balls, LauncherConstants.POLLEN_AUTO_RPM,
                        volleyTimeLeft(new PreloadCollectPaths(getAlliance()).getParkWaypoints())),
                AutoStep.instant("Decide after volley", new Runnable() {
                    @Override
                    public void run() {
                        decideAfterShootSpotVolley();
                    }
                })));
    }

    /** At the other-cell spot (the caller drove there with routeLegs): look for the cell, decide. */
    private List<AutoStep> otherCellSteps(final int balls) {
        setSpinUpRpm(AutoConstants.OTHER_CELL_POLLEN_RPM);
        return new ArrayList<>(Arrays.asList(
                settle(AutoConstants.SETTLE_SECONDS, Mode.SPIN_UP),
                lookForCell("Other cell", HiveCell.getAudienceCell(getAlliance()), AutoConstants.OTHER_CELL_LOOK_SECONDS),
                AutoStep.instant("Decide at other cell", new Runnable() {
                    @Override
                    public void run() {
                        decideAtOtherCell(balls);
                    }
                })));
    }

    /** At the other-cell spot with the cell seen: Limelight aim, volley (park first), decide. */
    private List<AutoStep> otherCellVolleySteps(int balls) {
        List<AutoStep> steps = aimSteps(HiveCell.getAudienceCell(getAlliance()), getAimBearingDegrees(),
                getAimDistanceMeters(), AutoConstants.AIM_MAX_TURN_DEGREES);
        steps.add(shoot("Other cell volley", balls, AutoConstants.OTHER_CELL_POLLEN_RPM,
                volleyTimeLeft(otherSideParkWaypoints())));
        steps.add(AutoStep.instant("Decide after other volley", new Runnable() {
            @Override
            public void run() {
                decideAfterOtherVolley();
            }
        }));
        return steps;
    }

    /** Far start, other cell not seen: down the west lane to the near spot and shoot there. */
    private List<AutoStep> otherSpotToShootSpotSteps(int balls) {
        setSpinUpRpm(LauncherConstants.POLLEN_AUTO_RPM);
        List<AutoStep> steps = otherSpotLegsToShootSpot(Mode.SPIN_UP);
        steps.add(settle(AutoConstants.SETTLE_SECONDS, Mode.SPIN_UP));
        steps.addAll(shootSpotVolleySteps("Preload volley", balls));
        return steps;
    }

    /** Other-cell spot down the west lane to the shooting spot (turning in the lane). */
    private List<AutoStep> otherSpotLegsToShootSpot(Mode mode) {
        return routeLegs(DriveRoute.OTHER_CELL_TRANSIT, "To shoot spot",
                Arrays.asList(field(NearRoutePoses.NORTH_WAYPOINT), field(NearRoutePoses.SOUTH_WAYPOINT),
                        field(NearRoutePoses.SHOOT_SPOT)), mode);
    }

    /** One diagonal north of the HIVE into PARK (the park leg of the other-cell route). */
    private List<AutoStep> parkFromOtherSideSteps() {
        return timedParkSteps(DriveRoute.OTHER_SIDE_PARK, "Park from other side", otherSideParkWaypoints(), false);
    }

    // ------------------------------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------------------------------

    private List<Pose> otherSideParkWaypoints() {
        return Collections.singletonList(field(NearRoutePoses.OTHER_PARK));
    }

    /** At the other cell: seen, and a volley longer than the aim still fits before the park. */
    private boolean canShootAtOtherCell() {
        List<Pose> parkRoute = new ArrayList<>();
        parkRoute.add(getPose());
        parkRoute.addAll(otherSideParkWaypoints());
        return wasCellSeen() && getPlanner().getVolleyTimeLeft(parkRoute) > AutoConstants.OTHER_CELL_AIM_SECONDS;
    }

    /**
     * Time-model check before going to the other cell (vision-align 1e723f2); logs the plan.
     *
     * @param waypoints the waypoints from here to the other-cell spot (the last is the spot)
     * @return true to go, false to park instead
     */
    private boolean hasTimeForOtherCell(List<Pose> waypoints) {
        List<Pose> route = new ArrayList<>();
        route.add(getPose());
        route.addAll(waypoints);
        List<Pose> park = new ArrayList<>();
        park.add(waypoints.get(waypoints.size() - 1));
        park.addAll(otherSideParkWaypoints());
        boolean hasTime = getPlanner().hasTimeForOtherCell(route, park);
        getSequence().note(String.format(Locale.US, "Other cell plan at %.1f s: parked ~%.1f s (deadline %.1f s) -> %s",
                getMatchTimer().getElapsedSeconds(), getPlanner().getOtherCellParkedSeconds(route, park),
                AutoPlanner.getParkDeadlineSeconds(), hasTime ? "GO" : "PARK instead"));
        return hasTime;
    }

    /** @return a supplier of the volley time left before {@code latestEndSeconds}, read at the volley start */
    private DoubleSupplier shootTimeLeft(final double latestEndSeconds) {
        return new DoubleSupplier() {
            @Override
            public double getAsDouble() {
                return getPlanner().getShootTimeLeft(LauncherConstants.SHOOT_MAX_SECONDS, latestEndSeconds);
            }
        };
    }

    /**
     * @param parkWaypoints the park legs after the volley (field poses)
     * @return a supplier of the volley time that still leaves the park by the deadline, read at the volley start
     */
    private DoubleSupplier volleyTimeLeft(final List<Pose> parkWaypoints) {
        return new DoubleSupplier() {
            @Override
            public double getAsDouble() {
                List<Pose> route = new ArrayList<>();
                route.add(getPose());
                route.addAll(parkWaypoints);
                return getPlanner().getVolleyTimeLeft(route);
            }
        };
    }

    private double getAimBearingDegrees() {
        return Double.isNaN(shootBearingDegrees) ? AutoConstants.DEFAULT_SHOOT_BEARING_DEGREES : shootBearingDegrees;
    }

    private double getAimDistanceMeters() {
        return Double.isNaN(shootDistanceMeters) ? AutoConstants.DEFAULT_SHOOT_DISTANCE_METERS : shootDistanceMeters;
    }

    /**
     * Driving time of a collect cycle up to a short volley (the volley itself is time-boxed).
     *
     * @return seconds
     */
    static double estimateNearCycleSeconds() {
        double toBalls = AutoPlanner.estimateLegSeconds(
                distance(NearRoutePoses.SHOOT_SPOT, NearRoutePoses.COLLECT_START));
        double overBalls = AutoPlanner.estimateLegSeconds(
                distance(NearRoutePoses.COLLECT_START, NearRoutePoses.COLLECT_END), AutoConstants.COLLECT_POWER);
        double back = AutoPlanner.estimateLegSeconds(
                distance(NearRoutePoses.COLLECT_END, NearRoutePoses.SHOOT_SPOT),
                Math.min(DriveConstants.AUTO_MAX_POWER, AutoConstants.RETURN_POWER));
        return AutoConstants.ESTIMATE_ROUTE_FIXED_SECONDS
                + 2.0 * AutoPlanner.estimateTurnSeconds(AutoConstants.COLLECT_TURN_DEGREES)
                + toBalls + overBalls + back + AutoPlanner.estimateLegSeconds(0.0)
                + 2.0 * AutoConstants.SETTLE_SECONDS + AutoConstants.TIP_CHECK_SECONDS
                + AutoPlanner.estimateShootSeconds(AutoConstants.CYCLE_MIN_PLANNED_BALLS);
    }

    /** @return seconds to park from the shooting spot through the south-west waypoint */
    static double estimateParkFromShootSideSeconds() {
        return AutoPlanner.estimateLegsSeconds(Arrays.asList(NearRoutePoses.SHOOT_SPOT,
                NearRoutePoses.SOUTH_WAYPOINT, NearRoutePoses.PARK)) + AutoConstants.SETTLE_SECONDS;
    }

    /** @return seconds from the other-cell spot down the west lane to the shooting spot */
    static double estimateOtherSpotToShootSpotSeconds() {
        double turnDegrees = Math.abs(NearRoutePoses.OTHER_HEADING_DEG - NearRoutePoses.START_HEADING_DEG);
        return AutoPlanner.estimateLegsSeconds(Arrays.asList(NearRoutePoses.OTHER_SPOT,
                NearRoutePoses.NORTH_WAYPOINT, NearRoutePoses.SOUTH_WAYPOINT, NearRoutePoses.SHOOT_SPOT))
                + AutoPlanner.estimateTurnSeconds(turnDegrees);
    }

    private static double distance(Pose a, Pose b) {
        return Math.hypot(a.x() - b.x(), a.y() - b.y());
    }
}
