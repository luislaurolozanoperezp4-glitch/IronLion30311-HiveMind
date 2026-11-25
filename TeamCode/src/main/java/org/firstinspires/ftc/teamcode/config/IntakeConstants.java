// Sources: IronLion30311BIOBUZZInicioDeTemporada / branch feature/turrubiates/vision-align / TeamCode/.../subsystems/Intake.java @ a7b8f4c (power, ramp)
//          IronLion30311BIOBUZZInicioDeTemporada / branch feature/turrubiates/vision-align / TeamCode/.../RobotHardware.java @ a7b8f4c (direction, commit 927e4df)
//          IronLion30311BIOBUZZInicioDeTemporada / branch feature/turrubiates/vision-align / TeamCode/.../opmodes/auto/PedroAutoBase.java @ a7b8f4c (shake, commit 7004eba)
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
}
