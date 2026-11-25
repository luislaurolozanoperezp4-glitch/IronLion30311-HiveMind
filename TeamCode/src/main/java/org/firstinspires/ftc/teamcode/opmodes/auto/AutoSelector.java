// Sources: IronLion30311BIOBUZZInicioDeTemporada / branch feature/turrubiates/vision-align / TeamCode/.../opmodes/auto/PedroAutoBase.java @ a7b8f4c (INIT alliance buttons: B = red, X = blue)
//          IronLion30311BIOBUZZInicioDeTemporada / branch feature/turrubiates/auto-leave-park / TeamCode/.../opmodes/auto/LeaveParkAuto.java @ e455142 (start delay on the D-pad)
//          — adapted for BIOBUZZ
package org.firstinspires.ftc.teamcode.opmodes.auto;

import org.firstinspires.ftc.teamcode.config.AutoConstants;
import org.firstinspires.ftc.teamcode.util.Alliance;
import org.firstinspires.ftc.teamcode.util.GamepadEx;

import java.util.Locale;

/**
 * The INIT menu of every autonomous OpMode (gamepad1):
 * <ul>
 *     <li>B = RED, X = BLUE (only when the OpMode does not fix the alliance);</li>
 *     <li>D-pad left / right = start position (only when there is more than one);</li>
 *     <li>D-pad up / down = start delay, 0 to {@link AutoConstants#START_DELAY_MAX_SECONDS} in
 *     {@link AutoConstants#START_DELAY_STEP_SECONDS} steps, to let the alliance partner go first.</li>
 * </ul>
 * Pure logic: feed it button presses with {@link #onPress(GamepadEx.Button)} (from
 * {@code GamepadEx.wasJustPressed}). Tested by {@code AutoSelectorTest}.
 */
public class AutoSelector {

    private final boolean isAllianceFixed;
    private final StartPosition[] startPositions;
    private Alliance alliance;
    private int startIndex;
    private double startDelaySeconds;

    /**
     * @param fixedAlliance  the alliance, or null to let the drive team choose (default RED)
     * @param startPositions the start positions this OpMode can run from (the first is the default)
     */
    public AutoSelector(Alliance fixedAlliance, StartPosition... startPositions) {
        if (startPositions.length == 0) {
            throw new IllegalArgumentException("At least one start position is needed");
        }
        this.isAllianceFixed = fixedAlliance != null;
        this.alliance = fixedAlliance != null ? fixedAlliance : Alliance.RED;
        this.startPositions = startPositions.clone();
    }

    /**
     * Applies one button press.
     *
     * @param button the button that was just pressed
     * @return true if a choice changed (the start pose may have to be set again)
     */
    public boolean onPress(GamepadEx.Button button) {
        switch (button) {
            case B:
                return setAlliance(Alliance.RED);
            case X:
                return setAlliance(Alliance.BLUE);
            case DPAD_RIGHT:
                return moveStart(1);
            case DPAD_LEFT:
                return moveStart(-1);
            case DPAD_UP:
                return changeDelay(AutoConstants.START_DELAY_STEP_SECONDS);
            case DPAD_DOWN:
                return changeDelay(-AutoConstants.START_DELAY_STEP_SECONDS);
            default:
                return false;
        }
    }

    private boolean setAlliance(Alliance wanted) {
        if (isAllianceFixed || alliance == wanted) {
            return false;
        }
        alliance = wanted;
        return true;
    }

    private boolean moveStart(int step) {
        if (startPositions.length < 2) {
            return false;
        }
        startIndex = (startIndex + step + startPositions.length) % startPositions.length;
        return true;
    }

    private boolean changeDelay(double delta) {
        double steps = Math.round((startDelaySeconds + delta) / AutoConstants.START_DELAY_STEP_SECONDS);
        double wanted = Math.max(0.0, Math.min(AutoConstants.START_DELAY_MAX_SECONDS,
                steps * AutoConstants.START_DELAY_STEP_SECONDS));
        if (wanted == startDelaySeconds) {
            return false;
        }
        startDelaySeconds = wanted;
        return true;
    }

    /** @return the chosen (or fixed) alliance */
    public Alliance getAlliance() {
        return alliance;
    }

    /** @return true if the OpMode fixes the alliance (no B / X choice) */
    public boolean isAllianceFixed() {
        return isAllianceFixed;
    }

    /** @return the chosen start position */
    public StartPosition getStartPosition() {
        return startPositions[startIndex];
    }

    /** @return true if the drive team can choose the start position */
    public boolean hasStartChoice() {
        return startPositions.length > 1;
    }

    /** @return the chosen start delay, seconds */
    public double getStartDelaySeconds() {
        return startDelaySeconds;
    }

    /** @return the controls line for the telemetry */
    public String getControlsText() {
        StringBuilder text = new StringBuilder("gamepad1: ");
        if (!isAllianceFixed) {
            text.append("B = RED, X = BLUE | ");
        }
        if (hasStartChoice()) {
            text.append("D-pad L/R = start | ");
        }
        text.append("D-pad Up/Down = delay | A = re-zero odometry");
        return text.toString();
    }

    /** @return the choices in one line for the telemetry */
    public String getSummary() {
        return String.format(Locale.US, "%s | %s | delay %.1f s", alliance,
                getStartPosition().getLabel(), startDelaySeconds);
    }
}
