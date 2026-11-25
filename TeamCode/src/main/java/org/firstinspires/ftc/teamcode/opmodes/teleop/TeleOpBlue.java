// Sources: IronLion30311BIOBUZZInicioDeTemporada / branch feature/turrubiates/vision-align / TeamCode/.../opmodes/teleop/MainTeleOp.java @ a7b8f4c (INIT alliance choice, now one OpMode per alliance) — adapted for BIOBUZZ
package org.firstinspires.ftc.teamcode.opmodes.teleop;

import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

import org.firstinspires.ftc.teamcode.util.Alliance;

/**
 * Competition TeleOp for the BLUE alliance. All the logic is in {@link TeleOpBase}.
 * <p>
 * The name must stay "TeleOp Blue": the autonomous OpModes preselect it by name.
 */
@TeleOp(name = "TeleOp Blue", group = "Competition")
public class TeleOpBlue extends TeleOpBase {

    @Override
    protected Alliance getAlliance() {
        return Alliance.BLUE;
    }

    @Override
    protected String getLogTag() {
        return "TeleOpBlue";
    }
}
