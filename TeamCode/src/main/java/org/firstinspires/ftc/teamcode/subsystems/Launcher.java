// Sources: IronLion30311BIOBUZZInicioDeTemporada / branch feature/turrubiates/vision-align / TeamCode/.../subsystems/Shooter.java @ a7b8f4c (fire/reverse/stop per flywheel, updateReady hysteresis, compensateForVoltage, applyPidf, RPM conversion, clampRpm, setTargetRpmP) @ 854de85 (pollen idle spin: setPollenIdle, isPollenIdleEnabled, off in stop) @ 19de992 (asymmetric feed window in updateReady)
//          IronLion30311BIOBUZZInicioDeTemporada / branch master / TeamCode/.../subsystems/Shooter.java @ 37b7a16 (setTargetRpm(p, n), atTargetRPM -> isAtTargetRpm)
//          IronLion30311BIOBUZZInicioDeTemporada / branch feature/renata/auto-align / TeamCode/.../subsystems/Shooter.java @ 3e733d4 (idle pre-spin idea, off by default)
//          — adapted for BIOBUZZ (renamed Shooter -> Launcher; gamepad logic moved out; shot detection from ShooterLogger)
package org.firstinspires.ftc.teamcode.subsystems;

import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.PIDFCoefficients;

import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.teamcode.config.LauncherConstants;
import org.firstinspires.ftc.teamcode.config.RobotConstants;
import org.firstinspires.ftc.teamcode.hardware.RobotHardware;
import org.firstinspires.ftc.teamcode.util.MatchTimer;

import java.util.function.DoubleSupplier;

/**
 * The two flywheel launchers at the rear: one for POLLEN, one for NECTAR.
 * <p>
 * Each flywheel runs closed-loop velocity control in RPM with the SDK's built-in PIDF
 * ({@link DcMotorEx#setVelocity(double)}). The feedforward F is rescaled with the battery
 * voltage ({@code F = KF_AT_12V * 12 / volts}), so the same command gives the same RPM on a full
 * or a tired battery. All gains are TESTED on the robot (see {@link LauncherConstants}).
 * <p>
 * A flywheel is "ready" (the indexer may feed) once it is inside the asymmetric feed window
 * [target - {@link LauncherConstants#READY_BELOW_RPM}, target +
 * {@link LauncherConstants#READY_ABOVE_RPM}], and stops being ready only when it is more than
 * {@link LauncherConstants#NOT_READY_TOLERANCE_RPM} away (hysteresis, so it does not flicker).
 * Each RPM dip is counted as a shot ({@link ShotDetector}).
 * <p>
 * <b>POLLEN idle spin</b> ({@link #setPollenIdle(boolean)}, TeleOp only): while it is on, a
 * POLLEN flywheel that is not firing or reversing keeps {@link LauncherConstants#POLLEN_IDLE_RPM}
 * so the next volley is ready sooner. It stays in state {@code IDLE}: never ready, no shots.
 * <p>
 * Call {@link #spinUpPollen()} etc. to say what you want, then {@link #periodic()} every loop.
 */
public class Launcher implements Subsystem {

    /** What one flywheel is doing. */
    public enum FlywheelState { IDLE, FIRING, REVERSING }

    private static final double SECONDS_PER_MINUTE = 60.0;

    private final DcMotorEx pollenMotor;
    private final DcMotorEx nectarMotor;
    private final DoubleSupplier batteryVolts;
    private final MatchTimer.Clock clock;
    private final ShotDetector pollenShots = new ShotDetector();
    private final ShotDetector nectarShots = new ShotDetector();

    private FlywheelState pollenState = FlywheelState.IDLE;
    private FlywheelState nectarState = FlywheelState.IDLE;
    private double pollenTargetRpm = LauncherConstants.POLLEN_TELEOP_RPM;
    private double nectarTargetRpm = LauncherConstants.NECTAR_RPM;
    private boolean isPollenReady = false;
    private boolean isNectarReady = false;
    private boolean isPollenIdleEnabled = false;
    private PIDFCoefficients appliedPidf = new PIDFCoefficients();
    private double lastKfUpdateSec = Double.NaN;
    private double lastPollenVelocity = Double.NaN;
    private double lastNectarVelocity = Double.NaN;
    private double pollenRpm = 0.0;
    private double nectarRpm = 0.0;

    /** @param hardware robot hardware; a missing flywheel motor is skipped */
    public Launcher(final RobotHardware hardware) {
        this(hardware.pollenLauncherMotor, hardware.nectarLauncherMotor, new DoubleSupplier() {
            @Override
            public double getAsDouble() {
                return hardware.getBatteryVolts();
            }
        }, MatchTimer.systemClock());
    }

    /**
     * For tests and special setups.
     *
     * @param pollenMotor  POLLEN flywheel motor, or null
     * @param nectarMotor  NECTAR flywheel motor, or null
     * @param batteryVolts battery voltage
     * @param clock        time source
     */
    public Launcher(DcMotorEx pollenMotor, DcMotorEx nectarMotor, DoubleSupplier batteryVolts,
                    MatchTimer.Clock clock) {
        this.pollenMotor = pollenMotor;
        this.nectarMotor = nectarMotor;
        this.batteryVolts = batteryVolts;
        this.clock = clock;
        applyPidf(compensatedKf(readBatteryVolts()));
    }

    // ---------------- Commands ----------------

    /** Spins the POLLEN flywheel forward at its target RPM. */
    public void spinUpPollen() {
        pollenState = FlywheelState.FIRING;
    }

    /** Spins the POLLEN flywheel backward (to clear a ball). */
    public void reversePollen() {
        pollenState = FlywheelState.REVERSING;
    }

    /**
     * Lets the POLLEN flywheel coast down (or keep {@link LauncherConstants#POLLEN_IDLE_RPM}
     * while the idle spin is on, else {@link LauncherConstants#IDLE_RPM}).
     */
    public void stopPollen() {
        pollenState = FlywheelState.IDLE;
    }

    /** Spins the NECTAR flywheel forward at its target RPM. */
    public void spinUpNectar() {
        nectarState = FlywheelState.FIRING;
    }

    /** Spins the NECTAR flywheel backward. */
    public void reverseNectar() {
        nectarState = FlywheelState.REVERSING;
    }

    /** Lets the NECTAR flywheel coast down (or run at {@link LauncherConstants#IDLE_RPM}). */
    public void stopNectar() {
        nectarState = FlywheelState.IDLE;
    }

    /**
     * Turns the POLLEN idle spin on or off (TeleOp). The OpMode turns it off while the battery is
     * low. AUTO never turns it on: the routine spins up on its own.
     *
     * @param isEnabled true to keep {@link LauncherConstants#POLLEN_IDLE_RPM} between volleys
     */
    public void setPollenIdle(boolean isEnabled) {
        isPollenIdleEnabled = isEnabled;
    }

    /** @return true while the POLLEN idle spin is on */
    public boolean isPollenIdleEnabled() {
        return isPollenIdleEnabled;
    }

    /** Stops both flywheels right away (they coast) and turns the idle spin off. Keeps the targets. */
    @Override
    public void stop() {
        isPollenIdleEnabled = false;
        pollenState = FlywheelState.IDLE;
        nectarState = FlywheelState.IDLE;
        setPollenVelocity(0.0);
        setNectarVelocity(0.0);
        isPollenReady = false;
        isNectarReady = false;
    }

    /**
     * Sets both target RPMs (for example per autonomous route).
     *
     * @param pollenRpm POLLEN target, clamped to the safe range
     * @param nectarRpm NECTAR target, clamped to the safe range
     */
    public void setTargetRpm(double pollenRpm, double nectarRpm) {
        pollenTargetRpm = clampRpm(pollenRpm);
        nectarTargetRpm = clampRpm(nectarRpm);
    }

    /** @param rpm new POLLEN target, clamped to the safe range */
    public void setPollenTargetRpm(double rpm) {
        pollenTargetRpm = clampRpm(rpm);
    }

    /** @param rpm new NECTAR target, clamped to the safe range */
    public void setNectarTargetRpm(double rpm) {
        nectarTargetRpm = clampRpm(rpm);
    }

    /** @param deltaRpm change of the POLLEN target (for example +/- RPM_STEP on a D-pad tap) */
    public void adjustPollenRpm(double deltaRpm) {
        setPollenTargetRpm(pollenTargetRpm + deltaRpm);
    }

    /** @param deltaRpm change of the NECTAR target */
    public void adjustNectarRpm(double deltaRpm) {
        setNectarTargetRpm(nectarTargetRpm + deltaRpm);
    }

    // ---------------- Loop ----------------

    /**
     * Rescales F with the battery, sends the velocity commands, reads the RPM, and updates the
     * ready flags and shot counts. Call once per loop.
     */
    @Override
    public void periodic() {
        double nowSec = clock.nowSeconds();
        compensateForVoltage(nowSec);

        double pollenIdleRpm = isPollenIdleEnabled ? LauncherConstants.POLLEN_IDLE_RPM
                : LauncherConstants.IDLE_RPM;
        setPollenVelocity(rpmToTicksPerSecond(commandRpm(pollenState, pollenTargetRpm, pollenIdleRpm)));
        setNectarVelocity(rpmToTicksPerSecond(commandRpm(nectarState, nectarTargetRpm,
                LauncherConstants.IDLE_RPM)));

        pollenRpm = pollenMotor == null ? 0.0 : ticksPerSecondToRpm(pollenMotor.getVelocity());
        nectarRpm = nectarMotor == null ? 0.0 : ticksPerSecondToRpm(nectarMotor.getVelocity());

        isPollenReady = updateReady(isPollenReady, pollenState == FlywheelState.FIRING,
                pollenRpm, pollenTargetRpm);
        isNectarReady = updateReady(isNectarReady, nectarState == FlywheelState.FIRING,
                nectarRpm, nectarTargetRpm);

        pollenShots.update(pollenState == FlywheelState.FIRING, pollenTargetRpm, pollenRpm, nowSec);
        nectarShots.update(nectarState == FlywheelState.FIRING, nectarTargetRpm, nectarRpm, nowSec);
    }

    /**
     * Readiness of one flywheel, with hysteresis: it becomes ready only inside the asymmetric
     * feed window [target - {@link LauncherConstants#READY_BELOW_RPM}, target +
     * {@link LauncherConstants#READY_ABOVE_RPM}], and stays ready until it is more than
     * {@link LauncherConstants#NOT_READY_TOLERANCE_RPM} off. A shot dip ends readiness, so the
     * next ball waits for the window again.
     *
     * @param wasReady  readiness last loop
     * @param isFiring  true if the flywheel is spinning forward
     * @param rpm       measured RPM
     * @param targetRpm target RPM
     * @return the new readiness; always false unless firing
     */
    public static boolean updateReady(boolean wasReady, boolean isFiring, double rpm,
                                      double targetRpm) {
        if (!isFiring) {
            return false;
        }
        double errorRpm = rpm - targetRpm;
        if (wasReady) {
            return Math.abs(errorRpm) <= LauncherConstants.NOT_READY_TOLERANCE_RPM;
        }
        return errorRpm >= -LauncherConstants.READY_BELOW_RPM
                && errorRpm <= LauncherConstants.READY_ABOVE_RPM;
    }

    /**
     * @param volts battery voltage
     * @return F rescaled for this voltage; readings below the valid minimum use 12 V
     */
    public static double compensatedKf(double volts) {
        double usedVolts = volts >= LauncherConstants.KF_MIN_VALID_VOLTS ? volts
                : RobotConstants.NOMINAL_VOLTS;
        return LauncherConstants.KF_AT_12V * RobotConstants.NOMINAL_VOLTS / usedVolts;
    }

    /**
     * @param rpm revolutions per minute
     * @return encoder ticks per second for {@link DcMotorEx#setVelocity(double)}
     */
    public static double rpmToTicksPerSecond(double rpm) {
        return rpm * LauncherConstants.TICKS_PER_REV / SECONDS_PER_MINUTE;
    }

    /**
     * @param ticksPerSecond encoder velocity
     * @return revolutions per minute
     */
    public static double ticksPerSecondToRpm(double ticksPerSecond) {
        return ticksPerSecond / LauncherConstants.TICKS_PER_REV * SECONDS_PER_MINUTE;
    }

    // ---------------- Queries ----------------

    /** @return true while the POLLEN flywheel is firing at its target (ready to feed) */
    public boolean isPollenReady() {
        return isPollenReady;
    }

    /** @return true while the NECTAR flywheel is firing at its target */
    public boolean isNectarReady() {
        return isNectarReady;
    }

    /** @return true if at least one flywheel is firing and every firing flywheel is ready */
    public boolean isAtTargetRpm() {
        boolean isPollenFiring = pollenState == FlywheelState.FIRING;
        boolean isNectarFiring = nectarState == FlywheelState.FIRING;
        if (!isPollenFiring && !isNectarFiring) {
            return false;
        }
        return (!isPollenFiring || isPollenReady) && (!isNectarFiring || isNectarReady);
    }

    /** @return true if either flywheel is spinning forward */
    public boolean isFiring() {
        return pollenState == FlywheelState.FIRING || nectarState == FlywheelState.FIRING;
    }

    /** @return POLLEN target RPM */
    public double getPollenTargetRpm() {
        return pollenTargetRpm;
    }

    /** @return NECTAR target RPM */
    public double getNectarTargetRpm() {
        return nectarTargetRpm;
    }

    /** @return measured POLLEN RPM (from the last {@link #periodic()}) */
    public double getPollenRpm() {
        return pollenRpm;
    }

    /** @return measured NECTAR RPM (from the last {@link #periodic()}) */
    public double getNectarRpm() {
        return nectarRpm;
    }

    /** @return POLLEN flywheel state */
    public FlywheelState getPollenState() {
        return pollenState;
    }

    /** @return NECTAR flywheel state */
    public FlywheelState getNectarState() {
        return nectarState;
    }

    /** @return POLLEN shots detected since the OpMode started */
    public int getPollenShotCount() {
        return pollenShots.getShotCount();
    }

    /** @return NECTAR shots detected since the OpMode started */
    public int getNectarShotCount() {
        return nectarShots.getShotCount();
    }

    /** @return shots of both flywheels */
    public int getTotalShotCount() {
        return getPollenShotCount() + getNectarShotCount();
    }

    /** @return POLLEN shot statistics */
    public ShotDetector getPollenShotStats() {
        return pollenShots;
    }

    /** @return NECTAR shot statistics */
    public ShotDetector getNectarShotStats() {
        return nectarShots;
    }

    /** @return the feedforward F now applied, after voltage compensation */
    public double getAppliedKf() {
        return appliedPidf.f;
    }

    /** @return true if both flywheel motors are configured */
    public boolean isConnected() {
        return pollenMotor != null && nectarMotor != null;
    }

    @Override
    public void addTelemetry(Telemetry t) {
        t.addData("Launcher POLLEN", "%s %.0f / %.0f RPM%s, shots %d", pollenState, pollenRpm,
                pollenTargetRpm, isPollenReady ? " READY" : "", getPollenShotCount());
        t.addData("Launcher NECTAR", "%s %.0f / %.0f RPM%s, shots %d", nectarState, nectarRpm,
                nectarTargetRpm, isNectarReady ? " READY" : "", getNectarShotCount());
        t.addData("Launcher kF", "%.2f", appliedPidf.f);
        t.addData("Launcher POLLEN idle", "%s", isPollenIdleEnabled
                ? "ON " + Math.round(LauncherConstants.POLLEN_IDLE_RPM) + " RPM" : "off");
        if (!isConnected()) {
            t.addLine("Launcher: flywheel motor missing");
        }
    }

    // ---------------- Internals ----------------

    private static double commandRpm(FlywheelState state, double targetRpm, double idleRpm) {
        switch (state) {
            case FIRING:
                return targetRpm;
            case REVERSING:
                return -targetRpm;
            default:
                return idleRpm;
        }
    }

    /**
     * Rescales F every {@link LauncherConstants#KF_UPDATE_SECONDS}, and resends the gains when P,
     * I or D were changed from FTC Dashboard. F is resent only if it changed by more than
     * {@link LauncherConstants#KF_MIN_CHANGE} (fraction), to save hub traffic.
     */
    private void compensateForVoltage(double nowSec) {
        if (!Double.isNaN(lastKfUpdateSec)
                && nowSec - lastKfUpdateSec < LauncherConstants.KF_UPDATE_SECONDS) {
            return;
        }
        lastKfUpdateSec = nowSec;
        double kf = compensatedKf(readBatteryVolts());
        boolean isKfChanged = Math.abs(kf - appliedPidf.f) > appliedPidf.f * LauncherConstants.KF_MIN_CHANGE;
        boolean isPidChanged = appliedPidf.p != LauncherConstants.KP
                || appliedPidf.i != LauncherConstants.KI || appliedPidf.d != LauncherConstants.KD;
        if (isKfChanged || isPidChanged) {
            applyPidf(kf);
        }
    }

    private double readBatteryVolts() {
        return batteryVolts == null ? RobotConstants.NOMINAL_VOLTS : batteryVolts.getAsDouble();
    }

    private void applyPidf(double kf) {
        appliedPidf = new PIDFCoefficients(LauncherConstants.KP, LauncherConstants.KI,
                LauncherConstants.KD, kf);
        if (pollenMotor != null) {
            pollenMotor.setPIDFCoefficients(DcMotor.RunMode.RUN_USING_ENCODER, appliedPidf);
        }
        if (nectarMotor != null) {
            nectarMotor.setPIDFCoefficients(DcMotor.RunMode.RUN_USING_ENCODER, appliedPidf);
        }
    }

    private void setPollenVelocity(double ticksPerSecond) {
        if (ticksPerSecond == lastPollenVelocity) {
            return;
        }
        lastPollenVelocity = ticksPerSecond;
        if (pollenMotor != null) {
            pollenMotor.setVelocity(ticksPerSecond);
        }
    }

    private void setNectarVelocity(double ticksPerSecond) {
        if (ticksPerSecond == lastNectarVelocity) {
            return;
        }
        lastNectarVelocity = ticksPerSecond;
        if (nectarMotor != null) {
            nectarMotor.setVelocity(ticksPerSecond);
        }
    }

    private static double clampRpm(double rpm) {
        return Math.max(LauncherConstants.MIN_RPM, Math.min(LauncherConstants.MAX_RPM, rpm));
    }
}
