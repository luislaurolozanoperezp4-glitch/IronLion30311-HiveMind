// Sources: IronLion30311BIOBUZZInicioDeTemporada / branch feature/turrubiates/vision-align / TeamCode/.../opmodes/auto/PedroAutoBase.java @ a7b8f4c (collectBalls: center the closest ball, drive on, keep going after it leaves the view)
//          — adapted for BIOBUZZ (blocking loop -> non-blocking; stops at the G407 limit; outputs drive commands instead of driving)
package org.firstinspires.ftc.teamcode.statemachine;

import org.firstinspires.ftc.teamcode.config.IntakeConstants;
import org.firstinspires.ftc.teamcode.subsystems.BallColor;
import org.firstinspires.ftc.teamcode.subsystems.ColorSensing;
import org.firstinspires.ftc.teamcode.subsystems.ElementCounter;
import org.firstinspires.ftc.teamcode.subsystems.Intake;
import org.firstinspires.ftc.teamcode.util.MatchTimer;

/**
 * Drives onto the balls the HuskyLens sees, with the intake on, as in the tested AUTO.
 * <p>
 * Each loop it turns to center the closest ball and drives forward. When the ball disappears
 * under the camera (into the intake) it keeps going
 * {@link IntakeConstants#COLLECT_BALL_LOST_DRIVE_SECONDS}, then looks for the next one. It ends
 * when no ball is in view, after {@code maxSec}, or when the robot controls the maximum number
 * of elements (G407).
 * <p>
 * It does not move the wheels itself (in AUTO, Pedro Pathing owns them): read
 * {@link #getForwardCommand()} and {@link #getTurnCommand()} every loop and pass them to the
 * follower ({@code follower.manual(...)}) or the Drivetrain.
 * <p>
 * Use: {@link #start(double, BallColor...)}, then {@link #periodic()} every loop until
 * {@link #isDone()}.
 */
public class CollectSequence {

    /** The steps. */
    public enum State { IDLE, SEARCHING, APPROACHING, INTO_INTAKE, DONE }

    /** Why the sequence ended. */
    public enum Result { NONE, NO_BALL_IN_VIEW, TIME_UP, FULL, CANCELLED }

    /** What the sequence needs from the robot. {@link #forRobot} builds it from the subsystems. */
    public interface Inputs {
        /**
         * @param colors colors to collect; none means any
         * @return the closest ball of those colors, or null
         */
        ColorSensing.Ball getClosestBall(BallColor... colors);

        /** @return balls the intake picked up so far */
        int getTotalBalls();

        /** @return false at the G407 limit */
        boolean canCollectMore();

        /** @param isOn true to run the intake inward, false to stop it */
        void setIntake(boolean isOn);
    }

    private final Inputs inputs;
    private final MatchTimer.Clock clock;
    private final StateMachine<State> machine;

    private BallColor[] colors = new BallColor[0];
    private double maxSec;
    private double startSec;
    private double lastBallSeenSec;
    private boolean hasSeenBall;
    private int startBallCount;
    private double forwardCommand;
    private double turnCommand;
    private Result result = Result.NONE;

    /**
     * @param inputs what to read and drive (see {@link #forRobot})
     * @param clock  time source
     */
    public CollectSequence(Inputs inputs, MatchTimer.Clock clock) {
        this.inputs = inputs;
        this.clock = clock;
        this.machine = new StateMachine<>(State.IDLE, clock);
    }

    /**
     * Builds the sequence for the real robot.
     *
     * @param colorSensing the HuskyLens subsystem
     * @param intake       the intake
     * @param counter      the G407 element counter (may be null)
     * @return a sequence that uses these subsystems
     */
    public static CollectSequence forRobot(final ColorSensing colorSensing, final Intake intake,
                                           final ElementCounter counter) {
        Inputs inputs = new Inputs() {
            @Override
            public ColorSensing.Ball getClosestBall(BallColor... wanted) {
                return colorSensing.getClosestBall(wanted);
            }

            @Override
            public int getTotalBalls() {
                return colorSensing.getTotalBalls();
            }

            @Override
            public boolean canCollectMore() {
                return counter == null || counter.canCollectMore();
            }

            @Override
            public void setIntake(boolean isOn) {
                if (isOn) {
                    intake.collect();
                } else {
                    intake.idle();
                }
                colorSensing.countWhileIntaking(isOn);
            }
        };
        return new CollectSequence(inputs, MatchTimer.systemClock());
    }

    /**
     * Starts collecting.
     *
     * @param maxSec longest time to spend
     * @param wanted ball colors to collect; none means any color
     */
    public void start(double maxSec, BallColor... wanted) {
        this.maxSec = maxSec;
        colors = wanted;
        startSec = clock.nowSeconds();
        hasSeenBall = false;
        lastBallSeenSec = startSec;
        startBallCount = inputs.getTotalBalls();
        result = Result.NONE;
        machine.restartState(State.SEARCHING);
    }

    /** Runs one step. Call once per loop while it is not done. */
    public void periodic() {
        if (machine.isIn(State.IDLE) || machine.isIn(State.DONE)) {
            return;
        }
        double nowSec = clock.nowSeconds();
        if (nowSec - startSec >= maxSec) {
            finish(Result.TIME_UP);
            return;
        }
        if (!inputs.canCollectMore()) {
            finish(Result.FULL);
            return;
        }
        ColorSensing.Ball ball = inputs.getClosestBall(colors);
        if (ball != null) {
            hasSeenBall = true;
            lastBallSeenSec = nowSec;
            forwardCommand = IntakeConstants.COLLECT_APPROACH_POWER;
            turnCommand = Math.max(-IntakeConstants.COLLECT_MAX_TURN, Math.min(
                    IntakeConstants.COLLECT_MAX_TURN, IntakeConstants.COLLECT_TURN_KP * ball.xOffset));
            machine.setState(State.APPROACHING);
        } else if (hasSeenBall
                && nowSec - lastBallSeenSec < IntakeConstants.COLLECT_BALL_LOST_DRIVE_SECONDS) {
            // The ball is going under the camera into the intake
            forwardCommand = IntakeConstants.COLLECT_APPROACH_POWER;
            turnCommand = 0.0;
            machine.setState(State.INTO_INTAKE);
        } else {
            finish(Result.NO_BALL_IN_VIEW);
            return;
        }
        inputs.setIntake(true);
    }

    /** Stops right away. */
    public void cancel() {
        if (isRunning()) {
            finish(Result.CANCELLED);
        }
    }

    private void finish(Result why) {
        result = why;
        forwardCommand = 0.0;
        turnCommand = 0.0;
        inputs.setIntake(false);
        machine.setState(State.DONE);
    }

    /** @return true when collecting is over */
    public boolean isDone() {
        return machine.isIn(State.DONE);
    }

    /** @return true between {@link #start} and the end */
    public boolean isRunning() {
        return !machine.isIn(State.IDLE) && !machine.isIn(State.DONE);
    }

    /** @return the current step */
    public State getState() {
        return machine.getState();
    }

    /** @return why it ended ({@link Result#NONE} while running) */
    public Result getResult() {
        return result;
    }

    /** @return forward power to drive this loop (+ = forward, toward the intake) */
    public double getForwardCommand() {
        return forwardCommand;
    }

    /** @return turn power to drive this loop (+ = clockwise) */
    public double getTurnCommand() {
        return turnCommand;
    }

    /** @return balls the HuskyLens counted since {@link #start} */
    public int getBallsCollected() {
        return inputs.getTotalBalls() - startBallCount;
    }
}
