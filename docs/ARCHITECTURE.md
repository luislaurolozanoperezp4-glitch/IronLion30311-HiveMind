# ARCHITECTURE

How the IronLion30311-HiveMind code is organized, and the rules every file follows.
Read `docs/ROBOT_PROFILE.md` first: the code must match the real robot described there.
For a class-by-class tour in reading order, see `docs/CODE_GUIDE.md`.

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

`util/` holds hardware-free helpers that any layer may use. `pathing/` holds the Pedro Pathing
setup, the odometry driver, the time planner and the routes that the autonomous OpModes use.
A layer may call the layers **below** it, never the ones above (a subsystem never calls an OpMode).

## Package map

Verified against the source tree on 2026-11-25 (96 main Java files plus 11 `package-info.java`, 35 test files).

```
TeamCode/src/main/java/org/firstinspires/ftc/teamcode/
├── config/        RobotConstants (Driver Station names, size, battery, light colors),
│                  DriveConstants, IntakeConstants, IndexerConstants, LauncherConstants,
│                  FlowerArmConstants, VisionConstants, ControlConstants (gamepad feel, TeleOp
│                  options), AutoConstants (route legs, time budget), FieldConstants (BIOBUZZ
│                  poses for RED), MatchConstants (match timing, rule limits), TuningConstants
├── hardware/      RobotHardware
├── subsystems/    Subsystem (interface), Drivetrain, Intake, Indexer, Launcher, ShotDetector,
│   │              FlowerArm, ColorSensing, BallCounter, BallColor, ElementCounter, Vision,
│   │              Lights, BatteryMonitor
│   └── vision/    HiveVision, HiveAligner, HiveCell, HiveGeometry, HiveOrientation,
│                  HiveStateTracker, HiveClusterPose (Limelight / HIVE AprilTags)
├── statemachine/  StateMachine, ShootingSequence, CollectSequence, TipWatch
├── pathing/       PedroConstants, OdometryDriver, AutoPlanner, FieldDrawing, PoseLogger
│   ├── paths/     RoutePaths (base), NearRoutePoses, PreloadParkPaths, PreloadCollectPaths,
│   │              OtherCellPaths, FarCyclePaths, SafeParkPaths, OuterLanePaths, TickPaths,
│   │              CenterLanePaths, ShootCollectPaths, TestSquarePaths, PathCatalog
│   └── tuning/    Tuning (AutoTune @Tuner), MecanumTuner, PinpointTuner, ForesightTuner,
│                  Tests (vendored Quickstart), TestPedroPose, TestOdometrySquare,
│                  TestOdometryDriver
├── opmodes/
│   ├── auto/      AutoBase, ScoringAuto, AutoRedNear, AutoBlueNear, AutoRedFar, AutoBlueFar,
│   │              AutoSafePark, AutoSafeParkEncoders, AutoSelector, StartPosition, AutoStep,
│   │              StepSequence, EmergencyPark, GuardedLeaveMove, TerritoryGuard
│   ├── teleop/    TeleOpBase, TeleOpRed, TeleOpBlue, TeleOpLogic, EndgameAlerts
│   └── tuning/    TestDriveWheels, TestIndexerIntake, TuneColorSensor, TuneHiveVision,
│                  TuneLauncherPidf
└── util/          Alliance, MatchTimer, GamepadEx, PidfController, SlewRateLimiter,
                   PoseStorage, DataLogger
TeamCode/src/main/assets/images/biobuzz.webp   field image for FTC Dashboard
TeamCode/src/test/java/...   JUnit tests, same packages + testutil/ (FakeClock, RuleGeometry)
```

**Differences from the original plan (and why):**
- Constants are split into one class per mechanism or topic instead of one big
  `RobotConstants`, so each file stays short.
- `MatchConstants` holds the rule limits (4 elements for G407, last 60 s for G410, match timing).
- The planned "Placer" became **`FlowerArm`**. It is the `feature/renata/flower-arm` class,
  fixed so it compiles. Its servos are optional (decision D14 is still open: is the arm on the
  robot?), and it can only deploy in the last 60 s (G410).
- `ColorSensing` uses the HuskyLens, because the robot has no REV color sensor.
- `BatteryMonitor` lives in `subsystems/` (it has a `periodic()`), not in `util/`.
- The Pedro follower factory is merged into `PedroConstants.createFollower(...)`.
- Competition autos drive with `pathing/OdometryDriver` (the field-tested point-to-point driver),
  not Pedro path following, until AutoTune has been run (decision D6).
- HIVE cells use the manual's names (`RED_BACK`, `RED_AUDIENCE`, …) since decision D20.

## Rules

1. **OpModes only orchestrate.** They read gamepads and call subsystem methods. No motor math.
2. **No blocking.** Zero `sleep()` in TeleOp, AUTO, subsystems or state machines. Use
   `ElapsedTime`/`MatchTimer` and state machines. (Only the vendored Pedro tuners may block.)
3. **One name, one place.** Driver Station names live only in `RobotConstants`. One concept has
   one name everywhere (it is "Launcher" in code, telemetry, and docs).
4. **No magic numbers.** Every number goes in `config/`. Untested values say `// TODO: TUNE`.
5. **Fault tolerant.** A missing device gives a telemetry warning, never a crash.
6. **RED first.** Autonomous poses are written for RED; `util.Alliance` rotates them 180° for
   BLUE (decision D13). Never mirror.
7. **Bulk reads.** `RobotHardware` puts every hub in MANUAL bulk-caching mode. Every OpMode
   must call `hardware.clearBulkCache()` once at the start of each loop.
8. **Every file says where it came from.** The first line is `// Sources: ...` (repo, branch,
   path, commit). CI checks it.
9. **Rules of the game are code.** G402, G407, G408 and G410 are enforced in software where
   possible and covered by tests (`docs/RULES_COMPLIANCE.md`).
10. **Naming** follows `docs/NAMING.md`.

## Main loop shape (TeleOp)

This is the real order in `TeleOpBase.loop()` (simplified):

```java
@Override
public void loop() {
    hardware.clearBulkCache();          // one read of all sensors
    driver.readButtons();               // GamepadEx edge detection
    operator.readButtons();
    if (localizer != null) localizer.update();   // Pinpoint pose (optional)
    handleMatchAlerts();                // endgame rumbles, low battery
    handleToggles();                    // tap buttons: modes, RPM, shot marks, flower arm
    handleDriveAndShooting();           // sticks, intake, launcher, auto-shoot
    colorSensing.countWhileIntaking(intake.isCollecting());
    lights.updateNectarAlert(closestNectarColor());
    for (Subsystem s : subsystems) s.periodic();
    writeMatchLog();
    addTelemetry();
    telemetry.update();
}
```

`AutoBase.loop()` has the same idea: clear the bulk cache, `follower.update()`, the G402 and
time guards, `driver.periodic()`, `StepSequence.periodic()`, every subsystem `periodic()`, then
the pose log and telemetry.
