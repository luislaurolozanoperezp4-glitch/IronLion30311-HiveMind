// Sources: new for IronLion30311-HiveMind (tests pathing.PinpointDiagnostics, ported from vision-align @ 023ae35)
package org.firstinspires.ftc.teamcode.pathing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.qualcomm.hardware.gobilda.GoBildaPinpointDriver.DeviceStatus;

import org.junit.Test;

/** Status counting of {@link PinpointDiagnostics}, fed by hand (no device). */
public class PinpointDiagnosticsTest {

    @Test
    public void readyAndCalibratingAreNotFaults() {
        assertFalse(PinpointDiagnostics.isFault(DeviceStatus.READY));
        assertFalse(PinpointDiagnostics.isFault(DeviceStatus.CALIBRATING));
        assertTrue(PinpointDiagnostics.isFault(DeviceStatus.NOT_READY));
        assertTrue(PinpointDiagnostics.isFault(DeviceStatus.FAULT_BAD_READ));
    }

    @Test
    public void countsFaultReadsAndStatusChanges() {
        PinpointDiagnostics diagnostics = new PinpointDiagnostics(null);
        assertFalse(diagnostics.hasFault());
        diagnostics.record(DeviceStatus.READY);
        diagnostics.record(DeviceStatus.READY);
        diagnostics.record(DeviceStatus.FAULT_BAD_READ);
        assertTrue(diagnostics.hasFault());
        diagnostics.record(DeviceStatus.READY);
        assertFalse(diagnostics.hasFault());
        assertEquals(1, diagnostics.getFaultReads());
        assertEquals(2, diagnostics.getStatusChanges());
        assertEquals(DeviceStatus.FAULT_BAD_READ, diagnostics.getWorstStatus());
    }

    @Test
    public void missingPinpointIsNotConfiguredAndLogsEmptyColumns() {
        PinpointDiagnostics diagnostics = new PinpointDiagnostics(null);
        diagnostics.update();
        assertFalse(diagnostics.isConfigured());
        assertFalse(diagnostics.hasFault());
        assertNull(diagnostics.getStatus());
        assertEquals(",,,", diagnostics.csvFields());
        assertEquals("NOT CONFIGURED", diagnostics.summary());
    }
}
