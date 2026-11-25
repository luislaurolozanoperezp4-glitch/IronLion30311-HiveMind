// Sources: IronLion30311BIOBUZZInicioDeTemporada / branch feature/turrubiates/vision-align / TeamCode/.../opmodes/auto/PreloadRouteAuto.java @ a7b8f4c (runRoutine, runCollectRoutine, returnAndCheckTip, parkFromShootSide, shootAtOtherCellAndPark, relativeOther, shootTimeLeft)
//          docs/BEST_OF_MATRIX.md section 5 (time-aware cycle loop; Far Cycle from the other-cell spot)
//          — adapted for BIOBUZZ (non-blocking steps; AutoPlanner decides "cycle again" or "go park")
package org.firstinspires.ftc.teamcode.opmodes.auto;

import com.pedropathing.math.Pose;

import org.firstinspires.ftc.teamcode.config.AutoConstants;
import org.firstinspires.ftc.teamcode.config.DriveConstants;
import org.firstinspires.ftc.teamcode.config.LauncherConstants;
import org.firstinspires.ftc.teamcode.config.MatchConstants;
import org.firstinspires.ftc.teamcode.pathing.AutoPlanner;
import org.firstinspires.ftc.teamcode.pathing.paths.NearRoutePoses;
import org.firstinspires.ftc.teamcode.pathing.paths.OtherCellPaths;
import org.firstinspires.ftc.teamcode.pathing.paths.PreloadCollectPaths;
import org.firstinspires.ftc.teamcode.pathing.paths.PreloadParkPaths;
import org.firstinspires.ftc.teamcode.subsystems.vision.HiveCell;
import org.firstinspires.ftc.teamcode.subsystems.vision.HiveClusterPose;

import java.util.ArrayList;
import java.util.Arrays;
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
 *     <li>LEAVE 10 cm, shoot the 4 preloaded POLLEN at 2950 RPM (ends by 22 s).</li>
 *     <li>Decision ({@link AutoPlanner}): CYCLE_AGAIN if a collect cycle plus the park after it
 *     still fit (with the 4 s park reserve) and the volley ended before 11 s (TESTED backstop);
 *     otherwise GO_PARK with the TESTED park legs (back 5, right 60, back 230, right 60 cm).</li>
 *     <li>Cycle: early TIP watch on our scoring cell (up to 4 s, shorter if time is short), turn
 *     90 deg, collect the ball row slowly, then
 *     <ul>
 *         <li>TIP seen: around the HIVE to the other cell (if before 15 s), shoot there, park;</li>
 *         <li>no TIP: back to the shooting spot, re-center, TIP check; up = shoot here and ask
 *         the planner again (it parks: {@code MAX_CYCLES} = 1); down = other cell.</li>
 *     </ul></li>
 * </ol>
 * <b>FAR start</b> (8.96, 131.19, 90 deg), <b>UNTESTED</b> (decision D21): drive to the
 * other-cell spot north of the HIVE; if that cell is seen, shoot the preload there; otherwise
 * go down the west lane to the near shooting spot and shoot there. Then the same decisions.
 * <p>
 * The time guard in {@link AutoBase} parks the robot from anywhere if a step runs long.
 */
public abstract class ScoringAuto extends AutoBase {

    /** Unit conversion (not a tuning value). */
    private static final double CM_PER_INCH = 2.54;
    /** Turn between facing the HIVE from the south and from the north, degrees. */
    private static final double HALF_TURN_DEGREES = 180.0;

    private int cyclesDone;
    /** Camera-to-scoring-cell distance at the first volley (the one that scores), meters. */
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
                AutoStep.instant("Measure HIVE distance", new Runnable() {
                    @Override
                    public void run() {
                        shootDistanceMeters = cellForwardMeters(HiveCell.getBackCell(getAlliance()));
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
        sequence.add(
                driveThrough("North-west of the HIVE", field(NearRoutePoses.NORTH_WAYPOINT), Mode.SPIN_UP),
                driveTo("To other cell", field(NearRoutePoses.OTHER_SPOT), Mode.SPIN_UP),
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
            // TESTED park legs, straight from the preload volley
            getSequence().addNext(parkSteps("Park", new PreloadParkPaths(getAlliance()).getParkWaypoints(), true));
        } else {
            getSequence().addNext(parkSteps("Park", new PreloadCollectPaths(getAlliance()).getParkWaypoints(), false));
        }
    }

    /** After the collect row: other cell (TIP seen early), or back to the shooting spot. */
    private void decideAfterCollect() {
        cyclesDone++;
        getSequence().note("Collect: " + getBallSummary());
        boolean isOtherCellOpen = getPlanner().isBeforeDeadline(AutoConstants.OTHER_CELL_LATEST_START_SECONDS);
        if (wasTipDown() && isOtherCellOpen) {
            List<AutoStep> steps = new ArrayList<>();
            List<Pose> waypoints = new OtherCellPaths(getAlliance()).getFromCollectWaypoints();
            // Away from the red wall first (no turn there), then turning north on the way
            steps.add(driveThrough("Away from the wall", waypoints.get(0), Mode.SPIN_UP));
            steps.add(driveThrough("North-west of the HIVE", waypoints.get(1), Mode.SPIN_UP));
            steps.addAll(otherCellSteps(AutoConstants.COLLECT_BALLS));
            getSequence().addNext(steps);
        } else if (wasTipDown()) {
            // Too late for the other cell. VA drove the south-west diagonal from the wall here;
            // the emergency park keeps the heading instead (no turn next to the wall).
            getSequence().addNext(parkSteps("Park", EmergencyPark.plan(getPose(), getAlliance()), false));
        } else {
            getSequence().addNext(returnAndTipCheckSteps());
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
                        if (wasCellSeen()) {
                            getSequence().addNext(shootSpotVolleySteps("Row volley", AutoConstants.COLLECT_BALLS));
                        } else if (getPlanner().isBeforeDeadline(AutoConstants.OTHER_CELL_LATEST_START_SECONDS)) {
                            List<AutoStep> steps = new ArrayList<>();
                            List<Pose> waypoints = new OtherCellPaths(getAlliance()).getFromShootSpotWaypoints();
                            // Turning 180 deg on the way, south-west of the HIVE
                            steps.add(driveThrough("South-west of the HIVE", waypoints.get(0), Mode.SPIN_UP));
                            steps.add(driveThrough("North-west of the HIVE", waypoints.get(1), Mode.SPIN_UP));
                            steps.addAll(otherCellSteps(AutoConstants.COLLECT_BALLS));
                            getSequence().addNext(steps);
                        } else {
                            getSequence().addNext(parkSteps("Park",
                                    new PreloadCollectPaths(getAlliance()).getParkWaypoints(), false));
                        }
                    }
                }));
    }

    /** At the other-cell spot after looking: shoot there (with the vision approach), or park. */
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

    /** A volley at the near shooting spot, then the planner decides again. */
    private List<AutoStep> shootSpotVolleySteps(String name, int balls) {
        return new ArrayList<>(Arrays.asList(
                shoot(name, balls, LauncherConstants.POLLEN_AUTO_RPM, shootTimeLeft(AutoConstants.LATEST_SHOOT_END_SECONDS)),
                AutoStep.instant("Decide after volley", new Runnable() {
                    @Override
                    public void run() {
                        decideAfterShootSpotVolley();
                    }
                })));
    }

    /** From the north-west waypoint: to the other-cell spot, look for the cell, decide. */
    private List<AutoStep> otherCellSteps(final int balls) {
        setSpinUpRpm(AutoConstants.OTHER_CELL_POLLEN_RPM);
        return new ArrayList<>(Arrays.asList(
                driveTo("To other cell", field(NearRoutePoses.OTHER_SPOT), Mode.SPIN_UP),
                settle(AutoConstants.SETTLE_SECONDS, Mode.SPIN_UP),
                lookForCell("Other cell", HiveCell.getAudienceCell(getAlliance()), AutoConstants.OTHER_CELL_LOOK_SECONDS),
                AutoStep.instant("Decide at other cell", new Runnable() {
                    @Override
                    public void run() {
                        decideAtOtherCell(balls);
                    }
                })));
    }

    /** At the other-cell spot with the cell seen: Limelight approach, volley, decide. */
    private List<AutoStep> otherCellVolleySteps(int balls) {
        List<AutoStep> steps = new ArrayList<>();
        double moveCm = getVisionApproachCm();
        if (moveCm > AutoConstants.VISION_APPROACH_MIN_CM) {
            steps.add(driveTo(String.format(Locale.US, "Vision approach %.0f cm", moveCm),
                    field(otherSpotMovedTowardCell(moveCm)), Mode.SPIN_UP));
            steps.add(settle(AutoConstants.SETTLE_SECONDS, Mode.SPIN_UP));
        }
        steps.add(shoot("Other cell volley", balls, AutoConstants.OTHER_CELL_POLLEN_RPM,
                shootTimeLeft(AutoConstants.OTHER_CELL_SHOOT_END_SECONDS)));
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

    /** Other-cell spot down the west lane to the shooting spot (turning 180 deg in the lane). */
    private List<AutoStep> otherSpotLegsToShootSpot(Mode mode) {
        return new ArrayList<>(Arrays.asList(
                driveThrough("North-west of the HIVE", field(NearRoutePoses.NORTH_WAYPOINT), mode),
                driveThrough("South-west of the HIVE", field(NearRoutePoses.SOUTH_WAYPOINT), mode),
                driveTo("To shoot spot", field(NearRoutePoses.SHOOT_SPOT), mode)));
    }

    /** One diagonal north of the HIVE into PARK (TESTED leg of the other-cell route). */
    private List<AutoStep> parkFromOtherSideSteps() {
        return parkSteps("Park from other side", Arrays.asList(field(NearRoutePoses.OTHER_PARK)), false);
    }

    // ------------------------------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------------------------------

    private boolean canShootAtOtherCell() {
        return wasCellSeen() && getPlanner().isBeforeDeadline(
                AutoConstants.OTHER_CELL_SHOOT_END_SECONDS - AutoConstants.OTHER_CELL_MIN_SHOOT_SECONDS);
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
     * Limelight distance correction at the other cell (TESTED VA logic): move toward the cell
     * so the distance matches the one that scored at the first volley, never away from it
     * (that is toward the FLOWER).
     *
     * @return centimeters to move toward the cell (0 if the cell is not measured)
     */
    private double getVisionApproachCm() {
        double distanceMeters = cellForwardMeters(HiveCell.getAudienceCell(getAlliance()));
        if (Double.isNaN(distanceMeters)) {
            return 0.0;
        }
        double referenceMeters = Double.isNaN(shootDistanceMeters)
                ? AutoConstants.DEFAULT_SHOOT_DISTANCE_METERS : shootDistanceMeters;
        double targetMeters = referenceMeters - AutoConstants.OTHER_SPOT_SHORTEN_CM / 100.0;
        double moveCm = (distanceMeters - targetMeters) * 100.0;
        return Math.max(0.0, Math.min(AutoConstants.VISION_APPROACH_MAX_CM, moveCm));
    }

    /** @return the RED other-cell spot moved {@code towardCellCm} toward the HIVE (south) */
    private static Pose otherSpotMovedTowardCell(double towardCellCm) {
        Pose spot = NearRoutePoses.OTHER_SPOT;
        return new Pose(spot.x(), spot.y() - towardCellCm / CM_PER_INCH, spot.heading());
    }

    /** @return floor distance from the camera to the cell's tags, meters, or NaN if not in view */
    private double cellForwardMeters(HiveCell cell) {
        HiveClusterPose pose = getVision().getPose(cell);
        return pose == null || !getVision().isCellInView(cell) ? Double.NaN : pose.forwardMeters;
    }

    /**
     * Driving time of a collect cycle up to a short volley (the volley itself is time-boxed).
     *
     * @return seconds
     */
    static double estimateNearCycleSeconds() {
        double slowCollect = AutoConstants.COLLECT_POWER / DriveConstants.AUTO_MAX_POWER;
        double slowReturn = Math.min(1.0, AutoConstants.RETURN_POWER / DriveConstants.AUTO_MAX_POWER);
        double toBalls = AutoPlanner.estimateLegSeconds(
                distance(NearRoutePoses.SHOOT_SPOT, NearRoutePoses.COLLECT_START));
        double overBalls = slowLeg(distance(NearRoutePoses.COLLECT_START, NearRoutePoses.COLLECT_END), slowCollect);
        double back = slowLeg(distance(NearRoutePoses.COLLECT_END, NearRoutePoses.SHOOT_SPOT), slowReturn);
        return 2.0 * AutoPlanner.estimateTurnSeconds(AutoConstants.COLLECT_TURN_DEGREES)
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
        return AutoPlanner.estimateLegsSeconds(Arrays.asList(NearRoutePoses.OTHER_SPOT,
                NearRoutePoses.NORTH_WAYPOINT, NearRoutePoses.SOUTH_WAYPOINT, NearRoutePoses.SHOOT_SPOT))
                + AutoPlanner.estimateTurnSeconds(HALF_TURN_DEGREES);
    }

    private static double slowLeg(double inches, double speedFraction) {
        return inches / (AutoConstants.CRUISE_INCHES_PER_SECOND * speedFraction) + AutoConstants.LEG_OVERHEAD_SECONDS;
    }

    private static double distance(Pose a, Pose b) {
        return Math.hypot(a.x() - b.x(), a.y() - b.y());
    }
}
