// Sources: IronLion30311BIOBUZZInicioDeTemporada / branch feature/renata/flower-arm / TeamCode/.../subsystems/FlowerArm.java @ 889102d (POSITION_REST, POSITION_DEPLOYED)
//          IronLion30311BIOBUZZInicioDeTemporada / branch feature/renata/flower-arm / TeamCode/.../RobotHardware.java @ 889102d (servo directions)
//          — adapted for BIOBUZZ
package org.firstinspires.ftc.teamcode.config;

import com.acmerobotics.dashboard.config.Config;
import com.qualcomm.robotcore.hardware.Servo;

/**
 * Flower arm tuning values. The arm is two servos facing each other that swing together.
 * <p>
 * The arm was never tested on the robot (decision D14), so every value here is a guess.
 */
@Config
public final class FlowerArmConstants {

    private FlowerArmConstants() {
    }

    /** Servo position when the arm is folded in (0 degrees). TODO: TUNE */
    public static double REST_POSITION = 0.0;
    /** Servo position when the arm is out (90 degrees on a 180-degree servo). TODO: TUNE */
    public static double DEPLOYED_POSITION = 0.5;

    /** The servos face each other, so one is reversed to make them mirror. TODO: TUNE */
    public static final Servo.Direction LEFT_DIRECTION = Servo.Direction.FORWARD;
    public static final Servo.Direction RIGHT_DIRECTION = Servo.Direction.REVERSE;
}
