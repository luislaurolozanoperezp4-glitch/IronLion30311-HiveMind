// Sources: IronLion30311BIOBUZZInicioDeTemporada / branch feature/turrubiates/vision-align / TeamCode/.../subsystems/Shooter.java @ a7b8f4c (RPM targets, PIDF, voltage compensation) @ 854de85 (POLLEN_IDLE_RPM, POLLEN_IDLE_AT_START) @ 19de992 (READY_BELOW_RPM / READY_ABOVE_RPM feed window)
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
    /**
     * Speed of a flywheel that is not firing, when no idle spin is on. 0 = off (saves battery).
     * In TeleOp the POLLEN idle spin below replaces it for the POLLEN flywheel.
     */
    public static double IDLE_RPM = 0.0;
    /**
     * POLLEN idle spin between volleys in TeleOp (vision-align 854de85). FITTED from the
     * 2026-11-24 TeleOp logs: the POLLEN flywheel was ready 0.7 s after a fire command from
     * ~2000 RPM, versus 1.4-3.0 s from a stop. Steady spinning only costs friction and air
     * (~1-1.5 A). Idle keeps the IDLE state: never "ready", no shots counted. TODO: TUNE
     * (not yet run in a match; measure the idle current).
     */
    public static double POLLEN_IDLE_RPM = 2000.0;
    /** Whether TeleOp starts with the POLLEN idle spin on (gamepad2 right stick button toggles it). */
    public static boolean POLLEN_IDLE_AT_START = true;

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

    // ---------------- Ready check (feed window) ----------------
    /**
     * A firing flywheel becomes "ready" (the indexer may feed the next ball) only once it is at
     * most this many RPM BELOW its target... FITTED (vision-align 19de992): with a symmetric
     * 100 RPM window, 6 of 72 logged shots left 57-79 RPM low (two short shots at the other
     * cell). Costs ~0.1 s per shot (median, 0.3 s at p90). Readings come in ~43 RPM steps (28
     * ticks per revolution), so do not go below ~45. TODO: TUNE (not yet run on the robot)
     */
    public static double READY_BELOW_RPM = 50.0;
    /**
     * ...and at most this many RPM ABOVE it: an overshoot after the recovery also throws long
     * (one logged shot left +71 RPM high). FITTED (vision-align 19de992). TODO: TUNE
     */
    public static double READY_ABOVE_RPM = 80.0;
    /**
     * A ready flywheel stops being ready only when it is more than this many RPM off its target
     * (a shot dip). Wider than the feed window, so readiness does not flicker at its edge.
     * TESTED (vision-align a7b8f4c).
     */
    public static double NOT_READY_TOLERANCE_RPM = 200.0;
    public static double SPIN_UP_TIMEOUT_SECONDS = 3.0; // TODO: TUNE

    // ---------------- Preload (TESTED) ----------------
    public static int PRELOAD_BALLS = 4;
    public static double SHOOT_MAX_SECONDS = 10.0;

    // ---------------- Shot detection (TESTED, ShooterLogger on vision-align) ----------------
    /** A shot starts when a firing flywheel falls this many RPM below its target. */
    public static double SHOT_DIP_RPM = 200.0;
    /** A shot ends (flywheel recovered) once it is back within this many RPM of the target. */
    public static double SHOT_RECOVERED_RPM = 100.0;

    // ---------------- Shooting sequence (TESTED in the 4/4 preload run) ----------------
    /** After each shot, feeding stays stopped at least this long and until the RPM is back. */
    public static double SHOT_PAUSE_MIN_SECONDS = 0.15;
    /** Once a ball left, the volley ends if no other shot comes this long after the last one. */
    public static double SHOOT_NO_SHOT_END_SECONDS = 3.0;
}
