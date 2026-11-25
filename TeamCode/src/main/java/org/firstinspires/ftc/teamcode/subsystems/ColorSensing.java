// Sources: IronLion30311BIOBUZZInicioDeTemporada / branch feature/turrubiates/vision-align / TeamCode/.../subsystems/vision/BallVision.java @ a7b8f4c (HuskyLens read, Ball, getClosestBall, getAssistTurn, colorOf)
//          IronLion30311BIOBUZZInicioDeTemporada / branch feature/turrubiates/vision-align / TeamCode/.../subsystems/vision/BallIntakeCounter.java @ a7b8f4c (counting, now in BallCounter)
//          IronLion30311BIOBUZZInicioDeTemporada / branch feature/turrubiates/vision-align / TeamCode/.../subsystems/NectarAlertLight.java @ a7b8f4c (closest NECTAR check)
//          — adapted for BIOBUZZ (renamed BallVision -> ColorSensing; NECTAR-at-intake and opponent NECTAR checks for G408)
package org.firstinspires.ftc.teamcode.subsystems;

import com.qualcomm.hardware.dfrobot.HuskyLens;

import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.teamcode.config.VisionConstants;
import org.firstinspires.ftc.teamcode.hardware.RobotHardware;
import org.firstinspires.ftc.teamcode.util.Alliance;
import org.firstinspires.ftc.teamcode.util.MatchTimer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Sees POLLEN and NECTAR balls in front of the intake with the HuskyLens, by color, and counts
 * the balls the intake picks up.
 * <p>
 * The HuskyLens runs its COLOR_RECOGNITION algorithm. Each color must be learned on the
 * HuskyLens (point it at the ball and press the learn button) with the IDs in
 * {@link VisionConstants}: POLLEN 1, RED NECTAR 2, BLUE NECTAR 3 (decision D11).
 * <p>
 * Each read is a slow I2C transaction, so the camera is read at most every
 * {@link VisionConstants#HUSKY_READ_INTERVAL_SECONDS}. If the HuskyLens is missing,
 * {@link #isConnected()} is false and no balls are reported.
 * <p>
 * Every loop: {@link #countWhileIntaking(boolean)} (tell it if the intake collects), then
 * {@link #periodic()}.
 */
public class ColorSensing implements Subsystem {

    /** One ball seen in the last camera read. */
    public static class Ball {
        public final BallColor color;
        /** Horizontal position: -1 = left edge, 0 = center, +1 = right edge of the image. */
        public final double xOffset;
        /** Vertical position: -1 = top, 0 = center, +1 = bottom (closest to the intake). */
        public final double yOffset;
        /** Block area in pixels squared; larger means closer. */
        public final int areaPixels;

        /**
         * @param color      ball color
         * @param xOffset    horizontal position, -1..1
         * @param yOffset    vertical position, -1..1
         * @param areaPixels block area in pixels squared
         */
        public Ball(BallColor color, double xOffset, double yOffset, int areaPixels) {
            this.color = color;
            this.xOffset = xOffset;
            this.yOffset = yOffset;
            this.areaPixels = areaPixels;
        }
    }

    private final HuskyLens huskyLens;
    private final MatchTimer.Clock clock;
    private final BallCounter counter = new BallCounter();
    private List<Ball> balls = Collections.emptyList();
    private double lastReadSec = Double.NaN;
    private boolean isIntaking = false;

    /** @param hardware robot hardware; the HuskyLens may be missing */
    public ColorSensing(RobotHardware hardware) {
        this(hardware.huskyLens, MatchTimer.systemClock());
    }

    /**
     * For tests: pass {@code null} as the camera and feed frames with {@link #updateFromBalls(List)}.
     *
     * @param huskyLens the camera, or null
     * @param clock     time source
     */
    public ColorSensing(HuskyLens huskyLens, MatchTimer.Clock clock) {
        this.huskyLens = huskyLens;
        this.clock = clock;
        if (huskyLens != null) {
            huskyLens.selectAlgorithm(HuskyLens.Algorithm.COLOR_RECOGNITION);
        }
    }

    /**
     * Tells the counter whether the intake is collecting now (balls only count then).
     *
     * @param isIntakeCollecting true while the intake pulls balls in
     */
    public void countWhileIntaking(boolean isIntakeCollecting) {
        isIntaking = isIntakeCollecting;
    }

    /** Reads the camera when due and updates the ball list and counts. Call once per loop. */
    @Override
    public void periodic() {
        if (huskyLens == null) {
            return;
        }
        double nowSec = clock.nowSeconds();
        if (!Double.isNaN(lastReadSec)
                && nowSec - lastReadSec < VisionConstants.HUSKY_READ_INTERVAL_SECONDS) {
            return;
        }
        lastReadSec = nowSec;
        HuskyLens.Block[] blocks = huskyLens.blocks();
        List<Ball> seen = new ArrayList<>();
        if (blocks != null) {
            for (HuskyLens.Block block : blocks) {
                Ball ball = toBall(block.id, block.x, block.y, block.width, block.height);
                if (ball != null) {
                    seen.add(ball);
                }
            }
        }
        updateFromBalls(seen);
    }

    /**
     * Uses one camera frame. {@link #periodic()} calls it with the HuskyLens blocks; tests call
     * it directly.
     *
     * @param seen balls in this frame
     */
    public void updateFromBalls(List<Ball> seen) {
        balls = seen;
        counter.update(seen, isIntaking, clock.nowSeconds());
    }

    /**
     * Turns one HuskyLens block into a ball.
     *
     * @param id     learned color ID
     * @param x      block center x, pixels
     * @param y      block center y, pixels
     * @param width  block width, pixels
     * @param height block height, pixels
     * @return the ball, or null if the ID is unknown or the block is too small
     */
    public static Ball toBall(int id, int x, int y, int width, int height) {
        BallColor color = colorOf(id);
        int area = width * height;
        if (color == null || area < VisionConstants.HUSKY_MIN_BLOCK_AREA_PIXELS) {
            return null;
        }
        double halfWidth = VisionConstants.HUSKY_IMAGE_WIDTH_PIXELS / 2.0;
        double halfHeight = VisionConstants.HUSKY_IMAGE_HEIGHT_PIXELS / 2.0;
        return new Ball(color, (x - halfWidth) / halfWidth, (y - halfHeight) / halfHeight, area);
    }

    /** Nothing to stop: the camera only reads. */
    @Override
    public void stop() {
    }

    /** @return every ball seen in the last read (never null) */
    public List<Ball> getBalls() {
        return balls;
    }

    /**
     * @param colors colors to consider; none means any color
     * @return the closest (largest) ball of those colors, or null
     */
    public Ball getClosestBall(BallColor... colors) {
        Ball closest = null;
        for (Ball ball : balls) {
            if (matches(ball.color, colors) && (closest == null || ball.areaPixels > closest.areaPixels)) {
                closest = ball;
            }
        }
        return closest;
    }

    /** @return the closest NECTAR ball anywhere in view, or null (used by the NECTAR alert) */
    public Ball getClosestNectar() {
        return getClosestBall(BallColor.RED_NECTAR, BallColor.BLUE_NECTAR);
    }

    /** @return true if a NECTAR ball (either color) is right in front of the intake */
    public boolean isNectarAtIntake() {
        return isAtIntake(BallColor.RED_NECTAR) || isAtIntake(BallColor.BLUE_NECTAR);
    }

    /**
     * G408 helper: the intake can push away the other alliance's NECTAR.
     *
     * @param alliance our alliance
     * @return true if the opponent's NECTAR is right in front of the intake
     */
    public boolean isOpponentNectarAtIntake(Alliance alliance) {
        for (Ball ball : balls) {
            if (ball.color.isOpponentNectar(alliance)
                    && ball.yOffset >= VisionConstants.INTAKE_ZONE_Y_OFFSET) {
                return true;
            }
        }
        return false;
    }

    /**
     * TeleOp ball assist: turn command that centers the closest ball in front of the intake.
     *
     * @param colors colors to consider; none means any color
     * @return turn power (+ = clockwise), or 0 if no ball is seen
     */
    public double getAssistTurn(BallColor... colors) {
        Ball ball = getClosestBall(colors);
        if (ball == null) {
            return 0.0;
        }
        double turn = VisionConstants.BALL_ASSIST_KP * ball.xOffset;
        return Math.max(-VisionConstants.BALL_ASSIST_MAX_TURN,
                Math.min(VisionConstants.BALL_ASSIST_MAX_TURN, turn));
    }

    /**
     * @param color a ball color
     * @return balls of that color the intake picked up so far
     */
    public int getBallCount(BallColor color) {
        return counter.getCount(color);
    }

    /** @return all balls the intake picked up so far */
    public int getTotalBalls() {
        return counter.getTotal();
    }

    /** @return the color of the last ball picked up, or null */
    public BallColor getLastPicked() {
        return counter.getLastPicked();
    }

    /** @return for example "4 (pollen 4, red nectar 0, blue nectar 0)" */
    public String getSummary() {
        return counter.getSummary();
    }

    /** Sets the pickup counts back to zero. */
    public void resetCounts() {
        counter.reset();
    }

    /** @return true if the HuskyLens is in the configuration */
    public boolean isConnected() {
        return huskyLens != null;
    }

    @Override
    public void addTelemetry(Telemetry t) {
        if (!isConnected()) {
            t.addLine("Color sensing: HuskyLens not configured");
            return;
        }
        Ball closest = getClosestBall();
        t.addData("Balls seen", "%d, closest %s", balls.size(),
                closest == null ? "none" : closest.color.toString());
        t.addData("Balls picked", counter.getSummary());
    }

    private boolean isAtIntake(BallColor color) {
        for (Ball ball : balls) {
            if (ball.color == color && ball.yOffset >= VisionConstants.INTAKE_ZONE_Y_OFFSET) {
                return true;
            }
        }
        return false;
    }

    private static BallColor colorOf(int id) {
        if (id == VisionConstants.HUSKY_POLLEN_ID) {
            return BallColor.POLLEN;
        }
        if (id == VisionConstants.HUSKY_RED_NECTAR_ID) {
            return BallColor.RED_NECTAR;
        }
        if (id == VisionConstants.HUSKY_BLUE_NECTAR_ID) {
            return BallColor.BLUE_NECTAR;
        }
        return null;
    }

    private static boolean matches(BallColor color, BallColor[] colors) {
        if (colors.length == 0) {
            return true;
        }
        for (BallColor allowed : colors) {
            if (allowed == color) {
                return true;
            }
        }
        return false;
    }
}
