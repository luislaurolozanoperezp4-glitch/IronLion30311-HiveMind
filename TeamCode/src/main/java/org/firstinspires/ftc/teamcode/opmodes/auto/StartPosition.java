// Sources: IronLion30311BIOBUZZInicioDeTemporada / branch feature/turrubiates/vision-align / TeamCode/.../opmodes/auto/PreloadRouteAuto.java @ a7b8f4c (near start, TESTED)
//          IronLion30311BIOBUZZInicioDeTemporada / branch feature/autonomos-beta / TeamCode/.../opmodes/auto/ParkAuto.java @ 206fa84 (far start)
//          IronLion30311BIOBUZZInicioDeTemporada / branch feature/turrubiates/auto-leave-park / TeamCode/.../opmodes/auto/LeaveParkAuto.java @ e455142 (start inside the LOADING ZONE, front to the wall)
//          — adapted for BIOBUZZ
package org.firstinspires.ftc.teamcode.opmodes.auto;

import com.pedropathing.math.Pose;

import org.firstinspires.ftc.teamcode.config.AutoConstants;
import org.firstinspires.ftc.teamcode.config.FieldConstants;

/**
 * Where the robot is placed before AUTO. Poses are for RED; {@code Alliance.toField} turns them
 * into BLUE poses.
 */
public enum StartPosition {
    /** Against the audience wall, front (intake) to the wall. TESTED (4/4 preload run). */
    NEAR("Near (audience wall, x 57)", FieldConstants.NEAR_START),
    /** Against the red wall, tile A6, facing the back wall. UNTESTED (decision D21). */
    FAR("Far (red wall, tile A6)", FieldConstants.FAR_START),
    /** Inside the LOADING ZONE, front to the red wall. Position TODO: TUNE. */
    LOADING_ZONE("Loading zone (front to the red wall)", new Pose(AutoConstants.LOADING_ZONE_START_X,
            AutoConstants.LOADING_ZONE_START_Y, Math.toRadians(AutoConstants.LOADING_ZONE_START_HEADING_DEG)));

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
