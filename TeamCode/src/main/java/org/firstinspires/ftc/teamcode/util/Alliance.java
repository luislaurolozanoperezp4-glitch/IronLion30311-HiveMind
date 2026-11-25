// Sources: IronLion30311BIOBUZZInicioDeTemporada / branch feature/turrubiates/vision-align / TeamCode/.../subsystems/vision/Alliance.java @ a7b8f4c (RED/BLUE enum)
//          IronLion30311BIOBUZZInicioDeTemporada / branch feature/turrubiates/vision-align / TeamCode/.../subsystems/pathing/AlliancePoses.java @ a7b8f4c (pose conversion idea)
//          — adapted for BIOBUZZ (180-degree rotation instead of a mirror, decision D13)
package org.firstinspires.ftc.teamcode.util;

import com.pedropathing.math.Pose;

import org.firstinspires.ftc.teamcode.config.FieldConstants;

/**
 * Which alliance the robot plays for, and how to turn a RED pose into this alliance's pose.
 * <p>
 * Every autonomous pose is written once, for RED. BLUE uses the same pose rotated 180 degrees
 * around the field center, because the BIOBUZZ field is rotationally symmetric (decision D13,
 * TO VERIFY with the team). A mirror would put BLUE on the wrong side of the field.
 */
public enum Alliance {
    RED,
    BLUE;

    /** @return true for the RED alliance */
    public boolean isRed() {
        return this == RED;
    }

    /** @return the other alliance */
    public Alliance getOpponent() {
        return this == RED ? BLUE : RED;
    }

    /**
     * Converts a pose written for RED into this alliance's pose on the real field.
     *
     * @param redPose a pose in the RED frame
     * @return the same pose for RED, or the pose rotated 180 degrees about the field center for BLUE
     */
    public Pose toField(Pose redPose) {
        if (this == RED) {
            return redPose;
        }
        return rotate180(redPose);
    }

    /**
     * Rotates a pose 180 degrees around the field center: x and y flip, heading turns by pi.
     *
     * @param pose the pose to rotate
     * @return the rotated pose
     */
    public static Pose rotate180(Pose pose) {
        double size = FieldConstants.FIELD_SIZE_INCHES;
        return new Pose(size - pose.x(), size - pose.y(), normalizeRadians(pose.heading() + Math.PI));
    }

    /**
     * Wraps an angle to the range (-pi, pi].
     *
     * @param radians any angle in radians
     * @return the same direction, between -pi and pi
     */
    public static double normalizeRadians(double radians) {
        double wrapped = radians % (2.0 * Math.PI);
        if (wrapped <= -Math.PI) {
            wrapped += 2.0 * Math.PI;
        } else if (wrapped > Math.PI) {
            wrapped -= 2.0 * Math.PI;
        }
        return wrapped;
    }
}
