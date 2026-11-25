// Sources: new for IronLion30311-HiveMind (G410 gate on the fixed feature/renata/flower-arm FlowerArm)
package org.firstinspires.ftc.teamcode.subsystems;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.firstinspires.ftc.teamcode.testutil.FakeClock;
import org.firstinspires.ftc.teamcode.util.MatchTimer;
import org.junit.Test;

public class FlowerArmTest {

    @Test
    public void missingServosAreSafe() {
        FlowerArm arm = new FlowerArm(null, null);
        assertFalse(arm.isAvailable());
        arm.toggle();
        arm.periodic();
    }

    @Test
    public void deployIsRefusedBeforeTheLastMinute() {
        FakeClock clock = new FakeClock();
        MatchTimer timer = new MatchTimer(clock, false);
        timer.start();
        FlowerArm arm = new FlowerArm(null, null);
        arm.setDeployGate(timer::canPlaceNectar);

        assertFalse(arm.deploy());
        assertFalse(arm.deployIfAllowed(timer));
        assertEquals(2, arm.getRefusedCount());

        clock.advance(61.0); // 59 s left
        assertTrue(arm.deploy());
        assertTrue(arm.isDeployed());
        arm.retract();
        assertFalse(arm.isDeployed());
    }
}
