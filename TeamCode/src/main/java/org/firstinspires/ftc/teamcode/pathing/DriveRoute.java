// Sources: new for IronLion30311-HiveMind (hybrid drive: Pedro path following on long routes, odometry driver on short precise moves)
package org.firstinspires.ftc.teamcode.pathing;

/**
 * The multi-leg routes of the autonomous routines. Each one can be driven either by the TESTED
 * odometry driver ({@link OdometryDriver}) or by Pedro Pathing path following, chosen per route
 * in {@code config.HybridDriveConstants}.
 * <p>
 * Short precise moves (LEAVE, aim turns, the last few inches into PARK, holding still while
 * shooting) always use the odometry driver, whatever is chosen here.
 */
public enum DriveRoute {
    /** Shooting spot up the west lane into PARK (the TESTED 4/4 route). */
    PRELOAD_PARK,
    /** Park after a collect cycle (low edge of the LOADING ZONE). */
    CYCLE_PARK,
    /** Around the HIVE to the other-cell spot, and back down the west lane. */
    OTHER_CELL_TRANSIT,
    /** From the other-cell spot north of the HIVE into PARK. */
    OTHER_SIDE_PARK,
    /** Wait Tip autos: into the upper part of the LOADING ZONE. */
    WAIT_TIP_PARK,
    /** Auto Safe Park legs. */
    SAFE_PARK,
    /** The time guard's emergency park (cancels the current step when time is short). */
    EMERGENCY_PARK
}
