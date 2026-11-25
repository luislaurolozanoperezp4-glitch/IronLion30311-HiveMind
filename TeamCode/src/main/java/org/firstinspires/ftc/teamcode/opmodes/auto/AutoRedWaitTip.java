// Sources: IronLion30311BIOBUZZInicioDeTemporada / branch feature/turrubiates/vision-align / TeamCode/.../opmodes/auto/FarSideWaitTipAuto.java @ f26f8d0 (@Autonomous "Far Side: Wait TIP + Park (Odometry)", group "Competition")
//          — adapted for BIOBUZZ (one OpMode per alliance, back-wall start)
package org.firstinspires.ftc.teamcode.opmodes.auto;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;

import org.firstinspires.ftc.teamcode.util.Alliance;

/**
 * RED alliance, BACK WALL start ({@link StartPosition#BACK_WALL}, 61.5, 135, 90 deg in the
 * RED frame), front against the back wall right of the FLOWER. <b>UNTESTED</b>: waits for the
 * partner's TIP, aims and shoots the preload, parks in the upper LOADING ZONE.
 * <p>
 * See {@link WaitTipAuto} for the routine and {@link AutoSelector} for the INIT controls
 * (start delay on the D-pad, A = re-zero the odometry).
 */
@Autonomous(name = "Auto Red Wait Tip", group = "Competition", preselectTeleOp = "TeleOp Red")
public class AutoRedWaitTip extends WaitTipAuto {

    @Override
    protected AutoSelector createSelector() {
        return new AutoSelector(Alliance.RED, StartPosition.BACK_WALL);
    }
}
