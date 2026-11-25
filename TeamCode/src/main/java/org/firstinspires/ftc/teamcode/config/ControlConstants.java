// Sources: new for IronLion30311-HiveMind (gamepad thresholds used by util.GamepadEx)
package org.firstinspires.ftc.teamcode.config;

import com.acmerobotics.dashboard.config.Config;

/** Gamepad feel: when a trigger counts as pressed, and how long rumbles last. */
@Config
public final class ControlConstants {

    private ControlConstants() {
    }

    /** A trigger pulled past this (0..1) counts as a pressed button. TODO: TUNE */
    public static double TRIGGER_PRESS_THRESHOLD = 0.5;
    /** Length of a short rumble, in milliseconds. TODO: TUNE */
    public static int SHORT_RUMBLE_MILLIS = 200;
    /** Length of a long rumble (for example the endgame warning), in milliseconds. TODO: TUNE */
    public static int LONG_RUMBLE_MILLIS = 600;
}
