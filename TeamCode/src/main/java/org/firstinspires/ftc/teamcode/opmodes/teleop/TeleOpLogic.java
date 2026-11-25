// Sources: IronLion30311BIOBUZZInicioDeTemporada / branch feature/turrubiates/vision-align / TeamCode/.../opmodes/teleop/MainTeleOp.java @ a7b8f4c (isDriverMoving, light choice while firing, speed-mode light)
//          — adapted for BIOBUZZ (pure helpers so they can be tested without a robot)
package org.firstinspires.ftc.teamcode.opmodes.teleop;

import org.firstinspires.ftc.teamcode.config.MatchConstants;
import org.firstinspires.ftc.teamcode.subsystems.BallColor;
import org.firstinspires.ftc.teamcode.util.Alliance;

import java.util.Locale;

/**
 * Small TeleOp decisions with no hardware, so JUnit can test them.
 * <p>
 * {@link TeleOpBase} asks these methods what to do; they never touch a motor.
 */
public final class TeleOpLogic {

    /** What the indicator light should show. {@link TeleOpBase} turns it into a color. */
    public enum LightCue {
        /** White: the robot is aligning itself to the HIVE before feeding. */
        ALIGNING,
        /** Solid yellow: launcher ready, balls are being fed. */
        FEEDING,
        /** Blinking yellow: launcher spinning up (or recovering after a shot). */
        SPINNING_UP,
        /** Solid blue: slow drive mode. */
        SLOW,
        /** Solid red: fast drive mode. */
        FAST,
        /** Blinking blue: slow mode during ENDGAME (last 60 s). */
        ENDGAME_SLOW,
        /** Blinking red: fast mode during ENDGAME (last 60 s). */
        ENDGAME_FAST
    }

    private static final int SECONDS_PER_MINUTE = 60;

    private TeleOpLogic() {
    }

    /**
     * @param forward shaped forward stick
     * @param strafe  shaped strafe stick
     * @param turn    shaped turn stick
     * @return true if any drive input is not zero
     */
    public static boolean isDriverMoving(double forward, double strafe, double turn) {
        return forward != 0.0 || strafe != 0.0 || turn != 0.0;
    }

    /**
     * Light priority, as on the tested robot: launcher state while firing, otherwise the drive
     * speed mode. ENDGAME makes the speed-mode color blink so the drivers notice it.
     *
     * @param isFiring   true while a fire trigger is held or auto-shoot runs
     * @param isAimed    true if not aligning, or aligned
     * @param isFeeding  true while the indexer feeds
     * @param isEndgame  true in the last 60 s of TELEOP
     * @param isSlowMode true in slow drive mode
     * @return the light cue
     */
    public static LightCue pickLight(boolean isFiring, boolean isAimed, boolean isFeeding,
                                     boolean isEndgame, boolean isSlowMode) {
        if (isFiring) {
            if (!isAimed) {
                return LightCue.ALIGNING;
            }
            return isFeeding ? LightCue.FEEDING : LightCue.SPINNING_UP;
        }
        if (isEndgame) {
            return isSlowMode ? LightCue.ENDGAME_SLOW : LightCue.ENDGAME_FAST;
        }
        return isSlowMode ? LightCue.SLOW : LightCue.FAST;
    }

    /**
     * How many balls an auto-shoot volley should expect. With a working ball counter it is the
     * counted number; otherwise (or when the count is 0, which may be wrong) the most the robot
     * may hold, and the volley ends by itself a few seconds after the last shot.
     *
     * @param countedBalls       elements the counter thinks the robot holds
     * @param isCounterConnected true if the HuskyLens counter works
     * @return balls to expect, at least 1
     */
    public static int ballsForVolley(int countedBalls, boolean isCounterConnected) {
        if (!isCounterConnected || countedBalls <= 0) {
            return MatchConstants.MAX_CONTROLLED_ELEMENTS;
        }
        return Math.min(countedBalls, MatchConstants.MAX_CONTROLLED_ELEMENTS);
    }

    /**
     * Ball-assist colors: POLLEN and our own NECTAR. The tested robot turned toward any ball;
     * turning toward the opponent's NECTAR only makes the intake reject it (G408).
     *
     * @param alliance our alliance
     * @return the colors the ball assist may turn toward
     */
    public static BallColor[] assistColors(Alliance alliance) {
        return new BallColor[]{BallColor.POLLEN, BallColor.nectarOf(alliance)};
    }

    /**
     * @param remainingSec seconds left in the period
     * @return "m:ss" (never negative)
     */
    public static String formatClock(double remainingSec) {
        int totalSec = (int) Math.ceil(Math.max(0.0, remainingSec));
        return String.format(Locale.US, "%d:%02d", totalSec / SECONDS_PER_MINUTE,
                totalSec % SECONDS_PER_MINUTE);
    }
}
