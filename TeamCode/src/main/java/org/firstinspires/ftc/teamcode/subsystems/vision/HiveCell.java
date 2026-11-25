// Sources: IronLion30311BIOBUZZInicioDeTemporada / branch feature/turrubiates/vision-align / TeamCode/.../subsystems/vision/HiveCell.java @ a7b8f4c (enum, tag IDs, indexOfTag)
//          IronLion30311BIOBUZZInicioDeTemporada / branch feature/turrubiates/vision-align / TeamCode/.../subsystems/vision/Alliance.java @ a7b8f4c (getScoringCell/getAudienceCell, moved here as static helpers)
//          — adapted for BIOBUZZ
package org.firstinspires.ftc.teamcode.subsystems.vision;

import org.firstinspires.ftc.teamcode.config.VisionConstants;
import org.firstinspires.ftc.teamcode.util.Alliance;

/**
 * The four HIVE cells on the BIOBUZZ field and their AprilTag IDs (ordered left to right when
 * you face the cell from the field).
 * <p>
 * Each HIVE cell has a line of 4 AprilTags (36h11 family, 3.25 in) on its underside. Each
 * alliance owns two cells, and exactly one of them faces up (can be scored) at any time; every
 * TIP swaps which one.
 * <p>
 * Names follow the Competition Manual (TU10, section 9.9, Figure 9-17), confirmed by the team
 * (decision D20): BACK = the CELL on the side of the FIELD opposite the audience (+y in the
 * Pedro frame), AUDIENCE = the CELL on the audience side. Red 30-33 back, red 34-37 audience,
 * blue 38-41 audience, blue 42-45 back. Renamed from the tested robot's RED_SCORING /
 * BLUE_SCORING; the behavior is unchanged.
 */
public enum HiveCell {
    RED_BACK(Alliance.RED, "Red Back (30-33)", 30, 31, 32, 33),
    RED_AUDIENCE(Alliance.RED, "Red Audience (34-37)", 34, 35, 36, 37),
    BLUE_AUDIENCE(Alliance.BLUE, "Blue Audience (38-41)", 38, 39, 40, 41),
    BLUE_BACK(Alliance.BLUE, "Blue Back (42-45)", 42, 43, 44, 45);

    /** Lowest AprilTag ID used by any HIVE cell. */
    public static final int MIN_TAG_ID = VisionConstants.FIRST_HIVE_TAG_ID;

    /** Highest AprilTag ID used by any HIVE cell. */
    public static final int MAX_TAG_ID = VisionConstants.LAST_HIVE_TAG_ID;

    private final Alliance alliance;
    private final String displayName;
    private final int[] tagIds;

    HiveCell(Alliance alliance, String displayName, int... tagIds) {
        this.alliance = alliance;
        this.displayName = displayName;
        this.tagIds = tagIds;
    }

    /**
     * @param alliance our alliance
     * @return the alliance's CELL opposite the audience (the tested routine shoots it first)
     */
    public static HiveCell getBackCell(Alliance alliance) {
        return alliance == Alliance.RED ? RED_BACK : BLUE_BACK;
    }

    /**
     * @param alliance our alliance
     * @return the alliance's other cell, on the audience side
     */
    public static HiveCell getAudienceCell(Alliance alliance) {
        return alliance == Alliance.RED ? RED_AUDIENCE : BLUE_AUDIENCE;
    }

    /**
     * @param tagId an AprilTag ID
     * @return the cell that owns this tag, or {@code null} if it is not a HIVE tag
     */
    public static HiveCell fromTagId(int tagId) {
        for (HiveCell cell : values()) {
            if (cell.indexOfTag(tagId) >= 0) {
                return cell;
            }
        }
        return null;
    }

    /** @return the alliance that owns this cell */
    public Alliance getAlliance() {
        return alliance;
    }

    /** @return the cell name and tag range, for telemetry */
    public String getDisplayName() {
        return displayName;
    }

    /** @return the 4 AprilTag IDs of this cell, left to right (indices 0..3) */
    public int[] getTagIds() {
        return tagIds;
    }

    /**
     * @param tagId AprilTag ID
     * @return the index (0..3) of the tag in this cell, or -1 if it is not part of it
     */
    public int indexOfTag(int tagId) {
        for (int i = 0; i < tagIds.length; i++) {
            if (tagIds[i] == tagId) {
                return i;
            }
        }
        return -1;
    }
}
