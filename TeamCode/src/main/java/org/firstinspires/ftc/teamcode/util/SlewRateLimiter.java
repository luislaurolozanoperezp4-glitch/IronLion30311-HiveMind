// Sources: IronLion30311BIOBUZZInicioDeTemporada / branch feature/turrubiates/vision-align / TeamCode/.../subsystems/MecanumDrive.java @ a7b8f4c (applyRamp: separate accel/decel, stop at zero on a reversal)
//          IronLion30311BIOBUZZInicioDeTemporada / branch feature/turrubiates/vision-align / TeamCode/.../subsystems/Intake.java, Indexer.java @ a7b8f4c (applyRamp)
//          — adapted for BIOBUZZ (one shared ramp instead of three copies)
package org.firstinspires.ftc.teamcode.util;

/**
 * Makes a value (usually a motor power) change gradually instead of jumping.
 * <p>
 * You give it the time to go from 0 to full (1.0) when speeding up, and the time to go from full
 * to 0 when slowing down. Each loop, {@link #calculate(double, double)} moves the output toward
 * the target by at most one step. When the target is on the other side of zero (for example
 * forward to backward), the output first stops at zero and only then speeds up the other way,
 * so a mechanism never snaps from one direction to the other.
 * <p>
 * Pure math, no hardware: it is unit tested on the computer.
 */
public class SlewRateLimiter {

    private double accelSeconds;
    private double decelSeconds;
    private double value;

    /**
     * @param accelSeconds time to ramp from 0 to 1 (speeding up); 0 = no ramp
     * @param decelSeconds time to ramp from 1 to 0 (slowing down); 0 = no ramp
     */
    public SlewRateLimiter(double accelSeconds, double decelSeconds) {
        setRampSeconds(accelSeconds, decelSeconds);
    }

    /**
     * Same ramp time for speeding up and slowing down.
     *
     * @param rampSeconds time to ramp from 0 to 1 and back
     */
    public SlewRateLimiter(double rampSeconds) {
        this(rampSeconds, rampSeconds);
    }

    /**
     * Changes the ramp times (for example when tuned live from FTC Dashboard).
     *
     * @param accelSeconds time to ramp from 0 to 1
     * @param decelSeconds time to ramp from 1 to 0
     */
    public void setRampSeconds(double accelSeconds, double decelSeconds) {
        this.accelSeconds = Math.max(0.0, accelSeconds);
        this.decelSeconds = Math.max(0.0, decelSeconds);
    }

    /**
     * Moves the output one step toward the target.
     *
     * @param target the value you want
     * @param dtSec  seconds since the last call (the loop time)
     * @return the new, limited output
     */
    public double calculate(double target, double dtSec) {
        boolean isReversing = target * value < 0.0;
        boolean isSlowingDown = isReversing || Math.abs(target) < Math.abs(value);
        double rampSeconds = isSlowingDown ? decelSeconds : accelSeconds;
        if (rampSeconds <= 0.0) {
            value = isReversing ? 0.0 : target;
            return value;
        }
        double maxStep = Math.max(0.0, dtSec) / rampSeconds;
        double delta = Math.max(-maxStep, Math.min(maxStep, target - value));
        // A reversal stops at zero first; later calls speed up the other way.
        if (isReversing && Math.abs(delta) > Math.abs(value)) {
            delta = -value;
        }
        value += delta;
        return value;
    }

    /**
     * Jumps the output straight to a value (for example 0 when a motor is stopped hard).
     *
     * @param newValue the new output
     */
    public void reset(double newValue) {
        value = newValue;
    }

    /** @return the last output */
    public double getValue() {
        return value;
    }
}
