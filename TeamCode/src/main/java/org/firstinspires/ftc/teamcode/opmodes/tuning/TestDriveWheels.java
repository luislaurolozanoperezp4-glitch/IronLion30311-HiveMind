// Sources: IronLion30311BIOBUZZInicioDeTemporada / branch feature/turrubiates/vision-align / TeamCode/.../opmodes/teleop/DriveWheelTest.java @ a7b8f4c (one wheel per bumper/trigger, laid out like the robot, TEST_POWER)
//          — adapted for BIOBUZZ (iterative OpMode, missing-motor safe, all-wheels check, encoder sign hint)
package org.firstinspires.ftc.teamcode.opmodes.tuning;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotorEx;

import org.firstinspires.ftc.teamcode.config.RobotConstants;
import org.firstinspires.ftc.teamcode.config.TuningConstants;
import org.firstinspires.ftc.teamcode.hardware.RobotHardware;
import org.firstinspires.ftc.teamcode.util.GamepadEx;
import org.firstinspires.ftc.teamcode.util.GamepadEx.Button;

/**
 * Spins one drive wheel at a time to check the Driver Station configuration (port, motor type
 * and direction of each wheel).
 * <p>
 * gamepad1, laid out like the wheels on the robot:
 * <ul>
 *   <li>LB (hold): front left. RB (hold): front right.</li>
 *   <li>LT (hold): back left. RT (hold): back right.</li>
 *   <li>A (hold): all four together (the robot must drive straight FORWARD, toward the intake).</li>
 * </ul>
 * Each wheel must spin the way that drives the robot FORWARD. If the wrong wheel moves, its port
 * or name is wrong. If it spins backward, its direction in {@code DriveConstants} (or the motor
 * type) is wrong. The encoder column says whether the encoder counts up while the wheel drives
 * forward; Pedro uses the Pinpoint, so a reversed drive encoder only matters for
 * {@code Drivetrain.getAverageForwardCm()}.
 * <p>
 * Lift the robot or keep it on the floor with room around it: power is
 * {@link TuningConstants#WHEEL_TEST_POWER}.
 */
@TeleOp(name = "Test Drive Wheels", group = "Tuning")
public class TestDriveWheels extends OpMode {

    private RobotHardware hardware;
    private GamepadEx pad;

    /** Finds the drive motors. */
    @Override
    public void init() {
        hardware = new RobotHardware(hardwareMap);
        pad = new GamepadEx(gamepad1);
        hardware.addTelemetry(telemetry);
        telemetry.addLine("LB = front left, RB = front right");
        telemetry.addLine("LT = back left,  RT = back right, A = all four");
        telemetry.addLine("Each wheel must spin to drive FORWARD (toward the intake)");
    }

    /** Spins the wheels whose buttons are held. */
    @Override
    public void loop() {
        hardware.clearBulkCache();
        pad.readButtons();
        boolean isAll = pad.isPressed(Button.A);
        testWheel(RobotConstants.FRONT_LEFT_DRIVE, hardware.frontLeftDrive,
                isAll || pad.isPressed(Button.LEFT_BUMPER));
        testWheel(RobotConstants.FRONT_RIGHT_DRIVE, hardware.frontRightDrive,
                isAll || pad.isPressed(Button.RIGHT_BUMPER));
        testWheel(RobotConstants.BACK_LEFT_DRIVE, hardware.backLeftDrive,
                isAll || pad.isPressed(Button.LEFT_TRIGGER));
        testWheel(RobotConstants.BACK_RIGHT_DRIVE, hardware.backRightDrive,
                isAll || pad.isPressed(Button.RIGHT_TRIGGER));
        telemetry.addLine("LB FL | RB FR | LT BL | RT BR | A all");
    }

    /** Spins the wheel forward while pressed and shows its power, speed and encoder sign. */
    private void testWheel(String name, DcMotorEx motor, boolean isPressed) {
        if (motor == null) {
            telemetry.addData(name, "MISSING - check the configuration name and port");
            return;
        }
        double power = isPressed ? TuningConstants.WHEEL_TEST_POWER : 0.0;
        motor.setPower(power);
        double velocity = motor.getVelocity();
        String encoder = "-";
        if (isPressed && Math.abs(velocity) > TuningConstants.WHEEL_MOVING_TICKS_PER_SECOND) {
            encoder = velocity > 0.0 ? "counts up (OK)" : "counts DOWN (encoder reversed)";
        }
        telemetry.addData(name, "power %.2f | %.0f ticks/s | pos %d | encoder %s", power,
                velocity, motor.getCurrentPosition(), encoder);
    }

    /** Stops all wheels. */
    @Override
    public void stop() {
        if (hardware == null) {
            return;
        }
        DcMotorEx[] motors = {hardware.frontLeftDrive, hardware.frontRightDrive,
                hardware.backLeftDrive, hardware.backRightDrive};
        for (DcMotorEx motor : motors) {
            if (motor != null) {
                motor.setPower(0.0);
            }
        }
    }
}
