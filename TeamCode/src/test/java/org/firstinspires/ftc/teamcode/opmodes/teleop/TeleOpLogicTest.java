// Sources: new for IronLion30311-HiveMind (tests for opmodes.teleop.TeleOpLogic and EndgameAlerts)
package org.firstinspires.ftc.teamcode.opmodes.teleop;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.firstinspires.ftc.teamcode.config.LauncherConstants;
import org.firstinspires.ftc.teamcode.config.MatchConstants;
import org.firstinspires.ftc.teamcode.config.VisionConstants;
import org.firstinspires.ftc.teamcode.subsystems.ElementCounter;
import org.firstinspires.ftc.teamcode.util.PoseStorage;
import org.firstinspires.ftc.teamcode.subsystems.BallColor;
import org.firstinspires.ftc.teamcode.util.Alliance;
import org.junit.Test;

/** Checks the pure TeleOp decisions: lights, volley size, ball-assist colors, endgame alerts. */
public class TeleOpLogicTest {

    @Test
    public void ballAssistOnlyWhileDrivingForwardAndNotShooting() {
        double forward = VisionConstants.BALL_ASSIST_MIN_FORWARD;
        assertTrue(TeleOpLogic.shouldBallAssist(true, forward, 0.0, false));
        assertFalse("standing still at the shooting spot",
                TeleOpLogic.shouldBallAssist(true, 0.0, 0.0, false));
        assertFalse("driving backward", TeleOpLogic.shouldBallAssist(true, -0.5, 0.0, false));
        assertFalse("while firing", TeleOpLogic.shouldBallAssist(true, 0.8, 0.0, true));
        assertFalse("driver is turning", TeleOpLogic.shouldBallAssist(true, 0.8, 0.2, false));
        assertFalse("not collecting", TeleOpLogic.shouldBallAssist(false, 0.8, 0.0, false));
    }

    @Test
    public void driverMovingWhenAnyAxisIsNotZero() {
        assertFalse(TeleOpLogic.isDriverMoving(0.0, 0.0, 0.0));
        assertTrue(TeleOpLogic.isDriverMoving(0.1, 0.0, 0.0));
        assertTrue(TeleOpLogic.isDriverMoving(0.0, -0.1, 0.0));
        assertTrue(TeleOpLogic.isDriverMoving(0.0, 0.0, 0.2));
    }

    @Test
    public void lightWhileFiringFollowsTheLauncher() {
        assertEquals(TeleOpLogic.LightCue.ALIGNING,
                TeleOpLogic.pickLight(true, false, false, false, false));
        assertEquals(TeleOpLogic.LightCue.FEEDING,
                TeleOpLogic.pickLight(true, true, true, true, true));
        assertEquals(TeleOpLogic.LightCue.SPINNING_UP,
                TeleOpLogic.pickLight(true, true, false, false, false));
    }

    @Test
    public void lightWhenNotFiringShowsSpeedModeAndEndgame() {
        assertEquals(TeleOpLogic.LightCue.SLOW,
                TeleOpLogic.pickLight(false, true, false, false, true));
        assertEquals(TeleOpLogic.LightCue.FAST,
                TeleOpLogic.pickLight(false, true, false, false, false));
        assertEquals(TeleOpLogic.LightCue.ENDGAME_SLOW,
                TeleOpLogic.pickLight(false, true, false, true, true));
        assertEquals(TeleOpLogic.LightCue.ENDGAME_FAST,
                TeleOpLogic.pickLight(false, false, false, true, false));
    }

    @Test
    public void volleyUsesTheCountOnlyWhenTheCounterWorks() {
        int max = MatchConstants.MAX_CONTROLLED_ELEMENTS;
        assertEquals(2, TeleOpLogic.ballsForVolley(2, true));
        assertEquals(max, TeleOpLogic.ballsForVolley(0, true));
        assertEquals(max, TeleOpLogic.ballsForVolley(2, false));
        assertEquals(max, TeleOpLogic.ballsForVolley(max + 3, true));
    }

    @Test
    public void assistNeverTargetsOpponentNectar() {
        assertArrayEquals(new BallColor[]{BallColor.POLLEN, BallColor.RED_NECTAR},
                TeleOpLogic.assistColors(Alliance.RED));
        assertArrayEquals(new BallColor[]{BallColor.POLLEN, BallColor.BLUE_NECTAR},
                TeleOpLogic.assistColors(Alliance.BLUE));
    }

    @Test
    public void clockIsMinutesAndSeconds() {
        assertEquals("2:00", TeleOpLogic.formatClock(120.0));
        assertEquals("1:05", TeleOpLogic.formatClock(64.2));
        assertEquals("0:00", TeleOpLogic.formatClock(-3.0));
    }

    @Test
    public void endgameAlertsFireOnceEachInOrder() {
        EndgameAlerts alerts = new EndgameAlerts();
        double first = MatchConstants.FIRST_ENDGAME_WARNING_SECONDS;
        double second = MatchConstants.SECOND_ENDGAME_WARNING_SECONDS;
        assertEquals(EndgameAlerts.Alert.NONE, alerts.update(MatchConstants.TELEOP_SECONDS));
        assertEquals(EndgameAlerts.Alert.NONE, alerts.update(first + 0.1));
        assertEquals(EndgameAlerts.Alert.FIRST_WARNING, alerts.update(first));
        assertEquals(EndgameAlerts.Alert.NONE, alerts.update(first - 1.0));
        assertEquals(EndgameAlerts.Alert.ENDGAME_START, alerts.update(second));
        assertEquals(EndgameAlerts.Alert.NONE, alerts.update(second - 1.0));
        assertEquals(EndgameAlerts.Alert.NONE, alerts.update(0.0));
    }

    @Test
    public void lateStartSkipsThePassedWarning() {
        EndgameAlerts alerts = new EndgameAlerts();
        assertEquals(EndgameAlerts.Alert.ENDGAME_START,
                alerts.update(MatchConstants.SECOND_ENDGAME_WARNING_SECONDS - 2.0));
        assertEquals(EndgameAlerts.Alert.NONE, alerts.update(1.0));
        alerts.reset();
        assertEquals(EndgameAlerts.Alert.FIRST_WARNING,
                alerts.update(MatchConstants.FIRST_ENDGAME_WARNING_SECONDS - 0.5));
    }

    /** G407 hand-off: TeleOp starts from the AUTO count, or the preload when AUTO did not run. */
    @Test
    public void teleOpElementCountComesFromAuto() {
        PoseStorage.clear();
        assertEquals("No AUTO: the preload is still on board", LauncherConstants.PRELOAD_BALLS,
                TeleOpLogic.initialElementCount(PoseStorage.getElementCount(-1)));
        PoseStorage.saveElementCount(1);
        assertEquals(1, TeleOpLogic.initialElementCount(PoseStorage.getElementCount(-1)));
        PoseStorage.saveElementCount(0);
        assertEquals(0, TeleOpLogic.initialElementCount(PoseStorage.getElementCount(-1)));
        assertEquals("Never above the G407 limit", MatchConstants.MAX_CONTROLLED_ELEMENTS,
                TeleOpLogic.initialElementCount(9));
        PoseStorage.clear();
        assertEquals(-1, PoseStorage.getElementCount(-1));
    }

    /** G407 end to end: with the AUTO count handed over, the 5th pickup is refused. */
    @Test
    public void fifthElementAfterAutoIsRefused() {
        ElementCounter counter = new ElementCounter(null, null);
        counter.setCount(TeleOpLogic.initialElementCount(3));
        assertTrue(counter.recordCollected());
        assertFalse("5th element refused", counter.recordCollected());
        assertEquals(MatchConstants.MAX_CONTROLLED_ELEMENTS, counter.getCount());
    }
}
