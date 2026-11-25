// Sources: IronLion30311BIOBUZZInicioDeTemporada / branch feature/turrubiates/vision-align / TeamCode/.../subsystems/Indexer.java @ a7b8f4c (feed power, jam detection, commit 9c03b6c)
//          — adapted for BIOBUZZ
package org.firstinspires.ftc.teamcode.config;

import com.acmerobotics.dashboard.config.Config;
import com.qualcomm.robotcore.hardware.DcMotorSimple;

/**
 * Indexer tuning values. The indexer motor (old name "alejandro") moves stored balls into the
 * launcher flywheels. It pauses while the flywheel is below its target RPM, so each shot leaves
 * at the right speed.
 */
@Config
public final class IndexerConstants {

    private IndexerConstants() {
    }

    /** TESTED: positive power feeds balls toward the launchers. */
    public static final DcMotorSimple.Direction DIRECTION = DcMotorSimple.Direction.FORWARD;

    /** TESTED: power used in every scored run. */
    public static double FEED_POWER = 0.75;
    public static double RAMP_SECONDS = 0.15;

    /** TESTED ("29 of 46 scored" with it): pause feeding until the flywheel is ready. */
    public static boolean PAUSE_WHEN_NOT_READY = true;

    // Jam detection, FITTED from match logs: high current for too long means a ball is stuck.
    public static double JAM_CURRENT_AMPS = 5.0;
    public static double JAM_TIME_SECONDS = 0.5;
    public static double UNJAM_TIME_SECONDS = 0.3;
    public static double UNJAM_STEP_SECONDS = 0.2;
    public static int MAX_UNJAM_ATTEMPTS = 3;
    public static double JAM_RESET_SECONDS = 2.0;
    /** Time between current readings; each reading is an extra hub transaction. */
    public static double CURRENT_CHECK_INTERVAL_SECONDS = 0.05;
    /** Largest loop time used by the ramp, so the first loop after INIT does not jump. */
    public static final double RAMP_MAX_DT_SECONDS = 0.1;
}
