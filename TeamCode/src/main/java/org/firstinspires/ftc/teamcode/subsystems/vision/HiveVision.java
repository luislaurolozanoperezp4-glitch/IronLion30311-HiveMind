// Sources: IronLion30311BIOBUZZInicioDeTemporada / branch feature/turrubiates/vision-align / TeamCode/.../subsystems/vision/HiveVision.java @ a7b8f4c (whole class; isCellInView from PedroAutoBase.isCellInView) — adapted for BIOBUZZ
package org.firstinspires.ftc.teamcode.subsystems.vision;

import com.qualcomm.hardware.limelightvision.LLResult;
import com.qualcomm.hardware.limelightvision.LLResultTypes;
import com.qualcomm.hardware.limelightvision.Limelight3A;
import com.qualcomm.robotcore.util.ElapsedTime;

import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;
import org.firstinspires.ftc.robotcore.external.navigation.Pose3D;
import org.firstinspires.ftc.robotcore.external.navigation.Position;
import org.firstinspires.ftc.teamcode.config.VisionConstants;
import org.firstinspires.ftc.teamcode.util.Alliance;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

/**
 * Detects the BIOBUZZ HIVE AprilTag clusters using Limelight 3A.
 * <p>
 * Polls the Limelight 3A on pipeline 0 (AprilTag 36h11), delegates floor-plane projection and
 * 4-tag cluster center reconstruction to {@link HiveGeometry}, applies Exponential Moving Average
 * (EMA) filtering with brief dropout holdover, and selects the HIVE cell closest to the camera's
 * horizontal crosshair.
 * <p>
 * If the {@code limelight} is missing from the Driver Station configuration, every query
 * reports "no target" ({@link #isConnected()} is false) instead of stopping the OpMode.
 */
public class HiveVision {


    private final Limelight3A limelight;
    private final Map<Alliance, HiveStateTracker> stateTrackers = new EnumMap<>(Alliance.class);
    private final ElapsedTime clock = new ElapsedTime();
    private final Map<HiveCell, double[]> smoothedCellStates = new EnumMap<>(HiveCell.class);
    private final Map<HiveCell, HiveClusterPose> latestCellPoses = new EnumMap<>(HiveCell.class);
    private final ElapsedTime lastValidDetectionTimer = new ElapsedTime();
    private final EnumSet<HiveCell> cellsInView = EnumSet.noneOf(HiveCell.class);

    private HiveClusterPose bestPose = null;
    /** HIVE tag IDs in the latest frame, space separated (empty when none); for logs. */
    private String visibleTagIds = "";
    /** Mean straight-line camera-to-tag distance of the HIVE tags in the latest frame. */
    private double rawTagDistanceMeters = Double.NaN;
    /**
     * Raw Limelight values of the first HIVE tag in the latest frame, for diagnosing the pose:
     * {tx deg, ty deg, ta %, camera-space x, y, z m}; NaN when no HIVE tag is in view.
     */
    private final double[] firstTagRaw = new double[6];
    private double lastTrackedFrameTimestamp = Double.NaN;
    private boolean hasValidHistory = false;

    /**
     * Initializes and starts the Limelight 3A vision subsystem.
     *
     * @param limelight the Limelight 3A, or {@code null} if it is not in the configuration
     */
    public HiveVision(Limelight3A limelight) {
        this.limelight = limelight;
        if (limelight != null) {
            limelight.setPollRateHz(VisionConstants.LIMELIGHT_POLL_RATE_HZ);
            limelight.pipelineSwitch(VisionConstants.LIMELIGHT_PIPELINE);
            limelight.start();
        }
        for (Alliance alliance : Alliance.values()) {
            stateTrackers.put(alliance, new HiveStateTracker(alliance));
        }
    }

    /**
     * Polls the latest Limelight frame, reconstructs the 4-tag cluster center for every visible
     * HIVE cell, updates EMA filters, and selects the best visible cell (prioritizing the cell
     * most centered in the camera crosshair).
     * Call once per OpMode loop.
     */
    public void update() {
        if (limelight == null) {
            return;
        }
        LLResult result = limelight.getLatestResult();
        visibleTagIds = "";
        cellsInView.clear();
        rawTagDistanceMeters = Double.NaN;
        Arrays.fill(firstTagRaw, Double.NaN);
        if (result == null || !result.isValid() || result.getStaleness() > VisionConstants.TAG_STALE_MILLIS) {
            handleMissingDetection();
            return;
        }

        List<LLResultTypes.FiducialResult> fiducials = result.getFiducialResults();
        if (fiducials == null || fiducials.isEmpty()) {
            handleMissingDetection();
            return;
        }

        // Parse individual tag poses projected onto the horizontal floor frame:
        // tagById[id] = {strafeX, forwardZ, yawDeg, is3D, rollDeg, imageUpDeg, rawYawDeg}
        double[][] tagById = new double[HiveCell.MAX_TAG_ID + 1][];
        boolean hasHiveTag = false;
        StringBuilder tagIds = new StringBuilder();
        double distanceSumMeters = 0.0;
        int distanceCount = 0;

        for (LLResultTypes.FiducialResult fiducial : fiducials) {
            int id = fiducial.getFiducialId();
            if (id < HiveCell.MIN_TAG_ID || id > HiveCell.MAX_TAG_ID) {
                continue;
            }
            if (tagIds.length() == 0) {
                recordRaw(fiducial);
            }
            tagIds.append(tagIds.length() == 0 ? "" : " ").append(id);
            HiveCell seenCell = HiveCell.fromTagId(id);
            if (seenCell != null) {
                cellsInView.add(seenCell);
            }
            double distanceMeters = rawDistanceMeters(fiducial);
            if (!Double.isNaN(distanceMeters)) {
                distanceSumMeters += distanceMeters;
                distanceCount++;
            }
            double[] floorPose = HiveGeometry.extractTagFloorPose(fiducial);
            if (floorPose != null) {
                tagById[id] = floorPose;
                hasHiveTag = true;
            }
        }

        visibleTagIds = tagIds.toString();
        if (distanceCount > 0) {
            rawTagDistanceMeters = distanceSumMeters / distanceCount;
        }

        if (!hasHiveTag) {
            handleMissingDetection();
            return;
        }

        latestCellPoses.clear();
        bestPose = null;

        for (HiveCell cell : HiveCell.values()) {
            HiveClusterPose rawPose = HiveGeometry.reconstructRawCellPose(cell, tagById);
            if (rawPose != null) {
                HiveClusterPose smoothedPose = applyEmaFilter(rawPose);
                latestCellPoses.put(cell, smoothedPose);
                if (isBetterTarget(smoothedPose, bestPose)) {
                    bestPose = smoothedPose;
                }
            } else {
                smoothedCellStates.remove(cell);
            }
        }

        if (bestPose != null) {
            lastValidDetectionTimer.reset();
            hasValidHistory = true;
        }

        // Feed the HIVE state trackers once per new camera frame (getLatestResult() returns the
        // same frame until a new one arrives) and never with held-over poses.
        if (result.getTimestamp() == lastTrackedFrameTimestamp) {
            return;
        }
        lastTrackedFrameTimestamp = result.getTimestamp();
        long nowMs = (long) clock.milliseconds();
        for (Map.Entry<Alliance, HiveStateTracker> entry : stateTrackers.entrySet()) {
            Alliance alliance = entry.getKey();
            entry.getValue().update(
                    classifyUpright(latestCellPoses.get(HiveCell.getScoringCell(alliance))),
                    classifyUpright(latestCellPoses.get(HiveCell.getAudienceCell(alliance))),
                    nowMs);
        }
    }

    /**
     * Picks the cell to aim at for an alliance (see {@link HiveStateTracker#selectTarget}). Never
     * returns a cell of the other alliance or one that the current frame shows upside down.
     *
     * @param alliance our alliance
     * @return the cell to aim at, or {@code null} if there is no valid target right now
     */
    public HiveCell getAllianceTarget(Alliance alliance) {
        return stateTrackers.get(alliance).selectTarget(
                classifyUpright(latestCellPoses.get(HiveCell.getScoringCell(alliance))),
                classifyUpright(latestCellPoses.get(HiveCell.getAudienceCell(alliance))),
                nowMs());
    }

    /**
     * Upright verdict of one cell in the latest frame, for logging (the HIVE state trackers use
     * the same verdict).
     *
     * @param cell HIVE cell
     * @return {@code TRUE} upright, {@code FALSE} upside down, {@code null} if not visible
     */
    public Boolean getUprightVerdict(HiveCell cell) {
        return classifyUpright(latestCellPoses.get(cell));
    }

    /**
     * @param alliance our alliance
     * @return the confirmed upward cell (possibly stale), or {@code null} if never confirmed
     */
    public HiveCell getUpwardCell(Alliance alliance) {
        return stateTrackers.get(alliance).getUpwardCell();
    }

    /**
     * @param alliance our alliance
     * @return how much is known about which of the alliance's cells faces up
     */
    public HiveStateTracker.State getHiveState(Alliance alliance) {
        return stateTrackers.get(alliance).getState(nowMs());
    }

    /**
     * @param alliance our alliance
     * @return number of TIPs seen on the alliance's HIVE since this OpMode started
     */
    public int getTipCount(Alliance alliance) {
        return stateTrackers.get(alliance).getTipCount();
    }

    /**
     * Upright verdict for one cluster using the signal selected in
     * {@link HiveOrientation#UPRIGHT_SIGNAL}.
     *
     * @return {@code TRUE} upright, {@code FALSE} upside down, {@code null} if unknown
     */
    private static Boolean classifyUpright(HiveClusterPose pose) {
        if (pose == null) {
            return null;
        }
        double angleDeg;
        double referenceDeg;
        switch (HiveOrientation.UPRIGHT_SIGNAL) {
            case ROLL:
                angleDeg = pose.rollDeg;
                referenceDeg = HiveOrientation.UPRIGHT_ROLL_REFERENCE_DEG;
                break;
            case RAW_YAW:
                angleDeg = pose.rawYawDeg;
                referenceDeg = HiveOrientation.UPRIGHT_RAW_YAW_REFERENCE_DEG;
                break;
            case IMAGE_UP:
            default:
                angleDeg = pose.imageUpDeg;
                referenceDeg = HiveOrientation.UPRIGHT_IMAGE_UP_REFERENCE_DEG;
                break;
        }
        if (Double.isNaN(angleDeg)) {
            return null;
        }
        return HiveOrientation.isUpright(angleDeg, referenceDeg);
    }

    private long nowMs() {
        return (long) clock.milliseconds();
    }

    /**
     * Returns the best detected HIVE cell pose (prioritizing the cell most centered on the
     * camera crosshair), or {@code null} if no HIVE tags are currently visible.
     */
    public HiveClusterPose getBestPose() {
        return bestPose;
    }

    /**
     * Returns the detected pose for a specific {@link HiveCell} (useful in Autonomous),
     * or {@code null} if none of that cell's 4 tags are currently visible.
     *
     * @param targetCell the specific HIVE cell to query, or {@code null} for any best cell
     */
    public HiveClusterPose getPose(HiveCell targetCell) {
        if (targetCell == null) {
            return bestPose;
        }
        return latestCellPoses.get(targetCell);
    }

    /**
     * @return HIVE tag IDs in the latest Limelight frame, space separated, or an empty string
     */
    public String getVisibleTagIds() {
        return visibleTagIds;
    }

    /**
     * Unfiltered distance reference for shooting tests: the mean straight-line distance from
     * the camera to the HIVE tags in the latest frame (Limelight 3D pose, no EMA, no floor
     * projection).
     *
     * @return distance in meters, or {@code NaN} if no tag has a 3D pose this frame
     */
    public double getRawTagDistanceMeters() {
        return rawTagDistanceMeters;
    }

    /**
     * @return raw values of the first HIVE tag in the latest frame: {tx deg, ty deg, ta %,
     *         camera-space x, y, z m} (NaN when unavailable); do not modify
     */
    public double[] getFirstTagRaw() {
        return firstTagRaw;
    }

    private void recordRaw(LLResultTypes.FiducialResult fiducial) {
        firstTagRaw[0] = fiducial.getTargetXDegrees();
        firstTagRaw[1] = fiducial.getTargetYDegrees();
        firstTagRaw[2] = fiducial.getTargetArea();
        Pose3D camPose = fiducial.getTargetPoseCameraSpace();
        if (camPose != null && camPose.getPosition() != null) {
            Position pos = camPose.getPosition().toUnit(DistanceUnit.METER);
            firstTagRaw[3] = pos.x;
            firstTagRaw[4] = pos.y;
            firstTagRaw[5] = pos.z;
        }
    }

    /** Camera-to-tag straight-line distance from the tag's 3D pose, or NaN without one. */
    private static double rawDistanceMeters(LLResultTypes.FiducialResult fiducial) {
        Pose3D camPose = fiducial.getTargetPoseCameraSpace();
        if (camPose == null || camPose.getPosition() == null) {
            return Double.NaN;
        }
        Position pos = camPose.getPosition().toUnit(DistanceUnit.METER);
        double distance = Math.sqrt(pos.x * pos.x + pos.y * pos.y + pos.z * pos.z);
        return distance > 0.0 ? distance : Double.NaN;
    }

    /**
     * @param cell a HIVE cell
     * @return true if at least one tag of this cell is in the latest Limelight frame
     */
    public boolean isCellInView(HiveCell cell) {
        return cellsInView.contains(cell);
    }

    /** Stops the Limelight polling thread. Call at the end of the OpMode. */
    public void stop() {
        if (limelight != null) {
            limelight.stop();
        }
    }

    /** @return {@code true} if the Limelight is in the Driver Station configuration */
    public boolean isConnected() {
        return limelight != null;
    }

    /**
     * Retains the last known poses briefly during single-frame dropouts before clearing state.
     */
    private void handleMissingDetection() {
        if (!hasValidHistory || lastValidDetectionTimer.seconds() > VisionConstants.TAG_DROPOUT_HOLD_SECONDS) {
            latestCellPoses.clear();
            smoothedCellStates.clear();
            bestPose = null;
            hasValidHistory = false;
        }
    }

    /**
     * Applies Exponential Moving Average (EMA) smoothing to suppress single-frame solvePnP noise.
     */
    private HiveClusterPose applyEmaFilter(HiveClusterPose raw) {
        double[] smoothed = smoothedCellStates.get(raw.cell);
        if (smoothed == null) {
            smoothed = new double[]{raw.strafeMeters, raw.forwardMeters, raw.yawDeg};
            smoothedCellStates.put(raw.cell, smoothed);
        } else {
            smoothed[0] += VisionConstants.TAG_SMOOTHING * (raw.strafeMeters - smoothed[0]);
            smoothed[1] += VisionConstants.TAG_SMOOTHING * (raw.forwardMeters - smoothed[1]);
            smoothed[2] += VisionConstants.TAG_SMOOTHING * (raw.yawDeg - smoothed[2]);
        }

        return new HiveClusterPose(
                raw.cell,
                smoothed[1],
                smoothed[0],
                smoothed[2],
                raw.visibleTagCount,
                raw.isFrom3dSolvePnp,
                // Orientation is bimodal (upright vs upside down): copied, never smoothed.
                raw.rollDeg,
                raw.imageUpDeg,
                raw.rawYawDeg
        );
    }

    /**
     * Prioritizes the HIVE cell whose center is horizontally closest to the camera crosshair
     * ({@code |strafeMeters|}).
     */
    private static boolean isBetterTarget(HiveClusterPose candidate, HiveClusterPose currentBest) {
        if (currentBest == null) {
            return true;
        }
        return Math.abs(candidate.strafeMeters) < Math.abs(currentBest.strafeMeters);
    }
}
