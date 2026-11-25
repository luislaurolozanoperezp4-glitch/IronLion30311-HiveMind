// Sources: IronLion30311BIOBUZZInicioDeTemporada / branch feature/turrubiates/vision-align / TeamCode/.../opmodes/auto/OdometrySquareTestAuto.java @ a7b8f4c (runRoutine, side) — adapted for BIOBUZZ (iterative OpMode, no mechanisms)
package org.firstinspires.ftc.teamcode.pathing.tuning;

import com.pedropathing.follower.Follower;
import com.pedropathing.math.Pose;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.util.ElapsedTime;

import org.firstinspires.ftc.teamcode.config.AutoConstants;
import org.firstinspires.ftc.teamcode.pathing.FieldDrawing;
import org.firstinspires.ftc.teamcode.pathing.OdometryDriver;
import org.firstinspires.ftc.teamcode.pathing.PedroConstants;
import org.firstinspires.ftc.teamcode.pathing.paths.TestSquarePaths;
import org.firstinspires.ftc.teamcode.util.Alliance;

/**
 * Follows a one-tile square with Pedro paths, without turning (forward, left, back, right), then
 * shows how far the odometry thinks it ended from the start.
 * <p>
 * Run it after AutoTune. Mark the start with tape at {@link TestSquarePaths#START}:
 * robot on the mark but a large error shown = odometry wrong; small error shown but robot off the
 * mark = odometry also wrong; both agree the robot is off = the follower needs tuning.
 */
@Autonomous(name = "Test Odometry Square", group = "Tuning")
public class TestOdometrySquare extends OpMode {

    private Follower follower;
    private TestSquarePaths square;
    private final FieldDrawing fieldDrawing = new FieldDrawing();
    private final ElapsedTime sideTimer = new ElapsedTime();
    private int side;

    /** Builds the follower at the square's start. */
    @Override
    public void init() {
        follower = PedroConstants.createFollower(hardwareMap);
        square = new TestSquarePaths(Alliance.RED);
        follower.setPose(square.getStartPose());
        follower.update();
        telemetry.addData("Place the robot at", square.getStartPose());
    }

    /** Starts the first side. */
    @Override
    public void start() {
        side = 0;
        followSide();
    }

    /** Follows the sides one after the other, then shows the result. */
    @Override
    public void loop() {
        follower.update();
        Pose pose = follower.pose();
        boolean isSideOver = !follower.following() || sideTimer.seconds() > AutoConstants.PATH_TIMEOUT_SECONDS;
        if (side < 4 && isSideOver) {
            side++;
            if (side < 4) {
                followSide();
            } else {
                follower.hold(square.getStartPose());
            }
        }
        if (side >= 4) {
            Pose start = square.getStartPose();
            telemetry.addData("RESULT", "odometry says %.2f in and %.1f deg from the start",
                    OdometryDriver.distance(pose, start),
                    Math.toDegrees(OdometryDriver.angleError(pose.heading(), start.heading())));
        } else {
            telemetry.addData("Side", side + 1);
        }
        telemetry.addData("Pose (in, deg)", "x=%.2f  y=%.2f  h=%.1f",
                pose.x(), pose.y(), Math.toDegrees(pose.heading()));
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

    private void followSide() {
        follower.follow(square.side(side));
        fieldDrawing.drawPath(square.side(side));
        sideTimer.reset();
    }
}
