// Sources: IronLion30311BIOBUZZInicioDeTemporada / branch feature/luis/autonomospaths / TeamCode/.../subsystems/autosubsystems/Pid.java @ 9cf2c66 (derivative on measurement, anti-windup, dt clamp — ideas only)
//          IronLion30311BIOBUZZInicioDeTemporada / branch feature/turrubiates/vision-align / TeamCode/.../subsystems/vision/HiveAlignController.java @ a7b8f4c (computeAxisOutput: PD + static feedforward)
//          — adapted for BIOBUZZ
package org.firstinspires.ftc.teamcode.util;

/**
 * A PIDF controller: turns "how far am I from the target" into a motor command.
 * <ul>
 *   <li><b>P</b> pushes harder the further away you are.</li>
 *   <li><b>I</b> adds up the error over time, to fix a small error that P alone leaves. It is
 *   limited ({@link #setIntegralLimit(double)}) so it cannot "wind up" to a huge value.</li>
 *   <li><b>D</b> brakes when the measurement changes fast. It uses the change of the
 *   measurement, not of the error, so changing the setpoint does not cause a kick.</li>
 *   <li><b>F</b> (feedforward) is {@code kF * setpoint}: the command you already know you need.</li>
 *   <li><b>Static</b> feedforward ({@link #setStaticFeedforward(double)}) adds a small push in the
 *   error's direction, to beat friction.</li>
 * </ul>
 * Time comes from an injectable {@link MatchTimer.Clock}, so it is unit tested on the computer.
 */
public class PidfController {

    /** Longest loop time used for I and D, so a long pause does not create a huge step. */
    private static final double MAX_DT_SECONDS = 0.1;
    /** Shorter loop times than this are treated as "no time passed". */
    private static final double MIN_DT_SECONDS = 1e-4;

    private final MatchTimer.Clock clock;
    private double kP;
    private double kI;
    private double kD;
    private double kF;
    private double kStatic = 0.0;
    private double integralLimit = Double.POSITIVE_INFINITY;
    private double outputLimit = Double.POSITIVE_INFINITY;

    private double integral = 0.0;
    private double lastMeasurement = 0.0;
    private double lastTimeSec = Double.NaN;
    private double lastError = 0.0;

    /**
     * @param kP    proportional gain
     * @param kI    integral gain
     * @param kD    derivative gain
     * @param kF    feedforward gain (multiplied by the setpoint)
     * @param clock time source ({@link MatchTimer#systemClock()} on the robot)
     */
    public PidfController(double kP, double kI, double kD, double kF, MatchTimer.Clock clock) {
        this.clock = clock;
        setGains(kP, kI, kD, kF);
    }

    /**
     * Same as the other constructor, using the system clock.
     *
     * @param kP proportional gain
     * @param kI integral gain
     * @param kD derivative gain
     * @param kF feedforward gain
     */
    public PidfController(double kP, double kI, double kD, double kF) {
        this(kP, kI, kD, kF, MatchTimer.systemClock());
    }

    /**
     * Changes the gains (for example from FTC Dashboard values).
     *
     * @param p proportional gain
     * @param i integral gain
     * @param d derivative gain
     * @param f feedforward gain
     */
    public void setGains(double p, double i, double d, double f) {
        kP = p;
        kI = i;
        kD = d;
        kF = f;
    }

    /** @param limit largest absolute value the I term may reach (anti-windup) */
    public void setIntegralLimit(double limit) {
        integralLimit = Math.abs(limit);
    }

    /** @param limit largest absolute output (for example 1.0 for a motor power) */
    public void setOutputLimit(double limit) {
        outputLimit = Math.abs(limit);
    }

    /** @param value push added in the direction of the error while it is not zero */
    public void setStaticFeedforward(double value) {
        kStatic = value;
    }

    /**
     * Computes the command for one loop.
     *
     * @param setpoint    where you want to be
     * @param measurement where you are now
     * @return the command, limited by {@link #setOutputLimit(double)}
     */
    public double calculate(double setpoint, double measurement) {
        double nowSec = clock.nowSeconds();
        double error = setpoint - measurement;
        double dtSec = Double.isNaN(lastTimeSec) ? 0.0
                : Math.min(nowSec - lastTimeSec, MAX_DT_SECONDS);

        double derivative = 0.0;
        if (dtSec > MIN_DT_SECONDS) {
            integral += error * dtSec;
            integral = clamp(integral, integralLimit);
            derivative = -(measurement - lastMeasurement) / dtSec;
        }
        lastTimeSec = nowSec;
        lastMeasurement = measurement;
        lastError = error;

        double output = kP * error + kI * integral + kD * derivative + kF * setpoint;
        if (error != 0.0) {
            output += Math.copySign(kStatic, error);
        }
        return clamp(output, outputLimit);
    }

    /** @return the error of the last {@link #calculate(double, double)} call */
    public double getLastError() {
        return lastError;
    }

    /** Forgets the I term and the D history (call when the target changes a lot). */
    public void reset() {
        integral = 0.0;
        lastTimeSec = Double.NaN;
        lastMeasurement = 0.0;
        lastError = 0.0;
    }

    private static double clamp(double value, double limit) {
        return Math.max(-limit, Math.min(limit, value));
    }
}
