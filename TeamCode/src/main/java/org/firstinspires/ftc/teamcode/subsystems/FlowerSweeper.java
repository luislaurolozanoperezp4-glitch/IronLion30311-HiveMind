// Sources: IronLion30311BIOBUZZInicioDeTemporada / branch feature/turrubiates/vision-align / TeamCode/.../subsystems/FlowerSweeper.java @ 344254b (down, up, toggle, update -> periodic, getState, isDown, isConnected, command, apply)
//          — adapted for BIOBUZZ (Subsystem interface, constants in FlowerSweeperConstants, injectable clock, optional rule gate for lowering)
package org.firstinspires.ftc.teamcode.subsystems;

import com.qualcomm.robotcore.hardware.Servo;

import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.teamcode.config.FlowerSweeperConstants;
import org.firstinspires.ftc.teamcode.hardware.RobotHardware;
import org.firstinspires.ftc.teamcode.util.MatchTimer;

import java.util.function.BooleanSupplier;

/**
 * The FLOWER sweeper: a bar that sweeps POLLEN off the FLOWERS, lowered to the floor and raised
 * by two mirrored goBILDA servos ({@code sweeperLeftServo}, {@code sweeperRightServo}).
 * <p>
 * <b>How it moves.</b> The bar has a "travel" from 0 (up) to 1 (down). Each servo position is
 * interpolated between its own UP and DOWN value ({@link FlowerSweeperConstants}), so the two
 * mirrored servos stay together. Moves are slowed to {@code MOVE_SECONDS} for the whole travel
 * so the bar does not slam into the floor. Nothing is commanded until the first {@link #up()},
 * {@link #down()} or {@link #toggle()}, so the servos do not jump at INIT.
 * <p>
 * <b>Rule gate.</b> {@link #setDownGate(BooleanSupplier)} can block LOWERING (for example
 * {@code matchTimer::canPlaceNectar}); {@link #down()} then returns false. Raising is always
 * allowed. TeleOp wires the gate only while {@code LOCK_DOWN_UNTIL_NECTAR_UNLOCK} is true.
 * <p>
 * <b>Optional:</b> if either servo is missing, {@link #isConnected()} is false and nothing moves.
 * Call {@link #periodic()} once per loop.
 */
public class FlowerSweeper implements Subsystem {

    /** What the bar is doing. */
    public enum State { NOT_SET, UP, DOWN, MOVING_UP, MOVING_DOWN }

    private static final double TRAVEL_UP = 0.0;
    private static final double TRAVEL_DOWN = 1.0;
    private static final double TRAVEL_HALF = 0.5;

    private final Servo leftServo;
    private final Servo rightServo;
    private final MatchTimer.Clock clock;

    private BooleanSupplier downGate;
    private double travel = TRAVEL_UP;
    private double targetTravel = TRAVEL_UP;
    private boolean isCommanded = false;
    private double lastTimeSec = Double.NaN;
    private int refusedCount = 0;

    /** @param hardware robot hardware; the sweeper servos may be missing */
    public FlowerSweeper(RobotHardware hardware) {
        this(hardware.sweeperLeftServo, hardware.sweeperRightServo, MatchTimer.systemClock());
    }

    /**
     * For tests and tuning OpModes.
     *
     * @param leftServo  left servo, or null
     * @param rightServo right servo, or null
     * @param clock      time source
     */
    public FlowerSweeper(Servo leftServo, Servo rightServo, MatchTimer.Clock clock) {
        boolean isComplete = leftServo != null && rightServo != null;
        this.leftServo = isComplete ? leftServo : null;
        this.rightServo = isComplete ? rightServo : null;
        this.clock = clock;
    }

    /** @param gate returns true when lowering is allowed, or null for "always allowed" */
    public void setDownGate(BooleanSupplier gate) {
        downGate = gate;
    }

    /** @return true if the gate allows lowering the bar now */
    public boolean canLower() {
        return downGate == null || downGate.getAsBoolean();
    }

    /**
     * Lowers the bar to the floor, if the gate allows it.
     *
     * @return true if the bar is now going down
     */
    public boolean down() {
        if (!canLower()) {
            refusedCount++;
            return false;
        }
        command(TRAVEL_DOWN);
        return true;
    }

    /** Raises the bar (stowed). Always allowed. */
    public void up() {
        command(TRAVEL_UP);
    }

    /**
     * Lowers the bar if it is up or going up, raises it otherwise.
     *
     * @return false if lowering was refused by the gate
     */
    public boolean toggle() {
        if (!isCommanded || targetTravel < TRAVEL_HALF) {
            return down();
        }
        up();
        return true;
    }

    /** Moves the servos one step toward the target. Call once per loop. */
    @Override
    public void periodic() {
        double nowSec = clock.nowSeconds();
        double dtSec = Double.isNaN(lastTimeSec) ? 0.0
                : Math.min(nowSec - lastTimeSec, FlowerSweeperConstants.MOVE_MAX_DT_SECONDS);
        lastTimeSec = nowSec;
        if (!isCommanded) {
            return;
        }
        double maxStep = FlowerSweeperConstants.MOVE_SECONDS > 0.0
                ? dtSec / FlowerSweeperConstants.MOVE_SECONDS : TRAVEL_DOWN;
        double error = targetTravel - travel;
        // Snap onto the target on the last step, so "UP" / "DOWN" is exact
        travel = Math.abs(error) <= maxStep ? targetTravel : travel + Math.signum(error) * maxStep;
        apply();
    }

    /** Servos hold their position; nothing to stop. */
    @Override
    public void stop() {
    }

    /** @return the bar state */
    public State getState() {
        if (!isCommanded) {
            return State.NOT_SET;
        }
        boolean isGoingDown = targetTravel >= TRAVEL_HALF;
        if (travel == targetTravel) {
            return isGoingDown ? State.DOWN : State.UP;
        }
        return isGoingDown ? State.MOVING_DOWN : State.MOVING_UP;
    }

    /** @return true once the bar has reached the floor */
    public boolean isDown() {
        return getState() == State.DOWN;
    }

    /** @return true if both servos are configured */
    public boolean isConnected() {
        return leftServo != null;
    }

    /** @return how many lower requests the gate refused */
    public int getRefusedCount() {
        return refusedCount;
    }

    /** @return the left servo position for the current travel (0..1) */
    public double getLeftPosition() {
        return interpolate(FlowerSweeperConstants.LEFT_UP_POSITION,
                FlowerSweeperConstants.LEFT_DOWN_POSITION);
    }

    /** @return the right servo position for the current travel (0..1) */
    public double getRightPosition() {
        return interpolate(FlowerSweeperConstants.RIGHT_UP_POSITION,
                FlowerSweeperConstants.RIGHT_DOWN_POSITION);
    }

    @Override
    public void addTelemetry(Telemetry t) {
        if (!isConnected()) {
            t.addLine("Flower sweeper: servos not configured (sweeperLeftServo / sweeperRightServo)");
            return;
        }
        t.addData("Flower sweeper", "%s (L %.3f, R %.3f)%s", getState(), getLeftPosition(),
                getRightPosition(), canLower() ? "" : " - lowering locked until endgame");
    }

    private void command(double newTarget) {
        if (!isCommanded) {
            // First command: the position is unknown, so go straight there once (the servo moves
            // at its own speed) and slow down every move after that
            travel = newTarget;
            isCommanded = true;
        }
        targetTravel = newTarget;
        apply();
    }

    private double interpolate(double upPosition, double downPosition) {
        return upPosition + (downPosition - upPosition) * travel;
    }

    private void apply() {
        if (leftServo == null) {
            return;
        }
        leftServo.setPosition(getLeftPosition());
        rightServo.setPosition(getRightPosition());
    }
}
