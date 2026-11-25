// Sources: IronLion30311BIOBUZZInicioDeTemporada / branch feature/turrubiates/vision-align / TeamCode/.../RobotHardware.java @ a7b8f4c (constructor, getDevice with legacy names, motor setup, optional cameras)
//          IronLion30311BIOBUZZInicioDeTemporada / branch master / TeamCode/.../RobotHardware.java @ 37b7a16 (pinpoint lookup)
//          IronLion30311BIOBUZZInicioDeTemporada / branch feature/turrubiates/vision-align / TeamCode/.../subsystems/pathing/PinpointDiagnostics.java @ 023ae35 (Pinpoint device kept for the health read)
//          IronLion30311BIOBUZZInicioDeTemporada / branch feature/turrubiates/vision-align / TeamCode/.../RobotHardware.java @ 344254b (sweeper servos as ServoImplEx with the goBILDA 500-2500 us range; replaces the feature/renata/flower-arm servos @ 889102d)
//          — adapted for BIOBUZZ (fault tolerant: missing devices give warnings, MANUAL bulk caching)
package org.firstinspires.ftc.teamcode.hardware;

import com.qualcomm.hardware.dfrobot.HuskyLens;
import com.qualcomm.hardware.gobilda.GoBildaPinpointDriver;
import com.qualcomm.hardware.limelightvision.Limelight3A;
import com.qualcomm.hardware.lynx.LynxModule;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.PwmControl;
import com.qualcomm.robotcore.hardware.Servo;
import com.qualcomm.robotcore.hardware.ServoImplEx;
import com.qualcomm.robotcore.hardware.VoltageSensor;

import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.teamcode.config.DriveConstants;
import org.firstinspires.ftc.teamcode.config.FlowerSweeperConstants;
import org.firstinspires.ftc.teamcode.config.IndexerConstants;
import org.firstinspires.ftc.teamcode.config.IntakeConstants;
import org.firstinspires.ftc.teamcode.config.LauncherConstants;
import org.firstinspires.ftc.teamcode.config.RobotConstants;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * The only class that touches the {@link HardwareMap}.
 * <p>
 * It finds every device by its Driver Station name (from {@link RobotConstants}), sets its
 * direction and mode (from the {@code config/*Constants} classes), and hands it to the
 * subsystems. Subsystems never call {@code hardwareMap} themselves.
 * <p>
 * <b>Fault tolerant:</b> if a device is missing, its field is {@code null}, a warning is added
 * to {@link #getWarnings()}, and the OpMode keeps running. Each subsystem checks for
 * {@code null} and simply does nothing. Use the {@code hasX()} methods to check.
 * <p>
 * <b>Bulk reads:</b> every hub is put in MANUAL bulk-caching mode. Call
 * {@link #clearBulkCache()} once at the start of every loop; then all encoder, velocity and
 * current reads of that loop come from one hub transaction.
 */
public class RobotHardware {

    // ---------------- Drivetrain ----------------
    public final DcMotorEx frontLeftDrive;
    public final DcMotorEx frontRightDrive;
    public final DcMotorEx backLeftDrive;
    public final DcMotorEx backRightDrive;

    // ---------------- Mechanisms ----------------
    public final DcMotorEx intakeMotor;
    public final DcMotorEx indexerMotor;
    public final DcMotorEx pollenLauncherMotor;
    public final DcMotorEx nectarLauncherMotor;

    // ---------------- Lights ----------------
    public final Servo indicatorLight;
    /** Optional second light for the NECTAR alert (decision D12). */
    public final Servo nectarAlertLight;

    // ---------------- Optional FLOWER sweeper (vision-align 344254b) ----------------
    /** Mirrored goBILDA Dual Mode servos of the FLOWER sweeper bar, or null. */
    public final ServoImplEx sweeperLeftServo;
    public final ServoImplEx sweeperRightServo;

    // ---------------- Sensors and cameras ----------------
    public final VoltageSensor batteryVoltageSensor;
    /** Limelight 3A on the rear (launcher side), for HIVE AprilTags. */
    public final Limelight3A limelight;
    /** HuskyLens on the front (intake side), for POLLEN and NECTAR by color. */
    public final HuskyLens huskyLens;
    /**
     * goBILDA Pinpoint, or null. Pedro Pathing owns it (pose); only
     * {@code pathing.PinpointDiagnostics} reads its status here. Never set its pose from here.
     */
    public final GoBildaPinpointDriver pinpoint;

    private final HardwareMap hardwareMap;
    private final List<LynxModule> hubs;
    private final boolean hasPinpoint;
    private final List<String> warnings = new ArrayList<>();

    /**
     * Finds and configures every device.
     *
     * @param hardwareMap the hardware map from the OpMode
     */
    public RobotHardware(HardwareMap hardwareMap) {
        this.hardwareMap = hardwareMap;

        hubs = hardwareMap.getAll(LynxModule.class);
        for (LynxModule hub : hubs) {
            hub.setBulkCachingMode(LynxModule.BulkCachingMode.MANUAL);
        }

        frontLeftDrive = getDevice(DcMotorEx.class, RobotConstants.FRONT_LEFT_DRIVE,
                RobotConstants.FRONT_LEFT_DRIVE_LEGACY);
        frontRightDrive = getDevice(DcMotorEx.class, RobotConstants.FRONT_RIGHT_DRIVE,
                RobotConstants.FRONT_RIGHT_DRIVE_LEGACY);
        backLeftDrive = getDevice(DcMotorEx.class, RobotConstants.BACK_LEFT_DRIVE,
                RobotConstants.BACK_LEFT_DRIVE_LEGACY);
        backRightDrive = getDevice(DcMotorEx.class, RobotConstants.BACK_RIGHT_DRIVE,
                RobotConstants.BACK_RIGHT_DRIVE_LEGACY);
        configureDriveMotor(frontLeftDrive, DriveConstants.FRONT_LEFT_DIRECTION);
        configureDriveMotor(backLeftDrive, DriveConstants.BACK_LEFT_DIRECTION);
        configureDriveMotor(frontRightDrive, DriveConstants.FRONT_RIGHT_DIRECTION);
        configureDriveMotor(backRightDrive, DriveConstants.BACK_RIGHT_DIRECTION);

        // Intake: positive power pulls balls IN. REVERSE is TESTED (FORWARD pushed them out).
        intakeMotor = getDevice(DcMotorEx.class, RobotConstants.INTAKE_MOTOR,
                RobotConstants.INTAKE_MOTOR_LEGACY);
        configureOpenLoopMotor(intakeMotor, IntakeConstants.DIRECTION);

        // Indexer: positive power feeds balls into the launchers.
        indexerMotor = getDevice(DcMotorEx.class, RobotConstants.INDEXER_MOTOR,
                RobotConstants.INDEXER_MOTOR_LEGACY);
        configureOpenLoopMotor(indexerMotor, IndexerConstants.DIRECTION);

        // Launcher flywheels: closed-loop velocity needs RUN_USING_ENCODER. The PIDF gains are
        // owned by the Launcher subsystem, which rescales F with the battery voltage.
        pollenLauncherMotor = getDevice(DcMotorEx.class, RobotConstants.POLLEN_LAUNCHER_MOTOR,
                RobotConstants.POLLEN_LAUNCHER_MOTOR_LEGACY);
        nectarLauncherMotor = getDevice(DcMotorEx.class, RobotConstants.NECTAR_LAUNCHER_MOTOR,
                RobotConstants.NECTAR_LAUNCHER_MOTOR_LEGACY);
        configureLauncherMotor(pollenLauncherMotor);
        configureLauncherMotor(nectarLauncherMotor);

        indicatorLight = getDevice(Servo.class, RobotConstants.INDICATOR_LIGHT,
                RobotConstants.INDICATOR_LIGHT_LEGACY);
        nectarAlertLight = getOptionalDevice(Servo.class, RobotConstants.NECTAR_ALERT_LIGHT);

        // FLOWER sweeper: the full 300 deg of the goBILDA Dual Mode servos needs 500-2500 us
        sweeperLeftServo = getOptionalDevice(ServoImplEx.class, RobotConstants.SWEEPER_LEFT_SERVO,
                RobotConstants.SWEEPER_LEFT_SERVO_LEGACY);
        sweeperRightServo = getOptionalDevice(ServoImplEx.class, RobotConstants.SWEEPER_RIGHT_SERVO,
                RobotConstants.SWEEPER_RIGHT_SERVO_LEGACY);
        PwmControl.PwmRange sweeperRange = new PwmControl.PwmRange(
                FlowerSweeperConstants.PWM_MIN_MICROSECONDS, FlowerSweeperConstants.PWM_MAX_MICROSECONDS);
        if (sweeperLeftServo != null) {
            sweeperLeftServo.setPwmRange(sweeperRange);
        }
        if (sweeperRightServo != null) {
            sweeperRightServo.setPwmRange(sweeperRange);
        }

        batteryVoltageSensor = findVoltageSensor();
        limelight = getDevice(Limelight3A.class, RobotConstants.LIMELIGHT);
        huskyLens = getDevice(HuskyLens.class, RobotConstants.HUSKY_LENS);

        // Pedro Pathing owns the Pinpoint; here we only check that it is configured.
        pinpoint = tryGet(GoBildaPinpointDriver.class, RobotConstants.PINPOINT);
        hasPinpoint = pinpoint != null;
        if (!hasPinpoint) {
            warnings.add("Missing pinpoint \"" + RobotConstants.PINPOINT + "\" (odometry)");
        }
    }

    /**
     * Looks up a device by its Driver Station name, then by each legacy name.
     * If none is found it records a warning and returns {@code null} (it never throws).
     *
     * @param type        device class, for example {@code DcMotorEx.class}
     * @param name        current Driver Station name
     * @param legacyNames older names still accepted on some Control Hubs
     * @param <T>         device type
     * @return the device, or {@code null} if it is not in the configuration
     */
    public final <T> T getDevice(Class<? extends T> type, String name, String... legacyNames) {
        T device = findDevice(type, name, legacyNames);
        if (device == null) {
            warnings.add("Missing " + type.getSimpleName() + " \"" + name + "\""
                    + (legacyNames.length > 0 ? " (or legacy " + join(legacyNames) + ")" : ""));
        }
        return device;
    }

    /** Same as {@link #getDevice} but without a warning: for devices that may be absent. */
    private <T> T getOptionalDevice(Class<? extends T> type, String name, String... legacyNames) {
        return findDevice(type, name, legacyNames);
    }

    private <T> T findDevice(Class<? extends T> type, String name, String... legacyNames) {
        T device = tryGet(type, name);
        for (int i = 0; device == null && i < legacyNames.length; i++) {
            device = tryGet(type, legacyNames[i]);
        }
        return device;
    }

    private <T> T tryGet(Class<? extends T> type, String name) {
        try {
            return hardwareMap.tryGet(type, name);
        } catch (RuntimeException e) {
            // A device with this name but another type: treat it as missing.
            return null;
        }
    }

    /** Clears the bulk-read cache of every hub. Call once at the start of every loop. */
    public void clearBulkCache() {
        for (LynxModule hub : hubs) {
            hub.clearBulkCache();
        }
    }

    /** @return battery voltage in volts, or 0 if there is no voltage sensor */
    public double getBatteryVolts() {
        return batteryVoltageSensor == null ? 0.0 : batteryVoltageSensor.getVoltage();
    }

    /** @return one line per missing device (empty if everything was found) */
    public List<String> getWarnings() {
        return Collections.unmodifiableList(warnings);
    }

    /** @return true if at least one device is missing */
    public boolean hasWarnings() {
        return !warnings.isEmpty();
    }

    /** Shows every hardware warning on the telemetry (nothing if all devices were found). */
    public void addTelemetry(Telemetry t) {
        for (String warning : warnings) {
            t.addLine("HARDWARE WARNING: " + warning);
        }
    }

    /** @return true if all four drive motors were found */
    public boolean hasDrivetrain() {
        return frontLeftDrive != null && frontRightDrive != null
                && backLeftDrive != null && backRightDrive != null;
    }

    /** @return true if the intake motor was found */
    public boolean hasIntake() {
        return intakeMotor != null;
    }

    /** @return true if the indexer motor was found */
    public boolean hasIndexer() {
        return indexerMotor != null;
    }

    /** @return true if the POLLEN flywheel motor was found */
    public boolean hasPollenLauncher() {
        return pollenLauncherMotor != null;
    }

    /** @return true if the NECTAR flywheel motor was found */
    public boolean hasNectarLauncher() {
        return nectarLauncherMotor != null;
    }

    /** @return true if the indicator light was found */
    public boolean hasIndicatorLight() {
        return indicatorLight != null;
    }

    /** @return true if the optional NECTAR alert light was found */
    public boolean hasNectarAlertLight() {
        return nectarAlertLight != null;
    }

    /** @return true if both FLOWER sweeper servos were found */
    public boolean hasFlowerSweeper() {
        return sweeperLeftServo != null && sweeperRightServo != null;
    }

    /** @return true if the Limelight was found */
    public boolean hasLimelight() {
        return limelight != null;
    }

    /** @return true if the HuskyLens was found */
    public boolean hasHuskyLens() {
        return huskyLens != null;
    }

    /** @return true if the Pinpoint is configured (Pedro Pathing reads it) */
    public boolean hasPinpoint() {
        return hasPinpoint;
    }

    /** @return true if a battery voltage sensor was found */
    public boolean hasBatterySensor() {
        return batteryVoltageSensor != null;
    }

    private VoltageSensor findVoltageSensor() {
        Iterator<VoltageSensor> sensors = hardwareMap.voltageSensor.iterator();
        if (sensors.hasNext()) {
            return sensors.next();
        }
        warnings.add("Missing battery voltage sensor");
        return null;
    }

    /**
     * Drive motor setup (TESTED): BRAKE so the robot stops instead of coasting, and
     * RUN_WITHOUT_ENCODER for full open-loop power while still reading the encoders.
     */
    private static void configureDriveMotor(DcMotorEx motor, DcMotorSimple.Direction direction) {
        if (motor == null) {
            return;
        }
        motor.setDirection(direction);
        motor.setZeroPowerBehavior(DriveConstants.ZERO_POWER_BRAKE
                ? DcMotor.ZeroPowerBehavior.BRAKE : DcMotor.ZeroPowerBehavior.FLOAT);
        motor.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        motor.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        motor.setPower(0.0);
    }

    /** Intake and indexer setup (TESTED): FLOAT, open-loop power. */
    private static void configureOpenLoopMotor(DcMotorEx motor, DcMotorSimple.Direction direction) {
        if (motor == null) {
            return;
        }
        motor.setDirection(direction);
        motor.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.FLOAT);
        motor.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        motor.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        motor.setPower(0.0);
    }

    /**
     * Flywheel setup (TESTED): encoder velocity control, FLOAT so the flywheel coasts down
     * instead of braking hard (protects the shaft).
     */
    private static void configureLauncherMotor(DcMotorEx motor) {
        if (motor == null) {
            return;
        }
        motor.setDirection(LauncherConstants.DIRECTION);
        motor.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.FLOAT);
        motor.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        motor.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        motor.setVelocity(0.0);
    }

    private static String join(String[] names) {
        StringBuilder text = new StringBuilder();
        for (String n : names) {
            text.append(text.length() == 0 ? "" : ", ").append('"').append(n).append('"');
        }
        return text.toString();
    }
}
