// Sources: IronLion30311BIOBUZZInicioDeTemporada / branch feature/turrubiates/vision-align / TeamCode/.../subsystems/vision/HiveVision.java @ a7b8f4c (queries wrapped here)
//          IronLion30311BIOBUZZInicioDeTemporada / branch feature/turrubiates/vision-align / TeamCode/.../opmodes/auto/PedroAutoBase.java @ a7b8f4c (TIP check: sinceScoringCellSeen / hasSeenScoringCell -> isCellDown)
//          — adapted for BIOBUZZ (new facade for the Limelight)
package org.firstinspires.ftc.teamcode.subsystems;

import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.teamcode.hardware.RobotHardware;
import org.firstinspires.ftc.teamcode.subsystems.vision.HiveCell;
import org.firstinspires.ftc.teamcode.subsystems.vision.HiveClusterPose;
import org.firstinspires.ftc.teamcode.subsystems.vision.HiveStateTracker;
import org.firstinspires.ftc.teamcode.subsystems.vision.HiveVision;
import org.firstinspires.ftc.teamcode.util.Alliance;
import org.firstinspires.ftc.teamcode.util.MatchTimer;

import java.util.EnumMap;
import java.util.Map;

/**
 * The Limelight 3A on the rear: sees the AprilTags under the HIVE cells.
 * <p>
 * It tells you where each cell is ({@link #getPose(HiveCell)}), which of our two cells faces up
 * ({@link #getUpwardCell()}), how many TIPs happened ({@link #getTipCount()}), and whether a cell
 * has gone down ({@link #isCellDown(HiveCell, double)}). The heavy work is in
 * {@link HiveVision} and the helpers in {@code subsystems/vision}.
 * <p>
 * If the Limelight is missing, {@link #isConnected()} is false and every query says "nothing
 * seen" instead of stopping the OpMode.
 */
public class Vision implements Subsystem {

    private final HiveVision hiveVision;
    private final MatchTimer.Clock clock;
    private final Map<HiveCell, Double> lastSeenSec = new EnumMap<>(HiveCell.class);
    private Alliance alliance;

    /**
     * @param hardware robot hardware; the Limelight may be missing
     * @param alliance our alliance (picks which cells are ours)
     */
    public Vision(RobotHardware hardware, Alliance alliance) {
        this.hiveVision = new HiveVision(hardware.limelight);
        this.clock = MatchTimer.systemClock();
        this.alliance = alliance;
    }

    /** @param newAlliance our alliance (for example chosen in INIT) */
    public void setAlliance(Alliance newAlliance) {
        alliance = newAlliance;
    }

    /** @return our alliance */
    public Alliance getAlliance() {
        return alliance;
    }

    /** Reads the latest Limelight frame. Call once per loop. */
    @Override
    public void periodic() {
        hiveVision.update();
        double nowSec = clock.nowSeconds();
        for (HiveCell cell : HiveCell.values()) {
            if (hiveVision.isCellInView(cell)) {
                lastSeenSec.put(cell, nowSec);
            }
        }
    }

    /** Stops the Limelight polling. */
    @Override
    public void stop() {
        hiveVision.stop();
    }

    /** @return the cell pose closest to the camera crosshair, or null if none is visible */
    public HiveClusterPose getBestPose() {
        return hiveVision.getBestPose();
    }

    /**
     * @param cell a HIVE cell, or null for the best visible one
     * @return its pose, or null if none of its tags are visible
     */
    public HiveClusterPose getPose(HiveCell cell) {
        return hiveVision.getPose(cell);
    }

    /** @return the cell of ours to aim at now, or null if there is no valid target */
    public HiveCell getAllianceTarget() {
        return hiveVision.getAllianceTarget(alliance);
    }

    /** @return our confirmed upward cell (maybe old), or null if never confirmed */
    public HiveCell getUpwardCell() {
        return hiveVision.getUpwardCell(alliance);
    }

    /** @return how much is known about which of our cells faces up */
    public HiveStateTracker.State getHiveState() {
        return hiveVision.getHiveState(alliance);
    }

    /** @return TIPs seen on our HIVE since the OpMode started */
    public int getTipCount() {
        return hiveVision.getTipCount(alliance);
    }

    /**
     * @param cell a HIVE cell
     * @return true if at least one of its tags is in the latest frame
     */
    public boolean isCellInView(HiveCell cell) {
        return hiveVision.isCellInView(cell);
    }

    /**
     * @param cell a HIVE cell
     * @return true if its tags were seen at least once since the OpMode started
     */
    public boolean hasSeenCell(HiveCell cell) {
        return lastSeenSec.containsKey(cell);
    }

    /**
     * @param cell a HIVE cell
     * @return seconds since its tags were last seen, or infinity if never seen
     */
    public double getSecondsSinceSeen(HiveCell cell) {
        Double seen = lastSeenSec.get(cell);
        return seen == null ? Double.POSITIVE_INFINITY : clock.nowSeconds() - seen;
    }

    /**
     * TIP check from the tested AUTO: a cell counts as down (tipped) once its tags, seen
     * earlier, stay out of view for {@code goneSec}. Never-seen cells are NOT down (unknown).
     * For a watch that only counts time after it started, use {@code statemachine.TipWatch}.
     *
     * @param cell    the cell to check
     * @param goneSec how long its tags must be gone
     * @return true if the cell went down
     */
    public boolean isCellDown(HiveCell cell, double goneSec) {
        return hasSeenCell(cell) && getSecondsSinceSeen(cell) >= goneSec;
    }

    /** @return the HIVE tag IDs in the latest frame, space separated (for logs) */
    public String getVisibleTagIds() {
        return hiveVision.getVisibleTagIds();
    }

    /** @return mean camera-to-tag distance in the latest frame, in meters, or NaN */
    public double getRawTagDistanceMeters() {
        return hiveVision.getRawTagDistanceMeters();
    }

    /** @return the detailed HIVE vision (for tuning OpModes and loggers) */
    public HiveVision getHiveVision() {
        return hiveVision;
    }

    /** @return true if the Limelight is in the configuration */
    public boolean isConnected() {
        return hiveVision.isConnected();
    }

    @Override
    public void addTelemetry(Telemetry t) {
        if (!isConnected()) {
            t.addLine("Vision: Limelight not configured");
            return;
        }
        HiveCell upward = getUpwardCell();
        t.addData("HIVE", "up %s (%s), TIPs %d, tags [%s]",
                upward == null ? "?" : upward.getDisplayName(), getHiveState(), getTipCount(),
                getVisibleTagIds());
        HiveClusterPose best = getBestPose();
        if (best != null) {
            t.addData("HIVE pose", "%s fwd %.2f m, strafe %.2f m, yaw %.1f deg",
                    best.cell, best.forwardMeters, best.strafeMeters, best.yawDeg);
        }
    }
}
