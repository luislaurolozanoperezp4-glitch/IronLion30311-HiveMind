// Sources: IronLion30311BIOBUZZInicioDeTemporada / branch feature/turrubiates/vision-align / TeamCode/.../opmodes/auto/PedroAutoBase.java @ a7b8f4c (watchForTip, hasSeenScoringCell / sinceScoringCellSeen)
//          IronLion30311BIOBUZZInicioDeTemporada / branch feature/turrubiates/vision-align / TeamCode/.../opmodes/auto/PreloadRouteAuto.java @ a7b8f4c (early TIP watch, commits c1c59db, a7b8f4c)
//          — adapted for BIOBUZZ (blocking loop -> non-blocking)
package org.firstinspires.ftc.teamcode.statemachine;

import org.firstinspires.ftc.teamcode.subsystems.Vision;
import org.firstinspires.ftc.teamcode.subsystems.vision.HiveCell;
import org.firstinspires.ftc.teamcode.util.MatchTimer;

/**
 * Watches one HIVE cell for a TIP (the cell going down), as in the tested AUTO.
 * <p>
 * The cell counts as <b>down</b> once its tags, seen earlier in the match, stay out of view for
 * {@code goneSec}, counted from the start of the watch. Time without tags BEFORE the watch does
 * not count: during a volley the flywheel shakes the Limelight and it can lose the cell for the
 * whole volley, which once gave a false "down" (2026-11-24 16:56 run).
 * <p>
 * Results: {@link #isCellDown()} = TIP seen; {@link #isCellStillUp()} = watch over and the
 * cell was in view; {@link #isUnknown()} = the cell was never seen, so we cannot tell.
 * <p>
 * Use: {@link #start(HiveCell, double, double)}, then {@link #periodic()} every loop (after
 * {@code Vision.periodic()}) until {@link #isDone()}.
 */
public class TipWatch {

    /** The steps. */
    public enum State { IDLE, WATCHING, CELL_DOWN, CELL_UP, UNKNOWN }

    /** What the watch needs to know about a cell. {@link #forVision} builds it. */
    public interface CellView {
        /**
         * @param cell a HIVE cell
         * @return true if one of its tags is in the latest frame
         */
        boolean isCellInView(HiveCell cell);

        /**
         * @param cell a HIVE cell
         * @return true if its tags were seen at least once this match
         */
        boolean hasSeenCell(HiveCell cell);
    }

    private final CellView view;
    private final MatchTimer.Clock clock;
    private final StateMachine<State> machine;

    private HiveCell cell;
    private double watchSec;
    private double goneSec;
    private double startSec;
    private double lastSeenSec;

    /**
     * @param view  where to read the cell from (see {@link #forVision})
     * @param clock time source
     */
    public TipWatch(CellView view, MatchTimer.Clock clock) {
        this.view = view;
        this.clock = clock;
        this.machine = new StateMachine<>(State.IDLE, clock);
    }

    /**
     * Builds the watch for the real robot.
     *
     * @param vision the Limelight subsystem
     * @return a watch that reads from it
     */
    public static TipWatch forVision(final Vision vision) {
        return new TipWatch(new CellView() {
            @Override
            public boolean isCellInView(HiveCell c) {
                return vision.isCellInView(c);
            }

            @Override
            public boolean hasSeenCell(HiveCell c) {
                return vision.hasSeenCell(c);
            }
        }, MatchTimer.systemClock());
    }

    /**
     * Starts watching.
     *
     * @param watchedCell the cell to watch (usually our scoring cell)
     * @param maxWatchSec longest watch (TESTED early watch: 4.0 s)
     * @param goneTimeSec how long the tags must be gone to count as down (TESTED: 0.8 s)
     */
    public void start(HiveCell watchedCell, double maxWatchSec, double goneTimeSec) {
        cell = watchedCell;
        watchSec = maxWatchSec;
        goneSec = goneTimeSec;
        startSec = clock.nowSeconds();
        lastSeenSec = startSec;
        machine.restartState(State.WATCHING);
    }

    /** Checks the cell once. Call every loop while watching. */
    public void periodic() {
        if (!machine.isIn(State.WATCHING)) {
            return;
        }
        double nowSec = clock.nowSeconds();
        if (view.isCellInView(cell)) {
            lastSeenSec = nowSec;
        }
        boolean hasSeen = view.hasSeenCell(cell);
        if (hasSeen && nowSec - lastSeenSec >= goneSec) {
            machine.setState(State.CELL_DOWN);
        } else if (nowSec - startSec >= watchSec) {
            machine.setState(hasSeen ? State.CELL_UP : State.UNKNOWN);
        }
    }

    /** @return true when the watch is over (down, up or unknown) */
    public boolean isDone() {
        return !machine.isIn(State.IDLE) && !machine.isIn(State.WATCHING);
    }

    /** @return true if the TIP was seen (the cell went down) */
    public boolean isCellDown() {
        return machine.isIn(State.CELL_DOWN);
    }

    /** @return true if the watch ended with the cell still in view */
    public boolean isCellStillUp() {
        return machine.isIn(State.CELL_UP);
    }

    /** @return true if the cell was never seen, so the watch cannot tell */
    public boolean isUnknown() {
        return machine.isIn(State.UNKNOWN);
    }

    /** @return the current step */
    public State getState() {
        return machine.getState();
    }

    /** @return the watched cell */
    public HiveCell getCell() {
        return cell;
    }
}
