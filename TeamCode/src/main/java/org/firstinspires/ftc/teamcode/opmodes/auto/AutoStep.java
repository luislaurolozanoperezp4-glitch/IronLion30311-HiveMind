// Sources: IronLion30311BIOBUZZInicioDeTemporada / branch feature/turrubiates/vision-align / TeamCode/.../opmodes/auto/PedroAutoBase.java @ a7b8f4c (each blocking helper: driveTo, settle, shootPollenPreload, watchForTip, lookForCell)
//          — adapted for BIOBUZZ (one non-blocking step object per helper)
package org.firstinspires.ftc.teamcode.opmodes.auto;

/**
 * One step of an autonomous routine: a leg, a turn, a volley, a TIP watch, a wait, or a
 * decision. A {@link StepSequence} runs the steps one after the other, without blocking.
 * <p>
 * Life of a step: {@link #start()} once, then {@link #periodic()} once per loop until
 * {@link #isDone()} is true. {@link #cancel()} is called instead if the routine is aborted
 * (for example to go park because time is short).
 * <p>
 * Use {@link #instant(String, Runnable)} for a decision: it runs its code once in
 * {@link #start()} and is done at once. A decision usually adds the next steps with
 * {@link StepSequence#addNext}.
 */
public abstract class AutoStep {

    private final String name;

    /**
     * @param name short name shown on the telemetry and written to the logs
     */
    protected AutoStep(String name) {
        this.name = name;
    }

    /** @return the step name */
    public String getName() {
        return name;
    }

    /** Starts the step (commands a move, starts a sequence, ...). Called once. */
    public void start() {
    }

    /** Runs one control step. Called once per loop while the step is not done. */
    public void periodic() {
    }

    /** @return true when the step has ended */
    public abstract boolean isDone();

    /** Stops the step early (mechanisms and drive). Called instead of waiting for the end. */
    public void cancel() {
    }

    /** @return one line saying how the step ended, for telemetry and logs ("" if nothing to say) */
    public String getResultText() {
        return "";
    }

    /**
     * A step that runs {@code action} once and is done at once (a decision point).
     *
     * @param name   step name
     * @param action code to run, for example deciding and adding the next steps
     * @return the step
     */
    public static AutoStep instant(String name, final Runnable action) {
        return new AutoStep(name) {
            @Override
            public void start() {
                action.run();
            }

            @Override
            public boolean isDone() {
                return true;
            }
        };
    }
}
