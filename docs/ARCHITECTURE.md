# ARCHITECTURE

How the IronLion30311-HiveMind code is organized, and the rules every file follows.
Read `docs/ROBOT_PROFILE.md` first: the code must match the real robot described there.

## Versions

| Part | Version | Where it is set |
|---|---|---|
| FTC SDK (FtcRobotController) | v12.0 (official release, 2026-09-12) | `build.dependencies.gradle` |
| Pedro Pathing | `com.pedropathing:revhub:3.0.2` | `build.dependencies.gradle` |
| Pedro AutoTune | `com.pedropathing:tuning:1.0.3` | `build.dependencies.gradle` |
| FTC Dashboard | `com.acmerobotics.dashboard:dashboard:0.6.0` | `build.dependencies.gradle` |
| JUnit | 4.13.2 (tests only) | `TeamCode/build.gradle` |
| Gradle | 9.1.0 (wrapper from the SDK) | `gradle/wrapper/gradle-wrapper.properties` |
| Java language level | 8 (set by the SDK, `build.common.gradle`) | do not use `var`, records, or switch expressions |

The repository history starts from the official `v12.0` tag, so a future SDK release can be
merged with `git fetch upstream --tags && git merge v12.x` (see `docs/BRANCHING.md`).

## Layers

```
OpModes (opmodes/)            read gamepads, pick a routine, call subsystems, show telemetry
   |
State machines (statemachine/) chain mechanisms: intake -> index -> launch, never blocking
   |
Subsystems (subsystems/)      one class per mechanism, all robot logic lives here
   |
RobotHardware (hardware/)     the ONLY class that touches hardwareMap
   |
Constants (config/)           every name and number, tunable live from FTC Dashboard
```

`util/` holds hardware-free helpers that any layer may use, and `pathing/` holds the Pedro
Pathing follower setup and paths that the autonomous OpModes use.

## Package map

```
TeamCode/src/main/java/org/firstinspires/ftc/teamcode/
├── config/
│   ├── RobotConstants.java     Driver Station names (single source of truth), size, battery, lights
│   ├── DriveConstants.java     drive directions, ramps, speeds, odometry-driver gains
│   ├── IntakeConstants.java    intake direction, power, anti-jam shake
│   ├── IndexerConstants.java   feed power, jam detection
│   ├── LauncherConstants.java  RPM targets, velocity PIDF, voltage-compensated feedforward
│   ├── VisionConstants.java    Limelight (HIVE tags) and HuskyLens (ball color)
│   ├── FieldConstants.java     BIOBUZZ field poses for RED (BLUE is converted)
│   └── MatchConstants.java     match timing and rule limits (G407, G410)
├── hardware/       RobotHardware
├── subsystems/     Subsystem (interface), Drivetrain, Intake, Indexer, Launcher,
│                   ColorSensing, Vision, Lights, BatteryMonitor
├── pathing/        PedroConstants, paths/, tuning/
├── statemachine/   non-blocking sequences
├── opmodes/        teleop/, auto/, tuning/
└── util/           Alliance, MatchTimer, GamepadEx, PidfController, SlewRateLimiter,
                    PoseStorage, DataLogger
TeamCode/src/test/java/...   JUnit tests (run on the computer: ./gradlew testDebugUnitTest)
```

**Differences from the original plan (and why):**
- Constants are split into one class per mechanism (`IntakeConstants`, `IndexerConstants`,
  `VisionConstants`) instead of one big `RobotConstants`, so each file stays short.
- `MatchConstants` was added so rule limits (4 elements, last 60 s for NECTAR) are in one place.
- There is no `Placer` yet: the flower arm on `feature/renata/flower-arm` does not compile, and
  the team has not confirmed it is on the robot (decision D14). It will be added as `FlowerArm`.
- `ColorSensing` uses the HuskyLens, because the robot has no REV color sensor.

## Rules

1. **OpModes only orchestrate.** They read gamepads and call subsystem methods. No motor math.
2. **No blocking.** Zero `sleep()` in TeleOp. Use `ElapsedTime` and state machines.
3. **One name, one place.** Driver Station names live only in `RobotConstants`. One concept has
   one name everywhere (it is "Launcher" in code, telemetry, and docs).
4. **No magic numbers.** Every number goes in `config/`. Untested values say `// TODO: TUNE`.
5. **Fault tolerant.** A missing device gives a telemetry warning, never a crash.
6. **RED first.** Autonomous poses are written for RED; `util.Alliance` converts them for BLUE.
7. **Bulk reads.** `RobotHardware` puts every hub in bulk-caching mode so each loop reads all
   sensors in one transaction.
8. **Every file says where it came from.** The first lines list the source repo, branch, path and
   commit (`// Sources: ...`).
9. **Naming** follows `docs/NAMING.md`.

## Main loop shape (TeleOp)

```java
@Override
public void loop() {
    hardware.clearBulkCache();          // one read of all sensors
    readGamepads();                     // driver intent -> subsystem calls
    for (Subsystem s : subsystems) s.periodic();
    for (Subsystem s : subsystems) s.addTelemetry(telemetry);
    telemetry.update();
}
```
