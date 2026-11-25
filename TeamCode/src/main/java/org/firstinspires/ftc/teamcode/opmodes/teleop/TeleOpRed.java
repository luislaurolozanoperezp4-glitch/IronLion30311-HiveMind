// Sources: IronLion30311BIOBUZZInicioDeTemporada / branch feature/turrubiates/vision-align / TeamCode/.../opmodes/teleop/MainTeleOp.java @ a7b8f4c (INIT alliance choice, now one OpMode per alliance) — adapted for BIOBUZZ
package org.firstinspires.ftc.teamcode.opmodes.teleop;

import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

import org.firstinspires.ftc.teamcode.util.Alliance;

/**
 * Competition TeleOp for the RED alliance. All the logic is in {@link TeleOpBase}.
 * <p>
 * The name must stay "TeleOp Red": the autonomous OpModes preselect it by name.
 */
@TeleOp(name = "TeleOp Red", group = "Competition")
public class TeleOpRed extends TeleOpBase {

    @Override
    protected Alliance getAlliance() {
        return Alliance.RED;
    }

    @Override
    protected String getLogTag() {
        return "TeleOpRed";
    }
}
