// Sources: IronLion30311BIOBUZZInicioDeTemporada / branch feature/turrubiates/vision-align / TeamCode/.../opmodes/auto/PreloadRouteAuto.java @ a7b8f4c (near start, TESTED)
//          IronLion30311BIOBUZZInicioDeTemporada / branch feature/autonomos-beta / TeamCode/.../opmodes/auto/ParkAuto.java @ 206fa84 (far start)
//          IronLion30311BIOBUZZInicioDeTemporada / branch feature/turrubiates/auto-leave-park / TeamCode/.../opmodes/auto/LeaveParkAuto.java @ e455142 (start inside the LOADING ZONE, front to the wall; moved out of the zone for G304.E)
//          IronLion30311BIOBUZZInicioDeTemporada / branch feature/turrubiates/vision-align / TeamCode/.../opmodes/auto/FarSideWaitTipAuto.java @ f26f8d0 (back-wall start of the wait-TIP auto)
//          — adapted for BIOBUZZ
package org.firstinspires.ftc.teamcode.opmodes.auto;

import com.pedropathing.math.Pose;

import org.firstinspires.ftc.teamcode.config.AutoConstants;
import org.firstinspires.ftc.teamcode.config.FieldConstants;
import org.firstinspires.ftc.teamcode.pathing.paths.WaitTipPaths;

/**
 * Where the robot is placed before AUTO. Poses are for RED; {@code Alliance.toField} turns them
 * into BLUE poses.
 */
public enum StartPosition {
    /** Against the audience wall, front (intake) to the wall. TESTED (4/4 preload run). */
    NEAR("Near (audience wall, x 57)", FieldConstants.NEAR_START),
    /** Against the red wall, tile A6, facing the back wall. UNTESTED (team definition, D21). */
    FAR("Far (red wall, tile A6)", FieldConstants.FAR_START),
    /**
     * Beside (behind) the LOADING ZONE, front to the red wall. It used to start inside the zone,
     * which G304.E forbids (manual TU10). Position TODO: TUNE.
     */
    LOADING_ZONE("Beside loading zone (front to red wall, behind the tape)", new Pose(AutoConstants.LOADING_ZONE_START_X,
            AutoConstants.LOADING_ZONE_START_Y, Math.toRadians(AutoConstants.LOADING_ZONE_START_HEADING_DEG))),
    /**
     * Against the back wall (front to it), right of the back-wall FLOWER: the wait-TIP auto
     * (vision-align calls it "far side"). NOT the team's FAR (D21: the wall farthest from our
     * LOADING ZONE). UNTESTED, position TODO: TUNE.
     */
    BACK_WALL("Back wall (front to the wall, right of the FLOWER)", WaitTipPaths.START);

    private final String label;
    private final Pose redPose;

    StartPosition(String label, Pose redPose) {
        this.label = label;
        this.redPose = redPose;
    }

    /** @return text for the Driver Station */
    public String getLabel() {
        return label;
    }

    /** @return the start pose for RED */
    public Pose getRedPose() {
        return redPose;
    }
}
