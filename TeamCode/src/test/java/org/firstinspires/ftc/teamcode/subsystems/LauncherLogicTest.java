// Sources: new for IronLion30311-HiveMind (tests the vision-align Shooter readiness, voltage compensation and ShotTracker logic @ a7b8f4c; feed window @ 19de992; pollen idle @ 854de85)
package org.firstinspires.ftc.teamcode.subsystems;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.firstinspires.ftc.teamcode.config.LauncherConstants;
import org.junit.Test;

public class LauncherLogicTest {

    private static final double TARGET = 3000.0;

    @Test
    public void readyHasHysteresis() {
        assertFalse(Launcher.updateReady(false, true, TARGET - 150, TARGET));
        assertTrue(Launcher.updateReady(false, true, TARGET - 50, TARGET));
        assertTrue(Launcher.updateReady(true, true, TARGET - 150, TARGET));
        assertFalse(Launcher.updateReady(true, true, TARGET - 250, TARGET));
        assertFalse(Launcher.updateReady(true, false, TARGET, TARGET));
    }

    @Test
    public void feedWindowIsAsymmetric() {
        // Becomes ready only inside [target - READY_BELOW_RPM, target + READY_ABOVE_RPM]
        double below = LauncherConstants.READY_BELOW_RPM;
        double above = LauncherConstants.READY_ABOVE_RPM;
        assertFalse(Launcher.updateReady(false, true, TARGET - below - 10, TARGET));
        assertTrue(Launcher.updateReady(false, true, TARGET - below, TARGET));
        assertTrue(Launcher.updateReady(false, true, TARGET + above, TARGET));
        assertFalse(Launcher.updateReady(false, true, TARGET + above + 10, TARGET));
        // Once ready, only a dip beyond NOT_READY_TOLERANCE_RPM ends it (either side)
        assertTrue(Launcher.updateReady(true, true, TARGET + above + 10, TARGET));
        assertFalse(Launcher.updateReady(true, true,
                TARGET + LauncherConstants.NOT_READY_TOLERANCE_RPM + 1, TARGET));
    }

    @Test
    public void pollenIdleIsNeverReadyAndStopTurnsItOff() {
        Launcher launcher = new Launcher(null, null, () -> 12.0,
                new org.firstinspires.ftc.teamcode.testutil.FakeClock());
        launcher.setPollenIdle(true);
        launcher.periodic();
        assertTrue(launcher.isPollenIdleEnabled());
        assertEquals(Launcher.FlywheelState.IDLE, launcher.getPollenState());
        assertFalse(launcher.isPollenReady());
        assertFalse(launcher.isFiring());
        launcher.stop();
        assertFalse(launcher.isPollenIdleEnabled());
    }

    @Test
    public void feedforwardScalesWithBattery() {
        assertEquals(LauncherConstants.KF_AT_12V, Launcher.compensatedKf(12.0), 1e-9);
        assertEquals(LauncherConstants.KF_AT_12V * 12.0 / 10.0, Launcher.compensatedKf(10.0), 1e-9);
        // A glitch reading is treated as 12 V
        assertEquals(LauncherConstants.KF_AT_12V, Launcher.compensatedKf(0.0), 1e-9);
    }

    @Test
    public void rpmConversionRoundTrips() {
        double ticks = Launcher.rpmToTicksPerSecond(TARGET);
        assertEquals(TARGET * LauncherConstants.TICKS_PER_REV / 60.0, ticks, 1e-9);
        assertEquals(TARGET, Launcher.ticksPerSecondToRpm(ticks), 1e-9);
    }

    @Test
    public void shotDetectorIgnoresSpinUpAndCountsDips() {
        ShotDetector detector = new ShotDetector();
        detector.update(true, TARGET, 1000, 0.0);   // spinning up: not a shot
        assertEquals(0, detector.getShotCount());
        detector.update(true, TARGET, TARGET, 0.5); // reached target
        detector.update(true, TARGET, TARGET - 400, 0.6); // dip = shot
        assertEquals(1, detector.getShotCount());
        detector.update(true, TARGET, TARGET - 500, 0.65);
        detector.update(true, TARGET, TARGET - 20, 0.8); // recovered
        assertEquals(500.0, detector.getLastDropRpm(), 1e-9);
        detector.update(true, TARGET, TARGET - 400, 1.0);
        assertEquals(2, detector.getShotCount());
    }

    @Test
    public void launcherWithoutMotorsIsSafe() {
        Launcher launcher = new Launcher(null, null, () -> 12.0, new org.firstinspires.ftc.teamcode.testutil.FakeClock());
        launcher.spinUpPollen();
        launcher.periodic();
        assertFalse(launcher.isAtTargetRpm());
        launcher.adjustPollenRpm(LauncherConstants.MAX_RPM * 2);
        assertEquals(LauncherConstants.MAX_RPM, launcher.getPollenTargetRpm(), 1e-9);
        launcher.stop();
        assertFalse(launcher.isFiring());
    }
}
