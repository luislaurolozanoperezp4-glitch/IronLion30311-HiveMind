// Sources: new for IronLion30311-HiveMind (BIOBUZZ Competition Manual — values TO VERIFY by the Rules Compliance Agent)
package org.firstinspires.ftc.teamcode.config;

/**
 * Match timing and game-rule limits for BIOBUZZ. Software uses these to stay inside the rules.
 * <p>
 * TO VERIFY against the latest official Competition Manual and Team Updates. If the manual
 * disagrees, the manual wins: change the value here and note it in docs/RULES_COMPLIANCE.md.
 */
public final class MatchConstants {

    private MatchConstants() {
    }

    /** Length of the AUTO period. */
    public static final double AUTO_SECONDS = 30.0;
    /** Length of the TELEOP period (2:00). */
    public static final double TELEOP_SECONDS = 120.0;

    /** G410: NECTAR may only go into FLOWERS during the last 60 seconds. */
    public static final double NECTAR_UNLOCK_SECONDS = 60.0;
    /** Rumble warnings at these seconds remaining in TELEOP. */
    public static final double FIRST_ENDGAME_WARNING_SECONDS = 65.0;
    public static final double SECOND_ENDGAME_WARNING_SECONDS = 60.0;

    /** G407: the robot may control at most this many scoring elements at once. */
    public static final int MAX_CONTROLLED_ELEMENTS = 4;

    /** AUTO time kept in reserve so the robot always reaches PARK before the buzzer. */
    public static final double AUTO_PARK_RESERVE_SECONDS = 4.0;
}
