// Sources: IronLion30311BIOBUZZInicioDeTemporada / branch feature/autonomos-beta / TeamCode/.../opmodes/auto/ShootTurnCollectAuto.java @ 206fa84 (runRoutine: driveForward -5, turnBy 180, driveForward 24)
//          IronLion30311BIOBUZZInicioDeTemporada / branch feature/autonomos-beta / TeamCode/.../Constants.java @ 206fa84 (Constants.ShootTurnCollect)
//          — adapted for BIOBUZZ
package org.firstinspires.ftc.teamcode.pathing.paths;

import com.pedropathing.math.Pose;
import com.pedropathing.paths.Path;

import org.firstinspires.ftc.teamcode.util.Alliance;

/**
 * "Shoot Turn Collect" (beta, {@code @Disabled} in the uncommitted diff): shoot at the start,
 * back 5 in off the wall, turn 180 degrees in place, drive 24 in forward collecting.
 * <p>
 * ARCHIVE: it has no PARK. The idea is merged into the near cycle (see
 * {@link PreloadCollectPaths}). The 180 degree turn between the two paths is not a path.
 */
public class ShootCollectPaths extends RoutePaths {

    private static final double BETA_ROBOT_LENGTH_INCHES = 16.5354;

    /** Start: front against the audience wall. */
    public static final Pose START = pose(56.0, BETA_ROBOT_LENGTH_INCHES / 2.0 + 0.25, -90.0);
    /** 5 in off the wall, room for the 12.2 in turning radius. */
    public static final Pose TURN_SPOT = pose(56.0, START.y() + 5.0, -90.0);
    /** Same spot after the 180 degree turn. */
    public static final Pose TURN_SPOT_TURNED = pose(56.0, START.y() + 5.0, 90.0);
    /** End of the 24 in collect drive. */
    public static final Pose COLLECT_END = pose(56.0, START.y() + 5.0 + 24.0, 90.0);

    /**
     * @param alliance alliance to build the paths for
     */
    public ShootCollectPaths(Alliance alliance) {
        super(alliance);
    }

    /** @return the start pose for this alliance */
    public Pose getStartPose() {
        return toField(START);
    }

    /** @return back 5 in off the wall (rear first) */
    public Path backOff() {
        return lineConstant(START, TURN_SPOT, START);
    }

    /** @return straight copy of {@link #backOff()} (already one line) */
    public Path backOffStraight() {
        return lineConstant(START, TURN_SPOT, START);
    }

    /** @return 24 in forward with the intake on */
    public Path collect() {
        return lineConstant(TURN_SPOT_TURNED, COLLECT_END, TURN_SPOT_TURNED);
    }

    /** @return straight copy of {@link #collect()} (already one line) */
    public Path collectStraight() {
        return lineConstant(TURN_SPOT_TURNED, COLLECT_END, TURN_SPOT_TURNED);
    }
}
