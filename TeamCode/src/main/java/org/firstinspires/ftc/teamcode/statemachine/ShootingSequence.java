// Sources: IronLion30311BIOBUZZInicioDeTemporada / branch feature/turrubiates/vision-align / TeamCode/.../opmodes/auto/PedroAutoBase.java @ a7b8f4c (shootPollenPreload: feed lock after each shot, no-shot end, intake shake; applyMechanisms SHOOT_POLLEN)
//          IronLion30311BIOBUZZInicioDeTemporada / branch master / TeamCode/.../opmodes/base/PedroAutoBase.java @ 37b7a16 (shootWhenReady idea)
//          — adapted for BIOBUZZ (blocking while-loop -> non-blocking start()/periodic()/isDone())
package org.firstinspires.ftc.teamcode.statemachine;

import org.firstinspires.ftc.teamcode.config.IntakeConstants;
import org.firstinspires.ftc.teamcode.config.LauncherConstants;
import org.firstinspires.ftc.teamcode.subsystems.Indexer;
import org.firstinspires.ftc.teamcode.subsystems.Intake;
import org.firstinspires.ftc.teamcode.subsystems.Launcher;
import org.firstinspires.ftc.teamcode.util.MatchTimer;

import java.util.function.BooleanSupplier;

/**
 * Shoots a number of POLLEN balls, one per "ready" window, as in the tested 4/4 preload AUTO.
 * <p>
 * The balls wait in line: intake and indexer stay stopped until the flywheel reaches its target
 * RPM, then both feed. When a shot is detected (RPM dip) both stop again for at least
 * {@link LauncherConstants#SHOT_PAUSE_MIN_SECONDS} and until the flywheel is back at target, and
 * so on. If the indexer feeds for {@link IntakeConstants#SHAKE_WAIT_SECONDS} with no shot, a ball
 * is stuck before the indexer, so the intake reverses briefly (a "shake", at most
 * {@link IntakeConstants#SHAKE_MAX_TRIES} per volley).
 * <p>
 * It ends when all balls were shot, after {@code maxSec}, or when no other shot came
 * {@link LauncherConstants#SHOOT_NO_SHOT_END_SECONDS} after the last one (fewer balls than
 * expected). Then intake, indexer and the POLLEN flywheel are stopped.
 * <p>
 * Use: {@link #start(int, double, double)} once, then {@link #periodic()} every loop (before the
 * subsystems' own {@code periodic()}), until {@link #isDone()}.
 * <p>
 * Steps: {@code SPIN_UP -> FEED_ONE -> RECOVER -> WAIT_READY -> FEED_ONE ... -> DONE}.
 */
public class ShootingSequence {

    /** The steps of a volley. */
    public enum State {
        /** Not started yet. */
        IDLE,
        /** Flywheel spinning up for the first ball. */
        SPIN_UP,
        /** Feeding one ball (or shaking the intake to free it). */
        FEED_ONE,
        /** A shot just left: feeding locked for the minimum pause. */
        RECOVER,
        /** Pause over, waiting for the flywheel to be back at target RPM. */
        WAIT_READY,
        /** Finished; everything stopped. */
        DONE
    }

    /** What the sequence needs from the robot. {@link #forRobot} builds it from the subsystems. */
    public interface Mechanisms {
        /** @param rpm spin the POLLEN flywheel up to this RPM */
        void spinUp(double rpm);

        /** @return true while the POLLEN flywheel is ready to be fed */
        boolean isReady();

        /** @return POLLEN shots detected since the OpMode started */
        int getShotCount();

        /**
         * Sets intake and indexer for this loop.
         *
         * @param isFeeding true: intake pushes and indexer feeds; false: both wait
         * @param isShaking true: intake reverses, indexer waits
         */
        void setFeed(boolean isFeeding, boolean isShaking);

        /** Stops intake, indexer and the POLLEN flywheel. */
        void stopAll();
    }

    private final Mechanisms mechanisms;
    private final MatchTimer.Clock clock;
    private final StateMachine<State> machine;
    private BooleanSupplier aimedSupplier;

    private int ballsToShoot;
    private double maxSec;
    private double startSec;
    private int startShotCount;
    private int shotsFired;
    private boolean isFeedLocked;
    private boolean isShaking;
    private int shakeCount;
    private double lastShotSec;
    private double feedingSinceSec;
    private double shakeStartSec;
    private boolean hasBeenReady;

    /**
     * @param mechanisms what to drive (see {@link #forRobot})
     * @param clock      time source
     */
    public ShootingSequence(Mechanisms mechanisms, MatchTimer.Clock clock) {
        this.mechanisms = mechanisms;
        this.clock = clock;
        this.machine = new StateMachine<>(State.IDLE, clock);
    }

    /**
     * Builds the sequence for the real robot.
     *
     * @param launcher the launcher
     * @param indexer  the indexer
     * @param intake   the intake
     * @return a sequence that drives these subsystems
     */
    public static ShootingSequence forRobot(final Launcher launcher, final Indexer indexer,
                                            final Intake intake) {
        Mechanisms mechanisms = new Mechanisms() {
            @Override
            public void spinUp(double rpm) {
                launcher.setPollenTargetRpm(rpm);
                launcher.spinUpPollen();
            }

            @Override
            public boolean isReady() {
                return launcher.isPollenReady();
            }

            @Override
            public int getShotCount() {
                return launcher.getPollenShotCount();
            }

            @Override
            public void setFeed(boolean isFeeding, boolean isShakingNow) {
                if (isShakingNow) {
                    intake.eject();
                } else if (isFeeding) {
                    intake.pushToIndexer();
                } else {
                    intake.idle();
                }
                indexer.feed();
                indexer.setPaused(!isFeeding || isShakingNow);
            }

            @Override
            public void stopAll() {
                intake.idle();
                indexer.idle();
                indexer.setPaused(false);
                launcher.stopPollen();
            }
        };
        return new ShootingSequence(mechanisms, MatchTimer.systemClock());
    }

    /**
     * @param supplier returns true when the robot is aimed (for example the HIVE aligner);
     *                 null = always aimed (shoot from where the path ended)
     */
    public void setAimedSupplier(BooleanSupplier supplier) {
        aimedSupplier = supplier;
    }

    /**
     * Starts a volley.
     *
     * @param balls  how many balls to shoot
     * @param rpm    POLLEN flywheel target RPM (for example {@link LauncherConstants#POLLEN_AUTO_RPM})
     * @param maxSec longest time for the whole volley, spin-up included
     */
    public void start(int balls, double rpm, double maxSec) {
        ballsToShoot = balls;
        this.maxSec = maxSec;
        startSec = clock.nowSeconds();
        startShotCount = mechanisms.getShotCount();
        shotsFired = 0;
        isFeedLocked = false;
        isShaking = false;
        shakeCount = 0;
        lastShotSec = startSec;
        feedingSinceSec = startSec;
        hasBeenReady = false;
        mechanisms.spinUp(rpm);
        machine.restartState(State.SPIN_UP);
    }

    /** Runs one step. Call once per loop while it is not done. */
    public void periodic() {
        if (machine.isIn(State.IDLE) || machine.isIn(State.DONE)) {
            return;
        }
        double nowSec = clock.nowSeconds();

        int detected = mechanisms.getShotCount() - startShotCount;
        if (detected > shotsFired) {
            shotsFired = detected;
            isFeedLocked = true;
            isShaking = false;
            lastShotSec = nowSec;
            feedingSinceSec = nowSec;
        }

        boolean isOutOfTime = nowSec - startSec >= maxSec;
        boolean isOutOfBalls = shotsFired > 0 && !isFeedLocked
                && nowSec - lastShotSec > LauncherConstants.SHOOT_NO_SHOT_END_SECONDS;
        if (shotsFired >= ballsToShoot || isOutOfTime || isOutOfBalls) {
            finish();
            return;
        }

        boolean isReady = mechanisms.isReady();
        hasBeenReady |= isReady;
        if (isFeedLocked && nowSec - lastShotSec >= LauncherConstants.SHOT_PAUSE_MIN_SECONDS
                && isReady) {
            isFeedLocked = false;
        }
        if (isShaking && nowSec - shakeStartSec >= IntakeConstants.SHAKE_REVERSE_SECONDS) {
            isShaking = false;
            feedingSinceSec = nowSec;
        }

        boolean isAimed = aimedSupplier == null || aimedSupplier.getAsBoolean();
        boolean isFeeding = isAimed && !isFeedLocked && isReady && !isShaking;
        if (!isFeeding && !isShaking) {
            feedingSinceSec = nowSec;
        } else if (!isShaking && shakeCount < IntakeConstants.SHAKE_MAX_TRIES
                && nowSec - feedingSinceSec >= IntakeConstants.SHAKE_WAIT_SECONDS) {
            isShaking = true;
            isFeeding = false;
            shakeCount++;
            shakeStartSec = nowSec;
        }

        mechanisms.setFeed(isFeeding, isShaking);
        machine.setState(pickState(isFeeding));
    }

    private State pickState(boolean isFeeding) {
        if (isFeeding || isShaking) {
            return State.FEED_ONE;
        }
        if (isFeedLocked) {
            return clock.nowSeconds() - lastShotSec < LauncherConstants.SHOT_PAUSE_MIN_SECONDS
                    ? State.RECOVER : State.WAIT_READY;
        }
        return hasBeenReady ? State.WAIT_READY : State.SPIN_UP;
    }

    /** Stops the volley right away (for example when AUTO must go park). */
    public void cancel() {
        if (!machine.isIn(State.IDLE) && !machine.isIn(State.DONE)) {
            finish();
        }
    }

    private void finish() {
        isShaking = false;
        isFeedLocked = false;
        mechanisms.stopAll();
        machine.setState(State.DONE);
    }

    /** @return true when the volley is over */
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

    /** @return shots detected in this volley */
    public int getShotsFired() {
        return shotsFired;
    }

    /** @return intake shakes in this volley */
    public int getShakeCount() {
        return shakeCount;
    }

    /** @return seconds since {@link #start} */
    public double getElapsedSec() {
        return clock.nowSeconds() - startSec;
    }
}
