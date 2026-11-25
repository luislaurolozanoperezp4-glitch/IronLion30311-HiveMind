// Sources: IronLion30311BIOBUZZInicioDeTemporada / branch feature/turrubiates/vision-align / TeamCode/.../subsystems/Intake.java @ a7b8f4c (states, ramp, stop, getState, getPower)
//          IronLion30311BIOBUZZInicioDeTemporada / branch feature/turrubiates/vision-align / TeamCode/.../opmodes/auto/PedroAutoBase.java @ a7b8f4c (intake shake: short reverse to free a stuck ball)
//          — adapted for BIOBUZZ (update(collect, eject) -> collect()/eject()/idle() + periodic(); G407 collect gate; G408 stop for opponent NECTAR)
package org.firstinspires.ftc.teamcode.subsystems;

import com.qualcomm.robotcore.hardware.DcMotorEx;

import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.teamcode.config.IntakeConstants;
import org.firstinspires.ftc.teamcode.hardware.RobotHardware;
import org.firstinspires.ftc.teamcode.util.MatchTimer;
import org.firstinspires.ftc.teamcode.util.SlewRateLimiter;

import java.util.function.BooleanSupplier;

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
        /** Stopped because the opponent's NECTAR is at the intake (G408). Never pushes it. */
        OPPONENT_NECTAR,
        /** Collect refused: the robot already controls the maximum number of elements (G407). */
        FULL
    }

    private final DcMotorEx motor;
    private final MatchTimer.Clock clock;
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

    /** @param hardware robot hardware; the intake motor may be missing */
    public Intake(RobotHardware hardware) {
        this(hardware.intakeMotor, MatchTimer.systemClock());
    }

    /**
     * For tests.
     *
     * @param motor intake motor, or null
     * @param clock time source
     */
    public Intake(DcMotorEx motor, MatchTimer.Clock clock) {
        this.motor = motor;
        this.clock = clock;
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
        ramp.setRampSeconds(IntakeConstants.RAMP_SECONDS, IntakeConstants.RAMP_SECONDS);
        writePower(ramp.calculate(targetPowerFor(state), dtSec));
    }

    private State decideState(double nowSec) {
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
        t.addData("Intake", "%s (%.2f)%s", state, lastWrittenPower,
                isConnected() ? "" : " - motor missing");
    }

    private static double targetPowerFor(State state) {
        switch (state) {
            case COLLECTING:
            case PUSHING:
                return IntakeConstants.COLLECT_POWER;
            case EJECTING:
            case SHAKING:
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
