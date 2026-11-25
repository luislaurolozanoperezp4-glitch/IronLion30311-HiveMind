// Sources: new for IronLion30311-HiveMind (pattern of the vision-align Indexer state enum + timers @ a7b8f4c)
package org.firstinspires.ftc.teamcode.statemachine;

import org.firstinspires.ftc.teamcode.util.MatchTimer;

/**
 * A tiny helper for "which step am I in, and for how long".
 * <p>
 * Example:
 * <pre>
 *   enum Step { SPIN_UP, FEED, DONE }
 *   StateMachine&lt;Step&gt; machine = new StateMachine&lt;&gt;(Step.SPIN_UP, clock);
 *   if (machine.isIn(Step.SPIN_UP) &amp;&amp; machine.getTimeInStateSec() &gt; 1.0) {
 *       machine.setState(Step.FEED);
 *   }
 * </pre>
 * It never blocks: you check it once per loop.
 *
 * @param <S> the enum of steps
 */
public class StateMachine<S extends Enum<S>> {

    private final MatchTimer.Clock clock;
    private S state;
    private S previousState;
    private double enteredSec;

    /**
     * @param initialState the first step
     * @param clock        time source ({@link MatchTimer#systemClock()} on the robot)
     */
    public StateMachine(S initialState, MatchTimer.Clock clock) {
        this.clock = clock;
        this.state = initialState;
        this.previousState = initialState;
        this.enteredSec = clock.nowSeconds();
    }

    /**
     * Moves to a step. Setting the same step again does NOT restart its timer.
     *
     * @param newState the new step
     */
    public void setState(S newState) {
        if (newState == state) {
            return;
        }
        previousState = state;
        state = newState;
        enteredSec = clock.nowSeconds();
    }

    /**
     * Moves to a step and always restarts its timer (even if it is the same step).
     *
     * @param newState the new step
     */
    public void restartState(S newState) {
        previousState = state;
        state = newState;
        enteredSec = clock.nowSeconds();
    }

    /** @return the current step */
    public S getState() {
        return state;
    }

    /** @return the step before the current one */
    public S getPreviousState() {
        return previousState;
    }

    /**
     * @param candidate a step
     * @return true if it is the current step
     */
    public boolean isIn(S candidate) {
        return state == candidate;
    }

    /** @return seconds since the current step started */
    public double getTimeInStateSec() {
        return clock.nowSeconds() - enteredSec;
    }
}
