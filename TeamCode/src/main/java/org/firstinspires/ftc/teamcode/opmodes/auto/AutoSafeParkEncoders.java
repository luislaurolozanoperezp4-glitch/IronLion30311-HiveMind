// Sources: IronLion30311BIOBUZZInicioDeTemporada / branch feature/turrubiates/auto-leave-park / TeamCode/.../opmodes/auto/LeaveParkAuto.java @ e455142 (LEAVE + PARK from inside the LOADING ZONE, start delay, light result)
//          — adapted for BIOBUZZ (iterative OpMode, wheel encoders only)
package org.firstinspires.ftc.teamcode.opmodes.auto;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;

import org.firstinspires.ftc.teamcode.config.AutoConstants;
import org.firstinspires.ftc.teamcode.hardware.RobotHardware;
import org.firstinspires.ftc.teamcode.subsystems.Drivetrain;
import org.firstinspires.ftc.teamcode.subsystems.Lights;
import org.firstinspires.ftc.teamcode.util.GamepadEx;
import org.firstinspires.ftc.teamcode.util.MatchTimer;
import org.firstinspires.ftc.teamcode.util.PoseStorage;

/**
 * Fallback auto that needs no Pinpoint, no Pedro and no cameras: LEAVE (3 points) and PARK
 * (5 points) on the wheel encoders only. Use it when the odometry is broken.
 * <p>
 * <b>Setup:</b> place the robot inside our LOADING ZONE with the front (intake) touching the
 * side wall. It drives backwards {@link AutoConstants#ENCODER_LEAVE_CM} so it no longer touches
 * the wall (LEAVE) while its front stays in the zone (PARK). A hard timeout keeps it in the zone
 * even if the encoders fail ({@link GuardedLeaveMove}).
 * <p>
 * INIT (gamepad1): B = RED, X = BLUE (light color only), D-pad up / down = start delay.
 * Light after the move: green = distance reached, red = timed out (check the robot position).
 * <p>
 * No pose is saved for TeleOp (there is no odometry). No {@code preselectTeleOp}: pick it by hand.
 */
@Autonomous(name = "Auto Safe Park Encoders", group = "Competition")
public class AutoSafeParkEncoders extends OpMode {

    private final MatchTimer.Clock clock = MatchTimer.systemClock();
    private final AutoSelector selector = new AutoSelector(null, StartPosition.LOADING_ZONE);
    private RobotHardware hardware;
    private Drivetrain drivetrain;
    private Lights lights;
    private GamepadEx pad;
    private GuardedLeaveMove leaveMove;
    private double startSeconds;
    private boolean hasStarted;

    /** Builds the drivetrain and the light. */
    @Override
    public void init() {
        hardware = new RobotHardware(hardwareMap);
        drivetrain = new Drivetrain(hardware);
        lights = new Lights(hardware);
        pad = new GamepadEx(gamepad1);
        leaveMove = new GuardedLeaveMove(new GuardedLeaveMove.Wheels() {
            @Override
            public int[] getWheelPositions() {
                return drivetrain.getWheelPositions();
            }

            @Override
            public void drive(double forward) {
                // Short ramp: the move only lasts ~0.3 s
                drivetrain.driveRamped(forward, 0.0, 0.0);
            }

            @Override
            public void stop() {
                drivetrain.stop();
            }
        }, clock);
        lights.showAlliance(selector.getAlliance());
    }

    /** INIT menu. */
    @Override
    public void init_loop() {
        hardware.clearBulkCache();
        pad.readButtons();
        for (GamepadEx.Button button : GamepadEx.Button.values()) {
            if (pad.wasJustPressed(button) && selector.onPress(button)) {
                lights.showAlliance(selector.getAlliance());
            }
        }
        lights.periodic();
        telemetry.addLine("Setup: inside the LOADING ZONE, front (intake) against the side wall");
        telemetry.addData("Selected", selector.getSummary());
        telemetry.addData("Controls", "gamepad1: B = RED, X = BLUE | D-pad Up/Down = delay");
        for (String warning : hardware.getWarnings()) {
            telemetry.addLine("WARNING: " + warning);
        }
    }

    /** Starts the delay timer. */
    @Override
    public void start() {
        PoseStorage.clear();
        startSeconds = clock.nowSeconds();
    }

    /** Waits for the delay, then runs the guarded move and holds still. */
    @Override
    public void loop() {
        hardware.clearBulkCache();
        double elapsed = clock.nowSeconds() - startSeconds;
        if (!hasStarted && elapsed >= selector.getStartDelaySeconds()) {
            hasStarted = true;
            lights.setWhite();
            leaveMove.start(AutoConstants.ENCODER_LEAVE_CM, -AutoConstants.ENCODER_LEAVE_POWER,
                    AutoConstants.ENCODER_LEAVE_TIMEOUT_SECONDS);
        }
        if (hasStarted && !leaveMove.isDone()) {
            leaveMove.periodic();
            if (leaveMove.isDone()) {
                if (leaveMove.getResult() == GuardedLeaveMove.Result.REACHED) {
                    lights.setGreen();
                } else {
                    lights.setRed();
                }
            }
        }
        drivetrain.periodic();
        lights.periodic();
        telemetry.addData("State", !hasStarted ? String.format("Start delay (%.1f s left)",
                selector.getStartDelaySeconds() - elapsed)
                : leaveMove.isDone() ? "Done: holding in the LOADING ZONE" : "LEAVE move");
        telemetry.addData("LEAVE", leaveMove.getResultText());
    }

    /** Stops the wheels. */
    @Override
    public void stop() {
        leaveMove.cancel();
        drivetrain.stop();
        lights.stop();
    }
}
