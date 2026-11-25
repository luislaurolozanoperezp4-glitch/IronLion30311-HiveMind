// Sources: IronLion30311BIOBUZZInicioDeTemporada / branch feature/turrubiates/vision-align / TeamCode/.../opmodes/teleop/MainTeleOp.java @ a7b8f4c (button mapping, brake rumble, ball assist, align-while-firing, feed only when ready and aimed, jam rumble, low-battery rumble, light logic, shot marks, telemetry)
//          IronLion30311BIOBUZZInicioDeTemporada / branch feature/mar/duopodriveNcam / TeamCode/.../PinpointOdometry.java @ 2d92fba (saved AUTO pose idea, via util.PoseStorage)
//          — adapted for BIOBUZZ (iterative OpMode, subsystems + ShootingSequence, G407/G408/G410 wiring, endgame alerts, competition telemetry, match CSV log)
package org.firstinspires.ftc.teamcode.opmodes.teleop;

import com.acmerobotics.dashboard.FtcDashboard;
import com.acmerobotics.dashboard.telemetry.MultipleTelemetry;
import com.pedropathing.math.Pose;
import com.pedropathing.revhub.localizers.PinpointLocalizer;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.util.ElapsedTime;

import org.firstinspires.ftc.teamcode.config.ControlConstants;
import org.firstinspires.ftc.teamcode.config.LauncherConstants;
import org.firstinspires.ftc.teamcode.config.MatchConstants;
import org.firstinspires.ftc.teamcode.config.RobotConstants;
import org.firstinspires.ftc.teamcode.hardware.RobotHardware;
import org.firstinspires.ftc.teamcode.pathing.PedroConstants;
import org.firstinspires.ftc.teamcode.statemachine.ShootingSequence;
import org.firstinspires.ftc.teamcode.subsystems.BallColor;
import org.firstinspires.ftc.teamcode.subsystems.BatteryMonitor;
import org.firstinspires.ftc.teamcode.subsystems.ColorSensing;
import org.firstinspires.ftc.teamcode.subsystems.Drivetrain;
import org.firstinspires.ftc.teamcode.subsystems.ElementCounter;
import org.firstinspires.ftc.teamcode.subsystems.FlowerArm;
import org.firstinspires.ftc.teamcode.subsystems.Indexer;
import org.firstinspires.ftc.teamcode.subsystems.Intake;
import org.firstinspires.ftc.teamcode.subsystems.Launcher;
import org.firstinspires.ftc.teamcode.subsystems.Lights;
import org.firstinspires.ftc.teamcode.subsystems.Subsystem;
import org.firstinspires.ftc.teamcode.subsystems.Vision;
import org.firstinspires.ftc.teamcode.subsystems.vision.HiveAligner;
import org.firstinspires.ftc.teamcode.subsystems.vision.HiveCell;
import org.firstinspires.ftc.teamcode.util.Alliance;
import org.firstinspires.ftc.teamcode.util.DataLogger;
import org.firstinspires.ftc.teamcode.util.GamepadEx;
import org.firstinspires.ftc.teamcode.util.GamepadEx.Button;
import org.firstinspires.ftc.teamcode.util.MatchTimer;
import org.firstinspires.ftc.teamcode.util.PoseStorage;

import java.util.Arrays;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.DoubleSupplier;

/**
 * The competition TeleOp. {@link TeleOpRed} and {@link TeleOpBlue} only pick the alliance.
 * <p>
 * <b>gamepad1 (driver):</b> left stick drive, right stick X turn (cubic shaping), LT hold =
 * brake, A = slow, Y = fast, LB hold = collect (with ball assist), RB hold = eject,
 * B = flower arm (only in ENDGAME, G410), X = reset the element count (if the G407 counter is
 * wrong), BACK = field-centric on/off.
 * <p>
 * <b>gamepad2 (operator):</b> RT hold = fire POLLEN, LT hold = fire NECTAR, RB / LB hold =
 * reverse POLLEN / NECTAR, D-pad up/down = POLLEN RPM +/-, right/left = NECTAR RPM +/-,
 * X / Y / B = mark the last shot SHORT / LONG / WIDE, A = auto-shoot volley (tap again to
 * cancel), BACK = COMPETITION / DEBUG telemetry.
 * <p>
 * Full table and the reasons for each change: {@code docs/agents/teleop.md}.
 * <p>
 * Every loop: clear the bulk cache, read the gamepads, give commands, run the auto-shoot
 * sequence, run every subsystem's {@code periodic()}, then one telemetry update. Nothing
 * blocks; there is no {@code sleep()}.
 */
public abstract class TeleOpBase extends OpMode {

    /** CSV folder of the match log (under /sdcard/FIRST). */
    private static final String MATCH_LOG_FOLDER = "match_logs";
    private static final String MATCH_LOG_HEADER = "time_s,remaining_s,phase,battery_v,"
            + "pollen_target_rpm,pollen_rpm,nectar_target_rpm,nectar_rpm,pollen_shots,"
            + "nectar_shots,intake,indexer,elements,auto_shoot,x_in,y_in,heading_deg,event";

    /** How the last shot missed, marked by the operator for the log. */
    private enum ShotMark { SHORT, LONG, WIDE }

    // ---------------- Robot ----------------
    private RobotHardware hardware;
    private Drivetrain drivetrain;
    private Intake intake;
    private Indexer indexer;
    private Launcher launcher;
    private ColorSensing colorSensing;
    private ElementCounter elementCounter;
    private Vision vision;
    private HiveAligner aligner;
    private Lights lights;
    private BatteryMonitor battery;
    private FlowerArm flowerArm;
    private ShootingSequence autoShoot;
    private List<Subsystem> subsystems;
    private PinpointLocalizer localizer;

    // ---------------- Match state ----------------
    private Alliance alliance;
    private GamepadEx driver;
    private GamepadEx operator;
    private MatchTimer matchTimer;
    private final EndgameAlerts endgameAlerts = new EndgameAlerts();
    private final DataLogger matchLog = new DataLogger();
    private final ElapsedTime loopTimer = new ElapsedTime();
    private double lastLogRowSec = Double.NaN;
    private double lastLogFlushSec = Double.NaN;
    private String handoffText = "none (pose starts at 0, 0, 0)";
    private String odometryText = "off";

    private boolean isCompetitionTelemetry;
    private boolean isFieldCentric;
    private boolean isAimed = true;
    private boolean isAligning = false;
    private boolean wasAligning = false;
    private boolean wasLowBattery = false;
    private boolean wasFull = false;
    private boolean hasTarget = false;
    private boolean isFeedingNow = false;
    private final int[] markCounts = new int[ShotMark.values().length];
    private String pendingEvent = "";
    private double loopMillis = 0.0;

    /** @return the alliance this OpMode plays for */
    protected abstract Alliance getAlliance();

    /** @return a short name for the log files, for example "TeleOpRed" */
    protected abstract String getLogTag();

    // =====================================================================================
    // INIT
    // =====================================================================================

    /** Builds the hardware and subsystems, wires the rule gates and reads the AUTO pose. */
    @Override
    public void init() {
        // Telemetry also goes to FTC Dashboard (http://192.168.43.1:8080/dash) for live graphs.
        telemetry = new MultipleTelemetry(telemetry, FtcDashboard.getInstance().getTelemetry());
        alliance = getAlliance();
        driver = new GamepadEx(gamepad1);
        operator = new GamepadEx(gamepad2);
        matchTimer = new MatchTimer(MatchTimer.systemClock(), false);

        hardware = new RobotHardware(hardwareMap);
        battery = new BatteryMonitor(hardware);
        drivetrain = new Drivetrain(hardware);
        intake = new Intake(hardware);
        indexer = new Indexer(hardware);
        launcher = new Launcher(hardware);
        colorSensing = new ColorSensing(hardware);
        elementCounter = new ElementCounter(colorSensing, launcher);
        vision = new Vision(hardware, alliance);
        aligner = new HiveAligner(vision.getHiveVision(), drivetrain);
        lights = new Lights(hardware);
        flowerArm = new FlowerArm(hardware);
        autoShoot = ShootingSequence.forRobot(launcher, indexer, intake);

        // Order matters: sensors first, ElementCounter after ColorSensing and Launcher, the
        // intake after the gates it reads, lights last.
        subsystems = Arrays.<Subsystem>asList(battery, colorSensing, launcher, elementCounter,
                vision, intake, indexer, drivetrain, flowerArm, lights);

        wireRules();
        drivetrain.setBatteryMonitor(battery);
        setUpOdometry();

        isCompetitionTelemetry = ControlConstants.START_IN_COMPETITION_TELEMETRY;
        isFieldCentric = ControlConstants.START_FIELD_CENTRIC && localizer != null;
        lights.showAlliance(alliance);
    }

    /** G407 collect gate, G408 opponent NECTAR rejection, G410 flower arm gate. */
    private void wireRules() {
        final ElementCounter counter = elementCounter;
        intake.setCollectGate(new BooleanSupplier() {
            @Override
            public boolean getAsBoolean() {
                return counter.canCollectMore();
            }
        });
        final ColorSensing sensing = colorSensing;
        final Alliance ourAlliance = alliance;
        intake.setRejectDetector(new BooleanSupplier() {
            @Override
            public boolean getAsBoolean() {
                return sensing.isOpponentNectarAtIntake(ourAlliance);
            }
        });
        final MatchTimer timer = matchTimer;
        flowerArm.setDeployGate(new BooleanSupplier() {
            @Override
            public boolean getAsBoolean() {
                return timer.canPlaceNectar();
            }
        });
        autoShoot.setAimedSupplier(new BooleanSupplier() {
            @Override
            public boolean getAsBoolean() {
                return isAimed;
            }
        });
    }

    /** Reads the Pinpoint through Pedro's localizer and applies the pose AUTO saved. */
    private void setUpOdometry() {
        if (!ControlConstants.USE_ODOMETRY_IN_TELEOP || !hardware.hasPinpoint()) {
            odometryText = hardware.hasPinpoint() ? "off (USE_ODOMETRY_IN_TELEOP)" : "no Pinpoint";
            return;
        }
        try {
            localizer = new PinpointLocalizer(hardwareMap, PedroConstants.LOCALIZER_CONFIG);
        } catch (RuntimeException e) {
            localizer = null;
            odometryText = "Pinpoint error: " + e.getMessage();
            return;
        }
        odometryText = "on";
        if (PoseStorage.hasPose()) {
            Pose saved = PoseStorage.getPose();
            localizer.setPose(saved);
            Alliance autoAlliance = PoseStorage.getAlliance();
            handoffText = String.format("AUTO pose (%.1f, %.1f, %.0f deg)%s", saved.x(), saved.y(),
                    Math.toDegrees(saved.heading()),
                    autoAlliance != null && autoAlliance != alliance
                            ? " !! AUTO ran as " + autoAlliance + ", check the OpMode" : "");
        }
        final PinpointLocalizer pinpoint = localizer;
        drivetrain.setHeadingSupplier(new DoubleSupplier() {
            @Override
            public double getAsDouble() {
                return pinpoint.pose().heading();
            }
        });
    }

    /** Shows the alliance, hardware warnings and camera status until START. */
    @Override
    public void init_loop() {
        hardware.clearBulkCache();
        battery.periodic();
        vision.periodic();
        colorSensing.periodic();
        if (localizer != null) {
            localizer.update();
        }
        lights.periodic();

        telemetry.addLine("TeleOp " + alliance + " ready - press START");
        if (hardware.hasWarnings()) {
            telemetry.addLine("!! " + hardware.getWarnings().size()
                    + " device(s) missing - check the configuration:");
            hardware.addTelemetry(telemetry);
        } else {
            telemetry.addLine("All devices found");
        }
        telemetry.addData("Cameras", "Limelight %s | HuskyLens %s",
                vision.isConnected() ? "OK" : "MISSING",
                colorSensing.isConnected() ? "OK" : "MISSING");
        telemetry.addData("Odometry", odometryText);
        telemetry.addData("Pose handoff", handoffText);
        telemetry.addData("Flower arm", flowerArm.isAvailable() ? "OK" : "not configured (optional)");
        telemetry.addData("HIVE auto-align", aligner.isEnabled() ? "ON" : "off (VisionConstants)");
        battery.addTelemetry(telemetry);
        telemetry.update();
    }

    /** Starts the match clock and the match log; forgets the AUTO pose. */
    @Override
    public void start() {
        matchTimer.start();
        endgameAlerts.reset();
        PoseStorage.clear();
        if (ControlConstants.MATCH_LOG_ENABLED) {
            matchLog.open(MATCH_LOG_FOLDER, getLogTag(), MATCH_LOG_HEADER);
        }
        loopTimer.reset();
    }

    // =====================================================================================
    // LOOP
    // =====================================================================================

    /** One TeleOp loop. Never blocks. */
    @Override
    public void loop() {
        loopMillis = loopTimer.milliseconds();
        loopTimer.reset();

        hardware.clearBulkCache();
        driver.readButtons();
        operator.readButtons();
        if (localizer != null) {
            localizer.update();
        }

        handleMatchAlerts();
        handleToggles();
        handleDriveAndShooting();

        colorSensing.countWhileIntaking(intake.isCollecting());
        lights.updateNectarAlert(closestNectarColor());
        for (Subsystem subsystem : subsystems) {
            subsystem.periodic();
        }

        writeMatchLog();
        addTelemetry();
        telemetry.update();
    }

    /** Endgame rumbles, low-battery rumble. */
    private void handleMatchAlerts() {
        EndgameAlerts.Alert alert = endgameAlerts.update(matchTimer.getRemainingSeconds());
        if (alert == EndgameAlerts.Alert.FIRST_WARNING) {
            driver.rumbleBlips(ControlConstants.FIRST_ENDGAME_WARNING_BLIPS);
            operator.rumbleBlips(ControlConstants.FIRST_ENDGAME_WARNING_BLIPS);
            pendingEvent = "endgame_warning";
        } else if (alert == EndgameAlerts.Alert.ENDGAME_START) {
            driver.rumbleLong();
            operator.rumbleLong();
            pendingEvent = "endgame_start";
        }

        if (battery.isLow() && !wasLowBattery) {
            gamepad1.rumble(ControlConstants.LOW_BATTERY_RUMBLE_MILLIS);
            gamepad2.rumble(ControlConstants.LOW_BATTERY_RUMBLE_MILLIS);
        }
        wasLowBattery = battery.isLow();
    }

    /** Buttons that switch a mode on a tap. */
    private void handleToggles() {
        if (operator.wasJustPressed(Button.BACK)) {
            isCompetitionTelemetry = !isCompetitionTelemetry;
        }
        if (driver.wasJustPressed(Button.BACK)) {
            if (localizer == null) {
                driver.rumbleShort(); // no heading: field-centric is not possible
            } else {
                isFieldCentric = !isFieldCentric;
                if (isFieldCentric) {
                    // The way the robot faces now becomes "away from the driver".
                    drivetrain.resetFieldHeading();
                }
            }
        }
        if (driver.wasJustPressed(Button.A)) {
            drivetrain.setSlowMode();
        } else if (driver.wasJustPressed(Button.Y)) {
            drivetrain.setFastMode();
        }
        if (driver.wasJustPressed(Button.X)) {
            // Driver override for G407: the HuskyLens count can be wrong.
            elementCounter.reset();
            pendingEvent = "element_count_reset";
        }
        if (driver.wasJustPressed(Button.B)) {
            toggleFlowerArm();
        }
        if (operator.wasJustPressed(Button.DPAD_UP)) {
            launcher.adjustPollenRpm(LauncherConstants.RPM_STEP);
        } else if (operator.wasJustPressed(Button.DPAD_DOWN)) {
            launcher.adjustPollenRpm(-LauncherConstants.RPM_STEP);
        }
        if (operator.wasJustPressed(Button.DPAD_RIGHT)) {
            launcher.adjustNectarRpm(LauncherConstants.RPM_STEP);
        } else if (operator.wasJustPressed(Button.DPAD_LEFT)) {
            launcher.adjustNectarRpm(-LauncherConstants.RPM_STEP);
        }
        if (operator.wasJustPressed(Button.X)) {
            markShot(ShotMark.SHORT);
        } else if (operator.wasJustPressed(Button.Y)) {
            markShot(ShotMark.LONG);
        } else if (operator.wasJustPressed(Button.B)) {
            markShot(ShotMark.WIDE);
        }
    }

    /** Flower arm on gamepad1 B: retract if out, else deploy (refused before ENDGAME, G410). */
    private void toggleFlowerArm() {
        if (flowerArm.isDeployed()) {
            flowerArm.retract();
        } else if (!flowerArm.deploy()) {
            driver.rumbleShort(); // locked until the last 60 s
        }
    }

    /** Drive, aim, intake, launcher, indexer and auto-shoot. */
    private void handleDriveAndShooting() {
        double forward = driver.getShapedLeftY();
        double strafe = driver.getShapedLeftX();
        double turn = driver.getShapedRightX();
        boolean isDriverMoving = TeleOpLogic.isDriverMoving(forward, strafe, turn);

        boolean isCollect = driver.isPressed(Button.LEFT_BUMPER);
        boolean isEject = driver.isPressed(Button.RIGHT_BUMPER);
        boolean isFirePollen = operator.isPressed(Button.RIGHT_TRIGGER);
        boolean isReversePollen = operator.isPressed(Button.RIGHT_BUMPER);
        boolean isFireNectar = operator.isPressed(Button.LEFT_TRIGGER);
        boolean isReverseNectar = operator.isPressed(Button.LEFT_BUMPER);
        boolean isManualMechanism = isCollect || isEject || isFirePollen || isReversePollen
                || isFireNectar || isReverseNectar;

        // Auto-shoot (gamepad2 A): any manual intake or launcher input takes control back.
        if (autoShoot.isRunning() && isManualMechanism) {
            autoShoot.cancel();
            pendingEvent = "auto_shoot_cancelled";
        }
        if (operator.wasJustPressed(Button.A)) {
            if (autoShoot.isRunning()) {
                autoShoot.cancel();
                pendingEvent = "auto_shoot_cancelled";
            } else {
                autoShoot.start(TeleOpLogic.ballsForVolley(elementCounter.getCount(),
                        colorSensing.isConnected()), launcher.getPollenTargetRpm(),
                        ControlConstants.AUTO_SHOOT_MAX_SECONDS);
                pendingEvent = "auto_shoot_start";
            }
        }
        boolean isAutoShooting = autoShoot.isRunning();
        boolean isFiring = isFirePollen || isFireNectar || isAutoShooting;

        // Brake (hold LT): rumble if the driver tries to move while braking.
        drivetrain.setBrake(driver.isPressed(Button.LEFT_TRIGGER));
        if (drivetrain.isBraking() && isDriverMoving && !gamepad1.isRumbling()) {
            gamepad1.rumble(ControlConstants.BRAKE_RUMBLE_POWER, ControlConstants.BRAKE_RUMBLE_POWER,
                    ControlConstants.BRAKE_RUMBLE_MILLIS);
        }

        // HIVE auto-align while firing (off unless VisionConstants.HIVE_ALIGN_ENABLED).
        HiveCell target = vision.getAllianceTarget();
        hasTarget = target != null;
        isAligning = aligner.isEnabled() && isFiring && hasTarget && !drivetrain.isBraking()
                && !isDriverMoving;
        boolean isAligned = false;
        if (isAligning) {
            isAligned = aligner.alignTo(target);
        } else {
            if (wasAligning) {
                aligner.reset();
            }
            // Ball assist: while collecting and not turning, turn toward the closest ball.
            if (isCollect && turn == 0.0) {
                turn = colorSensing.getAssistTurn(TeleOpLogic.assistColors(alliance));
            }
            if (isFieldCentric) {
                drivetrain.driveFieldCentric(forward, strafe, turn);
            } else {
                drivetrain.drive(forward, strafe, turn);
            }
        }
        wasAligning = isAligning;
        isAimed = !isAligning || isAligned;

        if (isAutoShooting) {
            launcher.stopNectar();
            autoShoot.periodic();
            isFeedingNow = indexer.isFeeding();
        } else {
            commandMechanisms(isCollect, isEject, isFirePollen, isReversePollen, isFireNectar,
                    isReverseNectar);
        }

        if (indexer.isJammed() && !gamepad2.isRumbling()) {
            gamepad2.rumble(ControlConstants.JAM_RUMBLE_MILLIS); // release fire to clear it
        }
        if (intake.isFull() && !wasFull) {
            driver.rumbleShort(); // G407: 4 elements, collect refused
        }
        wasFull = intake.isFull();

        showLight(isFiring);
    }

    /** Manual intake, launcher and indexer, as on the tested robot. */
    private void commandMechanisms(boolean isCollect, boolean isEject, boolean isFirePollen,
                                   boolean isReversePollen, boolean isFireNectar,
                                   boolean isReverseNectar) {
        if (isCollect) {
            intake.collect();
        } else if (isEject) {
            intake.eject();
        } else {
            intake.idle();
        }

        if (isFirePollen) {
            launcher.spinUpPollen();
        } else if (isReversePollen) {
            launcher.reversePollen();
        } else {
            launcher.stopPollen();
        }
        if (isFireNectar) {
            launcher.spinUpNectar();
        } else if (isReverseNectar) {
            launcher.reverseNectar();
        } else {
            launcher.stopNectar();
        }

        // The indexer runs forward whenever a flywheel is active. While firing it feeds only
        // when that flywheel is at its target RPM and the robot is aimed (one ball per shot).
        boolean isFiring = isFirePollen || isFireNectar;
        boolean isAnyLauncherActive = isFiring || isReversePollen || isReverseNectar;
        boolean isLauncherReady = isFirePollen ? launcher.isPollenReady() : launcher.isNectarReady();
        isFeedingNow = isFiring && isLauncherReady && isAimed;
        if (isAnyLauncherActive) {
            indexer.feed();
            indexer.setPaused(isFiring && !isFeedingNow);
        } else {
            indexer.idle();
            indexer.setPaused(false);
        }
    }

    /** Sets the indicator light from {@link TeleOpLogic#pickLight}. */
    private void showLight(boolean isFiring) {
        TeleOpLogic.LightCue cue = TeleOpLogic.pickLight(isFiring, isAimed, isFeedingNow,
                matchTimer.isEndgame(), drivetrain.isSlowMode());
        switch (cue) {
            case ALIGNING:
                lights.setWhite();
                break;
            case FEEDING:
                lights.setYellow();
                break;
            case SPINNING_UP:
                lights.blink(RobotConstants.LIGHT_YELLOW);
                break;
            case SLOW:
                lights.setBlue();
                break;
            case ENDGAME_SLOW:
                lights.blink(RobotConstants.LIGHT_BLUE);
                break;
            case ENDGAME_FAST:
                lights.blink(RobotConstants.LIGHT_RED);
                break;
            default:
                lights.setRed();
                break;
        }
    }

    private BallColor closestNectarColor() {
        ColorSensing.Ball nectar = colorSensing.getClosestNectar();
        return nectar == null ? null : nectar.color;
    }

    private void markShot(ShotMark mark) {
        markCounts[mark.ordinal()]++;
        pendingEvent = "mark_" + mark.name().toLowerCase();
        operator.rumbleShort();
    }

    // =====================================================================================
    // LOG AND TELEMETRY
    // =====================================================================================

    /** One CSV row every {@link ControlConstants#MATCH_LOG_INTERVAL_SECONDS}, events right away. */
    private void writeMatchLog() {
        if (!matchLog.isOpen()) {
            pendingEvent = "";
            return;
        }
        double nowSec = matchTimer.getElapsedSeconds();
        boolean isRowDue = Double.isNaN(lastLogRowSec)
                || nowSec - lastLogRowSec >= ControlConstants.MATCH_LOG_INTERVAL_SECONDS;
        if (isRowDue || !pendingEvent.isEmpty()) {
            lastLogRowSec = nowSec;
            Pose pose = localizer == null ? null : localizer.pose();
            matchLog.logRow(nowSec, matchTimer.getRemainingSeconds(), matchTimer.getPhase(),
                    battery.getVolts(), launcher.getPollenTargetRpm(), launcher.getPollenRpm(),
                    launcher.getNectarTargetRpm(), launcher.getNectarRpm(),
                    launcher.getPollenShotCount(), launcher.getNectarShotCount(),
                    intake.getState(), indexer.getState(), elementCounter.getCount(),
                    autoShoot.getState(),
                    pose == null ? "" : pose.x(), pose == null ? "" : pose.y(),
                    pose == null ? "" : Math.toDegrees(pose.heading()), pendingEvent);
            pendingEvent = "";
        }
        if (Double.isNaN(lastLogFlushSec)
                || nowSec - lastLogFlushSec >= ControlConstants.MATCH_LOG_FLUSH_SECONDS) {
            lastLogFlushSec = nowSec;
            matchLog.flush();
        }
    }

    private void addTelemetry() {
        addBanner();
        if (isCompetitionTelemetry) {
            addCompetitionTelemetry();
        } else {
            addDebugTelemetry();
        }
    }

    /** First lines in both modes: match phase, time, and anything the drivers must act on. */
    private void addBanner() {
        String clock = TeleOpLogic.formatClock(matchTimer.getRemainingSeconds());
        if (matchTimer.isEndgame()) {
            telemetry.addLine("*** ENDGAME " + clock + " - NECTAR + flower arm allowed ***");
        } else {
            telemetry.addLine(alliance + " TELEOP " + clock + " (endgame at "
                    + TeleOpLogic.formatClock(MatchConstants.NECTAR_UNLOCK_SECONDS) + ")");
        }
        if (battery.isLow()) {
            telemetry.addLine(String.format("!! BATTERY LOW %.1f V - swap it after this match",
                    battery.getVolts()));
        }
        if (indexer.isJammed()) {
            telemetry.addLine("!! INDEXER JAMMED - release fire to clear it");
        }
        if (lights.getNectarAlertColor() != null) {
            telemetry.addLine("!! NECTAR AT THE INTAKE: " + lights.getNectarAlertColor()
                    + " (eject: gamepad1 RB)");
        }
        if (hardware.hasWarnings()) {
            telemetry.addLine("!! " + hardware.getWarnings().size()
                    + " device(s) missing (gamepad2 BACK = details)");
        }
    }

    /** Short view for matches: what the drivers need, nothing else. */
    private void addCompetitionTelemetry() {
        telemetry.addData("POLLEN", "%.0f / %.0f RPM%s", launcher.getPollenRpm(),
                launcher.getPollenTargetRpm(), launcher.isPollenReady() ? " READY" : "");
        telemetry.addData("Elements", "%d / %d%s", elementCounter.getCount(),
                MatchConstants.MAX_CONTROLLED_ELEMENTS,
                intake.isFull() ? " FULL (gamepad1 X resets)" : "");
        telemetry.addData("HIVE TIPs", "%d", vision.getTipCount());
        telemetry.addData("Drive", "%s, %s%s", drivetrain.isSlowMode() ? "SLOW" : "FAST",
                isFieldCentric ? "field-centric" : "robot-centric",
                drivetrain.isBraking() ? ", BRAKE" : "");
        if (autoShoot.isRunning()) {
            telemetry.addData("Auto-shoot", "%s, %d shots", autoShoot.getState(),
                    autoShoot.getShotsFired());
        }
        if (flowerArm.isDeployed()) {
            telemetry.addLine("Flower arm DEPLOYED");
        }
    }

    /** Full view for the pits and testing: every subsystem plus graph values. */
    private void addDebugTelemetry() {
        hardware.addTelemetry(telemetry);
        telemetry.addData("Loop", "%.0f ms", loopMillis);
        telemetry.addData("Drive mode", "%s, %s, brake %s", drivetrain.isSlowMode() ? "SLOW" : "FAST",
                isFieldCentric ? "field-centric" : "robot-centric",
                drivetrain.isBraking() ? "ON" : "OFF");
        if (localizer != null) {
            Pose pose = localizer.pose();
            telemetry.addData("Pose (in, deg)", "x %.1f y %.1f h %.1f", pose.x(), pose.y(),
                    Math.toDegrees(pose.heading()));
        } else {
            telemetry.addData("Pose", odometryText);
        }
        telemetry.addData("Align", !aligner.isEnabled() ? "disabled"
                : !hasTarget ? "no target (manual aim)"
                : isAligning ? (isAimed ? "ALIGNED" : "ALIGNING") : "idle");
        telemetry.addData("Feed", isFeedingNow ? "FEEDING" : "-");
        telemetry.addData("Auto-shoot", "%s, %d shots, %d shakes", autoShoot.getState(),
                autoShoot.getShotsFired(), autoShoot.getShakeCount());
        for (Subsystem subsystem : subsystems) {
            subsystem.addTelemetry(telemetry);
        }
        telemetry.addData("Rejected opponent NECTAR", intake.getRejectCount());
        telemetry.addData("Shot marks", "short %d | long %d | wide %d",
                markCounts[ShotMark.SHORT.ordinal()], markCounts[ShotMark.LONG.ordinal()],
                markCounts[ShotMark.WIDE.ordinal()]);
        telemetry.addData("Match log", matchLog.isOpen()
                ? matchLog.getFileName() + " (" + matchLog.getRowCount() + " rows)"
                : "off " + matchLog.getLastError());
        // Plain numbers so FTC Dashboard can graph them.
        telemetry.addData("Graph/PollenRpm", launcher.getPollenRpm());
        telemetry.addData("Graph/PollenTargetRpm", launcher.getPollenTargetRpm());
        telemetry.addData("Graph/NectarRpm", launcher.getNectarRpm());
        telemetry.addData("Graph/NectarTargetRpm", launcher.getNectarTargetRpm());
        telemetry.addData("Graph/FeedReady", isFeedingNow ? 1 : 0);
        telemetry.addData("Graph/LoopMs", loopMillis);
        telemetry.addData("Graph/BatteryVolts", battery.getVolts());
        telemetry.addLine("gamepad2 BACK = competition telemetry");
    }

    // =====================================================================================
    // STOP
    // =====================================================================================

    /** Stops every mechanism and closes the match log. */
    @Override
    public void stop() {
        if (autoShoot != null) {
            autoShoot.cancel();
        }
        if (subsystems != null) {
            for (Subsystem subsystem : subsystems) {
                subsystem.stop();
            }
        }
        matchLog.close();
    }
}
