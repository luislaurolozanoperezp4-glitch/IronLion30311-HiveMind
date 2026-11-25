// Sources: IronLion30311BIOBUZZInicioDeTemporada / branch feature/turrubiates/vision-align / TeamCode/.../subsystems/pathing/PinpointDiagnostics.java @ 023ae35 (update, hasFault, summary, csvFields, status counters) — adapted for BIOBUZZ (device from RobotHardware, testable record(), a missing Pinpoint is "not configured", not a fault)
package org.firstinspires.ftc.teamcode.pathing;

import com.qualcomm.hardware.gobilda.GoBildaPinpointDriver;

import java.util.Locale;

/**
 * Health of the goBILDA Pinpoint odometry computer, to explain lost odometry in the logs.
 * <p>
 * It reads what Pedro's localizer fetched in its last {@code update()} (no extra I2C traffic):
 * the device status ({@code READY}, {@code CALIBRATING}, {@code NOT_READY} or a fault such as a
 * pod not detected, IMU runaway or a bad read), the Pinpoint loop frequency and the raw counts
 * of both pods. A pod count that stays still while the robot moves, a fault status or a
 * frequency drop shows up in the logs before (or when) the pose goes wrong.
 * <p>
 * Field notes (vision-align, 2026-11-24): the Pinpoint rebooted to (0, 0, 0) a few times (a wall
 * hit, vibration while shooting, a battery swap without a full power cycle). After a battery
 * swap it once kept a bad state until the robot was fully power-cycled. {@code AutoBase} warns
 * about a fault in INIT and stops the drive if the Pinpoint reports a fault at START.
 * <p>
 * Call {@link #update()} once per loop, after {@code follower.update()}.
 */
public class PinpointDiagnostics {

    private final GoBildaPinpointDriver pinpoint;
    private GoBildaPinpointDriver.DeviceStatus lastStatus;
    private GoBildaPinpointDriver.DeviceStatus worstStatus;
    private int statusChanges = 0;
    private int faultReads = 0;

    /** @param pinpoint the Pinpoint from {@code RobotHardware.pinpoint}, or null if missing */
    public PinpointDiagnostics(GoBildaPinpointDriver pinpoint) {
        this.pinpoint = pinpoint;
    }

    /** Reads the latest status. Call once per loop, after the follower update. */
    public void update() {
        if (pinpoint != null) {
            record(pinpoint.getDeviceStatus());
        }
    }

    /**
     * Counts one status reading (public so tests can feed statuses without a device).
     *
     * @param status the status read this loop
     */
    public void record(GoBildaPinpointDriver.DeviceStatus status) {
        if (lastStatus != null && status != lastStatus) {
            statusChanges++;
        }
        if (isFault(status)) {
            faultReads++;
            worstStatus = status;
        }
        lastStatus = status;
    }

    /**
     * @param status a Pinpoint status
     * @return true for anything but READY and CALIBRATING (NOT_READY counts as a fault)
     */
    public static boolean isFault(GoBildaPinpointDriver.DeviceStatus status) {
        return status != GoBildaPinpointDriver.DeviceStatus.READY
                && status != GoBildaPinpointDriver.DeviceStatus.CALIBRATING;
    }

    /** @return true if the Pinpoint is in the configuration */
    public boolean isConfigured() {
        return pinpoint != null;
    }

    /** @return true if the last status read was a fault (false without a Pinpoint or a read) */
    public boolean hasFault() {
        return lastStatus != null && isFault(lastStatus);
    }

    /** @return the last status read, or null */
    public GoBildaPinpointDriver.DeviceStatus getStatus() {
        return lastStatus;
    }

    /** @return status changes since the start (a reboot shows up as changes) */
    public int getStatusChanges() {
        return statusChanges;
    }

    /** @return loops that read a fault status */
    public int getFaultReads() {
        return faultReads;
    }

    /** @return the last fault status seen, or null if none */
    public GoBildaPinpointDriver.DeviceStatus getWorstStatus() {
        return worstStatus;
    }

    /** @return for example "READY, 1480 Hz, encX 1234, encY -56" (telemetry) */
    public String summary() {
        if (pinpoint == null) {
            return "NOT CONFIGURED";
        }
        return String.format(Locale.US, "%s, %.0f Hz, encX %d, encY %d", lastStatus,
                pinpoint.getFrequency(), pinpoint.getEncoderX(), pinpoint.getEncoderY());
    }

    /** @return one line for the end-of-AUTO log: fault reads, status changes, worst status */
    public String getMatchSummary() {
        return String.format(Locale.US, "Pinpoint: %d fault reads, %d status changes, worst %s",
                faultReads, statusChanges, worstStatus == null ? "none" : worstStatus);
    }

    /** @return CSV fields: status, frequency (Hz), raw X and Y pod counts (4 fields) */
    public String csvFields() {
        if (pinpoint == null) {
            return ",,,";
        }
        return String.format(Locale.US, "%s,%.0f,%d,%d", lastStatus, pinpoint.getFrequency(),
                pinpoint.getEncoderX(), pinpoint.getEncoderY());
    }
}
