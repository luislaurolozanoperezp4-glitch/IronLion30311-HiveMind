// Sources: IronLion30311BIOBUZZInicioDeTemporada / branch feature/turrubiates/vision-align / TeamCode/.../subsystems/vision/HiveOrientation.java @ a7b8f4c (whole class) — adapted for BIOBUZZ
package org.firstinspires.ftc.teamcode.subsystems.vision;

/**
 * Pure angle math used to decide whether a HIVE cell's AprilTag cluster is upright.
 *
 * <p>Per the official BIOBUZZ AprilTag tech tip, the scorable (upward-facing) CELL is the one
 * whose cluster looks upright to the camera; the other CELL of the same alliance looks upside
 * down. Upright versus upside down is a ~180 degree difference in the tag's in-image orientation,
 * so it is decided with a wide 90 degree margin around a reference angle measured on the field
 * with {@code HiveStateProbe}.</p>
 *
 * <p>Orientation angles are bimodal (around the reference or 180 degrees away), so they must never
 * be averaged arithmetically or EMA-filtered: use {@link #circularMeanDeg} instead.</p>
 *
 * <p>No FTC SDK dependencies, so it can be unit tested on the computer.</p>
 */
public final class HiveOrientation {

    /**
     * 3D camera-space roll (degrees) of an upright cluster. Provisional until measured on the
     * field with {@code HiveStateProbe}; a cluster is upright when its roll is within 90 degrees.
     */
    public static final double UPRIGHT_ROLL_REFERENCE_DEG = 0.0;

    /**
     * In-image angle (degrees) of the tag's "up" corner vector for an upright cluster. Image y
     * grows downward, so an upright tag points to about -90 degrees if the Limelight keeps the
     * AprilTag corner order. Provisional until measured on the field with {@code HiveStateProbe}.
     */
    public static final double UPRIGHT_IMAGE_UP_REFERENCE_DEG = -90.0;

    /**
     * Raw 3D camera-space yaw (degrees) of an upright cluster. Provisional until measured on the
     * field with {@code HiveStateProbe}.
     */
    public static final double UPRIGHT_RAW_YAW_REFERENCE_DEG = 0.0;

    /** Which orientation signal decides upright versus upside down. */
    public enum Signal {
        /** 3D camera-space roll; needs "Full 3D" enabled in the Limelight pipeline. */
        ROLL,
        /** Raw 3D camera-space yaw; the flip may show here if the Limelight angle order differs. */
        RAW_YAW,
        /** In-image corner angle; also works in 2D if the corner order is preserved. */
        IMAGE_UP
    }

    /**
     * Signal used by the robot to classify clusters. Provisional: confirm on the field with
     * {@code HiveStateProbe}, which shows both signals side by side.
     */
    public static final Signal UPRIGHT_SIGNAL = Signal.IMAGE_UP;

    /** Half-width of the "upright" window around the reference angle. */
    private static final double UPRIGHT_HALF_WINDOW_DEG = 90.0;

    /**
     * Minimum agreement (mean resultant length, 0..1) for a circular mean to be trusted. Two
     * angles 90 degrees apart give ~0.71; opposite angles give 0.
     */
    private static final double MIN_CIRCULAR_AGREEMENT = 0.5;

    private HiveOrientation() {
        // Utility class
    }

    /**
     * @param angleDeg any angle in degrees
     * @return the same angle wrapped to the range [-180, 180)
     */
    public static double wrapDeg(double angleDeg) {
        double wrapped = (angleDeg + 180.0) % 360.0;
        if (wrapped < 0.0) {
            wrapped += 360.0;
        }
        double result = wrapped - 180.0;
        // Floating-point rounding can land exactly on +180; keep the range half-open.
        return result >= 180.0 ? -180.0 : result;
    }

    /**
     * Circular mean of a set of angles (mean of unit vectors), which stays correct across the
     * +/-180 degree seam where an arithmetic mean breaks. {@code NaN} entries are ignored.
     *
     * @param anglesDeg angles in degrees
     * @return mean angle in [-180, 180), or {@code NaN} if there is no valid angle or the angles
     *         disagree too much to have a dominant direction (see {@link #MIN_CIRCULAR_AGREEMENT})
     */
    public static double circularMeanDeg(double... anglesDeg) {
        double sumX = 0.0;
        double sumY = 0.0;
        int count = 0;
        for (double angleDeg : anglesDeg) {
            if (Double.isNaN(angleDeg)) {
                continue;
            }
            double angleRad = Math.toRadians(angleDeg);
            sumX += Math.cos(angleRad);
            sumY += Math.sin(angleRad);
            count++;
        }
        if (count == 0 || Math.hypot(sumX, sumY) / count < MIN_CIRCULAR_AGREEMENT) {
            return Double.NaN;
        }
        return wrapDeg(Math.toDegrees(Math.atan2(sumY, sumX)));
    }

    /**
     * Angle (degrees) of the image-space vector from one tag corner to another.
     *
     * @return angle in [-180, 180), or {@code NaN} if the two corners coincide
     */
    public static double cornerVectorAngleDeg(double fromX, double fromY, double toX, double toY) {
        double dx = toX - fromX;
        double dy = toY - fromY;
        if (Math.hypot(dx, dy) < 1e-9) {
            return Double.NaN;
        }
        return wrapDeg(Math.toDegrees(Math.atan2(dy, dx)));
    }

    /**
     * @param angleDeg     measured orientation of the cluster, in degrees ({@code NaN} = unknown)
     * @param referenceDeg orientation of an upright cluster, in degrees
     * @return {@code true} if the cluster is upright; {@code false} if it is upside down or unknown
     */
    public static boolean isUpright(double angleDeg, double referenceDeg) {
        if (Double.isNaN(angleDeg)) {
            return false;
        }
        return Math.abs(wrapDeg(angleDeg - referenceDeg)) < UPRIGHT_HALF_WINDOW_DEG;
    }
}
