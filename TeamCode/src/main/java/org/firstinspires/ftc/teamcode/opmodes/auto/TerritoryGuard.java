// Sources: new for IronLion30311-HiveMind (G402 runtime guard; FieldConstants.TERRITORY_LINE_X)
package org.firstinspires.ftc.teamcode.opmodes.auto;

import com.pedropathing.math.Pose;

import org.firstinspires.ftc.teamcode.config.AutoConstants;
import org.firstinspires.ftc.teamcode.config.DriveConstants;
import org.firstinspires.ftc.teamcode.config.FieldConstants;
import org.firstinspires.ftc.teamcode.config.RobotConstants;
import org.firstinspires.ftc.teamcode.util.Alliance;

/**
 * G402 runtime check: in AUTO the robot must not enter the opponent's territory.
 * <p>
 * The pose is first turned into the RED frame (BLUE = RED rotated 180 degrees, so the same
 * rotation brings it back). In that frame our territory is {@code x < TERRITORY_LINE_X}
 * (columns A-C, manual TU10 G402). The guard trips when the robot footprint reaches the line
 * (plus {@link AutoConstants#TERRITORY_GUARD_MARGIN_INCHES}). The footprint's reach in x depends
 * on the heading: half the width when square to the field, up to half the diagonal at 45 degrees.
 * Every competition path keeps the center at x 57 or less (tested by {@code PathsTest}), so the
 * guard only trips on a fault, for example a bad odometry reading.
 * <p>
 * G402 itself forbids disrupting the opponent's AUTO (controlling SCORING ELEMENTS on their
 * side, contacting their robot on their side); staying out of their half is the team's
 * conservative way to never do that.
 */
public final class TerritoryGuard {

    private TerritoryGuard() {
    }

    /** @return largest allowed robot-center x in the RED frame, robot square to the field (heading +-90) */
    public static double getMaxCenterX() {
        return getMaxCenterX(Math.PI / 2.0);
    }

    /**
     * @param heading robot heading in radians (either frame: the reach does not change by 180 degrees)
     * @return largest allowed robot-center x in the RED frame at that heading
     */
    public static double getMaxCenterX(double heading) {
        return FieldConstants.TERRITORY_LINE_X - getHalfReachX(heading)
                + AutoConstants.TERRITORY_GUARD_MARGIN_INCHES;
    }

    /**
     * @param heading robot heading in radians (0 = front toward +x)
     * @return how far the robot rectangle reaches in x from its center
     */
    public static double getHalfReachX(double heading) {
        return RobotConstants.ROBOT_LENGTH_INCHES / 2.0 * Math.abs(Math.cos(heading))
                + RobotConstants.ROBOT_WIDTH_INCHES / 2.0 * Math.abs(Math.sin(heading));
    }

    /**
     * @param fieldPose robot pose on the real field (as the follower reads it)
     * @param alliance  our alliance
     * @return true if the robot touches or crosses into the opponent's territory
     */
    public static boolean isPastLine(Pose fieldPose, Alliance alliance) {
        Pose redPose = alliance.toField(fieldPose);
        return redPose.x() > getMaxCenterX(redPose.heading());
    }

    /**
     * @param fieldPose robot pose on the real field
     * @param alliance  our alliance
     * @return true if the robot stays at least {@link AutoConstants#AIM_TERRITORY_SAFETY_INCHES}
     *         inside our territory, even if a turn ends {@code TURN_TOLERANCE_DEGREES} off
     */
    public static boolean isSafeWithMargin(Pose fieldPose, Alliance alliance) {
        Pose redPose = alliance.toField(fieldPose);
        double tolerance = Math.toRadians(DriveConstants.TURN_TOLERANCE_DEGREES);
        double[] headings = {redPose.heading() - tolerance, redPose.heading(), redPose.heading() + tolerance};
        for (double heading : headings) {
            if (redPose.x() > getMaxCenterX(heading) - AutoConstants.AIM_TERRITORY_SAFETY_INCHES) {
                return false;
            }
        }
        return true;
    }

    /**
     * Shrinks an aim turn until the turned robot (turn overshoot included) stays safely inside
     * our territory, so aiming can never trip the G402 guard (review finding #2).
     *
     * @param fieldPose   robot pose before the turn
     * @param turnDegrees wanted turn, degrees counter-clockwise
     * @param alliance    our alliance
     * @return the largest safe turn in the same direction (0 if no turn is safe)
     */
    public static double limitTurnDegrees(Pose fieldPose, double turnDegrees, Alliance alliance) {
        double turn = turnDegrees;
        while (Math.abs(turn) > 0.0) {
            Pose turned = new Pose(fieldPose.x(), fieldPose.y(), fieldPose.heading() + Math.toRadians(turn));
            if (isSafeWithMargin(turned, alliance)) {
                return turn;
            }
            double smaller = Math.abs(turn) - TURN_STEP_DEGREES;
            turn = smaller <= 0.0 ? 0.0 : Math.signum(turn) * smaller;
        }
        return 0.0;
    }

    /**
     * Shortens a move so its end stays safely inside our territory (G402).
     *
     * @param fieldStart  where the move starts
     * @param fieldTarget where the move wants to end
     * @param alliance    our alliance
     * @return {@code fieldTarget}, or the farthest safe point on the way to it
     *         ({@code fieldStart} if no part of the move is safe)
     */
    public static Pose limitMoveTarget(Pose fieldStart, Pose fieldTarget, Alliance alliance) {
        if (isSafeWithMargin(fieldTarget, alliance)) {
            return fieldTarget;
        }
        double safeFraction = 0.0;
        double unsafeFraction = 1.0;
        for (int i = 0; i < SEARCH_STEPS; i++) {
            double middle = (safeFraction + unsafeFraction) / 2.0;
            if (isSafeWithMargin(interpolate(fieldStart, fieldTarget, middle), alliance)) {
                safeFraction = middle;
            } else {
                unsafeFraction = middle;
            }
        }
        return interpolate(fieldStart, fieldTarget, safeFraction);
    }

    private static Pose interpolate(Pose start, Pose end, double fraction) {
        return new Pose(start.x() + (end.x() - start.x()) * fraction,
                start.y() + (end.y() - start.y()) * fraction, end.heading());
    }

    /** Turn reduction per try in {@link #limitTurnDegrees}, degrees. */
    private static final double TURN_STEP_DEGREES = 0.5;
    /** Bisection steps in {@link #limitMoveTarget} (2^-20 of the move: far below a millimeter). */
    private static final int SEARCH_STEPS = 20;
}
