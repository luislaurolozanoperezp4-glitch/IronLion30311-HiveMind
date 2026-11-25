// Sources: IronLion30311BIOBUZZInicioDeTemporada / branch feature/turrubiates/vision-align / TeamCode/.../RobotHardware.java @ a7b8f4c (device names, legacy names)
//          IronLion30311BIOBUZZInicioDeTemporada / branch feature/turrubiates/vision-align / TeamCode/.../subsystems/BatteryMonitor.java @ a7b8f4c (battery limits)
//          IronLion30311BIOBUZZInicioDeTemporada / branch feature/turrubiates/vision-align / TeamCode/.../subsystems/IndicatorLight.java @ a7b8f4c (light colors)
//          IronLion30311BIOBUZZInicioDeTemporada / branch master / TeamCode/.../RobotHardware.java @ 37b7a16 ("pinpoint", legacy "indexer", green light)
//          IronLion30311BIOBUZZInicioDeTemporada / branch feature/turrubiates/vision-align / TeamCode/.../RobotHardware.java @ 344254b (sweeperLeftServo, sweeperRightServo)
//          — adapted for BIOBUZZ
package org.firstinspires.ftc.teamcode.config;

import com.acmerobotics.dashboard.config.Config;

/**
 * The single source of truth for the robot's hardware.
 * <p>
 * Every Driver Station configuration name lives here, so a name is typed exactly once in the
 * whole codebase. If you rename a device on the Driver Station, change it here and nowhere else.
 * The README has the same names in a table.
 * <p>
 * Also holds robot dimensions, battery limits, and indicator light colors.
 * See {@code docs/ROBOT_PROFILE.md} for where each value came from.
 */
@Config
public final class RobotConstants {

    private RobotConstants() {
    }

    // ------------------------------------------------------------------------------------------
    // Driver Station configuration names (must match the Control Hub configuration exactly)
    // ------------------------------------------------------------------------------------------

    /** Drive motors. TESTED: Pedro and TeleOp found these names in the 4/4 preload run. */
    public static final String FRONT_LEFT_DRIVE = "frontLeftDrive";
    public static final String FRONT_RIGHT_DRIVE = "frontRightDrive";
    public static final String BACK_LEFT_DRIVE = "backLeftDrive";
    public static final String BACK_RIGHT_DRIVE = "backRightDrive";

    /** Intake motor (front of the robot). */
    public static final String INTAKE_MOTOR = "intakeMotor";

    /** Indexer motor that feeds balls from storage into the launchers. Old name: "alejandro". */
    public static final String INDEXER_MOTOR = "indexerMotor";

    /**
     * Launcher flywheel motors. The Driver Station still uses the "Shooter" names (decision D15),
     * while the code calls the mechanism "Launcher".
     */
    public static final String POLLEN_LAUNCHER_MOTOR = "pollenShooterMotor";
    public static final String NECTAR_LAUNCHER_MOTOR = "nectarShooterMotor";

    /** goBILDA PWM RGB indicator light (driven as a servo). */
    public static final String INDICATOR_LIGHT = "indicatorLight";

    /** Optional second light that blinks when NECTAR is in front of the intake (decision D12). */
    public static final String NECTAR_ALERT_LIGHT = "nectarAlertLight";

    /** Limelight 3A on the rear (launcher side), reads HIVE AprilTags. */
    public static final String LIMELIGHT = "limelight";

    /** HuskyLens on the front (intake side), sees POLLEN and NECTAR by color. */
    public static final String HUSKY_LENS = "huskyLens";

    /** goBILDA Pinpoint odometry computer (two 4-bar pods + IMU). Owned by Pedro Pathing. */
    public static final String PINPOINT = "pinpoint";

    /**
     * FLOWER sweeper servos: two mirrored goBILDA 2000-0025-0002 (Dual Mode, Torque, 300 deg)
     * that lower and raise the bar that sweeps POLLEN off the FLOWERS. CONFIRMED on the robot
     * branch (vision-align 344254b); optional: if they are missing, FlowerSweeper does nothing.
     */
    public static final String SWEEPER_LEFT_SERVO = "sweeperLeftServo";
    public static final String SWEEPER_RIGHT_SERVO = "sweeperRightServo";

    // ------------------------------------------------------------------------------------------
    // Legacy names: accepted as a fallback while a Control Hub still has the old configuration.
    // Delete each one once every hub is renamed (decision D2, docs/BEST_OF_MATRIX.md section 6).
    // ------------------------------------------------------------------------------------------

    public static final String[] FRONT_LEFT_DRIVE_LEGACY = {"FL"};
    public static final String[] FRONT_RIGHT_DRIVE_LEGACY = {"FR"};
    public static final String[] BACK_LEFT_DRIVE_LEGACY = {"BL"};
    public static final String[] BACK_RIGHT_DRIVE_LEGACY = {"BR"};
    public static final String[] INTAKE_MOTOR_LEGACY = {"intake"};
    public static final String[] INDEXER_MOTOR_LEGACY = {"alejandro", "indexer"};
    public static final String[] POLLEN_LAUNCHER_MOTOR_LEGACY = {"P"};
    public static final String[] NECTAR_LAUNCHER_MOTOR_LEGACY = {"N"};
    public static final String[] INDICATOR_LIGHT_LEGACY = {"indicator"};
    /**
     * Names the two FLOWER servos had on older configurations: the removed flower arm
     * ({@code flowerArmLeftServo}, feature/renata/flower-arm {@code servoLeft}). Accepted so a hub
     * that still uses them keeps working; rename them to the sweeper names.
     */
    public static final String[] SWEEPER_LEFT_SERVO_LEGACY = {"flowerArmLeftServo", "servoLeft"};
    public static final String[] SWEEPER_RIGHT_SERVO_LEGACY = {"flowerArmRightServo", "servoRight"};

    // ------------------------------------------------------------------------------------------
    // Robot dimensions
    // ------------------------------------------------------------------------------------------

    /** Robot length, front to back, in inches. TODO: TUNE (decision D16: measure the robot). */
    public static final double ROBOT_LENGTH_INCHES = 18.0;

    /** Robot width, side to side, in inches. TODO: TUNE (decision D16: measure the robot). */
    public static final double ROBOT_WIDTH_INCHES = 18.0;

    // ------------------------------------------------------------------------------------------
    // Battery
    // ------------------------------------------------------------------------------------------

    /** Below this voltage for {@link #BATTERY_LOW_SECONDS} the driver gets a warning. */
    public static double BATTERY_LOW_VOLTS = 11.0;
    public static double BATTERY_LOW_SECONDS = 3.0;
    /** The warning clears once the battery recovers above this voltage. */
    public static double BATTERY_LOW_CLEAR_VOLTS = 11.3;
    /** A sudden sag below this voltage limits drive power so the hub does not brown out. */
    public static double BATTERY_SAG_VOLTS = 8.5;
    public static double BATTERY_SAG_DRIVE_LIMIT = 0.5;
    public static double BATTERY_SAG_HOLD_SECONDS = 0.5;
    /** Nominal voltage that feedforward gains are tuned at. */
    public static final double NOMINAL_VOLTS = 12.0;
    /** Time between battery readings; each reading is an extra hub transaction. */
    public static double BATTERY_READ_INTERVAL_SECONDS = 0.05;
    /** Readings below this are sensor glitches (the hub reads 0 V when the OpMode stops). */
    public static final double BATTERY_MIN_VALID_VOLTS = 3.0;

    // ------------------------------------------------------------------------------------------
    // Indicator light (goBILDA RGB light: servo position = color)
    // ------------------------------------------------------------------------------------------

    public static double LIGHT_OFF = 0.0;
    public static double LIGHT_RED = 0.33;
    /** TODO: TUNE (yellow was never checked on the real light). */
    public static double LIGHT_YELLOW = 0.42;
    /** From branch master, not tested on the current robot. TODO: TUNE */
    public static double LIGHT_GREEN = 0.50;
    public static double LIGHT_BLUE = 0.63;
    public static double LIGHT_WHITE = 1.0;
    public static double LIGHT_BLINK_PERIOD_SECONDS = 0.4;
    public static double NECTAR_ALERT_HOLD_SECONDS = 0.6;
    public static double NECTAR_ALERT_BLINK_SECONDS = 0.25;
}
