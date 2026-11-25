// Sources: IronLion30311BIOBUZZInicioDeTemporada / branch feature/turrubiates/vision-align / TeamCode/.../opmodes/teleop/MainTeleOp.java @ a7b8f4c (addBallTelemetry, ball counter while collecting)
//          — adapted for BIOBUZZ (new tuning OpMode for the HuskyLens color sensing)
package org.firstinspires.ftc.teamcode.opmodes.tuning;

import com.qualcomm.hardware.dfrobot.HuskyLens;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.util.ElapsedTime;

import org.firstinspires.ftc.teamcode.config.VisionConstants;
import org.firstinspires.ftc.teamcode.hardware.RobotHardware;
import org.firstinspires.ftc.teamcode.subsystems.ColorSensing;
import org.firstinspires.ftc.teamcode.subsystems.Intake;
import org.firstinspires.ftc.teamcode.util.Alliance;
import org.firstinspires.ftc.teamcode.util.GamepadEx;
import org.firstinspires.ftc.teamcode.util.GamepadEx.Button;

import java.util.ArrayList;
import java.util.List;

/**
 * Shows what the HuskyLens (front, intake side) sees and how the code classifies it.
 * <p>
 * The HuskyLens must be in COLOR_RECOGNITION with these learned IDs:
 * {@link VisionConstants#HUSKY_POLLEN_ID} = POLLEN, {@link VisionConstants#HUSKY_RED_NECTAR_ID}
 * = red NECTAR, {@link VisionConstants#HUSKY_BLUE_NECTAR_ID} = blue NECTAR.
 * <p>
 * For every block: ID, center, size, area and the result ({@code POLLEN}, {@code RED_NECTAR},
 * {@code BLUE_NECTAR}, or why it was ignored). Use it to tune
 * {@link VisionConstants#HUSKY_MIN_BLOCK_AREA_PIXELS} (smallest real ball) and
 * {@link VisionConstants#INTAKE_ZONE_Y_OFFSET} (how low in the image a ball is "at the intake",
 * used by the G408 rejection and the NECTAR alert).
 * <p>
 * gamepad1: LB hold = intake collects (balls are counted, as in TeleOp), RB hold = eject,
 * X = reset the counts.
 */
@TeleOp(name = "Tune Color Sensor", group = "Tuning")
public class TuneColorSensor extends OpMode {

    private RobotHardware hardware;
    private ColorSensing colorSensing;
    private Intake intake;
    private GamepadEx pad;
    private final ElapsedTime readTimer = new ElapsedTime();
    private HuskyLens.Block[] lastBlocks = new HuskyLens.Block[0];

    /** Builds the camera and intake. */
    @Override
    public void init() {
        hardware = new RobotHardware(hardwareMap);
        colorSensing = new ColorSensing(hardware);
        intake = new Intake(hardware);
        pad = new GamepadEx(gamepad1);
        hardware.addTelemetry(telemetry);
    }

    /** Shows the blocks already in INIT. */
    @Override
    public void init_loop() {
        hardware.clearBulkCache();
        readCamera();
        addTelemetry();
    }

    /** Intake by hand, camera read, telemetry. */
    @Override
    public void loop() {
        hardware.clearBulkCache();
        pad.readButtons();
        if (pad.isPressed(Button.LEFT_BUMPER)) {
            intake.collect();
        } else if (pad.isPressed(Button.RIGHT_BUMPER)) {
            intake.eject();
        } else {
            intake.idle();
        }
        if (pad.wasJustPressed(Button.X)) {
            colorSensing.resetCounts();
        }
        intake.periodic();
        colorSensing.countWhileIntaking(intake.isCollecting());
        readCamera();
        addTelemetry();
    }

    /** One camera read per {@link VisionConstants#HUSKY_READ_INTERVAL_SECONDS}, shared with ColorSensing. */
    private void readCamera() {
        if (hardware.huskyLens == null
                || readTimer.seconds() < VisionConstants.HUSKY_READ_INTERVAL_SECONDS) {
            return;
        }
        readTimer.reset();
        HuskyLens.Block[] blocks = hardware.huskyLens.blocks();
        lastBlocks = blocks == null ? new HuskyLens.Block[0] : blocks;
        List<ColorSensing.Ball> balls = new ArrayList<>();
        for (HuskyLens.Block block : lastBlocks) {
            ColorSensing.Ball ball = ColorSensing.toBall(block.id, block.x, block.y, block.width,
                    block.height);
            if (ball != null) {
                balls.add(ball);
            }
        }
        colorSensing.updateFromBalls(balls);
    }

    private void addTelemetry() {
        if (hardware.huskyLens == null) {
            telemetry.addLine("HuskyLens not configured (name in RobotConstants.HUSKY_LENS)");
            telemetry.update();
            return;
        }
        telemetry.addData("Blocks", "%d (min area %d px2, intake zone y >= %.2f)",
                lastBlocks.length, VisionConstants.HUSKY_MIN_BLOCK_AREA_PIXELS,
                VisionConstants.INTAKE_ZONE_Y_OFFSET);
        for (int i = 0; i < lastBlocks.length; i++) {
            HuskyLens.Block block = lastBlocks[i];
            ColorSensing.Ball ball = ColorSensing.toBall(block.id, block.x, block.y, block.width,
                    block.height);
            telemetry.addData("Block " + i, "id %d at (%d, %d) %dx%d area %d -> %s", block.id,
                    block.x, block.y, block.width, block.height, block.width * block.height,
                    describe(block, ball));
        }
        ColorSensing.Ball closest = colorSensing.getClosestBall();
        telemetry.addData("Closest", closest == null ? "none"
                : String.format("%s x %.2f y %.2f area %d", closest.color, closest.xOffset,
                closest.yOffset, closest.areaPixels));
        telemetry.addData("NECTAR at intake", colorSensing.isNectarAtIntake());
        telemetry.addData("Opponent NECTAR at intake", "as RED %s | as BLUE %s",
                colorSensing.isOpponentNectarAtIntake(Alliance.RED),
                colorSensing.isOpponentNectarAtIntake(Alliance.BLUE));
        telemetry.addData("Ball assist turn", "%.2f", colorSensing.getAssistTurn());
        telemetry.addData("Intake", intake.getState());
        telemetry.addData("Picked up", colorSensing.getSummary());
        telemetry.addLine("LB collect | RB eject | X reset counts");
        telemetry.update();
    }

    private static String describe(HuskyLens.Block block, ColorSensing.Ball ball) {
        if (ball != null) {
            return ball.color + (ball.yOffset >= VisionConstants.INTAKE_ZONE_Y_OFFSET
                    ? " (AT INTAKE)" : "");
        }
        if (block.id != VisionConstants.HUSKY_POLLEN_ID
                && block.id != VisionConstants.HUSKY_RED_NECTAR_ID
                && block.id != VisionConstants.HUSKY_BLUE_NECTAR_ID) {
            return "ignored: unknown ID";
        }
        return "ignored: too small";
    }

    /** Stops the intake. */
    @Override
    public void stop() {
        if (intake != null) {
            intake.stop();
        }
    }
}
