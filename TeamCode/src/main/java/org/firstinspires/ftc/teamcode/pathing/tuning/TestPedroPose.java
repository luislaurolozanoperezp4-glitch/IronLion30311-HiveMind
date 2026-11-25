// Sources: IronLion30311BIOBUZZInicioDeTemporada / branch feature/turrubiates/vision-align / TeamCode/.../opmodes/calibration/PedroPoseCheck.java @ a7b8f4c (runOpMode, resetPose) — adapted for BIOBUZZ (iterative OpMode)
package org.firstinspires.ftc.teamcode.pathing.tuning;

import com.pedropathing.follower.Follower;
import com.pedropathing.math.Pose;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

import org.firstinspires.ftc.teamcode.config.FieldConstants;
import org.firstinspires.ftc.teamcode.pathing.FieldDrawing;
import org.firstinspires.ftc.teamcode.pathing.PedroConstants;

/**
 * Drive by hand while watching the Pedro odometry, to check it against the field tiles (24 in
 * each). The pose is on the telemetry and drawn on the FTC Dashboard field view.
 * <p>
 * gamepad1: left stick translate, right stick x rotate (robot-centric, slow); A sets the pose to
 * {@link FieldConstants#NEAR_START} (place the robot there first).
 * <p>
 * Checks: a 4-tile (96 in) push must read ~96 in, and a 360 degree spin in place must come back
 * to the same x and y.
 */
@TeleOp(name = "Test Pedro Pose", group = "Tuning")
public class TestPedroPose extends OpMode {

    /** Drive power scale so the robot moves slowly while checking. */
    private static final double DRIVE_SCALE = 0.4;

    private Follower follower;
    private final FieldDrawing fieldDrawing = new FieldDrawing();

    /** Builds the follower and sets the start pose. */
    @Override
    public void init() {
        follower = PedroConstants.createFollower(hardwareMap);
        resetPose();
        telemetry.addLine("Place the robot on the near start pose, then press START");
    }

    /** Drives by hand and shows the pose. */
    @Override
    public void loop() {
        if (gamepad1.a) {
            resetPose();
        }
        double forward = -gamepad1.left_stick_y * DRIVE_SCALE;
        double strafe = gamepad1.left_stick_x * DRIVE_SCALE;
        double turn = gamepad1.right_stick_x * DRIVE_SCALE;
        // Pedro's mixing uses + strafe = left and + turn = counter-clockwise
        follower.manual(forward, -strafe, -turn);
        follower.update();
        Pose pose = follower.pose();
        Pose start = FieldConstants.NEAR_START;
        telemetry.addData("Pose (in, deg)", "x=%.2f  y=%.2f  h=%.1f",
                pose.x(), pose.y(), Math.toDegrees(pose.heading()));
        telemetry.addData("From start (in)", "dx=%.2f  dy=%.2f", pose.x() - start.x(), pose.y() - start.y());
        telemetry.addLine("A = reset to start pose | Dashboard field view shows the robot");
        fieldDrawing.draw(pose);
    }

    /** Stops the drive. */
    @Override
    public void stop() {
        if (follower != null) {
            follower.stop();
            follower.update();
        }
    }

    private void resetPose() {
        follower.setPose(FieldConstants.NEAR_START);
        follower.update();
        fieldDrawing.clearTrail();
    }
}
