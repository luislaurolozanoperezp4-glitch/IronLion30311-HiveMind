// Sources: IronLion30311BIOBUZZInicioDeTemporada / branch feature/turrubiates/vision-align / TeamCode/.../opmodes/teleop/MainTeleOp.java @ a7b8f4c (Graph/* telemetry for FTC Dashboard, D-pad RPM steps)
//          IronLion30311BIOBUZZInicioDeTemporada / branch feature/turrubiates/vision-align / TeamCode/.../subsystems/ShooterLogger.java @ a7b8f4c (launcher CSV columns)
//          — adapted for BIOBUZZ (new OpMode: hold an RPM and tune the gains live, matrix §2.6 TestLauncherRpm)
package org.firstinspires.ftc.teamcode.opmodes.tuning;

import com.acmerobotics.dashboard.FtcDashboard;
import com.acmerobotics.dashboard.telemetry.MultipleTelemetry;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.util.ElapsedTime;

import org.firstinspires.ftc.teamcode.config.LauncherConstants;
import org.firstinspires.ftc.teamcode.config.TuningConstants;
import org.firstinspires.ftc.teamcode.hardware.RobotHardware;
import org.firstinspires.ftc.teamcode.subsystems.BatteryMonitor;
import org.firstinspires.ftc.teamcode.subsystems.Indexer;
import org.firstinspires.ftc.teamcode.subsystems.Launcher;
import org.firstinspires.ftc.teamcode.util.DataLogger;
import org.firstinspires.ftc.teamcode.util.GamepadEx;
import org.firstinspires.ftc.teamcode.util.GamepadEx.Button;

/**
 * Holds a flywheel at a target RPM so the velocity PIDF can be tuned, with a live graph.
 * <p>
 * Open FTC Dashboard (http://192.168.43.1:8080/dash) and graph {@code Graph/TargetRpm} and
 * {@code Graph/Rpm}. Every row also goes to {@code /sdcard/FIRST/launcher_logs/*.csv}.
 * <p>
 * gamepad1:
 * <ul>
 *   <li>A: POLLEN flywheel on/off. B: NECTAR flywheel on/off.</li>
 *   <li>X: choose which flywheel the D-pad and the graph use.</li>
 *   <li>D-pad up/down: target +/- {@link LauncherConstants#RPM_STEP};
 *   right/left: +/- {@link TuningConstants#LAUNCHER_BIG_RPM_STEP}.</li>
 *   <li>Y: choose the gain to change (P, I, D, F at 12 V); RB / LB: that gain + / -.</li>
 *   <li>RT (hold): run the indexer (feeds only while the chosen flywheel is ready), to watch
 *   the RPM dip and recovery of real shots.</li>
 * </ul>
 * The gains are changed in {@link LauncherConstants} (same as editing them on the Dashboard).
 * They reset when the Robot Controller app restarts: <b>copy the good values into
 * {@code LauncherConstants.java}</b> and mark them TESTED.
 */
@TeleOp(name = "Tune Launcher PIDF", group = "Tuning")
public class TuneLauncherPidf extends OpMode {

    private static final String LOG_FOLDER = "launcher_logs";
    private static final String LOG_HEADER = "time_s,flywheel,target_rpm,rpm,error_rpm,ready,"
            + "kp,ki,kd,kf_12v,kf_applied,battery_v,indexer,shots";

    private enum Flywheel { POLLEN, NECTAR }

    private enum Gain { P, I, D, F }

    private RobotHardware hardware;
    private Launcher launcher;
    private Indexer indexer;
    private BatteryMonitor battery;
    private GamepadEx pad;
    private final DataLogger log = new DataLogger();
    private final ElapsedTime runTimer = new ElapsedTime();
    private final ElapsedTime spinUpTimer = new ElapsedTime();

    private Flywheel selected = Flywheel.POLLEN;
    private Gain selectedGain = Gain.P;
    private boolean isPollenOn = false;
    private boolean isNectarOn = false;
    private boolean wasReady = false;
    private double lastSpinUpSec = Double.NaN;
    private double lastLogSec = Double.NaN;

    /** Builds the launcher, indexer and battery monitor. */
    @Override
    public void init() {
        telemetry = new MultipleTelemetry(telemetry, FtcDashboard.getInstance().getTelemetry());
        hardware = new RobotHardware(hardwareMap);
        launcher = new Launcher(hardware);
        indexer = new Indexer(hardware);
        battery = new BatteryMonitor(hardware);
        pad = new GamepadEx(gamepad1);
        hardware.addTelemetry(telemetry);
        telemetry.addLine("Keep hands away from the flywheels. Press START.");
    }

    /** Opens the CSV log. */
    @Override
    public void start() {
        log.open(LOG_FOLDER, "TuneLauncherPidf", LOG_HEADER);
        runTimer.reset();
    }

    /** Reads the gamepad, runs the launcher, logs and graphs. */
    @Override
    public void loop() {
        hardware.clearBulkCache();
        pad.readButtons();
        handleButtons();

        if (isPollenOn) {
            launcher.spinUpPollen();
        } else {
            launcher.stopPollen();
        }
        if (isNectarOn) {
            launcher.spinUpNectar();
        } else {
            launcher.stopNectar();
        }
        boolean isReady = selected == Flywheel.POLLEN ? launcher.isPollenReady()
                : launcher.isNectarReady();
        if (pad.isPressed(Button.RIGHT_TRIGGER)) {
            indexer.feed();
            indexer.setPaused(!isReady);
        } else {
            indexer.idle();
            indexer.setPaused(false);
        }

        battery.periodic();
        launcher.periodic();
        indexer.periodic();
        trackSpinUp();
        writeLog();
        addTelemetry();
    }

    private void handleButtons() {
        if (pad.wasJustPressed(Button.A)) {
            isPollenOn = !isPollenOn;
            restartSpinUpTimer(Flywheel.POLLEN, isPollenOn);
        }
        if (pad.wasJustPressed(Button.B)) {
            isNectarOn = !isNectarOn;
            restartSpinUpTimer(Flywheel.NECTAR, isNectarOn);
        }
        if (pad.wasJustPressed(Button.X)) {
            selected = selected == Flywheel.POLLEN ? Flywheel.NECTAR : Flywheel.POLLEN;
        }
        if (pad.wasJustPressed(Button.DPAD_UP)) {
            changeTarget(LauncherConstants.RPM_STEP);
        } else if (pad.wasJustPressed(Button.DPAD_DOWN)) {
            changeTarget(-LauncherConstants.RPM_STEP);
        } else if (pad.wasJustPressed(Button.DPAD_RIGHT)) {
            changeTarget(TuningConstants.LAUNCHER_BIG_RPM_STEP);
        } else if (pad.wasJustPressed(Button.DPAD_LEFT)) {
            changeTarget(-TuningConstants.LAUNCHER_BIG_RPM_STEP);
        }
        if (pad.wasJustPressed(Button.Y)) {
            selectedGain = Gain.values()[(selectedGain.ordinal() + 1) % Gain.values().length];
        }
        if (pad.wasJustPressed(Button.RIGHT_BUMPER)) {
            changeGain(1.0);
        } else if (pad.wasJustPressed(Button.LEFT_BUMPER)) {
            changeGain(-1.0);
        }
    }

    private void restartSpinUpTimer(Flywheel flywheel, boolean isOn) {
        if (flywheel == selected && isOn) {
            spinUpTimer.reset();
            wasReady = false;
            lastSpinUpSec = Double.NaN;
        }
    }

    private void changeTarget(double deltaRpm) {
        if (selected == Flywheel.POLLEN) {
            launcher.adjustPollenRpm(deltaRpm);
        } else {
            launcher.adjustNectarRpm(deltaRpm);
        }
        spinUpTimer.reset();
        wasReady = false;
    }

    /** @param sign +1 to raise, -1 to lower the selected gain by its step (never below 0) */
    private void changeGain(double sign) {
        switch (selectedGain) {
            case P:
                LauncherConstants.KP = Math.max(0.0,
                        LauncherConstants.KP + sign * TuningConstants.LAUNCHER_KP_STEP);
                break;
            case I:
                LauncherConstants.KI = Math.max(0.0,
                        LauncherConstants.KI + sign * TuningConstants.LAUNCHER_KI_STEP);
                break;
            case D:
                LauncherConstants.KD = Math.max(0.0,
                        LauncherConstants.KD + sign * TuningConstants.LAUNCHER_KD_STEP);
                break;
            default:
                LauncherConstants.KF_AT_12V = Math.max(0.0,
                        LauncherConstants.KF_AT_12V + sign * TuningConstants.LAUNCHER_KF_STEP);
                break;
        }
    }

    /** Remembers how long the selected flywheel took to become ready after a change. */
    private void trackSpinUp() {
        boolean isReady = selected == Flywheel.POLLEN ? launcher.isPollenReady()
                : launcher.isNectarReady();
        if (isReady && !wasReady) {
            lastSpinUpSec = spinUpTimer.seconds();
        }
        wasReady = isReady;
    }

    private double selectedTarget() {
        return selected == Flywheel.POLLEN ? launcher.getPollenTargetRpm()
                : launcher.getNectarTargetRpm();
    }

    private double selectedRpm() {
        return selected == Flywheel.POLLEN ? launcher.getPollenRpm() : launcher.getNectarRpm();
    }

    private void writeLog() {
        double nowSec = runTimer.seconds();
        if (!Double.isNaN(lastLogSec)
                && nowSec - lastLogSec < TuningConstants.LAUNCHER_LOG_INTERVAL_SECONDS) {
            return;
        }
        lastLogSec = nowSec;
        log.logRow(nowSec, selected, selectedTarget(), selectedRpm(),
                selectedTarget() - selectedRpm(), wasReady ? 1 : 0, LauncherConstants.KP,
                LauncherConstants.KI, LauncherConstants.KD, LauncherConstants.KF_AT_12V,
                launcher.getAppliedKf(), battery.getVolts(), indexer.getState(),
                launcher.getTotalShotCount());
    }

    private void addTelemetry() {
        telemetry.addData("Flywheel (X)", "%s | POLLEN %s, NECTAR %s", selected,
                isPollenOn ? "ON" : "off", isNectarOn ? "ON" : "off");
        telemetry.addData("Target / actual", "%.0f / %.0f RPM (error %.0f)%s", selectedTarget(),
                selectedRpm(), selectedTarget() - selectedRpm(), wasReady ? " READY" : "");
        telemetry.addData("Spin-up time", Double.isNaN(lastSpinUpSec) ? "-"
                : String.format("%.2f s", lastSpinUpSec));
        telemetry.addData("Gain (Y, RB+/LB-)", "%s | P %.2f  I %.2f  D %.2f  F@12V %.2f (applied %.2f)",
                selectedGain, LauncherConstants.KP, LauncherConstants.KI, LauncherConstants.KD,
                LauncherConstants.KF_AT_12V, launcher.getAppliedKf());
        launcher.addTelemetry(telemetry);
        indexer.addTelemetry(telemetry);
        battery.addTelemetry(telemetry);
        telemetry.addData("Log", log.isOpen() ? log.getFileName() : "off " + log.getLastError());
        telemetry.addData("Graph/TargetRpm", selectedTarget());
        telemetry.addData("Graph/Rpm", selectedRpm());
        telemetry.addData("Graph/ErrorRpm", selectedTarget() - selectedRpm());
        telemetry.addLine("A pollen | B nectar | X select | D-pad target | Y gain | RB/LB +/- | RT feed");
        telemetry.addLine("Copy good gains into LauncherConstants.java");
    }

    /** Stops the flywheels and the indexer, closes the log. */
    @Override
    public void stop() {
        if (launcher != null) {
            launcher.stop();
        }
        if (indexer != null) {
            indexer.stop();
        }
        log.close();
    }
}
