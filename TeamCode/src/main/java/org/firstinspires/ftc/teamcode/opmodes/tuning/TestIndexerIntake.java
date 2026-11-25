// Sources: IronLion30311BIOBUZZInicioDeTemporada / branch feature/turrubiates/vision-align / TeamCode/.../subsystems/Indexer.java @ a7b8f4c (jam current threshold and time, the values this OpMode helps to tune)
//          — adapted for BIOBUZZ (new test OpMode: manual power and current readout)
package org.firstinspires.ftc.teamcode.opmodes.tuning;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.util.ElapsedTime;

import org.firstinspires.ftc.robotcore.external.navigation.CurrentUnit;
import org.firstinspires.ftc.teamcode.config.IndexerConstants;
import org.firstinspires.ftc.teamcode.config.IntakeConstants;
import org.firstinspires.ftc.teamcode.config.TuningConstants;
import org.firstinspires.ftc.teamcode.hardware.RobotHardware;
import org.firstinspires.ftc.teamcode.util.DataLogger;
import org.firstinspires.ftc.teamcode.util.GamepadEx;
import org.firstinspires.ftc.teamcode.util.GamepadEx.Button;

/**
 * Runs the intake and the indexer by hand and shows their motor current, to tune the jam
 * protection ({@link IndexerConstants#JAM_CURRENT_AMPS}, {@link IndexerConstants#JAM_TIME_SECONDS}).
 * <p>
 * The motors are driven directly (no ramp, no jam protection), so watch them.
 * <p>
 * gamepad1:
 * <ul>
 *   <li>Left stick Y: intake power (up = pull in). Right stick Y: indexer power (up = feed).</li>
 *   <li>A (hold): intake at {@link IntakeConstants#COLLECT_POWER}.
 *   B (hold): indexer at {@link IndexerConstants#FEED_POWER}.</li>
 *   <li>X: reset the peak currents and the "time above threshold".</li>
 * </ul>
 * Procedure: feed balls normally and note the current; then hold a ball so it jams and note the
 * current and how fast it rises. {@code JAM_CURRENT_AMPS} should sit between the two, and
 * {@code JAM_TIME_SECONDS} longer than the normal peaks. Every row is also written to
 * {@code /sdcard/FIRST/current_logs/*.csv}.
 */
@TeleOp(name = "Test Indexer Intake", group = "Tuning")
public class TestIndexerIntake extends OpMode {

    private static final String LOG_FOLDER = "current_logs";
    private static final String LOG_HEADER = "time_s,intake_power,intake_amps,indexer_power,"
            + "indexer_amps,indexer_above_jam_s";

    private RobotHardware hardware;
    private GamepadEx pad;
    private final DataLogger log = new DataLogger();
    private final ElapsedTime runTimer = new ElapsedTime();
    private final ElapsedTime loopTimer = new ElapsedTime();
    private double lastLogSec = Double.NaN;

    private double intakePeakAmps = 0.0;
    private double indexerPeakAmps = 0.0;
    private double indexerAboveJamSec = 0.0;
    private double indexerLongestAboveJamSec = 0.0;

    /** Finds the motors. */
    @Override
    public void init() {
        hardware = new RobotHardware(hardwareMap);
        pad = new GamepadEx(gamepad1);
        hardware.addTelemetry(telemetry);
        telemetry.addLine("Left stick = intake, right stick = indexer, A/B = normal powers");
    }

    /** Opens the current log. */
    @Override
    public void start() {
        log.open(LOG_FOLDER, "TestIndexerIntake", LOG_HEADER);
        runTimer.reset();
        loopTimer.reset();
    }

    /** Applies the powers and reads the currents. */
    @Override
    public void loop() {
        double dtSec = loopTimer.seconds();
        loopTimer.reset();
        hardware.clearBulkCache();
        pad.readButtons();
        if (pad.wasJustPressed(Button.X)) {
            intakePeakAmps = 0.0;
            indexerPeakAmps = 0.0;
            indexerAboveJamSec = 0.0;
            indexerLongestAboveJamSec = 0.0;
        }

        double intakePower = pad.isPressed(Button.A) ? IntakeConstants.COLLECT_POWER
                : -gamepad1.left_stick_y;
        double indexerPower = pad.isPressed(Button.B) ? IndexerConstants.FEED_POWER
                : -gamepad1.right_stick_y;
        double intakeAmps = applyAndRead(hardware.intakeMotor, intakePower);
        double indexerAmps = applyAndRead(hardware.indexerMotor, indexerPower);

        intakePeakAmps = Math.max(intakePeakAmps, intakeAmps);
        indexerPeakAmps = Math.max(indexerPeakAmps, indexerAmps);
        if (indexerAmps >= IndexerConstants.JAM_CURRENT_AMPS) {
            indexerAboveJamSec += dtSec;
            indexerLongestAboveJamSec = Math.max(indexerLongestAboveJamSec, indexerAboveJamSec);
        } else {
            indexerAboveJamSec = 0.0;
        }

        writeLog(intakePower, intakeAmps, indexerPower, indexerAmps);

        telemetry.addData("Intake", "%s power %.2f | %.2f A (peak %.2f)",
                hardware.intakeMotor == null ? "MISSING" : "", intakePower, intakeAmps,
                intakePeakAmps);
        telemetry.addData("Indexer", "%s power %.2f | %.2f A (peak %.2f)",
                hardware.indexerMotor == null ? "MISSING" : "", indexerPower, indexerAmps,
                indexerPeakAmps);
        telemetry.addData("Indexer above jam", "%.2f s now, longest %.2f s (jam = %.1f A for %.2f s)",
                indexerAboveJamSec, indexerLongestAboveJamSec, IndexerConstants.JAM_CURRENT_AMPS,
                IndexerConstants.JAM_TIME_SECONDS);
        telemetry.addData("Would jam-stop", indexerAboveJamSec >= IndexerConstants.JAM_TIME_SECONDS
                ? "YES" : "no");
        telemetry.addData("Log", log.isOpen() ? log.getFileName() : "off " + log.getLastError());
        telemetry.addLine("Sticks = power | A intake, B indexer | X reset peaks");
    }

    /** @return the motor current in amps after setting its power (0 if the motor is missing) */
    private static double applyAndRead(DcMotorEx motor, double power) {
        if (motor == null) {
            return 0.0;
        }
        motor.setPower(power);
        return motor.getCurrent(CurrentUnit.AMPS);
    }

    private void writeLog(double intakePower, double intakeAmps, double indexerPower,
                          double indexerAmps) {
        double nowSec = runTimer.seconds();
        if (!Double.isNaN(lastLogSec)
                && nowSec - lastLogSec < TuningConstants.CURRENT_LOG_INTERVAL_SECONDS) {
            return;
        }
        lastLogSec = nowSec;
        log.logRow(nowSec, intakePower, intakeAmps, indexerPower, indexerAmps, indexerAboveJamSec);
    }

    /** Stops both motors and closes the log. */
    @Override
    public void stop() {
        if (hardware != null) {
            applyAndRead(hardware.intakeMotor, 0.0);
            applyAndRead(hardware.indexerMotor, 0.0);
        }
        log.close();
    }
}
