// Sources: docs/BEST_OF_MATRIX.md section 2.7 (PathsTest) and docs/AUDIT_PEDRO.md section 5 (rule checks) — new for IronLion30311-HiveMind
package org.firstinspires.ftc.teamcode.pathing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.pedropathing.math.Pose;
import com.pedropathing.paths.Path;

import org.firstinspires.ftc.teamcode.config.FieldConstants;
import org.firstinspires.ftc.teamcode.config.RobotConstants;
import org.firstinspires.ftc.teamcode.pathing.paths.NearRoutePoses;
import org.firstinspires.ftc.teamcode.pathing.paths.PathCatalog;
import org.firstinspires.ftc.teamcode.pathing.paths.RoutePaths;
import org.firstinspires.ftc.teamcode.testutil.RuleGeometry;
import org.firstinspires.ftc.teamcode.util.Alliance;
import org.junit.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;

/**
 * Checks every path in {@link PathCatalog} against the BIOBUZZ rules as modeled in
 * {@link FieldConstants}, with the robot as an 18 x 18 in rectangle that turns with the path
 * heading.
 */
public class PathsTest {

    private static final double STEP = 0.005;
    private static final double EPSILON = 1e-6;
    private static final double HALF_LENGTH = RobotConstants.ROBOT_LENGTH_INCHES / 2.0;
    private static final double HALF_WIDTH = RobotConstants.ROBOT_WIDTH_INCHES / 2.0;
    private static final int POINTS_PER_SIDE = 8;

    @Test
    public void catalogBuildsEveryPathForBothAlliances() {
        for (PathCatalog.Entry entry : PathCatalog.getAll()) {
            assertNotNull(entry.getName(), entry.build(Alliance.RED));
            assertNotNull(entry.getName(), entry.build(Alliance.BLUE));
            // get() throws if a path has no heading interpolation
            entry.build(Alliance.RED).get(0.5);
        }
    }

    @Test
    public void everyOriginalHasAStraightCopy() {
        for (PathCatalog.Entry entry : PathCatalog.getAll()) {
            if (entry.isStraightCopy || entry.use == PathCatalog.Use.TUNING) {
                continue;
            }
            PathCatalog.Entry copy = find(entry.ownerClass, entry.methodName + "Straight");
            assertNotNull("No straight copy for " + entry.getName(), copy);
            assertTrue(copy.isStraightCopy);
            assertEquals(entry.isParkPath, copy.isParkPath);
            Path original = entry.build(Alliance.RED);
            Path straight = copy.build(Alliance.RED);
            assertSamePosition(entry.getName() + " start", original.get(0.0), straight.get(0.0));
            assertSamePosition(entry.getName() + " end", original.get(1.0), straight.get(1.0));
        }
    }

    @Test
    public void straightCopiesAreOneStraightLine() {
        for (PathCatalog.Entry entry : PathCatalog.getAll()) {
            if (!entry.isStraightCopy) {
                continue;
            }
            Path path = entry.build(Alliance.RED);
            Pose start = path.get(0.0);
            Pose end = path.get(1.0);
            for (double t = 0.0; t <= 1.0; t += 0.1) {
                Pose p = path.get(t);
                // Distance from the start-end line must be zero
                double cross = (end.x() - start.x()) * (p.y() - start.y())
                        - (end.y() - start.y()) * (p.x() - start.x());
                double length = Math.hypot(end.x() - start.x(), end.y() - start.y());
                assertEquals(entry.getName(), 0.0, cross / length, 1e-6);
            }
        }
    }

    @Test
    public void deprecatedExactlyWhenBlockedForCompetition() throws Exception {
        for (PathCatalog.Entry entry : PathCatalog.getAll()) {
            Method method = entry.ownerClass.getMethod(entry.methodName);
            assertEquals(entry.getName() + ": @Deprecated must match the HIVE/TERRITORY flag",
                    entry.isBlockedForCompetition(), method.isAnnotationPresent(Deprecated.class));
            if (entry.isBlockedForCompetition()) {
                assertFalse(entry.getName() + " cannot be a competition path",
                        entry.use == PathCatalog.Use.COMPETITION);
            }
        }
    }

    @Test
    public void hiveFlagMatchesGeometry() {
        for (PathCatalog.Entry entry : PathCatalog.getAll()) {
            boolean isEntering = entersHive(entry.build(Alliance.RED));
            assertEquals(entry.getName() + (isEntering ? " enters the HIVE box but has no HIVE flag"
                            : " has a HIVE flag but stays clear of the HIVE box"),
                    entry.flags.contains(PathCatalog.Flag.HIVE), isEntering);
        }
    }

    @Test
    public void unflaggedPathsStayOnTheRedSide() {
        for (PathCatalog.Entry entry : PathCatalog.getAll()) {
            if (entry.flags.contains(PathCatalog.Flag.TERRITORY)) {
                continue;
            }
            Path path = entry.build(Alliance.RED);
            for (double t = 0.0; t <= 1.0 + EPSILON; t += STEP) {
                Pose p = path.get(Math.min(t, 1.0));
                assertTrue(entry.getName() + " crosses the territory line at t=" + t + ": " + p,
                        p.x() + HALF_WIDTH <= FieldConstants.TERRITORY_LINE_X);
                assertTrue(entry.getName() + " leaves the field at t=" + t + ": " + p,
                        p.x() >= 0.0 && p.y() >= 0.0
                                && p.x() <= FieldConstants.FIELD_SIZE_INCHES
                                && p.y() <= FieldConstants.FIELD_SIZE_INCHES);
            }
        }
    }

    @Test
    public void parkPathsEndInTheLoadingZone() {
        for (PathCatalog.Entry entry : PathCatalog.getAll()) {
            if (!entry.isParkPath) {
                continue;
            }
            Pose end = entry.build(Alliance.RED).get(1.0);
            assertTrue(entry.getName() + " robot ends outside the red LOADING ZONE: " + end,
                    RuleGeometry.overlapsRedLoadingZone(end));
            if (entry.use == PathCatalog.Use.COMPETITION) {
                // Team decision: competition parks end >= WALL_CLEARANCE_INCHES off the wall (LEAVE).
                assertTrue(entry.getName() + " ends too close to the wall for LEAVE: " + end,
                        RuleGeometry.wallGap(end) >= FieldConstants.WALL_CLEARANCE_INCHES - EPSILON);
            }
        }
    }

    @Test
    public void bluePathsAreRedRotated180() {
        for (PathCatalog.Entry entry : PathCatalog.getAll()) {
            Path red = entry.build(Alliance.RED);
            Path blue = entry.build(Alliance.BLUE);
            for (double t = 0.0; t <= 1.0 + EPSILON; t += 0.05) {
                double s = Math.min(t, 1.0);
                assertSamePosition(entry.getName(), Alliance.rotate180(red.get(s)), blue.get(s));
            }
        }
    }

    @Test
    public void everyPublicPathMethodIsInTheCatalog() {
        Set<Class<?>> owners = new HashSet<>();
        Set<String> listed = new HashSet<>();
        for (PathCatalog.Entry entry : PathCatalog.getAll()) {
            owners.add(entry.ownerClass);
            listed.add(entry.ownerClass.getSimpleName() + "." + entry.methodName);
        }
        for (Class<?> owner : owners) {
            for (Method method : owner.getDeclaredMethods()) {
                if (Modifier.isPublic(method.getModifiers()) && method.getReturnType() == Path.class
                        && method.getParameterTypes().length == 0) {
                    String name = owner.getSimpleName() + "." + method.getName();
                    assertTrue(name + " is missing from PathCatalog", listed.contains(name));
                }
            }
        }
    }

    @Test
    public void nearPosesMatchTheTestedRoute() {
        // docs/AUDIT_PEDRO.md section 7.3 (converted from the tested cm legs)
        assertPose(NearRoutePoses.SHOOT_SPOT, 57.000, 12.937);
        assertPose(NearRoutePoses.WEST_LANE_SOUTH, 33.378, 14.906);
        assertPose(NearRoutePoses.WEST_LANE_NORTH, 33.378, 95.417);
        // PARK moved from the TESTED x 9.756 to 2 in off the wall (team decision, RIGHT_2_CM 60 -> 56.5)
        assertPose(NearRoutePoses.PARK, 11.134, 95.417);
        assertPose(NearRoutePoses.COLLECT_START, 29.047, 9.394);
        assertPose(NearRoutePoses.COLLECT_END, 9.756, 9.394);
        assertPose(NearRoutePoses.COLLECT_EXIT, 23.929, 29.866);
        assertPose(NearRoutePoses.NORTH_WAYPOINT, 33.378, 110.181);
        assertPose(NearRoutePoses.OTHER_SPOT, 46.764, 118.449);
        assertEquals(0.0, OdometryDriver.angleError(Math.toRadians(115.0), NearRoutePoses.OTHER_SPOT.heading()), 1e-9);
        // Pedro stores headings in [0, 2 pi): -90 degrees reads as 270
        assertEquals(0.0, OdometryDriver.angleError(Math.toRadians(-90.0), NearRoutePoses.PARK.heading()), 1e-9);
    }

    @Test
    public void routePoseHelperUsesDegrees() {
        Pose pose = RoutePaths.pose(1.0, 2.0, 90.0);
        assertEquals(Math.PI / 2.0, pose.heading(), 1e-12);
    }

    // ------------------------------------------------------------------------------------------

    private static PathCatalog.Entry find(Class<?> owner, String methodName) {
        for (PathCatalog.Entry entry : PathCatalog.getAll()) {
            if (entry.ownerClass == owner && entry.methodName.equals(methodName)) {
                return entry;
            }
        }
        return null;
    }

    /** True if any point of the robot outline is strictly inside the HIVE box. */
    private static boolean entersHive(Path path) {
        for (double t = 0.0; t <= 1.0 + EPSILON; t += STEP) {
            Pose p = path.get(Math.min(t, 1.0));
            double cos = Math.cos(p.heading());
            double sin = Math.sin(p.heading());
            for (int side = 0; side < 4; side++) {
                for (int i = 0; i <= POINTS_PER_SIDE; i++) {
                    double f = -1.0 + 2.0 * i / POINTS_PER_SIDE;
                    double along;
                    double across;
                    if (side == 0) {
                        along = HALF_LENGTH;
                        across = f * HALF_WIDTH;
                    } else if (side == 1) {
                        along = -HALF_LENGTH;
                        across = f * HALF_WIDTH;
                    } else if (side == 2) {
                        along = f * HALF_LENGTH;
                        across = HALF_WIDTH;
                    } else {
                        along = f * HALF_LENGTH;
                        across = -HALF_WIDTH;
                    }
                    double x = p.x() + along * cos - across * sin;
                    double y = p.y() + along * sin + across * cos;
                    if (x > FieldConstants.HIVE_MIN_X && x < FieldConstants.HIVE_MAX_X
                            && y > FieldConstants.HIVE_MIN_Y && y < FieldConstants.HIVE_MAX_Y) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static void assertSamePosition(String message, Pose expected, Pose actual) {
        assertEquals(message + " x", expected.x(), actual.x(), 1e-6);
        assertEquals(message + " y", expected.y(), actual.y(), 1e-6);
    }

    private static void assertPose(Pose pose, double x, double y) {
        assertEquals(x, pose.x(), 0.001);
        assertEquals(y, pose.y(), 0.001);
    }
}
