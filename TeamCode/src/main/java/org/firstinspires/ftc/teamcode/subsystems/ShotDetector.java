// Sources: IronLion30311BIOBUZZInicioDeTemporada / branch feature/turrubiates/vision-align / TeamCode/.../subsystems/ShooterLogger.java @ a7b8f4c (ShotTracker: RPM-dip shot detection and per-shot stats)
//          — adapted for BIOBUZZ (own class, used by Launcher; thresholds in LauncherConstants)
package org.firstinspires.ftc.teamcode.subsystems;

import org.firstinspires.ftc.teamcode.config.LauncherConstants;

/**
 * Detects shots on one flywheel from its RPM dips.
 * <p>
 * When a ball leaves, it slows the flywheel down. A shot starts when a firing flywheel falls
 * more than {@link LauncherConstants#SHOT_DIP_RPM} below its target, and ends when it is back
 * within {@link LauncherConstants#SHOT_RECOVERED_RPM}. Spin-up is not counted: detection only
 * starts after the flywheel first reached its target.
 * <p>
 * Pure logic, unit tested on the computer.
 */
public class ShotDetector {

    private boolean hasReachedTarget = false;
    private boolean isInShot = false;
    private int shotCount = 0;
    private double shotStartSec = 0.0;
    private double shotMinRpm = 0.0;
    private double lastDropRpm = 0.0;
    private double lastRecoveryMs = 0.0;
    private double worstDropRpm = 0.0;

    /**
     * Feeds one RPM reading.
     *
     * @param isFiring  true while the flywheel is spinning forward at its target
     * @param targetRpm target RPM
     * @param rpm       measured RPM
     * @param timeSec   current time in seconds
     */
    public void update(boolean isFiring, double targetRpm, double rpm, double timeSec) {
        if (!isFiring) {
            hasReachedTarget = false;
            isInShot = false;
            return;
        }
        double errorRpm = targetRpm - rpm;
        if (!hasReachedTarget) {
            hasReachedTarget = errorRpm <= LauncherConstants.SHOT_RECOVERED_RPM;
        } else if (!isInShot && errorRpm > LauncherConstants.SHOT_DIP_RPM) {
            isInShot = true;
            shotCount++;
            shotStartSec = timeSec;
            shotMinRpm = rpm;
        } else if (isInShot) {
            shotMinRpm = Math.min(shotMinRpm, rpm);
            if (errorRpm <= LauncherConstants.SHOT_RECOVERED_RPM) {
                isInShot = false;
                lastDropRpm = targetRpm - shotMinRpm;
                lastRecoveryMs = (timeSec - shotStartSec) * 1000.0;
                worstDropRpm = Math.max(worstDropRpm, lastDropRpm);
            }
        }
    }

    /** @return shots detected since the OpMode started */
    public int getShotCount() {
        return shotCount;
    }

    /** @return true while a shot dip is in progress */
    public boolean isInShot() {
        return isInShot;
    }

    /** @return RPM lost on the last finished shot */
    public double getLastDropRpm() {
        return lastDropRpm;
    }

    /** @return time from the start of the last shot until recovered, in ms */
    public double getLastRecoveryMs() {
        return lastRecoveryMs;
    }

    /** @return largest RPM drop of any shot */
    public double getWorstDropRpm() {
        return worstDropRpm;
    }
}
