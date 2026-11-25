// Sources: new for IronLion30311-HiveMind (common shape of every vision-align subsystem @ a7b8f4c)
package org.firstinspires.ftc.teamcode.subsystems;

import org.firstinspires.ftc.robotcore.external.Telemetry;

/**
 * One mechanism of the robot (drivetrain, intake, indexer, launcher, ...).
 * <p>
 * Rules every subsystem follows:
 * <ul>
 *   <li>It gets its hardware from {@code hardware.RobotHardware}, never from the hardware map.</li>
 *   <li>It never reads gamepads. OpModes read gamepads and call subsystem methods.</li>
 *   <li>It never blocks: no {@code sleep()} and no long loops. Timing uses {@code ElapsedTime}.</li>
 *   <li>Its tuning values live in a {@code config/...Constants} class, not in the code.</li>
 * </ul>
 */
public interface Subsystem {

    /** Called once per OpMode loop. Updates motors and state machines. Must return quickly. */
    void periodic();

    /** Stops every motor of this subsystem. Called when the OpMode ends. */
    void stop();

    /**
     * Adds this subsystem's lines to the telemetry.
     *
     * @param t the telemetry to write to
     */
    void addTelemetry(Telemetry t);
}
