// Sources: IronLion30311BIOBUZZInicioDeTemporada / branch feature/turrubiates/vision-align / TeamCode/.../subsystems/FlowerSweeper.java @ 344254b (LEFT_UP, RIGHT_UP, LEFT_DOWN, RIGHT_DOWN, MOVE_TIME_SEC, DEGREES_PER_UNIT)
//          IronLion30311BIOBUZZInicioDeTemporada / branch feature/turrubiates/vision-align / TeamCode/.../RobotHardware.java @ 344254b (goBILDA Dual Mode PWM range 500-2500 us)
//          — adapted for BIOBUZZ
package org.firstinspires.ftc.teamcode.config;

import com.acmerobotics.dashboard.config.Config;

/**
 * FLOWER sweeper tuning values. The sweeper is a bar that sweeps POLLEN off the FLOWERS,
 * lowered to the floor and raised by two goBILDA 2000-0025-0002 servos (Dual Mode, Torque,
 * 300 degrees) mounted as mirrors.
 * <p>
 * Each servo has its own UP and DOWN position (0..1 = 500-2500 us = 0-300 degrees), because
 * mirrored servos turn opposite ways and their splines are never exactly aligned. Find them
 * with the {@code Tune Flower Sweeper} OpMode (group Tuning) and copy them here.
 */
@Config
public final class FlowerSweeperConstants {

    private FlowerSweeperConstants() {
    }

    /** Servo positions with the bar up (stowed). TODO: TUNE (calibrate with Tune Flower Sweeper) */
    public static double LEFT_UP_POSITION = 0.50;
    public static double RIGHT_UP_POSITION = 0.50;

    /**
     * Servo positions with the bar down on the floor. Calibrate at the first contact with the
     * floor plus 1-2 degrees, no more: pushing into the floor stalls the servo and heats it.
     * TODO: TUNE (calibrate with Tune Flower Sweeper)
     */
    public static double LEFT_DOWN_POSITION = 0.50;
    public static double RIGHT_DOWN_POSITION = 0.50;

    /**
     * Time for the whole UP-to-DOWN travel (the servo alone is much faster), so the bar does not
     * slam into the floor. Vision-align value. TODO: TUNE
     */
    public static double MOVE_SECONDS = 0.5;

    /** Longest loop time used by the slowed-down move, so one slow loop does not jump. */
    public static final double MOVE_MAX_DT_SECONDS = 0.1;

    /** Servo travel for the full 0..1 range, in degrees (goBILDA Dual Mode, position mode). */
    public static final double DEGREES_PER_UNIT = 300.0;

    /**
     * goBILDA Dual Mode servo pulse range: 500-2500 us = the full 300 degrees. The hub's default
     * (600-2400 us) would only give ~270 degrees. Set by {@code RobotHardware}.
     */
    public static final double PWM_MIN_MICROSECONDS = 500.0;
    public static final double PWM_MAX_MICROSECONDS = 2500.0;

    /**
     * Conservative rule gate (team policy, see docs/RULES_COMPLIANCE.md): while true, the bar can
     * only be LOWERED in the last 60 s of TELEOP (the same unlock as G410 for NECTAR), because
     * FLOWER scoring "cannot begin" before 1:00 (section 10.5.2). Raising is always allowed.
     * Set false once the Q&A confirms that removing POLLEN from a FLOWER (G418, from the
     * bottom) is fine earlier. TODO: TUNE (team decision)
     */
    public static boolean LOCK_DOWN_UNTIL_NECTAR_UNLOCK = true;
}
