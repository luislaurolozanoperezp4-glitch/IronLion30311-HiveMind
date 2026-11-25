// Sources: new for IronLion30311-HiveMind (review finding #2: Wait Tip aim vs the G402 guard)
package org.firstinspires.ftc.teamcode.opmodes.auto;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.pedropathing.math.Pose;

import org.firstinspires.ftc.teamcode.config.AutoConstants;
import org.firstinspires.ftc.teamcode.config.DriveConstants;
import org.firstinspires.ftc.teamcode.util.Alliance;
import org.junit.Test;

public class TerritoryGuardTest {

    /** Wait Tip start column: 1.5 in inside the guard limit (RED frame, facing the back wall). */
    private static final Pose WAIT_TIP_RED = new Pose(61.5, 120.0, Math.toRadians(90.0));

    private static final double REAR_MOVE_INCHES = AutoConstants.AIM_MAX_TOWARD_CM / 2.54;

    @Test
    public void shrinksAimTurnSoOvershootNeverTripsTheGuard() {
        for (Alliance alliance : Alliance.values()) {
            Pose start = alliance.toField(WAIT_TIP_RED);
            for (double wanted : new double[] {-AutoConstants.WAIT_TIP_AIM_MAX_TURN_DEGREES,
                    AutoConstants.WAIT_TIP_AIM_MAX_TURN_DEGREES}) {
                double turn = TerritoryGuard.limitTurnDegrees(start, wanted, alliance);
                // Worst case: the turn overshoots by the full turn tolerance
                double worst = turn + Math.signum(turn) * DriveConstants.TURN_TOLERANCE_DEGREES;
                Pose turned = new Pose(start.x(), start.y(), start.heading() + Math.toRadians(worst));
                assertFalse(alliance + " turn " + wanted + " -> " + turn + " trips the guard",
                        TerritoryGuard.isPastLine(turned, alliance));
            }
        }
    }

    @Test
    public void shortensAimMoveThatWouldCrossTheLine() {
        for (Alliance alliance : Alliance.values()) {
            // Turned 9 degrees toward the line, then the longest rear move
            Pose redTurned = new Pose(WAIT_TIP_RED.x(), WAIT_TIP_RED.y(), Math.toRadians(90.0 - 9.0));
            Pose start = alliance.toField(redTurned);
            Pose wanted = AutoBase.movedTowardRear(start, AutoConstants.AIM_MAX_TOWARD_CM);
            Pose limited = TerritoryGuard.limitMoveTarget(start, wanted, alliance);
            assertFalse(alliance + " aim move trips the guard", TerritoryGuard.isPastLine(limited, alliance));
            assertTrue(alliance + " limited target must stay safe", TerritoryGuard.isSafeWithMargin(limited, alliance)
                    || samePosition(limited, start));
        }
    }

    @Test
    public void keepsSafeMovesUnchanged() {
        Pose start = new Pose(40.0, 60.0, Math.toRadians(-90.0));
        Pose target = AutoBase.movedTowardRear(start, REAR_MOVE_INCHES * 2.54);
        assertTrue(samePosition(target, TerritoryGuard.limitMoveTarget(start, target, Alliance.RED)));
    }

    private static boolean samePosition(Pose a, Pose b) {
        return Math.abs(a.x() - b.x()) < 1e-6 && Math.abs(a.y() - b.y()) < 1e-6;
    }
}
