// Sources: IronLion30311BIOBUZZInicioDeTemporada / branch feature/turrubiates/vision-align / TeamCode/.../subsystems/Shooter.java @ a7b8f4c (RPM targets, PIDF, voltage compensation)
//          IronLion30311BIOBUZZInicioDeTemporada / branch feature/turrubiates/vision-align / TeamCode/.../opmodes/auto/PreloadRouteAuto.java @ a7b8f4c (auto RPM, preload)
//          IronLion30311BIOBUZZInicioDeTemporada / branch master / TeamCode/.../Constants.java @ 37b7a16 (spin-up timeout)
//          — adapted for BIOBUZZ ("Shooter" renamed to "Launcher")
package org.firstinspires.ftc.teamcode.config;

import com.acmerobotics.dashboard.config.Config;
import com.qualcomm.robotcore.hardware.DcMotorSimple;

/**
 * Launcher tuning values. The robot has two flywheels at the rear: one for POLLEN and one for
 * NECTAR. Each runs velocity control (PIDF) with a feedforward that is rescaled with the battery
 * voltage, so shots land the same on a full or a tired battery.
 */
@Config
public final class LauncherConstants {

    private LauncherConstants() {
    }

    /** TESTED: both flywheels run REVERSE, using the encoder, and coast (FLOAT) when stopped. */
    public static final DcMotorSimple.Direction DIRECTION = DcMotorSimple.Direction.REVERSE;

    /** Encoder ticks per flywheel revolution (bare goBILDA/REV motor). Decision D7. */
    public static final double TICKS_PER_REV = 28.0;
    public static final double MAX_RPM = 6000.0;
    public static final double MIN_RPM = 0.0;
    /** How much one button press changes the target RPM in TeleOp. */
    public static double RPM_STEP = 50.0;

    // ---------------- Targets ----------------
    /** TESTED: 2900 scored 11 of 25, 3000 scored 29. */
    public static double POLLEN_TELEOP_RPM = 3000.0;
    /** TESTED 2026-11-23: at 3000 both AUTO shots went long from the near spot. */
    public static double POLLEN_AUTO_RPM = 2950.0;
    /** FITTED: 2800 left 2 of 3 short at the other cell. TODO: TUNE */
    public static double POLLEN_OTHER_CELL_RPM = 2950.0;
    /** TESTED: 4500 was unreachable. NECTAR is not scored at the first event. */
    public static double NECTAR_RPM = 3600.0;
    /** Flywheel speed between shots. 0 = off (saves battery). Alternative 1800. TODO: TUNE */
    public static double IDLE_RPM = 0.0;

    // ---------------- Velocity PIDF (TESTED from logs) ----------------
    public static double KP = 30.0;
    /** Zero on purpose: the I term wound up on every shot dip. */
    public static double KI = 0.0;
    public static double KD = 0.0;
    /** Feedforward at 12 V. Rescaled by 12 / batteryVolts. */
    public static double KF_AT_12V = 15.0;
    public static double KF_UPDATE_SECONDS = 0.25;
    public static double KF_MIN_CHANGE = 0.02;
    /** Below this voltage the reading is treated as noise and ignored. */
    public static double KF_MIN_VALID_VOLTS = 7.0;

    // ---------------- Ready check (TESTED) ----------------
    /** The launcher becomes "ready" within this many RPM of the target... */
    public static double READY_TOLERANCE_RPM = 100.0;
    /** ...and only stops being ready if it falls further than this (hysteresis). */
    public static double NOT_READY_TOLERANCE_RPM = 200.0;
    public static double SPIN_UP_TIMEOUT_SECONDS = 3.0; // TODO: TUNE

    // ---------------- Preload (TESTED) ----------------
    public static int PRELOAD_BALLS = 4;
    public static double SHOOT_MAX_SECONDS = 10.0;
}
