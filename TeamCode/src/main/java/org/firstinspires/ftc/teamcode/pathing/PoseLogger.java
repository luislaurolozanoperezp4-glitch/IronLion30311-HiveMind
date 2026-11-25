// Sources: IronLion30311BIOBUZZInicioDeTemporada / branch feature/turrubiates/vision-align / TeamCode/.../subsystems/pathing/PoseLogger.java @ a7b8f4c (start, setOrigin, update, event, robotFrameMoveCm, close, getFileName) @ 023ae35 (Pinpoint health columns) — adapted for BIOBUZZ (pose columns only; vision and ball columns removed)
package org.firstinspires.ftc.teamcode.pathing;

import com.pedropathing.math.Pose;
import com.qualcomm.robotcore.util.ElapsedTime;
import com.qualcomm.robotcore.util.RobotLog;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Logs the robot pose during AUTO to {@code /sdcard/FIRST/auto_logs/pose_<tag>_<timestamp>.csv},
 * to check whether the robot followed the route and to tune {@code AutoConstants} estimates.
 * <p>
 * Each row: time, field x/y/heading, move from the start in the start's robot frame
 * ({@code fwd_cm}, {@code right_cm}, compare with a tape measure), the target and the distance to
 * it, the Pinpoint health ({@link PinpointDiagnostics}: status, loop frequency, raw pod counts;
 * empty without {@link #setPinpointDiagnostics}), and the routine step. {@link #event} rows (for
 * example the end of each leg) are written at once. A logging error never stops the OpMode.
 */
public class PoseLogger {

    private static final double SAMPLE_INTERVAL_SECONDS = 0.05;
    private static final double FLUSH_INTERVAL_SECONDS = 1.0;
    private static final double CM_PER_INCH = 2.54;
    private static final String LOG_DIR = "/sdcard/FIRST/auto_logs";

    private final ElapsedTime sessionTimer = new ElapsedTime();
    private final ElapsedTime sampleTimer = new ElapsedTime();
    private final ElapsedTime flushTimer = new ElapsedTime();
    private BufferedWriter writer;
    private File logFile;
    private Pose origin;
    private PinpointDiagnostics pinpointDiagnostics;

    /**
     * Opens a new timestamped CSV file; call once after START.
     *
     * @param tag OpMode name, added to the file name
     */
    public void start(String tag) {
        if (writer != null) {
            return;
        }
        try {
            File dir = new File(LOG_DIR);
            if (!dir.exists() && !dir.mkdirs()) {
                RobotLog.e("PoseLogger: cannot create " + LOG_DIR);
            }
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
            logFile = new File(dir, "pose_" + tag.replaceAll("[^A-Za-z0-9]", "") + "_" + timestamp + ".csv");
            writer = new BufferedWriter(new FileWriter(logFile));
            writer.write("time_sec,x_in,y_in,heading_deg,fwd_cm,right_cm,"
                    + "target_x_in,target_y_in,target_heading_deg,error_cm,"
                    + "pinpoint_status,pinpoint_hz,pinpoint_enc_x,pinpoint_enc_y,step\n");
            sessionTimer.reset();
            sampleTimer.reset();
            flushTimer.reset();
        } catch (IOException e) {
            RobotLog.e("PoseLogger: init failed: " + e.getMessage());
            writer = null;
        }
    }

    /**
     * Sets the pose that {@code fwd_cm} and {@code right_cm} are measured from (the start pose).
     *
     * @param origin start pose (field coordinates)
     */
    public void setOrigin(Pose origin) {
        this.origin = origin;
    }

    /**
     * Sets the Pinpoint health source for the pinpoint_* columns (they stay empty without it).
     *
     * @param pinpointDiagnostics the diagnostics, or null
     */
    public void setPinpointDiagnostics(PinpointDiagnostics pinpointDiagnostics) {
        this.pinpointDiagnostics = pinpointDiagnostics;
    }

    /**
     * Writes a row when due; call once per loop.
     *
     * @param pose   robot pose
     * @param target pose the routine drives to, or null
     * @param step   what the routine is doing
     */
    public void update(Pose pose, Pose target, String step) {
        if (writer == null || sampleTimer.seconds() < SAMPLE_INTERVAL_SECONDS) {
            return;
        }
        sampleTimer.reset();
        write(pose, target, step);
    }

    /**
     * Writes a row now, ignoring the sampling interval. Same parameters as {@link #update}.
     */
    public void event(Pose pose, Pose target, String step) {
        if (writer == null) {
            return;
        }
        write(pose, target, step);
        try {
            writer.flush();
        } catch (IOException e) {
            RobotLog.e("PoseLogger: write error: " + e.getMessage());
        }
    }

    private void write(Pose pose, Pose target, String step) {
        StringBuilder line = new StringBuilder(String.format(Locale.US, "%.3f,%.2f,%.2f,%.1f",
                sessionTimer.seconds(), pose.x(), pose.y(), Math.toDegrees(pose.heading())));
        if (origin == null) {
            line.append(",,");
        } else {
            double[] moved = robotFrameMoveCm(origin, pose);
            line.append(String.format(Locale.US, ",%.1f,%.1f", moved[0], moved[1]));
        }
        if (target == null) {
            line.append(",,,,");
        } else {
            line.append(String.format(Locale.US, ",%.2f,%.2f,%.1f,%.1f", target.x(), target.y(),
                    Math.toDegrees(target.heading()), OdometryDriver.distance(pose, target) * CM_PER_INCH));
        }
        line.append(',').append(pinpointDiagnostics == null ? ",,," : pinpointDiagnostics.csvFields());
        line.append(',').append(step == null ? "" : step.replace(',', ';')).append('\n');
        try {
            writer.write(line.toString());
            if (flushTimer.seconds() >= FLUSH_INTERVAL_SECONDS) {
                flushTimer.reset();
                writer.flush();
            }
        } catch (IOException e) {
            RobotLog.e("PoseLogger: write error: " + e.getMessage());
        }
    }

    /**
     * Move from {@code from} to {@code to}, in centimeters, in {@code from}'s robot frame.
     *
     * @return {forward, right}; forward points where the robot faced at {@code from}
     */
    public static double[] robotFrameMoveCm(Pose from, Pose to) {
        double dx = to.x() - from.x();
        double dy = to.y() - from.y();
        double cos = Math.cos(from.heading());
        double sin = Math.sin(from.heading());
        return new double[]{(dx * cos + dy * sin) * CM_PER_INCH, (dx * sin - dy * cos) * CM_PER_INCH};
    }

    /** Flushes and closes the file; call when the OpMode ends. */
    public void close() {
        if (writer == null) {
            return;
        }
        try {
            writer.close();
        } catch (IOException e) {
            RobotLog.e("PoseLogger: close error: " + e.getMessage());
        } finally {
            writer = null;
        }
    }

    /** @return the active CSV file name, or "None" if not logging */
    public String getFileName() {
        return logFile != null ? logFile.getName() : "None";
    }
}
