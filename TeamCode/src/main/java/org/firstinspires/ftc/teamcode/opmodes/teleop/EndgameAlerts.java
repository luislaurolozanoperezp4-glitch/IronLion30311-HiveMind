// Sources: new for IronLion30311-HiveMind (endgame warnings at MatchConstants.FIRST/SECOND_ENDGAME_WARNING_SECONDS)
package org.firstinspires.ftc.teamcode.opmodes.teleop;

import org.firstinspires.ftc.teamcode.config.MatchConstants;

/**
 * Decides when to warn the drivers that ENDGAME is coming.
 * <p>
 * Call {@link #update(double)} every loop with the seconds left in TELEOP. It returns each
 * alert exactly once: {@link Alert#FIRST_WARNING} at 65 s left and {@link Alert#ENDGAME_START}
 * at 60 s left (values in {@link MatchConstants}). If the OpMode starts late (for example with
 * 62 s left) only the alerts that are still ahead fire, and a passed warning is skipped.
 */
public class EndgameAlerts {

    /** What to tell the drivers this loop. */
    public enum Alert {
        /** Nothing new. */
        NONE,
        /** 65 s left: get ready (short blips). */
        FIRST_WARNING,
        /** 60 s left: ENDGAME starts, NECTAR and the flower arm are allowed (G410). */
        ENDGAME_START
    }

    private boolean isFirstDone = false;
    private boolean isSecondDone = false;

    /**
     * @param remainingSec seconds left in TELEOP
     * @return the alert to give now, or {@link Alert#NONE}
     */
    public Alert update(double remainingSec) {
        if (remainingSec <= 0.0) {
            return Alert.NONE;
        }
        if (!isSecondDone && remainingSec <= MatchConstants.SECOND_ENDGAME_WARNING_SECONDS) {
            isSecondDone = true;
            isFirstDone = true;
            return Alert.ENDGAME_START;
        }
        if (!isFirstDone && remainingSec <= MatchConstants.FIRST_ENDGAME_WARNING_SECONDS) {
            isFirstDone = true;
            return Alert.FIRST_WARNING;
        }
        return Alert.NONE;
    }

    /** Forgets the alerts already given (for a new match). */
    public void reset() {
        isFirstDone = false;
        isSecondDone = false;
    }
}
