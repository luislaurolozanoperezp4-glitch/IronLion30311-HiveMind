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
 * Names are the ones used on the tested robot. Which physical cell each tag range is on, and
 * which one starts UP, is decision D20 (team must confirm).
 */
public enum HiveCell {
    RED_SCORING(Alliance.RED, "Red Scoring (30-33)", 30, 31, 32, 33),
    RED_AUDIENCE(Alliance.RED, "Red Audience (34-37)", 34, 35, 36, 37),
    BLUE_AUDIENCE(Alliance.BLUE, "Blue Audience (38-41)", 38, 39, 40, 41),
    BLUE_SCORING(Alliance.BLUE, "Blue Scoring (42-45)", 42, 43, 44, 45);

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
     * @return the alliance's cell on the scoring side (shot first from the near spot)
     */
    public static HiveCell getScoringCell(Alliance alliance) {
        return alliance == Alliance.RED ? RED_SCORING : BLUE_SCORING;
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
