// Sources: IronLion30311BIOBUZZInicioDeTemporada / branch feature/turrubiates/vision-align / TeamCode/.../subsystems/Intake.java @ a7b8f4c (states, ramp, stop, getState, getPower)
//          IronLion30311BIOBUZZInicioDeTemporada / branch feature/turrubiates/vision-align / TeamCode/.../opmodes/auto/PedroAutoBase.java @ a7b8f4c (intake shake: short reverse to free a stuck ball)
//          IronLion30311BIOBUZZInicioDeTemporada / branch feature/turrubiates/vision-align / TeamCode/.../subsystems/Intake.java @ b6913d9 (jam protection: checkJam, UNJAMMING, getTotalUnjams)
//          — adapted for BIOBUZZ (update(collect, eject) -> collect()/eject()/idle() + periodic(); G407 collect gate; G408 stop for opponent NECTAR, also during an unjam)
package org.firstinspires.ftc.teamcode.subsystems;

import com.qualcomm.robotcore.hardware.DcMotorEx;

import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.robotcore.external.navigation.CurrentUnit;
import org.firstinspires.ftc.teamcode.config.IntakeConstants;
import org.firstinspires.ftc.teamcode.hardware.RobotHardware;
import org.firstinspires.ftc.teamcode.util.MatchTimer;
import org.firstinspires.ftc.teamcode.util.SlewRateLimiter;

import java.util.function.BooleanSupplier;
import java.util.function.DoubleSupplier;

/**
 * The intake at the front of the robot: one motor that pulls balls in or pushes them out.
 * <p>
 * Tell it what you want ({@link #collect()}, {@link #pushToIndexer()}, {@link #eject()},
 * {@link #idle()}, {@link #shake()}) and call {@link #periodic()} every loop; power changes go
 * through a short ramp to protect the motor and reduce jams.
 * <p>
 * Rule helpers (both optional):
 * <ul>
 *   <li><b>G407</b> ({@link #setCollectGate(BooleanSupplier)}): when the gate says the robot
 *   already controls the maximum number of elements, {@link #collect()} is refused and the
 *   intake stays stopped ({@link State#FULL}).</li>
 *   <li><b>G408</b> ({@link #setOpponentNectarDetector(BooleanSupplier)}): while collecting, if
 *   the detector sees the opponent's NECTAR at the intake, the intake STOPS (it neither pulls the
 *   ball in nor pushes it out) until the ball has been gone for
 *   {@link IntakeConstants#OPPONENT_NECTAR_HOLD_SECONDS} ({@link State#OPPONENT_NECTAR}). Team
 *   decision: pushing it away is not allowed either (G408 control, G405 if it leaves the FIELD,
 *   G402 in AUTO).</li>
 * </ul>
 * <p>
 * <b>Jam protection</b> (vision-align b6913d9): while collecting at speed, a motor current above
 * {@link IntakeConstants#JAM_CURRENT_AMPS} for {@link IntakeConstants#JAM_SECONDS} means a ball
 * is stuck. The intake reverses for {@link IntakeConstants#JAM_REVERSE_SECONDS}
 * ({@link State#UNJAMMING}) and then collects again if collect is still requested. The G408
 * stop wins over an unjam: an opponent NECTAR at the intake is never pushed out.
 */
public class Intake implements Subsystem {

    /** What the intake is doing. */
    public enum State {
        /** Stopped. */
        IDLE,
        /** Pulling new balls in. */
        COLLECTING,
        /** Pushing stored balls toward the indexer while shooting (ignores the gates). */
        PUSHING,
        /** Pushing balls out. */
        EJECTING,
        /** Short reverse to free a stuck ball. */
        SHAKING,
        /** Short reverse after the motor current showed a jam while collecting. */
        UNJAMMING,
        /** Stopped because the opponent's NECTAR is at the intake (G408). Never pushes it. */
        OPPONENT_NECTAR,
        /** Collect refused: the robot already controls the maximum number of elements (G407). */
        FULL
    }

    private final DcMotorEx motor;
    private final MatchTimer.Clock clock;
    private final DoubleSupplier currentAmps;
    private final SlewRateLimiter ramp = new SlewRateLimiter(IntakeConstants.RAMP_SECONDS);

    private BooleanSupplier collectGate;
    private BooleanSupplier opponentNectarDetector;

    private State requested = State.IDLE;
    private State state = State.IDLE;
    private double shakeEndSec = Double.NaN;
    private double opponentNectarLastSeenSec = Double.NaN;
    private double lastTimeSec = Double.NaN;
    private double lastWrittenPower = 0.0;
    private int opponentNectarStopCount = 0;
    private double unjamEndSec = Double.NaN;
    private double highCurrentSec = 0.0;
    private int unjamCount = 0;

    /** @param hardware robot hardware; the intake motor may be missing */
    public Intake(RobotHardware hardware) {
        this(hardware.intakeMotor, MatchTimer.systemClock());
    }

    /**
     * For tests. Reads the motor current from the motor (none without a motor).
     *
     * @param motor intake motor, or null
     * @param clock time source
     */
    public Intake(final DcMotorEx motor, MatchTimer.Clock clock) {
        this(motor, clock, motor == null ? null : new DoubleSupplier() {
            @Override
            public double getAsDouble() {
                return motor.getCurrent(CurrentUnit.AMPS);
            }
        });
    }

    /**
     * For tests of the jam protection.
     *
     * @param motor       intake motor, or null
     * @param clock       time source
     * @param currentAmps motor current in amps, or null to turn the jam protection off
     */
    public Intake(DcMotorEx motor, MatchTimer.Clock clock, DoubleSupplier currentAmps) {
        this.motor = motor;
        this.clock = clock;
        this.currentAmps = currentAmps;
    }

    /** @param gate returns false when the robot may not take another element (G407), or null */
    public void setCollectGate(BooleanSupplier gate) {
        collectGate = gate;
    }

    /** @param detector returns true when the opponent's NECTAR is at the intake (G408), or null */
    public void setOpponentNectarDetector(BooleanSupplier detector) {
        opponentNectarDetector = detector;
    }

    /** Pull balls in (refused while the robot is full, see {@link #setCollectGate}). */
    public void collect() {
        requested = State.COLLECTING;
    }

    /** Push stored balls toward the indexer while shooting. Ignores the gates. */
    public void pushToIndexer() {
        requested = State.PUSHING;
    }

    /** Push balls out of the robot. */
    public void eject() {
        requested = State.EJECTING;
    }

    /** Stop (with the ramp). */
    public void idle() {
        requested = State.IDLE;
    }

    /** Reverse briefly ({@link IntakeConstants#SHAKE_REVERSE_SECONDS}) to free a stuck ball. */
    public void shake() {
        shakeEndSec = clock.nowSeconds() + IntakeConstants.SHAKE_REVERSE_SECONDS;
    }

    /** Decides the state and ramps the motor power. Call once per loop. */
    @Override
    public void periodic() {
        double nowSec = clock.nowSeconds();
        double dtSec = Double.isNaN(lastTimeSec) ? 0.0
                : Math.min(nowSec - lastTimeSec, IntakeConstants.RAMP_MAX_DT_SECONDS);
        lastTimeSec = nowSec;

        state = decideState(nowSec);
        checkJam(nowSec, dtSec);
        ramp.setRampSeconds(IntakeConstants.RAMP_SECONDS, IntakeConstants.RAMP_SECONDS);
        writePower(ramp.calculate(targetPowerFor(state), dtSec));
    }

    /**
     * Starts an unjam once the current stayed above {@link IntakeConstants#JAM_CURRENT_AMPS} for
     * {@link IntakeConstants#JAM_SECONDS} while collecting at speed. The current is read only
     * then (each read is an extra hub transaction).
     */
    private void checkJam(double nowSec, double dtSec) {
        boolean isAtSpeed = lastWrittenPower
                >= IntakeConstants.COLLECT_POWER * IntakeConstants.JAM_MIN_POWER_FRACTION;
        if (!IntakeConstants.JAM_PROTECTION_ENABLED || currentAmps == null
                || state != State.COLLECTING || !isAtSpeed
                || currentAmps.getAsDouble() < IntakeConstants.JAM_CURRENT_AMPS) {
            highCurrentSec = 0.0;
            return;
        }
        highCurrentSec += dtSec;
        if (highCurrentSec >= IntakeConstants.JAM_SECONDS) {
            highCurrentSec = 0.0;
            unjamCount++;
            unjamEndSec = nowSec + IntakeConstants.JAM_REVERSE_SECONDS;
            state = State.UNJAMMING;
        }
    }

    private State decideState(double nowSec) {
        if (!Double.isNaN(unjamEndSec)) {
            if (nowSec < unjamEndSec) {
                // G408 wins: never push an opponent NECTAR out, even to clear a jam
                boolean isOpponentNectar = IntakeConstants.STOP_FOR_OPPONENT_NECTAR
                        && opponentNectarDetector != null && opponentNectarDetector.getAsBoolean();
                if (isOpponentNectar) {
                    unjamEndSec = Double.NaN;
                } else {
                    return State.UNJAMMING;
                }
            } else {
                unjamEndSec = Double.NaN;
            }
        }
        if (!Double.isNaN(shakeEndSec)) {
            if (nowSec < shakeEndSec) {
                return State.SHAKING;
            }
            shakeEndSec = Double.NaN;
        }
        if (requested != State.COLLECTING) {
            opponentNectarLastSeenSec = Double.NaN;
            return requested;
        }
        if (IntakeConstants.STOP_FOR_OPPONENT_NECTAR && opponentNectarDetector != null
                && opponentNectarDetector.getAsBoolean()) {
            if (state != State.OPPONENT_NECTAR) {
                opponentNectarStopCount++;
            }
            opponentNectarLastSeenSec = nowSec;
            return State.OPPONENT_NECTAR;
        }
        if (!Double.isNaN(opponentNectarLastSeenSec)) {
            if (nowSec - opponentNectarLastSeenSec < IntakeConstants.OPPONENT_NECTAR_HOLD_SECONDS) {
                return State.OPPONENT_NECTAR;
            }
            opponentNectarLastSeenSec = Double.NaN;
        }
        if (collectGate != null && !collectGate.getAsBoolean()) {
            return State.FULL;
        }
        return State.COLLECTING;
    }

    /** Stops the motor immediately, skipping the ramp. */
    @Override
    public void stop() {
        requested = State.IDLE;
        state = State.IDLE;
        shakeEndSec = Double.NaN;
        unjamEndSec = Double.NaN;
        highCurrentSec = 0.0;
        opponentNectarLastSeenSec = Double.NaN;
        ramp.reset(0.0);
        writePower(0.0);
    }

    /** @return what the intake is doing (after the last {@link #periodic()}) */
    public State getState() {
        return state;
    }

    /** @return true while it is pulling new balls in (used to count balls) */
    public boolean isCollecting() {
        return state == State.COLLECTING;
    }

    /** @return true during a shake */
    public boolean isShaking() {
        return state == State.SHAKING;
    }

    /** @return true while collect is refused because the robot is full (G407) */
    public boolean isFull() {
        return state == State.FULL;
    }

    /** @return true while collecting is paused for the opponent's NECTAR (G408) */
    public boolean isStoppedForOpponentNectar() {
        return state == State.OPPONENT_NECTAR;
    }

    /** @return true during a jam reverse */
    public boolean isUnjamming() {
        return state == State.UNJAMMING;
    }

    /** @return how many jams the current check found since the OpMode started */
    public int getUnjamCount() {
        return unjamCount;
    }

    /** @return how many times the intake stopped for an opponent NECTAR */
    public int getOpponentNectarStopCount() {
        return opponentNectarStopCount;
    }

    /** @return the power applied to the motor, -1 to 1 */
    public double getPower() {
        return lastWrittenPower;
    }

    /** @return true if the intake motor is configured */
    public boolean isConnected() {
        return motor != null;
    }

    @Override
    public void addTelemetry(Telemetry t) {
        t.addData("Intake", "%s (%.2f), unjams %d%s", state, lastWrittenPower, unjamCount,
                isConnected() ? "" : " - motor missing");
    }

    private static double targetPowerFor(State state) {
        switch (state) {
            case COLLECTING:
            case PUSHING:
                return IntakeConstants.COLLECT_POWER;
            case EJECTING:
            case SHAKING:
            case UNJAMMING:
                return -IntakeConstants.EJECT_POWER;
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
