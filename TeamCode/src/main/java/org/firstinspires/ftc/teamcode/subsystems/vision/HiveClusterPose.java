// Sources: IronLion30311BIOBUZZInicioDeTemporada / branch feature/turrubiates/vision-align / TeamCode/.../subsystems/vision/HiveClusterPose.java @ a7b8f4c (whole class) — adapted for BIOBUZZ
package org.firstinspires.ftc.teamcode.subsystems.vision;

/**
 * Immutable floor-projected pose of a 4-AprilTag HIVE cell relative to the rear-facing camera frame.
 * <ul>
 *     <li>{@link #forwardMeters}: horizontal floor distance in front of the camera (+ = farther from HIVE).</li>
 *     <li>{@link #strafeMeters}: lateral offset of the 4-tag center in camera view (+ = target is to the right of crosshair).</li>
 *     <li>{@link #yawDeg}: wall angle relative to the camera (+ = right side of cell in camera view is farther than left side).</li>
 *     <li>{@link #visibleTagCount}: how many of the 4 cluster tags were detected in the frame (1..4).</li>
 *     <li>{@link #isFrom3dSolvePnp}: {@code true} if derived from full 3D solvePnP, {@code false} if using 2D trig fallback.</li>
 *     <li>{@link #rollDeg}: circular mean of the visible tags' 3D camera-space roll, in degrees
 *     ({@code NaN} in 2D mode).</li>
 *     <li>{@link #imageUpDeg}: circular mean of the visible tags' in-image corner orientation, in
 *     degrees ({@code NaN} if the Limelight reported no corners).</li>
 *     <li>{@link #rawYawDeg}: circular mean of the visible tags' raw 3D camera-space yaw, in
 *     degrees ({@code NaN} in 2D mode). Kept because the Limelight angle order is not verified:
 *     the 180 degree in-plane flip may show up in yaw instead of roll.</li>
 * </ul>
 * The two orientation fields tell an upright cluster (upward CELL) from an upside-down one; see
 * {@link HiveOrientation}. They are bimodal, so they are never EMA-filtered.
 */
public class HiveClusterPose {

    public final HiveCell cell;
    public final double forwardMeters;
    public final double strafeMeters;
    public final double yawDeg;
    public final int visibleTagCount;
    public final boolean isFrom3dSolvePnp;
    public final double rollDeg;
    public final double imageUpDeg;
    public final double rawYawDeg;

    /**
     * Creates a reconstructed HIVE cluster pose.
     *
     * @param cell            the identified HIVE cell
     * @param forwardMeters   horizontal floor distance from camera to the 4-tag center (meters)
     * @param strafeMeters    lateral offset of the 4-tag center relative to camera crosshair (meters)
     * @param yawDeg      wall yaw angle relative to camera optical plane (degrees)
     * @param visibleTagCount number of detected tags belonging to this cell (1..4)
     * @param isFrom3dSolvePnp true if 3D pose was available from Limelight, false if 2D fallback was used
     */
    public HiveClusterPose(HiveCell cell, double forwardMeters, double strafeMeters,
                           double yawDeg, int visibleTagCount, boolean isFrom3dSolvePnp) {
        this(cell, forwardMeters, strafeMeters, yawDeg, visibleTagCount, isFrom3dSolvePnp,
                Double.NaN, Double.NaN, Double.NaN);
    }

    /**
     * Creates a reconstructed HIVE cluster pose including the cluster orientation.
     *
     * @param cell            the identified HIVE cell
     * @param forwardMeters   horizontal floor distance from camera to the 4-tag center (meters)
     * @param strafeMeters    lateral offset of the 4-tag center from the crosshair (meters)
     * @param yawDeg      wall yaw angle relative to camera optical plane (degrees)
     * @param visibleTagCount number of detected tags belonging to this cell (1..4)
     * @param isFrom3dSolvePnp true if the Limelight gave a 3D pose, false for the 2D fallback
     * @param rollDeg         cluster 3D roll in degrees, or {@code NaN} if unknown
     * @param imageUpDeg      cluster in-image orientation in degrees, or {@code NaN} if unknown
     * @param rawYawDeg       cluster raw 3D camera-space yaw in degrees, or {@code NaN} if unknown
     */
    public HiveClusterPose(HiveCell cell, double forwardMeters, double strafeMeters,
                           double yawDeg, int visibleTagCount, boolean isFrom3dSolvePnp,
                           double rollDeg, double imageUpDeg, double rawYawDeg) {
        this.cell = cell;
        this.forwardMeters = forwardMeters;
        this.strafeMeters = strafeMeters;
        this.yawDeg = yawDeg;
        this.visibleTagCount = visibleTagCount;
        this.isFrom3dSolvePnp = isFrom3dSolvePnp;
        this.rollDeg = rollDeg;
        this.imageUpDeg = imageUpDeg;
        this.rawYawDeg = rawYawDeg;
    }
}
