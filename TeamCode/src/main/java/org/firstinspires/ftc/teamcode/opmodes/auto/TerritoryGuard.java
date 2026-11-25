// Sources: new for IronLion30311-HiveMind (G402 runtime guard; FieldConstants.TERRITORY_LINE_X)
package org.firstinspires.ftc.teamcode.opmodes.auto;

import com.pedropathing.math.Pose;

import org.firstinspires.ftc.teamcode.config.AutoConstants;
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
}
