// Sources: new for IronLion30311-HiveMind (G407 element limit; uses the VA ball counter and shot detection)
package org.firstinspires.ftc.teamcode.subsystems;

import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.teamcode.config.MatchConstants;

/**
 * Keeps track of how many scoring elements the robot controls, for rule G407 (at most
 * {@link MatchConstants#MAX_CONTROLLED_ELEMENTS} at once).
 * <p>
 * The count goes up when the HuskyLens sees a ball go into the intake ({@link ColorSensing}) and
 * down on every shot ({@link Launcher}). Give the intake {@link #canCollectMore()} as its collect
 * gate ({@code intake.setCollectGate(counter::canCollectMore)}) so collecting a 5th element is
 * refused.
 * <p>
 * It is an estimate (the camera can miss or double-count a ball), so the driver can fix it with
 * {@link #setCount(int)}. At the start of AUTO call {@code setCount(PRELOAD_BALLS)}.
 * Without a HuskyLens only shots are counted, so the gate never closes by itself.
 */
public class ElementCounter implements Subsystem {

    private final ColorSensing colorSensing;
    private final Launcher launcher;

    private int count = 0;
    private int lastCollectedTotal = 0;
    private int lastShotTotal = 0;
    private int refusedCount = 0;

    /**
     * @param colorSensing source of pickups, or null
     * @param launcher     source of shots, or null
     */
    public ElementCounter(ColorSensing colorSensing, Launcher launcher) {
        this.colorSensing = colorSensing;
        this.launcher = launcher;
        if (colorSensing != null) {
            lastCollectedTotal = colorSensing.getTotalBalls();
        }
        if (launcher != null) {
            lastShotTotal = launcher.getTotalShotCount();
        }
    }

    /** Reads new pickups and shots. Call once per loop, after ColorSensing and Launcher. */
    @Override
    public void periodic() {
        int collectedTotal = colorSensing == null ? lastCollectedTotal : colorSensing.getTotalBalls();
        int shotTotal = launcher == null ? lastShotTotal : launcher.getTotalShotCount();
        update(collectedTotal, shotTotal);
    }

    /**
     * Pure-logic update from running totals (used by {@link #periodic()} and by tests).
     *
     * @param collectedTotal balls picked up since the start
     * @param shotTotal      shots since the start
     */
    public void update(int collectedTotal, int shotTotal) {
        for (int i = lastCollectedTotal; i < collectedTotal; i++) {
            recordCollected();
        }
        for (int i = lastShotTotal; i < shotTotal; i++) {
            recordLaunched();
        }
        lastCollectedTotal = collectedTotal;
        lastShotTotal = shotTotal;
    }

    /**
     * Counts one element in.
     *
     * @return true if it was accepted; false if the robot was already at the limit (the count
     *         stays at the limit and the refusal is counted)
     */
    public boolean recordCollected() {
        if (!canCollectMore()) {
            refusedCount++;
            return false;
        }
        count++;
        return true;
    }

    /** Counts one element out (shot). */
    public void recordLaunched() {
        count = Math.max(0, count - 1);
    }

    /** Counts one element out (pushed out by the intake). */
    public void recordEjected() {
        count = Math.max(0, count - 1);
    }

    /** @param newCount the real number of elements on board (clamped to 0..max) */
    public void setCount(int newCount) {
        count = Math.max(0, Math.min(MatchConstants.MAX_CONTROLLED_ELEMENTS, newCount));
    }

    /** @return elements the robot controls now (estimate) */
    public int getCount() {
        return count;
    }

    /** @return true if one more element may be collected (G407) */
    public boolean canCollectMore() {
        return count < MatchConstants.MAX_CONTROLLED_ELEMENTS;
    }

    /** @return true at the G407 limit */
    public boolean isFull() {
        return !canCollectMore();
    }

    /** @return how many pickups arrived while already full (should stay 0) */
    public int getRefusedCount() {
        return refusedCount;
    }

    /** Sets the count to zero. */
    public void reset() {
        count = 0;
        refusedCount = 0;
    }

    /** Nothing to stop. */
    @Override
    public void stop() {
    }

    @Override
    public void addTelemetry(Telemetry t) {
        t.addData("Elements", "%d / %d%s", count, MatchConstants.MAX_CONTROLLED_ELEMENTS,
                isFull() ? " FULL" : "");
    }
}
