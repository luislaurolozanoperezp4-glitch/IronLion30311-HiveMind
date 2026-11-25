// Sources: new for IronLion30311-HiveMind (tests the jam protection ported from vision-align Intake @ b6913d9 and its interaction with the G408 stop)
package org.firstinspires.ftc.teamcode.subsystems;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.firstinspires.ftc.teamcode.config.IntakeConstants;
import org.firstinspires.ftc.teamcode.testutil.FakeClock;
import org.junit.Test;

import java.util.function.BooleanSupplier;
import java.util.function.DoubleSupplier;

/** Jam protection of {@link Intake}: current threshold, reverse, resume, and the G408 priority. */
public class IntakeTest {

    private static final double LOOP_SECONDS = 0.02;
    private static final double STALL_AMPS = 8.2;
    private static final double NORMAL_AMPS = 2.0;

    private final FakeClock clock = new FakeClock();
    private double amps = NORMAL_AMPS;
    private boolean isOpponentNectar = false;

    private Intake newIntake() {
        Intake intake = new Intake(null, clock, new DoubleSupplier() {
            @Override
            public double getAsDouble() {
                return amps;
            }
        });
        intake.setOpponentNectarDetector(new BooleanSupplier() {
            @Override
            public boolean getAsBoolean() {
                return isOpponentNectar;
            }
        });
        return intake;
    }

    private void run(Intake intake, double seconds) {
        double end = clock.now + seconds;
        while (clock.now < end) {
            intake.periodic();
            clock.advance(LOOP_SECONDS);
        }
    }

    @Test
    public void highCurrentWhileCollectingReversesThenCollectsAgain() {
        Intake intake = newIntake();
        intake.collect();
        run(intake, 0.5); // ramp up to full collect power
        assertEquals(Intake.State.COLLECTING, intake.getState());

        amps = STALL_AMPS;
        run(intake, IntakeConstants.JAM_SECONDS + 2 * LOOP_SECONDS);
        assertEquals(Intake.State.UNJAMMING, intake.getState());
        assertEquals(1, intake.getUnjamCount());

        amps = NORMAL_AMPS;
        run(intake, IntakeConstants.JAM_REVERSE_SECONDS);
        assertTrue("the motor reversed during the unjam", intake.getPower() < 0.0);
        run(intake, 2 * LOOP_SECONDS);
        assertEquals(Intake.State.COLLECTING, intake.getState());
    }

    @Test
    public void shortCurrentPeakIsNotAJam() {
        Intake intake = newIntake();
        intake.collect();
        run(intake, 0.5);
        amps = STALL_AMPS;
        run(intake, IntakeConstants.JAM_SECONDS / 2.0);
        amps = NORMAL_AMPS;
        run(intake, 1.0);
        assertEquals(0, intake.getUnjamCount());
        assertEquals(Intake.State.COLLECTING, intake.getState());
    }

    @Test
    public void startUpSpikeBeforeFullPowerIsIgnored() {
        Intake intake = newIntake();
        amps = STALL_AMPS;
        intake.collect();
        intake.periodic(); // first loop: power still ramping
        clock.advance(LOOP_SECONDS);
        intake.periodic();
        assertFalse(intake.isUnjamming());
        assertEquals(0, intake.getUnjamCount());
    }

    @Test
    public void opponentNectarStopWinsOverAnUnjam() {
        Intake intake = newIntake();
        intake.collect();
        run(intake, 0.5);
        amps = STALL_AMPS;
        run(intake, IntakeConstants.JAM_SECONDS + 2 * LOOP_SECONDS);
        assertTrue(intake.isUnjamming());

        isOpponentNectar = true;
        intake.periodic();
        assertEquals("G408: never push the opponent's NECTAR out",
                Intake.State.OPPONENT_NECTAR, intake.getState());
    }

    @Test
    public void noCurrentReadingMeansNoJamProtection() {
        Intake intake = new Intake(null, clock);
        intake.collect();
        run(intake, 2.0);
        assertEquals(0, intake.getUnjamCount());
        assertEquals(Intake.State.COLLECTING, intake.getState());
    }
}
