// Sources: IronLion30311BIOBUZZInicioDeTemporada / branch feature/mar/duopodriveNcam / TeamCode/.../PinpointOdometry.java @ 2d92fba (saveCurrentPose idea)
//          — adapted for BIOBUZZ (plus the G407 element-count hand-off)
package org.firstinspires.ftc.teamcode.util;

import com.pedropathing.math.Pose;

/**
 * Hands the robot's last AUTO pose, and the number of scoring elements it still holds (G407),
 * to TeleOp.
 * <p>
 * Static fields survive between OpModes while the Robot Controller app keeps running, so AUTO
 * saves its final pose here and TeleOp reads it. Restarting the app clears it.
 */
public final class PoseStorage {

    private static Pose savedPose;
    private static Alliance savedAlliance;
    /** Elements the robot held at the end of AUTO, or -1 if AUTO saved none. */
    private static int savedElementCount = -1;

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

    /**
     * Saves how many scoring elements the robot holds at the end of AUTO (G407 hand-off).
     *
     * @param count elements on board (estimate from {@code ElementCounter})
     */
    public static synchronized void saveElementCount(int count) {
        savedElementCount = Math.max(0, count);
    }

    /**
     * @param fallback value to use when AUTO saved no count (for example the preload, because a
     *                 robot that did not run AUTO still holds its pre-loaded POLLEN)
     * @return the saved element count, or {@code fallback}
     */
    public static synchronized int getElementCount(int fallback) {
        return savedElementCount >= 0 ? savedElementCount : fallback;
    }

    /** Forgets the saved pose and element count, so a later OpMode does not reuse old ones. */
    public static synchronized void clear() {
        savedPose = null;
        savedAlliance = null;
        savedElementCount = -1;
    }
}
