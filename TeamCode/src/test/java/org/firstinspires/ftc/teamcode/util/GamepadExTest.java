// Sources: new for IronLion30311-HiveMind (QA tests for util/GamepadEx)
package org.firstinspires.ftc.teamcode.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.qualcomm.robotcore.hardware.Gamepad;

import org.firstinspires.ftc.teamcode.config.ControlConstants;
import org.firstinspires.ftc.teamcode.config.DriveConstants;
import org.junit.Test;

/** Edge detection, trigger threshold, stick shaping and null safety of {@link GamepadEx}. */
public class GamepadExTest {

    private static final double EPS = 1e-9;

    private final Gamepad gamepad = new Gamepad();
    private final GamepadEx pad = new GamepadEx(gamepad);

    @Test
    public void justPressedFiresOnlyInTheFirstLoopOfAPress() {
        gamepad.a = true;
        pad.readButtons();
        assertTrue(pad.wasJustPressed(GamepadEx.Button.A));
        assertTrue(pad.isPressed(GamepadEx.Button.A));

        pad.readButtons();
        assertFalse(pad.wasJustPressed(GamepadEx.Button.A));
        assertTrue(pad.isPressed(GamepadEx.Button.A));
    }

    @Test
    public void justReleasedFiresOnlyInTheLoopTheButtonComesUp() {
        gamepad.x = true;
        pad.readButtons();
        gamepad.x = false;
        pad.readButtons();
        assertTrue(pad.wasJustReleased(GamepadEx.Button.X));
        assertFalse(pad.isPressed(GamepadEx.Button.X));

        pad.readButtons();
        assertFalse(pad.wasJustReleased(GamepadEx.Button.X));
    }

    @Test
    public void aSecondTapFiresAgain() {
        gamepad.dpad_up = true;
        pad.readButtons();
        gamepad.dpad_up = false;
        pad.readButtons();
        gamepad.dpad_up = true;
        pad.readButtons();
        assertTrue(pad.wasJustPressed(GamepadEx.Button.DPAD_UP));
    }

    @Test
    public void buttonsAreIndependent() {
        gamepad.left_bumper = true;
        pad.readButtons();
        assertTrue(pad.wasJustPressed(GamepadEx.Button.LEFT_BUMPER));
        assertFalse(pad.wasJustPressed(GamepadEx.Button.RIGHT_BUMPER));
        assertFalse(pad.isPressed(GamepadEx.Button.B));
    }

    @Test
    public void triggerCountsAsPressedOnlyPastTheThreshold() {
        gamepad.right_trigger = (float) ControlConstants.TRIGGER_PRESS_THRESHOLD;
        pad.readButtons();
        assertFalse(pad.isPressed(GamepadEx.Button.RIGHT_TRIGGER));

        gamepad.right_trigger = 1.0f;
        pad.readButtons();
        assertTrue(pad.wasJustPressed(GamepadEx.Button.RIGHT_TRIGGER));
        assertEquals(1.0, pad.getRightTrigger(), EPS);
    }

    @Test
    public void leftStickForwardIsPositiveAndCubed() {
        gamepad.left_stick_y = -0.5f; // the raw gamepad value is negative forward
        assertEquals(0.125, pad.getShapedLeftY(), 1e-6);
        gamepad.left_stick_x = -1.0f;
        assertEquals(-1.0, pad.getShapedLeftX(), EPS);
    }

    @Test
    public void stickInsideTheDeadzoneIsZeroAndAtTheEdgeIsNot() {
        double inside = DriveConstants.STICK_DEADZONE * 0.5;
        assertEquals(0.0, GamepadEx.shapeStick(inside), EPS);
        assertEquals(0.0, GamepadEx.shapeStick(-inside), EPS);
        double edge = DriveConstants.STICK_DEADZONE;
        assertEquals(edge * edge * edge, GamepadEx.shapeStick(edge), EPS);
    }

    @Test
    public void shapedStickKeepsTheSignAndFullScale() {
        assertEquals(1.0, GamepadEx.shapeStick(1.0), EPS);
        assertEquals(-1.0, GamepadEx.shapeStick(-1.0), EPS);
        assertTrue(GamepadEx.shapeStick(-0.3) < 0.0);
    }

    @Test
    public void missingGamepadReadsAsReleasedAndCentered() {
        GamepadEx none = new GamepadEx(null);
        none.readButtons();
        assertFalse(none.isPressed(GamepadEx.Button.A));
        assertFalse(none.wasJustPressed(GamepadEx.Button.A));
        assertEquals(0.0, none.getShapedLeftY(), EPS);
        assertEquals(0.0, none.getShapedRightX(), EPS);
        assertEquals(0.0, none.getLeftTrigger(), EPS);
        none.rumbleShort();
        none.rumbleLong();
        none.rumbleBlips(2);
    }

    @Test
    public void beforeTheFirstReadNothingIsPressed() {
        gamepad.a = true;
        assertFalse(pad.isPressed(GamepadEx.Button.A));
        assertFalse(pad.wasJustPressed(GamepadEx.Button.A));
    }
}
