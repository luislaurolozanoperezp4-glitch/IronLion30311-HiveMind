# MIGRATION

What came from where, and every rename, when the season-start code was rebuilt as
IronLion30311-HiveMind. Built from the `// Sources:` header of every Java file, the agent
hand-offs in `docs/agents/`, and `docs/BEST_OF_MATRIX.md`.

## 1. Source repositories

| Repository | Use | Access |
|---|---|---|
| `ironlionrobotics/IronLion30311BIOBUZZInicioDeTemporada` (all branches) | **Main source.** Mechanisms, vision, TeleOp, tested autos, routes. Read-only. | Team repo |
| `pedro-pathing-biobuzz-analysis-a65cc9` | A worktree of the same repo at `feature/autonomos-beta` @ `206fa84`. It adds only an uncommitted `@Disabled` on `ShootTurnCollectAuto`. Every line in it is also in the main repo, so nothing was taken from it directly. | Local copy |
| `FIRST-Tech-Challenge/FtcRobotController` tag `v12.0` | The SDK. HiveMind's history starts from this tag. | Public |
| `Pedro-Pathing/Quickstart` (BSD-3-Clause-Clear) | `MecanumTuner`, `PinpointTuner`, `ForesightTuner`, `Tests` are vendored (only the package changed), through the copies on `vision-align`. | Public |
| `Pedro-Pathing/Visualizer` (Apache-2.0) | Field image `TeamCode/src/main/assets/images/biobuzz.webp`. | Public |
| `FTC22526FondyFireTech/Biobuzz_PedroPathing_V3` (BSD-3-Clause-Clear) | **Idea only, no code copied:** "if the HIVE has not changed by a deadline, go straight to park" (credited in `pathing/AutoPlanner`). Also supported the 180° alliance rotation (D13). | Public |

### Branch / commit shorthand

| Short | Branch @ commit (date) | What it was |
|---|---|---|
| `VA` | `feature/turrubiates/vision-align` @ `a7b8f4c` (2026-11-24) | Field-tested robot code. **Base (decision D18).** |
| `MASTER` | `master` @ `37b7a16` (2026-11-22) | Merge of `feature/autonomos-beta`; Pedro autos checked in a simulator only |
| `BETA` | `feature/autonomos-beta` @ `206fa84` | Pedro routes (Tick, Center Lane, Outer Lane, Park, Shoot Turn Collect) |
| `NCAM` | `feature/mar/duopodriveNcam` @ `2d92fba` (2026-11-21) | Field map, saved pose, alternative tag spacing |
| `ALP` | `feature/turrubiates/auto-leave-park` @ `e455142` | `LeaveParkAuto`, `GuardedStraightDrive` |
| `FLOWER` | `feature/renata/flower-arm` @ `889102d` | `FlowerArm` (did not compile) |
| `ALIGN` | `feature/renata/auto-align` @ `3e733d4` | Launcher idle pre-spin idea |
| `TA` | `feature/theautonomous` @ `125df22` | `MatchClock`, `TimeModel`, `RouteRunner` (ideas) |
| `LUISAUTO` | `feature/luis/autonomospaths` @ `9cf2c66` | `Pid` (ideas) |

## 2. Where every class came from

Paths in the "From" column are under the old `TeamCode/src/main/java/org/firstinspires/ftc/teamcode/`.
"New" = written for HiveMind (no legacy code).

### config/

| New class | From (ref / file: what) |
|---|---|
| `RobotConstants` | VA `RobotHardware.java` (device + legacy names), `subsystems/BatteryMonitor.java` (battery limits), `subsystems/IndicatorLight.java` (light colors); MASTER `RobotHardware.java` (`pinpoint`, legacy `indexer`, green light) |
| `DriveConstants` | VA `subsystems/MecanumDrive.java` (ramps, speeds), `opmodes/auto/PedroAutoBase.java` (odometry driver, pose guards), `opmodes/auto/SafeMoveAuto.java` (ticks/cm); MASTER `Constants.java` (motor ticks, wheel diameter) |
| `IntakeConstants` | VA `subsystems/Intake.java` (power, ramp), `RobotHardware.java` (direction, commit 927e4df), `PedroAutoBase.java` (shake, commit 7004eba) |
| `IndexerConstants` | VA `subsystems/Indexer.java` (feed power, jam detection, commit 9c03b6c) |
| `LauncherConstants` | VA `subsystems/Shooter.java` (RPM, PIDF, voltage compensation), `PreloadRouteAuto.java` (auto RPM, preload); MASTER `Constants.java` (spin-up timeout) |
| `FlowerArmConstants` | FLOWER `subsystems/FlowerArm.java` (positions), `RobotHardware.java` (servo directions) |
| `VisionConstants` | VA `subsystems/vision/HiveVision.java`, `HiveGeometry.java`, `HiveAlignController.java`, `BallVision.java` (HuskyLens IDs); NCAM (alternative tag spacing) |
| `ControlConstants` | New (gamepad thresholds); VA `opmodes/teleop/MainTeleOp.java` (brake, jam, battery rumbles) |
| `AutoConstants` | VA `PreloadRouteAuto.java` (leg distances, time limits), `PedroAutoBase.java` (path timeout); TA `TimeModel.java` (idea); ALP `LeaveParkAuto.java` (encoder LEAVE) |
| `FieldConstants` | NCAM field map (HIVE box, tiles); VA `PreloadRouteAuto.java` (near start, TESTED); BETA `ParkAuto.java` (far start); `docs/AUDIT_PEDRO.md` §5.1 (zones, FLOWERS) |
| `MatchConstants` | New (Competition Manual TU10, `docs/RULES_COMPLIANCE.md`) |
| `TuningConstants` | VA `opmodes/teleop/DriveWheelTest.java` (test power), `opmodes/calibration/HiveStateProbe.java` (drive scale, no-tag interval) |

### hardware/, util/

| New class | From |
|---|---|
| `hardware/RobotHardware` | VA `RobotHardware.java` (constructor, `getDevice` with legacy names, motor setup, optional cameras); MASTER `RobotHardware.java` (pinpoint lookup); FLOWER `RobotHardware.java` (flower arm servos). Changed: missing devices give warnings, MANUAL bulk caching. |
| `util/Alliance` | VA `subsystems/vision/Alliance.java` (enum), `subsystems/pathing/AlliancePoses.java` (pose idea). Changed: 180° rotation, not a mirror (D13). |
| `util/MatchTimer` | VA `PedroAutoBase.autoSeconds()`; TA `MatchClock.java` (idea) |
| `util/GamepadEx` | VA `Shooter.handleRpmTuning` (edge detection), `MainTeleOp.shapeStickInput` (deadzone + cubic) |
| `util/PidfController` | LUISAUTO `subsystems/autosubsystems/Pid.java` (ideas: derivative on measurement, anti-windup, dt clamp); VA `HiveAlignController.computeAxisOutput` (PD + static feedforward) |
| `util/SlewRateLimiter` | VA `MecanumDrive.applyRamp`, `Intake.applyRamp`, `Indexer.applyRamp` (three copies → one) |
| `util/PoseStorage` | NCAM `PinpointOdometry.saveCurrentPose` (idea); plus the G407 element-count hand-off (new) |
| `util/DataLogger` | VA `subsystems/LogFiles.java` (`tagPart`), `subsystems/ShooterLogger.java` (timestamped CSV, buffered writer, flush) |

### subsystems/

| New class | From (methods) |
|---|---|
| `Subsystem` | New (common shape of the VA subsystems) |
| `Drivetrain` | VA `subsystems/MecanumDrive.java` (`drive`, `driveRamped`, `computeTargets`, `applyRamp`, `setPowerLimit`, `setBrake`, slow/fast, `getWheelPositions`, `resetEncoders`); MASTER `MecanumDrive.java` (`stop`, `getAverageForwardCm`). New: field-centric. |
| `Intake` | VA `subsystems/Intake.java` (states, ramp, `stop`, `getState`, `getPower`); VA `PedroAutoBase.java` (intake shake). New: G407 gate, G408 stop. |
| `Indexer` | VA `subsystems/Indexer.java` (states, pause, `checkForJam`, ramp, counters) |
| `Launcher` | VA `subsystems/Shooter.java` (fire/reverse/stop, `updateReady`, `compensateForVoltage`, `applyPidf`, RPM conversion, `clampRpm`, `setTargetRpmP`); MASTER `Shooter.java` (`setTargetRpm(p, n)`, `atTargetRPM`); ALIGN `Shooter.java` (idle pre-spin idea, off) |
| `ShotDetector` | VA `subsystems/ShooterLogger.java` inner class `ShotTracker` |
| `ColorSensing` | VA `subsystems/vision/BallVision.java` (HuskyLens read, `Ball`, `getClosestBall`, `getAssistTurn`, `colorOf`), `BallIntakeCounter.java`, `subsystems/NectarAlertLight.java` (closest NECTAR check) |
| `BallCounter` | VA `subsystems/vision/BallIntakeCounter.java` (counting logic) |
| `BallColor` | VA `BallVision.BallColor` enum |
| `ElementCounter` | New (G407) |
| `FlowerArm` | FLOWER `subsystems/FlowerArm.java` (`deploy`, `retract`, `toggle`, `isDeployed`). Fixed: the constructor was named `DualServo`. New: optional servos, G410 gate. |
| `Vision` | VA `subsystems/vision/HiveVision.java` (queries); VA `PedroAutoBase.java` (TIP check `sinceScoringCellSeen` / `hasSeenScoringCell` → `isCellDown`) |
| `Lights` | VA `subsystems/IndicatorLight.java` (colors, blink); MASTER `IndicatorLight.java` (`setGreen`); VA `subsystems/NectarAlertLight.java` |
| `BatteryMonitor` | VA `subsystems/BatteryMonitor.java` (whole class) |
| `vision/HiveVision` | VA `subsystems/vision/HiveVision.java` (whole class; `isCellInView` from `PedroAutoBase`) |
| `vision/HiveAligner` | VA `subsystems/vision/HiveAlignController.java` (`reset`, `update`, `alignTo`, `evaluate`, `computeAxisOutput`) |
| `vision/HiveCell` | VA `subsystems/vision/HiveCell.java` (enum, tag IDs, `indexOfTag`); VA `vision/Alliance.java` (`getScoringCell`/`getAudienceCell` → static helpers) |
| `vision/HiveGeometry`, `HiveOrientation`, `HiveStateTracker`, `HiveClusterPose` | VA same files (whole classes) |

### statemachine/

| New class | From |
|---|---|
| `StateMachine` | New (pattern of the VA `Indexer` state enum + timers) |
| `ShootingSequence` | VA `PedroAutoBase.shootPollenPreload` (feed lock, no-shot end, shake) + `applyMechanisms SHOOT_POLLEN`; MASTER `opmodes/base/PedroAutoBase.shootWhenReady` (idea) |
| `CollectSequence` | VA `PedroAutoBase.collectBalls` |
| `TipWatch` | VA `PedroAutoBase.watchForTip`, `hasSeenScoringCell` / `sinceScoringCellSeen`; VA `PreloadRouteAuto` early TIP watch (commits c1c59db, a7b8f4c) |

### pathing/

| New class | From |
|---|---|
| `PedroConstants` | VA `subsystems/pathing/PedroFollowerFactory.java` (`DRIVETRAIN_CONFIG`, `LOCALIZER_CONFIG`, `FOLLOWER_CONFIG`, `create`) |
| `OdometryDriver` | VA `PedroAutoBase.java` (`driveTo`, `driveThrough`, `driveLeg`, `turnTo`, `settle`, `holdTarget`, `holdStep`, `checkOdometry`, `startPoseErrorIn`, `startHeadingErrorDeg`, `driveRobotCentric`) |
| `AutoPlanner` | VA `PreloadRouteAuto.runCollectRoutine` time guards, `shootTimeLeft`; TA `MatchClock`, `TimeModel` (idea); FTC 22526 (idea only) |
| `FieldDrawing` | VA `subsystems/pathing/FieldDrawing.java` (+ `drawPath`) |
| `PoseLogger` | VA `subsystems/pathing/PoseLogger.java` (pose columns only) |
| `paths/NearRoutePoses` | VA `PreloadRouteAuto.buildPoses`, `relative` |
| `paths/PreloadParkPaths` | VA `PreloadRouteAuto.runRoutine` park legs |
| `paths/PreloadCollectPaths` | VA `PreloadRouteAuto.runCollectRoutine`, `returnAndCheckTip`, `parkFromShootSide`; `PreloadCollectAuto.isCollecting` |
| `paths/OtherCellPaths` | VA `PreloadRouteAuto` TIP branches, `shootAtOtherCellAndPark` |
| `paths/FarCyclePaths` | VA `PreloadRouteAuto` waypoints; BETA `ParkAuto` far start; `BEST_OF_MATRIX` §4 row 23 (new route) |
| `paths/SafeParkPaths` | BETA `opmodes/auto/ParkAuto.java` (`buildPoses`, `downWall`) |
| `paths/OuterLanePaths` | BETA `OuterLaneAuto.java`; BETA `opmodes/base/PedroAutoBase.java` (preload heading −95°) |
| `paths/TickPaths` | BETA `TickAuto.java`, `Constants.Tick` |
| `paths/CenterLanePaths` | BETA `CenterLaneAuto.java`, `Constants.CenterLane` |
| `paths/ShootCollectPaths` | BETA `ShootTurnCollectAuto.java`, `Constants.ShootTurnCollect` |
| `paths/TestSquarePaths` | VA `OdometrySquareTestAuto.java` |
| `paths/RoutePaths` | BETA `TickAuto` (`Paths.line` usage); VA `AlliancePoses` (one set of poses) |
| `paths/PathCatalog` | New (`docs/AUDIT_PEDRO.md` §7, `BEST_OF_MATRIX` §4) |
| `tuning/Tuning` | VA `subsystems/pathing/tuning/Tuning.java` |
| `tuning/MecanumTuner`, `PinpointTuner`, `ForesightTuner`, `Tests` | VA copies of the Pedro Pathing Quickstart (whole files, only the package changed) |
| `tuning/TestPedroPose` | VA `opmodes/calibration/PedroPoseCheck.java` |
| `tuning/TestOdometrySquare` | VA `OdometrySquareTestAuto.java` |
| `tuning/TestOdometryDriver` | VA `PreloadRouteAuto.runRoutine` park legs (no shooting) |

### opmodes/

| New class | From |
|---|---|
| `auto/AutoBase` | VA `PedroAutoBase.java` (INIT alliance, A = re-zero, start-pose check, logs, `stopAll`, helpers, `applyMechanisms`); ALP `LeaveParkAuto` (start delay) |
| `auto/ScoringAuto` | VA `PreloadRouteAuto` (`runRoutine`, `runCollectRoutine`, `returnAndCheckTip`, `parkFromShootSide`, `shootAtOtherCellAndPark`, `relativeOther`, `shootTimeLeft`); `BEST_OF_MATRIX` §5 |
| `auto/AutoRedNear`, `AutoBlueNear`, `AutoRedFar`, `AutoBlueFar` | VA `PreloadRouteAuto` (`@Autonomous "Preload + Park (Odometry)"`) |
| `auto/AutoSafePark` | VA `PreloadRouteAuto` (LEAVE + park legs); BETA `ParkAuto`; ALP `LeaveParkAuto` |
| `auto/AutoSafeParkEncoders` | ALP `LeaveParkAuto` (start moved out of the zone for G304.E) |
| `auto/AutoSelector` | VA `PedroAutoBase` INIT buttons (B red, X blue); ALP D-pad start delay |
| `auto/AutoStep` | VA `PedroAutoBase` blocking helpers → one non-blocking step each |
| `auto/StepSequence` | VA `PreloadRouteAuto.runRoutine`; TA `RouteRunner.java` (idea) |
| `auto/EmergencyPark` | VA `PreloadRouteAuto` west-lane park legs, `collectExit` |
| `auto/GuardedLeaveMove` | ALP `subsystems/GuardedStraightDrive.java` (`driveStraightCm`, `meanAbsoluteWheelTravelCm`) |
| `auto/StartPosition` | VA, BETA, ALP start poses |
| `auto/TerritoryGuard` | New (G402) |
| `teleop/TeleOpBase` | VA `opmodes/teleop/MainTeleOp.java` (button map, rumbles, ball assist, align-while-firing, feed when ready, lights, shot marks, telemetry); NCAM `PinpointOdometry` (saved pose idea) |
| `teleop/TeleOpRed`, `TeleOpBlue` | VA `MainTeleOp` (INIT alliance choice → one OpMode per alliance) |
| `teleop/TeleOpLogic` | VA `MainTeleOp` (`isDriverMoving`, light choice) |
| `teleop/EndgameAlerts` | New |
| `tuning/TestDriveWheels` | VA `opmodes/teleop/DriveWheelTest.java` |
| `tuning/TestIndexerIntake` | New (VA `Indexer` jam values) |
| `tuning/TuneColorSensor` | VA `MainTeleOp.addBallTelemetry` |
| `tuning/TuneHiveVision` | VA `opmodes/calibration/HiveStateProbe.java` |
| `tuning/TuneLauncherPidf` | VA `MainTeleOp` (`Graph/*` telemetry), `ShooterLogger` (CSV columns) |

### Tests

Ported from VA `src/test` @ `a7b8f4c`: `HiveGeometryTest`, `HiveOrientationTest`,
`HiveStateTrackerTest` (adapted for `util.Alliance`). Every other test is new.

## 3. Renames (old → new)

### 3.1 Classes

| Old (ref) | New |
|---|---|
| `MecanumDrive` (VA) | `subsystems/Drivetrain` |
| `Shooter` (VA, MASTER) | `subsystems/Launcher` |
| `ShooterLogger.ShotTracker` (VA) | `subsystems/ShotDetector` |
| `BallVision` (VA) | `subsystems/ColorSensing` |
| `BallIntakeCounter` (VA) | `subsystems/BallCounter` (used inside `ColorSensing`) |
| `BallVision.BallColor` (VA) | `subsystems/BallColor` |
| `IndicatorLight` + `NectarAlertLight` (VA) | `subsystems/Lights` |
| `HiveAlignController` (VA) | `subsystems/vision/HiveAligner` |
| `vision/Alliance` (VA), `pathing/Alliance` (MASTER) | `util/Alliance` |
| `LogFiles` (VA) | `util/DataLogger` |
| `PedroFollowerFactory` (VA) | `pathing/PedroConstants.createFollower` |
| `PedroAutoBase` (VA, 1126 lines) | `opmodes/auto/AutoBase` + `pathing/OdometryDriver` + `statemachine/*` + `util/MatchTimer` |
| `PedroAutoBase.shootPollenPreload / collectBalls / watchForTip` (VA) | `ShootingSequence`, `CollectSequence`, `TipWatch` |
| `GuardedStraightDrive` (ALP) | `opmodes/auto/GuardedLeaveMove` |
| `MainTeleOp` (VA) | `TeleOpBase` + `TeleOpRed` + `TeleOpBlue` |
| `DriveWheelTest` (VA) | `TestDriveWheels` |
| `HiveStateProbe` (VA) | `TuneHiveVision` |
| `PedroPoseCheck` (VA) | `TestPedroPose` |
| `OdometrySquareTestAuto` (VA) | `TestOdometrySquare` + `paths/TestSquarePaths` |
| `PreloadRouteAuto` (VA) | `ScoringAuto` + `AutoRed/BlueNear` + `paths/PreloadParkPaths` + `NearRoutePoses` |
| `PreloadCollectAuto` (VA) | `paths/PreloadCollectPaths` (+ `ScoringAuto` cycle) |
| `SafeMoveAuto` (VA), `LeaveParkAuto` (ALP) | `AutoSafeParkEncoders`, `AutoSafePark` |
| `ParkAuto` (BETA) | `paths/SafeParkPaths` (+ `AutoSafePark` FAR) |
| `TickAuto`, `CenterLaneAuto`, `OuterLaneAuto`, `ShootTurnCollectAuto` (BETA) | `paths/TickPaths`, `CenterLanePaths`, `OuterLanePaths`, `ShootCollectPaths` (path data only, no OpMode) |
| `BiobuzzFieldMap` (NCAM), `Constants` (MASTER) | `config/FieldConstants`, `config/AutoConstants` |

### 3.2 Methods

| Old | New |
|---|---|
| `Shooter.fireP / reverseP / stopP` (VA) | `Launcher.spinUpPollen / reversePollen / stopPollen` |
| `Shooter.fireN / reverseN / stopN` (VA) | `Launcher.spinUpNectar / reverseNectar / stopNectar` |
| `Shooter.setTargetRpmP` (VA) | `Launcher.setPollenTargetRpm` |
| `Shooter.setTargetRpm(rpmP, rpmN)` (MASTER) | `Launcher.setTargetRpm(pollenRpm, nectarRpm)` |
| `getTargetRpmP/N`, `getCurrentRpmP/N`, `getStateP/N` (VA) | `getPollenTargetRpm / getNectarTargetRpm`, `getPollenRpm / getNectarRpm`, `getPollenState / getNectarState` |
| `atTargetRPM()` (MASTER) | `isAtTargetRpm()` |
| `Shooter.handleRpmTuning(4 booleans)` (VA) | `adjustPollenRpm(delta)`, `adjustNectarRpm(delta)` (+ `GamepadEx` edges) |
| `Shooter.update(8 booleans)`, `Intake.update(c, e)`, `Indexer.update(f, e, p)` (VA) | Command methods (`spinUpPollen()`, `collect()`, `feed()`, …) + `periodic()` |
| `BallIntakeCounter.update / getCount / getTotal / summary` (VA) | `ColorSensing.countWhileIntaking / getBallCount / getTotalBalls / getSummary` |
| `vision.Alliance.getScoringCell() / getAudienceCell()` (VA) | `HiveCell.getBackCell(alliance) / getAudienceCell(alliance)` |
| `PedroAutoBase.driveTo / driveThrough / turnTo / holdTarget` (VA) | `OdometryDriver.driveTo / driveThrough / turnTo / hold` |
| `PedroAutoBase.checkOdometry` (VA) | `OdometryDriver.checkStartPose`, `armPoseJumpGuard` |
| `autoSeconds()` (VA) | `MatchTimer.getElapsedSeconds()` |
| `MainTeleOp.shapeStickInput` (VA) | `GamepadEx.getShapedLeftY / getShapedLeftX / getShapedRightX` |
| `PinpointOdometry.saveCurrentPose` (NCAM) | `PoseStorage.save(pose, alliance)` |
| `GuardedStraightDrive.driveStraightCm` (ALP) | `GuardedLeaveMove.start(...)` + `periodic()` |
| `Intake.setRejectDetector`, `getRejectCount` (HiveMind, before the G408 decision) | `setOpponentNectarDetector`, `getOpponentNectarStopCount` |

### 3.3 Enum values and constants

| Old | New | Why |
|---|---|---|
| `HiveCell.RED_SCORING` / `BLUE_SCORING` (VA) | `RED_BACK` / `BLUE_BACK` | Manual TU10 §9.9 terms (D20) |
| `FieldConstants.RED_NORTH_CELL` / `RED_SOUTH_CELL` (early HiveMind) | `RED_BACK_CELL` / `RED_AUDIENCE_CELL` | D20 |
| `HiveAlignController.ENABLED` + gains (VA) | `VisionConstants.HIVE_ALIGN_ENABLED`, `ALIGN_*` | Every number in `config/` |
| `HiveGeometry.CAMERA_PITCH_DEG`, `DEFAULT_TAG_SPACING_METERS` (VA) | `VisionConstants.CAMERA_PITCH_DEGREES`, `TAG_SPACING_METERS` | Same |
| `IntakeConstants.REJECT_OPPONENT_NECTAR`, `REJECT_EJECT_SECONDS` | `STOP_FOR_OPPONENT_NECTAR`, `OPPONENT_NECTAR_HOLD_SECONDS` (eject removed) | G408 decision |
| Intake state `REJECTING` | `OPPONENT_NECTAR` | G408 decision |
| `PATH_TIMEOUT_SEC` (VA) | `AutoConstants.PATH_TIMEOUT_SECONDS` | Units spelled out |

### 3.4 OpModes (Driver Station list)

| Old DS name (ref) | New DS name | Group |
|---|---|---|
| `Main TeleOp` (VA) | `TeleOp Red`, `TeleOp Blue` | Competition |
| `Preload + Park (Odometry)`, `Preload + Collect + Park (Odometry)` (VA) | `Auto Red Near`, `Auto Blue Near` (preload, then cycle-or-park decided by time) | Competition |
| — (new FAR route) | `Auto Red Far`, `Auto Blue Far` | Competition (untested) |
| `Leave + Park` (ALP), `Park` (BETA) | `Auto Safe Park` | Competition |
| `Safe Move (Encoders)` (VA) | `Auto Safe Park Encoders` | Competition |
| `Drive Wheel Test` (VA) | `Test Drive Wheels` | Tuning |
| `Hive State Probe` (VA) | `Tune Hive Vision` | Tuning |
| `Pedro Pose Check` (VA) | `Test Pedro Pose` | Tuning |
| `Odometry Square Test` (VA) | `Test Odometry Square` | Tuning |
| — | `Test Odometry Driver`, `Test Indexer Intake`, `Tune Color Sensor`, `Tune Launcher PIDF` | Tuning (new) |
| `Tick`, `Center Lane`, `Outer Lane`, `Shoot Turn Collect` (BETA) | not OpModes any more (path classes kept as data) | — |

### 3.5 Driver Station hardware names

The DS configuration must use the **"Name now"** column. `RobotHardware` still accepts the
legacy names as a fallback (Pedro Pathing does not). See also `README.md` → Hardware configuration.

| Old name(s) | Name now (`RobotConstants`) | Code field |
|---|---|---|
| `FL`, `FR`, `BL`, `BR` | `frontLeftDrive`, `frontRightDrive`, `backLeftDrive`, `backRightDrive` | `frontLeftDrive` … |
| `intake` | `intakeMotor` | `intakeMotor` |
| `alejandro`, `indexer` | `indexerMotor` | `indexerMotor` |
| `P`, `shooterP`, `shooterPollen` | `pollenShooterMotor` (DS name kept, D15) | `pollenLauncherMotor` |
| `N`, `shooterN`, `shooterNectar` | `nectarShooterMotor` (DS name kept, D15) | `nectarLauncherMotor` |
| `indicator` | `indicatorLight` | `indicatorLight` |
| — | `nectarAlertLight` (optional) | `nectarAlertLight` |
| `servoLeft`, `servoRight` | `flowerArmLeftServo`, `flowerArmRightServo` (optional) | `flowerArmLeftServo`, `flowerArmRightServo` |
| `limelight`, `huskyLens`, `pinpoint` | unchanged | `limelight`, `huskyLens` (pinpoint is owned by Pedro) |
| `pera`, `stopServo`, `shooterGateServo` | removed (gate removed from the robot 2026-11-20) | — |
| `imu 1` | not used by the code | — |
| `lift`, `pollen2`, `nectar2`, `pollenFeeder`, `nectarFeeder` | never existed on the robot (LUISAUTO) | — |

## 4. What changed in behavior, and why

| Change | Why |
|---|---|
| Blocking `while` loops with `sleep` → non-blocking `start()/periodic()/isDone()` | The robot keeps reading sensors and the STOP button always works (ARCHITECTURE rule 2) |
| One OpMode per alliance (TeleOp and the scoring autos) | The alliance can no longer be forgotten in INIT; AUTO preselects the matching TeleOp |
| BLUE = RED rotated 180°, never mirrored | The field is rotationally symmetric (D13, manual TU10) |
| Bulk caching MANUAL (VA used AUTO) | One sensor read per loop; every OpMode calls `clearBulkCache()` |
| Intake stops for opponent NECTAR instead of ejecting it | G408 (team decision): pushing it away is CONTROL too |
| G407 element counter gates the intake; count handed from AUTO to TeleOp | Never control more than 4 elements |
| Flower arm locked until the last 60 s (1 s margin) | G410 |
| Every park ends 2 in off the wall (`RIGHT_2_CM` 60 → 56.5) | LEAVE must still count (team decision). **Needs a robot re-test.** |
| Safe Park "loading zone" start moved behind the zone tape | G304.E forbids starting inside the LOADING ZONE |
| Time-aware AUTO (`AutoPlanner`) + `EmergencyPark` | Always park before 30 s |
| TERRITORY guard in AUTO | G402 (team policy: never enter the opponent half in AUTO) |
| Constants in `config/` with FTC Dashboard `@Config` | No magic numbers; tune live |
| Ramps share one `SlewRateLimiter` | A direction change now passes through 0 for one loop (tiny change vs VA) |
| `Intake.pushToIndexer()` added | While shooting the intake must push even when the robot holds 4 |

## 5. Not migrated

See `docs/BEST_OF_MATRIX.md` §7 for the full list and reasons. In short: the speculative
hardware of `feature/luis/autonomospaths` and `feature/theautonomous` (lift, feeder servos,
`pollen2`/`nectar2`), Mar's second Pinpoint owner (`PinpointOdometry`, `AutoDrive`), the gate
servos (`Pera`, `StopServo`), the Start Routine / old Center Lane / Top Wall routes, the
`WPILibAuto` folder, and the VA `MotorDiagnosticsLogger`, `ShooterLogger` and `MatchSummary`
files (`DataLogger` is ready if they are needed again). NCAM's odometry-assisted aim
(`syncOdometry`, `getAimErrorDeg`) was not ported.
