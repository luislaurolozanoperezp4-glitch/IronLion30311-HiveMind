// Sources: IronLion30311BIOBUZZInicioDeTemporada / branch feature/turrubiates/vision-align / TeamCode/.../opmodes/auto/PreloadRouteAuto.java @ a7b8f4c (@Autonomous "Preload + Park (Odometry)", group "Competition")
//          — adapted for BIOBUZZ
package org.firstinspires.ftc.teamcode.opmodes.auto;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;

import org.firstinspires.ftc.teamcode.util.Alliance;

/**
 * BLUE alliance, FAR start (8.96, 131.19, 90 deg in the RED frame, rotated 180 deg for BLUE)
 * against our side wall (tile A6 for RED).
 * <b>UNTESTED</b> (decision D21: confirm the far start is legal, G304): drives to the other-cell
 * spot north of the HIVE and shoots there if the cell is seen, otherwise shoots from the near
 * spot; then the planner chooses a cycle or the park. Test it on a practice field first.
 * <p>
 * See {@link ScoringAuto} for the routine and {@link AutoSelector} for the INIT controls
 * (start delay on the D-pad, A = re-zero the odometry).
 */
@Autonomous(name = "Auto Blue Far", group = "Competition", preselectTeleOp = "TeleOp Blue")
public class AutoBlueFar extends ScoringAuto {

    @Override
    protected AutoSelector createSelector() {
        return new AutoSelector(Alliance.BLUE, StartPosition.FAR);
    }
}
