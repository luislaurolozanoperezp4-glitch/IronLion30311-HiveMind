// Sources: IronLion30311BIOBUZZInicioDeTemporada / branch feature/turrubiates/vision-align / TeamCode/.../subsystems/Shooter.java @ a7b8f4c (handleRpmTuning rising-edge detection)
//          IronLion30311BIOBUZZInicioDeTemporada / branch feature/turrubiates/vision-align / TeamCode/.../opmodes/teleop/MainTeleOp.java @ a7b8f4c (shapeStickInput: deadzone + cubic)
//          — adapted for BIOBUZZ
package org.firstinspires.ftc.teamcode.util;

import com.qualcomm.robotcore.hardware.Gamepad;

import org.firstinspires.ftc.teamcode.config.ControlConstants;
import org.firstinspires.ftc.teamcode.config.DriveConstants;

/**
 * A gamepad with "was just pressed" detection, shaped sticks and rumble helpers.
 * <p>
 * Call {@link #readButtons()} once at the start of every loop. Then:
 * <ul>
 *   <li>{@link #isPressed(Button)}: the button is held down now.</li>
 *   <li>{@link #wasJustPressed(Button)}: it went down this loop (fires once per tap).</li>
 *   <li>{@link #wasJustReleased(Button)}: it came up this loop.</li>
 * </ul>
 * Sticks are shaped like on the tested robot: a small deadzone, then cubed, so small stick
 * moves give fine control. {@link #getShapedLeftY()} is positive when the stick is pushed
 * forward (the raw gamepad value is negative forward).
 */
public class GamepadEx {

    /** Every digital input. Triggers count as pressed past {@link ControlConstants#TRIGGER_PRESS_THRESHOLD}. */
    public enum Button {
        A, B, X, Y,
        DPAD_UP, DPAD_DOWN, DPAD_LEFT, DPAD_RIGHT,
        LEFT_BUMPER, RIGHT_BUMPER, LEFT_TRIGGER, RIGHT_TRIGGER,
        LEFT_STICK_BUTTON, RIGHT_STICK_BUTTON, BACK, START
    }

    private final Gamepad gamepad;
    private final boolean[] current = new boolean[Button.values().length];
    private final boolean[] previous = new boolean[Button.values().length];

    /** @param gamepad {@code gamepad1} or {@code gamepad2} from the OpMode */
    public GamepadEx(Gamepad gamepad) {
        this.gamepad = gamepad;
    }

    /** Takes a snapshot of every button. Call once per loop, before reading any button. */
    public void readButtons() {
        System.arraycopy(current, 0, previous, 0, current.length);
        if (gamepad == null) {
            return;
        }
        set(Button.A, gamepad.a);
        set(Button.B, gamepad.b);
        set(Button.X, gamepad.x);
        set(Button.Y, gamepad.y);
        set(Button.DPAD_UP, gamepad.dpad_up);
        set(Button.DPAD_DOWN, gamepad.dpad_down);
        set(Button.DPAD_LEFT, gamepad.dpad_left);
        set(Button.DPAD_RIGHT, gamepad.dpad_right);
        set(Button.LEFT_BUMPER, gamepad.left_bumper);
        set(Button.RIGHT_BUMPER, gamepad.right_bumper);
        set(Button.LEFT_TRIGGER, gamepad.left_trigger > ControlConstants.TRIGGER_PRESS_THRESHOLD);
        set(Button.RIGHT_TRIGGER, gamepad.right_trigger > ControlConstants.TRIGGER_PRESS_THRESHOLD);
        set(Button.LEFT_STICK_BUTTON, gamepad.left_stick_button);
        set(Button.RIGHT_STICK_BUTTON, gamepad.right_stick_button);
        set(Button.BACK, gamepad.back);
        set(Button.START, gamepad.start);
    }

    /**
     * @param button the button
     * @return true while it is held down
     */
    public boolean isPressed(Button button) {
        return current[button.ordinal()];
    }

    /**
     * @param button the button
     * @return true only in the loop where it went down
     */
    public boolean wasJustPressed(Button button) {
        return current[button.ordinal()] && !previous[button.ordinal()];
    }

    /**
     * @param button the button
     * @return true only in the loop where it came up
     */
    public boolean wasJustReleased(Button button) {
        return !current[button.ordinal()] && previous[button.ordinal()];
    }

    /** @return left stick forward/back, shaped, +1 = pushed forward */
    public double getShapedLeftY() {
        return gamepad == null ? 0.0 : shapeStick(-gamepad.left_stick_y);
    }

    /** @return left stick left/right, shaped, +1 = right */
    public double getShapedLeftX() {
        return gamepad == null ? 0.0 : shapeStick(gamepad.left_stick_x);
    }

    /** @return right stick left/right, shaped, +1 = right (turn clockwise) */
    public double getShapedRightX() {
        return gamepad == null ? 0.0 : shapeStick(gamepad.right_stick_x);
    }

    /** @return how far the left trigger is pulled, 0 to 1 */
    public double getLeftTrigger() {
        return gamepad == null ? 0.0 : gamepad.left_trigger;
    }

    /** @return how far the right trigger is pulled, 0 to 1 */
    public double getRightTrigger() {
        return gamepad == null ? 0.0 : gamepad.right_trigger;
    }

    /** Rumbles once, short ({@link ControlConstants#SHORT_RUMBLE_MILLIS}). */
    public void rumbleShort() {
        if (gamepad != null) {
            gamepad.rumble(ControlConstants.SHORT_RUMBLE_MILLIS);
        }
    }

    /** Rumbles once, long ({@link ControlConstants#LONG_RUMBLE_MILLIS}). */
    public void rumbleLong() {
        if (gamepad != null) {
            gamepad.rumble(ControlConstants.LONG_RUMBLE_MILLIS);
        }
    }

    /**
     * Rumbles a number of short blips (for example 2 blips = "60 seconds left").
     *
     * @param count how many blips
     */
    public void rumbleBlips(int count) {
        if (gamepad != null) {
            gamepad.rumbleBlips(count);
        }
    }

    /**
     * Shapes a raw stick value as on the tested robot: inside the deadzone it is 0, otherwise it
     * is cubed (keeps the sign, makes small moves smaller).
     *
     * @param value raw stick value, -1 to 1
     * @return shaped value, -1 to 1
     */
    public static double shapeStick(double value) {
        return Math.abs(value) < DriveConstants.STICK_DEADZONE ? 0.0 : value * value * value;
    }

    private void set(Button button, boolean isDown) {
        current[button.ordinal()] = isDown;
    }
}
