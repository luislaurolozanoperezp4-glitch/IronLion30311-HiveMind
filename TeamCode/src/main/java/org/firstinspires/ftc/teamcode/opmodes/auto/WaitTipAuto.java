// Sources: IronLion30311BIOBUZZInicioDeTemporada / branch feature/turrubiates/vision-align / TeamCode/.../opmodes/auto/FarSideWaitTipAuto.java @ f26f8d0 (runRoutine: LEAVE, pre-spin, waitForStableCell, aimAtCell, shoot, park in the upper load zone; give-up time from timeBeforePark)
//          — adapted for BIOBUZZ (non-blocking steps, back-wall start checked against G304, park 2 in off the wall, aim turn limited for G402)
package org.firstinspires.ftc.teamcode.opmodes.auto;

import com.pedropathing.math.Pose;

import org.firstinspires.ftc.teamcode.config.AutoConstants;
import org.firstinspires.ftc.teamcode.config.LauncherConstants;
import org.firstinspires.ftc.teamcode.pathing.AutoPlanner;
import org.firstinspires.ftc.teamcode.pathing.paths.WaitTipPaths;
import org.firstinspires.ftc.teamcode.subsystems.vision.HiveCell;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.DoubleSupplier;

/**
 * The wait-TIP routine (vision-align {@code FarSideWaitTipAuto}, <b>UNTESTED</b>): for a robot
 * that starts against the BACK wall while the alliance partner scores the first TIP.
 * <ol>
 *     <li>LEAVE: back 10 cm off the back wall ({@link WaitTipPaths}).</li>
 *     <li>Pre-spin the POLLEN flywheel at {@code WAIT_TIP_PRESPIN_RPM} and wait until our
 *     AUDIENCE cell (34-37 for RED) is in view and still (the partner's TIP made it face up).
 *     The wait gives up early enough that the aim, a useful volley and the park still fit.</li>
 *     <li>Spin up to the AUTO RPM, aim with the Limelight (bearing and distance of a volley that
 *     scores, turn limited to {@code WAIT_TIP_AIM_MAX_TURN_DEGREES} so the corners stay on our
 *     side, G402), shoot the 4 preloaded POLLEN until only the park time is left.</li>
 *     <li>PARK in the UPPER part of the LOADING ZONE in one diagonal without turning, 2 in off
 *     the red wall (the partner parks at the zone's low edge).</li>
 * </ol>
 * Rules: the start is checked against G304 by {@code StartPositionRulesTest} (touching the back
 * wall, own half, outside the LOADING ZONE, clear of the FLOWER). It is NOT the team's FAR start
 * (D21: the wall farthest from our LOADING ZONE); see docs/SOURCE_SYNC.md for the open question.
 */
public abstract class WaitTipAuto extends AutoBase {

    @Override
    protected String describeRoute() {
        return "WAIT TIP (UNTESTED): LEAVE -> wait for our audience cell -> aim + shoot 4 -> park (upper zone)";
    }

    @Override
    protected void buildRoutine(StepSequence sequence) {
        final HiveCell cell = HiveCell.getAudienceCell(getAlliance());
        setSpinUpRpm(AutoConstants.WAIT_TIP_PRESPIN_RPM);
        sequence.add(
                driveTo("LEAVE back " + AutoConstants.LEAVE_BACK_CM + " cm", field(WaitTipPaths.LEAVE), Mode.IDLE),
                settle(AutoConstants.SETTLE_SECONDS, Mode.IDLE),
                waitForStableCell("Wait for TIP", cell, new DoubleSupplier() {
                    @Override
                    public double getAsDouble() {
                        // Latest moment to stop waiting: aim + a useful volley + the park must still fit
                        return AutoPlanner.getParkDeadlineSeconds() - AutoConstants.SETTLE_SECONDS
                                - AutoPlanner.estimateRouteSeconds(AutoConstants.PARK_MAX_POWER, parkRoute())
                                - AutoConstants.WAIT_TIP_AIM_SECONDS - AutoConstants.WAIT_TIP_MIN_SHOOT_SECONDS;
                    }
                }, Mode.SPIN_UP),
                AutoStep.instant("Decide after the wait", new Runnable() {
                    @Override
                    public void run() {
                        if (wasCellSeen()) {
                            setSpinUpRpm(LauncherConstants.POLLEN_AUTO_RPM);
                            List<AutoStep> steps = aimSteps(cell, AutoConstants.DEFAULT_SHOOT_BEARING_DEGREES,
                                    AutoConstants.DEFAULT_SHOOT_DISTANCE_METERS,
                                    AutoConstants.WAIT_TIP_AIM_MAX_TURN_DEGREES);
                            steps.add(shoot("Preload volley", LauncherConstants.PRELOAD_BALLS,
                                    LauncherConstants.POLLEN_AUTO_RPM, volleyTimeLeft()));
                            steps.add(AutoStep.instant("Park", new Runnable() {
                                @Override
                                public void run() {
                                    getSequence().addNext(parkSteps());
                                }
                            }));
                            getSequence().addNext(steps);
                        } else {
                            getSequence().addNext(parkSteps());
                        }
                    }
                }));
    }

    /** @return the park legs from here, at the power that parks by the deadline */
    private List<AutoStep> parkSteps() {
        return timedParkSteps("Park (upper zone)", Collections.singletonList(field(WaitTipPaths.PARK)), false);
    }

    /** @return where the robot is, then PARK (for the time model) */
    private List<Pose> parkRoute() {
        return Arrays.asList(getPose(), field(WaitTipPaths.PARK));
    }

    /** @return the volley time that still leaves the park (0 = skip a volley too short to matter) */
    private DoubleSupplier volleyTimeLeft() {
        return new DoubleSupplier() {
            @Override
            public double getAsDouble() {
                double seconds = getPlanner().getVolleyTimeLeft(parkRoute());
                return seconds >= AutoConstants.WAIT_TIP_MIN_VOLLEY_SECONDS ? seconds : 0.0;
            }
        };
    }
}
