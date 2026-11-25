// Sources: new for IronLion30311-HiveMind (tests mecanum mixing from vision-align MecanumDrive @ a7b8f4c and the new field-centric rotation)
package org.firstinspires.ftc.teamcode.subsystems;

import static org.junit.Assert.assertEquals;

import com.qualcomm.robotcore.hardware.DcMotorEx;

import org.firstinspires.ftc.teamcode.config.DriveConstants;
import org.firstinspires.ftc.teamcode.testutil.FakeClock;
import org.junit.Test;

public class DrivetrainTest {

    private static final double EPS = 1e-9;

    @Test
    public void mixIsNormalized() {
        double[] p = Drivetrain.mix(1.0, 1.0, 0.0, 1.0);
        assertEquals(1.0, p[Drivetrain.FRONT_LEFT], EPS);
        assertEquals(0.0, p[Drivetrain.FRONT_RIGHT], EPS);
        assertEquals(0.0, p[Drivetrain.BACK_LEFT], EPS);
        assertEquals(1.0, p[Drivetrain.BACK_RIGHT], EPS);
    }

    @Test
    public void fieldCentricTurnedLeftDrivesRight() {
        // Robot turned 90 degrees counter-clockwise; "away from the driver" is its right side.
        double[] r = Drivetrain.fieldToRobot(1.0, 0.0, Math.PI / 2.0);
        assertEquals(0.0, r[0], EPS);
        assertEquals(1.0, r[1], EPS);
    }

    @Test
    public void rampsAndStopsWithoutMotors() {
        FakeClock clock = new FakeClock();
        Drivetrain drive = new Drivetrain(new DcMotorEx[4], clock);
        drive.drive(1.0, 0.0, 0.0);
        drive.periodic();
        clock.advance(0.02);
        drive.drive(1.0, 0.0, 0.0);
        drive.periodic();
        double expected = 0.02 / DriveConstants.ACCEL_RAMP_SECONDS;
        assertEquals(expected, drive.getWheelPowers()[0], EPS);

        drive.setBrake(true);
        assertEquals(0.0, drive.getWheelPowers()[0], EPS);
    }
}
