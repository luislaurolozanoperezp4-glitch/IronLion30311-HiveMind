// Sources: IronLion30311BIOBUZZInicioDeTemporada / branch feature/autonomos-beta / TeamCode/.../opmodes/auto/ParkAuto.java @ 206fa84 (buildPoses, downWall) — adapted for BIOBUZZ
package org.firstinspires.ftc.teamcode.pathing.paths;

import com.pedropathing.math.Pose;
import com.pedropathing.paths.Path;

import org.firstinspires.ftc.teamcode.config.FieldConstants;
import org.firstinspires.ftc.teamcode.util.Alliance;

/**
 * "Park" (beta, old {@code ParkAuto}, earlier {@code TopWallAuto}): from the far start against
 * the red wall (tile A6), drive backwards down the wall into the LOADING ZONE. No turn.
 * <p>
 * The end is 0.59 in from the red wall (17.83 in robot), so LEAVE may not count (WALL flag).
 * The fallback autos that do not use Pedro (10 cm leave from inside the LOADING ZONE, and the
 * encoder-only leave) are not paths; see {@code docs/agents/auto.md}.
 */
public class SafeParkPaths extends RoutePaths {

    /** Far start, against the red wall, tile A6 (decision D21). */
    public static final Pose FAR_START = FieldConstants.FAR_START;
    /** End of the park, inside the LOADING ZONE. */
    public static final Pose PARK = pose(9.5, 102.4463, 90.0);

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
