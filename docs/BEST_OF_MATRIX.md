# BEST_OF_MATRIX — FTC #30311 Iron Lion → `IronLion30311-HiveMind` (BIOBUZZ 2026–27)

Consolidation agent output, 2026-11-24. Target: Java, FTC SDK 12.0, Pedro Pathing `com.pedropathing:revhub:3.0.2` + `tuning:1.0.3`.
Inputs: `docs/ROBOT_PROFILE.md` (cited as **RP §n / Qn**), `docs/AUDIT_MAIN.md` (**AM §n**), `docs/AUDIT_PEDRO.md` (**AP §n**), `docs/agents/*.md`, `docs/snapshots/*`.
Spot checks were run read-only (`git show / ls-tree / grep`) in `legacy/IronLion30311BIOBUZZInicioDeTemporada`. Every value marked "verified" below was read again from the ref named.

## How to read this file

**Ref shorthand** (same as RP, plus a few):

| Short | Ref @ commit | Note |
|---|---|---|
| `VA` | `origin/feature/turrubiates/vision-align` @ `a7b8f4c` (2026-11-24) | **Current robot. Primary source.** |
| `MASTER` | `origin/master` @ `37b7a16` (2026-11-22) | = `feature/autonomos-beta` @ `206fa84` + merge |
| `BETA` / `WT` | `feature/autonomos-beta` @ `206fa84` / repo #2 worktree (same files + `@Disabled` on `ShootTurnCollectAuto`) | The worktree adds nothing else |
| `NCAM` | `origin/feature/mar/duopodriveNcam` @ `2d92fba` (2026-11-21) | **Trap:** the local clone's `localcopy/feature/mar/duopodriveNcam` is the older `c73454f`. Always use `origin/…` |
| `ALP` | `origin/feature/turrubiates/auto-leave-park` @ `e455142` | `LeaveParkAuto`, `GuardedStraightDrive` |
| `FLOWER` | `origin/feature/renata/flower-arm` @ `889102d` | `FlowerArm` (does not compile) |
| `AS` | `localcopy/auto-shooter` @ `d4642a0` | **Local only.** `ShootAndCollectAuto`, `WPILibAuto/` |
| `TA` | `origin/feature/theautonomous` @ `125df22` | `MatchClock` / `TimeModel` / `RouteRunner` concept |
| `LUISAUTO` | `feature/luis/autonomospaths` @ `9cf2c66` + `TA` | Speculative hardware |
| `ALIGN` | `origin/feature/renata/auto-align` @ `3e733d4` | Launcher idle pre-spin idea |
| `PIDB` | `origin/PID` @ `6b8ac1e` | PID hold brake idea |
| `BALLS` | `origin/Balls-counting` @ `55f69ad` | LED inventory idea |

Paths are under `TeamCode/src/main/java/org/firstinspires/ftc/teamcode/` unless stated otherwise. Blob ids (6 chars) are from AM.

**Tie-breakers, in this order** (from the brief): (1) matches the current robot, (2) tested on the robot, (3) compiles with SDK 12 / Pedro 3.0.2, (4) follows the BIOBUZZ rules, (5) readable, (6) most recent. In the Reason column they appear as **T1…T6**.

**Constant status:** `TESTED` = a field-test value cited in a VA commit or Javadoc. `FITTED` = derived from a logged run but not measured. `TODO: TUNE` = placeholder, estimate or untested. `DECISION` = the team must choose (see §8).

**Target root package:** `org.firstinspires.ftc.teamcode` (kept, because the SDK annotation processor and the Pedro tuning `@Tuner` scan expect it). Sub-packages are as in the brief.

---

## 1. Best-of matrix (every level)

### 1.1 Repository level

| Item | Level | Candidates | Winner | Reason | New name | Changes needed |
|---|---|---|---|---|---|---|
| Source of truth for the new repo | repository | legacy repo #1 (`IronLion30311BIOBUZZInicioDeTemporada`); repo #2 `pedro-pathing-biobuzz-analysis-a65cc9` (worktree of `BETA`) | **repo #1** | Repo #2 has the same files as `BETA` @ `206fa84` plus one `@Disabled` (RP header, `docs/snapshots/pedro-worktree-uncommitted.diff`). Every unique line in it is also in repo #1. | `IronLion30311-HiveMind` (new) | Start a clean history. Archive repo #1 with a tag for each mined ref (`archive/<branch>`) before deleting branches. Import the `@Disabled` decision only as a note (Shoot Turn Collect is not migrated as is, see §4). |
| Local-only refs | repository | `localcopy/auto-shooter` (`d4642a0`), `localcopy/bm`, `localcopy/*` backups | `AS` mined; the rest archived | `AS` is the only local ref with unique code (AM §1 #4) | — | Push `AS` to a remote archive tag **before** the laptop is wiped (DECISION D18). |
| Branch hygiene | repository | `origin/CSV` vs `origin/csv` (case clash), orphan `localcopy/bm`, 4 scripted `luis/*` duplicates | — | AM §6.7 | — | The new repo has `main` + short-lived `feature/<topic>` only (no person names in branch names). |

### 1.2 Branch level

| Branch @ commit | Level | Winner / role | Reason | What is taken | Changes needed |
|---|---|---|---|---|---|
| `VA` @ `a7b8f4c` | branch | **Base (winner)** | T1 + T2: newest, field-tested 2026-11-19 → 11-24, corrects intake/pod/PIDF values (RP §10) | All mechanism, vision, logging, tests, TeleOp, tested autos, Pedro factory, tuners, scripts, CONVENTIONS | Rename and split per §2 |
| `MASTER` / `BETA` @ `37b7a16` / `206fa84` | branch | merged (secondary) | T3 compiles, but T1 fails (intake FORWARD, `FL`/`indexer` names, 0/0 pod offsets) and T2 fails (simulator only) | `Launcher.setTargetRpm/atTargetRpm`, `IndicatorLight.setGreen`, `Constants` route values, Pedro path set, small `PedroAutoBase` API, `pinpoint` mapping | Re-target to VA names and alliance-relative frame |
| `NCAM` @ `2d92fba` | branch | merged (secondary) | Unique: field map, odometry-assisted aim, saved pose, game/field notes; "worked as intended" calibration (RP §8) | `BiobuzzFieldMap` → `FieldConstants`; `HiveAlignController@7828c9` odometry fallback; `HiveCell@63f41d` coordinates; `saveCurrentPose` → `PoseStorage`; docs (translated) | Convert center-origin cm frame to the Pedro frame (AP §5.1) |
| `ALP` @ `e455142` | branch | merged | Only source of `LeaveParkAuto` + `GuardedStraightDrive` | Both classes | Port to the VA `Drivetrain` + Pedro Pinpoint reading (it uses `PinpointOdometry` from the Mar line) |
| `FLOWER` @ `889102d` | branch | **held** (D14) | Only FlowerArm source, but does not compile and is not confirmed on the robot (RP §4.5, Q14) | `FlowerArm` (fixed) only if Q14 = yes | Fix the constructor, add constants, rebind |
| `AS` @ `d4642a0` | branch | merged (ideas) | `ShootAndCollectAuto` route idea; its in-place turn at y = 8 hits the wall (AP §5.3) | Collect-after-shot idea, merged into `ShootCollectPaths` | Add a 5 in back-off (as in BETA `206fa84`) and a PARK |
| `TA` @ `125df22` | branch | merged (concept) | Time-budget runner; hardware is fictional, code blocks with sleeps (AM §2.6) | `MatchClock`, `TimeModel` constants (`AUTO_PERIOD_S 30`, `SAFETY_MARGIN_S 1.0`) as a concept for `MatchTimer` + `CycleBudget` | Rewrite non-blocking |
| `ALIGN` @ `3e733d4` | branch | idea only | `IDLE_RPM 1800` pre-spin (AM §2.6) | Optional `LAUNCHER_IDLE_RPM` (default 0 = off) | `TODO: TUNE`, battery cost |
| `PIDB` @ `6b8ac1e` | branch | idea only | PID position-hold brake, untested | Nothing now (VA `holdTarget` already holds in auto) | — |
| `BALLS` @ `55f69ad` | branch | idea only | LED inventory display stub | Nothing now; backlog item | — |
| `hive-state-probe`, `hive-alliance-target`, `naming-conventions`, `luis/{park,alliance,probe,naming}`, `mar/{duopodrive,vision,onepodrive}`, `develop`, `SIT`, `Stop-servo`, `CSV`, `csv`, `renata/{shooter,auto-test,removing-Alejandro}`, `luis/shooters-upgraded`, all `localcopy/*` backups | branch | DISCARD (archive tag) | Superseded by VA or broken (AM §1) | — | — |
| `LUISAUTO` (`autonomospaths`) | branch | DISCARD | Speculative hardware (RP §4.6) | `Pid@6aeb44` as `PidfController` base only | — |

### 1.3 Folder level

| Old folder (ref) | Level | Winner | Reason | New folder | Changes needed |
|---|---|---|---|---|---|
| `teamcode/` root (`RobotHardware.java`, `Constants.java`, `*.md`) | folder | merged | Root holds hardware + constants + stray notes | `hardware/`, `config/`, docs to `/docs` | Move Markdown out of the Java package (AM §6.7) |
| `subsystems/` (VA) | folder | VA | T1 | `subsystems/` (flat, one class per mechanism) | Diagnostics moved to `util/` + `diagnostics/` |
| `subsystems/vision/` (VA) | folder | VA + NCAM | T1, T2 | `subsystems/vision/` (Limelight) and `subsystems/colorsensing/` (HuskyLens) | Split ball vision from HIVE vision |
| `subsystems/pathing/` (VA) | folder | VA | T1 | `pathing/` | Factory → `PedroConstants` + `PedroFollowerFactory` |
| `subsystems/pathing/tuning/` (VA) | folder | VA | Only AutoTune set (AP §2.1) | `pathing/tuning/` | None besides the package |
| `opmodes/auto/` (VA, MASTER) | folder | merged | | `opmodes/auto/` | Competition autos only |
| `opmodes/base/` (MASTER) | folder | merged into `opmodes/auto/` | | `opmodes/auto/` (`AutoBase`) | — |
| `opmodes/teleop/` (VA) | folder | VA | | `opmodes/teleop/` | `DriveWheelTest` moves to `opmodes/tuning/` |
| `opmodes/calibration/` (VA, MASTER) | folder | merged | | `opmodes/tuning/` | Group name `"Tuning"`, never `"Competition"` (AM §6.4) |
| `subsystems/autosubsystems/`, `opmodes/auto/theautonomous/` (LUISAUTO, TA) | folder | DISCARD | Fictional hardware | — | — |
| `subsystems/shooters/` (`dfe9f54`) | folder | DISCARD | NPE duplicate | — | — |
| `util/` (PIDB) | folder | DISCARD the file; folder name reused | | `util/` (new content) | — |
| `TeamCode/src/test/java/…` (VA) | folder | VA | Only JUnit tests | same | Add tests for pure logic (see §2.7) |
| `TeamCode/src/main/assets/images/` (VA) | folder | VA | Dashboard field image | same | — |
| `scripts/` (VA) | folder | VA | adb log pull + visualizer export | `tools/` | Update package paths in the Python exporters |
| `doc/naming/` (VA) | folder | VA | | `docs/naming/` | Update the map with §6 |
| `docs/` (MASTER) | folder | merged | | `docs/` | Translate, fix alliance labels |
| `WPILibAuto/` (AS), `.idea/`, `.artifacts/` | folder | DISCARD | FRC code / IDE / agent scratch | — | `.gitignore` them |
| `.github/` (VA) | folder | VA | CONTRIBUTING + PR template | `.github/` | Replace the unused `develop → SIT → UAT → master` flow with `feature → main`; add a build CI workflow (AM §6.5) |

### 1.4 File and class level

| Item | Level | Candidates (ref / path / blob) | Winner | Reason | New name | Changes needed |
|---|---|---|---|---|---|---|
| Hardware map | class | `VA:RobotHardware@ae9fef`; `MASTER:RobotHardware@684154`; `NCAM@169592`; `FLOWER@329450` | **VA** + merged bits | T1 (new DS names + legacy fallback, bulk caching, optional cameras) | `hardware/RobotHardware` | Add `pinpoint` `tryGet` (MASTER); add `"indexer"` as a 2nd legacy name (D2); fields `pollenLauncherMotor`/`nectarLauncherMotor`; flower servos only if D14 = yes; all names come from `RobotConstants` |
| Launcher | class | `VA:subsystems/Shooter@ac639e`; `MASTER@ec491a`; `ALIGN@d0a2bb`; 8 older blobs | **merged** (VA base) | T1 + T2 (PIDF from logs, voltage-compensated kF, ready hysteresis) | `subsystems/Launcher` | Add `setTargetRpm(p, n)` + `isAtTargetRpm()` from MASTER; drop the 8-boolean `update(...)` (gamepad logic → TeleOp); rename methods (§2.4) |
| Launcher CSV logger | class | `VA:subsystems/ShooterLogger@5af560` | VA | Only one | `util/logging/LauncherLogger` | Build on `DataLogger` |
| Indexer | class | `VA:Indexer@74d3a7`; `MASTER@6a271f`; `Alejandro@a20fa6`; `FLOWER@715f59` | **VA** | T1 + T2 (jam detection from logs, RPM pause) | `subsystems/Indexer` | Make `FEED_POWER` a tunable; `periodic()` instead of `update(feed, eject, paused)` |
| Intake | class | `VA:Intake@0aca19` (0.75); `luis/*@e73f94` (1.0) | **VA** | Same class; VA direction REVERSE is tested (T2) | `subsystems/Intake` | Power → `RobotConstants.INTAKE_POWER` (D8); add `shake()` from `VA:PedroAutoBase` intake wiggle |
| Drivetrain | class | `VA:MecanumDrive@746e93`; `MASTER@0ae95b`; `NCAM@798792`; `PIDB@5fe5c7` | **VA** | T1 + T2 (ramps tuned: "0.1 … risk tipping over; 0.25 … slid", RP §2) | `subsystems/Drivetrain` | Re-add `stop()` and `getAverageForwardCm()` (needed by `GuardedStraightMove`); constants → `DriveConstants` |
| Guarded straight move | class | `ALP:subsystems/GuardedStraightDrive@f7d1fd` | ALP | Only one; stops on pod **or** wheel encoders | `subsystems/GuardedStraightMove` | Read the pod through the Pedro `Follower` pose, not `PinpointOdometry` |
| Pinpoint wrapper (non-Pedro) | class | `MASTER:PinpointOdometry@d9e1d1`; `NCAM@13a0c3`; `ALP@4abb7d` | **DISCARD** (keep 2 ideas) | Pedro's `PinpointLocalizer` owns the device in auto; two owners of one I²C device is a bug (AP §8 last row) | — | Ideas kept: pod health → `Drivetrain`/`AutoSafety`; `saveCurrentPose` → `util/PoseStorage` |
| Point-to-point auto drive | class | `MASTER:AutoDrive@42327c`; `NCAM@86ed33`; `VA:PedroAutoBase.driveTo/driveThrough` | **VA `driveTo`** | T2 ("instead of the untuned Pedro follower, which was slow", `927e4df`) | `pathing/OdometryDriver` | Extract from the 1126-line base; keep it until Pedro is tuned (D6) |
| Indicator light | class | `VA:IndicatorLight@17e842`; `MASTER@98d9b4` (+green) | **merged** | VA base (T1) + `setGreen` (T5) | `subsystems/Lights` (wraps `indicatorLight` + optional `nectarAlertLight`) | Colors → `RobotConstants`; yellow 0.42 `TODO: TUNE` |
| NECTAR alert light | class | `VA:NectarAlertLight@b99610` | VA | Only one | inside `subsystems/Lights` (`updateNectarAlert(...)`) | Optional device (D12) |
| Battery monitor | class | `VA:BatteryMonitor@2c6210` | VA | Only one | `util/BatteryMonitor` | — |
| Match summary / log files / motor logger / pose logger | class | `VA:MatchSummary@c7a737`, `LogFiles@9c9858`, `MotorDiagnosticsLogger@93a19c`, `pathing/PoseLogger@3e0997` | VA | Only tested versions | `util/logging/{MatchSummary, MotorLogger, PoseLogger}` on top of `util/DataLogger` | One CSV writer (`DataLogger`) instead of four |
| HIVE vision | class | `VA:vision/HiveVision@1a134f`; `MASTER@7610b8`; `NCAM@66d8eb` | **VA** | T1 + T2 (TIP detection field-checked) | `subsystems/Vision` (facade) + `subsystems/vision/HiveVision` | `addTelemetry(t)` |
| HIVE align | class | `VA:HiveAlignController@6b6f19`; `NCAM@7828c9` (odometry hybrid); `MASTER@d50f15` | **merged** | VA tuned setpoint 1.03 m / 0.08 m (T2) + NCAM aim fallback when tags are lost (T5) | `subsystems/vision/HiveAligner` | Stay `ENABLED = false` until calibrated (D17) |
| HIVE geometry / orientation / tracker / cluster pose | class | `VA:HiveGeometry@7fbf90`, `HiveOrientation@791c5d`, `HiveStateTracker@7ef04c`, `HiveClusterPose@1dc8a3`; `NCAM:HiveGeometry@db231c` | **VA** | Unit-tested (T2-ish, T3) | same names in `subsystems/vision/` | — |
| HIVE cell enum | class | `VA:HiveCell@2d9fe8`; `NCAM@63f41d` (field coords, `findClosestCell`) | **merged** | VA tag IDs + NCAM coordinates | `subsystems/vision/HiveCell` | Coordinates converted to the Pedro frame. **Cell naming vs position must be confirmed (D20)** |
| Alliance | class | `VA:vision/Alliance@fcfd12`; `MASTER:pathing/Alliance@6d3163` (mirror); `AS@116500`; `TA@a7450d`; `VA:pathing/AlliancePoses@582d51` | **merged** (VA enum + VA identity poses) | VA approach matches the 180° rotational field (AP §5.2) | `util/Alliance` (enum + `toFieldAbsolute(Pose)`) | `rotate180` only for drawing / AprilTag relocalization; **no `mirrorX`** (D13) |
| Ball vision (HuskyLens) | class | `VA:vision/BallVision@263911`; `BALLS:BallsCounting@e989e8` | **VA** | T1 + T2 | `subsystems/ColorSensing` | IDs from `RobotConstants` (D11) |
| Ball counter | class | `VA:vision/BallIntakeCounter@f80c96` | VA | Only working one | inside `subsystems/ColorSensing` (`getBallCount(color)`) | — |
| FLOWER arm | class | `FLOWER:subsystems/FlowerArm@428e23` | **held** | Does not compile; mechanism UNKNOWN on robot (Q14) | `subsystems/FlowerArm` (only if D14 = yes; otherwise **no Placer class**) | Rename the `DualServo` constructor; constants; state enum; rebind off GP1 bumpers |
| Gate servos | class | `NCAM:Pera@2894ef`; `ALIGN/FLOWER:StopServo@9c7b50` | DISCARD | Gate removed 2026-11-20 (`c1580df`) | — | Re-add only if D10 says a gate is back |
| Pedro follower factory | class | `VA:pathing/PedroFollowerFactory@d937b5`; `MASTER@04feea` | **VA** | T1 + T2 (fitted pod offsets, Y REVERSED, `manualBrakeMode`) | `pathing/PedroConstants` + `pathing/PedroFollowerFactory` | Device names from `RobotConstants` |
| AutoTune | class | `VA:pathing/tuning/{Tuning, MecanumTuner, PinpointTuner, ForesightTuner, Tests}` | VA | Only source, `tuning:1.0.3` | `pathing/tuning/*` | — |
| Dashboard field drawing | class | `VA:pathing/FieldDrawing@7c4dcb` | VA | Only one | `pathing/FieldDrawing` | Draw blue with `rotate180` |
| Auto base | class | `VA:opmodes/auto/PedroAutoBase@b24b02` (1126 lines); `MASTER:opmodes/base/PedroAutoBase@882a98` | **merged** | VA guards + tested shooting (T2); MASTER's small API (T5) | `opmodes/auto/AutoBase` + `pathing/OdometryDriver` + `statemachine/*` + `util/MatchTimer` | Split into files of < 300 lines |
| Route constants | file | `MASTER:Constants@43c1c9` | merged | Per-route values | `config/FieldConstants` + per-route `pathing/paths/*Paths` | RPM/feed values come from `LauncherConstants` (one copy) |
| Field map | class | `NCAM:BiobuzzFieldMap@e6038b` | merged | Only field-geometry source (from "Manual TU10") | `config/FieldConstants` | Pedro frame (x = East + 72, y = North + 72, inches) |
| TeleOp | class | `VA:opmodes/teleop/MainTeleOp@ad3c7d`; `MASTER@4d35ec`; `NCAM@4d4252`; `FLOWER@a8f804` | **VA** | T1 + T2 | `opmodes/teleop/TeleOpBase` + `TeleOpRed` + `TeleOpBlue` | Gamepad edge logic → `GamepadEx`; telemetry → each subsystem's `addTelemetry` |
| Odometry calibrator | class | `MASTER:opmodes/calibration/OdometryCalibrationAuto@854c72` | DISCARD | Depends on the discarded `AutoDrive`/`PinpointOdometry`; AutoTune `PinpointTuner` covers it | — | — |
| Vision calibration | class | `NCAM:opmodes/auto/VisionCalibrationAuto@8710e8`; `VA:opmodes/calibration/HiveStateProbe@bfd780` | **VA** + NCAM steps | | `opmodes/tuning/TuneHiveVision` | Add NCAM's "capture current pose as target" step |
| JUnit tests | file | `VA:src/test/…/Hive{Geometry,Orientation,StateTracker}Test` | VA | Only tests | same | Add tests (§2.7) |

### 1.5 Method level (the methods that have competing versions)

| Method | Candidates | Winner | Reason | New name | Changes |
|---|---|---|---|---|---|
| Launcher set both setpoints | `MASTER:Shooter.setTargetRpm(rpmP, rpmN)@ec491a`; `VA:Shooter.setTargetRpmP@ac639e` | merged | MASTER autos need the pair; VA only sets pollen | `setTargetRpm(double pollenRpm, double nectarRpm)` + `setPollenTargetRpm(double)` | Clamp with VA `clampRpm` |
| Launcher at-speed check | `MASTER:atTargetRPM()` (±100, both wheels); `VA:isPollenReady()/isNectarReady()` (hysteresis 100/200) | **VA** + wrapper | T2 (hysteresis tested) | `isPollenReady()`, `isNectarReady()`, `isAtTargetRpm()` = active wheels ready | — |
| Voltage compensation | `VA:Shooter.compensateForVoltage()` | VA | Only one, T2 | `compensateForVoltage()` (private, called from `periodic()`) | — |
| RPM D-pad tuning | `VA:Shooter.handleRpmTuning(...)` | VA | | `adjustPollenRpm(double delta)` / `adjustNectarRpm(double delta)` | Edge detection moves to `GamepadEx` |
| Indexer jam handling | `VA:Indexer.checkForJam()` | VA | T2 (from logs, `9c03b6c`) | `checkForJam()` | — |
| Drive ramp | `VA:MecanumDrive.applyRamp(dt, accel, decel)`; `MASTER:driveRamped` | VA | T2 (`99a7233`) | `Drivetrain.applyRamp` using `util/SlewRateLimiter` | — |
| Stick shaping | `VA:MainTeleOp.shapeStickInput` (deadzone 0.05, cubic) | VA | | `GamepadEx.getShapedLeftY()` etc. | — |
| Point-to-point drive | `VA:PedroAutoBase.driveTo/driveThrough/driveLeg`; `MASTER:AutoDrive.driveToPoseCm` | VA | T2 | `OdometryDriver.driveTo(...)`, `driveThrough(...)` | Non-blocking step API |
| Turn | `VA:PedroAutoBase.turnTo(name, headingDeg, mode)` (fixed rad/deg bug `12377ee`); `MASTER:turnBy` | VA | T2 | `OdometryDriver.turnTo(double headingDeg)` | — |
| Preload volley | `VA:shootPollenPreload(balls, maxSec)`; `MASTER:shootPreload/shootWhenReady` | VA | T2 ("4/4") | `statemachine/ShootingSequence` | One ball per ready window |
| Collect | `VA:collectBalls(maxSec, colors…)`; `MASTER:keepIntakeAndIndexerOn` | VA | T2 (partially) | `statemachine/CollectSequence` | — |
| TIP watch | `VA:PedroAutoBase` TIP check + `PreloadRouteAuto` early watch (`c1c59db`, `a7b8f4c`) | VA | T2 (field-checked) | `Vision.isCellDown(HiveCell, double goneSec)` + `statemachine/TipWatch` | — |
| Odometry fault guard | `VA:checkOdometry`, `startPoseErrorIn`, re-zero (`3fbcbb8`, `03cb6df`, `61dd9ef`) | VA | Only one, T2 | `AutoBase.checkOdometry()` (or `util/OdometryGuard`) | — |
| Auto timer | `VA:autoSeconds()`; `TA:MatchClock.elapsed/remaining` | merged | | `MatchTimer.getElapsedSec()`, `getRemainingSec()`, `hasTimeFor(double sec)` | — |
| Pose hand-off auto → TeleOp | `NCAM:PinpointOdometry.saveCurrentPose/hasSavedAutonomousPose` | NCAM idea | Only one | `PoseStorage.save(Pose)`, `PoseStorage.hasPose()` | Static holder, Pedro `Pose` |
| Odometry-assisted aim | `NCAM:HiveAlignController.syncOdometryWithVisibleHive/getAimErrorDegrees/getTargetHeadingDegrees@7828c9` | NCAM | Only one | `HiveAligner.syncOdometry()`, `getAimErrorDeg()` | Behind `ENABLED` |

### 1.6 Constant / config level

The full conflict table is §3. The single-source constants are listed per class in §2.

### 1.7 OpMode level

| Old OpMode (ref) | Winner | Reason | New OpMode name (DS label) | Group | Notes |
|---|---|---|---|---|---|
| `Main TeleOp` (VA) | VA | T1/T2 | `TeleOp Red`, `TeleOp Blue` | Competition | Alliance fixed per OpMode, no INIT button |
| `Preload + Park (Odometry)` (VA) | VA | **TESTED 4/4** | `Red Near Preload Park`, `Blue Near Preload Park` | Competition | Same poses both alliances (D13) |
| `Preload + Collect + Park (Odometry)` (VA) | VA | partially tested | `Red Near Cycle`, `Blue Near Cycle` | Competition | Time-aware (§5) |
| `Safe Move (Encoders)` (VA) | VA | fallback | `Safe Park Encoders` | Competition | ticks/cm `TODO: TUNE` |
| `Leave + Park` (ALP) | ALP | only source | `Safe Park Loading Zone` | Competition | Part of AutoSafePark (§5) |
| `Park` (BETA) | BETA | simple, legal | merged into `AutoSafePark` | Competition | End x ≥ 11 in (AP §8) |
| `Tick`, `Center Lane` (BETA) | **held** | Under-HIVE risk | `Red/Blue Far Two Shot` only after re-routing west (§4) | Test until field-proved | Originals kept in `pathing/paths/archive` as data |
| `Outer Lane` (BETA `@Disabled`, AS, VA) | BETA poses | legal-safe | `Red/Blue Near Outer Lane` | Test → Competition after field test | End y ≤ 117 |
| `Shoot Turn Collect` (BETA) + `Shoot And Collect` (AS) | merged | No PARK; AS hits the wall | `ShootCollectPaths` used by the Near Cycle | — | Not its own OpMode |
| `Start Routine`, `Center Lane` (old), `Top Wall` (AS, VA, NCAM) | DISCARD | FLOWER/HIVE/wall risks (AP §5.3) | — | — | — |
| `Odometry Square Test`, `Pedro Pose Check`, `Hive State Probe`, `Drive Wheel Test` (VA) | VA | test tools | `Test Odometry Square`, `Test Pedro Pose`, `Tune Hive Vision`, `Test Drive Wheels` | Tuning | — |
| AutoTune `@Tuner` set (VA) | VA | | unchanged (web UI :10158) | — | — |
| `Odometry & Gyro Calibrator` (MASTER), `Red Corner -> Closest Cell Test`, `Camera & Align Test`, `Top Wall` (NCAM) | DISCARD / merged | §1.4 | `Tune Hive Vision` takes the NCAM capture step | — | — |
| 16 `AUTO ROJO/AZUL …`, `THE AUTONOMOUS` (LUISAUTO, TA), `Auto-test` (renata) | DISCARD | §7 | — | — | — |

### 1.8 Dependency level

| Dependency | Candidates | Winner | Reason | Changes |
|---|---|---|---|---|
| FTC SDK | `12.0.0` on every branch | `12.0.0` | latest (AP §3, `docs/snapshots/target-versions.md`) | — |
| Pedro Pathing | `com.pedropathing:revhub:3.0.2` (MASTER, VA, AS) | `3.0.2` | latest; VA built and ran with it (INFERRED from field runs) | — |
| Pedro AutoTune | `com.pedropathing:tuning:1.0.3` (VA only) | `1.0.3` | needed for Foresight tuning | Needs `https://repo.dairy.foundation/releases/` |
| FTC Dashboard | `com.acmerobotics.dashboard:dashboard:0.6.0` from `maven.brott.dev` (VA) | keep | Every `@Config` tunable relies on it; VA tested values were tuned with it | Keep `exclude group: 'org.firstinspires.ftc'`. CI must prove the coordinate resolves (AM §6.5) |
| JUnit | `junit:4.13.2` (VA `TeamCode/build.gradle`) | keep | `testImplementation` only | — |
| Pedro Ivy (`com.pedropathing.ivy`) | renata Auto-test (imports, no artifact) | DISCARD | not declared anywhere (AP §3 item 7) | — |
| WPILib | `AS:WPILibAuto/` | DISCARD | FRC code | — |
| AGP 9.3.3 / Gradle 9.5–9.8 | `d3aa850`, `ff40234`, `9cf2c66`, `125df22` | DISCARD | SDK template says do not modify (AM §1) | Keep AGP 8.13.2 / Gradle 9.1.0 from the SDK template |

### 1.9 Resource / asset level

| Asset | Source | Winner | Changes |
|---|---|---|---|
| `TeamCode/src/main/assets/images/biobuzz.webp` | VA | keep | Dashboard background; check it matches the frame orientation |
| `res/xml/teamwebcamcalibrations.xml`, `res/values/strings.xml`, `res/raw/readme.md` | SDK (identical everywhere) | keep (SDK) | — |
| `TeamCode/lib/OpModeAnnotationProcessor.jar` | SDK | keep | — |
| `scripts/*.sh`, `scripts/*_to_visualizer.py` | VA | keep → `tools/` | Update class paths |
| `.idea/`, `.artifacts/`, `cool.txt`, `contextshooters` | various | DISCARD | `.gitignore` |

### 1.10 Documentation level

| Doc | Source | Winner | New path | Changes |
|---|---|---|---|---|
| `CONVENTIONS.md` | VA | keep | `docs/CONVENTIONS.md` | Add the method rules of this matrix (§2 header) |
| `doc/naming/rename-map.md` | VA | keep | `docs/naming/rename-map.md` | Append §6 |
| `PROJECT_CONTEXT.md` | VA/MASTER/NCAM | REPLACE | `docs/ROBOT_PROFILE.md` (already written) | Stale content |
| `DRIVE_MODES_REFERENCE.md` (×2) | MASTER | merge one copy | `docs/DRIVE_MODES.md` | Update bindings to the VA TeleOp |
| `docs/autonomos-cancha.html` | BETA (`6aa75b`), AS (`95792a`) | refactor | `docs/auto-routes.html` | Fix alliance labels; draw territory line x = 72, LZs, HIVE box, FLOWERS |
| `juego-del-robot-biobuzz.md`, `mapa-cancha-odometria-biobuzz.md` | NCAM (inside the Java package) | keep, translate | `docs/game-rules.md`, `docs/field-coordinates.md` | English; add the Pedro-frame conversion table (AP §5.1) |
| `PLAN.md`, `NOTAS_AUTONOMOS.md` | TA | keep as history only | `docs/archive/auto-notes.md` | Translate the time-budget part |
| `.github/CONTRIBUTING.md`, `PULL_REQUEST_TEMPLATE.md` | VA | keep, edit | `.github/` | New branch flow |
| `readme.md` (teamcode) | SDK | DISCARD | — | — |
| `docs/ROBOT_PROFILE.md`, `AUDIT_*.md`, this file | new | keep | `docs/` | — |

---

## 2. Merge plan per final class

**Method rules for every class:** camelCase verbs; booleans start with `is` / `has` / `can`; each subsystem implements `Subsystem` with `periodic()` (called once per loop) and `addTelemetry(Telemetry t)`; no gamepad objects inside subsystems; no `sleep()` in loops; every tunable is a `public static` in a `config/` class with `@Config` (FTC Dashboard), marked `TESTED`, `FITTED` or `// TODO: TUNE`.

### 2.1 `config/`

**`config/RobotConstants`** (device names + mechanism tunables; `@Config`)

| Constant | Value | Status | Source |
|---|---|---|---|
| `FRONT_LEFT_DRIVE` … `BACK_RIGHT_DRIVE` | `"frontLeftDrive"`, `"frontRightDrive"`, `"backLeftDrive"`, `"backRightDrive"` | TESTED (Pedro found them in the 4/4 run) | `VA:RobotHardware.java` l.23-26 (verified) |
| `INTAKE_MOTOR`, `INDEXER_MOTOR` | `"intakeMotor"`, `"indexerMotor"` | DECISION D2 | VA l.28-29 |
| `POLLEN_LAUNCHER_MOTOR`, `NECTAR_LAUNCHER_MOTOR` | `"pollenShooterMotor"`, `"nectarShooterMotor"` (keep until D15) | DECISION D15 | VA l.30-31 |
| `INDICATOR_LIGHT`, `NECTAR_ALERT_LIGHT` | `"indicatorLight"`, `"nectarAlertLight"` | D12 for the 2nd | VA |
| `LIMELIGHT`, `HUSKY_LENS`, `PINPOINT` | `"limelight"`, `"huskyLens"`, `"pinpoint"` | TESTED | VA, `PedroFollowerFactory` |
| `LEGACY_NAMES` (fallback) | `FL FR BL BR intake alejandro indexer P N indicator` | keep until D2 is closed | VA l.38-46 + MASTER `"indexer"` |
| `INTAKE_DIRECTION` | `REVERSE` | TESTED 2026-11-23 (`927e4df`) | VA |
| `INTAKE_POWER` | `0.75` | DECISION D8 (alt 1.0) | `VA:Intake` l.22 |
| `INTAKE_RAMP_SEC` | `0.15` | TESTED (in use) | VA |
| `INTAKE_SHAKE_REVERSE_SEC` / `_WAIT_SEC` / `_MAX_TRIES` | `0.35` / `0.8` / `3` | FITTED (`7004eba`) | `VA:PedroAutoBase` |
| `INDEXER_FEED_POWER` | `0.75` | TESTED (in use) | `VA:Indexer` l.40 |
| `INDEXER_RAMP_SEC` | `0.15` | TESTED | VA |
| `INDEXER_JAM_CURRENT_AMPS` / `JAM_TIME_SEC` / `UNJAM_TIME_SEC` / `UNJAM_STEP_SEC` / `MAX_UNJAM_ATTEMPTS` / `JAM_RESET_SEC` | `5.0` / `0.5` / `0.3` / `0.2` / `3` / `2.0` | FITTED from logs (`9c03b6c`) | VA l.55-73 |
| `INDEXER_PAUSE_WHEN_NOT_READY` | `true` | TESTED ("29 of 46 scored") | VA l.52 |
| `HUSKY_POLLEN_ID` / `HUSKY_RED_NECTAR_ID` / `HUSKY_BLUE_NECTAR_ID` | `1` / `2` / `3` | DECISION D11 | `VA:BallVision` l.34-40 |
| `HUSKY_MIN_BLOCK_AREA_PX`, `HUSKY_READ_INTERVAL_SEC`, `BALL_ASSIST_KP`, `BALL_ASSIST_MAX_TURN` | `150`, `0.05`, `0.35`, `0.3` | `TODO: TUNE` (no test cited) | VA |
| `LIGHT_OFF/RED/YELLOW/BLUE/WHITE/GREEN` | `0.0 / 0.33 / 0.42 / 0.63 / 1.0 / 0.50` | yellow `TODO: TUNE`; green from MASTER untested on VA | `VA:IndicatorLight` l.18-23, `MASTER@98d9b4` |
| `LIGHT_BLINK_PERIOD_SEC`, `NECTAR_ALERT_HOLD_SEC`, `NECTAR_ALERT_BLINK_SEC` | `0.4`, `0.6`, `0.25` | in use | VA |
| `BATTERY_LOW_VOLTS` / `LOW_TIME_SEC` / `LOW_CLEAR_VOLTS` / `SAG_VOLTS` / `SAG_DRIVE_LIMIT` / `SAG_HOLD_SEC` | `11.0 / 3.0 / 11.3 / 8.5 / 0.5 / 0.5` | in use | `VA:BatteryMonitor` |
| `ROBOT_LENGTH_IN`, `ROBOT_WIDTH_IN` | `18.0`, `18.0` | DECISION D16 | §3 |

**`config/DriveConstants`** (`@Config`)

| Constant | Value | Status | Source |
|---|---|---|---|
| Directions | FL, BL `REVERSE`; FR, BR `FORWARD` | TESTED (same on all branches) | VA |
| `ZERO_POWER_BRAKE` | `true` | TESTED (`64dfe81`) | VA |
| `RUN_MODE` | `RUN_WITHOUT_ENCODER` (encoders still read) | TESTED | VA |
| `ACCEL_RAMP_SEC` / `DECEL_RAMP_SEC` / `STOP_CUTOFF_POWER` / `ALIGN_RAMP_SEC` | `0.3 / 0.2 / 0.25 / 0.1` | TESTED | `VA:MecanumDrive` l.32-49 |
| `SLOW_SPEED` / `FAST_SPEED` | `0.4` / `1.0` | TESTED | VA |
| `STICK_DEADZONE`, stick curve | `0.05`, cubic | TESTED | `VA:MainTeleOp` |
| `TICKS_PER_REV`, `WHEEL_DIAMETER_CM` | `537.7`, `10.4` | DECISION D3 (confirm motor/wheel) | MASTER |
| `ENCODER_TICKS_PER_CM_FORWARD` / `_STRAFE` | `16.5` / `18.5` | `TODO: TUNE` ("calibrate on the field") | `VA:SafeMoveAuto` |
| Odometry driver (`OdometryDriver`): `MAX_POWER 0.85`, `MIN_POWER 0.25`, `SLOW_ZONE_IN 18`, `TOLERANCE_IN 0.6`, `ACCEL_PER_SEC 3.0`, `KP_HEADING 1.0`, `MAX_TURN 0.45`, `CORNER_POWER 0.45`, `PASS_THROUGH_IN 4`, `TURN_KP 1.2`, `TURN_MAX 0.5`, `TURN_MIN 0.15`, `TURN_TOL_DEG 2`, `TURN_TIMEOUT_SEC 2.5`, `HOLD_KP 0.12`, `HOLD_MAX 0.25`, `HOLD_DEADBAND_IN 0.3`, `DRIVE_TIMEOUT_SEC 1.5 + d/20` | as listed | TESTED (the 4/4 run used them) | `VA:PedroAutoBase` (AP §2.3) |
| Odometry guard: `POSE_JUMP_LIMIT_IN 12`, `POSE_JUMP_LIMIT_DEG 30`, `START_POSE_TOL_IN 2`, `START_HEADING_TOL_DEG 5`, `START_REZERO_MAX_IN 12`, `START_REZERO_MAX_DEG 10` | as listed | TESTED (`3fbcbb8`, `03cb6df`, `61dd9ef`) | VA |

**`config/LauncherConstants`** (`@Config`)

| Constant | Value | Status | Source |
|---|---|---|---|
| `DIRECTION` (both) | `REVERSE`, `RUN_USING_ENCODER`, FLOAT | TESTED | `VA:RobotHardware` |
| `TICKS_PER_REV` | `28.0` | value in use; motor model D7 | `VA:Shooter` l.51 |
| `MAX_RPM` / `MIN_RPM` / `RPM_STEP` | `6000 / 0 / 50` | in use | VA |
| `POLLEN_TELEOP_RPM` | `3000` | TESTED ("at 2900 … 11 of 25"; 3000 "scored 29") | VA l.60 |
| `POLLEN_AUTO_RPM` | `2950` | TESTED ("both shots went long at 3000", 11-23) | `VA:PreloadRouteAuto` l.211 |
| `POLLEN_OTHER_CELL_RPM` | `2950` | FITTED ("2800 left 2 of 3 short"; `TODO(team)`) | VA l.168 |
| `NECTAR_RPM` | `3600` | TESTED ("4500 was unreachable") — NECTAR is not scored this event | VA l.67 |
| `KP / KI / KD` | `30 / 0 / 0` | TESTED (from logs; "I wound up on every shot dip") | VA l.92-101 |
| `KF_AT_12V` | `15.0`, rescaled every `0.25 s` when it changes > `0.02`, ignored below `7.0 V` | TESTED | VA l.109-121 |
| `READY_TOLERANCE_RPM` / `NOT_READY_TOLERANCE_RPM` | `100 / 200` | TESTED | VA l.76-82 |
| `IDLE_RPM` | `0` (off). Idea `1800` from ALIGN | `TODO: TUNE` (battery cost) | `ALIGN:Shooter@d0a2bb` |
| `SPIN_UP_TIMEOUT_SEC` | `3.0` | `TODO: TUNE` | MASTER `Constants` |
| `PRELOAD_BALLS`, `SHOOT_MAX_SEC` | `4`, `10.0` | TESTED | `VA:PreloadRouteAuto` |
| HIVE align (`HiveAligner`): `ENABLED false`, `TARGET_FORWARD_M 1.03`, `TARGET_STRAFE_M 0.08`, tolerances `0.03 m / 0.025 m / 2.5°`, PD gains fwd `1.45/0.14` max `0.60`, strafe `1.75/0.16` max `0.70`, yaw `0.022/0.0025` max `0.45`, FF `0.06 / 0.05` | as listed | setpoint TESTED (11-20); gains `TODO: TUNE` (disabled after overshoot, `21900d5`) | `VA:HiveAlignController` l.31-78 |
| Limelight: pipeline `0`, `100 Hz`, EMA `0.45`, stale `200 ms`, dropout hold `0.15 s`, `CAMERA_PITCH_DEG 40` | as listed | pitch `TODO: TUNE` (D17) | `VA:HiveVision`, `HiveGeometry` |
| `TAG_SPACING_M` | `0.1143` | DECISION D17 (alt `0.0825`) | VA vs NCAM |

**`config/FieldConstants`** (Pedro frame: inches, origin = red/audience corner, x right, y toward the back wall, heading CCW from +x; plain `final`, not tunable)

| Constant | Value | Status | Source |
|---|---|---|---|
| `FIELD_SIZE_IN` | `144.0` | DECISION D19 (alt 141.5) | MASTER, NCAM, `VA:FIELD_LENGTH_CM 365.76` |
| `TILE_IN` | `24.0` | from the manual (NCAM) | NCAM |
| `TERRITORY_LINE_X` | `72.0` (red x < 72) | TO VERIFY vs manual (D13) | AP §5.1 |
| `HIVE_FRAME_BOX` | x `47.3–96.7`, y `52.5–91.5` | from NCAM (49.46 × 38.95 in) | NCAM l.122-128 |
| `HIVE_CELL_*` centers | red-south `(59.25, 57.4)`, red-north `(59.25, 86.6)`, blue-north `(84.75, 86.6)`, blue-south `(84.75, 57.4)` | TO VERIFY + D20 (tag-ID mapping) | AP §5.1 |
| `RED_LOADING_ZONE` | x `0–11`, y `96.5–119.5` | TO VERIFY | AP §5.1 |
| `BLUE_LOADING_ZONE` (absolute) | x `133–144`, y `24.5–47.5` (= red rotated 180°) | TO VERIFY | AP §5.1 |
| `FLOWERS` | `(96, 1.9)`, `(48, 142.1)`, `(1.9, 48)`, `(142.1, 96)` | TO VERIFY | AP §5.1 |
| `WALL_CLEARANCE_IN` | `2.0` minimum gap at the end of AUTO (LEAVE) | rule safety margin | AP §5.3 |
| `NEAR_START` | `(57, 9, −90°)` | TESTED | `VA:PreloadRouteAuto` l.58-60 |
| `FAR_START` | `(8.96, 131.19, 90°)` (BETA Park start, red wall, tile A6) or ALP "inside the LZ, front against the wall" | DECISION D21 | `BETA:ParkAuto`, ALP |

### 2.2 `hardware/RobotHardware`

| Source method @ ref | New method | Notes |
|---|---|---|
| `VA:RobotHardware(HardwareMap)` + `getDevice(primary, legacy)` @ `a7b8f4c` | `RobotHardware(HardwareMap map)`, `private <T> T getDevice(Class<T>, String name, String... legacyNames)` | Names from `RobotConstants` |
| `VA` optional `tryGet` for `limelight`, `huskyLens`, `nectarAlertLight` | `hasLimelight()`, `hasHuskyLens()`, `hasNectarAlertLight()` | — |
| `MASTER:RobotHardware` `pinpoint` `tryGet` @ `37b7a16` | `hasPinpoint()` (read-only check; Pedro owns the device) | Do not create a second driver object in AUTO |
| `VA` bulk caching (`LynxModule` AUTO) | `clearBulkCache()` (if MANUAL mode is chosen) | — |
| `VA` voltage sensor | `getBatteryVolts()` | — |
| `FLOWER:servoLeft/servoRight@329450` | `flowerArmLeftServo`, `flowerArmRightServo` | Only if D14 = yes |

### 2.3 `subsystems/`

**`Subsystem`** (new interface; no legacy source): `void periodic(); void addTelemetry(Telemetry t); void stop();`

**`Drivetrain`** — base `VA:subsystems/MecanumDrive@746e93` (a7b8f4c)

| Source @ ref | New |
|---|---|
| `drive(fwd, strafe, turn)` | `drive(double forward, double strafe, double turn)` |
| `driveRamped(...)` | `driveRamped(double forward, double strafe, double turn)` |
| `setPowerLimit`, `setBrake`, `setSlowMode`, `setFastMode` | `setPowerLimit(double)`, `setBrake(boolean)`, `setSlowMode()`, `setFastMode()` |
| `isBraking`, `isSlowMode` | `isBraking()`, `isSlowMode()` |
| `getWheelPositions`, `resetEncoders` | `getWheelPositions()`, `resetEncoders()` |
| `MASTER:MecanumDrive.stop()`, `getAverageForwardCm()` @ `37b7a16` | `stop()`, `getAverageForwardCm()` |
| `applyRamp` (private) | uses `util/SlewRateLimiter` |
| — | `periodic()`, `addTelemetry(t)` |

**`GuardedStraightMove`** — `ALP:subsystems/GuardedStraightDrive@f7d1fd` (e455142): `driveStraightCm(distanceCm, power, timeoutSec)` → step API `start(double distanceCm, double power, double timeoutSec)`, `periodic()`, `isDone()`, `getResult()`; `getLastPodTravelCm()`, `getLastWheelTravelCm()` unchanged. Pod reading from the Pedro follower pose.

**`Intake`** — `VA:subsystems/Intake@0aca19`

| Source | New |
|---|---|
| `update(collect, eject)` | `collect()`, `eject()`, `idle()` (set state) + `periodic()` (ramp) |
| `stop()`, `getState()`, `getPower()` | `stop()`, `getState()`, `getPower()` |
| `VA:PedroAutoBase` intake wiggle | `shake()`, `isShaking()` |
| — | `addTelemetry(t)` |

Constants: `INTAKE_POWER 0.75` (D8), ramp `0.15`, direction `REVERSE` TESTED.

**`Indexer`** — `VA:subsystems/Indexer@74d3a7`

| Source | New |
|---|---|
| `update(feed, eject, isPaused)` | `feed()`, `eject()`, `idle()`, `setPaused(boolean)` + `periodic()` |
| `checkForJam()` | `checkForJam()` (private) |
| `getState()`, `getPower()`, `getTotalUnjams()`, `getTotalJamStops()` | same + `isJammed()` |
| `stop()` | `stop()` |

States IDLE / FEEDING / PAUSED / EJECTING / UNJAMMING / JAMMED (unchanged). Activation: with either flywheel (VA) — D9.

**`Launcher`** — base `VA:subsystems/Shooter@ac639e`, + `MASTER:Shooter@ec491a`

| Source @ ref | New |
|---|---|
| `VA fireP()/reverseP()/stopP()` | `spinUpPollen()`, `reversePollen()`, `stopPollen()` |
| `VA fireN()/reverseN()/stopN()` | `spinUpNectar()`, `reverseNectar()`, `stopNectar()` |
| `VA stop()` | `stop()` |
| `VA setTargetRpmP(rpm)` | `setPollenTargetRpm(double)` |
| `MASTER setTargetRpm(rpmP, rpmN)` | `setTargetRpm(double pollenRpm, double nectarRpm)` |
| `VA getTargetRpmP/N`, `getCurrentRpmP/N` | `getPollenTargetRpm()`, `getNectarTargetRpm()`, `getPollenRpm()`, `getNectarRpm()` |
| `VA isPollenReady()`, `isNectarReady()` | same |
| `MASTER atTargetRPM()` | `isAtTargetRpm()` (built on the two above) |
| `VA handleRpmTuning(4 dpad bools)` | `adjustPollenRpm(double)`, `adjustNectarRpm(double)` |
| `VA compensateForVoltage()`, `applyPidf(kf)` | private, called from `periodic()` |
| `VA getAppliedKf()`, `getStateP/N()` | `getAppliedKf()`, `getPollenState()`, `getNectarState()` |
| `VA update(8 bools)` | **removed** (TeleOp maps buttons to the calls above) |
| — | `addTelemetry(t)` |

**Placer** — **not created.** No placing mechanism is confirmed (RP §1 item 7, Q14). If D14 = yes, add `subsystems/FlowerArm` from `FLOWER:FlowerArm@428e23`: constructor `FlowerArm(RobotHardware)`, `deploy()`, `retract()`, `toggle()`, `isDeployed()`, `periodic()`; constants `FLOWER_ARM_REST 0.0`, `FLOWER_ARM_DEPLOYED 0.5` (`TODO: TUNE`); bind to a free gp2 button (both gp1 bumpers are taken by intake).

**`ColorSensing`** (HuskyLens) — `VA:vision/BallVision@263911` + `VA:vision/BallIntakeCounter@f80c96`

| Source | New |
|---|---|
| `BallVision.update()` | `periodic()` |
| `getBalls()`, `getClosestBall()` | `getBalls()`, `getClosestBall()` |
| `getAssistTurn()` | `getAssistTurn()` |
| `isConnected()` | `isConnected()` |
| `BallIntakeCounter.update(isIntaking)`, `getCount(color)`, `getTotal()`, `summary()` | `countWhileIntaking(boolean)`, `getBallCount(BallColor)`, `getTotalBalls()`, `getSummary()` |
| — | `isNectarAtIntake()` (used by `Lights`) |

**`Vision`** (Limelight facade) — `VA:vision/HiveVision@1a134f` + helpers

| Source | New |
|---|---|
| `HiveVision.update()` | `periodic()` |
| `getBestPose()`, `getPose(cell)`, `getAllianceTarget()` | same |
| `getUpwardCell()`, `getTipCount()` | `getUpwardCell()`, `getTipCount()` |
| VA TIP check (auto base) | `isCellDown(HiveCell cell, double goneSec)` |
| `isConnected()`, `stop()` | `isConnected()`, `stop()` |
| `HiveAlignController.update/alignTo/isAligned/reset` (VA) + `syncOdometryWithVisibleHive/getAimErrorDegrees` (NCAM `7828c9`) | `HiveAligner.alignTo(HiveCell)`, `isAligned()`, `reset()`, `syncOdometry()`, `getAimErrorDeg()` |

**`Lights`** — `VA:IndicatorLight@17e842` + `MASTER@98d9b4` + `VA:NectarAlertLight@b99610`: `setRed()`, `setBlue()`, `setYellow()`, `setWhite()`, `setGreen()`, `setOff()`, `blink(double color)`, `showAlliance(Alliance)`, `showLauncherState(boolean isAligning, boolean isReady)`, `updateNectarAlert(boolean isNectarSeen)`, `periodic()`.

### 2.4 `pathing/`

| Class | Source @ ref | Content |
|---|---|---|
| `PedroConstants` | `VA:pathing/PedroFollowerFactory@d937b5` | `DRIVETRAIN_CONFIG` (names from `RobotConstants`, directions, `manualBrakeMode true` TESTED); `LOCALIZER_CONFIG` (`pinpoint`, 4-bar pod, X FORWARD, **Y REVERSED** TESTED, `xPodOffset −6.79 in`, `yPodOffset −0.72 in` **FITTED**, D4); `FOLLOWER_CONFIG` (Foresight, **all `TODO: TUNE`**: translational P 0.03 → 0.1 above 2.5 in, coast/brake PF 0.01, heading P 1.0, heading brake (0.1, 0.01), linear brake 0.05, quadratic 0.001, max fwd 60 / strafe 45 in/s, decel 40/40, `maxPathSpeed 0.6` until AutoTune) |
| `PedroFollowerFactory` | same | `create(HardwareMap)` |
| `OdometryDriver` | `VA:PedroAutoBase.driveTo/driveThrough/driveLeg/turnTo/holdTarget/settle` | Non-blocking step API on `follower.manual()`; constants in `DriveConstants` |
| `FieldDrawing`, `PoseLogger` | VA | unchanged (PoseLogger → `util/logging`) |
| `paths/*` | see §4 | one class per route, each with `build(PoseFactory)` and a `…Straight` copy |
| `tuning/*` | `VA:pathing/tuning/*` | unchanged |

### 2.5 `statemachine/`

| Class | Source @ ref | Content |
|---|---|---|
| `StateMachine<S extends Enum<S>>` | new (pattern from VA `Indexer`) | `setState(S)`, `getState()`, `getTimeInStateSec()`, `isIn(S)` |
| `ShootingSequence` | `VA:PedroAutoBase.shootPollenPreload@b24b02` + `MASTER:shootWhenReady` | States SPIN_UP → WAIT_READY → FEED_ONE → RECOVER → DONE; `start(int balls, double rpm, double maxSec)`, `periodic()`, `isDone()`, `getShotsFired()` |
| `CollectSequence` | `VA:collectBalls` | `start(double maxSec, BallColor... colors)`, `periodic()`, `isDone()`, `getBallsCollected()` |
| `TipWatch` | `VA:PreloadRouteAuto` early watch + TIP check | `start(HiveCell, double watchSec, double goneSec)`, `isCellDown()`, `isDone()` |
| `AutoRoutine` | new | ordered list of steps; each step has `estimateSec()`; drives the time-aware logic (§5) |

### 2.6 `opmodes/`

| Package | Classes | Source |
|---|---|---|
| `teleop` | `TeleOpBase`, `TeleOpRed`, `TeleOpBlue` | `VA:MainTeleOp@ad3c7d` (bindings unchanged: gp1 LB collect / RB eject, LT brake, A/Y slow/fast; gp2 RT/LT fire pollen/nectar, RB/LB reverse, D-pad ±50 RPM, X/Y/B shot marks). Pose from `PoseStorage` if present |
| `auto` | `AutoBase`, `AutoRedNearPreloadPark`, `AutoBlueNearPreloadPark`, `AutoRedNearCycle`, `AutoBlueNearCycle`, `AutoRedFarCycle`, `AutoBlueFarCycle`, `AutoSafePark`, `AutoSafeParkEncoders` | §5 |
| `tuning` | `TestOdometrySquare`, `TestPedroPose`, `TuneHiveVision`, `TestDriveWheels`, `TestLauncherRpm` (new: hold an RPM and log it) | VA calibration + test OpModes |

### 2.7 `util/` and tests

| Class | Source @ ref | Content |
|---|---|---|
| `Alliance` | `VA:vision/Alliance@fcfd12` + `VA:AlliancePoses@582d51` | `RED`, `BLUE`; `getScoringCell()`, `getOtherCell()`; `toFieldAbsolute(Pose)` (identity for RED, rotate 180° about (72, 72) for BLUE) |
| `MatchTimer` | `VA:autoSeconds()` + `TA:MatchClock@125df22` | `start()`, `getElapsedSec()`, `getRemainingSec()`, `hasTimeFor(double sec)`; `AUTO_LENGTH_SEC 30`, `SAFETY_MARGIN_SEC 1.0` (`TODO: TUNE`) |
| `GamepadEx` | VA edge detection in `Shooter.handleRpmTuning` + `MainTeleOp.shapeStickInput` | `readButtons()`, `isPressed(Button)`, `wasJustPressed(Button)`, `getShapedLeftX/Y()`, `getShapedRightX()` |
| `PidfController` | `LUISAUTO:autosubsystems/Pid@6aeb44` (derivative on measurement, anti-windup, dt clamp) + VA `computeAxisOutput` PD | `calculate(double setpoint, double measurement)`, `reset()`, `setGains(p, i, d, f)` |
| `SlewRateLimiter` | ramp logic duplicated in `VA:Intake`, `Indexer`, `MecanumDrive.applyRamp` | `calculate(double target, double dtSec)`, `reset(double value)` |
| `PoseStorage` | `NCAM:PinpointOdometry.saveCurrentPose@13a0c3` (idea) | `save(Pose)`, `hasPose()`, `getPose()`, `clear()` |
| `DataLogger` | `VA:LogFiles@9c9858` + the 4 VA CSV loggers | `open(String folder, String name, String header)`, `logRow(Object...)`, `close()`; files under `/sdcard/FIRST/…` as on VA |
| `BatteryMonitor` | `VA@2c6210` | unchanged |
| Tests (`src/test`) | VA `HiveGeometryTest`, `HiveOrientationTest`, `HiveStateTrackerTest` | keep; add `SlewRateLimiterTest`, `PidfControllerTest`, `MatchTimerTest` (with a fake clock), `AllianceTest` (rotate180), `CycleBudgetTest` (§5), `PathsTest` (every path ends in the LZ, stays at x ≤ 72 − half-width, avoids the HIVE box) |

---

## 3. Conflicting constants

| Constant | Chosen value | Alternatives (ref) | Evidence | Status |
|---|---|---|---|---|
| Intake direction | **REVERSE** | FORWARD (MASTER, NCAM, DEV) | `927e4df`: "on the robot FORWARD spun outward … the autonomous expelled the preloaded balls" (RP §4.1) | TESTED |
| Intake collect button | **gp1 LB** (eject RB) | gp1 RB collect (MASTER); gp1 RT (NCAM); LT (FLOWER, double-bound bug) | VA TeleOp, physically the same as MASTER with direction flipped (RP §7) | TESTED |
| Intake power | **0.75** (tunable) | 1.0 (`luis/*`, `mar/duopodrive`, 2026-11-22 "run intake at 100%") | 0.75 set after the first physical test (`bc00121`); all VA field tests ran at 0.75. 1.0 is the newest intent but was never on the tested line | DECISION D8 |
| Indexer DS name | **`indexerMotor`** with fallbacks `alejandro`, `indexer` | `alejandro` (develop line), `indexer` (MASTER) | VA lookup + "Confirmed with the team (2026-11-15): alejandro = indexer" | DECISION D2 |
| Indexer trigger | **either flywheel** | nectar only (MASTER TeleOp) | VA field tests; POLLEN-only scoring needs it on pollen | DECISION D9 (confirm) |
| Drive zero power / mode | **BRAKE / RUN_WITHOUT_ENCODER** | FLOAT + RUN_USING_ENCODER (MASTER) | `64dfe81` | TESTED |
| Launcher PIDF | **P 30, I 0, D 0, F 15 @ 12 V, voltage-compensated** | (10, 0.5, 0, 12) fixed (MASTER) | Tuned from logs; I = 0.5 caused 3470 RPM overshoot | TESTED |
| Pollen RPM TeleOp | **3000** | 2900 (MASTER, develop), 3200 (LUISAUTO) | "at 2900 … 11 of 25 scored"; 3000 "scored 29" | TESTED |
| Pollen RPM auto preload | **2950** | 2900 (MASTER `Constants`) | "both shots went long at 3000" (11-23); 4/4 at 2950 | TESTED |
| Pollen RPM other cell | **2950** | 2800 | "2800 left 2 of 3 short" (11-24 16:56) | FITTED, `TODO: TUNE` |
| Nectar RPM | **3600** | 4500 = 0.75 × 6000 (MASTER), 3200 (NCAM), 2600 (LUISAUTO) | "4500 was unreachable" at ~11 V | TESTED (not used for scoring) |
| Launcher ticks/rev | **28** | 537.7 (`mar/shoters` `e4c326`), 384.5 (LUISAUTO) | 6000 RPM direct drive (INFERRED) | DECISION D7 |
| Pinpoint Y pod direction | **REVERSED** | FORWARD (MASTER, NCAM — NCAM negates Y in software) | `927e4df`: "with FORWARD the robot strafed right while the Pinpoint read left" | TESTED |
| Pod offsets (Pedro) | **x −6.79 in, y −0.72 in** | 0 / 0 (MASTER); NCAM X 65 mm (2.56 in) / Y −170 mm (−6.69 in) (measured, other axis convention); LUISAUTO −84 / −168 mm | Fitted from a 92° spin, residual 0.08 in (`12377ee`); NCAM looks axis-swapped | FITTED, DECISION D4 |
| Odometry distance scale | **1.00** (Pedro has no scale field here) | 1.01 (NCAM, 101 cm read for 100 cm) | NCAM only; AP §2.3 | `TODO: TUNE` via AutoTune Pinpoint (D4) |
| Pedro follower gains | **placeholders**, `maxPathSpeed 0.6` | none (identical everywhere) | never tuned (AP §0) | `TODO: TUNE` (D6) |
| Robot size | **18.0 × 18.0 in** planning envelope | 17.83 W × 16.54 L (MASTER `Constants`), 17.5 W × 18.0 L (NCAM) | VA tested start pose assumes 18 × 18 (`START_WALL_DISTANCE_CM 22.86` = 9 in); 18 is the legal maximum, so it is the safe choice for clearance checks | DECISION D16 (measure) |
| Field size | **144.0 in** | 141.5 in (Pedro docs example) | Team code (MASTER, NCAM, VA `365.76 cm`); only matters for `rotate180` (center 72 vs 70.75) and wall offsets | DECISION D19 |
| Alliance transform | **identity (same poses), rotate 180° only for absolute drawing** | `mirrorX(72)` blue-authored (BETA/MASTER), red-authored (AS) | 180° rotational field (AP §5.2); `d06ea2d` | DECISION D13 (**high risk**) |
| HuskyLens learned IDs | **POLLEN 1, RED NECTAR 2, BLUE NECTAR 3** | POLLEN 1, BLUE 2, RED 3 (BALLS) | VA newest, used by `NectarAlertLight`; retrain the camera to match | DECISION D11 |
| HIVE tag spacing fallback | **0.1143 m** | 0.0825 m (NCAM = tag size) | VA tested 3D reconstruction | DECISION D17 |
| Camera pitch | **40°** | — | guess, "measure on the robot" | `TODO: TUNE` (D17) |
| HIVE align setpoint | **1.03 m fwd / 0.08 m strafe** | 0.45 m (MASTER) | measured pollen spot 11-20 | TESTED (align still disabled) |
| Light green | **0.50** | none on VA | MASTER `98d9b4` | `TODO: TUNE` |
| Light yellow | **0.42** | — | "TODO fine-tune" | `TODO: TUNE` |
| Encoder ticks/cm (fallback auto) | **16.5 fwd / 18.5 strafe** | 16.457 (theory from 537.7 / 32.67 cm) | `VA:SafeMoveAuto` estimate | `TODO: TUNE` |
| Start pose near | **(57, 9, −90°)** | (60, 8.52, −90°) Tick; (56, 8.52, −90°) Center Lane / STC; (56, 8, 90°) Outer Lane | VA tested; corner on tile vertex (W, 0) | TESTED |
| Path timeout | **8 s** | 5 s (BETA) | VA; 5 s may cut a 90 in leg at the 0.6 cap (AP §5.3) | in use |

---

## 4. Paths plan

Rules:
- Keep every original route as a path class. Add a `…Straight` copy: same start and end, one `Paths.line(start, end)`, same heading interpolation (AP §7).
- All legacy paths are already `BezierLine`s. Only multi-segment chains really change geometry in the straight copy.
- Each class lives in `pathing/paths/`, is authored in the alliance-relative (= red-absolute) frame, and is checked by `PathsTest`.
- Flags: **HIVE** = the robot footprint (half-width 9 in, corner radius 12.7 in) enters the HIVE frame box x 47.3–96.7, y 52.5–91.5 (G417 risk, TO VERIFY). **G402** = the footprint crosses x = 72 into the opponent's half in AUTO (number TO VERIFY). **WALL** = ends < 2 in from a wall (LEAVE risk). **FLOWER** = within 5 in of a FLOWER.

| # | Original (ref) | Segments | New class (original) | Straight copy | Flags on the original | Flags on the straight copy | Use |
|---|---|---|---|---|---|---|---|
| 1 | `VA:PreloadRouteAuto` park legs | shootSpot (57, 12.94) → backOff2 (57, 14.91) → right1 (33.38, 14.91) → longBack (33.38, 105.46) → right2 (9.76, 105.46), heading −90 constant | `PreloadParkPaths` (4 lines) | `PreloadParkPathsStraight`: (57, 14.91) → (9.76, 105.46) | WALL marginal (right2 edge ≈ 0.8 in from the west wall: move to x ≥ 11) | **HIVE marginal**: at y 52.5 the center is at x ≈ 37.4, so the corner reaches ≈ 50 > 47.3. **Do not use** | Primary (TESTED) |
| 2 | `VA:PreloadCollectAuto` collect | shootSpot → turn to 180 → collectStart (33.38, 12.94) → collectEnd (9.76, 12.94) at 0.2 power | `PreloadCollectPaths.collectRow` | identical (already one line) | collectEnd < 1 in from the west wall; turning there sweeps corners into the wall (VA Javadoc): keep the exit waypoint | same | Near Cycle |
| 3 | VA collect → other cell | collectEnd → collectExit (23.93, 29.87) → northWaypoint (33.38, 110.18) → otherSpot (57, 125.16, 90) | `OtherCellPaths.fromCollect` | `OtherCellPathsStraight.fromCollect`: (9.76, 12.94) → (57, 125.16) | FLOWER (otherSpot shortened 15 cm for the back-wall FLOWER, keep) | **HIVE** (center x ≈ 42.8 at y 91.5, edge ≈ 52). Also turns at the wall. **Do not use** | Near Cycle, after TIP |
| 4 | VA shooting spot → other cell | shootSpot → southWaypoint (33.38, 40.50) → northWaypoint → otherSpot | `OtherCellPaths.fromShootSpot` | `…Straight`: (57, 12.94) → (57, 125.16) | — | **HIVE** (straight through x = 57). **Do not use** | Near Cycle |
| 5 | VA other cell → park | otherSpot → otherPark (9.76, 105.46, 90) | `OtherCellPaths.toPark` | identical | WALL marginal (x ≥ 11) | same | Near Cycle |
| 6 | `BETA:TickAuto` T1 | (60, 8.52, −90) → shoot1 (60, 24, −85) | `TickPaths.offTheWall` | identical | start 0.25 in from the wall ("touching"? TO VERIFY) | same | archive / Far re-route |
| 7 | BETA Tick T2 | (60, 24, 180) → corner (9.77, 16, 180) | `TickPaths.intoCorner` | identical | turn at (60, 24) sweeps to x ≈ 72.2: **G402 marginal** | same | archive |
| 8 | BETA Tick T3 (chain) | corner → crossBottom (60, 30) → shoot2Approach (60, 120, 180) | `TickPaths.sidewaysToShootSpot` | `TickPathsStraight`: (9.77, 16) → (60, 120) | **HIVE (drives under it, x = 60, y 30 → 120)** | **HIVE (diagonal through it)** | **Not for competition.** Re-route as `TickPathsWest` via x ≈ 33 (row 1 waypoints) |
| 9 | BETA Tick T4 | (60, 120, 90) → park (9.92, 108, 90) | `TickPaths.sidewaysToPark` | identical | WALL marginal (edge 1.0 in) | same | archive |
| 10 | `BETA:CenterLaneAuto` C1 | (56, 8.52, −90) → (56, 24, −96) | `CenterLanePaths.offTheWall` | identical | — | — | archive |
| 11 | BETA Center Lane C2 | (56, 24) → corner (9.77, 12, 180), linear −96 → 180 | `CenterLanePaths.intoCorner` | identical | heading may turn the long way (TO VERIFY) | same | archive |
| 12 | BETA Center Lane C3 | corner → (9.77, 30, 180) | `CenterLanePaths.upTheWall` | identical | front ≈ 1.5 in from the west wall near the FLOWER (1.9, 48): **FLOWER** | same | archive |
| 13 | BETA Center Lane C4 (chain) | (9.77, 30) → crossBottom (60, 40, 90) → shoot2 (60, 120, 90) | `CenterLanePaths.underTheStructure` | `CenterLanePathsStraight`: (9.77, 30) → (60, 120) | **HIVE (under it)** | **HIVE** (center x ≈ 44 at y 91.5) | **Not for competition** (re-route west) |
| 14 | BETA Center Lane C5 | (60, 120) → park (9.92, 108, 90) | `CenterLanePaths.sidewaysToPark` | identical | WALL marginal | same | archive |
| 15 | `BETA:OuterLaneAuto` O1–O6 | start (56, 8, 90) → backOff (56.04, 14.11) → nearCorner (9.17, 9.11) → startColumn (54.81, 16.84) → shootPose (54.85, 20.29, −97) → outerWall (12.68, 79.19) → park (10.08, 118.67) | `OuterLanePaths` (6 lines) | identical per segment; whole-route straight start → park is not useful | nearCorner is a corner sweep (wall); O6 park y 118.67 is at the LZ edge: end at y ≤ 117 | — | `Near Outer Lane` (after a field test) |
| 16 | `BETA:ParkAuto` P1 | (8.96, 131.19, 90) → (9.5, 102.45, 90), reverseTangent | `SafeParkPaths.farStartToLoadingZone` | identical | **WALL** (edge 0.59 in): end x ≥ 11 | same | `AutoSafePark` (far start) |
| 17 | `BETA:ShootTurnCollectAuto` | (56, 8.52, −90) back 5 in → (56, 13.52), turn 180, → (56, 37.52, 90) | `ShootCollectPaths` | identical | **no PARK**; add `toPark` via row 1 legs | same | idea merged into Near Cycle |
| 18 | `AS:ShootAndCollectAuto` | (56, 8, −95) turn → (56, 8, 85) → (58.09, 31.91, 85) | merged into #17 | — | **WALL** (turns in place at y = 8: the collision fixed in BETA `206fa84`) | — | DISCARD as a route |
| 19 | `VA:OdometrySquareTestAuto` | (56, 8) → (56, 32) → (32, 32) → (32, 8) → start | `TestSquarePaths` | n/a (test) | start at the wall | — | Tuning |
| 20 | `ALP:LeaveParkAuto` | 10 cm straight from inside the LZ, front to the wall | `SafeParkPaths.leaveLoadingZone` (encoder/guarded move, not Pedro) | identical | must end ≥ 2 in off the wall: 10 cm = 3.9 in OK | — | `AutoSafePark` |
| 21 | `VA:SafeMoveAuto` | back 40 cm, left 40 cm (encoders) | `SafeParkPaths.encoderLeave` (not Pedro) | identical | no PARK unless started in the LZ | — | `AutoSafeParkEncoders` |
| 22 | Start Routine, Center Lane (old), Top Wall (AS, VA, NCAM), Renata Auto-test | — | **not migrated** (poses kept in `docs/archive/`) | — | FLOWER (Start Routine topApproach y ≈ 141); **HIVE + G402** (old Center Lane, spin to x ≈ 75.5); **WALL** + 180° spin (Top Wall, Auto-test) | — | DISCARD |
| 23 | NEW `FarCyclePaths` (for §5) | farStart → (33.38, 110.18) → otherSpot (57, 125.16, 90); fallback → row 1 legs reversed → shootSpot (57, 12.94, −90); back to LZ | `FarCyclePaths` | `FarCyclePathsStraight`: farStart → otherSpot is a short open diagonal north of the HIVE | built only from VA waypoints already driven on the field | straight copy stays at y > 105, north of the HIVE box (91.5) + 9 in: OK | Far Cycle (untested, D21) |

---

## 5. Autonomous plan

All autos share `AutoBase` (odometry guard, alliance, logging, `MatchTimer`, `PoseStorage.save()` at the end). **Same poses for both alliances** (D13). Red and blue OpModes differ only in `Alliance` (HIVE tag IDs, light color, dashboard drawing). Group `"Competition"`, `preselectTeleOp` = the matching `TeleOp Red/Blue`.

| OpMode (DS label) | Classes | Start | Routine | Status |
|---|---|---|---|---|
| `Red Near Preload Park` / `Blue Near Preload Park` | `AutoRedNearPreloadPark`, `AutoBlueNearPreloadPark` | NEAR (57, 9, −90) | LEAVE 10 cm → shoot 4 POLLEN at 2950 RPM (max 10 s) → `PreloadParkPaths` → park | **TESTED 4/4, 12–14 s.** Safest scoring auto |
| `Red Near Cycle` / `Blue Near Cycle` | `AutoRedNearCycle`, `AutoBlueNearCycle` | NEAR | Preload volley → early TIP watch (≤ 4 s) → **cycle loop** (collect row → shoot at the up cell: our scoring cell, or the other cell via the west waypoints) → park | Partly tested (VA PreloadCollect logs 11-23/24) |
| `Red Far Cycle` / `Blue Far Cycle` | `AutoRedFarCycle`, `AutoBlueFarCycle` | FAR (red wall near the LZ, D21) | Drive `FarCyclePaths` to otherSpot → look for the up cell (Limelight `getUpwardCell`) → shoot preload there, or go down the west lane to shootSpot and shoot → if time, one collect cycle → park in the LZ | **Untested.** Built from VA waypoints |
| `Safe Park` | `AutoSafePark` (INIT: gp1 D-pad picks NEAR / FAR / IN-LZ; start delay 0–20 s in 1 s steps from ALP) | any | IN-LZ: `GuardedStraightMove` 10 cm (LEAVE 3 + PARK 5). FAR: `SafeParkPaths.farStartToLoadingZone`. NEAR: `PreloadParkPaths` without shooting | ALP + BETA Park + VA park legs |
| `Safe Park Encoders` | `AutoSafeParkEncoders` | NEAR or IN-LZ | VA `SafeMoveAuto` legs on wheel encoders only, for when the Pinpoint fails | Fallback, ticks/cm `TODO: TUNE` |
| `Near Outer Lane` (later) | `AutoRedNearOuterLane`, … | (56, 8, 90) | BETA Outer Lane, re-enabled | Test group until field-tested |

### Time-aware logic (`AutoRoutine` + `MatchTimer`)

Each step has an estimate: `estimateSec()` = drive distance / `CRUISE_IN_PER_SEC` + turn angle / `TURN_DEG_PER_SEC` + fixed overheads (spin-up, `FEED_SEC_PER_BALL`). Seeds come from `TA:TimeModel` (`AUTO_PERIOD_S 30`, `SAFETY_MARGIN_S 1.0`, overheads) and from the measured VA times (preload + park = 12–14 s). All `TODO: TUNE` from the pose logs.

```
after each scoring step:
  parkSec  = estimateParkFrom(currentPose)            // legs to the LZ from here
  cycleSec = estimateCycle(next target cell) + estimateParkFrom(cycleEndPose)
  if timer.getRemainingSec() - cycleSec >= SAFETY_MARGIN_SEC
        and cycles < MAX_CYCLES and ballsAvailable:
      run another cycle (collect → shoot)
  else:
      park now
during any step:
  if timer.getRemainingSec() < parkSec + SAFETY_MARGIN_SEC: abort the step (stop intake/launcher) and park
hard ceilings (TESTED values from VA, kept as a backstop):
  collect only if it starts before 11.0 s; other cell only before 15.0 s;
  last volley ends by 22.0 s (scoring cell) / 26.5 s (other cell); parked by 30 s
```

`MAX_CYCLES` = 1 for now (a 4-ball row per side, INFERRED capacity 4). `CycleBudgetTest` checks the decision with a fake clock: at 14 s remaining with a 12 s cycle + 4 s park it must park, and at 20 s it must cycle.

---

## 6. Hardware config rename table (PROPOSED — needs team approval)

**PROPOSED, NOT APPROVED.** It changes the Driver Station configuration on every Control Hub. It follows RP §12 (do not duplicate the reasoning). Default = keep the names VA already adopted, so the tested hub needs no change.

| Old name(s) | VA name (in use on the tested hub, INFERRED) | Proposed name | Alternative | Code fallback kept until D2 |
|---|---|---|---|---|
| `FL`, `FR`, `BL`, `BR` | `frontLeftDrive`, `frontRightDrive`, `backLeftDrive`, `backRightDrive` | same as VA | `frontLeft` … (D15) | yes (Pedro itself has no fallback) |
| `intake` | `intakeMotor` | `intakeMotor` | — | yes |
| `alejandro`, `indexer` | `indexerMotor` | `indexerMotor` | — | yes (both) |
| `P`, `shooterP`, `shooterPollen` | `pollenShooterMotor` | `pollenLauncherMotor` | keep `pollenShooterMotor` (D15) | yes |
| `N`, `shooterN`, `shooterNectar` | `nectarShooterMotor` | `nectarLauncherMotor` | keep `nectarShooterMotor` (D15) | yes |
| `indicator` | `indicatorLight` | `indicatorLight` | — | yes |
| — | `nectarAlertLight` | `nectarAlertLight` | — | optional (D12) |
| `limelight` | `limelight` | `limelight` | — | — |
| `huskyLens` | `huskyLens` | `huskyLens` | `frontCamera` | — |
| `pinpoint` | `pinpoint` | `pinpoint` | — | — |
| `imu 1` | `expansionHubImu` (doc only) | `expansionHubImu` | `imu` | unused by code |
| `servoLeft`, `servoRight` | — | `flowerArmLeftServo`, `flowerArmRightServo` | — | only if D14 = yes |
| `pera`, `stopServo`, `shooterGateServo` | removed | (do not add) | `gateServo` | only if D10 = gate back |
| `lift`, `pollen2`, `nectar2`, `pollenFeeder`, `nectarFeeder` | — | (do not add) | — | speculative |

---

## 7. DISCARD list

| Item (ref) | Reason |
|---|---|
| `LUISAUTO`: `subsystems/autosubsystems/*` (9 files), 16 `opmodes/auto/{Red,Blue}*Auto`, `AutoBase` | Hardware that never existed (lift, feeder servos, `pollen2`/`nectar2`, 435 RPM, 96 mm); `SystemClock.sleep` loops; Spanish OpMode names. Only `Pid@6aeb44` is kept as the base of `PidfController` |
| `TA`: `theautonomous/*` code (`RouteRunner`, `RouteBuilder`, `RouteLibrary`, `Step`, `Phase`, `Route`, `TheAutonomous`, `AutoRobot`, `Alliance`) | Blocking, fictional hardware. Concept kept (§5) |
| `MASTER`/`BETA` `pathing/Alliance@6d3163`, `AS@116500` (mirrorX) | Wrong transform for a 180° rotational field; red default ends outside any LZ (AP §5.2) |
| `MASTER:RobotHardware@684154`, `MainTeleOp@4d35ec`, `Indexer@6a271f`, `MecanumDrive@0ae95b` (except `stop`/`getAverageForwardCm`), `MotorDiagnosticsLogger@d2a593`, `vision/*` (Mar v1) | Superseded by the tested VA versions |
| `MASTER:subsystems/AutoDrive@42327c`, `PinpointOdometry@d9e1d1`, `NCAM@86ed33/13a0c3`, `onepodrive@22503e/feb667`, `opmodes/calibration/OdometryCalibrationAuto@854c72` | Second owner of the Pinpoint next to Pedro; untested ("has to be calibrated"); replaced by `OdometryDriver` + AutoTune. Ideas kept: pod health, `saveCurrentPose` |
| `NCAM:HiveGeometry@db231c`, `MainTeleOp@4d4252`, `RedCornerToCellTestAuto@603669`, `TopWallAuto@c422f0` | Untested variant / other button map / test / duplicate |
| `NCAM:Pera@2894ef`, `StopServo@9c7b50` | Gate removed from the robot 2026-11-20 (`c1580df`); `Pera` −35° clips to 0.0 |
| `FLOWER:Indexer@715f59`, `MainTeleOp@a8f804`, `opmodes/teleop/Auto-test.java@67fce5` | Do not compile; double-bound left trigger; Ivy not declared |
| `renata/removing-Alejandro`, `renata/auto-test` | Broken (AM §0 item 6) |
| `BALLS:BallsCounting@e989e8` | Empty `setPrismPixel` stub; HuskyLens IDs conflict with VA. Idea kept in the backlog |
| `PIDB:util/PIDController@3345d1`, `MecanumDrive@5fe5c7` | No anti-windup; untested hold brake |
| `ALIGN:MecanumDrive@fcd92a` (`setDriveStraightOnly`), `ALIGN:Shooter@d0a2bb` (except `IDLE_RPM` idea) | Superseded |
| Launcher variants `68256d`, `9f3ac7`, `e7b425` (`Shooters`), `e4c326` (537.7 CPR), `203747` (open loop), `13b317` (`PidShooter`) | Gamepad inside subsystem / wrong CPR / open loop / fictional feeders |
| `subsystems/shooters/Shooters.java`, `contextshooters` (`dfe9f54`) | NPE duplicate; empty file |
| `robotHardware.java@981bda` (`1f0c541`, `0bef98e`) | File name case does not match the class |
| Routes: Start Routine, Center Lane (old), Top Wall (all 4 blobs), Shoot And Collect (as a route), Renata Auto-test | FLOWER, HIVE, G402 and wall risks (AP §5.3) |
| `AS:WPILibAuto/**` | FRC code inside an FTC repo |
| `.idea/**`, `.artifacts/**`, `cool.txt` | IDE state, agent scratch, junk |
| AGP 9.3.3 / Gradle 9.5–9.8 root build changes, `build.common.gradle` without `pickFirst` | SDK template says do not modify |
| `PROJECT_CONTEXT.md` (all versions), teamcode `readme.md` | Stale / SDK boilerplate; replaced by `docs/ROBOT_PROFILE.md` |
| Branches: `origin/CSV` + `origin/csv`, `localcopy/bm`, `localcopy/*` backups, `luis/{park,alliance,probe,naming}` duplicates, `localcopy/feature/mar/duopodriveNcam` (`c73454f`, stale) | Superseded; case clash; orphan. Archive with tags first |

---

## 8. DECISIONS NEEDED FROM THE TEAM

Each item points to the matching question in `docs/ROBOT_PROFILE.md` ("QUESTIONS FOR THE TEAM") instead of repeating it. Only the new items (D20, D21, D22) are spelled out in full. **Blocking** = code cannot be finalised without the answer.

| # | Decision | RP ref | What this matrix assumes until answered | Blocking? |
|---|---|---|---|---|
| D1 | Hub / port map | Q1 | nothing (names only) | no |
| D2 | Active DS names; one or several Control Hub configs | Q2 | VA new names + legacy fallbacks (`alejandro` **and** `indexer`) | **yes** (Pedro has no fallback) |
| D3 | Drive motors and wheels | Q3 | 312 RPM, 104 mm | no |
| D4 | Pod offsets tape measure, Y pod push test, 1.01 scale | Q4 | VA fitted −6.79 / −0.72 in, Y REVERSED, scale 1.00 | **yes** for Pedro paths |
| D5 | IMU source / hub orientation | Q5 | Pinpoint IMU only | no |
| D6 | Pedro AutoTune run, robot mass, max velocities | Q6 | placeholders + `maxPathSpeed 0.6`; VA `OdometryDriver` used for competition autos until tuned | **yes** for any Pedro-followed path |
| D7 | Motor models (intake, indexer, flywheels; 28 ticks/rev?) | Q7 | 28 ticks/rev, 6000 RPM | no |
| D8 | Intake power 0.75 vs 1.0 (direction REVERSE is settled by test) | Q8 | 0.75 | no (tunable) |
| D9 | Ball path to the two flywheels; indexer with both shooters? | Q9 | indexer with either flywheel | no |
| D10 | Any gate / stopper now? `pera` = `stopServo`? | Q10 | no gate | no |
| D11 | HuskyLens learned IDs | Q11 | 1 POLLEN, 2 RED, 3 BLUE (retrain to match) | no |
| D12 | `nectarAlertLight` / Prism strip mounted? | Q12 | optional `tryGet` | no |
| D13 | **Alliance frame: confirm the field is 180° rotationally symmetric, red = columns A–C, same poses for both alliances (no mirror)** | Q13 | identity poses, `rotate180` for drawing only | **yes — highest rule risk** (G402 / lost PARK, AP §5.2) |
| D14 | Flower arm on the robot? Any placing plan? | Q14 | no `Placer` / `FlowerArm` class | no |
| D15 | Naming style (`…ShooterMotor` vs `…LauncherMotor`, short drive names) | Q15 | keep VA names on the DS | no |
| D16 | Robot length / width / height | Q16 | 18 × 18 in envelope; no under-HIVE paths | **yes** for Tick / Center Lane |
| D17 | Limelight pitch, height, tag spacing, Full 3D | Q17 | 40°, 0.1143 m; `HiveAligner` stays disabled | no |
| D18 | Build on VA; retire `master` and `duopodriveNcam` after this merge; push `localcopy/auto-shooter` to an archive | Q18 | yes to all | **yes** (sign-off) |
| D19 | Field size 144 vs 141.5 in (manual inner dimension) | — (AP §3 item 3) | 144 | no (affects drawing / rotate only) |
| D20 | **New: HIVE cell naming vs position.** VA calls tags 30–33 `RED_SCORING` and shoots them first from the south spot (57, 12.9). It calls tags 34–37 `RED_AUDIENCE` and shoots them from the north `otherSpot` (57, 125.2). Mar's field map calls the south red cell the "audience" cell (starts UP). Which tag IDs are on which physical cell, and which starts UP? | new (related to Q13, Q17) | VA tag IDs (they scored on the field); `FieldConstants` cell names = `RED_SOUTH` / `RED_NORTH` until confirmed | **yes** for `FieldConstants` and the Far auto |
| D21 | **New: what "far" start means.** Options: (a) BETA Park start (8.96, 131.19, 90°) against the red wall, tile A6; (b) ALP "inside the LOADING ZONE, front to the wall". Is a far start legal under G304 (inside own territory, touching the wall)? Does the alliance partner usually take NEAR? | new (related to Q13, Q19) | (a), with the start pose re-checked by `AutoBase` | **yes** for the Far Cycle autos |
| D22 | **New: Tick / Center Lane under the HIVE.** Can the robot physically and legally drive under the central structure (G417)? If not, the two routes are re-routed west via x ≈ 33 and the originals stay archived | new (Q16 height) | not used in competition | no (Near autos cover the event) |
| D23 | What is MXMOQ and which auto ran there? | Q19 | — | no |
