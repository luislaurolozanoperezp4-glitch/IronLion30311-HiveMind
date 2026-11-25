// Sources: new for IronLion30311-HiveMind (hybrid drive switch; see docs/PEDRO_TUNING.md)
package org.firstinspires.ftc.teamcode.config;

import com.acmerobotics.dashboard.config.Config;

/**
 * Chooses, route by route, whether AUTO drives with Pedro Pathing path following or with the
 * TESTED odometry driver.
 * <p>
 * <b>Safety:</b> Pedro is used only when {@link #IS_PEDRO_TUNED} is true AND the route's own
 * switch is true. Set {@code IS_PEDRO_TUNED = true} only after the Pedro AutoTune values were
 * copied into {@code pathing/PedroConstants.java} and Test Odometry Square passed (steps in
 * {@code docs/PEDRO_TUNING.md}). Until then every route uses the odometry driver, exactly as in
 * the 4/4 field test.
 * <p>
 * Recommended order: switch ONE long route on, compare time and end accuracy with the
 * odometry driver on the practice field, then the next one. Short precise moves always use the
 * odometry driver.
 */
@Config
public final class HybridDriveConstants {

    private HybridDriveConstants() {
    }

    /** Master safety switch: true only after AutoTune (docs/PEDRO_TUNING.md). */
    public static boolean IS_PEDRO_TUNED = false;

    // ---------------- Per route: true = Pedro path following, false = odometry driver ----------------
    /** Best first candidate: the long west-lane leg of the TESTED route. */
    public static boolean PEDRO_PRELOAD_PARK = false;
    public static boolean PEDRO_CYCLE_PARK = false;
    /** Good candidate: long legs around the HIVE, where carrying speed through corners saves time. */
    public static boolean PEDRO_OTHER_CELL_TRANSIT = false;
    public static boolean PEDRO_OTHER_SIDE_PARK = false;
    public static boolean PEDRO_WAIT_TIP_PARK = false;
    public static boolean PEDRO_SAFE_PARK = false;
    /** Keep false until the others are proven: the emergency park must never surprise anyone. */
    public static boolean PEDRO_EMERGENCY_PARK = false;

    // ---------------- Pedro leg limits ----------------
    /** A Pedro route gives up after base + length / rate seconds, then the odometry driver finishes. TODO: TUNE */
    public static double PEDRO_TIMEOUT_BASE_SECONDS = 1.5;
    public static double PEDRO_TIMEOUT_INCHES_PER_SECOND = 20.0;
}
