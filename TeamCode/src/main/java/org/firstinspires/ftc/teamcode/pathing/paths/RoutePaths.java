// Sources: IronLion30311BIOBUZZInicioDeTemporada / branch feature/autonomos-beta / TeamCode/.../opmodes/auto/TickAuto.java @ 206fa84 (Paths.line(...).linear/constant usage)
//          IronLion30311BIOBUZZInicioDeTemporada / branch feature/turrubiates/vision-align / TeamCode/.../subsystems/pathing/AlliancePoses.java @ a7b8f4c (one set of poses for both alliances)
//          — adapted for BIOBUZZ (RED poses rotated 180 degrees for BLUE by util.Alliance)
package org.firstinspires.ftc.teamcode.pathing.paths;

import com.pedropathing.api.Paths;
import com.pedropathing.math.Pose;
import com.pedropathing.paths.Path;

import org.firstinspires.ftc.teamcode.util.Alliance;

/**
 * Base class for every route in this package.
 * <p>
 * A route class keeps its poses as {@code public static final} RED poses (field frame, inches,
 * radians). An instance is made for one alliance; every path it builds converts the RED poses
 * with {@link Alliance#toField(Pose)} (identity for RED, 180-degree rotation for BLUE).
 * <p>
 * Each "chain" (the moves the original routine ran between two stops) has a method returning
 * one Pedro {@link Path}, and a copy with the same start and end built from one straight line,
 * named {@code <chain>Straight()}. Straight copies that cut through the HIVE or cross into the
 * opponent's half are {@code @Deprecated}: DO NOT USE IN COMPETITION.
 */
public abstract class RoutePaths {

    /** Alliance this instance builds paths for. */
    protected final Alliance alliance;

    /**
     * @param alliance the alliance to build paths for
     */
    protected RoutePaths(Alliance alliance) {
        this.alliance = alliance;
    }

    /** @return the alliance these paths are built for */
    public Alliance getAlliance() {
        return alliance;
    }

    /**
     * @param redPose a RED pose
     * @return the same pose for this alliance
     */
    public Pose toField(Pose redPose) {
        return alliance.toField(redPose);
    }

    /**
     * Makes a RED pose from degrees (all route constants are written this way).
     *
     * @param x          inches
     * @param y          inches
     * @param headingDeg degrees, counter-clockwise from +x
     * @return the pose (heading in radians)
     */
    public static Pose pose(double x, double y, double headingDeg) {
        return new Pose(x, y, Math.toRadians(headingDeg));
    }

    /** Straight line that keeps the heading of {@code redHeading} the whole way. */
    protected Path lineConstant(Pose redFrom, Pose redTo, Pose redHeading) {
        return Paths.line(toField(redFrom), toField(redTo)).constant(toField(redHeading));
    }

    /** Straight line whose heading turns from the start heading to the end heading (short way). */
    protected Path lineLinear(Pose redFrom, Pose redTo) {
        Pose from = toField(redFrom);
        Pose to = toField(redTo);
        return Paths.line(from, to).linear(from, to);
    }

    /** Straight line driven front first (heading = direction of travel). */
    protected Path lineTangent(Pose redFrom, Pose redTo) {
        return Paths.line(toField(redFrom), toField(redTo)).tangent();
    }

    /** Straight line driven rear first (heading = opposite of the direction of travel). */
    protected Path lineReverseTangent(Pose redFrom, Pose redTo) {
        return Paths.line(toField(redFrom), toField(redTo)).reverseTangent();
    }

    /**
     * Joins paths into one Pedro path followed without stopping.
     *
     * @param segments the paths, in order
     * @return one compound path
     */
    protected static Path chain(Path... segments) {
        return Paths.path(segments);
    }
}
