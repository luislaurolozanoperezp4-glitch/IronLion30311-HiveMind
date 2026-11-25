// Sources: docs/AUDIT_PEDRO.md section 7 and docs/BEST_OF_MATRIX.md section 4 (path list, flags) — new for IronLion30311-HiveMind
package org.firstinspires.ftc.teamcode.pathing.paths;

import com.pedropathing.paths.Path;

import org.firstinspires.ftc.teamcode.util.Alliance;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Every path in {@code pathing.paths}, with where it came from and its known risks.
 * <p>
 * Used by {@code PathsTest} (every competition path is checked against the rules) and by
 * selectors and tuning OpModes that want to list or draw the routes.
 */
public final class PathCatalog {

    private PathCatalog() {
    }

    /** What a path may be used for. */
    public enum Use {
        /** Field-tested or the fallback the team relies on. */
        COMPETITION,
        /** Legal geometry, not yet proven on the field. */
        CANDIDATE,
        /** Kept for reference only (old routes). */
        ARCHIVE,
        /** Tuning and test OpModes only. */
        TUNING
    }

    /** Known risks of a path (all TO VERIFY against the Competition Manual). */
    public enum Flag {
        /** The robot footprint enters the HIVE frame box (G417 risk). */
        HIVE,
        /** The robot crosses into the opponent's half in AUTO (G402, number TO VERIFY). */
        TERRITORY,
        /** Ends or passes less than 2 in from a wall (LEAVE or collision risk). */
        WALL,
        /** Passes within ~5 in of a FLOWER. */
        FLOWER,
        /** The route has no PARK at the end. */
        NO_PARK
    }

    /** Builds one path for an alliance. */
    public interface PathBuilder {
        /**
         * @param alliance alliance to build for
         * @return the path
         */
        Path build(Alliance alliance);
    }

    /** One path of one route. */
    public static final class Entry {
        /** Route name, for example "Preload Park". */
        public final String route;
        /** Method name in {@link #ownerClass} that builds the path. */
        public final String methodName;
        /** Source repository ref and commit. */
        public final String source;
        /** What the path may be used for. */
        public final Use use;
        /** Known risks. */
        public final Set<Flag> flags;
        /** True for a {@code ...Straight} copy. */
        public final boolean isStraightCopy;
        /** True if the path is the last move of the route and must end in the LOADING ZONE. */
        public final boolean isParkPath;
        /** Class that owns the method. */
        public final Class<? extends RoutePaths> ownerClass;
        private final PathBuilder builder;

        Entry(String route, Class<? extends RoutePaths> ownerClass, String methodName, String source,
              Use use, Set<Flag> flags, boolean isStraightCopy, boolean isParkPath, PathBuilder builder) {
            this.route = route;
            this.ownerClass = ownerClass;
            this.methodName = methodName;
            this.source = source;
            this.use = use;
            this.flags = Collections.unmodifiableSet(flags);
            this.isStraightCopy = isStraightCopy;
            this.isParkPath = isParkPath;
            this.builder = builder;
        }

        /**
         * @param alliance alliance to build for
         * @return the path for that alliance
         */
        public Path build(Alliance alliance) {
            return builder.build(alliance);
        }

        /** @return true if the path must never run in a match (HIVE or TERRITORY flag) */
        public boolean isBlockedForCompetition() {
            return flags.contains(Flag.HIVE) || flags.contains(Flag.TERRITORY);
        }

        /** @return "Route.methodName" */
        public String getName() {
            return route + "." + methodName;
        }

        @Override
        public String toString() {
            return getName();
        }
    }

    private static final String VA = "IronLion30311BIOBUZZInicioDeTemporada feature/turrubiates/vision-align @ a7b8f4c";
    private static final String BETA = "IronLion30311BIOBUZZInicioDeTemporada feature/autonomos-beta @ 206fa84";
    private static final String NEW = "new (docs/BEST_OF_MATRIX.md section 4 row 23)";

    private static final List<Entry> ENTRIES = buildEntries();

    /** @return every path, originals and straight copies */
    public static List<Entry> getAll() {
        return ENTRIES;
    }

    /** @return the paths with no HIVE or TERRITORY flag */
    public static List<Entry> getAllowedForCompetition() {
        List<Entry> allowed = new ArrayList<>();
        for (Entry entry : ENTRIES) {
            if (!entry.isBlockedForCompetition()) {
                allowed.add(entry);
            }
        }
        return allowed;
    }

    private static Set<Flag> flags(Flag... flags) {
        EnumSet<Flag> set = EnumSet.noneOf(Flag.class);
        Collections.addAll(set, flags);
        return set;
    }

    @SuppressWarnings("deprecation")
    private static List<Entry> buildEntries() {
        List<Entry> list = new ArrayList<>();

        // ---- Preload Park (TESTED primary route) ----
        list.add(new Entry("Preload Park", PreloadParkPaths.class, "leaveWall", VA, Use.COMPETITION,
                flags(), false, false, a -> new PreloadParkPaths(a).leaveWall()));
        list.add(new Entry("Preload Park", PreloadParkPaths.class, "leaveWallStraight", VA, Use.COMPETITION,
                flags(), true, false, a -> new PreloadParkPaths(a).leaveWallStraight()));
        list.add(new Entry("Preload Park", PreloadParkPaths.class, "park", VA, Use.COMPETITION,
                flags(Flag.WALL), false, true, a -> new PreloadParkPaths(a).park()));
        list.add(new Entry("Preload Park", PreloadParkPaths.class, "parkStraight", VA, Use.ARCHIVE,
                flags(Flag.HIVE, Flag.WALL), true, true, a -> new PreloadParkPaths(a).parkStraight()));

        // ---- Preload Collect (shooting side) ----
        list.add(new Entry("Preload Collect", PreloadCollectPaths.class, "collectRow", VA, Use.CANDIDATE,
                flags(Flag.WALL), false, false, a -> new PreloadCollectPaths(a).collectRow()));
        list.add(new Entry("Preload Collect", PreloadCollectPaths.class, "collectRowStraight", VA, Use.CANDIDATE,
                flags(Flag.WALL), true, false, a -> new PreloadCollectPaths(a).collectRowStraight()));
        list.add(new Entry("Preload Collect", PreloadCollectPaths.class, "returnToShootSpot", VA, Use.CANDIDATE,
                flags(Flag.WALL), false, false, a -> new PreloadCollectPaths(a).returnToShootSpot()));
        list.add(new Entry("Preload Collect", PreloadCollectPaths.class, "returnToShootSpotStraight", VA,
                Use.CANDIDATE, flags(Flag.WALL), true, false,
                a -> new PreloadCollectPaths(a).returnToShootSpotStraight()));
        list.add(new Entry("Preload Collect", PreloadCollectPaths.class, "parkFromShootSide", VA, Use.CANDIDATE,
                flags(Flag.WALL), false, true, a -> new PreloadCollectPaths(a).parkFromShootSide()));
        list.add(new Entry("Preload Collect", PreloadCollectPaths.class, "parkFromShootSideStraight", VA,
                Use.ARCHIVE, flags(Flag.HIVE, Flag.WALL), true, true,
                a -> new PreloadCollectPaths(a).parkFromShootSideStraight()));

        // ---- Other cell (after a TIP) ----
        list.add(new Entry("Other Cell", OtherCellPaths.class, "fromCollect", VA, Use.CANDIDATE,
                flags(Flag.WALL, Flag.FLOWER), false, false, a -> new OtherCellPaths(a).fromCollect()));
        list.add(new Entry("Other Cell", OtherCellPaths.class, "fromCollectStraight", VA, Use.ARCHIVE,
                flags(Flag.HIVE, Flag.WALL, Flag.FLOWER), true, false,
                a -> new OtherCellPaths(a).fromCollectStraight()));
        list.add(new Entry("Other Cell", OtherCellPaths.class, "fromShootSpot", VA, Use.CANDIDATE,
                flags(Flag.FLOWER), false, false, a -> new OtherCellPaths(a).fromShootSpot()));
        list.add(new Entry("Other Cell", OtherCellPaths.class, "fromShootSpotStraight", VA, Use.ARCHIVE,
                flags(Flag.HIVE, Flag.FLOWER), true, false, a -> new OtherCellPaths(a).fromShootSpotStraight()));
        list.add(new Entry("Other Cell", OtherCellPaths.class, "toPark", VA, Use.CANDIDATE,
                flags(Flag.WALL), false, true, a -> new OtherCellPaths(a).toPark()));
        list.add(new Entry("Other Cell", OtherCellPaths.class, "toParkStraight", VA, Use.CANDIDATE,
                flags(Flag.WALL), true, true, a -> new OtherCellPaths(a).toParkStraight()));

        // ---- Tick (beta, archive) ----
        list.add(new Entry("Tick", TickPaths.class, "offTheWall", BETA, Use.ARCHIVE,
                flags(Flag.WALL), false, false, a -> new TickPaths(a).offTheWall()));
        list.add(new Entry("Tick", TickPaths.class, "offTheWallStraight", BETA, Use.ARCHIVE,
                flags(Flag.WALL), true, false, a -> new TickPaths(a).offTheWallStraight()));
        list.add(new Entry("Tick", TickPaths.class, "intoCorner", BETA, Use.ARCHIVE,
                flags(Flag.WALL), false, false, a -> new TickPaths(a).intoCorner()));
        list.add(new Entry("Tick", TickPaths.class, "intoCornerStraight", BETA, Use.ARCHIVE,
                flags(Flag.WALL), true, false, a -> new TickPaths(a).intoCornerStraight()));
        list.add(new Entry("Tick", TickPaths.class, "sidewaysToShootSpot", BETA, Use.ARCHIVE,
                flags(Flag.HIVE), false, false, a -> new TickPaths(a).sidewaysToShootSpot()));
        list.add(new Entry("Tick", TickPaths.class, "sidewaysToShootSpotStraight", BETA, Use.ARCHIVE,
                flags(Flag.HIVE), true, false, a -> new TickPaths(a).sidewaysToShootSpotStraight()));
        list.add(new Entry("Tick", TickPaths.class, "sidewaysToPark", BETA, Use.ARCHIVE,
                flags(Flag.WALL), false, true, a -> new TickPaths(a).sidewaysToPark()));
        list.add(new Entry("Tick", TickPaths.class, "sidewaysToParkStraight", BETA, Use.ARCHIVE,
                flags(Flag.WALL), true, true, a -> new TickPaths(a).sidewaysToParkStraight()));

        // ---- Center Lane (beta, archive) ----
        list.add(new Entry("Center Lane", CenterLanePaths.class, "offTheWall", BETA, Use.ARCHIVE,
                flags(Flag.WALL), false, false, a -> new CenterLanePaths(a).offTheWall()));
        list.add(new Entry("Center Lane", CenterLanePaths.class, "offTheWallStraight", BETA, Use.ARCHIVE,
                flags(Flag.WALL), true, false, a -> new CenterLanePaths(a).offTheWallStraight()));
        list.add(new Entry("Center Lane", CenterLanePaths.class, "intoCorner", BETA, Use.ARCHIVE,
                flags(Flag.WALL), false, false, a -> new CenterLanePaths(a).intoCorner()));
        list.add(new Entry("Center Lane", CenterLanePaths.class, "intoCornerStraight", BETA, Use.ARCHIVE,
                flags(Flag.WALL), true, false, a -> new CenterLanePaths(a).intoCornerStraight()));
        list.add(new Entry("Center Lane", CenterLanePaths.class, "upTheWall", BETA, Use.ARCHIVE,
                flags(Flag.WALL, Flag.FLOWER), false, false, a -> new CenterLanePaths(a).upTheWall()));
        list.add(new Entry("Center Lane", CenterLanePaths.class, "upTheWallStraight", BETA, Use.ARCHIVE,
                flags(Flag.WALL, Flag.FLOWER), true, false, a -> new CenterLanePaths(a).upTheWallStraight()));
        list.add(new Entry("Center Lane", CenterLanePaths.class, "underTheStructure", BETA, Use.ARCHIVE,
                flags(Flag.HIVE), false, false, a -> new CenterLanePaths(a).underTheStructure()));
        list.add(new Entry("Center Lane", CenterLanePaths.class, "underTheStructureStraight", BETA, Use.ARCHIVE,
                flags(Flag.HIVE), true, false, a -> new CenterLanePaths(a).underTheStructureStraight()));
        list.add(new Entry("Center Lane", CenterLanePaths.class, "sidewaysToPark", BETA, Use.ARCHIVE,
                flags(Flag.WALL), false, true, a -> new CenterLanePaths(a).sidewaysToPark()));
        list.add(new Entry("Center Lane", CenterLanePaths.class, "sidewaysToParkStraight", BETA, Use.ARCHIVE,
                flags(Flag.WALL), true, true, a -> new CenterLanePaths(a).sidewaysToParkStraight()));

        // ---- Outer Lane (beta, candidate) ----
        list.add(new Entry("Outer Lane", OuterLanePaths.class, "backOffWall", BETA, Use.CANDIDATE,
                flags(Flag.WALL), false, false, a -> new OuterLanePaths(a).backOffWall()));
        list.add(new Entry("Outer Lane", OuterLanePaths.class, "backOffWallStraight", BETA, Use.CANDIDATE,
                flags(Flag.WALL), true, false, a -> new OuterLanePaths(a).backOffWallStraight()));
        list.add(new Entry("Outer Lane", OuterLanePaths.class, "toNearCorner", BETA, Use.CANDIDATE,
                flags(Flag.WALL), false, false, a -> new OuterLanePaths(a).toNearCorner()));
        list.add(new Entry("Outer Lane", OuterLanePaths.class, "toNearCornerStraight", BETA, Use.CANDIDATE,
                flags(Flag.WALL), true, false, a -> new OuterLanePaths(a).toNearCornerStraight()));
        list.add(new Entry("Outer Lane", OuterLanePaths.class, "backToStartColumn", BETA, Use.CANDIDATE,
                flags(Flag.WALL), false, false, a -> new OuterLanePaths(a).backToStartColumn()));
        list.add(new Entry("Outer Lane", OuterLanePaths.class, "backToStartColumnStraight", BETA, Use.CANDIDATE,
                flags(Flag.WALL), true, false, a -> new OuterLanePaths(a).backToStartColumnStraight()));
        list.add(new Entry("Outer Lane", OuterLanePaths.class, "toShootPose", BETA, Use.CANDIDATE,
                flags(), false, false, a -> new OuterLanePaths(a).toShootPose()));
        list.add(new Entry("Outer Lane", OuterLanePaths.class, "toShootPoseStraight", BETA, Use.CANDIDATE,
                flags(), true, false, a -> new OuterLanePaths(a).toShootPoseStraight()));
        list.add(new Entry("Outer Lane", OuterLanePaths.class, "toOuterWall", BETA, Use.CANDIDATE,
                flags(Flag.FLOWER), false, false, a -> new OuterLanePaths(a).toOuterWall()));
        list.add(new Entry("Outer Lane", OuterLanePaths.class, "toOuterWallStraight", BETA, Use.CANDIDATE,
                flags(Flag.FLOWER), true, false, a -> new OuterLanePaths(a).toOuterWallStraight()));
        list.add(new Entry("Outer Lane", OuterLanePaths.class, "toPark", BETA, Use.CANDIDATE,
                flags(Flag.WALL), false, true, a -> new OuterLanePaths(a).toPark()));
        list.add(new Entry("Outer Lane", OuterLanePaths.class, "toParkStraight", BETA, Use.CANDIDATE,
                flags(Flag.WALL), true, true, a -> new OuterLanePaths(a).toParkStraight()));

        // ---- Safe Park (beta Park) ----
        list.add(new Entry("Safe Park", SafeParkPaths.class, "farStartToLoadingZone", BETA, Use.COMPETITION,
                flags(Flag.WALL), false, true, a -> new SafeParkPaths(a).farStartToLoadingZone()));
        list.add(new Entry("Safe Park", SafeParkPaths.class, "farStartToLoadingZoneStraight", BETA,
                Use.COMPETITION, flags(Flag.WALL), true, true,
                a -> new SafeParkPaths(a).farStartToLoadingZoneStraight()));

        // ---- Shoot Collect (beta Shoot Turn Collect, archive) ----
        list.add(new Entry("Shoot Collect", ShootCollectPaths.class, "backOff", BETA, Use.ARCHIVE,
                flags(Flag.WALL, Flag.NO_PARK), false, false, a -> new ShootCollectPaths(a).backOff()));
        list.add(new Entry("Shoot Collect", ShootCollectPaths.class, "backOffStraight", BETA, Use.ARCHIVE,
                flags(Flag.WALL, Flag.NO_PARK), true, false, a -> new ShootCollectPaths(a).backOffStraight()));
        list.add(new Entry("Shoot Collect", ShootCollectPaths.class, "collect", BETA, Use.ARCHIVE,
                flags(Flag.NO_PARK), false, false, a -> new ShootCollectPaths(a).collect()));
        list.add(new Entry("Shoot Collect", ShootCollectPaths.class, "collectStraight", BETA, Use.ARCHIVE,
                flags(Flag.NO_PARK), true, false, a -> new ShootCollectPaths(a).collectStraight()));

        // ---- Far Cycle (new, untested) ----
        list.add(new Entry("Far Cycle", FarCyclePaths.class, "toOtherSpot", NEW, Use.CANDIDATE,
                flags(Flag.WALL, Flag.FLOWER), false, false, a -> new FarCyclePaths(a).toOtherSpot()));
        list.add(new Entry("Far Cycle", FarCyclePaths.class, "toOtherSpotStraight", NEW, Use.CANDIDATE,
                flags(Flag.WALL, Flag.FLOWER), true, false, a -> new FarCyclePaths(a).toOtherSpotStraight()));
        list.add(new Entry("Far Cycle", FarCyclePaths.class, "otherSpotToShootSpot", NEW, Use.CANDIDATE,
                flags(), false, false, a -> new FarCyclePaths(a).otherSpotToShootSpot()));
        list.add(new Entry("Far Cycle", FarCyclePaths.class, "otherSpotToShootSpotStraight", NEW, Use.ARCHIVE,
                flags(Flag.HIVE), true, false, a -> new FarCyclePaths(a).otherSpotToShootSpotStraight()));

        // ---- Test square (tuning only, no straight copy) ----
        list.add(new Entry("Test Square", TestSquarePaths.class, "square", VA, Use.TUNING,
                flags(Flag.WALL), false, false, a -> new TestSquarePaths(a).square()));

        return Collections.unmodifiableList(list);
    }
}
