// Sources: IronLion30311BIOBUZZInicioDeTemporada / branch feature/turrubiates/vision-align / TeamCode/.../subsystems/pathing/FieldDrawing.java @ a7b8f4c (draw, clearTrail, drawRobot, toDashboard) — adapted for BIOBUZZ (adds drawPath)
package org.firstinspires.ftc.teamcode.pathing;

import com.acmerobotics.dashboard.FtcDashboard;
import com.acmerobotics.dashboard.canvas.Canvas;
import com.acmerobotics.dashboard.telemetry.TelemetryPacket;
import com.pedropathing.math.Pose;
import com.pedropathing.paths.Path;
import com.qualcomm.robotcore.util.ElapsedTime;

import org.firstinspires.ftc.teamcode.config.FieldConstants;
import org.firstinspires.ftc.teamcode.config.RobotConstants;

import java.util.ArrayList;
import java.util.List;

/**
 * Draws the robot pose, the path it has driven and (optionally) a planned Pedro path on the FTC
 * Dashboard field view (http://192.168.43.1:8080/dash), over the BIOBUZZ field image.
 * <p>
 * The image {@code TeamCode/src/main/assets/images/biobuzz.webp} comes from the Pedro Pathing
 * Visualizer (Apache-2.0, https://github.com/Pedro-Pathing/Visualizer). Pedro poses (origin at
 * the bottom-left corner) are converted to the Dashboard frame (origin at the center, +x up the
 * screen, +y left): {@code xDash = yPedro - 72}, {@code yDash = 72 - xPedro}.
 * Poses are drawn as given: draw BLUE poses in absolute field coordinates.
 */
public class FieldDrawing {

    private static final String FIELD_IMAGE = "/images/biobuzz.webp";
    private static final int TILES_PER_SIDE = 6;
    private static final double TRAIL_INTERVAL_SECONDS = 0.1;
    private static final int MAX_TRAIL_POINTS = 300;
    private static final int PATH_SAMPLES = 50;

    private final List<double[]> trail = new ArrayList<>();
    private final ElapsedTime trailTimer = new ElapsedTime();
    private Path plannedPath;

    /**
     * Sets a path drawn under the robot until changed (null clears it).
     *
     * @param path the planned path, in field coordinates
     */
    public void drawPath(Path path) {
        plannedPath = path;
    }

    /**
     * Sends one field-view packet with the robot at {@code pose}; call once per loop.
     *
     * @param pose robot pose in Pedro field coordinates (inches, radians)
     */
    public void draw(Pose pose) {
        double[] robot = toDashboard(pose.x(), pose.y());
        if (trail.isEmpty() || trailTimer.seconds() >= TRAIL_INTERVAL_SECONDS) {
            trailTimer.reset();
            trail.add(robot);
            if (trail.size() > MAX_TRAIL_POINTS) {
                trail.remove(0);
            }
        }
        double size = FieldConstants.FIELD_SIZE_INCHES;
        // false: skip the Dashboard's default field image
        TelemetryPacket packet = new TelemetryPacket(false);
        Canvas field = packet.fieldOverlay();
        field.drawImage(FIELD_IMAGE, 0, 0, size, size)
                .drawGrid(0, 0, size, size, TILES_PER_SIDE + 1, TILES_PER_SIDE + 1);

        if (plannedPath != null) {
            double[] xs = new double[PATH_SAMPLES + 1];
            double[] ys = new double[PATH_SAMPLES + 1];
            for (int i = 0; i <= PATH_SAMPLES; i++) {
                Pose p = plannedPath.get((double) i / PATH_SAMPLES);
                double[] d = toDashboard(p.x(), p.y());
                xs[i] = d[0];
                ys[i] = d[1];
            }
            field.setStroke("#2196F3").setStrokeWidth(1).strokePolyline(xs, ys);
        }

        double[] xs = new double[trail.size()];
        double[] ys = new double[trail.size()];
        for (int i = 0; i < trail.size(); i++) {
            xs[i] = trail.get(i)[0];
            ys[i] = trail.get(i)[1];
        }
        field.setStroke("#4CAF50").setStrokeWidth(1).strokePolyline(xs, ys);

        drawRobot(field, robot[0], robot[1], pose.heading() - Math.PI / 2.0);
        FtcDashboard.getInstance().sendTelemetryPacket(packet);
    }

    /** Clears the driven-path trail (for example when the start pose changes in INIT). */
    public void clearTrail() {
        trail.clear();
    }

    /** Robot footprint plus a line from the center to the front (intake side). */
    private static void drawRobot(Canvas field, double x, double y, double headingRadians) {
        double halfLength = RobotConstants.ROBOT_LENGTH_INCHES / 2.0;
        double halfWidth = RobotConstants.ROBOT_WIDTH_INCHES / 2.0;
        double cos = Math.cos(headingRadians);
        double sin = Math.sin(headingRadians);
        double[] cornersX = {halfLength, halfLength, -halfLength, -halfLength};
        double[] cornersY = {halfWidth, -halfWidth, -halfWidth, halfWidth};
        double[] xs = new double[4];
        double[] ys = new double[4];
        for (int i = 0; i < 4; i++) {
            xs[i] = x + cornersX[i] * cos - cornersY[i] * sin;
            ys[i] = y + cornersX[i] * sin + cornersY[i] * cos;
        }
        field.setStroke("#FFC107").setStrokeWidth(2)
                .strokePolygon(xs, ys)
                .strokeLine(x, y, x + halfLength * cos, y + halfLength * sin);
    }

    /** Pedro field coordinates (origin bottom-left) to the Dashboard frame (origin center). */
    private static double[] toDashboard(double xPedro, double yPedro) {
        double half = FieldConstants.FIELD_CENTER_INCHES;
        return new double[]{yPedro - half, half - xPedro};
    }
}
