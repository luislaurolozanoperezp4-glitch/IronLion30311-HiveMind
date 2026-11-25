// Sources: new for IronLion30311-HiveMind (QA tests for subsystems/BatteryMonitor)
package org.firstinspires.ftc.teamcode.subsystems;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.firstinspires.ftc.teamcode.config.RobotConstants;
import org.firstinspires.ftc.teamcode.testutil.FakeClock;
import org.junit.Test;

import java.util.function.DoubleSupplier;

/** Low-battery warning (time filter + hysteresis) and sag drive limit of {@link BatteryMonitor}. */
public class BatteryMonitorTest {

    private static final double EPS = 1e-9;

    private final FakeClock clock = new FakeClock();
    private double volts = 12.8;
    private final BatteryMonitor monitor = new BatteryMonitor(new DoubleSupplier() {
        @Override
        public double getAsDouble() {
            return volts;
        }
    }, clock);

    /** Reads once, after enough time for a new sample. */
    private void read(double newVolts, double afterSec) {
        volts = newVolts;
        clock.advance(afterSec);
        monitor.periodic();
    }

    @Test
    public void tracksStartAndMinimumVoltage() {
        read(12.8, 0.0);
        read(12.1, 1.0);
        read(12.5, 1.0);
        assertEquals(12.8, monitor.getStartVolts(), EPS);
        assertEquals(12.1, monitor.getMinVolts(), EPS);
        assertEquals(12.5, monitor.getVolts(), EPS);
    }

    @Test
    public void ignoresImpossibleReadings() {
        read(12.0, 0.0);
        read(0.0, 1.0); // hub not reporting
        assertEquals(12.0, monitor.getVolts(), EPS);
        assertEquals(12.0, monitor.getMinVolts(), EPS);
    }

    @Test
    public void readsAtMostOncePerInterval() {
        read(12.0, 0.0);
        read(11.0, RobotConstants.BATTERY_READ_INTERVAL_SECONDS / 2);
        assertEquals(12.0, monitor.getVolts(), EPS);
    }

    @Test
    public void lowOnlyAfterStayingLowLongEnough() {
        double low = RobotConstants.BATTERY_LOW_VOLTS - 0.2;
        read(low, 0.0);
        read(low, RobotConstants.BATTERY_LOW_SECONDS / 2);
        assertFalse(monitor.isLow());
        read(low, RobotConstants.BATTERY_LOW_SECONDS);
        assertTrue(monitor.isLow());
        assertTrue(monitor.hasBeenLow());
    }

    @Test
    public void lowClearsOnlyAboveTheClearVoltage() {
        double low = RobotConstants.BATTERY_LOW_VOLTS - 0.2;
        read(low, 0.0);
        read(low, RobotConstants.BATTERY_LOW_SECONDS);
        double between = (RobotConstants.BATTERY_LOW_VOLTS + RobotConstants.BATTERY_LOW_CLEAR_VOLTS) / 2;
        read(between, 1.0);
        assertTrue(monitor.isLow());
        read(RobotConstants.BATTERY_LOW_CLEAR_VOLTS + 0.1, 1.0);
        assertFalse(monitor.isLow());
        assertTrue(monitor.hasBeenLow());
    }

    @Test
    public void shortDipDoesNotWarn() {
        read(RobotConstants.BATTERY_LOW_VOLTS - 0.5, 0.0);
        read(12.5, 0.5);
        read(RobotConstants.BATTERY_LOW_VOLTS - 0.5, 0.5);
        read(RobotConstants.BATTERY_LOW_VOLTS - 0.5, RobotConstants.BATTERY_LOW_SECONDS / 2);
        assertFalse(monitor.isLow());
    }

    @Test
    public void sagLimitsDriveForTheHoldTimeAndCountsEachEvent() {
        read(12.0, 0.0);
        assertEquals(1.0, monitor.getDrivePowerLimit(), EPS);

        read(RobotConstants.BATTERY_SAG_VOLTS - 0.5, 1.0);
        read(RobotConstants.BATTERY_SAG_VOLTS - 0.5, 0.1); // same event
        assertEquals(1, monitor.getSagCount());
        assertEquals(RobotConstants.BATTERY_SAG_DRIVE_LIMIT, monitor.getDrivePowerLimit(), EPS);

        read(12.0, 0.1);
        clock.advance(RobotConstants.BATTERY_SAG_HOLD_SECONDS);
        assertEquals(1.0, monitor.getDrivePowerLimit(), EPS);

        read(RobotConstants.BATTERY_SAG_VOLTS - 0.5, 1.0);
        assertEquals(2, monitor.getSagCount());
    }
}
