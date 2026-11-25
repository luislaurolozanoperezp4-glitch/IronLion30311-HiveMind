// Sources: IronLion30311BIOBUZZInicioDeTemporada / branch feature/mar/duopodriveNcam / TeamCode/.../PinpointOdometry.java @ 2d92fba (saveCurrentPose idea)
//          — adapted for BIOBUZZ
package org.firstinspires.ftc.teamcode.util;

import com.pedropathing.math.Pose;

/**
 * Hands the robot's last AUTO pose to TeleOp.
 * <p>
 * Static fields survive between OpModes while the Robot Controller app keeps running, so AUTO
 * saves its final pose here and TeleOp reads it. Restarting the app clears it.
 */
public final class PoseStorage {

    private static Pose savedPose;
    private static Alliance savedAlliance;

    private PoseStorage() {
    }

    /**
     * Saves the pose and alliance at the end of AUTO.
     *
     * @param pose     the robot pose in field coordinates
     * @param alliance the alliance AUTO ran for
     */
    public static synchronized void save(Pose pose, Alliance alliance) {
        savedPose = pose;
        savedAlliance = alliance;
    }

    /** @return true if AUTO saved a pose */
    public static synchronized boolean hasPose() {
        return savedPose != null;
    }

    /** @return the saved pose, or null */
    public static synchronized Pose getPose() {
        return savedPose;
    }

    /** @return the saved alliance, or null */
    public static synchronized Alliance getAlliance() {
        return savedAlliance;
    }

    /** Forgets the saved pose, so a later TeleOp does not reuse an old one. */
    public static synchronized void clear() {
        savedPose = null;
        savedAlliance = null;
    }
}
