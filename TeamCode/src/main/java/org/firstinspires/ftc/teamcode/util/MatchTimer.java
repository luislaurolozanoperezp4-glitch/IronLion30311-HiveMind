// Sources: IronLion30311BIOBUZZInicioDeTemporada / branch feature/turrubiates/vision-align / TeamCode/.../opmodes/auto/PedroAutoBase.java @ a7b8f4c (autoSeconds())
//          IronLion30311BIOBUZZInicioDeTemporada / branch feature/theautonomous / TeamCode/.../MatchClock.java @ 125df22 (idea of a match clock)
//          — adapted for BIOBUZZ
package org.firstinspires.ftc.teamcode.util;

import org.firstinspires.ftc.teamcode.config.MatchConstants;

/**
 * Knows how much time is left in the current period, and which match phase it is.
 * <p>
 * Used to enforce G410 (NECTAR into FLOWERS only in the last 60 s) and to decide in AUTO
 * whether there is time for one more cycle or the robot must go park.
 * <p>
 * The clock is injectable so JUnit tests can use a fake time.
 */
public class MatchTimer {

    /** Supplies the current time in seconds. */
    public interface Clock {
        double nowSeconds();
    }

    /** Match phases the robot cares about. */
    public enum Phase {
        NOT_STARTED,
        AUTO,
        TELEOP,
        ENDGAME,
        OVER
    }

    private final Clock clock;
    private final double periodSeconds;
    private final boolean isAuto;
    private double startSeconds = Double.NaN;

    /**
     * @param clock  time source (use {@link #systemClock()} on the robot)
     * @param isAuto true for the AUTO period, false for TELEOP
     */
    public MatchTimer(Clock clock, boolean isAuto) {
        this.clock = clock;
        this.isAuto = isAuto;
        this.periodSeconds = isAuto ? MatchConstants.AUTO_SECONDS : MatchConstants.TELEOP_SECONDS;
    }

    /** @return a clock based on {@link System#nanoTime()} */
    public static Clock systemClock() {
        return new Clock() {
            @Override
            public double nowSeconds() {
                return System.nanoTime() / 1e9;
            }
        };
    }

    /** Starts the period. Call it from the OpMode start() method. */
    public void start() {
        startSeconds = clock.nowSeconds();
    }

    /** @return true after {@link #start()} was called */
    public boolean isStarted() {
        return !Double.isNaN(startSeconds);
    }

    /** @return seconds since {@link #start()}, or 0 before it */
    public double getElapsedSeconds() {
        return isStarted() ? clock.nowSeconds() - startSeconds : 0.0;
    }

    /** @return seconds left in this period, never below 0 */
    public double getRemainingSeconds() {
        return Math.max(0.0, periodSeconds - getElapsedSeconds());
    }

    /**
     * @param seconds how long a task will take
     * @return true if the task fits before the end of the period, keeping the park reserve in AUTO
     */
    public boolean hasTimeFor(double seconds) {
        double reserve = isAuto ? MatchConstants.AUTO_PARK_RESERVE_SECONDS : 0.0;
        return getRemainingSeconds() - reserve >= seconds;
    }

    /** @return the current match phase */
    public Phase getPhase() {
        if (!isStarted()) {
            return Phase.NOT_STARTED;
        }
        if (getRemainingSeconds() <= 0.0) {
            return Phase.OVER;
        }
        if (isAuto) {
            return Phase.AUTO;
        }
        return isEndgame() ? Phase.ENDGAME : Phase.TELEOP;
    }

    /** @return true in TELEOP during the last 60 seconds */
    public boolean isEndgame() {
        return !isAuto && isStarted()
                && getRemainingSeconds() <= MatchConstants.NECTAR_UNLOCK_SECONDS;
    }

    /**
     * G410: NECTAR may go into FLOWERS only during the last 60 seconds of the MATCH (= of
     * TELEOP). Unlocks {@link MatchConstants#NECTAR_UNLOCK_MARGIN_SECONDS} later than that, because
     * this clock starts at the driver's START press, not with the FIELD timer.
     */
    public boolean canPlaceNectar() {
        double remaining = getRemainingSeconds();
        return isEndgame() && remaining > 0.0
                && remaining <= MatchConstants.NECTAR_UNLOCK_SECONDS
                - MatchConstants.NECTAR_UNLOCK_MARGIN_SECONDS;
    }
}
