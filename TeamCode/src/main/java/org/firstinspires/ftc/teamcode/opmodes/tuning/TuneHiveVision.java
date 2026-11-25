// Sources: IronLion30311BIOBUZZInicioDeTemporada / branch feature/turrubiates/vision-align / TeamCode/.../opmodes/calibration/HiveStateProbe.java @ a7b8f4c (per-cell orientation signals, labeled per-tag CSV capture, slow Pedro drive, odometry move)
//          — adapted for BIOBUZZ (iterative OpMode, DataLogger, GamepadEx, HiveCell.fromTagId, TuningConstants)
package org.firstinspires.ftc.teamcode.opmodes.tuning;

import com.pedropathing.follower.Follower;
import com.pedropathing.math.Pose;
import com.qualcomm.hardware.limelightvision.LLResult;
import com.qualcomm.hardware.limelightvision.LLResultTypes;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.util.ElapsedTime;

import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;
import org.firstinspires.ftc.robotcore.external.navigation.Pose3D;
import org.firstinspires.ftc.robotcore.external.navigation.Position;
import org.firstinspires.ftc.robotcore.external.navigation.YawPitchRollAngles;
import org.firstinspires.ftc.teamcode.config.TuningConstants;
import org.firstinspires.ftc.teamcode.hardware.RobotHardware;
import org.firstinspires.ftc.teamcode.pathing.PedroConstants;
import org.firstinspires.ftc.teamcode.pathing.PoseLogger;
import org.firstinspires.ftc.teamcode.subsystems.vision.HiveCell;
import org.firstinspires.ftc.teamcode.subsystems.vision.HiveClusterPose;
import org.firstinspires.ftc.teamcode.subsystems.vision.HiveOrientation;
import org.firstinspires.ftc.teamcode.subsystems.vision.HiveVision;
import org.firstinspires.ftc.teamcode.util.Alliance;
import org.firstinspires.ftc.teamcode.util.DataLogger;
import org.firstinspires.ftc.teamcode.util.GamepadEx;
import org.firstinspires.ftc.teamcode.util.GamepadEx.Button;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Field calibration of the HIVE vision (port of the tested "Hive State Probe"). It shows, for
 * every visible HIVE cell, the orientation signals that tell an upright cluster (upward CELL,
 * scorable) from an upside-down one, and records labeled CSV rows to tune TIP detection.
 * <p>
 * Use it to:
 * <ol>
 *   <li>Decide which signal separates upright from upside down cleanly: the 3D roll, the raw
 *   3D yaw (both need "Full 3D" in the Limelight pipeline) or the in-image corner angle (works
 *   in 2D too).</li>
 *   <li>Measure {@link HiveOrientation#UPRIGHT_ROLL_REFERENCE_DEG} and
 *   {@link HiveOrientation#UPRIGHT_IMAGE_UP_REFERENCE_DEG}.</li>
 *   <li>Check from which positions the Limelight sees our alliance's upward CELL.</li>
 * </ol>
 * <b>Labeled capture:</b> after START every new Limelight frame is written to
 * {@code /sdcard/FIRST/hive_logs/*.csv}, one row per visible HIVE tag, with the label set on
 * gamepad1: D-pad up = {@code UP} (our scoring cell up), D-pad down = {@code DOWN} (after the
 * TIP), B = {@code NONE} (set it before touching the robot or the HIVE). D-pad right/left
 * changes the position label (P1, P2...). Without any HIVE tag a row with an empty
 * {@code tag_id} is written every {@link TuningConstants#VISION_NO_TAG_LOG_INTERVAL_SECONDS}.
 * <p>
 * <b>Driving:</b> left stick translate, right stick X turn, slowly (robot-centric, through
 * Pedro). Each row has the Pinpoint move since START (or the last Y): forward and right in cm
 * in the start frame, and the turn in degrees (counter-clockwise positive). Hold A to show the
 * raw per-tag angles.
 */
@TeleOp(name = "Tune Hive Vision", group = "Tuning")
public class TuneHiveVision extends OpMode {

    private static final String LOG_FOLDER = "hive_logs";
    private static final String LOG_HEADER = "time_s,label,position,odo_fwd_cm,odo_right_cm,"
            + "odo_turn_deg,tag_id,tx_deg,ty_deg,ta,cam_x_m,cam_y_m,cam_z_m,yaw_deg,pitch_deg,"
            + "roll_deg,c0_x,c0_y,c1_x,c1_y,cluster_cell,cluster_tags,cluster_fwd_m,"
            + "cluster_roll_deg,cluster_image_up_deg,cluster_raw_yaw_deg,cluster_upright";
    /** Columns after odo_turn_deg, left empty on a "no tag" row. */
    private static final int TAG_COLUMNS = 21;
    /** Camera-space position (x, y, z) and angles (yaw, pitch, roll) of one tag. */
    private static final int CAMERA_POSE_COLUMNS = 6;
    /** cluster_cell ... cluster_upright. */
    private static final int CLUSTER_COLUMNS = 7;
    /** Odometry origin: the pose at START (or gamepad1 Y), heading 0 = facing +X. */
    private static final Pose ORIGIN = new Pose(0.0, 0.0, 0.0);

    private RobotHardware hardware;
    private HiveVision vision;
    private Follower follower;
    private GamepadEx pad;
    private final DataLogger log = new DataLogger();
    private final ElapsedTime sessionTimer = new ElapsedTime();
    private final ElapsedTime noTagTimer = new ElapsedTime();

    private String label = "NONE";
    private int position = 1;
    private int visibleTags = 0;
    private double lastFrameTimestamp = Double.NaN;

    /** Builds the Limelight vision and the Pedro follower (for the slow drive and odometry). */
    @Override
    public void init() {
        hardware = new RobotHardware(hardwareMap);
        vision = new HiveVision(hardware.limelight);
        follower = PedroConstants.createFollower(hardwareMap);
        follower.setPose(ORIGIN);
        pad = new GamepadEx(gamepad1);
    }

    /** Shows the signals before START. */
    @Override
    public void init_loop() {
        hardware.clearBulkCache();
        vision.update();
        addTelemetry("INIT - read values now or press START to record");
    }

    /** Zeroes the odometry and opens the log. */
    @Override
    public void start() {
        follower.setPose(ORIGIN);
        log.open(LOG_FOLDER, "TuneHiveVision", LOG_HEADER);
        sessionTimer.reset();
        noTagTimer.reset();
    }

    /** Drives slowly, reads the camera, records labeled rows. */
    @Override
    public void loop() {
        hardware.clearBulkCache();
        pad.readButtons();
        if (pad.wasJustPressed(Button.Y)) {
            follower.setPose(ORIGIN);
        }
        double scale = TuningConstants.VISION_DRIVE_SCALE;
        double forward = -gamepad1.left_stick_y * scale;
        double strafe = gamepad1.left_stick_x * scale;
        double turn = gamepad1.right_stick_x * scale;
        // Pedro's mixing uses + strafe = left and + turn = counter-clockwise
        follower.manual(forward, -strafe, -turn);
        follower.update();
        vision.update();

        if (pad.wasJustPressed(Button.DPAD_UP)) {
            label = "UP";
        } else if (pad.wasJustPressed(Button.DPAD_DOWN)) {
            label = "DOWN";
        } else if (pad.wasJustPressed(Button.B)) {
            label = "NONE";
        }
        if (pad.wasJustPressed(Button.DPAD_RIGHT)) {
            position++;
        } else if (pad.wasJustPressed(Button.DPAD_LEFT) && position > 1) {
            position--;
        }
        logFrame();

        double[] move = odometryMove();
        telemetry.addData("Capture", "label %s | P%d | %d HIVE tags | %d rows | %s", label,
                position, visibleTags, log.getRowCount(),
                log.isOpen() ? log.getFileName() : "NOT LOGGING " + log.getLastError());
        telemetry.addData("Odometry", "fwd %.1f cm | right %.1f cm | turn %.1f deg (Y = zero)",
                move[0], move[1], move[2]);
        telemetry.addLine("D-pad up UP / down DOWN, B NONE, left/right position; sticks drive");
        addTelemetry("Running");
    }

    /** One row per visible HIVE tag of every new Limelight frame. */
    private void logFrame() {
        if (!log.isOpen() || hardware.limelight == null) {
            return;
        }
        LLResult result = hardware.limelight.getLatestResult();
        if (result == null || !result.isValid() || result.getFiducialResults() == null) {
            logNoTags();
            return;
        }
        if (result.getTimestamp() == lastFrameTimestamp) {
            if (visibleTags == 0) {
                logNoTags();
            }
            return;
        }
        lastFrameTimestamp = result.getTimestamp();
        double[] move = odometryMove();
        int hiveTags = 0;
        for (LLResultTypes.FiducialResult fiducial : result.getFiducialResults()) {
            int id = fiducial.getFiducialId();
            HiveCell cell = HiveCell.fromTagId(id);
            if (cell == null) {
                continue;
            }
            hiveTags++;
            List<Object> row = new ArrayList<>();
            row.add(sessionTimer.seconds());
            row.add(label);
            row.add("P" + position);
            row.add(move[0]);
            row.add(move[1]);
            row.add(move[2]);
            row.add(id);
            row.add(fiducial.getTargetXDegrees());
            row.add(fiducial.getTargetYDegrees());
            row.add(fiducial.getTargetArea());
            addCameraPose(row, fiducial.getTargetPoseCameraSpace());
            addCorner(row, fiducial.getTargetCorners(), 0);
            addCorner(row, fiducial.getTargetCorners(), 1);
            addCluster(row, cell);
            log.logRow(row.toArray());
        }
        log.flush();
        if (hiveTags == 0) {
            logNoTags();
        } else {
            noTagTimer.reset();
            visibleTags = hiveTags;
        }
    }

    private static void addCameraPose(List<Object> row, Pose3D cam) {
        if (cam == null || cam.getPosition() == null || cam.getOrientation() == null) {
            for (int i = 0; i < CAMERA_POSE_COLUMNS; i++) {
                row.add("");
            }
            return;
        }
        Position pos = cam.getPosition().toUnit(DistanceUnit.METER);
        YawPitchRollAngles ori = cam.getOrientation();
        row.add(pos.x);
        row.add(pos.y);
        row.add(pos.z);
        row.add(ori.getYaw(AngleUnit.DEGREES));
        row.add(ori.getPitch(AngleUnit.DEGREES));
        row.add(ori.getRoll(AngleUnit.DEGREES));
    }

    /** Adds "x,y" of one corner, or two empty cells (the pipeline may not send corners). */
    private static void addCorner(List<Object> row, List<List<Double>> corners, int index) {
        if (corners == null || corners.size() <= index || corners.get(index).size() < 2) {
            row.add("");
            row.add("");
            return;
        }
        row.add(corners.get(index).get(0));
        row.add(corners.get(index).get(1));
    }

    private void addCluster(List<Object> row, HiveCell cell) {
        HiveClusterPose cluster = vision.getPose(cell);
        if (cluster == null) {
            for (int i = 0; i < CLUSTER_COLUMNS; i++) {
                row.add("");
            }
            return;
        }
        Boolean upright = vision.getUprightVerdict(cell);
        row.add(cell);
        row.add(cluster.visibleTagCount);
        row.add(cluster.forwardMeters);
        row.add(cluster.rollDeg);
        row.add(cluster.imageUpDeg);
        row.add(cluster.rawYawDeg);
        row.add(upright == null ? "" : upright ? 1 : 0);
    }

    /** Row with an empty tag_id, so the label is recorded even when no tag is visible. */
    private void logNoTags() {
        visibleTags = 0;
        if (noTagTimer.seconds() < TuningConstants.VISION_NO_TAG_LOG_INTERVAL_SECONDS) {
            return;
        }
        noTagTimer.reset();
        double[] move = odometryMove();
        List<Object> row = new ArrayList<>();
        row.add(sessionTimer.seconds());
        row.add(label);
        row.add("P" + position);
        row.add(move[0]);
        row.add(move[1]);
        row.add(move[2]);
        for (int i = 0; i < TAG_COLUMNS; i++) {
            row.add("");
        }
        log.logRow(row.toArray());
    }

    /** @return {forward cm, right cm, turn deg} from {@link #ORIGIN}, in its robot frame */
    private double[] odometryMove() {
        Pose pose = follower.pose();
        double[] cm = PoseLogger.robotFrameMoveCm(ORIGIN, pose);
        return new double[]{cm[0], cm[1], Math.toDegrees(Alliance.normalizeRadians(pose.heading()))};
    }

    private void addTelemetry(String state) {
        telemetry.addData("State", state);
        if (hardware.limelight == null) {
            telemetry.addLine("Limelight not configured (name in RobotConstants.LIMELIGHT)");
            telemetry.update();
            return;
        }
        telemetry.addData("References", "roll %.0f deg | imgUp %.0f deg | signal %s",
                HiveOrientation.UPRIGHT_ROLL_REFERENCE_DEG,
                HiveOrientation.UPRIGHT_IMAGE_UP_REFERENCE_DEG, HiveOrientation.UPRIGHT_SIGNAL);
        telemetry.addData("TIPs", "RED %d (%s) | BLUE %d (%s)",
                vision.getTipCount(Alliance.RED), vision.getHiveState(Alliance.RED),
                vision.getTipCount(Alliance.BLUE), vision.getHiveState(Alliance.BLUE));
        for (HiveCell cell : HiveCell.values()) {
            HiveClusterPose pose = vision.getPose(cell);
            String key = cell.getDisplayName();
            if (pose == null) {
                telemetry.addData(key, "not visible");
                continue;
            }
            telemetry.addData(key, "%d/4 tags %s | fwd %.2f m | roll %s -> %s | yaw3D %s | imgUp %s -> %s",
                    pose.visibleTagCount, pose.isFrom3dSolvePnp ? "3D" : "2D", pose.forwardMeters,
                    formatDeg(pose.rollDeg),
                    verdict(pose.rollDeg, HiveOrientation.UPRIGHT_ROLL_REFERENCE_DEG),
                    formatDeg(pose.rawYawDeg), formatDeg(pose.imageUpDeg),
                    verdict(pose.imageUpDeg, HiveOrientation.UPRIGHT_IMAGE_UP_REFERENCE_DEG));
        }
        if (gamepad1.a) {
            addRawTagLines();
        }
        telemetry.update();
    }

    /** One line per visible HIVE tag: raw camera-space yaw/pitch/roll. */
    private void addRawTagLines() {
        LLResult result = hardware.limelight.getLatestResult();
        if (result == null || !result.isValid() || result.getFiducialResults() == null) {
            telemetry.addData("Raw", "no valid Limelight result");
            return;
        }
        for (LLResultTypes.FiducialResult fiducial : result.getFiducialResults()) {
            int id = fiducial.getFiducialId();
            if (HiveCell.fromTagId(id) == null) {
                continue;
            }
            Pose3D cam = fiducial.getTargetPoseCameraSpace();
            if (cam == null || cam.getOrientation() == null) {
                telemetry.addData("Raw/" + id, "no 3D pose");
                continue;
            }
            YawPitchRollAngles ori = cam.getOrientation();
            telemetry.addData("Raw/" + id, "yaw %.0f pitch %.0f roll %.0f",
                    ori.getYaw(AngleUnit.DEGREES), ori.getPitch(AngleUnit.DEGREES),
                    ori.getRoll(AngleUnit.DEGREES));
        }
    }

    private static String formatDeg(double angleDeg) {
        return Double.isNaN(angleDeg) ? "n/a" : String.format(Locale.US, "%.0f", angleDeg);
    }

    private static String verdict(double angleDeg, double referenceDeg) {
        if (Double.isNaN(angleDeg)) {
            return "UNKNOWN";
        }
        return HiveOrientation.isUpright(angleDeg, referenceDeg) ? "UPRIGHT" : "UPSIDE DOWN";
    }

    /** Stops the drive, closes the log and the camera. */
    @Override
    public void stop() {
        if (follower != null) {
            follower.manual(0.0, 0.0, 0.0);
            follower.update();
            follower.stop();
        }
        if (vision != null) {
            vision.stop();
        }
        log.close();
    }
}
