// Sources: IronLion30311BIOBUZZInicioDeTemporada / branch feature/turrubiates/vision-align / TeamCode/.../subsystems/BatteryMonitor.java @ a7b8f4c (whole class: low warning with time filter + hysteresis, sag drive limit)
//          — adapted for BIOBUZZ (update() -> periodic(), constants in RobotConstants, injectable clock and voltage)
package org.firstinspires.ftc.teamcode.subsystems;

import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.teamcode.config.RobotConstants;
import org.firstinspires.ftc.teamcode.hardware.RobotHardware;
import org.firstinspires.ftc.teamcode.util.MatchTimer;

import java.util.function.DoubleSupplier;

/**
 * Watches the battery voltage: warns about a tired battery and protects against brownouts.
 * <ul>
 *   <li><b>Low battery</b>: below {@link RobotConstants#BATTERY_LOW_VOLTS} for
 *   {@link RobotConstants#BATTERY_LOW_SECONDS} (short dips under load do not count). Clears above
 *   {@link RobotConstants#BATTERY_LOW_CLEAR_VOLTS}. It means: swap the battery before the next
 *   match. The launcher still shoots at the right RPM (the indexer waits), only slower.</li>
 *   <li><b>Sag</b>: below {@link RobotConstants#BATTERY_SAG_VOLTS}. The Control Hub resets near
 *   7 V, so while sagging (and {@link RobotConstants#BATTERY_SAG_HOLD_SECONDS} after)
 *   {@link #getDrivePowerLimit()} caps the drivetrain, the biggest load.</li>
 * </ul>
 */
public class BatteryMonitor implements Subsystem {

    private final DoubleSupplier voltsSource;
    private final MatchTimer.Clock clock;

    private double volts = Double.NaN;
    private double startVolts = Double.NaN;
    private double minVolts = Double.NaN;
    private double lastReadSec = Double.NaN;
    private double belowLowSinceSec = Double.NaN;
    private double lastSagSec = Double.NaN;
    private boolean isLow = false;
    private boolean hasBeenLow = false;
    private boolean isSagging = false;
    private int sagCount = 0;

    /** @param hardware robot hardware, for the battery voltage */
    public BatteryMonitor(final RobotHardware hardware) {
        this(new DoubleSupplier() {
            @Override
            public double getAsDouble() {
                return hardware.getBatteryVolts();
            }
        }, MatchTimer.systemClock());
    }

    /**
     * For tests.
     *
     * @param voltsSource battery voltage
     * @param clock       time source
     */
    public BatteryMonitor(DoubleSupplier voltsSource, MatchTimer.Clock clock) {
        this.voltsSource = voltsSource;
        this.clock = clock;
    }

    /** Reads the voltage when due and updates the warnings. Call once per loop. */
    @Override
    public void periodic() {
        double nowSec = clock.nowSeconds();
        if (!Double.isNaN(lastReadSec)
                && nowSec - lastReadSec < RobotConstants.BATTERY_READ_INTERVAL_SECONDS) {
            return;
        }
        lastReadSec = nowSec;
        double reading = voltsSource.getAsDouble();
        if (reading < RobotConstants.BATTERY_MIN_VALID_VOLTS) {
            return;
        }
        volts = reading;
        if (Double.isNaN(startVolts)) {
            startVolts = volts;
        }
        minVolts = Double.isNaN(minVolts) ? volts : Math.min(minVolts, volts);

        // Low battery, with a time filter and hysteresis
        if (volts < RobotConstants.BATTERY_LOW_VOLTS) {
            if (Double.isNaN(belowLowSinceSec)) {
                belowLowSinceSec = nowSec;
            }
            if (nowSec - belowLowSinceSec >= RobotConstants.BATTERY_LOW_SECONDS) {
                isLow = true;
                hasBeenLow = true;
            }
        } else {
            belowLowSinceSec = Double.NaN;
            if (volts > RobotConstants.BATTERY_LOW_CLEAR_VOLTS) {
                isLow = false;
            }
        }

        // Sag: count each new event, hold the drive cap a little after it
        boolean isBelowSag = volts < RobotConstants.BATTERY_SAG_VOLTS;
        if (isBelowSag) {
            if (!isSagging) {
                sagCount++;
            }
            lastSagSec = nowSec;
        }
        isSagging = isBelowSag;
    }

    /** Nothing to stop: the monitor only reads. */
    @Override
    public void stop() {
    }

    /** @return the last valid voltage, or NaN before the first reading */
    public double getVolts() {
        return volts;
    }

    /** @return the first valid voltage of the OpMode */
    public double getStartVolts() {
        return startVolts;
    }

    /** @return the lowest valid voltage of the OpMode */
    public double getMinVolts() {
        return minVolts;
    }

    /** @return true while the low-battery warning is on */
    public boolean isLow() {
        return isLow;
    }

    /** @return true if the low-battery warning was on at any time */
    public boolean hasBeenLow() {
        return hasBeenLow;
    }

    /** @return how many times the voltage fell below the sag limit */
    public int getSagCount() {
        return sagCount;
    }

    /** @return drive power cap: {@link RobotConstants#BATTERY_SAG_DRIVE_LIMIT} during a sag, else 1 */
    public double getDrivePowerLimit() {
        boolean isLimited = !Double.isNaN(lastSagSec)
                && clock.nowSeconds() - lastSagSec < RobotConstants.BATTERY_SAG_HOLD_SECONDS;
        return isLimited ? RobotConstants.BATTERY_SAG_DRIVE_LIMIT : 1.0;
    }

    @Override
    public void addTelemetry(Telemetry t) {
        t.addData("Battery", "%.2f V (min %.2f)%s", volts, minVolts,
                isLow ? " LOW - swap battery" : "");
    }
}
