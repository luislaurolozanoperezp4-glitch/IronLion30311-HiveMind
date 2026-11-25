// Sources: IronLion30311BIOBUZZInicioDeTemporada / branch feature/autonomos-beta / TeamCode/.../opmodes/auto/ParkAuto.java @ 206fa84 (buildPoses, downWall) — adapted for BIOBUZZ
package org.firstinspires.ftc.teamcode.pathing.paths;

import com.pedropathing.math.Pose;
import com.pedropathing.paths.Path;

import org.firstinspires.ftc.teamcode.config.FieldConstants;
import org.firstinspires.ftc.teamcode.config.RobotConstants;
import org.firstinspires.ftc.teamcode.util.Alliance;

/**
 * "Park" (beta, old {@code ParkAuto}, earlier {@code TopWallAuto}): from the far start against
 * the red wall (tile A6), drive backwards down the wall into the LOADING ZONE. No turn.
 * <p>
 * The end keeps the robot side {@code FieldConstants.WALL_CLEARANCE_INCHES} off the red wall so
 * LEAVE counts (beta ended at x 9.5, 0.5 in from the wall). The encoder-only fallback auto is not
 * a path; see {@code docs/agents/auto.md}.
 */
public class SafeParkPaths extends RoutePaths {

    /** Far start, against the red wall, tile A6 (decision D21). */
    public static final Pose FAR_START = FieldConstants.FAR_START;
    /**
     * The reverse-tangent line ends a few degrees off 90, which widens the robot footprint in x;
     * this extra inch keeps the corner {@code WALL_CLEARANCE_INCHES} off the wall.
     */
    private static final double HEADING_SLANT_MARGIN_INCHES = 1.0;
    /** End of the park, partly inside the LOADING ZONE, off the wall (LEAVE). */
    public static final Pose PARK = pose(FieldConstants.RED_LOADING_ZONE_MIN_X
            + RobotConstants.ROBOT_WIDTH_INCHES / 2.0 + FieldConstants.WALL_CLEARANCE_INCHES
            + HEADING_SLANT_MARGIN_INCHES, 102.4463, 90.0);

    /**
     * @param alliance alliance to build the paths for
     */
    public SafeParkPaths(Alliance alliance) {
        super(alliance);
    }

    /** @return the start pose for this alliance */
    public Pose getStartPose() {
        return toField(FAR_START);
    }

    /** @return P1: backwards down the red wall into the LOADING ZONE */
    public Path farStartToLoadingZone() {
        return lineReverseTangent(FAR_START, PARK);
    }

    /** @return straight copy of {@link #farStartToLoadingZone()} (already one line) */
    public Path farStartToLoadingZoneStraight() {
        return lineReverseTangent(FAR_START, PARK);
    }
}
