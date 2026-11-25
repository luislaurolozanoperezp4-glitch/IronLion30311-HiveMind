// Sources: IronLion30311BIOBUZZInicioDeTemporada / branch feature/turrubiates/vision-align / TeamCode/.../opmodes/auto/PreloadRouteAuto.java @ a7b8f4c (runRoutine: steps in order)
//          IronLion30311BIOBUZZInicioDeTemporada / branch feature/theautonomous / TeamCode/.../RouteRunner.java @ 125df22 (idea: a runner that walks a list of steps)
//          — adapted for BIOBUZZ (non-blocking)
package org.firstinspires.ftc.teamcode.opmodes.auto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Runs {@link AutoStep}s one after the other, one control step per loop. It never blocks: call
 * {@link #periodic()} once per OpMode loop.
 * <p>
 * The plan can change while it runs:
 * <ul>
 *     <li>{@link #add} puts steps at the end of the plan;</li>
 *     <li>{@link #addNext} puts steps right after the current one (a decision step uses it to
 *     choose "cycle again" or "go park");</li>
 *     <li>{@link #abortAndReplace} cancels the current step, drops the rest of the plan and
 *     runs the given steps instead (used to go park when time runs out).</li>
 * </ul>
 * Pure logic, no hardware: tested by {@code StepSequenceTest}.
 */
public class StepSequence {

    /** Told about every line added to the step log (step results, notes, cancellations). */
    public interface Listener {
        /** @param line the new log line */
        void onLogLine(String line);
    }

    /** Instant steps (decisions) chained in one loop at most this many times, to never hang. */
    static final int MAX_STEPS_STARTED_PER_LOOP = 8;
    /** Lines kept in the step log. */
    static final int MAX_LOG_LINES = 40;

    private final LinkedList<AutoStep> pending = new LinkedList<>();
    private final List<String> log = new ArrayList<>();
    private AutoStep current;
    private int stepsFinished;
    private Listener listener;

    /** @param listener told about every new log line (for example the CSV pose log), or null */
    public void setListener(Listener listener) {
        this.listener = listener;
    }

    /**
     * Adds steps at the end of the plan.
     *
     * @param steps steps in order
     */
    public void add(AutoStep... steps) {
        Collections.addAll(pending, steps);
    }

    /**
     * Adds steps at the end of the plan.
     *
     * @param steps steps in order
     */
    public void addAll(List<AutoStep> steps) {
        pending.addAll(steps);
    }

    /**
     * Adds steps right after the current one, before the rest of the plan, keeping their order.
     *
     * @param steps steps in order
     */
    public void addNext(List<AutoStep> steps) {
        for (int i = steps.size() - 1; i >= 0; i--) {
            pending.addFirst(steps.get(i));
        }
    }

    /**
     * Same as {@link #addNext(List)}.
     *
     * @param steps steps in order
     */
    public void addNext(AutoStep... steps) {
        List<AutoStep> list = new ArrayList<>();
        Collections.addAll(list, steps);
        addNext(list);
    }

    /**
     * Cancels the current step, drops every step still planned and runs {@code steps} instead.
     *
     * @param steps the new plan (may be empty: then the sequence is finished)
     */
    public void abortAndReplace(List<AutoStep> steps) {
        if (current != null) {
            current.cancel();
            addLog(current.getName() + ": CANCELLED");
            current = null;
        }
        pending.clear();
        pending.addAll(steps);
    }

    /** Runs one loop: the current step, then starts the next ones that are due. */
    public void periodic() {
        if (current != null) {
            current.periodic();
            if (current.isDone()) {
                finishCurrent();
            }
        }
        int started = 0;
        while (current == null && !pending.isEmpty() && started < MAX_STEPS_STARTED_PER_LOOP) {
            current = pending.removeFirst();
            started++;
            current.start();
            if (current.isDone()) {
                finishCurrent();
            }
        }
    }

    private void finishCurrent() {
        String result = current.getResultText();
        addLog(result == null || result.isEmpty() ? current.getName() + ": done" : result);
        current = null;
        stepsFinished++;
    }

    /**
     * Adds a line to the step log, for example why a decision was taken.
     *
     * @param line text to log
     */
    public void note(String line) {
        addLog(line);
    }

    private void addLog(String line) {
        if (listener != null) {
            listener.onLogLine(line);
        }
        log.add(line);
        if (log.size() > MAX_LOG_LINES) {
            log.remove(0);
        }
    }

    /** @return true when no step is running and none is planned */
    public boolean isFinished() {
        return current == null && pending.isEmpty();
    }

    /** @return the running step, or null */
    public AutoStep getCurrent() {
        return current;
    }

    /** @return the running step's name, or "Finished" */
    public String getCurrentName() {
        return current == null ? (pending.isEmpty() ? "Finished" : "Starting") : current.getName();
    }

    /** @return names of the steps still planned after the current one */
    public List<String> getPendingNames() {
        List<String> names = new ArrayList<>();
        for (AutoStep step : pending) {
            names.add(step.getName());
        }
        return names;
    }

    /** @return how many steps have ended (cancelled ones not counted) */
    public int getStepsFinished() {
        return stepsFinished;
    }

    /** @return one line per ended step, oldest first (the last {@link #MAX_LOG_LINES}) */
    public List<String> getLog() {
        return Collections.unmodifiableList(log);
    }
}
