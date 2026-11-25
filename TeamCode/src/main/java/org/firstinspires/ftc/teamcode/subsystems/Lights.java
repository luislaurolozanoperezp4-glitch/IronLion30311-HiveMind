// Sources: IronLion30311BIOBUZZInicioDeTemporada / branch feature/turrubiates/vision-align / TeamCode/.../subsystems/IndicatorLight.java @ a7b8f4c (colors, blink, setColor only on change)
//          IronLion30311BIOBUZZInicioDeTemporada / branch master / TeamCode/.../subsystems/IndicatorLight.java @ 37b7a16 (setGreen)
//          IronLion30311BIOBUZZInicioDeTemporada / branch feature/turrubiates/vision-align / TeamCode/.../subsystems/NectarAlertLight.java @ a7b8f4c (NECTAR alert: hold, blink in the ball's color)
//          — adapted for BIOBUZZ (one Lights subsystem for both goBILDA lights; colors in RobotConstants)
package org.firstinspires.ftc.teamcode.subsystems;

import com.qualcomm.robotcore.hardware.Servo;

import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.teamcode.config.RobotConstants;
import org.firstinspires.ftc.teamcode.hardware.RobotHardware;
import org.firstinspires.ftc.teamcode.util.Alliance;
import org.firstinspires.ftc.teamcode.util.MatchTimer;

/**
 * The goBILDA RGB indicator lights. The light is driven like a servo: each position is a color
 * ({@link RobotConstants#LIGHT_RED}, ...).
 * <ul>
 *   <li><b>Indicator light</b>: solid colors ({@link #setRed()} ...) or blinking
 *   ({@link #blink(double)}).</li>
 *   <li><b>NECTAR alert light</b> (optional, decision D12): {@link #updateNectarAlert(BallColor)}
 *   blinks it in the NECTAR's color while the HuskyLens sees NECTAR, and keeps it on
 *   {@link RobotConstants#NECTAR_ALERT_HOLD_SECONDS} after the ball leaves the view.</li>
 * </ul>
 * Call {@link #periodic()} every loop so blinking works. A missing light is skipped.
 */
public class Lights implements Subsystem {

    private final Servo indicator;
    private final Servo nectarAlert;
    private final MatchTimer.Clock clock;
    private final double startSec;

    private double indicatorColor = RobotConstants.LIGHT_OFF;
    private boolean isBlinking = false;
    private double writtenIndicator = Double.NaN;
    private double writtenAlert = Double.NaN;

    private BallColor alertColor;
    private double alertLastSeenSec = Double.NaN;

    /** @param hardware robot hardware; either light may be missing */
    public Lights(RobotHardware hardware) {
        this(hardware.indicatorLight, hardware.nectarAlertLight, MatchTimer.systemClock());
    }

    /**
     * For tests.
     *
     * @param indicator   main light, or null
     * @param nectarAlert NECTAR alert light, or null
     * @param clock       time source
     */
    public Lights(Servo indicator, Servo nectarAlert, MatchTimer.Clock clock) {
        this.indicator = indicator;
        this.nectarAlert = nectarAlert;
        this.clock = clock;
        this.startSec = clock.nowSeconds();
    }

    /** Solid red (fast mode, or RED alliance). */
    public void setRed() {
        setSolid(RobotConstants.LIGHT_RED);
    }

    /** Solid blue (slow mode, or BLUE alliance). */
    public void setBlue() {
        setSolid(RobotConstants.LIGHT_BLUE);
    }

    /** Solid yellow (POLLEN launcher at target RPM). */
    public void setYellow() {
        setSolid(RobotConstants.LIGHT_YELLOW);
    }

    /** Solid white (aligning to the HIVE). */
    public void setWhite() {
        setSolid(RobotConstants.LIGHT_WHITE);
    }

    /** Solid green (aligned / ready). */
    public void setGreen() {
        setSolid(RobotConstants.LIGHT_GREEN);
    }

    /** Light off. */
    public void setOff() {
        setSolid(RobotConstants.LIGHT_OFF);
    }

    /**
     * Blinks a color on and off ({@link RobotConstants#LIGHT_BLINK_PERIOD_SECONDS} per cycle)
     * until another color is set.
     *
     * @param color light position of the color, for example {@link RobotConstants#LIGHT_RED}
     */
    public void blink(double color) {
        indicatorColor = color;
        isBlinking = true;
    }

    /** @param alliance shows red or blue for the alliance */
    public void showAlliance(Alliance alliance) {
        if (alliance == Alliance.RED) {
            setRed();
        } else {
            setBlue();
        }
    }

    /**
     * Launcher feedback for the driver: white while aligning, yellow when ready to shoot,
     * blinking yellow while spinning up.
     *
     * @param isAligning true while auto-aligning
     * @param isReady    true when the launcher is at its target RPM
     */
    public void showLauncherState(boolean isAligning, boolean isReady) {
        if (isAligning) {
            setWhite();
        } else if (isReady) {
            setYellow();
        } else {
            blink(RobotConstants.LIGHT_YELLOW);
        }
    }

    /**
     * Updates the NECTAR alert. Pass the color of the NECTAR the HuskyLens sees now (for
     * example {@code colorSensing.getClosestNectar().color}), or null if none.
     *
     * @param seenNectar NECTAR color in view, or null
     */
    public void updateNectarAlert(BallColor seenNectar) {
        double nowSec = clock.nowSeconds();
        if (seenNectar != null && seenNectar.isNectar()) {
            alertColor = seenNectar;
            alertLastSeenSec = nowSec;
        } else if (alertColor != null
                && nowSec - alertLastSeenSec > RobotConstants.NECTAR_ALERT_HOLD_SECONDS) {
            alertColor = null;
        }
    }

    /** @return the NECTAR color being signaled, or null */
    public BallColor getNectarAlertColor() {
        return alertColor;
    }

    /** Writes the light colors (and the blink). Call once per loop. */
    @Override
    public void periodic() {
        double elapsed = clock.nowSeconds() - startSec;
        double indicatorOut = indicatorColor;
        if (isBlinking && !isOnHalf(elapsed, RobotConstants.LIGHT_BLINK_PERIOD_SECONDS)) {
            indicatorOut = RobotConstants.LIGHT_OFF;
        }
        writeIndicator(indicatorOut);

        double alertOut = RobotConstants.LIGHT_OFF;
        if (alertColor != null && isOnHalf(elapsed, RobotConstants.NECTAR_ALERT_BLINK_SECONDS)) {
            alertOut = alertColor == BallColor.RED_NECTAR ? RobotConstants.LIGHT_RED
                    : RobotConstants.LIGHT_BLUE;
        }
        writeAlert(alertOut);
    }

    /** Turns both lights off. */
    @Override
    public void stop() {
        setOff();
        alertColor = null;
        writeIndicator(RobotConstants.LIGHT_OFF);
        writeAlert(RobotConstants.LIGHT_OFF);
    }

    /** @return the light position last written to the indicator light */
    public double getIndicatorPosition() {
        return writtenIndicator;
    }

    /** @return true if the NECTAR alert light is configured */
    public boolean hasNectarAlertLight() {
        return nectarAlert != null;
    }

    @Override
    public void addTelemetry(Telemetry t) {
        t.addData("Lights", "%.2f%s%s", indicatorColor, isBlinking ? " blink" : "",
                alertColor != null ? " | NECTAR " + alertColor : "");
    }

    private void setSolid(double color) {
        indicatorColor = color;
        isBlinking = false;
    }

    private static boolean isOnHalf(double elapsedSec, double periodSec) {
        return periodSec <= 0.0 || (elapsedSec % periodSec) < periodSec / 2.0;
    }

    private void writeIndicator(double position) {
        if (position == writtenIndicator) {
            return;
        }
        writtenIndicator = position;
        if (indicator != null) {
            indicator.setPosition(position);
        }
    }

    private void writeAlert(double position) {
        if (position == writtenAlert) {
            return;
        }
        writtenAlert = position;
        if (nectarAlert != null) {
            nectarAlert.setPosition(position);
        }
    }
}
