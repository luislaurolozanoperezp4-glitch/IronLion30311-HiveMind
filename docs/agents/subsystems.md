# Subsystems agent hand-off

Branch `agent/subsystems` (from `origin/dev` @ 5047e36). Build: `./gradlew assembleDebug testDebugUnitTest`
passes. 84 JUnit tests, 0 failures. No OpModes and nothing under `pathing/` or `opmodes/` were touched.

Base source: `VA` = `feature/turrubiates/vision-align` @ a7b8f4c. Others: `MASTER` @ 37b7a16,
`FLOWER` = `feature/renata/flower-arm` @ 889102d, `ALIGN` @ 3e733d4, `LUISAUTO` @ 9cf2c66 (ideas only).
No code was taken from other teams' repos.

## Classes and sources

| New class | Source(s) |
|---|---|
| `hardware/RobotHardware` | VA `RobotHardware` + MASTER pinpoint lookup + FLOWER servos |
| `subsystems/Drivetrain` | VA `MecanumDrive` + MASTER `stop()`, `getAverageForwardCm()`; field-centric is new |
| `subsystems/Intake` | VA `Intake` + VA `PedroAutoBase` intake shake; G407 gate and G408 rejection are new |
| `subsystems/Indexer` | VA `Indexer` (jam/unjam state machine unchanged) |
| `subsystems/Launcher` | VA `Shooter` + MASTER `setTargetRpm`/`atTargetRPM`; ALIGN idle-RPM idea (off: `IDLE_RPM = 0`) |
| `subsystems/ShotDetector` | VA `ShooterLogger.ShotTracker` |
| `subsystems/ColorSensing`, `BallCounter`, `BallColor` | VA `BallVision`, `BallIntakeCounter`, `NectarAlertLight` check |
| `subsystems/ElementCounter` | new (G407) |
| `subsystems/Vision` | facade over VA `HiveVision`; `isCellDown` from VA `PedroAutoBase` TIP check |
| `subsystems/vision/HiveVision, HiveCell, HiveGeometry, HiveOrientation, HiveStateTracker, HiveClusterPose` | VA same names |
| `subsystems/vision/HiveAligner` | VA `HiveAlignController` (disabled by default) |
| `subsystems/Lights` | VA `IndicatorLight` + MASTER `setGreen` + VA `NectarAlertLight` |
| `subsystems/BatteryMonitor` | VA `BatteryMonitor` |
| `subsystems/FlowerArm` | FLOWER `FlowerArm` (constructor fixed so it compiles), optional servos, G410 gate |
| `statemachine/StateMachine` | new |
| `statemachine/ShootingSequence` | VA `PedroAutoBase.shootPollenPreload` + `applyMechanisms` |
| `statemachine/CollectSequence` | VA `PedroAutoBase.collectBalls` |
| `statemachine/TipWatch` | VA `PedroAutoBase.watchForTip` + `PreloadRouteAuto` early watch |
| `util/SlewRateLimiter` | VA `applyRamp` (drive, intake, indexer) |
| `util/PidfController` | LUISAUTO `Pid` ideas + VA `computeAxisOutput` |
| `util/GamepadEx` | VA `Shooter.handleRpmTuning` edge detection + `MainTeleOp.shapeStickInput` |
| `util/DataLogger` | VA `LogFiles` + `ShooterLogger` file handling |
| `config/FlowerArmConstants`, `config/ControlConstants` | new (FLOWER values, gamepad thresholds) |

Constants added (no tested value was changed): `RobotConstants` flower servo names + legacy
`servoLeft`/`servoRight`, battery read interval/min valid volts; `DriveConstants.RAMP_MAX_DT_SECONDS`;
`IntakeConstants` ramp dt, `REJECT_EJECT_SECONDS`, `REJECT_OPPONENT_NECTAR`, `COLLECT_*` (VA auto
collect values); `IndexerConstants.DIRECTION`, `CURRENT_CHECK_INTERVAL_SECONDS`, ramp dt;
`LauncherConstants.SHOT_DIP_RPM`, `SHOT_RECOVERED_RPM`, `SHOT_PAUSE_MIN_SECONDS`,
`SHOOT_NO_SHOT_END_SECONDS`; `VisionConstants` HuskyLens image size, `BALL_*` counter values,
`INTAKE_ZONE_Y_OFFSET`.

## Renames (for docs/MIGRATION.md)

| Old (VA unless noted) | New |
|---|---|
| `RobotHardware.frontLeft/...`, `intake`, `shooterP`, `shooterN` | `frontLeftDrive/...`, `intakeMotor`, `pollenLauncherMotor`, `nectarLauncherMotor` |
| FLOWER `servoLeft/servoRight` | `flowerArmLeftServo/flowerArmRightServo` |
| `MecanumDrive` | `Drivetrain` |
| `Shooter` | `Launcher` |
| `Shooter.fireP/reverseP/stopP` (N likewise) | `spinUpPollen/reversePollen/stopPollen` (`...Nectar`) |
| `Shooter.setTargetRpmP`, MASTER `setTargetRpm(p, n)` | `setPollenTargetRpm`, `setTargetRpm(pollen, nectar)` |
| `getTargetRpmP/N`, `getCurrentRpmP/N`, `getStateP/N` | `getPollenTargetRpm/...`, `getPollenRpm/getNectarRpm`, `getPollenState/getNectarState` |
| MASTER `atTargetRPM()` | `isAtTargetRpm()` |
| `Shooter.handleRpmTuning(...)` | `adjustPollenRpm(delta)`, `adjustNectarRpm(delta)` |
| `Shooter.update(8 booleans)`, `Intake.update(c, e)`, `Indexer.update(f, e, p)` | command methods + `periodic()` |
| `ShooterLogger.ShotTracker` | `ShotDetector` |
| `BallVision` | `ColorSensing` |
| `BallIntakeCounter.update/getCount/getTotal/summary` | `ColorSensing.countWhileIntaking/getBallCount/getTotalBalls/getSummary` (logic in `BallCounter`) |
| `BallVision.BallColor` | `subsystems.BallColor` |
| `HiveAlignController` | `vision.HiveAligner` |
| `HiveAlignController.ENABLED` etc. | `VisionConstants.HIVE_ALIGN_ENABLED`, `ALIGN_*` |
| `HiveGeometry.CAMERA_PITCH_DEG`, `DEFAULT_TAG_SPACING_METERS` | `VisionConstants.CAMERA_PITCH_DEGREES`, `TAG_SPACING_METERS` |
| `vision.Alliance.getScoringCell()/getAudienceCell()` | `HiveCell.getScoringCell(Alliance)/getAudienceCell(Alliance)` |
| `IndicatorLight` + `NectarAlertLight` | `Lights` |
| `PedroAutoBase.shootPollenPreload/collectBalls/watchForTip` | `ShootingSequence`, `CollectSequence`, `TipWatch` |
| `LogFiles` | `DataLogger` |

## Public API (code against this)

Every subsystem implements `Subsystem`: `periodic()`, `stop()`, `addTelemetry(Telemetry)`.
Every constructor takes `RobotHardware`; a second constructor (motors/servos + `MatchTimer.Clock`)
exists for tests. Missing devices never crash: the subsystem does nothing and says so in telemetry.

**Loop order (TeleOp):** `hardware.clearBulkCache()` -> read gamepads and call commands ->
sequences' `periodic()` -> `colorSensing.countWhileIntaking(intake.isCollecting())` -> every
subsystem `periodic()` (ColorSensing and Launcher before ElementCounter) -> `addTelemetry` ->
`telemetry.update()`. `hardware.addTelemetry(t)` shows missing-device warnings.

- **RobotHardware**: public device fields (nullable); `getDevice(Class, name, legacyNames...)`;
  `clearBulkCache()`; `getBatteryVolts()` (0 if no sensor); `getWarnings()`, `hasWarnings()`;
  `hasDrivetrain/Intake/Indexer/PollenLauncher/NectarLauncher/IndicatorLight/NectarAlertLight/
  FlowerArm/Limelight/HuskyLens/Pinpoint/BatterySensor()`.
- **Drivetrain**: `drive(fwd, strafe, turn)`, `driveRamped(...)`, `driveFieldCentric(...)`,
  `setHeadingSupplier(DoubleSupplier rad CCW)`, `resetFieldHeading()`, `setBatteryMonitor(m)`,
  `setPowerLimit(l)`, `setBrake(b)`, `setSlowMode()`, `setFastMode()`, `isBraking()`,
  `isSlowMode()`, `getWheelPositions()`, `getAverageForwardCm()`, `resetEncoders()`,
  `getWheelPowers()`, static `mix(...)`, `fieldToRobot(...)`. Signs: + forward, + right, + clockwise.
  Does nothing until a drive method is called (so it does not fight Pedro in AUTO).
- **Intake**: `collect()`, `pushToIndexer()`, `eject()`, `idle()`, `shake()`,
  `setCollectGate(BooleanSupplier)` (G407), `setRejectDetector(BooleanSupplier)` (G408),
  `getState()` (IDLE/COLLECTING/PUSHING/EJECTING/SHAKING/REJECTING/FULL), `isCollecting()`,
  `isShaking()`, `isFull()`, `getRejectCount()`, `getPower()`, `isConnected()`.
- **Indexer**: `feed()`, `eject()`, `idle()`, `setPaused(b)`, `getState()`, `isJammed()`,
  `isFeeding()`, `getTotalUnjams()`, `getTotalJamStops()`, `getPower()`, `isConnected()`.
  Release feed (`idle()`) to clear JAMMED.
- **Launcher**: `spinUpPollen/reversePollen/stopPollen()`, `...Nectar()`, `setTargetRpm(p, n)`,
  `setPollenTargetRpm`, `setNectarTargetRpm`, `adjustPollenRpm(d)`, `adjustNectarRpm(d)`,
  `isPollenReady()`, `isNectarReady()`, `isAtTargetRpm()`, `isFiring()`, `getPollenRpm()`,
  `getNectarRpm()`, `getPollen/NectarTargetRpm()`, `getPollen/NectarState()`,
  `getPollen/Nectar/TotalShotCount()`, `getPollen/NectarShotStats()`, `getAppliedKf()`.
  Default targets: `POLLEN_TELEOP_RPM` / `NECTAR_RPM`; AUTO sets `POLLEN_AUTO_RPM`.
- **ColorSensing**: `countWhileIntaking(b)`, `getBalls()`, `getClosestBall(colors...)`,
  `getClosestNectar()`, `isNectarAtIntake()`, `isOpponentNectarAtIntake(Alliance)`,
  `getAssistTurn(colors...)`, `getBallCount(color)`, `getTotalBalls()`, `getLastPicked()`,
  `getSummary()`, `resetCounts()`, `isConnected()`, `updateFromBalls(list)` (tests).
- **ElementCounter** `(ColorSensing, Launcher)`: `canCollectMore()`, `isFull()`, `getCount()`,
  `setCount(n)` (AUTO start: `PRELOAD_BALLS`), `recordCollected()`, `recordLaunched()`,
  `recordEjected()`, `getRefusedCount()`, `reset()`.
  Wire: `intake.setCollectGate(counter::canCollectMore)`,
  `intake.setRejectDetector(() -> colorSensing.isOpponentNectarAtIntake(alliance))`.
- **Vision** `(RobotHardware, Alliance)`: `setAlliance`, `getBestPose()`, `getPose(cell)`,
  `getAllianceTarget()`, `getUpwardCell()`, `getHiveState()`, `getTipCount()`,
  `isCellInView(cell)`, `hasSeenCell(cell)`, `getSecondsSinceSeen(cell)`,
  `isCellDown(cell, goneSec)`, `getVisibleTagIds()`, `getRawTagDistanceMeters()`,
  `getHiveVision()`, `isConnected()`.
- **HiveCell**: `RED_SCORING, RED_AUDIENCE, BLUE_AUDIENCE, BLUE_SCORING`;
  static `getScoringCell(Alliance)`, `getAudienceCell(Alliance)`, `fromTagId(id)`.
- **HiveAligner** `(vision.getHiveVision(), drivetrain)`: `isEnabled()`, `alignTo(cell)`,
  `evaluate(cell)` (no driving), `isAligned()`, `reset()`, `get{Forward,Strafe,Turn}Command()`.
- **Lights**: `setRed/Blue/Yellow/White/Green/Off()`, `blink(color)`, `showAlliance(a)`,
  `showLauncherState(isAligning, isReady)`, `updateNectarAlert(BallColor or null)`,
  `getNectarAlertColor()`, `hasNectarAlertLight()`.
- **BatteryMonitor**: `getVolts()`, `getStartVolts()`, `getMinVolts()`, `isLow()`,
  `hasBeenLow()`, `getSagCount()`, `getDrivePowerLimit()`.
- **FlowerArm**: `deploy()` (bool, refused when gate closed), `deployIfAllowed(MatchTimer)`,
  `setDeployGate(BooleanSupplier)` (use `matchTimer::canPlaceNectar`), `canDeploy()`,
  `retract()`, `toggle()`, `isDeployed()`, `isAvailable()`, `getRefusedCount()`.
- **ShootingSequence** `forRobot(launcher, indexer, intake)`: `start(balls, rpm, maxSec)`,
  `periodic()`, `isDone()`, `isRunning()`, `cancel()`, `getState()`, `getShotsFired()`,
  `getShakeCount()`, `getElapsedSec()`, `setAimedSupplier(BooleanSupplier)`. Stops intake,
  indexer and the POLLEN flywheel when done.
- **CollectSequence** `forRobot(colorSensing, intake, counter)`: `start(maxSec, colors...)`,
  `periodic()`, `isDone()`, `cancel()`, `getResult()` (NO_BALL_IN_VIEW/TIME_UP/FULL/CANCELLED),
  `getForwardCommand()`, `getTurnCommand()` (apply them yourself, e.g. `follower.manual`; note
  Pedro's strafe/turn signs are opposite), `getBallsCollected()`.
- **TipWatch** `forVision(vision)`: `start(cell, maxWatchSec, goneSec)` (TESTED 4.0 / 0.8),
  `periodic()`, `isDone()`, `isCellDown()`, `isCellStillUp()`, `isUnknown()`.
- **StateMachine<S>**: `setState`, `restartState`, `getState`, `getPreviousState`, `isIn`,
  `getTimeInStateSec`.
- **GamepadEx**: `readButtons()`, `isPressed/wasJustPressed/wasJustReleased(Button)`,
  `getShapedLeftY()` (+ forward), `getShapedLeftX()`, `getShapedRightX()`, triggers,
  `rumbleShort/Long()`, `rumbleBlips(n)`, static `shapeStick`.
- **PidfController**, **SlewRateLimiter**, **DataLogger** (`open(folder, name, header)`,
  `logRow(...)`, `flush()`, `close()`, `isOpen()`).

## Deviations from the matrix

- Bulk caching is MANUAL (VA used AUTO): OpModes MUST call `hardware.clearBulkCache()` each loop,
  also in AUTO next to `follower.update()`, or encoder/velocity reads go stale.
- `BatteryMonitor` lives in `subsystems/` (ARCHITECTURE.md), not `util/`.
- HIVE cells keep VA names (`RED_SCORING`/`RED_AUDIENCE`), not `RED_SOUTH/NORTH`, until D20.
  Cell helpers are static on `HiveCell` because `util.Alliance` API must not change.
- NCAM odometry aim fallback (`syncOdometry`, `getAimErrorDeg`) was NOT ported (aligner is disabled;
  NCAM code not reviewed). Launcher/Pose loggers (`LauncherLogger`, `MatchSummary`, ...) not ported;
  `DataLogger` is ready for them.
- `Intake.pushToIndexer()` added: while shooting the intake must push even when the robot holds 4.
- Ramps now stop at exactly 0 for one loop on a direction change (shared SlewRateLimiter); tiny change vs VA intake/indexer.

## Open questions

1. G408 wording: is pushing away opponent NECTAR the right behaviour? `REJECT_EJECT_SECONDS` and
   `INTAKE_ZONE_Y_OFFSET` need a field test.
2. G407 count relies on the HuskyLens pickup estimate; without it only shots are counted.
3. D14: is the flower arm on the robot? Servo positions are guesses.
4. D20: tag-ID to physical cell mapping.
5. Indexer runs with either flywheel (D9) — sequences use POLLEN only.
