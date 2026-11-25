// Sources: new for IronLion30311-HiveMind (shared test clock)
package org.firstinspires.ftc.teamcode.testutil;

import org.firstinspires.ftc.teamcode.util.MatchTimer;

/** A clock that tests move by hand, so time-based logic can be tested without waiting. */
public class FakeClock implements MatchTimer.Clock {

    /** Current time in seconds. */
    public double now = 100.0;

    @Override
    public double nowSeconds() {
        return now;
    }

    /** @param seconds time to move forward */
    public void advance(double seconds) {
        now += seconds;
    }
}
