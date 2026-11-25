// Sources: IronLion30311BIOBUZZInicioDeTemporada / branch feature/turrubiates/vision-align / TeamCode/.../opmodes/auto/PreloadRouteAuto.java @ a7b8f4c (runRoutine park legs, without shooting) — adapted for BIOBUZZ
package org.firstinspires.ftc.teamcode.pathing.tuning;

import com.pedropathing.follower.Follower;
import com.pedropathing.math.Pose;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;

import org.firstinspires.ftc.teamcode.config.AutoConstants;
import org.firstinspires.ftc.teamcode.config.DriveConstants;
import org.firstinspires.ftc.teamcode.pathing.FieldDrawing;
import org.firstinspires.ftc.teamcode.pathing.OdometryDriver;
import org.firstinspires.ftc.teamcode.pathing.PedroConstants;
import org.firstinspires.ftc.teamcode.pathing.PoseLogger;
import org.firstinspires.ftc.teamcode.pathing.paths.PreloadParkPaths;
import org.firstinspires.ftc.teamcode.util.Alliance;
import org.firstinspires.ftc.teamcode.util.MatchTimer;

import java.util.ArrayList;
import java.util.List;

/**
 * Drives the TESTED near route legs (LEAVE, then the four park legs) with the ported
 * non-blocking {@link OdometryDriver}, without shooting. Use it to check the port on the robot
 * before the competition autos use it: it should park like the 2026-11-23 run (~6.3 s of legs).
 * <p>
 * Place the robot on the near start (57, 9, -90 deg), front against the audience wall. Each leg
 * result is shown on the telemetry and written to the pose log.
 */
@Autonomous(name = "Test Odometry Driver", group = "Tuning")
public class TestOdometryDriver extends OpMode {

    private Follower follower;
    private OdometryDriver driver;
    private PreloadParkPaths paths;
    private final FieldDrawing fieldDrawing = new FieldDrawing();
    private final PoseLogger poseLogger = new PoseLogger();
    private final List<Pose> legs = new ArrayList<>();
    private final List<String> legResults = new ArrayList<>();
    private int legIndex;
    private boolean isSettling;

    /** Builds the follower at the near start. */
    @Override
    public void init() {
        follower = PedroConstants.createFollower(hardwareMap);
        paths = new PreloadParkPaths(Alliance.RED);
        follower.setPose(paths.getStartPose());
        follower.update();
        driver = new OdometryDriver(OdometryDriver.fromFollower(follower), MatchTimer.systemClock());
        legs.addAll(paths.getLeaveWaypoints());
        legs.addAll(paths.getParkWaypoints());
    }

    /** Shows the odometry while waiting. */
    @Override
    public void init_loop() {
        follower.update();
        telemetry.addData("Start error (in)", "%.1f",
                OdometryDriver.distance(follower.pose(), paths.getStartPose()));
    }

    /** Checks the start pose and starts the first leg. */
    @Override
    public void start() {
        follower.update();
        legResults.add(driver.checkStartPose(paths.getStartPose()).toString());
        poseLogger.start("TestOdometryDriver");
        poseLogger.setOrigin(paths.getStartPose());
        legIndex = 0;
        driver.driveTo("Leg 1", legs.get(0), DriveConstants.AUTO_MAX_POWER);
    }

    /** Runs the legs one after the other, settling between them, then holds PARK. */
    @Override
    public void loop() {
        follower.update();
        driver.periodic();
        if (driver.isDone()) {
            if (!isSettling) {
                legResults.add(driver.getResultText());
                poseLogger.event(follower.pose(), driver.getTarget(), "END " + driver.getResultText());
                isSettling = true;
                driver.hold(legIndex == legs.size() - 1 ? Double.POSITIVE_INFINITY
                        : AutoConstants.SETTLE_SECONDS);
            } else if (legIndex < legs.size() - 1) {
                isSettling = false;
                legIndex++;
                driver.driveTo("Leg " + (legIndex + 1), legs.get(legIndex), DriveConstants.AUTO_MAX_POWER);
            }
        }
        Pose pose = follower.pose();
        poseLogger.update(pose, driver.getTarget(), "Leg " + (legIndex + 1));
        fieldDrawing.draw(pose);
        telemetry.addData("State", "%s leg %d/%d", driver.getState(), legIndex + 1, legs.size());
        for (String result : legResults) {
            telemetry.addLine(result);
        }
    }

    /** Stops the drive and closes the log. */
    @Override
    public void stop() {
        if (driver != null) {
            driver.cancel();
        }
        if (follower != null) {
            follower.update();
        }
        poseLogger.close();
    }
}
