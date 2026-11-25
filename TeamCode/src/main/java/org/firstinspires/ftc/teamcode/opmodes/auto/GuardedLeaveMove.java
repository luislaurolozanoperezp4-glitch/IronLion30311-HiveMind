// Sources: IronLion30311BIOBUZZInicioDeTemporada / branch feature/turrubiates/auto-leave-park / TeamCode/.../subsystems/GuardedStraightDrive.java @ e455142 (driveStraightCm, meanAbsoluteWheelTravelCm)
//          — adapted for BIOBUZZ (non-blocking, wheel encoders only: no Pinpoint needed)
package org.firstinspires.ftc.teamcode.opmodes.auto;

import org.firstinspires.ftc.teamcode.config.DriveConstants;
import org.firstinspires.ftc.teamcode.util.MatchTimer;

import java.util.Locale;

/**
 * A short straight move at constant power that stops on the wheel encoders, with a hard
 * timeout. Built for the LEAVE out of the LOADING ZONE when the Pinpoint or Pedro cannot be
 * trusted: the timeout is chosen so that even a full-timeout move (encoders dead) ends inside
 * the zone.
 * <p>
 * Wheel travel is the mean of the <i>absolute</i> change of the four wheels, so one wheel
 * counting backwards does not cancel the others (from the field-tested
 * {@code GuardedStraightDrive}). Non-blocking: {@link #start}, then {@link #periodic()} every
 * loop until {@link #isDone()}. Tested by {@code GuardedLeaveMoveTest}.
 */
public class GuardedLeaveMove {

    /** What the move needs from the drivetrain. */
    public interface Wheels {
        /** @return encoder ticks of the four wheels */
        int[] getWheelPositions();

        /**
         * @param forward constant robot-centric power, + = forward (toward the intake)
         */
        void drive(double forward);

        /** Stops the wheels. */
        void stop();
    }

    /** How the move ended. */
    public enum Result {
        NONE,
        REACHED,
        TIMED_OUT,
        CANCELLED
    }

    private final Wheels wheels;
    private final MatchTimer.Clock clock;
    private int[] startTicks;
    private double targetCm;
    private double power;
    private double timeoutSeconds;
    private double startSeconds;
    private double travelCm;
    private boolean isRunning;
    private Result result = Result.NONE;

    /**
     * @param wheels the drivetrain
     * @param clock  time source
     */
    public GuardedLeaveMove(Wheels wheels, MatchTimer.Clock clock) {
        this.wheels = wheels;
        this.clock = clock;
    }

    /**
     * Starts the move.
     *
     * @param distanceCm     travel to reach (magnitude only)
     * @param forwardPower   constant power, + forward, - backward
     * @param timeoutSeconds hard time limit
     */
    public void start(double distanceCm, double forwardPower, double timeoutSeconds) {
        startTicks = wheels.getWheelPositions().clone();
        targetCm = Math.abs(distanceCm);
        power = forwardPower;
        this.timeoutSeconds = timeoutSeconds;
        startSeconds = clock.nowSeconds();
        travelCm = 0.0;
        result = Result.NONE;
        isRunning = true;
        wheels.drive(power);
    }

    /** Checks the encoders and the timeout. Call once per loop. */
    public void periodic() {
        if (!isRunning) {
            return;
        }
        travelCm = meanAbsoluteTravelCm(startTicks, wheels.getWheelPositions());
        if (travelCm >= targetCm) {
            finish(Result.REACHED);
        } else if (clock.nowSeconds() - startSeconds >= timeoutSeconds) {
            finish(Result.TIMED_OUT);
        } else {
            wheels.drive(power);
        }
    }

    /** Stops the move early. */
    public void cancel() {
        if (isRunning) {
            finish(Result.CANCELLED);
        }
    }

    private void finish(Result endResult) {
        wheels.stop();
        result = endResult;
        isRunning = false;
    }

    /** @return true when the move has ended (or never started) */
    public boolean isDone() {
        return !isRunning;
    }

    /** @return how the move ended */
    public Result getResult() {
        return result;
    }

    /** @return wheel travel measured so far, cm */
    public double getTravelCm() {
        return travelCm;
    }

    /** @return one line for telemetry */
    public String getResultText() {
        return String.format(Locale.US, "LEAVE %s: wheels %.1f cm of %.1f cm", result, travelCm, targetCm);
    }

    /**
     * @param startTicks   ticks at the start
     * @param currentTicks ticks now
     * @return mean absolute travel of the wheels, cm
     */
    public static double meanAbsoluteTravelCm(int[] startTicks, int[] currentTicks) {
        double sumTicks = 0.0;
        for (int i = 0; i < startTicks.length; i++) {
            sumTicks += Math.abs(currentTicks[i] - startTicks[i]);
        }
        return sumTicks / startTicks.length / DriveConstants.ENCODER_TICKS_PER_CM_FORWARD;
    }
}
