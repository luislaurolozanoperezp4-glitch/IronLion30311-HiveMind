// Sources: IronLion30311BIOBUZZInicioDeTemporada / branch feature/turrubiates/vision-align / TeamCode/.../subsystems/vision/HiveGeometry.java @ a7b8f4c (whole class) — adapted for BIOBUZZ
package org.firstinspires.ftc.teamcode.subsystems.vision;

import com.qualcomm.hardware.limelightvision.LLResultTypes;

import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;
import org.firstinspires.ftc.robotcore.external.navigation.Pose3D;
import org.firstinspires.ftc.robotcore.external.navigation.Position;
import org.firstinspires.ftc.robotcore.external.navigation.YawPitchRollAngles;
import org.firstinspires.ftc.teamcode.config.VisionConstants;

import java.util.ArrayList;
import java.util.List;

/**
 * Pure geometric utilities for projecting Limelight 3A AprilTag detections onto the horizontal
 * floor plane and reconstructing the exact 4-tag HIVE cluster center (index 1.5).
 * <p>
 * Supports both full 3D {@code solvePnP} poses and an automatic 2D trigonometric fallback
 * (using {@code tx}, {@code ty}, and {@code ta}) in case 3D localization is disabled in the
 * Limelight pipeline configuration.
 * <p>
 * The camera pitch and the fallback tag spacing live in {@link VisionConstants} so they can be
 * tuned live from FTC Dashboard.
 */
public final class HiveGeometry {

    /**
     * Approximate optical-axis distance (meters) of a 3.25-inch Biobuzz AprilTag when it occupies
     * 1.0% of the Limelight 3A image area ({@code ta = 1.0}). Used only when 3D solvePnP is off.
     */
    private static final double TAG_DISTANCE_AT_UNIT_AREA_METERS = 0.72;

    /** Minimum valid span (meters) between two tags to dynamically compute spacing and wall angle. */
    private static final double MIN_SPAN_METERS = 0.03;

    /** Maximum plausible center-to-center tag spacing (meters) to reject corrupted 3D outliers. */
    private static final double MAX_PLAUSIBLE_SPACING_METERS = 0.25;

    /** Minimum plausible center-to-center tag spacing (meters) to reject corrupted 3D outliers. */
    private static final double MIN_PLAUSIBLE_SPACING_METERS = 0.05;

    private HiveGeometry() {
        // Utility class
    }

    /**
     * Extracts the floor-projected pose of a single AprilTag from a Limelight fiducial detection.
     * <p>
     * Uses full 3D {@code solvePnP} camera-space coordinates when available; automatically falls back
     * to 2D bearing/elevation/area trigonometry if 3D localization is disabled in the pipeline.
     *
     * @param fiducial the detected Limelight fiducial
     * @return {@code [strafeX_meters, forwardZ_meters, yaw_degrees, is3D (1.0 or 0.0),
     *         roll_degrees, image_up_degrees, raw_yaw_degrees]} (the last three may be
     *         {@code NaN}), or {@code null} if invalid
     */
    public static double[] extractTagFloorPose(LLResultTypes.FiducialResult fiducial) {
        double pitchRad = Math.toRadians(VisionConstants.CAMERA_PITCH_DEGREES);
        double cosPitch = Math.cos(pitchRad);
        double sinPitch = Math.sin(pitchRad);

        Pose3D camPose = fiducial.getTargetPoseCameraSpace();
        if (camPose != null && camPose.getPosition() != null && camPose.getOrientation() != null) {
            Position pos = camPose.getPosition().toUnit(DistanceUnit.METER);
            YawPitchRollAngles ori = camPose.getOrientation();

            // Limelight Camera Space convention:
            // +X = right, +Y = down, +Z = forward along optical axis.
            double xc = pos.x;
            double yc = pos.y;
            double zc = pos.z;

            if (zc > 0.05) {
                // Project onto horizontal floor plane (camera tilted upward by CAMERA_PITCH_DEG):
                // Forward floor distance Z = zc * cos(pitch) + yc * sin(pitch)
                double forwardFloorMeters = zc * cosPitch + yc * sinPitch;
                if (forwardFloorMeters > 0.05) {
                    // Project camera-space yaw onto the floor plane
                    double rawTagYawDeg = ori.getYaw(AngleUnit.DEGREES) * cosPitch;
                    return new double[]{xc, forwardFloorMeters, rawTagYawDeg, 1.0,
                            HiveOrientation.wrapDeg(ori.getRoll(AngleUnit.DEGREES)),
                            extractImageUpDeg(fiducial),
                            HiveOrientation.wrapDeg(ori.getYaw(AngleUnit.DEGREES))};
                }
            }
        }

        // 2D fallback when "Full 3D" is not enabled in the Limelight pipeline:
        // Estimate optical depth from tag target area (ta) and project using tx and ty.
        double ta = fiducial.getTargetArea();
        if (ta <= 0.01) {
            return null;
        }

        double opticalDistanceMeters = TAG_DISTANCE_AT_UNIT_AREA_METERS / Math.sqrt(ta);
        double txRad = Math.toRadians(fiducial.getTargetXDegrees());
        double tyRad = Math.toRadians(fiducial.getTargetYDegrees());

        // Elevation angle above horizontal floor plane = camera pitch + vertical offset ty
        double elevationRad = pitchRad + tyRad;
        double forwardFloorMeters = opticalDistanceMeters * Math.cos(elevationRad);
        double strafeFloorMeters = opticalDistanceMeters * Math.sin(txRad);

        if (forwardFloorMeters <= 0.05) {
            return null;
        }

        return new double[]{strafeFloorMeters, forwardFloorMeters, 0.0, 0.0,
                Double.NaN, extractImageUpDeg(fiducial), Double.NaN};
    }

    /**
     * In-image orientation of a tag: angle of the vector from corner 0 to corner 3, which is the
     * tag's "up" direction when corners follow the AprilTag order (bottom-left, bottom-right,
     * top-right, top-left). It flips by ~180 degrees when the tag is upside down, and it does not
     * need 3D solvePnP.
     *
     * @param fiducial the detected Limelight fiducial
     * @return angle in degrees in [-180, 180), or {@code NaN} if 4 corners are not available
     */
    public static double extractImageUpDeg(LLResultTypes.FiducialResult fiducial) {
        List<List<Double>> corners = fiducial.getTargetCorners();
        if (corners == null || corners.size() < 4) {
            return Double.NaN;
        }
        List<Double> from = corners.get(0);
        List<Double> to = corners.get(3);
        if (from == null || to == null || from.size() < 2 || to.size() < 2) {
            return Double.NaN;
        }
        return HiveOrientation.cornerVectorAngleDeg(from.get(0), from.get(1), to.get(0), to.get(1));
    }

    /**
     * Reconstructs the unsmoothed center pose of the 4-tag line for a single {@link HiveCell},
     * even when only 1, 2, or 3 of its 4 AprilTags are visible.
     *
     * @param cell    the HIVE cell to evaluate
     * @param tagById array indexed by AprilTag ID containing {@link #extractTagFloorPose} results
     * @return raw {@link HiveClusterPose} for the 4-tag center (index 1.5), or {@code null} if no tags of this cell are visible
     */
    public static HiveClusterPose reconstructRawCellPose(HiveCell cell, double[][] tagById) {
        int[] ids = cell.getTagIds();
        List<Integer> visibleIndices = new ArrayList<>(4);

        for (int idx = 0; idx < ids.length; idx++) {
            if (tagById[ids[idx]] != null) {
                visibleIndices.add(idx);
            }
        }

        if (visibleIndices.isEmpty()) {
            return null;
        }

        double wallYawRad;
        double spacingMeters = VisionConstants.TAG_SPACING_METERS;
        boolean using3D = true;

        if (visibleIndices.size() >= 2) {
            int leftIdx = visibleIndices.get(0);
            int rightIdx = visibleIndices.get(visibleIndices.size() - 1);

            double[] pL = tagById[ids[leftIdx]];
            double[] pR = tagById[ids[rightIdx]];

            // Vector along the tag line from leftmost visible tag to rightmost visible tag in floor coordinates (X, Z)
            double dx = pR[0] - pL[0];
            double dz = pR[1] - pL[1];
            double spanMeters = Math.hypot(dx, dz);

            if (spanMeters > MIN_SPAN_METERS) {
                wallYawRad = Math.atan2(dz, dx);
                double measuredSpacing = spanMeters / (rightIdx - leftIdx);
                if (measuredSpacing >= MIN_PLAUSIBLE_SPACING_METERS
                        && measuredSpacing <= MAX_PLAUSIBLE_SPACING_METERS) {
                    spacingMeters = measuredSpacing;
                }
            } else {
                wallYawRad = 0.0;
            }
        } else {
            int singleIdx = visibleIndices.get(0);
            double[] p = tagById[ids[singleIdx]];
            wallYawRad = Math.toRadians(p[2]);
        }

        // Extrapolate the true center of the 4-tag line (index 1.5, midway between indices 1 and 2)
        double sumStrafeX = 0.0;
        double sumForwardZ = 0.0;
        double centerIndex = (ids.length - 1) / 2.0; // 1.5 for 4 tags (indices 0, 1, 2, 3)

        for (int idx : visibleIndices) {
            double[] p = tagById[ids[idx]];
            double offsetFromCenter = (idx - centerIndex) * spacingMeters;

            double centerX = p[0] - offsetFromCenter * Math.cos(wallYawRad);
            double centerZ = p[1] - offsetFromCenter * Math.sin(wallYawRad);

            sumStrafeX += centerX;
            sumForwardZ += centerZ;
            if (p[3] < 0.5) {
                using3D = false;
            }
        }

        int count = visibleIndices.size();
        double rawStrafeX = sumStrafeX / count;
        double rawForwardZ = sumForwardZ / count;
        double rawYawDeg = Math.toDegrees(wallYawRad);

        // Orientation is bimodal (upright vs upside down): combine with a circular mean.
        double[] rollsDeg = new double[count];
        double[] imageUpsDeg = new double[count];
        double[] rawYawsDeg = new double[count];
        for (int i = 0; i < count; i++) {
            double[] p = tagById[ids[visibleIndices.get(i)]];
            rollsDeg[i] = p.length > 4 ? p[4] : Double.NaN;
            imageUpsDeg[i] = p.length > 5 ? p[5] : Double.NaN;
            rawYawsDeg[i] = p.length > 6 ? p[6] : Double.NaN;
        }

        return new HiveClusterPose(cell, rawForwardZ, rawStrafeX, rawYawDeg, count, using3D,
                HiveOrientation.circularMeanDeg(rollsDeg),
                HiveOrientation.circularMeanDeg(imageUpsDeg),
                HiveOrientation.circularMeanDeg(rawYawsDeg));
    }
}
