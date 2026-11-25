// Sources: IronLion30311BIOBUZZInicioDeTemporada / branch feature/turrubiates/vision-align / TeamCode/.../subsystems/Intake.java @ a7b8f4c (power, ramp)
//          IronLion30311BIOBUZZInicioDeTemporada / branch feature/turrubiates/vision-align / TeamCode/.../RobotHardware.java @ a7b8f4c (direction, commit 927e4df)
//          IronLion30311BIOBUZZInicioDeTemporada / branch feature/turrubiates/vision-align / TeamCode/.../opmodes/auto/PedroAutoBase.java @ a7b8f4c (shake, commit 7004eba)
//          IronLion30311BIOBUZZInicioDeTemporada / branch feature/turrubiates/vision-align / TeamCode/.../subsystems/Intake.java @ b6913d9 (jam protection: JAM_CURRENT_AMPS, JAM_TIME_SEC, UNJAM_TIME_SEC)
//          — adapted for BIOBUZZ
package org.firstinspires.ftc.teamcode.config;

import com.acmerobotics.dashboard.config.Config;
import com.qualcomm.robotcore.hardware.DcMotorSimple;

/** Intake tuning values. The intake is at the front of the robot and pulls balls in. */
@Config
public final class IntakeConstants {

    private IntakeConstants() {
    }

    /** TESTED 2026-11-23: with FORWARD the intake pushed balls out. */
    public static final DcMotorSimple.Direction DIRECTION = DcMotorSimple.Direction.REVERSE;

    /** Collect power. 0.75 on the tested branch; some branches use 1.0 (decision D8). */
    public static double COLLECT_POWER = 0.75;
    public static double EJECT_POWER = 0.75;
    /** Time to ramp from 0 to full power, so the motor does not spike the battery. */
    public static double RAMP_SECONDS = 0.15;

    /** Anti-jam shake: briefly reverse, wait, and try again (FITTED from logs). */
    public static double SHAKE_REVERSE_SECONDS = 0.35;
    public static double SHAKE_WAIT_SECONDS = 0.8;
    public static int SHAKE_MAX_TRIES = 3;

    /**
     * Jam protection (vision-align b6913d9): while collecting at speed, a motor current above
     * this for {@link #JAM_SECONDS} means a ball is stuck. FITTED: in a 2026-11-24 TeleOp the
     * intake motor stalled at 8.2 A (stall ~9.2 A) with two stall warnings. TODO: TUNE
     */
    public static double JAM_CURRENT_AMPS = 6.0;
    /** How long the current must stay above {@link #JAM_CURRENT_AMPS}. TODO: TUNE */
    public static double JAM_SECONDS = 0.4;
    /** Reverse time of one unjam (the ramp needs ~0.23 s to reverse fully). TODO: TUNE */
    public static double JAM_REVERSE_SECONDS = 0.4;
    /**
     * The current is only checked once the power is at least this fraction of
     * {@link #COLLECT_POWER} (the start-up current spike is not a jam).
     */
    public static double JAM_MIN_POWER_FRACTION = 0.9;
    /** Set false to turn the jam protection off (FTC Dashboard). */
    public static boolean JAM_PROTECTION_ENABLED = true;

    /** Largest loop time used by the ramp, so the first loop after INIT does not jump. */
    public static final double RAMP_MAX_DT_SECONDS = 0.1;

    /**
     * G408 helper (team decision 2026-11-25): when the HuskyLens sees the opponent's NECTAR in
     * front of the intake while collecting, the intake stops (never pushes it out) and stays
     * stopped until the ball has been out of view this long. TODO: TUNE
     */
    public static double OPPONENT_NECTAR_HOLD_SECONDS = 0.3;
    /** Set false to turn the opponent-NECTAR stop off. TODO: TUNE (needs field test) */
    public static boolean STOP_FOR_OPPONENT_NECTAR = true;

    // ---------------- AUTO collect (CollectSequence), TESTED partially in PreloadCollectAuto ----------------
    /** Forward power while driving onto a ball the HuskyLens sees. */
    public static double COLLECT_APPROACH_POWER = 0.3;
    /** Turn power per unit of the ball's horizontal offset (-1..1). */
    public static double COLLECT_TURN_KP = 0.5;
    /** Largest turn power used to center a ball. */
    public static double COLLECT_MAX_TURN = 0.35;
    /** After the ball leaves the view (it went under the camera), keep driving this long. */
    public static double COLLECT_BALL_LOST_DRIVE_SECONDS = 0.4;
}
