// Sources: IronLion30311BIOBUZZInicioDeTemporada / branch feature/turrubiates/vision-align / TeamCode/.../opmodes/calibration/SweeperServoCalibration.java @ 344254b (selection LEFT/RIGHT/BOTH, coarse/fine steps, save UP/DOWN, go UP/DOWN, describe)
//          — adapted for BIOBUZZ (iterative OpMode, GamepadEx edges, steps in TuningConstants, positions in FlowerSweeperConstants)
package org.firstinspires.ftc.teamcode.opmodes.tuning;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.Servo;

import org.firstinspires.ftc.teamcode.config.FlowerSweeperConstants;
import org.firstinspires.ftc.teamcode.config.TuningConstants;
import org.firstinspires.ftc.teamcode.hardware.RobotHardware;
import org.firstinspires.ftc.teamcode.subsystems.FlowerSweeper;
import org.firstinspires.ftc.teamcode.util.GamepadEx;
import org.firstinspires.ftc.teamcode.util.GamepadEx.Button;

import java.util.Locale;

/**
 * Finds the UP and DOWN positions of the two mirrored FLOWER sweeper servos
 * ({@code sweeperLeftServo}, {@code sweeperRightServo}). Nothing moves until a button is pressed;
 * start from the stored values (0.5 the first time) and move in small steps.
 * <p>
 * gamepad1:
 * <ul>
 *   <li>Left / right bumper: select LEFT, RIGHT or BOTH (BOTH moves them as mirrors: the right
 *   one the opposite way).</li>
 *   <li>D-pad up / down: +/- {@link TuningConstants#SWEEPER_COARSE_STEP} (~6 deg). D-pad right /
 *   left: +/- {@link TuningConstants#SWEEPER_FINE_STEP} (~1.5 deg). The first press starts from
 *   the stored UP positions.</li>
 *   <li>X: store the current positions as UP. Y: store them as DOWN (for this session and FTC
 *   Dashboard; copy the numbers into {@link FlowerSweeperConstants}).</li>
 *   <li>A: go to the stored UP. B: go to the stored DOWN (slowed down, as in a match).</li>
 * </ul>
 * Stop moving toward the floor at the first contact: pushing into it stalls the servo. The
 * lowering gate of TeleOp is not used here.
 */
@TeleOp(name = "Tune Flower Sweeper", group = "Tuning")
public class TuneFlowerSweeper extends OpMode {

    private enum Selection { LEFT, RIGHT, BOTH }

    /** Pulse width at position 0 and per unit of position (goBILDA range), for the readout. */
    private static final double PULSE_SPAN_MICROSECONDS = FlowerSweeperConstants.PWM_MAX_MICROSECONDS
            - FlowerSweeperConstants.PWM_MIN_MICROSECONDS;

    private RobotHardware hardware;
    private FlowerSweeper sweeper;
    private GamepadEx pad;
    private Servo left;
    private Servo right;
    private double leftPosition;
    private double rightPosition;
    private Selection selection = Selection.BOTH;
    private boolean isManual = false;

    /** Finds the servos; nothing moves yet. */
    @Override
    public void init() {
        hardware = new RobotHardware(hardwareMap);
        sweeper = new FlowerSweeper(hardware);
        pad = new GamepadEx(gamepad1);
        left = hardware.sweeperLeftServo;
        right = hardware.sweeperRightServo;
        leftPosition = FlowerSweeperConstants.LEFT_UP_POSITION;
        rightPosition = FlowerSweeperConstants.RIGHT_UP_POSITION;
        telemetry.addLine("Tune Flower Sweeper: nothing moves until a button is pressed");
        telemetry.addData("Servos", "left %s | right %s", left == null ? "MISSING" : "OK",
                right == null ? "MISSING" : "OK");
    }

    /** Reads the buttons, moves the servos and shows the values to copy. */
    @Override
    public void loop() {
        hardware.clearBulkCache();
        pad.readButtons();
        if (pad.wasJustPressed(Button.RIGHT_BUMPER)) {
            selection = Selection.values()[(selection.ordinal() + 1) % Selection.values().length];
        } else if (pad.wasJustPressed(Button.LEFT_BUMPER)) {
            selection = Selection.values()[(selection.ordinal() + Selection.values().length - 1)
                    % Selection.values().length];
        }

        double step = 0.0;
        if (pad.wasJustPressed(Button.DPAD_UP)) {
            step = TuningConstants.SWEEPER_COARSE_STEP;
        } else if (pad.wasJustPressed(Button.DPAD_DOWN)) {
            step = -TuningConstants.SWEEPER_COARSE_STEP;
        } else if (pad.wasJustPressed(Button.DPAD_RIGHT)) {
            step = TuningConstants.SWEEPER_FINE_STEP;
        } else if (pad.wasJustPressed(Button.DPAD_LEFT)) {
            step = -TuningConstants.SWEEPER_FINE_STEP;
        }
        if (step != 0.0) {
            isManual = true;
            if (selection != Selection.RIGHT) {
                leftPosition = clip(leftPosition + step);
            }
            if (selection != Selection.LEFT) {
                // Mirrors: in BOTH the right servo turns the other way
                rightPosition = clip(rightPosition + (selection == Selection.BOTH ? -step : step));
            }
        }

        if (pad.wasJustPressed(Button.X)) {
            FlowerSweeperConstants.LEFT_UP_POSITION = leftPosition;
            FlowerSweeperConstants.RIGHT_UP_POSITION = rightPosition;
        } else if (pad.wasJustPressed(Button.Y)) {
            FlowerSweeperConstants.LEFT_DOWN_POSITION = leftPosition;
            FlowerSweeperConstants.RIGHT_DOWN_POSITION = rightPosition;
        }
        if (pad.wasJustPressed(Button.A)) {
            isManual = false;
            sweeper.up();
        } else if (pad.wasJustPressed(Button.B)) {
            isManual = false;
            sweeper.down();
        }

        if (isManual) {
            if (left != null) {
                left.setPosition(leftPosition);
            }
            if (right != null) {
                right.setPosition(rightPosition);
            }
        } else {
            sweeper.periodic();
            leftPosition = sweeper.getLeftPosition();
            rightPosition = sweeper.getRightPosition();
        }

        telemetry.addData("Selected", "%s (bumpers change it)", selection);
        telemetry.addData("Left", describe(leftPosition));
        telemetry.addData("Right", describe(rightPosition));
        telemetry.addData("Mode", isManual ? "MANUAL (D-pad)" : "SWEEPER " + sweeper.getState());
        telemetry.addLine("D-pad up/down coarse | right/left fine | X = save UP | Y = save DOWN"
                + " | A = go UP | B = go DOWN");
        telemetry.addLine(String.format(Locale.US, "Copy into FlowerSweeperConstants: LEFT_UP %.3f,"
                        + " RIGHT_UP %.3f, LEFT_DOWN %.3f, RIGHT_DOWN %.3f",
                FlowerSweeperConstants.LEFT_UP_POSITION, FlowerSweeperConstants.RIGHT_UP_POSITION,
                FlowerSweeperConstants.LEFT_DOWN_POSITION, FlowerSweeperConstants.RIGHT_DOWN_POSITION));
    }

    /** @return "0.500 = 150.0 deg = 1500 us" */
    private static String describe(double position) {
        return String.format(Locale.US, "%.3f = %.1f deg = %.0f us", position,
                position * FlowerSweeperConstants.DEGREES_PER_UNIT,
                FlowerSweeperConstants.PWM_MIN_MICROSECONDS + PULSE_SPAN_MICROSECONDS * position);
    }

    private static double clip(double position) {
        return Math.max(0.0, Math.min(1.0, position));
    }
}
