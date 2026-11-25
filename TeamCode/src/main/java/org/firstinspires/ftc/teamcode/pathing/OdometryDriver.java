// Sources: IronLion30311BIOBUZZInicioDeTemporada / branch feature/turrubiates/vision-align / TeamCode/.../opmodes/auto/PedroAutoBase.java @ a7b8f4c (driveTo, driveThrough, driveLeg, turnTo, settle, holdTarget, holdStep, checkOdometry, startPoseErrorIn, startHeadingErrorDeg, driveRobotCentric) @ 28ed859 (Pinpoint reboot recovery in checkOdometry)
//          — adapted for BIOBUZZ (blocking while-loops turned into a non-blocking step API)
package org.firstinspires.ftc.teamcode.pathing;

import com.pedropathing.follower.Follower;
import com.pedropathing.math.Pose;

import org.firstinspires.ftc.teamcode.config.DriveConstants;
import org.firstinspires.ftc.teamcode.util.MatchTimer;

import java.util.Locale;

/**
 * Point-to-point driver on the Pinpoint odometry: the controller that scored 4/4 preload shots
 * and parked in the field test of 2026-11-23 (vision-align {@code PreloadRouteAuto}).
 * <p>
 * It does not use Pedro path following (its gains are still untuned). It reads the pose from the
 * follower and drives through {@code follower.manual()}, so Pedro keeps tracking the pose.
 * <p>
 * <b>Non-blocking.</b> Start a move, then call {@link #periodic()} once per loop until
 * {@link #isDone()}:
 * <pre>{@code
 * driver.driveTo(PreloadParkPaths.SHOOT_SPOT);      // start the move
 * // in loop():
 * follower.update();                                // once per loop, BEFORE periodic()
 * driver.periodic();
 * if (driver.isDone()) { ... driver.getResult() ... }
 * }</pre>
 * Moves: {@link #driveTo} (stop at the target), {@link #driveThrough} (pass a waypoint without
 * stopping), {@link #turnTo} (turn in place), {@link #hold} (stay on the last target).
 * <p>
 * <b>Safety guards</b> (all TESTED on the robot):
 * <ul>
 *     <li>Pinpoint reboot: a jump to exactly (0, 0, 0) is the Pinpoint rebooting. The last good
 *     pose is written back and the move goes on, up to
 *     {@code PINPOINT_RESET_MAX_RECOVERIES} times (vision-align 28ed859).</li>
 *     <li>Pose jump: any other move of more than {@code POSE_JUMP_LIMIT_*} in one loop means
 *     the odometry is lost. From then on the driver only commands zero power, for the rest of
 *     the OpMode.</li>
 *     <li>Start pose: {@link #checkStartPose(Pose)} re-zeroes a small error (the robot was moved
 *     after INIT) and refuses to drive with a large one (a pod read 5 m off before moving).</li>
 *     <li>Every leg and turn has a timeout.</li>
 * </ul>
 * Sign convention of {@link DriveBase#drive}: + forward = toward the intake, + strafe = right,
 * + turn = clockwise. Pedro heading is counter-clockwise from +x.
 */
public class OdometryDriver {

    /** What the driver needs from the robot. {@link #fromFollower(Follower)} wraps Pedro. */
    public interface DriveBase {
        /** @return the current field pose (inches, radians); already updated this loop */
        Pose getPose();

        /** Sets the odometry pose (used to re-zero at start). */
        void setPose(Pose pose);

        /**
         * Commands robot-centric powers.
         *
         * @param forward + = forward (toward the intake)
         * @param strafe  + = right
         * @param turn    + = clockwise
         */
        void drive(double forward, double strafe, double turn);
    }

    /** What the driver is doing. */
    public enum State {
        IDLE,
        DRIVING,
        TURNING,
        HOLDING
    }

    /** How the last move ended. */
    public enum Result {
        /** No move has ended yet, or one is still running. */
        NONE,
        /** Ended within the tolerance of the target. */
        REACHED,
        /** Went past the target; a {@link #hold} brings it back gently. */
        PASSED,
        /** Pass-through waypoint reached; the speed is handed to the next leg. */
        PASSED_THROUGH,
        /** Ran out of time. */
        TIMED_OUT,
        /** The odometry jumped or was bad at start: the drive is stopped for good. */
        ODOMETRY_LOST,
        /** {@link #cancel()} was called. */
        CANCELLED
    }

    /** Outcome of {@link #checkStartPose(Pose)}. */
    public enum StartCheck {
        /** The odometry reads the start pose. */
        OK,
        /** Small error: the pose was set back to the start pose. */
        REZEROED,
        /** Large error: odometry fault, the driver will not move the robot. */
        FAULT
    }

    private final DriveBase base;
    private final MatchTimer.Clock clock;

    private State state = State.IDLE;
    private Result result = Result.NONE;
    private String resultText = "";
    private String moveName = "";

    private Pose target;
    private Pose legStart;
    private double legDirX;
    private double legDirY;
    private double maxPower;
    private double floorPower;
    private double toleranceInches;
    private boolean isPassThrough;
    private double timeoutSeconds;
    private double holdEndSeconds;

    private double moveStartSeconds;
    private double lastLoopSeconds = Double.NaN;
    private double power;
    private double carriedPower;
    private double carriedPowerSeconds = Double.NEGATIVE_INFINITY;

    private boolean isOdometryLost;
    private boolean isGuardArmed;
    private Pose lastPose;
    private Pose currentPose;
    private boolean isPoseFresh;
    private int resetRecoveries;
    private String lastRecoveryText = "";

    /**
     * @param base  the robot drive (use {@link #fromFollower(Follower)} on the robot)
     * @param clock time source (use {@link MatchTimer#systemClock()} on the robot)
     */
    public OdometryDriver(DriveBase base, MatchTimer.Clock clock) {
        this.base = base;
        this.clock = clock;
    }

    /**
     * Wraps a Pedro follower. Pedro's mixing uses + strafe = left and + turn = counter-clockwise,
     * so both are negated here. The OpMode must call {@code follower.update()} once per loop.
     *
     * @param follower the follower from {@link PedroConstants#createFollower}
     * @return a drive base for this driver
     */
    public static DriveBase fromFollower(final Follower follower) {
        return new DriveBase() {
            @Override
            public Pose getPose() {
                return follower.pose();
            }

            @Override
            public void setPose(Pose pose) {
                follower.setPose(pose);
            }

            @Override
            public void drive(double forward, double strafe, double turn) {
                follower.manual(forward, -strafe, -turn);
            }
        };
    }

    // ------------------------------------------------------------------------------------------
    // Start of the routine
    // ------------------------------------------------------------------------------------------

    /**
     * Checks the odometry against the expected start pose when START is pressed, and arms the
     * pose-jump guard. A small error (within {@code START_REZERO_MAX_*}) means the robot was
     * placed after INIT: the pose is set back to {@code startPose}. A larger one is a fault: the
     * driver then refuses to move (mechanisms can still run).
     *
     * @param startPose the pose the robot should be on (already converted for the alliance)
     * @return what was found
     */
    public StartCheck checkStartPose(Pose startPose) {
        Pose pose = base.getPose();
        double errorInches = distance(pose, startPose);
        double errorDegrees = Math.abs(Math.toDegrees(angleError(startPose.heading(), pose.heading())));
        StartCheck check;
        if (errorInches <= DriveConstants.START_POSE_TOLERANCE_INCHES
                && errorDegrees <= DriveConstants.START_HEADING_TOLERANCE_DEGREES) {
            check = StartCheck.OK;
        } else if (errorInches <= DriveConstants.START_REZERO_MAX_INCHES
                && errorDegrees <= DriveConstants.START_REZERO_MAX_DEGREES) {
            base.setPose(startPose);
            check = StartCheck.REZEROED;
        } else {
            isOdometryLost = true;
            check = StartCheck.FAULT;
        }
        resultText = String.format(Locale.US, "Start check %s: %.1f in, %.1f deg off",
                check, errorInches, errorDegrees);
        lastPose = check == StartCheck.REZEROED ? startPose : pose;
        isGuardArmed = true;
        return check;
    }

    /**
     * Arms the pose-jump guard without a start check (for test OpModes).
     */
    public void armPoseJumpGuard() {
        lastPose = base.getPose();
        isGuardArmed = true;
    }

    // ------------------------------------------------------------------------------------------
    // Moves
    // ------------------------------------------------------------------------------------------

    /**
     * Starts a straight leg to {@code target} at {@code AUTO_MAX_POWER}, stopping at it.
     *
     * @param target field pose to drive to; the heading is turned to on the way
     */
    public void driveTo(Pose target) {
        driveTo("Drive", target, DriveConstants.AUTO_MAX_POWER);
    }

    /**
     * Starts a straight leg to {@code target}, stopping at it.
     *
     * @param name     leg name for telemetry and logs
     * @param target   field pose to drive to
     * @param maxPower top power for this leg (0 to 1), for example slow while collecting
     */
    public void driveTo(String name, Pose target, double maxPower) {
        startLeg(name, target, maxPower, false);
    }

    /**
     * Starts a leg to a waypoint the robot passes without stopping: it keeps at least
     * {@code AUTO_CORNER_POWER}, ends {@code AUTO_PASS_THROUGH_INCHES} before the waypoint and
     * hands its speed to the next leg. Start the next leg right after this one is done.
     *
     * @param name   leg name for telemetry and logs
     * @param target waypoint to pass
     */
    public void driveThrough(String name, Pose target) {
        driveThrough(name, target, DriveConstants.AUTO_MAX_POWER);
    }

    /**
     * Same as {@link #driveThrough(String, Pose)} with another top power (for example a faster
     * park when time is short).
     *
     * @param name     leg name for telemetry and logs
     * @param target   waypoint to pass
     * @param maxPower top power for this leg (0 to 1)
     */
    public void driveThrough(String name, Pose target, double maxPower) {
        startLeg(name, target, maxPower, true);
    }

    /**
     * Starts an in-place turn to a field heading. Afterwards {@link #hold} keeps the position of
     * the last target with the new heading.
     *
     * @param name           turn name for telemetry and logs
     * @param headingRadians target field heading (counter-clockwise from +x)
     */
    public void turnTo(String name, double headingRadians) {
        Pose pose = base.getPose();
        Pose anchor = target != null ? target : pose;
        target = new Pose(anchor.x(), anchor.y(), headingRadians);
        legStart = pose;
        moveName = name;
        state = State.TURNING;
        result = Result.NONE;
        timeoutSeconds = DriveConstants.TURN_TIMEOUT_SECONDS;
        moveStartSeconds = clock.nowSeconds();
    }

    /**
     * Holds the last target for {@code seconds} (gentle correction if the robot slid). Use it to
     * settle between legs and to stay parked until the end of AUTO.
     *
     * @param seconds how long to hold; {@link Double#POSITIVE_INFINITY} holds until cancelled
     */
    public void hold(double seconds) {
        moveName = "Hold";
        state = State.HOLDING;
        result = Result.NONE;
        moveStartSeconds = clock.nowSeconds();
        holdEndSeconds = moveStartSeconds + seconds;
    }

    /** Stops the current move and commands zero power. */
    public void cancel() {
        if (state != State.IDLE) {
            finish(Result.CANCELLED, true);
        }
    }

    /**
     * Reads the pose and runs the guards (Pinpoint reboot recovery, pose jump). Call it once per
     * loop after {@code follower.update()} when the OpMode needs the checked pose before
     * {@link #periodic()} (for example for its own guards); {@link #periodic()} then reuses it.
     *
     * @return the pose to use this loop (the restored pose after a recovered Pinpoint reboot)
     */
    public Pose updatePose() {
        currentPose = checkPoseJump(base.getPose());
        isPoseFresh = true;
        return currentPose;
    }

    /**
     * Stops the drive for good because the odometry cannot be trusted (for example the Pinpoint
     * reports a fault at START). Mechanisms are not affected.
     *
     * @param reason one line for the logs
     */
    public void lockOdometry(String reason) {
        isOdometryLost = true;
        resultText = "ODOMETRY LOCKED: " + reason;
    }

    /**
     * Runs one control step. Call it once per loop, after {@code follower.update()} (and after
     * {@link #updatePose()} if the OpMode calls it).
     */
    public void periodic() {
        double now = clock.nowSeconds();
        double dt = Double.isNaN(lastLoopSeconds) ? 0.0
                : Math.min(now - lastLoopSeconds, DriveConstants.AUTO_MAX_LOOP_SECONDS);
        lastLoopSeconds = now;

        Pose pose = isPoseFresh ? currentPose : updatePose();
        isPoseFresh = false;
        if (isOdometryLost) {
            base.drive(0.0, 0.0, 0.0);
            if (state != State.IDLE) {
                finish(Result.ODOMETRY_LOST, false);
            }
            return;
        }
        switch (state) {
            case DRIVING:
                stepDrive(pose, now, dt);
                break;
            case TURNING:
                stepTurn(pose, now);
                break;
            case HOLDING:
                stepHold(pose);
                if (now >= holdEndSeconds) {
                    finish(Result.REACHED, true);
                }
                break;
            default:
                break;
        }
    }

    // ------------------------------------------------------------------------------------------
    // Status
    // ------------------------------------------------------------------------------------------

    /** @return true while a move is running */
    public boolean isBusy() {
        return state != State.IDLE;
    }

    /** @return true when no move is running (the last one ended, see {@link #getResult()}) */
    public boolean isDone() {
        return state == State.IDLE;
    }

    /** @return what the driver is doing */
    public State getState() {
        return state;
    }

    /** @return how the last move ended ({@link Result#NONE} while one runs) */
    public Result getResult() {
        return result;
    }

    /** @return one line describing the last move (for telemetry and the pose log) */
    public String getResultText() {
        return resultText;
    }

    /** @return the pose the driver drives to or holds, or null */
    public Pose getTarget() {
        return target;
    }

    /** @return true once the odometry was found lost; the driver no longer moves the robot */
    public boolean isOdometryLost() {
        return isOdometryLost;
    }

    /** @return how many Pinpoint reboots were recovered (pose restored) since the start */
    public int getResetRecoveries() {
        return resetRecoveries;
    }

    /** @return the log line of the last recovered Pinpoint reboot, or "" */
    public String getLastRecoveryText() {
        return lastRecoveryText;
    }

    /** @return seconds since the current (or last) move started */
    public double getMoveSeconds() {
        return clock.nowSeconds() - moveStartSeconds;
    }

    // ------------------------------------------------------------------------------------------
    // Control steps (same math as the tested blocking version)
    // ------------------------------------------------------------------------------------------

    private void startLeg(String name, Pose legTarget, double legMaxPower, boolean shouldPassThrough) {
        Pose from = base.getPose();
        moveName = name;
        target = legTarget;
        legStart = from;
        maxPower = legMaxPower;
        isPassThrough = shouldPassThrough;
        double distanceInches = distance(from, legTarget);
        // A slow leg (maxPower below the minimum) drives at maxPower the whole way
        double minPower = Math.min(DriveConstants.AUTO_MIN_POWER, legMaxPower);
        // Slower legs get proportionally more time
        timeoutSeconds = DriveConstants.DRIVE_TIMEOUT_BASE_SECONDS + distanceInches
                / (DriveConstants.DRIVE_TIMEOUT_INCHES_PER_SECOND
                * Math.min(1.0, legMaxPower / DriveConstants.AUTO_MAX_POWER));
        legDirX = distanceInches > 0.0 ? (legTarget.x() - from.x()) / distanceInches : 0.0;
        legDirY = distanceInches > 0.0 ? (legTarget.y() - from.y()) / distanceInches : 0.0;
        toleranceInches = shouldPassThrough ? DriveConstants.AUTO_PASS_THROUGH_INCHES
                : DriveConstants.AUTO_TOLERANCE_INCHES;
        floorPower = shouldPassThrough ? Math.min(legMaxPower, DriveConstants.AUTO_CORNER_POWER) : minPower;
        moveStartSeconds = clock.nowSeconds();
        // Continue at the speed a pass-through leg just ended with (only if it just ended)
        power = moveStartSeconds - carriedPowerSeconds < DriveConstants.AUTO_CARRIED_POWER_WINDOW_SECONDS
                ? carriedPower : 0.0;
        carriedPower = 0.0;
        state = State.DRIVING;
        result = Result.NONE;
    }

    private void stepDrive(Pose pose, double now, double dt) {
        double dx = target.x() - pose.x();
        double dy = target.y() - pose.y();
        double remainingInches = Math.hypot(dx, dy);
        if (remainingInches <= toleranceInches) {
            finishLeg(isPassThrough ? Result.PASSED_THROUGH : Result.REACHED, now);
            return;
        }
        // Past the target: end the leg and let the gentle hold bring it back, instead of
        // reversing at full power (it overshot the LEAVE and drove back into the wall)
        if (dx * legDirX + dy * legDirY < 0.0) {
            finishLeg(isPassThrough ? Result.PASSED_THROUGH : Result.PASSED, now);
            return;
        }
        if (now - moveStartSeconds > timeoutSeconds) {
            finish(Result.TIMED_OUT, true);
            return;
        }
        // Field error to the robot frame (Pedro heading: counter-clockwise from +x)
        double cos = Math.cos(pose.heading());
        double sin = Math.sin(pose.heading());
        double forwardInches = dx * cos + dy * sin;
        double rightInches = dx * sin - dy * cos;

        double wanted = clip(maxPower * remainingInches / DriveConstants.AUTO_SLOW_ZONE_INCHES,
                floorPower, maxPower);
        // Ramp up gently (no jerk at the start); slowing down is immediate
        power = Math.min(wanted, power + DriveConstants.AUTO_ACCEL_PER_SECOND * dt);
        base.drive(power * forwardInches / remainingInches, power * rightInches / remainingInches,
                headingTurn(pose));
    }

    private void stepTurn(Pose pose, double now) {
        double errorRadians = angleError(target.heading(), pose.heading());
        if (Math.abs(Math.toDegrees(errorRadians)) <= DriveConstants.TURN_TOLERANCE_DEGREES) {
            finish(Result.REACHED, true);
            return;
        }
        if (now - moveStartSeconds > timeoutSeconds) {
            finish(Result.TIMED_OUT, true);
            return;
        }
        double turnPower = clip(DriveConstants.TURN_KP * Math.abs(errorRadians),
                DriveConstants.TURN_MIN_POWER, DriveConstants.TURN_MAX_POWER);
        // errorRadians is counter-clockwise; + turn is clockwise
        base.drive(0.0, 0.0, -Math.signum(errorRadians) * turnPower);
    }

    private void stepHold(Pose pose) {
        if (target == null) {
            base.drive(0.0, 0.0, 0.0);
            return;
        }
        double dx = target.x() - pose.x();
        double dy = target.y() - pose.y();
        double errorInches = Math.hypot(dx, dy);
        double forward = 0.0;
        double strafe = 0.0;
        if (errorInches > DriveConstants.HOLD_DEADBAND_INCHES) {
            double holdPower = Math.min(DriveConstants.HOLD_KP * errorInches,
                    DriveConstants.HOLD_MAX_POWER);
            double cos = Math.cos(pose.heading());
            double sin = Math.sin(pose.heading());
            forward = holdPower * (dx * cos + dy * sin) / errorInches;
            strafe = holdPower * (dx * sin - dy * cos) / errorInches;
        }
        base.drive(forward, strafe, headingTurn(pose));
    }

    /** Turn power that holds (or reaches) the target heading while driving. */
    private double headingTurn(Pose pose) {
        double headingError = angleError(target.heading(), pose.heading());
        // headingError is counter-clockwise; + turn is clockwise
        return clip(-DriveConstants.AUTO_HEADING_KP * headingError,
                -DriveConstants.AUTO_MAX_TURN, DriveConstants.AUTO_MAX_TURN);
    }

    private void finishLeg(Result legResult, double now) {
        if (legResult == Result.PASSED_THROUGH) {
            carriedPower = power;
            carriedPowerSeconds = now;
            finish(legResult, false);
        } else {
            finish(legResult, true);
        }
    }

    private void finish(Result endResult, boolean isStopping) {
        if (isStopping) {
            base.drive(0.0, 0.0, 0.0);
        }
        Pose end = base.getPose();
        double errorInches = target != null ? distance(end, target) : 0.0;
        double movedInches = legStart != null ? distance(legStart, end) : 0.0;
        resultText = String.format(Locale.US, "%s: %s, moved %.1f in, %.1f in from target, %.1f s",
                moveName, endResult, movedInches, errorInches, clock.nowSeconds() - moveStartSeconds);
        result = endResult;
        state = State.IDLE;
    }

    /**
     * Checks every pose read for jumps the robot cannot make in one loop. A jump to (0, 0, 0) is
     * a Pinpoint reboot: the last good pose is written back (up to
     * {@code PINPOINT_RESET_MAX_RECOVERIES} times). Any other jump marks the odometry as lost.
     *
     * @return the pose to use (the restored one after a recovered reboot)
     */
    private Pose checkPoseJump(Pose pose) {
        if (!isGuardArmed || isOdometryLost) {
            lastPose = pose;
            return pose;
        }
        if (lastPose != null) {
            double jumpInches = distance(pose, lastPose);
            double jumpDegrees = Math.abs(Math.toDegrees(angleError(pose.heading(), lastPose.heading())));
            boolean isJump = jumpInches > DriveConstants.POSE_JUMP_LIMIT_INCHES
                    || jumpDegrees > DriveConstants.POSE_JUMP_LIMIT_DEGREES;
            if (isJump && isPinpointResetPose(pose)
                    && resetRecoveries < DriveConstants.PINPOINT_RESET_MAX_RECOVERIES) {
                resetRecoveries++;
                base.setPose(lastPose);
                lastRecoveryText = String.format(Locale.US,
                        "PINPOINT RESET recovered (%d): pose restored to (%.1f, %.1f, %.0f deg)",
                        resetRecoveries, lastPose.x(), lastPose.y(), Math.toDegrees(lastPose.heading()));
                return lastPose;
            }
            if (isJump) {
                isOdometryLost = true;
                resultText = String.format(Locale.US,
                        "ODOMETRY LOST: pose jumped %.1f in / %.0f deg; drive stopped",
                        jumpInches, jumpDegrees);
            }
        }
        lastPose = pose;
        return pose;
    }

    /**
     * @param pose a pose read from the odometry
     * @return true if it looks like a Pinpoint that rebooted: (0, 0) with heading 0
     */
    public static boolean isPinpointResetPose(Pose pose) {
        return Math.abs(pose.x()) < DriveConstants.PINPOINT_RESET_POSITION_INCHES
                && Math.abs(pose.y()) < DriveConstants.PINPOINT_RESET_POSITION_INCHES
                && Math.abs(Math.toDegrees(angleError(pose.heading(), 0.0)))
                < DriveConstants.PINPOINT_RESET_HEADING_DEGREES;
    }

    // ------------------------------------------------------------------------------------------
    // Math helpers (public so tests and other classes can share them)
    // ------------------------------------------------------------------------------------------

    /**
     * @return straight-line distance between two poses, in inches
     */
    public static double distance(Pose a, Pose b) {
        return Math.hypot(a.x() - b.x(), a.y() - b.y());
    }

    /**
     * @param targetRadians  wanted heading
     * @param currentRadians current heading
     * @return the shortest signed turn from current to target, in (-pi, pi], counter-clockwise +
     */
    public static double angleError(double targetRadians, double currentRadians) {
        return Math.atan2(Math.sin(targetRadians - currentRadians),
                Math.cos(targetRadians - currentRadians));
    }

    private static double clip(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
