// Sources: IronLion30311BIOBUZZInicioDeTemporada / branch feature/turrubiates/vision-align / TeamCode/.../subsystems/vision/HiveStateTracker.java @ a7b8f4c (whole class) — adapted for BIOBUZZ
package org.firstinspires.ftc.teamcode.subsystems.vision;

import org.firstinspires.ftc.teamcode.util.Alliance;

/**
 * Tracks which of one alliance's two HIVE cells faces up (is scorable), with debouncing, and
 * counts TIPs (every TIP swaps the upward cell).
 *
 * <p>Each update receives the per-frame verdict for the alliance's two cells: {@code TRUE}
 * (upright cluster), {@code FALSE} (upside down) or {@code null} (not visible / unknown).
 * Exactly one cell faces up, so a single verdict is enough: an upright cell is the upward one,
 * and an upside-down cell means the other one is up. A frame where both verdicts agree (both
 * upright or both upside down) is contradictory and breaks the current streak.</p>
 *
 * <p>A candidate becomes the confirmed upward cell only after it has been seen consistently for
 * at least {@link #CONFIRM_UPDATES} updates spanning {@link #CONFIRM_MS}, so it does not depend on
 * the loop rate. The confirmed cell is kept while the HIVE is out of view, but the state turns
 * {@link State#UPWARD_STALE} after {@link #STALE_AFTER_MS} without a consistent observation
 * (for example, the alliance partner may have tipped it meanwhile).</p>
 *
 * <p>A streak is dropped if no observation arrives for {@link #STREAK_GAP_MS}, and refreshing a
 * stale confirmation also needs {@link #CONFIRM_UPDATES} consistent updates, so isolated glitch
 * frames cannot confirm, refresh or tip anything.</p>
 *
 * <p>Limitation: two TIPs while the HIVE is out of view look like no change.</p>
 *
 * <p>Pure logic with injected time, so it can be unit tested on the computer.</p>
 */
public class HiveStateTracker {

    /** How much the tracker knows about the upward cell. */
    public enum State {
        /** No upward cell confirmed yet. */
        UNKNOWN,
        /** Upward cell confirmed and observed recently. */
        UPWARD_KNOWN,
        /** Upward cell confirmed earlier, but not observed for a while. */
        UPWARD_STALE
    }

    /** Minimum number of consistent updates to confirm or change the upward cell. */
    static final int CONFIRM_UPDATES = 3;

    /** Minimum time a candidate must stay consistent to be confirmed, in milliseconds. */
    static final long CONFIRM_MS = 150;

    /** Time without a consistent observation after which the state becomes stale, in ms. */
    static final long STALE_AFTER_MS = 1500;

    /** Gap without any observation after which the current streak is dropped, in ms. */
    static final long STREAK_GAP_MS = 300;

    private final HiveCell firstCell;
    private final HiveCell secondCell;

    private HiveCell confirmedCell = null;
    private long lastConsistentMs = 0;
    private int tipCount = 0;

    private HiveCell streakCell = null;
    private int streakUpdates = 0;
    private long streakStartMs = 0;
    private long lastObservationMs = 0;

    /**
     * @param alliance the alliance whose two HIVE cells are tracked
     */
    public HiveStateTracker(Alliance alliance) {
        this.firstCell = HiveCell.getBackCell(alliance);
        this.secondCell = HiveCell.getAudienceCell(alliance);
    }

    /**
     * Feeds one frame of verdicts.
     *
     * @param scoringCellUpright  verdict for the alliance's scoring-side cell, or {@code null}
     * @param audienceCellUpright verdict for the alliance's audience-side cell, or {@code null}
     * @param nowMs               current monotonic time in milliseconds
     */
    public void update(Boolean scoringCellUpright, Boolean audienceCellUpright, long nowMs) {
        HiveCell candidate;
        if (scoringCellUpright == null && audienceCellUpright == null) {
            return; // Nothing observed: keep the streak, it only counts real observations.
        }
        if (streakCell != null && nowMs - lastObservationMs > STREAK_GAP_MS) {
            resetStreak(); // Too long without observations: start over.
        }
        lastObservationMs = nowMs;
        if (scoringCellUpright != null && audienceCellUpright != null
                && scoringCellUpright.booleanValue() == audienceCellUpright.booleanValue()) {
            resetStreak(); // Contradictory: both up or both down.
            return;
        } else if (scoringCellUpright != null) {
            candidate = scoringCellUpright ? firstCell : secondCell;
        } else {
            candidate = audienceCellUpright ? secondCell : firstCell;
        }

        if (candidate == streakCell) {
            streakUpdates++;
        } else {
            streakCell = candidate;
            streakUpdates = 1;
            streakStartMs = nowMs;
        }

        boolean isStreakConfirmed = streakUpdates >= CONFIRM_UPDATES
                && nowMs - streakStartMs >= CONFIRM_MS;

        if (candidate == confirmedCell) {
            // While fresh, every consistent frame keeps it fresh; a stale one needs a full streak.
            if (getState(nowMs) == State.UPWARD_KNOWN || isStreakConfirmed) {
                lastConsistentMs = nowMs;
            }
            return;
        }

        if (isStreakConfirmed) {
            if (confirmedCell != null) {
                tipCount++;
            }
            confirmedCell = candidate;
            lastConsistentMs = nowMs;
        }
    }

    /**
     * Picks the cell to aim at: the confirmed upward cell while it is fresh and the current frame
     * does not show it upside down; otherwise the alliance cell that is the only upright one in the
     * current frame. Never returns a cell that the current frame shows upside down.
     *
     * @param scoringCellUpright  current-frame verdict for the scoring-side cell, or {@code null}
     * @param audienceCellUpright current-frame verdict for the audience-side cell, or {@code null}
     * @param nowMs               current monotonic time in milliseconds
     * @return the cell to aim at, or {@code null} if there is no valid target right now
     */
    public HiveCell selectTarget(Boolean scoringCellUpright, Boolean audienceCellUpright,
                                 long nowMs) {
        if (getState(nowMs) == State.UPWARD_KNOWN) {
            Boolean confirmedVerdict =
                    confirmedCell == firstCell ? scoringCellUpright : audienceCellUpright;
            return Boolean.FALSE.equals(confirmedVerdict) ? null : confirmedCell;
        }
        boolean isScoringUp = Boolean.TRUE.equals(scoringCellUpright);
        boolean isAudienceUp = Boolean.TRUE.equals(audienceCellUpright);
        if (isScoringUp && !isAudienceUp) {
            return firstCell;
        }
        if (isAudienceUp && !isScoringUp) {
            return secondCell;
        }
        return null; // None upright, or both (contradictory).
    }

    /** @return the confirmed upward cell (possibly stale), or {@code null} if never confirmed */
    public HiveCell getUpwardCell() {
        return confirmedCell;
    }

    /**
     * @param nowMs current monotonic time in milliseconds
     * @return how much the tracker currently knows about the upward cell
     */
    public State getState(long nowMs) {
        if (confirmedCell == null) {
            return State.UNKNOWN;
        }
        return nowMs - lastConsistentMs > STALE_AFTER_MS ? State.UPWARD_STALE : State.UPWARD_KNOWN;
    }

    /** @return number of times the confirmed upward cell changed (TIPs seen) */
    public int getTipCount() {
        return tipCount;
    }

    /** Forgets everything (for example at the start of an OpMode). */
    public void reset() {
        confirmedCell = null;
        lastConsistentMs = 0;
        tipCount = 0;
        resetStreak();
    }

    private void resetStreak() {
        streakCell = null;
        streakUpdates = 0;
        streakStartMs = 0;
        lastObservationMs = 0;
    }
}
