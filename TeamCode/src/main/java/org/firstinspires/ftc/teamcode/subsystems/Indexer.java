// Sources: IronLion30311BIOBUZZInicioDeTemporada / branch feature/turrubiates/vision-align / TeamCode/.../subsystems/Indexer.java @ a7b8f4c (states, pause, checkForJam unjam state machine, ramp, counters)
//          — adapted for BIOBUZZ (update(feed, eject, paused) -> feed()/eject()/idle()/setPaused() + periodic(); injectable current and clock)
package org.firstinspires.ftc.teamcode.subsystems;

import com.qualcomm.robotcore.hardware.DcMotorEx;

import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.robotcore.external.navigation.CurrentUnit;
import org.firstinspires.ftc.teamcode.config.IndexerConstants;
import org.firstinspires.ftc.teamcode.hardware.RobotHardware;
import org.firstinspires.ftc.teamcode.util.MatchTimer;
import org.firstinspires.ftc.teamcode.util.SlewRateLimiter;

import java.util.function.DoubleSupplier;

/**
 * The indexer: one motor that feeds stored balls into the launcher flywheels.
 * <p>
 * <b>Pause:</b> while the flywheel is below its target RPM (spinning up, or recovering from the
 * last shot), call {@link #setPaused(boolean) setPaused(true)}. The indexer then waits
 * ({@link State#PAUSED}), so it meters one ball per shot. TESTED: "29 of 46 scored" with it.
 * <p>
 * <b>Jam protection</b> (FITTED from match logs): while feeding, a current above
 * {@link IndexerConstants#JAM_CURRENT_AMPS} for {@link IndexerConstants#JAM_TIME_SECONDS} means a
 * ball is stuck. The motor reverses for {@link IndexerConstants#UNJAM_TIME_SECONDS} (plus
 * {@link IndexerConstants#UNJAM_STEP_SECONDS} for each further jam in a row) and then feeds again.
 * After {@link IndexerConstants#MAX_UNJAM_ATTEMPTS} unjams in a row it stops
 * ({@link State#JAMMED}) until the driver lets go of feed, so it never keeps pushing a jam.
 * <p>
 * Call {@link #feed()} / {@link #eject()} / {@link #idle()} to say what you want, then
 * {@link #periodic()} every loop.
 */
public class Indexer implements Subsystem {

    /** What the indexer is doing. */
    public enum State { IDLE, FEEDING, PAUSED, EJECTING, UNJAMMING, JAMMED }

    /** What the driver or a sequence asked for. */
    private enum Request { IDLE, FEED, EJECT }

    private final DcMotorEx motor;
    private final DoubleSupplier currentAmps;
    private final MatchTimer.Clock clock;
    private final SlewRateLimiter ramp = new SlewRateLimiter(IndexerConstants.RAMP_SECONDS);

    private Request request = Request.IDLE;
    private boolean isPaused = false;
    private State state = State.IDLE;
    private double lastTimeSec = Double.NaN;
    private double lastCurrentCheckSec = Double.NaN;
    private double highCurrentSec = 0.0;
    private int unjamAttempts = 0;
    private double unjamStartSec = Double.NaN;
    private double unjamDurationSec = 0.0;
    private int totalUnjams = 0;
    private int totalJamStops = 0;
    private double lastWrittenPower = 0.0;

    /** @param hardware robot hardware; the indexer motor may be missing */
    public Indexer(final RobotHardware hardware) {
        this(hardware.indexerMotor, new DoubleSupplier() {
            @Override
            public double getAsDouble() {
                return hardware.indexerMotor == null ? 0.0
                        : hardware.indexerMotor.getCurrent(CurrentUnit.AMPS);
            }
        }, MatchTimer.systemClock());
    }

    /**
     * For tests.
     *
     * @param motor       indexer motor, or null
     * @param currentAmps motor current in amps
     * @param clock       time source
     */
    public Indexer(DcMotorEx motor, DoubleSupplier currentAmps, MatchTimer.Clock clock) {
        this.motor = motor;
        this.currentAmps = currentAmps;
        this.clock = clock;
    }

    /** Feed balls into the launcher (forward). */
    public void feed() {
        request = Request.FEED;
    }

    /** Run in reverse (push balls back toward the intake). */
    public void eject() {
        request = Request.EJECT;
    }

    /** Stop. This also clears a {@link State#JAMMED} stop. */
    public void idle() {
        request = Request.IDLE;
    }

    /**
     * While feeding, true holds the indexer stopped (launcher not ready) without clearing the
     * jam protection. Ignored while ejecting, and when
     * {@link IndexerConstants#PAUSE_WHEN_NOT_READY} is false.
     *
     * @param paused true to wait
     */
    public void setPaused(boolean paused) {
        isPaused = paused;
    }

    /** Runs the state machine and the power ramp. Call once per loop. */
    @Override
    public void periodic() {
        double nowSec = clock.nowSeconds();
        double dtSec = Double.isNaN(lastTimeSec) ? 0.0
                : Math.min(nowSec - lastTimeSec, IndexerConstants.RAMP_MAX_DT_SECONDS);
        lastTimeSec = nowSec;

        boolean isFeed = request == Request.FEED;
        boolean isEject = request == Request.EJECT;
        if (!isFeed && !isEject) {
            state = State.IDLE;
            highCurrentSec = 0.0;
            unjamAttempts = 0;
        } else if (state == State.JAMMED) {
            // Stay stopped until feed is released
        } else if (state == State.UNJAMMING && nowSec - unjamStartSec < unjamDurationSec) {
            // Keep reversing to free the jam
        } else if (isFeed && isPaused && IndexerConstants.PAUSE_WHEN_NOT_READY) {
            state = State.PAUSED;
            highCurrentSec = 0.0;
        } else {
            state = isFeed ? State.FEEDING : State.EJECTING;
            if (state == State.FEEDING) {
                checkForJam(nowSec);
            }
        }
        ramp.setRampSeconds(IndexerConstants.RAMP_SECONDS, IndexerConstants.RAMP_SECONDS);
        writePower(ramp.calculate(targetPowerFor(state), dtSec));
    }

    /**
     * Tracks how long the feeding current stays high and starts an unjam, or stops for good
     * after too many jams in a row.
     */
    private void checkForJam(double nowSec) {
        if (unjamAttempts > 0 && nowSec - unjamStartSec >= IndexerConstants.JAM_RESET_SECONDS) {
            unjamAttempts = 0;
        }
        if (Double.isNaN(lastCurrentCheckSec)) {
            lastCurrentCheckSec = nowSec;
            return;
        }
        double elapsedSec = nowSec - lastCurrentCheckSec;
        if (elapsedSec < IndexerConstants.CURRENT_CHECK_INTERVAL_SECONDS) {
            return;
        }
        lastCurrentCheckSec = nowSec;

        if (currentAmps.getAsDouble() < IndexerConstants.JAM_CURRENT_AMPS) {
            highCurrentSec = 0.0;
            return;
        }
        highCurrentSec += elapsedSec;
        if (highCurrentSec < IndexerConstants.JAM_TIME_SECONDS) {
            return;
        }
        highCurrentSec = 0.0;
        unjamAttempts++;
        unjamStartSec = nowSec;
        if (unjamAttempts > IndexerConstants.MAX_UNJAM_ATTEMPTS) {
            state = State.JAMMED;
            totalJamStops++;
        } else {
            state = State.UNJAMMING;
            unjamDurationSec = IndexerConstants.UNJAM_TIME_SECONDS
                    + IndexerConstants.UNJAM_STEP_SECONDS * (unjamAttempts - 1);
            totalUnjams++;
        }
    }

    /** Stops the motor immediately, skipping the ramp. */
    @Override
    public void stop() {
        request = Request.IDLE;
        state = State.IDLE;
        ramp.reset(0.0);
        writePower(0.0);
    }

    /** @return what the indexer is doing */
    public State getState() {
        return state;
    }

    /** @return true while it stopped for good on a jam (release feed to clear it) */
    public boolean isJammed() {
        return state == State.JAMMED;
    }

    /** @return true while it is actually pushing balls into the launcher */
    public boolean isFeeding() {
        return state == State.FEEDING;
    }

    /** @return unjams (short reverses) since the OpMode started */
    public int getTotalUnjams() {
        return totalUnjams;
    }

    /** @return times it stopped for good on a jam */
    public int getTotalJamStops() {
        return totalJamStops;
    }

    /** @return the power applied to the motor, -1 to 1 */
    public double getPower() {
        return lastWrittenPower;
    }

    /** @return true if the indexer motor is configured */
    public boolean isConnected() {
        return motor != null;
    }

    @Override
    public void addTelemetry(Telemetry t) {
        t.addData("Indexer", "%s (%.2f) unjams %d, jam stops %d%s", state, lastWrittenPower,
                totalUnjams, totalJamStops, isConnected() ? "" : " - motor missing");
    }

    private static double targetPowerFor(State state) {
        switch (state) {
            case FEEDING:
                return IndexerConstants.FEED_POWER;
            case EJECTING:
            case UNJAMMING:
                return -IndexerConstants.FEED_POWER;
            default:
                return 0.0;
        }
    }

    private void writePower(double power) {
        if (power == lastWrittenPower) {
            return;
        }
        lastWrittenPower = power;
        if (motor != null) {
            motor.setPower(power);
        }
    }
}
