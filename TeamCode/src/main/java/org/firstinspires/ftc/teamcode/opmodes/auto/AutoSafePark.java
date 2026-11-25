// Sources: IronLion30311BIOBUZZInicioDeTemporada / branch feature/turrubiates/vision-align / TeamCode/.../opmodes/auto/PreloadRouteAuto.java @ a7b8f4c (LEAVE + park legs, without shooting)
//          IronLion30311BIOBUZZInicioDeTemporada / branch feature/autonomos-beta / TeamCode/.../opmodes/auto/ParkAuto.java @ 206fa84 (far start down the wall into the LOADING ZONE)
//          IronLion30311BIOBUZZInicioDeTemporada / branch feature/turrubiates/auto-leave-park / TeamCode/.../opmodes/auto/LeaveParkAuto.java @ e455142 (10 cm LEAVE from inside the LOADING ZONE)
//          — adapted for BIOBUZZ
package org.firstinspires.ftc.teamcode.opmodes.auto;

import com.pedropathing.math.Pose;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;

import org.firstinspires.ftc.teamcode.config.AutoConstants;
import org.firstinspires.ftc.teamcode.pathing.paths.NearRoutePoses;
import org.firstinspires.ftc.teamcode.pathing.paths.PreloadParkPaths;
import org.firstinspires.ftc.teamcode.pathing.paths.SafeParkPaths;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The safest auto: LEAVE (3 points) and PARK (5 points) only, no shooting. Uses the odometry
 * driver (Pinpoint); if the Pinpoint is broken use {@link AutoSafeParkEncoders}.
 * <p>
 * INIT (gamepad1): B = RED, X = BLUE, D-pad left / right = start position, D-pad up / down =
 * start delay.
 * <ul>
 *     <li>NEAR: LEAVE 10 cm, then the TESTED park legs through the west lane.</li>
 *     <li>FAR: backwards down the side wall into the LOADING ZONE (beta {@code ParkAuto},
 *     ends ~0.6 in from the wall: LEAVE may not count, see the hand-off).</li>
 *     <li>LOADING ZONE: start inside the zone, front against the side wall; back off 10 cm
 *     (LEAVE) and stay (PARK).</li>
 * </ul>
 * No {@code preselectTeleOp}: the alliance is chosen in INIT, so pick the TeleOp by hand.
 */
@Autonomous(name = "Auto Safe Park", group = "Competition")
public class AutoSafePark extends AutoBase {

    /** Unit conversion (not a tuning value). */
    private static final double CM_PER_INCH = 2.54;

    @Override
    protected AutoSelector createSelector() {
        return new AutoSelector(null, StartPosition.NEAR, StartPosition.FAR, StartPosition.LOADING_ZONE);
    }

    @Override
    protected String describeRoute() {
        switch (getStartPosition()) {
            case FAR:
                return "SAFE PARK from FAR: down the side wall into the LOADING ZONE";
            case LOADING_ZONE:
                return "SAFE PARK from the LOADING ZONE: back off 10 cm and stay";
            default:
                return "SAFE PARK from NEAR: LEAVE 10 cm + TESTED park legs (no shooting)";
        }
    }

    @Override
    protected void buildRoutine(StepSequence sequence) {
        switch (getStartPosition()) {
            case FAR:
                sequence.addAll(parkSteps("Down the wall", Arrays.asList(field(SafeParkPaths.PARK)), false));
                break;
            case LOADING_ZONE:
                sequence.addAll(parkSteps("LEAVE", Arrays.asList(field(loadingZoneLeavePose())), false));
                break;
            default:
                List<AutoStep> steps = new ArrayList<>();
                steps.add(driveTo("LEAVE back " + AutoConstants.LEAVE_BACK_CM + " cm",
                        field(NearRoutePoses.SHOOT_SPOT), Mode.IDLE));
                steps.add(settle(AutoConstants.SETTLE_SECONDS, Mode.IDLE));
                steps.addAll(parkSteps("Park", new PreloadParkPaths(getAlliance()).getParkWaypoints(), true));
                sequence.addAll(steps);
                break;
        }
    }

    /** @return the RED loading-zone start moved {@code LOADING_ZONE_LEAVE_CM} backwards */
    static Pose loadingZoneLeavePose() {
        Pose start = StartPosition.LOADING_ZONE.getRedPose();
        double backInches = AutoConstants.LOADING_ZONE_LEAVE_CM / CM_PER_INCH;
        return new Pose(start.x() - Math.cos(start.heading()) * backInches,
                start.y() - Math.sin(start.heading()) * backInches, start.heading());
    }
}
