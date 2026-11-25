// Sources: IronLion30311BIOBUZZInicioDeTemporada / branch feature/turrubiates/vision-align / TeamCode/.../opmodes/auto/OdometrySquareTestAuto.java @ a7b8f4c (buildPoses, side) — adapted for BIOBUZZ
package org.firstinspires.ftc.teamcode.pathing.paths;

import com.pedropathing.math.Pose;
import com.pedropathing.paths.Path;

import org.firstinspires.ftc.teamcode.util.Alliance;

/**
 * Tuning route: a square of {@link #SIDE_INCHES} without turning (forward, left, back, right) back
 * to the start. Used by {@code pathing.tuning.TestOdometrySquare}. Not a competition route, so it
 * has no straight copy (the start and end are the same point).
 */
public class TestSquarePaths extends RoutePaths {

    /** Side of the square (one tile). */
    public static final double SIDE_INCHES = 24.0;
    /** Start: the old Outer Lane / Start Routine start, rear against the audience wall. */
    public static final Pose START = pose(56.0, 8.0, 90.0);
    /** Corner 1: one side forward (heading 90 faces +y). */
    public static final Pose FORWARD = pose(START.x(), START.y() + SIDE_INCHES, 90.0);
    /** Corner 2: then one side to the left (-x). */
    public static final Pose FORWARD_LEFT = pose(START.x() - SIDE_INCHES, START.y() + SIDE_INCHES, 90.0);
    /** Corner 3: then one side back. */
    public static final Pose LEFT = pose(START.x() - SIDE_INCHES, START.y(), 90.0);

    /**
     * @param alliance alliance to build the paths for
     */
    public TestSquarePaths(Alliance alliance) {
        super(alliance);
    }

    /** @return the start pose for this alliance */
    public Pose getStartPose() {
        return toField(START);
    }

    /** @return the four sides as one chain, constant heading */
    public Path square() {
        return chain(lineLinear(START, FORWARD), lineLinear(FORWARD, FORWARD_LEFT),
                lineLinear(FORWARD_LEFT, LEFT), lineLinear(LEFT, START));
    }

    /**
     * @param index side 0 to 3
     * @return one side of the square, to follow one at a time
     */
    public Path side(int index) {
        Pose[] corners = {START, FORWARD, FORWARD_LEFT, LEFT, START};
        int i = Math.max(0, Math.min(3, index));
        return lineLinear(corners[i], corners[i + 1]);
    }
}
