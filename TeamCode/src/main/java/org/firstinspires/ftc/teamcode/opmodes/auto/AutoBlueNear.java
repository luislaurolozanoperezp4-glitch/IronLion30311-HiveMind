// Sources: IronLion30311BIOBUZZInicioDeTemporada / branch feature/turrubiates/vision-align / TeamCode/.../opmodes/auto/PreloadRouteAuto.java @ a7b8f4c (@Autonomous "Preload + Park (Odometry)", group "Competition")
//          — adapted for BIOBUZZ
package org.firstinspires.ftc.teamcode.opmodes.auto;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;

import org.firstinspires.ftc.teamcode.util.Alliance;

/**
 * BLUE alliance, NEAR start (57, 9, -90 deg in the RED frame, rotated 180 deg for BLUE), front against the audience wall.
 * TESTED route (4/4 preload shots and parked, 2026-11-23): LEAVE, shoot 4, then the planner
 * chooses a collect cycle (other cell after a TIP) or the TESTED park.
 * <p>
 * See {@link ScoringAuto} for the routine and {@link AutoSelector} for the INIT controls
 * (start delay on the D-pad, A = re-zero the odometry).
 */
@Autonomous(name = "Auto Blue Near", group = "Competition", preselectTeleOp = "TeleOp Blue")
public class AutoBlueNear extends ScoringAuto {

    @Override
    protected AutoSelector createSelector() {
        return new AutoSelector(Alliance.BLUE, StartPosition.NEAR);
    }
}
