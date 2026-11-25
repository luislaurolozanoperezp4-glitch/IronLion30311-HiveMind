// Sources: IronLion30311BIOBUZZInicioDeTemporada / branch feature/turrubiates/vision-align / TeamCode/.../subsystems/MecanumDrive.java @ a7b8f4c (ramps, speeds)
//          IronLion30311BIOBUZZInicioDeTemporada / branch feature/turrubiates/vision-align / TeamCode/.../opmodes/auto/PedroAutoBase.java @ a7b8f4c (odometry driver, pose guards)
//          IronLion30311BIOBUZZInicioDeTemporada / branch feature/turrubiates/vision-align / TeamCode/.../opmodes/auto/SafeMoveAuto.java @ a7b8f4c (encoder ticks per cm)
//          IronLion30311BIOBUZZInicioDeTemporada / branch master / TeamCode/.../Constants.java @ 37b7a16 (motor ticks, wheel diameter)
//          — adapted for BIOBUZZ
package org.firstinspires.ftc.teamcode.config;

import com.acmerobotics.dashboard.config.Config;
import com.qualcomm.robotcore.hardware.DcMotorSimple;

/**
 * Drivetrain tuning values: motor directions, ramps, speeds, and the odometry driver that the
 * tested autonomous uses.
 * <p>
 * Values marked TESTED were used in a successful run on the real robot. Values marked
 * {@code TODO: TUNE} are best guesses that still need to be checked.
 * {@code public static} (not final) values can be changed live from FTC Dashboard.
 */
@Config
public final class DriveConstants {

    private DriveConstants() {
    }

    // ---------------- Motors (TESTED, same on every branch) ----------------
    public static final DcMotorSimple.Direction FRONT_LEFT_DIRECTION = DcMotorSimple.Direction.REVERSE;
    public static final DcMotorSimple.Direction BACK_LEFT_DIRECTION = DcMotorSimple.Direction.REVERSE;
    public static final DcMotorSimple.Direction FRONT_RIGHT_DIRECTION = DcMotorSimple.Direction.FORWARD;
    public static final DcMotorSimple.Direction BACK_RIGHT_DIRECTION = DcMotorSimple.Direction.FORWARD;

    /** BRAKE when the sticks are released. TESTED: FLOAT rolled ~12 cm past the target. */
    public static final boolean ZERO_POWER_BRAKE = true;

    /** goBILDA 312 RPM Yellow Jacket: 537.7 ticks per output revolution. Decision D3. */
    public static final double TICKS_PER_REV = 537.7;
    /** goBILDA 104 mm mecanum wheels. Decision D3. */
    public static final double WHEEL_DIAMETER_CM = 10.4;

    /** Measured wheel-encoder ticks per cm (for the encoder-only fallback). TODO: TUNE */
    public static double ENCODER_TICKS_PER_CM_FORWARD = 16.5;
    public static double ENCODER_TICKS_PER_CM_STRAFE = 18.5;

    // ---------------- TeleOp feel (TESTED) ----------------
    public static double ACCEL_RAMP_SECONDS = 0.3;
    public static double DECEL_RAMP_SECONDS = 0.2;
    /** Below this power the ramp snaps straight to zero, so the robot does not creep. */
    public static double STOP_CUTOFF_POWER = 0.25;
    public static double ALIGN_RAMP_SECONDS = 0.1;
    public static double SLOW_SPEED = 0.4;
    public static double FAST_SPEED = 1.0;
    public static double STICK_DEADZONE = 0.05;

    // ---------------- Odometry driver for AUTO (TESTED in the 4/4 preload run) ----------------
    public static double AUTO_MAX_POWER = 0.85;
    public static double AUTO_MIN_POWER = 0.25;
    public static double AUTO_SLOW_ZONE_INCHES = 18.0;
    public static double AUTO_TOLERANCE_INCHES = 0.6;
    public static double AUTO_ACCEL_PER_SECOND = 3.0;
    public static double AUTO_HEADING_KP = 1.0;
    public static double AUTO_MAX_TURN = 0.45;
    public static double AUTO_CORNER_POWER = 0.45;
    public static double AUTO_PASS_THROUGH_INCHES = 4.0;
    public static double TURN_KP = 1.2;
    public static double TURN_MAX_POWER = 0.5;
    public static double TURN_MIN_POWER = 0.15;
    public static double TURN_TOLERANCE_DEGREES = 2.0;
    public static double TURN_TIMEOUT_SECONDS = 2.5;
    public static double HOLD_KP = 0.12;
    public static double HOLD_MAX_POWER = 0.25;
    public static double HOLD_DEADBAND_INCHES = 0.3;
    /** Drive timeout = base + distance / rate. */
    public static double DRIVE_TIMEOUT_BASE_SECONDS = 1.5;
    public static double DRIVE_TIMEOUT_INCHES_PER_SECOND = 20.0;

    // ---------------- Odometry safety guards (TESTED) ----------------
    /** A pose jump bigger than this in one loop is treated as an odometry glitch. */
    public static double POSE_JUMP_LIMIT_INCHES = 12.0;
    public static double POSE_JUMP_LIMIT_DEGREES = 30.0;
    /** How far the robot may be from the expected start pose before AUTO warns. */
    public static double START_POSE_TOLERANCE_INCHES = 2.0;
    public static double START_HEADING_TOLERANCE_DEGREES = 5.0;
    public static double START_REZERO_MAX_INCHES = 12.0;
    public static double START_REZERO_MAX_DEGREES = 10.0;
}
