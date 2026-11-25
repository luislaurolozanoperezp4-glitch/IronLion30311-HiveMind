// Sources: new for IronLion30311-HiveMind (QA tests for util/PoseStorage)
package org.firstinspires.ftc.teamcode.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import com.pedropathing.math.Pose;

import org.junit.After;
import org.junit.Test;

/** The AUTO -> TeleOp pose hand-off of {@link PoseStorage}. */
public class PoseStorageTest {

    @After
    public void clearStorage() {
        PoseStorage.clear();
    }

    @Test
    public void startsEmptyAfterClear() {
        PoseStorage.clear();
        assertFalse(PoseStorage.hasPose());
        assertNull(PoseStorage.getPose());
        assertNull(PoseStorage.getAlliance());
    }

    @Test
    public void keepsTheSavedPoseAndAlliance() {
        Pose pose = new Pose(12.0, 34.0, Math.PI / 2);
        PoseStorage.save(pose, Alliance.BLUE);
        assertTrue(PoseStorage.hasPose());
        assertSame(pose, PoseStorage.getPose());
        assertEquals(Alliance.BLUE, PoseStorage.getAlliance());
    }

    @Test
    public void aLaterSaveReplacesTheEarlierOne() {
        PoseStorage.save(new Pose(1.0, 2.0, 0.0), Alliance.RED);
        Pose last = new Pose(5.0, 6.0, 1.0);
        PoseStorage.save(last, Alliance.BLUE);
        assertSame(last, PoseStorage.getPose());
        assertEquals(Alliance.BLUE, PoseStorage.getAlliance());
    }

    @Test
    public void clearForgetsBothPoseAndAlliance() {
        PoseStorage.save(new Pose(1.0, 2.0, 0.0), Alliance.RED);
        PoseStorage.clear();
        assertFalse(PoseStorage.hasPose());
        assertNull(PoseStorage.getAlliance());
    }

    @Test
    public void savingANullPoseMeansNoPose() {
        PoseStorage.save(null, Alliance.RED);
        assertFalse(PoseStorage.hasPose());
    }
}
