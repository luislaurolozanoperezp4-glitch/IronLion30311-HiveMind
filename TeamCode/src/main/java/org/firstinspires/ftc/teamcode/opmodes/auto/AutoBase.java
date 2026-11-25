// Sources: IronLion30311BIOBUZZInicioDeTemporada / branch feature/turrubiates/vision-align / TeamCode/.../opmodes/auto/PedroAutoBase.java @ a7b8f4c (runOpMode: INIT alliance choice, A = re-zero, start-pose check, logs, stopAll; driveTo/settle/turnTo/holdTarget/lookForCell/watchForTip/shootPollenPreload helpers; applyMechanisms modes)
//          IronLion30311BIOBUZZInicioDeTemporada / branch feature/turrubiates/vision-align / TeamCode/.../opmodes/auto/PedroAutoBase.java @ 28ed859 (Pinpoint reboot recovery log) @ 023ae35 (Pinpoint health: INIT warning, fault at START stops the drive, log columns, summary)
//          IronLion30311BIOBUZZInicioDeTemporada / branch feature/turrubiates/auto-leave-park / TeamCode/.../opmodes/auto/LeaveParkAuto.java @ e455142 (start delay)
//          — adapted for BIOBUZZ (iterative OpMode, non-blocking steps, time-aware park, G402/G407/G408 guards)
package org.firstinspires.ftc.teamcode.opmodes.auto;

import com.pedropathing.follower.Follower;
import com.pedropathing.math.Pose;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;

import org.firstinspires.ftc.teamcode.config.AutoConstants;
import org.firstinspires.ftc.teamcode.config.DriveConstants;
import org.firstinspires.ftc.teamcode.config.LauncherConstants;
import org.firstinspires.ftc.teamcode.hardware.RobotHardware;
import org.firstinspires.ftc.teamcode.pathing.AutoPlanner;
import org.firstinspires.ftc.teamcode.pathing.FieldDrawing;
import org.firstinspires.ftc.teamcode.pathing.OdometryDriver;
import org.firstinspires.ftc.teamcode.pathing.PedroConstants;
import org.firstinspires.ftc.teamcode.pathing.PinpointDiagnostics;
import org.firstinspires.ftc.teamcode.pathing.PoseLogger;
import org.firstinspires.ftc.teamcode.statemachine.ShootingSequence;
import org.firstinspires.ftc.teamcode.statemachine.TipWatch;
import org.firstinspires.ftc.teamcode.subsystems.BatteryMonitor;
import org.firstinspires.ftc.teamcode.subsystems.ColorSensing;
import org.firstinspires.ftc.teamcode.subsystems.ElementCounter;
import org.firstinspires.ftc.teamcode.subsystems.Indexer;
import org.firstinspires.ftc.teamcode.subsystems.Intake;
import org.firstinspires.ftc.teamcode.subsystems.Launcher;
import org.firstinspires.ftc.teamcode.subsystems.Lights;
import org.firstinspires.ftc.teamcode.subsystems.Subsystem;
import org.firstinspires.ftc.teamcode.subsystems.Vision;
import org.firstinspires.ftc.teamcode.subsystems.vision.HiveCell;
import org.firstinspires.ftc.teamcode.util.Alliance;
import org.firstinspires.ftc.teamcode.util.GamepadEx;
import org.firstinspires.ftc.teamcode.util.MatchTimer;
import org.firstinspires.ftc.teamcode.util.PoseStorage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import java.util.function.DoubleSupplier;

/**
 * Shared base of every Pedro/odometry autonomous OpMode. A subclass only says which alliance and
 * start positions it allows ({@link #createSelector()}) and builds its routine
 * ({@link #buildRoutine(StepSequence)}) from the step factories below.
 * <p>
 * <b>INIT</b> (gamepad1, see {@link AutoSelector}): alliance (B / X, unless the OpMode fixes
 * it), start position (D-pad left / right), start delay (D-pad up / down), A = set the odometry
 * back to the start pose if the robot was moved. The telemetry shows the route, missing
 * hardware and whether the odometry reads the start pose.
 * <p>
 * <b>START</b>: the odometry is checked against the start pose
 * ({@link OdometryDriver#checkStartPose}): small error = re-zeroed, large error = the robot does
 * not drive (it still shoots). Then the routine runs, one non-blocking step per loop.
 * <p>
 * <b>Every loop</b>: {@code clearBulkCache()} (bulk caching is MANUAL) → {@code follower.update()}
 * → guards → {@code driver.periodic()} → the current step → subsystems → logs and telemetry.
 * <p>
 * <b>Guards</b>:
 * <ul>
 *     <li>Odometry: a Pinpoint reboot (pose jump to (0, 0, 0)) is recovered by
 *     {@link OdometryDriver}; any other jump, or a Pinpoint fault at START
 *     ({@link PinpointDiagnostics}), stops the drive for the rest of AUTO.</li>
 *     <li>Time: when the park from here (see {@link EmergencyPark}) no longer fits before the
 *     end of AUTO with {@code AUTO_PARK_RESERVE_SECONDS} spare, the current step is cancelled,
 *     the mechanisms stop and the robot parks.</li>
 *     <li>G402: if the pose crosses into the opponent's territory ({@link TerritoryGuard}), the
 *     drive stops for the rest of AUTO.</li>
 *     <li>G407: the intake refuses to collect while the robot controls 4 elements
 *     ({@code ElementCounter}, starts at the preload). G408: the intake stops (never pushes)
 *     while the opponent's NECTAR is at the intake. At STOP the element count is handed to
 *     TeleOp ({@link PoseStorage#saveElementCount(int)}).</li>
 * </ul>
 * <b>STOP</b>: everything stops and the final pose is saved for TeleOp ({@link PoseStorage}),
 * unless the odometry was lost (then nothing is saved).
 * <p>
 * Drive moves use the TESTED {@link OdometryDriver} (decision D6: Pedro path following is not
 * tuned yet); Pedro still tracks the pose.
 */
public abstract class AutoBase extends OpMode {

    /** What the mechanisms do during a drive step. */
    protected enum Mode {
        /** Intake, indexer and POLLEN flywheel off. */
        IDLE,
        /** Intake collecting (G407 gate applies), flywheel off. */
        INTAKE,
        /** POLLEN flywheel spinning up to {@link #getSpinUpRpm()}, no feeding. */
        SPIN_UP,
        /** Leave the mechanisms as they are. */
        KEEP
    }

    /** The kinds of {@link DriveStep}. */
    private enum DriveKind { DRIVE_TO, DRIVE_THROUGH, TURN, HOLD }

    /** How many step log lines the telemetry shows. */
    private static final int TELEMETRY_LOG_LINES = 6;
    /** Unit conversion (not a tuning value). */
    private static final double CM_PER_INCH = 2.54;

    private final MatchTimer.Clock clock = MatchTimer.systemClock();
    private final StepSequence sequence = new StepSequence();
    private final PoseLogger poseLogger = new PoseLogger();
    private final FieldDrawing fieldDrawing = new FieldDrawing();
    private final List<Subsystem> subsystems = new ArrayList<>();

    private RobotHardware hardware;
    private Intake intake;
    private Indexer indexer;
    private Launcher launcher;
    private ColorSensing colorSensing;
    private ElementCounter elementCounter;
    private Vision vision;
    private Lights lights;
    private BatteryMonitor battery;
    private ShootingSequence shooting;
    private TipWatch tipWatch;

    private Follower follower;
    private OdometryDriver driver;
    private PinpointDiagnostics pinpointDiagnostics;
    private int loggedRecoveries;
    private String followerError = "";

    private AutoSelector selector;
    private GamepadEx pad;
    private MatchTimer matchTimer;
    private AutoPlanner planner;
    private Pose startPose;
    private String startCheckText = "";

    private boolean isParking;
    private boolean isDriveLocked;
    private boolean wasCellSeen;
    private boolean wasTipDown;
    private double spinUpRpm = LauncherConstants.POLLEN_AUTO_RPM;

    // ------------------------------------------------------------------------------------------
    // What a subclass provides
    // ------------------------------------------------------------------------------------------

    /** @return the INIT menu: fixed alliance (or null) and the allowed start positions */
    protected abstract AutoSelector createSelector();

    /**
     * Adds the routine's steps. Called once when START is pressed, after the alliance and start
     * position are final (the start delay is already in the sequence).
     *
     * @param sequence the sequence to fill
     */
    protected abstract void buildRoutine(StepSequence sequence);

    /** @return one or two lines describing the route, for the INIT telemetry */
    protected abstract String describeRoute();

    // ------------------------------------------------------------------------------------------
    // OpMode
    // ------------------------------------------------------------------------------------------

    /** Builds the hardware, subsystems, follower and driver. */
    @Override
    public void init() {
        hardware = new RobotHardware(hardwareMap);
        selector = createSelector();
        pad = new GamepadEx(gamepad1);
        matchTimer = new MatchTimer(clock, true);
        planner = new AutoPlanner(matchTimer);

        battery = new BatteryMonitor(hardware);
        intake = new Intake(hardware);
        indexer = new Indexer(hardware);
        launcher = new Launcher(hardware);
        colorSensing = new ColorSensing(hardware);
        elementCounter = new ElementCounter(colorSensing, launcher);
        vision = new Vision(hardware, selector.getAlliance());
        lights = new Lights(hardware);
        // ColorSensing and Launcher before ElementCounter (it reads their totals)
        subsystems.addAll(Arrays.<Subsystem>asList(battery, colorSensing, launcher, elementCounter,
                intake, indexer, vision, lights));

        // G407: no collecting while the robot controls the limit. G408: stop (never push) for opponent NECTAR.
        intake.setCollectGate(new BooleanSupplier() {
            @Override
            public boolean getAsBoolean() {
                return elementCounter.canCollectMore();
            }
        });
        intake.setOpponentNectarDetector(new BooleanSupplier() {
            @Override
            public boolean getAsBoolean() {
                return colorSensing.isOpponentNectarAtIntake(selector.getAlliance());
            }
        });
        shooting = ShootingSequence.forRobot(launcher, indexer, intake);
        tipWatch = TipWatch.forVision(vision);
        pinpointDiagnostics = new PinpointDiagnostics(hardware.pinpoint);
        poseLogger.setPinpointDiagnostics(pinpointDiagnostics);

        try {
            follower = PedroConstants.createFollower(hardwareMap);
            driver = new OdometryDriver(OdometryDriver.fromFollower(follower), clock);
        } catch (RuntimeException e) {
            // Missing Pinpoint or drive motor: the robot will not drive, but can still shoot
            follower = null;
            driver = null;
            followerError = "Pedro/Pinpoint not available (" + e.getMessage()
                    + "): the robot will NOT drive. Use Auto Safe Park Encoders.";
        }
        sequence.setListener(new StepSequence.Listener() {
            @Override
            public void onLogLine(String line) {
                poseLogger.event(getPose(), driver != null ? driver.getTarget() : null, line);
            }
        });
        applySelection();
    }

    /** INIT menu, live odometry check and warnings. */
    @Override
    public void init_loop() {
        hardware.clearBulkCache();
        pad.readButtons();
        boolean isChanged = false;
        for (GamepadEx.Button button : GamepadEx.Button.values()) {
            if (pad.wasJustPressed(button) && selector.onPress(button)) {
                isChanged = true;
            }
        }
        if (isChanged) {
            applySelection();
        }
        if (pad.wasJustPressed(GamepadEx.Button.A) && follower != null) {
            follower.setPose(startPose);
        }
        if (follower != null) {
            follower.update();
        }
        pinpointDiagnostics.update();
        vision.periodic();
        lights.periodic();
        battery.periodic();

        telemetry.addLine("INIT: place the robot on the start pose");
        telemetry.addData("Route", describeRoute());
        telemetry.addData("Selected", selector.getSummary());
        telemetry.addData("Controls", selector.getControlsText());
        telemetry.addData("Odometry", describeStartPose());
        telemetry.addData("Pinpoint", pinpointDiagnostics.summary());
        if (pinpointDiagnostics.hasFault()) {
            telemetry.addLine("!! PINPOINT FAULT: power-cycle the robot (battery out 10 s) before"
                    + " the match; AUTO will not drive");
        }
        if (!followerError.isEmpty()) {
            telemetry.addLine("WARNING: " + followerError);
        }
        for (String warning : hardware.getWarnings()) {
            telemetry.addLine("WARNING: " + warning);
        }
        telemetry.addData("Cameras", "Limelight %s | HuskyLens %s",
                vision.isConnected() ? "OK" : "MISSING", colorSensing.isConnected() ? "OK" : "MISSING");
        telemetry.addData("Battery", "%.1f V", battery.getVolts());
        if (follower != null) {
            fieldDrawing.draw(follower.pose());
        }
    }

    /** Checks the start pose, opens the log and builds the routine. */
    @Override
    public void start() {
        matchTimer.start();
        PoseStorage.clear();
        elementCounter.setCount(LauncherConstants.PRELOAD_BALLS);
        if (follower != null) {
            follower.update();
            pinpointDiagnostics.update();
            OdometryDriver.StartCheck check = driver.checkStartPose(startPose);
            startCheckText = driver.getResultText()
                    + (check == OdometryDriver.StartCheck.FAULT ? " -> robot will NOT drive" : "");
            if (pinpointDiagnostics.hasFault()) {
                driver.lockOdometry("Pinpoint fault at START (" + pinpointDiagnostics.summary() + ")");
                startCheckText += "; PINPOINT FAULT -> robot will NOT drive (power-cycle it)";
            }
        } else {
            startCheckText = "No odometry: robot will NOT drive";
        }
        poseLogger.start(getClass().getSimpleName());
        poseLogger.setOrigin(startPose);
        sequence.note(startCheckText);
        sequence.note("Selected: " + selector.getSummary());
        if (selector.getStartDelaySeconds() > 0.0) {
            sequence.add(waitFor("Start delay", selector.getStartDelaySeconds()));
        }
        buildRoutine(sequence);
    }

    /** One non-blocking step of the routine. */
    @Override
    public void loop() {
        hardware.clearBulkCache();
        if (follower != null) {
            follower.update();
        }
        pinpointDiagnostics.update();
        // The driver's checked pose: after a Pinpoint reboot it is the restored pose, not (0, 0, 0)
        Pose pose = driver != null ? driver.updatePose() : getPose();
        if (driver != null && driver.getResetRecoveries() > loggedRecoveries) {
            loggedRecoveries = driver.getResetRecoveries();
            sequence.note(driver.getLastRecoveryText() + "; Pinpoint " + pinpointDiagnostics.summary());
        }
        if (follower != null) {
            checkTerritory(pose);
            checkTimeToPark(pose);
        }
        if (driver != null) {
            driver.periodic();
        }
        sequence.periodic();

        colorSensing.countWhileIntaking(intake.isCollecting());
        for (Subsystem subsystem : subsystems) {
            subsystem.periodic();
        }

        poseLogger.update(pose, driver != null ? driver.getTarget() : null, sequence.getCurrentName());
        fieldDrawing.draw(pose);
        addRunTelemetry(pose);
    }

    /** Stops everything and saves the pose for TeleOp. */
    @Override
    public void stop() {
        sequence.abortAndReplace(new ArrayList<AutoStep>());
        if (driver != null) {
            driver.cancel();
        }
        if (follower != null) {
            follower.update();
            if (driver != null && !driver.isOdometryLost() && matchTimer.isStarted()) {
                PoseStorage.save(follower.pose(), selector.getAlliance());
            }
        }
        if (matchTimer.isStarted()) {
            // G407 hand-off: TeleOp starts from what the robot still holds.
            PoseStorage.saveElementCount(elementCounter.getCount());
            sequence.note(pinpointDiagnostics.getMatchSummary() + ", reset recoveries "
                    + (driver != null ? driver.getResetRecoveries() : 0));
        }
        for (Subsystem subsystem : subsystems) {
            subsystem.stop();
        }
        poseLogger.close();
    }

    // ------------------------------------------------------------------------------------------
    // Guards
    // ------------------------------------------------------------------------------------------

    /** G402: stops the drive for good if the robot crosses into the opponent's territory. */
    private void checkTerritory(Pose pose) {
        if (isDriveLocked || !TerritoryGuard.isPastLine(pose, getAlliance())) {
            return;
        }
        isDriveLocked = true;
        sequence.abortAndReplace(new ArrayList<AutoStep>());
        stopMechanisms();
        if (driver != null) {
            driver.cancel();
        }
        sequence.note(String.format(Locale.US,
                "G402 GUARD: pose x %.1f is past the territory line; drive stopped", pose.x()));
    }

    /** Cancels the current step and parks when the park from here barely fits in the time left. */
    private void checkTimeToPark(Pose pose) {
        if (isParking || isDriveLocked || driver == null || driver.isOdometryLost()
                || sequence.isFinished()) {
            return;
        }
        double parkSeconds = EmergencyPark.estimateSeconds(pose, getAlliance());
        if (!planner.mustParkNow(parkSeconds)) {
            return;
        }
        String cancelled = sequence.getCurrentName();
        isParking = true;
        stopMechanisms();
        sequence.abortAndReplace(parkSteps("Time park", EmergencyPark.plan(pose, getAlliance()), false));
        sequence.note(String.format(Locale.US,
                "TIME: %.1f s left, park needs ~%.1f s: stopped '%s' and parking",
                matchTimer.getRemainingSeconds(), parkSeconds, cancelled));
    }

    // ------------------------------------------------------------------------------------------
    // Step factories for subclasses
    // ------------------------------------------------------------------------------------------

    /**
     * @param name     leg name
     * @param target   field pose to stop at
     * @param maxPower top power
     * @param mode     mechanisms during the leg
     * @return a leg that stops at the target
     */
    protected AutoStep driveTo(String name, Pose target, double maxPower, Mode mode) {
        return new DriveStep(name, DriveKind.DRIVE_TO, target, maxPower, 0.0, mode);
    }

    /** Same as {@link #driveTo(String, Pose, double, Mode)} at {@code AUTO_MAX_POWER}. */
    protected AutoStep driveTo(String name, Pose target, Mode mode) {
        return driveTo(name, target, DriveConstants.AUTO_MAX_POWER, mode);
    }

    /**
     * @param name   leg name
     * @param target field waypoint passed without stopping
     * @param mode   mechanisms during the leg
     * @return a pass-through leg (start the next leg right after it)
     */
    protected AutoStep driveThrough(String name, Pose target, Mode mode) {
        return new DriveStep(name, DriveKind.DRIVE_THROUGH, target, DriveConstants.AUTO_MAX_POWER, 0.0, mode);
    }

    /**
     * @param name           turn name
     * @param headingRadians field heading
     * @param mode           mechanisms during the turn
     * @return an in-place turn
     */
    protected AutoStep turnTo(String name, double headingRadians, Mode mode) {
        return new DriveStep(name, DriveKind.TURN, null, 0.0, headingRadians, mode);
    }

    /**
     * @param seconds how long to hold the last target
     * @param mode    mechanisms meanwhile
     * @return a settle step
     */
    protected AutoStep settle(double seconds, Mode mode) {
        return new DriveStep("Settle", DriveKind.HOLD, null, 0.0, seconds, mode);
    }

    /** @return holds PARK until the end of AUTO (never done unless the odometry is lost) */
    protected AutoStep holdParked() {
        return new DriveStep("PARKED: holding", DriveKind.HOLD, null, 0.0,
                Double.POSITIVE_INFINITY, Mode.IDLE);
    }

    /** @return marks the start of the park, so the time guard no longer interrupts it */
    protected AutoStep markParking() {
        return AutoStep.instant("Start park", new Runnable() {
            @Override
            public void run() {
                isParking = true;
            }
        });
    }

    /**
     * Park legs: mark parking, the legs, settle and hold.
     *
     * @param name            leg name prefix
     * @param waypoints       field waypoints, the last is PARK
     * @param isStopEachLeg   true = stop and settle on every waypoint (the TESTED near park);
     *                        false = pass through all but the last
     * @return the steps
     */
    protected List<AutoStep> parkSteps(String name, List<Pose> waypoints, boolean isStopEachLeg) {
        List<AutoStep> steps = new ArrayList<>();
        steps.add(markParking());
        for (int i = 0; i < waypoints.size(); i++) {
            String legName = String.format(Locale.US, "%s leg %d/%d", name, i + 1, waypoints.size());
            boolean isLast = i == waypoints.size() - 1;
            if (isStopEachLeg || isLast) {
                steps.add(driveTo(legName, waypoints.get(i), Mode.IDLE));
                steps.add(settle(AutoConstants.SETTLE_SECONDS, Mode.IDLE));
            } else {
                steps.add(driveThrough(legName, waypoints.get(i), Mode.IDLE));
            }
        }
        steps.add(holdParked());
        return steps;
    }

    /**
     * A volley with the TESTED {@link ShootingSequence}, holding the pose meanwhile.
     *
     * @param name          step name
     * @param balls         balls to shoot
     * @param rpm           POLLEN RPM
     * @param maxSecSupplier longest volley, read when the step starts (0 or less = skip)
     * @return the step
     */
    protected AutoStep shoot(String name, final int balls, final double rpm, final DoubleSupplier maxSecSupplier) {
        return new AutoStep(name) {
            private boolean isSkipped;
            private double maxSec;

            @Override
            public void start() {
                maxSec = maxSecSupplier.getAsDouble();
                isSkipped = maxSec <= 0.0;
                if (isSkipped) {
                    return;
                }
                holdHere();
                shooting.start(balls, rpm, maxSec);
            }

            @Override
            public void periodic() {
                shooting.periodic();
            }

            @Override
            public boolean isDone() {
                return isSkipped || shooting.isDone();
            }

            @Override
            public void cancel() {
                shooting.cancel();
            }

            @Override
            public String getResultText() {
                return isSkipped ? getName() + ": SKIPPED (no time)"
                        : String.format(Locale.US, "%s: %d/%d shots in %.1f s (max %.1f s)", getName(),
                        shooting.getShotsFired(), balls, shooting.getElapsedSec(), maxSec);
            }
        };
    }

    /**
     * Watches a HIVE cell after a volley (TESTED early TIP watch); afterwards
     * {@link #wasTipDown()} tells whether it went down.
     *
     * @param name           step name
     * @param cell           the cell to watch
     * @param maxSecSupplier longest watch, read at start (0 or less = skip, "not down")
     * @param goneSec        tags gone this long = down
     * @return the step
     */
    protected AutoStep watchForTip(String name, final HiveCell cell, final DoubleSupplier maxSecSupplier,
                                   final double goneSec) {
        return new AutoStep(name) {
            private boolean isSkipped;

            @Override
            public void start() {
                double maxSec = maxSecSupplier.getAsDouble();
                wasTipDown = false;
                isSkipped = maxSec <= 0.0;
                if (!isSkipped) {
                    holdHere();
                    tipWatch.start(cell, maxSec, goneSec);
                }
            }

            @Override
            public void periodic() {
                tipWatch.periodic();
                if (tipWatch.isDone()) {
                    wasTipDown = tipWatch.isCellDown();
                }
            }

            @Override
            public boolean isDone() {
                return isSkipped || tipWatch.isDone();
            }

            @Override
            public String getResultText() {
                return getName() + ": " + (isSkipped ? "SKIPPED (no time)" : tipWatch.getState().toString());
            }
        };
    }

    /**
     * Looks for a cell's tags for up to {@code windowSec}, holding the pose; afterwards
     * {@link #wasCellSeen()} tells whether they were seen (cell up).
     *
     * @param name      step name
     * @param cell      the cell
     * @param windowSec longest look
     * @return the step
     */
    protected AutoStep lookForCell(String name, final HiveCell cell, final double windowSec) {
        return new AutoStep(name) {
            private double startSec;

            @Override
            public void start() {
                startSec = clock.nowSeconds();
                wasCellSeen = false;
                holdHere();
            }

            @Override
            public void periodic() {
                if (vision.isCellInView(cell)) {
                    wasCellSeen = true;
                }
            }

            @Override
            public boolean isDone() {
                return wasCellSeen || clock.nowSeconds() - startSec >= windowSec;
            }

            @Override
            public String getResultText() {
                return String.format(Locale.US, "%s: %s %s after %.2f s", getName(), cell.getDisplayName(),
                        wasCellSeen ? "SEEN (up)" : "NOT SEEN (down)", clock.nowSeconds() - startSec);
            }
        };
    }

    /**
     * @param name    step name
     * @param seconds how long to wait (robot still, mechanisms as they are)
     * @return the step
     */
    protected AutoStep waitFor(String name, final double seconds) {
        return new AutoStep(name) {
            private double startSec;

            @Override
            public void start() {
                startSec = clock.nowSeconds();
            }

            @Override
            public boolean isDone() {
                return clock.nowSeconds() - startSec >= seconds;
            }
        };
    }

    // ------------------------------------------------------------------------------------------
    // Helpers for subclasses
    // ------------------------------------------------------------------------------------------

    /** @return the chosen alliance */
    protected Alliance getAlliance() {
        return selector.getAlliance();
    }

    /** @return the chosen start position */
    protected StartPosition getStartPosition() {
        return selector.getStartPosition();
    }

    /**
     * @param redPose a pose written for RED
     * @return the pose on the real field for our alliance
     */
    protected Pose field(Pose redPose) {
        return getAlliance().toField(redPose);
    }

    /**
     * @param redHeadingDeg a heading written for RED, degrees
     * @return the field heading for our alliance, radians
     */
    protected double fieldHeading(double redHeadingDeg) {
        return field(new Pose(0.0, 0.0, Math.toRadians(redHeadingDeg))).heading();
    }

    /** @return the robot's field pose (the start pose without odometry) */
    protected Pose getPose() {
        return follower != null ? follower.pose() : startPose;
    }

    /**
     * @param target       field pose
     * @param toleranceCm  position tolerance
     * @param toleranceDeg heading tolerance
     * @return true if the odometry puts the robot on {@code target}
     */
    protected boolean isNear(Pose target, double toleranceCm, double toleranceDeg) {
        if (follower == null || driver.isOdometryLost()) {
            return false;
        }
        Pose pose = follower.pose();
        double errorCm = OdometryDriver.distance(pose, target) * CM_PER_INCH;
        double errorDeg = Math.abs(Math.toDegrees(OdometryDriver.angleError(target.heading(), pose.heading())));
        return errorCm <= toleranceCm && errorDeg <= toleranceDeg;
    }

    /** @return the AUTO match timer */
    protected MatchTimer getMatchTimer() {
        return matchTimer;
    }

    /** @return the time-budget planner */
    protected AutoPlanner getPlanner() {
        return planner;
    }

    /** @return the Limelight subsystem */
    protected Vision getVision() {
        return vision;
    }

    /** @return the step sequence (to add steps from a decision) */
    protected StepSequence getSequence() {
        return sequence;
    }

    /** @return true if the last {@link #lookForCell} saw the cell */
    protected boolean wasCellSeen() {
        return wasCellSeen;
    }

    /** @return true if the last {@link #watchForTip} saw the cell go down */
    protected boolean wasTipDown() {
        return wasTipDown;
    }

    /** @return true once the routine is parking (the time guard no longer interrupts) */
    protected boolean isParking() {
        return isParking;
    }

    /** @return POLLEN RPM used by {@link Mode#SPIN_UP} */
    protected double getSpinUpRpm() {
        return spinUpRpm;
    }

    /** @param rpm POLLEN RPM for the next {@link Mode#SPIN_UP} legs */
    protected void setSpinUpRpm(double rpm) {
        spinUpRpm = rpm;
    }

    /** @return the ball-pickup summary from the HuskyLens */
    protected String getBallSummary() {
        return colorSensing.isConnected() ? colorSensing.getSummary() : "HuskyLens not configured";
    }

    private boolean canDrive() {
        return driver != null && !isDriveLocked;
    }

    /** Holds the current target while a volley or a watch runs. */
    private void holdHere() {
        if (canDrive()) {
            driver.hold(Double.POSITIVE_INFINITY);
        }
    }

    private void applyMode(Mode mode) {
        switch (mode) {
            case IDLE:
                intake.idle();
                indexer.idle();
                launcher.stopPollen();
                break;
            case INTAKE:
                intake.collect();
                indexer.idle();
                launcher.stopPollen();
                break;
            case SPIN_UP:
                intake.idle();
                indexer.idle();
                launcher.setPollenTargetRpm(spinUpRpm);
                launcher.spinUpPollen();
                break;
            default:
                break;
        }
    }

    private void stopMechanisms() {
        shooting.cancel();
        intake.idle();
        indexer.idle();
        indexer.setPaused(false);
        launcher.stopPollen();
        launcher.stopNectar();
    }

    private void applySelection() {
        Alliance alliance = selector.getAlliance();
        vision.setAlliance(alliance);
        lights.showAlliance(alliance);
        startPose = alliance.toField(selector.getStartPosition().getRedPose());
        if (follower != null) {
            follower.setPose(startPose);
        }
        fieldDrawing.clearTrail();
    }

    /** @return the INIT odometry check line (same limits as the check at START) */
    private String describeStartPose() {
        if (follower == null) {
            return "NO ODOMETRY";
        }
        Pose pose = follower.pose();
        double errorInches = OdometryDriver.distance(pose, startPose);
        double errorDeg = Math.abs(Math.toDegrees(OdometryDriver.angleError(startPose.heading(), pose.heading())));
        String verdict;
        if (errorInches <= DriveConstants.START_POSE_TOLERANCE_INCHES
                && errorDeg <= DriveConstants.START_HEADING_TOLERANCE_DEGREES) {
            verdict = "OK";
        } else if (errorInches <= DriveConstants.START_REZERO_MAX_INCHES
                && errorDeg <= DriveConstants.START_REZERO_MAX_DEGREES) {
            verdict = "will RE-ZERO at start (press A if the robot was moved)";
        } else {
            verdict = "FAULT: robot will NOT drive. Check Pinpoint/pods, then press A";
        }
        return String.format(Locale.US, "%s (%.1f in, %.1f deg off)", verdict, errorInches, errorDeg);
    }

    private void addRunTelemetry(Pose pose) {
        telemetry.addData("Time", "%.1f s, %.1f s left", matchTimer.getElapsedSeconds(),
                matchTimer.getRemainingSeconds());
        telemetry.addData("Step", sequence.getCurrentName());
        telemetry.addData("Pose", "(%.1f, %.1f) %.0f deg", pose.x(), pose.y(), Math.toDegrees(pose.heading()));
        if (isDriveLocked) {
            telemetry.addLine("DRIVE STOPPED by the G402 territory guard");
        }
        if (driver != null && driver.isOdometryLost()) {
            telemetry.addLine("ODOMETRY LOST: drive stopped (Pinpoint " + pinpointDiagnostics.summary() + ")");
        }
        if (driver != null && driver.getResetRecoveries() > 0) {
            telemetry.addData("Pinpoint resets recovered", driver.getResetRecoveries());
        }
        if (isParking) {
            telemetry.addLine("Parking");
        }
        telemetry.addData("Elements", "%d held | POLLEN shots %d", elementCounter.getCount(),
                launcher.getPollenShotCount());
        telemetry.addData("Start check", startCheckText);
        List<String> log = sequence.getLog();
        for (int i = Math.max(0, log.size() - TELEMETRY_LOG_LINES); i < log.size(); i++) {
            telemetry.addLine(log.get(i));
        }
        telemetry.addData("Log", poseLogger.getFileName());
    }

    /** A drive move with the odometry driver. */
    private class DriveStep extends AutoStep {

        private final DriveKind kind;
        private final Pose target;
        private final double maxPower;
        private final double value;
        private final Mode mode;
        private boolean isSkipped;

        DriveStep(String name, DriveKind kind, Pose target, double maxPower, double value, Mode mode) {
            super(name);
            this.kind = kind;
            this.target = target;
            this.maxPower = maxPower;
            this.value = value;
            this.mode = mode;
        }

        @Override
        public void start() {
            applyMode(mode);
            isSkipped = !canDrive();
            if (isSkipped) {
                return;
            }
            switch (kind) {
                case DRIVE_TO:
                    driver.driveTo(getName(), target, maxPower);
                    break;
                case DRIVE_THROUGH:
                    driver.driveThrough(getName(), target);
                    break;
                case TURN:
                    driver.turnTo(getName(), value);
                    break;
                default:
                    driver.hold(value);
                    break;
            }
        }

        @Override
        public boolean isDone() {
            return isSkipped || driver.isDone();
        }

        @Override
        public void cancel() {
            if (driver != null) {
                driver.cancel();
            }
        }

        @Override
        public String getResultText() {
            if (isSkipped) {
                return getName() + ": SKIPPED (drive stopped)";
            }
            return kind == DriveKind.HOLD ? "" : driver.getResultText();
        }
    }
}
