// Sources: IronLion30311BIOBUZZInicioDeTemporada / branch feature/turrubiates/vision-align / TeamCode/.../subsystems/vision/BallVision.java @ a7b8f4c (BallColor enum)
//          — adapted for BIOBUZZ (own file; NECTAR / opponent helpers for G408)
package org.firstinspires.ftc.teamcode.subsystems;

import org.firstinspires.ftc.teamcode.util.Alliance;

/** Color of a ball, as learned on the HuskyLens. */
public enum BallColor {
    POLLEN,
    RED_NECTAR,
    BLUE_NECTAR;

    /** @return true for either NECTAR color */
    public boolean isNectar() {
        return this == RED_NECTAR || this == BLUE_NECTAR;
    }

    /**
     * @param alliance our alliance
     * @return true if this is the other alliance's NECTAR (the robot should not keep it)
     */
    public boolean isOpponentNectar(Alliance alliance) {
        return alliance == Alliance.RED ? this == BLUE_NECTAR : this == RED_NECTAR;
    }

    /**
     * @param alliance our alliance
     * @return the NECTAR color that belongs to that alliance
     */
    public static BallColor nectarOf(Alliance alliance) {
        return alliance == Alliance.RED ? RED_NECTAR : BLUE_NECTAR;
    }
}
