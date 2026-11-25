// Sources: new for IronLion30311-HiveMind (robot footprint checks for BIOBUZZ manual TU10 G304, LEAVE and PARK)
package org.firstinspires.ftc.teamcode.testutil;

import com.pedropathing.math.Pose;

import org.firstinspires.ftc.teamcode.config.FieldConstants;
import org.firstinspires.ftc.teamcode.config.RobotConstants;

/**
 * Robot footprint geometry for rule tests. The robot is a {@code ROBOT_LENGTH x ROBOT_WIDTH}
 * rectangle turned to the pose heading; its axis-aligned box is used (exact at 0/90/180/270
 * degrees, conservative in between). Poses are in the RED frame.
 */
public final class RuleGeometry {

    private RuleGeometry() {
    }

    /** @return half the footprint size along field x */
    public static double halfX(Pose pose) {
        return RobotConstants.ROBOT_LENGTH_INCHES / 2.0 * Math.abs(Math.cos(pose.heading()))
                + RobotConstants.ROBOT_WIDTH_INCHES / 2.0 * Math.abs(Math.sin(pose.heading()));
    }

    /** @return half the footprint size along field y */
    public static double halfY(Pose pose) {
        return RobotConstants.ROBOT_LENGTH_INCHES / 2.0 * Math.abs(Math.sin(pose.heading()))
                + RobotConstants.ROBOT_WIDTH_INCHES / 2.0 * Math.abs(Math.cos(pose.heading()));
    }

    /** @return smallest gap between the footprint and any perimeter wall (negative = overlap) */
    public static double wallGap(Pose pose) {
        double size = FieldConstants.FIELD_SIZE_INCHES;
        double left = pose.x() - halfX(pose);
        double right = size - (pose.x() + halfX(pose));
        double bottom = pose.y() - halfY(pose);
        double top = size - (pose.y() + halfY(pose));
        return Math.min(Math.min(left, right), Math.min(bottom, top));
    }

    /** @return true if any part of the footprint is in the RED LOADING ZONE (tape included) */
    public static boolean overlapsRedLoadingZone(Pose pose) {
        return pose.x() - halfX(pose) <= FieldConstants.RED_LOADING_ZONE_MAX_X
                && pose.x() + halfX(pose) >= FieldConstants.RED_LOADING_ZONE_MIN_X
                && pose.y() - halfY(pose) <= FieldConstants.RED_LOADING_ZONE_MAX_Y
                && pose.y() + halfY(pose) >= FieldConstants.RED_LOADING_ZONE_MIN_Y;
    }

    /** @return true if the footprint is fully on the red half (columns A-C) */
    public static boolean isOnRedHalf(Pose pose) {
        return pose.x() + halfX(pose) <= FieldConstants.TERRITORY_LINE_X;
    }

    /** @return true if the footprint comes within the keep-out radius of the FLOWER center */
    public static boolean touchesFlower(Pose pose, Pose flower) {
        double dx = Math.max(Math.abs(flower.x() - pose.x()) - halfX(pose), 0.0);
        double dy = Math.max(Math.abs(flower.y() - pose.y()) - halfY(pose), 0.0);
        return Math.hypot(dx, dy) < FieldConstants.FLOWER_KEEP_OUT_RADIUS_INCHES;
    }
}
