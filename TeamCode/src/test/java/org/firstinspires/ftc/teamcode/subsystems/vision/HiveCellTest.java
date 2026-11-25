// Sources: new for IronLion30311-HiveMind (QA tests for subsystems/vision/HiveCell and subsystems/BallColor)
package org.firstinspires.ftc.teamcode.subsystems.vision;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.firstinspires.ftc.teamcode.subsystems.BallColor;
import org.firstinspires.ftc.teamcode.util.Alliance;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

/** AprilTag ownership of the HIVE cells and the alliance helpers of {@link BallColor}. */
public class HiveCellTest {

    @Test
    public void everyHiveTagBelongsToExactlyOneCell() {
        Set<Integer> seen = new HashSet<>();
        for (HiveCell cell : HiveCell.values()) {
            assertEquals(4, cell.getTagIds().length);
            for (int i = 0; i < 4; i++) {
                int tagId = cell.getTagIds()[i];
                assertTrue("duplicate tag " + tagId, seen.add(tagId));
                assertEquals(cell, HiveCell.fromTagId(tagId));
                assertEquals(i, cell.indexOfTag(tagId));
            }
        }
        for (int tagId = HiveCell.MIN_TAG_ID; tagId <= HiveCell.MAX_TAG_ID; tagId++) {
            assertTrue("tag " + tagId + " has no cell", seen.contains(tagId));
        }
    }

    @Test
    public void tagsOutsideTheHiveRangeHaveNoCell() {
        assertNull(HiveCell.fromTagId(HiveCell.MIN_TAG_ID - 1));
        assertNull(HiveCell.fromTagId(HiveCell.MAX_TAG_ID + 1));
        assertEquals(-1, HiveCell.RED_BACK.indexOfTag(HiveCell.MAX_TAG_ID));
    }

    @Test
    public void eachAllianceHasABackAndAnAudienceCell() {
        for (Alliance alliance : Alliance.values()) {
            HiveCell back = HiveCell.getBackCell(alliance);
            HiveCell audience = HiveCell.getAudienceCell(alliance);
            assertEquals(alliance, back.getAlliance());
            assertEquals(alliance, audience.getAlliance());
            assertFalse(back == audience);
        }
    }

    @Test
    public void opponentNectarDependsOnTheAlliance() {
        assertTrue(BallColor.BLUE_NECTAR.isOpponentNectar(Alliance.RED));
        assertFalse(BallColor.RED_NECTAR.isOpponentNectar(Alliance.RED));
        assertTrue(BallColor.RED_NECTAR.isOpponentNectar(Alliance.BLUE));
        assertFalse(BallColor.POLLEN.isOpponentNectar(Alliance.RED));
        assertFalse(BallColor.POLLEN.isOpponentNectar(Alliance.BLUE));
    }

    @Test
    public void nectarOfAnAllianceIsNeverItsOpponents() {
        for (Alliance alliance : Alliance.values()) {
            BallColor own = BallColor.nectarOf(alliance);
            assertTrue(own.isNectar());
            assertFalse(own.isOpponentNectar(alliance));
        }
        assertFalse(BallColor.POLLEN.isNectar());
    }
}
