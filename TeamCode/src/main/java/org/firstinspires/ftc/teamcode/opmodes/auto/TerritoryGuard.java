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
 * rotation brings it back). In that frame our territory is {@code x < TERRITORY_LINE_X}. The
 * guard trips when the robot center is past the line minus half the robot width (plus
 * {@link AutoConstants#TERRITORY_GUARD_MARGIN_INCHES}), meaning its side touches the line.
 * Every competition path keeps the center at x 57 or less (tested by {@code PathsTest}), so the
 * guard only trips on a fault, for example a bad odometry reading.
 */
public final class TerritoryGuard {

    private TerritoryGuard() {
    }

    /** @return largest allowed robot-center x in the RED frame */
    public static double getMaxCenterX() {
        return FieldConstants.TERRITORY_LINE_X - RobotConstants.ROBOT_WIDTH_INCHES / 2.0
                + AutoConstants.TERRITORY_GUARD_MARGIN_INCHES;
    }

    /**
     * @param fieldPose robot pose on the real field (as the follower reads it)
     * @param alliance  our alliance
     * @return true if the robot touches or crosses into the opponent's territory
     */
    public static boolean isPastLine(Pose fieldPose, Alliance alliance) {
        Pose redPose = alliance.toField(fieldPose);
        return redPose.x() > getMaxCenterX();
    }
}
