// Sources: new for IronLion30311-HiveMind (tests for opmodes/auto/AutoSelector)
package org.firstinspires.ftc.teamcode.opmodes.auto;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.firstinspires.ftc.teamcode.config.AutoConstants;
import org.firstinspires.ftc.teamcode.util.Alliance;
import org.firstinspires.ftc.teamcode.util.GamepadEx.Button;
import org.junit.Test;

/** The INIT menu: alliance, start position and start delay. */
public class AutoSelectorTest {

    private static final double EPSILON = 1e-9;

    @Test
    public void fixedAllianceIgnoresTheAllianceButtons() {
        AutoSelector selector = new AutoSelector(Alliance.BLUE, StartPosition.NEAR);
        assertFalse(selector.onPress(Button.B));
        assertEquals(Alliance.BLUE, selector.getAlliance());
        assertTrue(selector.isAllianceFixed());
    }

    @Test
    public void freeAllianceStartsRedAndSwitches() {
        AutoSelector selector = new AutoSelector(null, StartPosition.NEAR);
        assertEquals(Alliance.RED, selector.getAlliance());
        assertTrue(selector.onPress(Button.X));
        assertEquals(Alliance.BLUE, selector.getAlliance());
        assertFalse("Same alliance again is no change", selector.onPress(Button.X));
        assertTrue(selector.onPress(Button.B));
        assertEquals(Alliance.RED, selector.getAlliance());
    }

    @Test
    public void delayStepsAndClamps() {
        AutoSelector selector = new AutoSelector(Alliance.RED, StartPosition.NEAR);
        assertFalse("No negative delay", selector.onPress(Button.DPAD_DOWN));
        assertEquals(0.0, selector.getStartDelaySeconds(), EPSILON);
        selector.onPress(Button.DPAD_UP);
        assertEquals(AutoConstants.START_DELAY_STEP_SECONDS, selector.getStartDelaySeconds(), EPSILON);
        for (int i = 0; i < 100; i++) {
            selector.onPress(Button.DPAD_UP);
        }
        assertEquals(AutoConstants.START_DELAY_MAX_SECONDS, selector.getStartDelaySeconds(), EPSILON);
        assertFalse(selector.onPress(Button.DPAD_UP));
        selector.onPress(Button.DPAD_DOWN);
        assertEquals(AutoConstants.START_DELAY_MAX_SECONDS - AutoConstants.START_DELAY_STEP_SECONDS,
                selector.getStartDelaySeconds(), EPSILON);
    }

    @Test
    public void startPositionCyclesBothWays() {
        AutoSelector selector = new AutoSelector(null, StartPosition.NEAR, StartPosition.FAR,
                StartPosition.LOADING_ZONE);
        assertTrue(selector.hasStartChoice());
        assertEquals(StartPosition.NEAR, selector.getStartPosition());
        selector.onPress(Button.DPAD_RIGHT);
        assertEquals(StartPosition.FAR, selector.getStartPosition());
        selector.onPress(Button.DPAD_LEFT);
        selector.onPress(Button.DPAD_LEFT);
        assertEquals(StartPosition.LOADING_ZONE, selector.getStartPosition());
    }

    @Test
    public void singleStartPositionCannotChange() {
        AutoSelector selector = new AutoSelector(Alliance.RED, StartPosition.FAR);
        assertFalse(selector.hasStartChoice());
        assertFalse(selector.onPress(Button.DPAD_RIGHT));
        assertEquals(StartPosition.FAR, selector.getStartPosition());
    }

    @Test(expected = IllegalArgumentException.class)
    public void needsAStartPosition() {
        new AutoSelector(Alliance.RED);
    }
}
