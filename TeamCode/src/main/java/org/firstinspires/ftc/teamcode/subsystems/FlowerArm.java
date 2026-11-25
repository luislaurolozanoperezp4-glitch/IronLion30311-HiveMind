// Sources: IronLion30311BIOBUZZInicioDeTemporada / branch feature/renata/flower-arm / TeamCode/.../subsystems/FlowerArm.java @ 889102d (deploy, retract, toggle, isDeployed) — fixed: the constructor was named DualServo and did not compile
//          — adapted for BIOBUZZ (optional servos, positions in FlowerArmConstants, G410 deploy gate, edge detection moved to GamepadEx)
package org.firstinspires.ftc.teamcode.subsystems;

import com.qualcomm.robotcore.hardware.Servo;

import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.teamcode.config.FlowerArmConstants;
import org.firstinspires.ftc.teamcode.hardware.RobotHardware;
import org.firstinspires.ftc.teamcode.util.MatchTimer;

import java.util.function.BooleanSupplier;

/**
 * The flower arm: two servos facing each other that swing out together.
 * <p>
 * <b>Optional:</b> the team has not confirmed the arm is on the robot (decision D14). If the
 * servos are not in the configuration, {@link #isAvailable()} is false, a warning is shown, and
 * every call does nothing.
 * <p>
 * <b>G410:</b> NECTAR may go into FLOWERS only in the last 60 seconds. Give the arm a gate with
 * {@link #setDeployGate(BooleanSupplier)} (for example {@code matchTimer::canPlaceNectar}), or
 * use {@link #deployIfAllowed(MatchTimer)}; {@link #deploy()} is refused while the gate is
 * closed. Retracting is always allowed.
 */
public class FlowerArm implements Subsystem {

    private final Servo leftServo;
    private final Servo rightServo;

    private BooleanSupplier deployGate;
    private boolean isDeployed = false;
    private int refusedCount = 0;

    /** @param hardware robot hardware; the flower arm servos may be missing */
    public FlowerArm(RobotHardware hardware) {
        this(hardware.flowerArmLeftServo, hardware.flowerArmRightServo);
    }

    /**
     * For tests.
     *
     * @param leftServo  left servo, or null
     * @param rightServo right servo, or null
     */
    public FlowerArm(Servo leftServo, Servo rightServo) {
        boolean isComplete = leftServo != null && rightServo != null;
        this.leftServo = isComplete ? leftServo : null;
        this.rightServo = isComplete ? rightServo : null;
        retract();
    }

    /**
     * @param gate returns true when deploying is allowed (G410), or null for "always allowed"
     */
    public void setDeployGate(BooleanSupplier gate) {
        deployGate = gate;
    }

    /** @return true if the gate allows deploying now */
    public boolean canDeploy() {
        return deployGate == null || deployGate.getAsBoolean();
    }

    /**
     * Swings the arm out, if the gate allows it.
     *
     * @return true if the arm is now deployed
     */
    public boolean deploy() {
        if (!canDeploy()) {
            refusedCount++;
            return false;
        }
        isDeployed = true;
        setPositions(FlowerArmConstants.DEPLOYED_POSITION);
        return true;
    }

    /**
     * Deploys only if the match timer says NECTAR may be placed (G410: last 60 s of TELEOP).
     *
     * @param timer the TeleOp match timer
     * @return true if the arm is now deployed
     */
    public boolean deployIfAllowed(MatchTimer timer) {
        if (timer == null || !timer.canPlaceNectar()) {
            refusedCount++;
            return false;
        }
        isDeployed = true;
        setPositions(FlowerArmConstants.DEPLOYED_POSITION);
        return true;
    }

    /** Folds the arm back in. Always allowed. */
    public void retract() {
        isDeployed = false;
        setPositions(FlowerArmConstants.REST_POSITION);
    }

    /** Deploys if folded (and allowed), folds if deployed. */
    public void toggle() {
        if (isDeployed) {
            retract();
        } else {
            deploy();
        }
    }

    /** @return true if the arm is out */
    public boolean isDeployed() {
        return isDeployed;
    }

    /** @return true if both servos are configured */
    public boolean isAvailable() {
        return leftServo != null;
    }

    /** @return how many deploy requests the gate refused */
    public int getRefusedCount() {
        return refusedCount;
    }

    /** Re-sends the position so values tuned from FTC Dashboard apply. */
    @Override
    public void periodic() {
        setPositions(isDeployed ? FlowerArmConstants.DEPLOYED_POSITION
                : FlowerArmConstants.REST_POSITION);
    }

    /** Servos hold their position; nothing to stop. */
    @Override
    public void stop() {
    }

    @Override
    public void addTelemetry(Telemetry t) {
        if (!isAvailable()) {
            t.addLine("Flower arm: servos not configured (optional)");
            return;
        }
        t.addData("Flower arm", "%s%s", isDeployed ? "DEPLOYED" : "REST",
                canDeploy() ? "" : " (locked until endgame)");
    }

    private void setPositions(double position) {
        if (leftServo == null) {
            return;
        }
        // Both servos get the same value; RobotHardware reverses one so they mirror.
        if (leftServo.getPosition() != position) {
            leftServo.setPosition(position);
        }
        if (rightServo.getPosition() != position) {
            rightServo.setPosition(position);
        }
    }
}
