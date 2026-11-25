// Sources: new for IronLion30311-HiveMind (hybrid drive: decides Pedro vs odometry driver per route and builds the Pedro path)
package org.firstinspires.ftc.teamcode.pathing;

import com.pedropathing.api.Paths;
import com.pedropathing.math.Pose;
import com.pedropathing.paths.Path;

import org.firstinspires.ftc.teamcode.config.HybridDriveConstants;

import java.util.ArrayList;
import java.util.List;

/**
 * Hardware-free helpers for the hybrid drive.
 * <p>
 * A route is a list of field waypoints (the same ones the odometry driver drives). When Pedro is
 * chosen, the robot follows ONE Pedro path through all of them without stopping: a straight
 * line per leg, holding each leg's end heading (the same geometry the odometry driver uses).
 * The odometry driver then finishes the last few inches precisely.
 */
public final class HybridDrive {

    private HybridDrive() {
    }

    /**
     * @param route the route
     * @return true if this route should use Pedro path following now (master switch AND route switch)
     */
    public static boolean usesPedro(DriveRoute route) {
        return HybridDriveConstants.IS_PEDRO_TUNED && isRouteSwitchOn(route);
    }

    /**
     * @param route the route
     * @return the route's own switch in {@link HybridDriveConstants}
     */
    static boolean isRouteSwitchOn(DriveRoute route) {
        switch (route) {
            case PRELOAD_PARK:
                return HybridDriveConstants.PEDRO_PRELOAD_PARK;
            case CYCLE_PARK:
                return HybridDriveConstants.PEDRO_CYCLE_PARK;
            case OTHER_CELL_TRANSIT:
                return HybridDriveConstants.PEDRO_OTHER_CELL_TRANSIT;
            case OTHER_SIDE_PARK:
                return HybridDriveConstants.PEDRO_OTHER_SIDE_PARK;
            case WAIT_TIP_PARK:
                return HybridDriveConstants.PEDRO_WAIT_TIP_PARK;
            case SAFE_PARK:
                return HybridDriveConstants.PEDRO_SAFE_PARK;
            case EMERGENCY_PARK:
                return HybridDriveConstants.PEDRO_EMERGENCY_PARK;
            default:
                return false;
        }
    }

    /**
     * The legs of a route: from the robot's pose through every waypoint.
     *
     * @param start     robot pose when the route starts (field frame)
     * @param waypoints field waypoints in order
     * @return pairs {from, to}, one per leg (zero-length legs are skipped)
     */
    public static List<Pose[]> getLegs(Pose start, List<Pose> waypoints) {
        List<Pose[]> legs = new ArrayList<>();
        Pose from = start;
        for (Pose to : waypoints) {
            if (OdometryDriver.distance(from, to) > MIN_LEG_INCHES) {
                legs.add(new Pose[] {from, to});
                from = to;
            }
        }
        return legs;
    }

    /**
     * @param start     robot pose when the route starts
     * @param waypoints field waypoints in order
     * @return the route length in inches
     */
    public static double getLengthInches(Pose start, List<Pose> waypoints) {
        double length = 0.0;
        for (Pose[] leg : getLegs(start, waypoints)) {
            length += OdometryDriver.distance(leg[0], leg[1]);
        }
        return length;
    }

    /**
     * @param start     robot pose when the route starts
     * @param waypoints field waypoints in order
     * @return how long Pedro may take before the odometry driver takes over, seconds
     */
    public static double getTimeoutSeconds(Pose start, List<Pose> waypoints) {
        return HybridDriveConstants.PEDRO_TIMEOUT_BASE_SECONDS
                + getLengthInches(start, waypoints) / HybridDriveConstants.PEDRO_TIMEOUT_INCHES_PER_SECOND;
    }

    /**
     * Builds the Pedro path: one straight line per leg, each holding its end heading, joined so
     * the robot does not stop at the corners.
     *
     * @param start     robot pose when the route starts
     * @param waypoints field waypoints in order
     * @return the path, or null if every leg is shorter than {@link #MIN_LEG_INCHES}
     */
    public static Path buildPath(Pose start, List<Pose> waypoints) {
        List<Pose[]> legs = getLegs(start, waypoints);
        if (legs.isEmpty()) {
            return null;
        }
        Path[] segments = new Path[legs.size()];
        for (int i = 0; i < legs.size(); i++) {
            segments[i] = Paths.line(legs.get(i)[0], legs.get(i)[1]).constant(legs.get(i)[1]);
        }
        return segments.length == 1 ? segments[0] : Paths.path(segments);
    }

    /** Legs shorter than this are skipped (Pedro needs a real segment). */
    public static final double MIN_LEG_INCHES = 0.5;
}
