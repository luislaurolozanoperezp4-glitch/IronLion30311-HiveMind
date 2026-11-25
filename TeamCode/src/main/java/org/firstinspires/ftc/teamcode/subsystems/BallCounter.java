// Sources: IronLion30311BIOBUZZInicioDeTemporada / branch feature/turrubiates/vision-align / TeamCode/.../subsystems/vision/BallIntakeCounter.java @ a7b8f4c (whole counting logic)
//          — adapted for BIOBUZZ (pure logic: balls and time are passed in, so it is unit tested)
package org.firstinspires.ftc.teamcode.subsystems;

import org.firstinspires.ftc.teamcode.config.VisionConstants;

import java.util.List;
import java.util.Locale;

/**
 * Counts the balls the intake picks up, by color, from what the front HuskyLens sees.
 * <p>
 * A ball is counted when, with the intake running, the nearest ball (lowest in the image,
 * closest to the intake) was at least {@link VisionConstants#BALL_NEAR_Y_OFFSET} down the image
 * and then either disappears for {@link VisionConstants#BALL_LOST_CONFIRM_SECONDS} or is replaced
 * by one much higher in the image (the next ball of a row; the near one went into the intake).
 * <p>
 * It is an estimate: a ball pushed aside right in front of the intake also counts.
 * Used by {@link ColorSensing}.
 */
public class BallCounter {

    private final int[] counts = new int[BallColor.values().length];

    private ColorSensing.Ball nearest;
    private BallColor nearColor;
    private double lastNearestY = Double.NaN;
    private double nearSeenSec = Double.NaN;
    private double lastPickupSec = Double.NaN;
    private BallColor lastPicked;

    /**
     * Updates the counts with one camera frame.
     *
     * @param balls      balls seen in this frame
     * @param isIntaking true while the intake collects (balls only count then)
     * @param nowSec     current time in seconds
     */
    public void update(List<ColorSensing.Ball> balls, boolean isIntaking, double nowSec) {
        nearest = null;
        for (ColorSensing.Ball ball : balls) {
            if (nearest == null || ball.yOffset > nearest.yOffset) {
                nearest = ball;
            }
        }
        if (!isIntaking) {
            nearColor = null;
            lastNearestY = Double.NaN;
            return;
        }
        if (nearest != null) {
            boolean isNextBall = nearColor != null && !Double.isNaN(lastNearestY)
                    && lastNearestY - nearest.yOffset > VisionConstants.BALL_NEXT_JUMP;
            if (isNextBall) {
                countPickup(nowSec);
            }
            if (nearest.yOffset >= VisionConstants.BALL_NEAR_Y_OFFSET) {
                nearColor = nearest.color;
                nearSeenSec = nowSec;
            }
            lastNearestY = nearest.yOffset;
        } else {
            lastNearestY = Double.NaN;
            if (nearColor != null
                    && nowSec - nearSeenSec >= VisionConstants.BALL_LOST_CONFIRM_SECONDS) {
                countPickup(nowSec);
            }
        }
    }

    private void countPickup(double nowSec) {
        boolean isNewBall = Double.isNaN(lastPickupSec)
                || nowSec - lastPickupSec >= VisionConstants.BALL_MIN_PICKUP_INTERVAL_SECONDS;
        if (isNewBall) {
            counts[nearColor.ordinal()]++;
            lastPicked = nearColor;
            lastPickupSec = nowSec;
        }
        nearColor = null;
    }

    /**
     * @param color a ball color
     * @return balls of that color picked up so far
     */
    public int getCount(BallColor color) {
        return counts[color.ordinal()];
    }

    /** @return all balls picked up so far */
    public int getTotal() {
        int total = 0;
        for (int count : counts) {
            total += count;
        }
        return total;
    }

    /** @return the nearest ball in the last frame, or null */
    public ColorSensing.Ball getNearest() {
        return nearest;
    }

    /** @return the color of the last ball picked up, or null */
    public BallColor getLastPicked() {
        return lastPicked;
    }

    /** Sets every count back to zero. */
    public void reset() {
        for (int i = 0; i < counts.length; i++) {
            counts[i] = 0;
        }
        nearColor = null;
        lastPicked = null;
        lastNearestY = Double.NaN;
        lastPickupSec = Double.NaN;
    }

    /** @return for example "4 (pollen 4, red nectar 0, blue nectar 0)" */
    public String getSummary() {
        return String.format(Locale.US, "%d (pollen %d, red nectar %d, blue nectar %d)",
                getTotal(), getCount(BallColor.POLLEN), getCount(BallColor.RED_NECTAR),
                getCount(BallColor.BLUE_NECTAR));
    }
}
