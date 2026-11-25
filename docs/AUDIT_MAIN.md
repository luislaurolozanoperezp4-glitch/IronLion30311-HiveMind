# AUDIT_MAIN — Iron Lion #30311 · BIOBUZZ 2026-27 legacy repository

| | |
|---|---|
| Source (read-only) | `legacy/IronLion30311BIOBUZZInicioDeTemporada` |
| Refs audited | 63 refs (`origin/*` + `localcopy/*`), 41 distinct head commits, 143 commits |
| Audit date | 2026-11-24 |
| Scope | Robot / mechanism code, Gradle, docs, data. Pedro Pathing routes on `feature/autonomos-beta` are only inventoried here (a separate agent audits them in depth). FtcRobotController SDK samples ignored. |
| Method | `git for-each-ref`, `rev-list --left-right --count`, `merge-base --is-ancestor`, `ls-tree` blob hashes (dedupe), `git show`, `git diff`, `git log -L` (regex ranges; Java has no userdiff driver in this repo so `:funcname:` ranges do not resolve). No build was run: "compiles?" is a static judgement (symbol / field / filename / dependency checks). |

Short hashes are 7 chars for commits and 6 chars for blobs (`@abc123`).

---

## 0. Executive summary

1. **There are two parallel "current robot" lines that have never been merged:**
   - `origin/master` @ `37b7a16` (2026-11-22) = develop + Mar's odometry/vision work + Luis's Pedro autos (`feature/autonomos-beta`).
   - `origin/feature/turrubiates/vision-align` @ `a7b8f4c` (2026-11-24, **newest commit in the repo**) = develop + 57 commits by turrubiates that re-implement vision, rename `Alejandro`→`Indexer`, add FTC Dashboard, voltage-compensated launcher, indexer jam handling, logging, battery monitor, HuskyLens ball vision, Pinpoint odometry autos validated on the robot ("4/4 preload shots and parked in ~12-14 s").
   - Merge base of both is `afbabf1` (develop, 2026-11-06). vision-align is **18 behind / 57 ahead** of master. It does **not** contain Mar's `182e03d` ("todo sirve como debe") or any of master's Pedro autos.
2. **vision-align is the most valuable branch** for every mechanism class (Launcher/Shooter, Indexer, Drivetrain, IndicatorLight, RobotHardware, vision, logging). master is the source for Pedro auto structure, `Constants`, `PinpointOdometry`, `AutoDrive`, odometry calibration.
3. **Smaller unique contributions:** `feature/renata/flower-arm` (only FlowerArm source, does not compile as is), `feature/turrubiates/auto-leave-park` (LeaveParkAuto + GuardedStraightDrive), `feature/mar/duopodriveNcam` @ `2d92fba` (BiobuzzFieldMap, odometry-assisted HIVE aim, field notes in Spanish), `Balls-counting` (LED inventory idea), `PID` (PID hold brake), `feature/renata/auto-align` (launcher idle pre-spin + ramp idea), `localcopy/auto-shooter` (local-only, Pedro + WPILib reference).
4. **Serious hardware-truth conflicts between master and vision-align** (must be resolved on the robot before merging):
   - Intake direction: master `FORWARD` + right bumper collects; vision-align `REVERSE` + left bumper collects, with commit `927e4df fix(auto): intake direction` stating FORWARD spun outward. master's TeleOp is therefore very likely inverted.
   - Indexer DS name: master looks up `"indexer"`; robot config (per `doc/naming/rename-map.md`) was `"alejandro"`; vision-align looks up `"indexerMotor"` with fallback `"alejandro"`. master will crash on an unrenamed hub.
   - Drive motors: master `FLOAT` + `RUN_USING_ENCODER`; vision-align `BRAKE` + `RUN_WITHOUT_ENCODER` + damped decel ramp.
   - Launcher PIDF: master fixed `(10, 0.5, 0, 12)`; vision-align `P=30, F=15@12 V` voltage-compensated (`@Config`).
   - Initial RPM: master pollen 2900 / nectar 4500 (75 % of 6000); vision-align pollen 3000 / nectar 3600.
   - Pinpoint pod offsets: master Pedro factory 0/0 (TODO); vision-align `xPodOffset -6.79 in, yPodOffset -0.72 in` (fitted); Mar's `PinpointOdometry@13a0c3` X/Y = 65 mm / -170 mm (different axis convention).
   - Robot size: `Constants` 17.83 × 16.54 in; `BiobuzzFieldMap` 18.0 × 17.5 in.
   - HuskyLens color IDs: `BallsCounting` POLLEN=1, BLUE=2, RED=3; `BallVision` POLLEN=1, RED=2, BLUE=3.
5. **Abandoned / never-matching-the-robot code:** `feature/luis/autonomospaths` + `feature/theautonomous` (`autosubsystems/*`, 16 Red/Blue autos, `theautonomous/*`) target hardware the robot never had (`lift`, `pollen2`, `pollenFeeder`, `nectarFeeder` servos, 384.5 CPR motors) and use blocking `SystemClock.sleep` loops. Concepts worth keeping: time-budgeted route runner (`RouteRunner`/`TimeModel`/`MatchClock`) and anti-windup `Pid`.
6. **Broken branches:** `feature/renata/removing-Alejandro` and `flower-arm` (`hardware.Indexer` does not exist; `FlowerArm` constructor named `DualServo`), `feature/renata/auto-test` (`Auto-test.java` declares `AutoPath`, and the branch has no Pedro dependency), `localcopy/develop-button-shooter` (`shooterP/shooterN` never assigned → NPE), `localcopy/feature/mar/shoters` (flywheel CPR 537.7, wrong for a 6000 RPM bare motor).
7. **Repo hygiene problems:** case-colliding branches `origin/CSV` and `origin/csv` (different commits — breaks on Windows/macOS ref storage), orphan root commit `localcopy/bm`, `.idea/` and `.artifacts/` (AI agent scratch) committed, `cool.txt`, empty `contextshooters` file, FRC `WPILibAuto/` inside an FTC repo, three branches upgrading AGP to 9.3.3 / Gradle 9.5-9.8 against the SDK template's "do not modify" note.

---

## 1. Branch inventory

Legend: **b/a** = commits behind / ahead (`rev-list --left-right --count base...ref`). **Merged** = head is an ancestor of master (M) and/or develop (D). Same-hash refs are listed together (duplicates).

| # | Branch(es) (duplicates on one row) | Head | Date | Author | Last commit message | Purpose (inferred) | vs master b/a | vs develop b/a | Merged | Worth mining |
|---|---|---|---|---|---|---|---|---|---|---|
| 1 | `origin/master`, `origin` (HEAD) | `37b7a16` | 2026-11-22 | luis | Merge PR #14 feature/autonomos-beta | Integration branch: develop + odometry/vision + Pedro autos | 0/0 | 0/18 | — | **YES** – baseline, Pedro autos, Constants, Pinpoint, AutoDrive |
| 2 | `origin/feature/turrubiates/vision-align` | `a7b8f4c` | 2026-11-24 | turrubiates | feat(auto): watch up to 4 s after the preload for the slow TIP | Full rework: vision, launcher, indexer, logs, Dashboard, odometry autos (robot-tested) | 18/57 | 0/57 | no | **YES – most valuable** (winner for almost every mechanism class) |
| 3 | `origin/feature/autonomos-beta`, `localcopy/feature/autonomos-beta` | `206fa84` | 2026-11-22 | luis | Fix wall collisions found in path simulation (Park, Shoot Turn Collect) | Pedro routes (Tick, Center Lane, Park, Shoot Turn Collect) | 3/0 | 0/15 | M | NO – fully in master (Pedro agent audits it) |
| 4 | `localcopy/auto-shooter` (**local only**) | `d4642a0` | 2026-11-22 | luis | Add Shoot And Collect autonomous and WPILib reference routine | Pedro "Shoot And Collect" + FRC WPILib reference project | 6/3 | 0/15 | no | LOW – `ShootAndCollectAuto`, `Shooter.atTargetRPM` (also in master); WPILib = reference only. Push or archive before laptop loss |
| 5 | `origin/feature/mar/duopodriveNcam` | `2d92fba` | 2026-11-21 | estra (Mar) | improvements en odometria y auto apuntado… mapear coordenadas | Two-pod Pinpoint + Limelight hybrid aim, field coordinate map | 12/5 | 0/11 | no | **YES** – `BiobuzzFieldMap`, odometry-assisted aim (`HiveAlignController@7828c9`), `HiveCell` field coords, field/game notes |
| 6 | `localcopy/feature/mar/duopodriveNcam` | `c73454f` | 2026-11-19 | estra | cam and odometry calibration (worked as intended) | Stale local copy of #5 (1 commit behind origin) | 12/4 | 0/10 | no | NO – superseded by #5 |
| 7 | `origin/feature/mar/duopodrive`, `localcopy/feature/mar/duopodrive` | `38caa7f` | 2026-11-22 | luis | update | Two-pod odometry first commit + intake power 1.0 | 16/1 | 0/3 | no | NO – content in master; only change is `INTAKE_POWER 1.0` |
| 8 | `origin/feature/mar/vision`, `localcopy/feature/mar/vision` | `dae149e` | 2026-11-14 | estradamar | Merge duopodriveNcam into vision | Vision first version (merged via PR #8) | 13/0 | 0/5 | M | NO – in master |
| 9 | `origin/feature/mar/onepodrive`, `localcopy/feature/mar/onepodrive` | `898e0da` | 2026-11-11 | estra | firstcommit, this only uses 1 odometrypod | Single-pod odometry experiment | 17/0 | 0/1 | M | NO – in master |
| 10 | `origin/feature/luis/park`, `localcopy/feature/luis/park` | `a8c4215` | 2026-11-22 | luis | feat(intake): run intake at 100% power | = #11 + intake power 1.0 | 12/4 | 0/10 | no | YES (same content as #11) |
| 11 | `origin/feature/turrubiates/auto-leave-park` | `e455142` | 2026-11-15 | turrubiates | fix(auto): start Leave + Park with the intake against the wall | Deterministic LEAVE + PARK auto | 12/3 | 0/9 | no | **YES** – `LeaveParkAuto`, `GuardedStraightDrive` exist only here |
| 12 | `origin/feature/luis/alliance`, `localcopy/feature/luis/alliance` | `64a1504` | 2026-11-22 | luis | feat(intake): run intake at 100% power | = #13 + intake 1.0 | 12/5 | 0/11 | no | NO – superseded by #2 |
| 13 | `origin/feature/turrubiates/hive-alliance-target` | `c17992f` | 2026-11-15 | turrubiates | feat(teleop): optional alliance selection for HIVE auto-align | Alliance-aware HIVE target | 12/4 | 0/10 | no | NO – re-done in #2 (`8a51435`, `c1c25a3`) |
| 14 | `origin/feature/luis/probe`, `localcopy/feature/luis/probe` | `e9ef5bb` | 2026-11-22 | luis | feat(intake): run intake at 100% power | = #15 + intake 1.0 | 12/3 | 0/9 | no | NO |
| 15 | `origin/feature/turrubiates/hive-state-probe` | `6fbf6e8` | 2026-11-15 | turrubiates | feat(vision): add HIVE cluster orientation and Hive State Probe | HiveOrientation + probe OpMode | 12/2 | 0/8 | no | NO – newer version in #2 |
| 16 | `origin/feature/luis/naming`, `localcopy/feature/luis/naming` | `b9c6e12` | 2026-11-22 | luis | feat(intake): run intake at 100% power | = #17 + intake 1.0 | 18/3 | 0/3 | no | NO |
| 17 | `origin/feature/turrubiates/naming-conventions` | `ea8f5a1` | 2026-11-15 | turrubiates | docs(conventions): robot orientation and files added after the rename map | `CONVENTIONS.md` + `doc/naming/rename-map.md` | 18/2 | 0/2 | no | YES (docs) – later version in #2 |
| 18 | `origin/feature/renata/flower-arm` | `889102d` | 2026-11-15 | RenataP0911 | Flower arm implementation, servos face to face, right bumper | FLOWER arm (2 mirrored servos) + StopServo + BallsCounting | 18/5 | 0/5 | no | **YES** – only FlowerArm source (needs fixes) |
| 19 | `origin/feature/renata/removing-Alejandro` | `89d97d0` | 2026-11-15 | RenataP0911 | Removing "Alejandro" | Rename Alejandro→Indexer (broken) | 18/4 | 0/4 | no | NO – broken; done properly in #2 (`76bfab1`) |
| 20 | `origin/feature/renata/auto-test` | `7c1e75a` | 2026-11-14 | RenataP0911 | autonomo de prueba | Pedro test auto in `teleop/Auto-test.java` | 18/3 | 0/3 | no | NO – does not compile |
| 21 | `origin/Balls-counting` | `55f69ad` | 2026-11-09 | RenataP0911 | fixes to camara vision | HuskyLens ball inventory + Prism LED (stub) | 18/2 | 0/2 | no | PARTIAL – inventory/LED-slot idea only |
| 22 | `origin/PID` | `6b8ac1e` | 2026-11-09 | RenataP0911 | Update of project context | `util/PIDController` + PID wheel-hold brake | 18/2 | 0/2 | no | PARTIAL – PID-hold brake idea |
| 23 | `origin/feature/renata/auto-align` | `3e733d4` | 2026-11-07 | RenataP0911 | Button-declared | Misnamed: StopServo + launcher with idle pre-spin/ramp | 32/6 | 14/6 | no | PARTIAL – idle pre-spin + ramp (`Shooter@d0a2bb`) |
| 24 | `origin/develop`, `origin/Stop-servo`, `localcopy/develop` | `afbabf1` | 2026-11-06 | estradamar | Merge PR #7 from SIT | Develop baseline ("first physical test, everything worked") | 18/0 | 0/0 | M D | NO – fully in master |
| 25 | `origin/SIT` | `9734d9c` | 2026-11-06 | estradamar | Merge PR #6 improveChassis | Staging (develop→SIT→UAT→master flow) | 19/0 | 1/0 | M D | NO |
| 26 | `origin/csv` (lowercase) | `6f297f1` | 2026-10-31 | RenataP0911 | major fixes | MotorDiagnosticsLogger v2 | 23/2 | 5/2 | no | NO – superseded; **name collides with `origin/CSV`** |
| 27 | `origin/CSV` | `2eb4c59` | 2026-10-28 | RenataP0911 | CSV | MotorDiagnosticsLogger v1 | 23/0 | 5/0 | M D | NO |
| 28 | `origin/feature/renata/shooter` | `10331bd` | 2026-10-24 | estra | rebranching | Launcher with RPM ramp, `update(Gamepad)` | 28/4 | 10/4 | no | NO – superseded |
| 29 | `origin/feature/luis/shooters-upgraded`, `localcopy/feature/luis/shooters-upgraded` | `ff40234` | 2026-11-01 | luis | update | Open-loop % power launcher + AGP 9.3.3 / Gradle 9.5 | 28/1 | 10/1 | no | NO |
| 30 | `localcopy/feature/mar/shoters`, `localcopy` (HEAD) | `d3aa850` | 2026-11-01 | luis | autogeneral | Launcher CPR 537.7 + AGP upgrade + `.artifacts/` | 32/3 | 14/3 | no | NO |
| 31 | `localcopy/develop-button-shooter` | `dfe9f54` | 2026-11-01 | luis | autogeneral | `subsystems/shooters/Shooters.java` (NPE) | 32/1 | 14/1 | no | NO |
| 32 | `localcopy/button-shooter`, `localcopy/backup-button-shooter-previous`, `localcopy/develop-button-shooter-upgraded`, `localcopy/develop-shooters-upgraded`, `localcopy/develop-shooters-upgraded-gradle`, `localcopy/future/luis/feature-develop-shooters-upgraded-pot` (6 refs) | `fcacb1c` | 2026-10-24 | luis | autogeneral | First closed-loop launcher | 28/0 | 10/0 | M D | NO |
| 33 | `origin/feature/luis/autonomospaths`, `localcopy/feature/luis/autonomospaths` | `9cf2c66` | 2026-10-22 | luis | autogeneral | 16 Red/Blue autos on fictional `AutoHardware` | 32/2 | 14/2 | no | NO (concepts only: `Pid`) |
| 34 | `origin/feature/theautonomous`, `localcopy/feature/theautonomous` | `125df22` | 2026-11-14 | luis | Mover las notas de los autonomos junto al codigo | #33 + "THE AUTONOMOUS" time-budget route runner | 32/5 | 14/5 | no | PARTIAL – `RouteRunner`/`TimeModel`/`MatchClock` concept |
| 35 | `localcopy/backup-shooters-6307da6` | `6307da6` | 2026-10-24 | RenataP0911 | Shooters | Backup | 30/0 | 12/0 | M D | NO |
| 36 | `localcopy/backup-context-ef21c09` | `ef21c09` | 2026-10-24 | RenataP0911 | projectContext-shooters | Backup (same TeamCode tree as #35) | 29/0 | 11/0 | M D | NO |
| 37 | `localcopy/backup-develop-8f831dd` | `8f831dd` | 2026-10-16 | estradamar | Merge PR #2 intakeintegration | Backup | 32/0 | 14/0 | M D | NO |
| 38 | `localcopy/indicator-lights` | `1f0c541` | 2026-10-13 | RenataP0911 | Indicator-light | Early drive + light | 36/0 | 18/0 | M D | NO |
| 39 | `localcopy/Intake+Indexer`, `localcopy/main` | `0bef98e` | 2026-10-16 | luis | files | Adds `.idea/` only | 35/1 | 17/1 | no | NO |
| 40 | `localcopy/bm` | `e314915` | 2026-10-16 | luis | files | **Orphan root commit**, `.idea/` only | 40/1 | 22/1 | no | NO |
| 41 | `localcopy/master`, `localcopy/claude/pedro-pathing-biobuzz-analysis-a65cc9` | `160e1b9` | 2026-10-11 | estra | first commit | SDK skeleton (`readme.md` only) | 38/0 | 21/1 | M | NO |

**Duplicate summary:** 63 refs → 41 heads. Biggest clusters: `fcacb1c` (6 refs), `afbabf1` (3 refs), 15 `origin`/`localcopy` pairs on the same hash. The four `feature/luis/{park,alliance,probe,naming}` branches are each a turrubiates branch plus the same one-line "intake 100 %" commit (created 2026-11-22 13:24:34-37, i.e. scripted).

**SDK / build per branch:** every branch with code uses FTC SDK **12.0.0** (`build.dependencies.gradle`, FtcRobotController `versionName="12.0"`, `versionCode 63`). Differences:

| Gradle variant | Branches | Notes |
|---|---|---|
| deps `390cc64` – SDK only, no Pedro | develop line, renata/*, mar/*, luis/{park,alliance,probe,naming}, theautonomous, autonomospaths | `feature/renata/auto-test` imports Pedro but has no Pedro dependency → cannot compile |
| deps `496dcd9` – + `com.pedropathing:revhub:3.0.2` (dairy.foundation repo) | master, autonomos-beta, auto-shooter | |
| deps `f134131` – + FTC Dashboard `0.6.0`, Pedro `revhub:3.0.2`, Pedro `tuning:1.0.3`, Sloth repo | vision-align | verify the Dashboard `0.6.0` coordinate resolves (not built here) |
| TeamCode `48eaaed` – + `testImplementation junit:4.13.2` | vision-align, hive-state-probe, hive-alliance-target, luis/probe, luis/alliance | unit tests under `TeamCode/src/test` |
| root `937524a` – AGP 9.3.3, wrapper Gradle 9.5.0 / 9.8.0, `build.common.gradle` drops `pickFirst` | `d3aa850`, `ff40234`, `9cf2c66`, `125df22` | Deviates from SDK template ("Do not modify this yourself"); DISCARD |
| default – AGP 8.13.2, Gradle 9.1.0, compileSdk 30, minSdk 24, Java 1.8 | all others | SDK template |

---

## 2. Folder & file inventory (distinct trees worth mining)

Paths are relative to `TeamCode/src/main/java/org/firstinspires/ftc/teamcode/` unless noted. Quality 1 (poor) – 5 (production). "Compiles" = static judgement against SDK 12.0 and the branch's own dependencies.

### 2.1 `feature/turrubiates/vision-align` @ `a7b8f4c` (45 Java files + 3 tests) — primary source

| File | Blob | Purpose | Q | Compiles | Deps | Issues | Decision | Proposed name |
|---|---|---|---|---|---|---|---|---|
| `RobotHardware.java` | `ae9fef` | All device mapping; new DS names with legacy fallback, bulk caching, optional cameras | 5 | yes | SDK, Limelight3A, HuskyLens | No Pinpoint field (Pedro maps it itself); legacy fallback lacks `"indexer"` used by master; `shooterP/N` single-letter fields | KEEP→REFACTOR | `hardware/RobotHardware` (fields `pollenLauncherMotor`, `nectarLauncherMotor`) |
| `subsystems/Shooter.java` | `ac639e` | Two flywheels, closed-loop velocity, voltage-compensated kF, ready hysteresis, `@Config` | 5 | yes | Dashboard | `fireP/fireN` naming; no `setTargetRpm(p,n)` / `atTargetRPM()` that master autos call; `MAX_RPM`, `TICKS_PER_REV=28` TODO | KEEP→REFACTOR | `subsystems/launcher/Launcher` + `LauncherConstants` |
| `subsystems/ShooterLogger.java` | `5af560` | 50 Hz flywheel CSV, shot dip/recovery detection, shot marks | 4 | yes | – | – | KEEP | `diagnostics/LauncherLogger` |
| `subsystems/Indexer.java` | `74d3a7` | Feed state machine: FEEDING/PAUSED/EJECTING/UNJAMMING/JAMMED, current-based jam detect | 5 | yes | Dashboard | `FEED_POWER` not tunable | KEEP | `subsystems/indexer/Indexer` |
| `subsystems/Intake.java` | `0aca19` | 1 motor, ramp 0.15 s, 0.75 power | 4 | yes | – | power disputed (0.75 vs 1.0) | KEEP | `subsystems/intake/Intake` |
| `subsystems/MecanumDrive.java` | `746e93` | Robot-centric drive, accel/decel ramp, stop cutoff, power limit, BRAKE | 5 | yes | Dashboard | removed `driveRaw`/`stop`/`getAverageForwardCm` used by master's AutoDrive | KEEP | `subsystems/drivetrain/Drivetrain` |
| `subsystems/IndicatorLight.java` | `17e842` | goBILDA PWM RGB light, yellow + blink | 4 | yes | – | lost `setGreen()` from master | MERGE (+ green from `98d9b4`) | `subsystems/lighting/IndicatorLight` |
| `subsystems/NectarAlertLight.java` | `b99610` | 2nd light blinks NECTAR color when seen | 4 | yes | Dashboard | – | KEEP | `subsystems/lighting/NectarAlertLight` |
| `subsystems/BatteryMonitor.java` | `2c6210` | Low-battery / sag detect, drive power limit | 4 | yes | Dashboard | – | KEEP | `diagnostics/BatteryMonitor` |
| `subsystems/MatchSummary.java` | `c7a737` | One-row CSV per run (`/sdcard/FIRST/match_logs`) | 4 | yes | – | – | KEEP | `diagnostics/MatchSummary` |
| `subsystems/LogFiles.java` | `9c9858` | File-name helper | 4 | yes | – | – | KEEP | `diagnostics/LogFiles` |
| `subsystems/MotorDiagnosticsLogger.java` | `93a19c` | 8-motor CSV (current, W, torque %) every 0.75 s | 4 | yes | – | – | KEEP | `diagnostics/MotorLogger` |
| `subsystems/vision/HiveVision.java` | `1a134f` | Limelight HIVE tags → cluster poses, alliance target, TIP count | 4 | yes | Limelight3A | 367 lines | KEEP | `subsystems/vision/HiveVision` |
| `subsystems/vision/HiveAlignController.java` | `6b6f19` | PD align to cell, `ENABLED=false` until calibrated | 4 | yes | Dashboard | disabled by default; no odometry fusion (see Mar `7828c9`) | MERGE | `subsystems/vision/HiveAligner` |
| `subsystems/vision/HiveGeometry.java` | `7fbf90` | Tag → cell pose reconstruction | 4 | yes | – | unit-tested | KEEP | same |
| `subsystems/vision/HiveCell.java` | `2d9fe8` | Cell enum with alliance | 4 | yes | – | lacks field coords (Mar `63f41d`) | MERGE | same |
| `subsystems/vision/HiveClusterPose.java` | `1dc8a3` | Value object | 4 | yes | – | – | KEEP | same |
| `subsystems/vision/HiveOrientation.java` | `791c5d` | Upright/upside-down detection | 4 | yes | – | unit-tested | KEEP | same |
| `subsystems/vision/HiveStateTracker.java` | `7ef04c` | Debounced upward cell + TIP counter | 4 | yes | – | unit-tested | KEEP | same |
| `subsystems/vision/Alliance.java` | `fcfd12` | RED/BLUE + scoring cells | 3 | yes | – | duplicate concept of `pathing/Alliance` | MERGE | `common/Alliance` |
| `subsystems/vision/BallVision.java` | `263911` | HuskyLens color blocks, assist turn | 4 | yes | HuskyLens | IDs POLLEN 1 / RED 2 / BLUE 3 conflict with BallsCounting | KEEP | same |
| `subsystems/vision/BallIntakeCounter.java` | `f80c96` | Counts picked balls by color | 3 | yes | – | estimate only (documented) | KEEP | same |
| `subsystems/pathing/*` (PedroFollowerFactory `d937b5`, AlliancePoses, FieldDrawing, PoseLogger, tuning/*) | – | Pedro config, Dashboard drawing, pose CSV, AutoTune | – | yes (judged) | Pedro, Dashboard | Pedro agent | → Pedro agent | – |
| `opmodes/teleop/MainTeleOp.java` | `ad3c7d` | Full TeleOp: alliance in INIT, auto-align while firing, feed gating, shot marks, logs | 4 | yes | Dashboard | 450 lines; loop OK (no sleep), `idle()` only in INIT | REFACTOR (split into `TeleOpBase` + `TeleOpRed`/`TeleOpBlue`) | `TeleOpRed`, `TeleOpBlue` |
| `opmodes/teleop/DriveWheelTest.java` | `43087c` | Spins one wheel at a time | 4 | yes | – | test OpMode in teleop folder | KEEP (move) | `opmodes/test/TestDriveWheels` |
| `opmodes/auto/PedroAutoBase.java` | `b24b02` | 1126-line base: alliance select, odometry fault guard, shooting, TIP watch | 3 | yes | Pedro | far too long | → Pedro agent / REFACTOR | `AutoBase` |
| `opmodes/auto/PreloadRouteAuto.java` | `d21bf3` | Odometry preload + park ("validated 4/4") | 4 | yes | Pinpoint via Pedro | – | KEEP | `AutoRedNearPreload` / `AutoBlueNearPreload` |
| `opmodes/auto/PreloadCollectAuto.java` | `6a9cc9` | + collect pollen row | 3 | yes | – | 18-line subclass | KEEP | `AutoRedNearCollect` … |
| `opmodes/auto/SafeMoveAuto.java` | `24d5a1` | Encoder-only fallback auto | 4 | yes | Dashboard | – | KEEP | `AutoFallbackLeave` |
| `opmodes/auto/{CenterLane,OuterLane,StartRoutine,TopWall}Auto.java` | – | Pedro routes | – | yes | Pedro | Pedro agent | → Pedro agent | – |
| `opmodes/auto/OdometrySquareTestAuto.java` | `9fc833` | Pedro square test | 3 | yes | Pedro | – | KEEP (move) | `opmodes/test/TestOdometrySquare` |
| `opmodes/calibration/HiveStateProbe.java` | `bfd780` | HIVE orientation capture, drive by hand | 4 | yes | Pedro, Dashboard | – | KEEP | `TuneHiveStateProbe` |
| `opmodes/calibration/PedroPoseCheck.java` | `fdf9d6` | Drive + watch Pedro pose | 3 | yes | Pedro | – | KEEP | `TunePedroPose` |
| `readme.md` | `4d1da4` | SDK readme | 1 | – | – | SDK boilerplate | DISCARD | – |
| `TeamCode/src/test/.../Hive{Geometry,Orientation,StateTracker}Test.java` | `74b497`,`43fb52`,`dc7b8b` | JUnit tests | 4 | yes (junit) | junit 4.13.2 | – | KEEP | same |
| `TeamCode/src/main/assets/images/biobuzz.webp` | – | Dashboard field image | – | – | – | – | KEEP | – |
| `CONVENTIONS.md`, `doc/naming/rename-map.md` | – | Naming rules + old→new map | 5 | – | – | rename map predates vision-align additions | KEEP (update) | superseded by `docs/NAMING.md` |
| `PROJECT_CONTEXT.md` | – | AI-session context | 2 | – | – | stale ("no Pedro", "Indexer runs forward only", local Windows paths) | REPLACE | – |
| `scripts/{pull-robot-logs,pull-latest-logs,clear-robot-logs}.sh`, `scripts/{routes,odometry_routes}_to_visualizer.py` | – | adb log pull, route export | 4 | – | adb, python | – | KEEP | `tools/` |
| `TeamCode/src/main/res/xml/teamwebcamcalibrations.xml`, `res/values/strings.xml`, `res/raw/readme.md` | – | SDK defaults (identical on all 40 branches) | – | – | – | unchanged | KEEP (SDK) | – |

### 2.2 `origin/master` @ `37b7a16` (25 Java files) — files not superseded by vision-align

| File | Blob | Purpose | Q | Compiles | Issues | Decision | Proposed name |
|---|---|---|---|---|---|---|---|
| `Constants.java` | `43c1c9` | Field + per-auto values (Tick, CenterLane, ShootTurnCollect) | 3 | yes | 3 copies of identical RPM/time values; robot size disagrees with BiobuzzFieldMap | REFACTOR → split | `FieldConstants`, `AutoConstants` (per routine), `LauncherConstants` |
| `RobotHardware.java` | `684154` | Mapping with Pinpoint (`tryGet`), Limelight (required `get`) | 3 | yes | DS name `"indexer"` (robot has `"alejandro"`); intake FORWARD (inverted); drive FLOAT+RUN_USING_ENCODER; PIDF magic numbers | REPLACE (by `ae9fef`, add `pinpoint` tryGet) | – |
| `subsystems/Shooter.java` | `ec491a` | Closed loop, `setTargetRpm(p,n)`, `atTargetRPM()` | 3 | yes | nectar initial = 0.75×6000 = 4500 RPM; no voltage comp | MERGE (take the two methods) | `Launcher` |
| `subsystems/Indexer.java` | `6a271f` | Alejandro renamed, ramp only | 3 | yes | no jam handling | REPLACE (by `74d3a7`) | – |
| `subsystems/MecanumDrive.java` | `0ae95b` | Drive + `driveRamped`, `driveRaw`, `getAverageForwardCm` | 3 | yes | brake toggles ZeroPowerBehavior at runtime | REPLACE (keep `getAverageForwardCm` only if AutoDrive survives) | – |
| `subsystems/IndicatorLight.java` | `98d9b4` | + `setGreen` | 4 | yes | – | MERGE into `17e842` | – |
| `subsystems/AutoDrive.java` | `42327c` | Pinpoint closed-loop straight/strafe/turn/driveToPose (non-blocking) | 3 | yes | overlaps Pedro; untested per commit msgs ("has to be calibrated") | REFACTOR or DISCARD after Pedro decision | `DrivetrainController` |
| `subsystems/PinpointOdometry.java` | `d9e1d1` | Pinpoint wrapper, pod health | 3 | yes | offsets 0/0 TODO | MERGE with Mar `13a0c3` (saved pose, field sync) | `subsystems/odometry/Odometry` |
| `subsystems/MotorDiagnosticsLogger.java` | `d2a593` | CSV logger | 4 | yes | – | REPLACE (by `93a19c`) | – |
| `subsystems/vision/*` | `7610b8` etc. | Mar's first vision | 3 | yes | no alliance | REPLACE (by vision-align) | – |
| `subsystems/pathing/{Alliance,PedroFollowerFactory}.java` | `6d3163`,`04feea` | Pedro factory, pod offsets 0 | – | yes | Pedro agent | → Pedro agent | – |
| `opmodes/base/PedroAutoBase.java`, `opmodes/auto/{Tick,CenterLane,OuterLane,Park,ShootTurnCollect}Auto.java` | – | Pedro routes | – | yes | Pedro agent | → Pedro agent | `AutoRed…`/`AutoBlue…` |
| `opmodes/calibration/OdometryCalibrationAuto.java` | `854c72` | Interactive pod/gyro calibrator | 3 | yes | depends on AutoDrive | KEEP (if AutoDrive kept) | `TuneOdometryPods` |
| `opmodes/teleop/MainTeleOp.java` | `4d35ec` | Older TeleOp; GP2 LB = align, GP1 RB collect | 3 | yes | intake button mapping inverted vs robot | REPLACE (by `ad3c7d`) | – |
| `DRIVE_MODES_REFERENCE.md`, `docs/DRIVE_MODES_REFERENCE.md` | – | Duplicate copies of one doc | 3 | – | duplicated | MERGE (one copy) | superseded by README "Controls" |
| `docs/autonomos-cancha.html` | – | Field diagram of autos | – | – | Spanish filename | → Pedro agent | `docs/auto-routes-field.html` |

### 2.3 `feature/mar/duopodriveNcam` @ `2d92fba` — unique / differing files

| File | Blob | Purpose | Q | Compiles | Issues | Decision | Proposed name |
|---|---|---|---|---|---|---|---|
| `subsystems/BiobuzzFieldMap.java` | `e6038b` | 829-line field geometry (zones, cells, pose helpers) for Pinpoint (center origin, North/East) | 3 | yes | Spanish Javadoc; different origin/axes than Pedro (bottom-left); robot 18.0×17.5 in | REFACTOR (extract constants, convert to Pedro frame) | `FieldConstants` + `FieldMap` |
| `subsystems/vision/HiveAlignController.java` | `7828c9` | Hybrid vision + odometry aim (`syncOdometryWithVisibleHive`, `getAimErrorDegrees`, `getTargetHeadingDegrees`) | 3 | yes | 388 lines, Spanish comments | MERGE (odometry fallback into `HiveAligner`) | – |
| `subsystems/vision/HiveCell.java` | `63f41d` | Cells with field-center coordinates, `findClosestCell` | 3 | yes | – | MERGE into `2d9fe8` | – |
| `subsystems/vision/HiveGeometry.java` | `db231c` | 451-line variant | 3 | yes | untested; larger than VA's tested version | DISCARD (keep VA `7fbf90`) | – |
| `subsystems/PinpointOdometry.java` | `13a0c3` | + saved autonomous pose, North/East, `syncFieldPositionCm`, measured offsets | 3 | yes | offset convention differs from Pedro | MERGE | `Odometry` |
| `subsystems/Pera.java` | `2894ef` | Nectar gate servo (Spanish name "pear") | 2 | yes | `CLOSED_DEGREES=-35` → negative servo position (clamped to 0); gate removed from robot in VA `c1580df` | DISCARD (keep as reference for `NectarGate` if hardware returns) | `NectarGate` |
| `subsystems/MecanumDrive.java` | `798792` | + `driveWithAutoTurn` | 3 | yes | – | MERGE idea only | – |
| `opmodes/teleop/MainTeleOp.java` | `4d4252` | Hybrid aim TeleOp; intake on triggers | 3 | yes | different button map from all others | DISCARD (keep VA) | – |
| `opmodes/auto/RedCornerToCellTestAuto.java` | `603669` | Coordinate-system test | 2 | yes | Spanish | DISCARD (test) | – |
| `opmodes/auto/VisionCalibrationAuto.java` | `8710e8` | Camera & align test | 3 | yes | – | MERGE into `TuneHiveStateProbe` or KEEP | `TuneHiveAlign` |
| `opmodes/auto/TopWallAuto.java` | `c422f0` | Pinpoint version of Top Wall | 2 | yes | duplicate of Pedro version | DISCARD | – |
| `juego-del-robot-biobuzz.md`, `mapa-cancha-odometria-biobuzz.md` (in java package!) | `2e14dd`,`2fe39b` | Game rules + field coordinate notes (Spanish) | 3 | – | stored inside the Java source tree | KEEP as docs (translate) | `docs/game-rules.md`, `docs/field-coordinates.md` |

### 2.4 `feature/turrubiates/auto-leave-park` @ `e455142` (= `feature/luis/park` @ `a8c4215` + intake 1.0)

| File | Blob | Purpose | Q | Compiles | Issues | Decision | Proposed name |
|---|---|---|---|---|---|---|---|
| `opmodes/auto/LeaveParkAuto.java` | `c67f79` | Deterministic LEAVE (3) + PARK (5) from loading zone | 4 | yes | depends on master-line `MecanumDrive@319d18`/Pinpoint | REFACTOR (port to VA Drivetrain) | `AutoRedPark`, `AutoBluePark` |
| `subsystems/GuardedStraightDrive.java` | `f7d1fd` | Fixed-power straight move stopping on pod OR wheel encoders | 4 | yes | – | KEEP | `subsystems/drivetrain/GuardedStraightMove` |

### 2.5 `feature/renata/flower-arm` @ `889102d`

| File | Blob | Purpose | Q | Compiles | Issues | Decision | Proposed name |
|---|---|---|---|---|---|---|---|
| `subsystems/FlowerArm.java` | `428e23` | Two mirrored servos, toggle 0↔90° on rising edge | 2 | **no** – constructor named `DualServo` | positions are magic numbers, no state enum | REFACTOR (fix ctor, constants, `FlowerArmConstants`) | `subsystems/flower/FlowerArm` |
| `RobotHardware.java` | `329450` | + `servoLeft`/`servoRight` (right REVERSED), `stopServo`, `huskyLens` | 3 | yes | generic names | MERGE (`flowerLeftServo`, `flowerRightServo`) | – |
| `subsystems/Indexer.java` | `715f59` | Rename attempt | 1 | **no** – `hardware.Indexer`, `Indexer_POWER` | – | DISCARD | – |
| `subsystems/StopServo.java` | `9c7b50` | Toggle servo gate | 2 | yes | gate removed from robot (VA `c1580df`) | DISCARD | – |
| `subsystems/BallsCounting.java` | `e989e8` | see 2.6 | | | | | |
| `opmodes/teleop/MainTeleOp.java` | `a8f804` | GP1 RB toggles flower | 2 | no (depends on above) | conflicts with intake bumper use | DISCARD (take only binding idea) | – |
| `opmodes/teleop/Auto-test.java` | `67fce5` | Pedro test | 1 | **no** – file/class name mismatch, no Pedro dep | Spanish name; in teleop folder | DISCARD | – |

### 2.6 Partial-value branches

| Branch @ commit | File | Blob | Purpose | Q | Compiles | Issues | Decision |
|---|---|---|---|---|---|---|---|
| `Balls-counting` @ `55f69ad` | `subsystems/BallsCounting.java` | `e989e8` | HuskyLens inventory (max 4) + 6-LED Prism slots | 2 | yes | `setPrismPixel` is an empty stub (Spanish TODO); ID map conflicts with BallVision; `SINGLE_BALL_WIDTH_PX` magic | DISCARD (idea of LED inventory display → future `BallInventory`) |
| `PID` @ `6b8ac1e` | `util/PIDController.java` | `3345d1` | Generic PID | 3 | yes | no anti-windup / dt clamp (autosubsystems `Pid` is better) | DISCARD |
| `PID` @ `6b8ac1e` | `subsystems/MecanumDrive.java` | `5fe5c7` | Brake = PID position hold per wheel | 3 | yes | untested | MERGE idea (active hold exists in VA autos `99a7233`) |
| `renata/auto-align` @ `3e733d4` | `subsystems/Shooter.java` | `d0a2bb` | Ramp + idle pre-spin 1800 RPM, `getTicksPerRev()` from motor type | 3 | yes (own RobotHardware `8b6c5e`) | 5 % tolerance; misnamed branch | MERGE idea (`IDLE_RPM` pre-spin) into Launcher |
| `renata/auto-align` @ `3e733d4` | `subsystems/MecanumDrive.java` | `fcd92a` | `setDriveStraightOnly` | 2 | yes | – | DISCARD |
| `autonomospaths` @ `9cf2c66` / `theautonomous` @ `125df22` | `subsystems/autosubsystems/*` (9 files), `opmodes/auto/{Red,Blue}*Auto.java` (16), `AutoBase` | – | Blocking autos for lift + feeder-servo shooters | 1 | yes (self-contained) | hardware does not exist on robot; `SystemClock.sleep` loops; Spanish OpMode names ("AUTO AZUL - Recolecta y Anota", group "Competencia") | DISCARD |
| same | `subsystems/autosubsystems/Pid.java` | `6aeb44` | PID with derivative-on-measurement, anti-windup, dt clamp | 4 | yes | – | KEEP as `util/PidController` if a software PID is needed |
| `theautonomous` @ `125df22` | `opmodes/auto/theautonomous/{RouteRunner,TimeModel,MatchClock,Phase,Route,RouteBuilder,Step,RouteLibrary,TheAutonomous,AutoRobot,Alliance}.java`, `PLAN.md`, `NOTAS_AUTONOMOS.md` | – | Time-budgeted 30 s route runner (skip phase if it no longer fits, park instead) | 3 | yes | bound to fictional hardware; blocking; Spanish notes | REFACTOR concept only (Pedro agent) |
| `auto-shooter` (local) @ `d4642a0` | `opmodes/auto/ShootAndCollectAuto.java`, `WPILibAuto/**` | `162bd4` | Pedro routine + FRC reference | 2 | yes / n.a. | WPILib is FRC code in an FTC repo | Pedro agent / DISCARD WPILib |

### 2.7 Discard-only artefacts

| Path | Where | Reason |
|---|---|---|
| `.idea/**` | `0bef98e`, `e314915` | IDE state committed (2.4k-line `deviceStreaming.xml`) |
| `.artifacts/36fb932d…/*.artifact.md` | `d3aa850`, `ff40234` | AI-agent scratch files |
| `opmodes/teleop/cool.txt` | `ff40234` | "actualizado, updated" |
| `subsystems/shooters/contextshooters` | `dfe9f54` | empty file |
| `subsystems/shooters/Shooters.java` | `dfe9f54` | duplicate of `Shooter@9f3ac7`; RobotHardware never assigns `shooterP/N` → NPE |
| `subsystems/Shooter.java@e4c326` | `d3aa850` | `COUNTS_PER_REV = 537.7` (312 RPM gearmotor) for a 6000 RPM flywheel |
| `subsystems/Shooter.java@203747` | `ff40234` | open-loop % power, superseded |
| `robotHardware.java@981bda` | `1f0c541`, `0bef98e` | file name case ≠ class `RobotHardware` |

---

## 3. Class & method inventory (deduplicated by blob, grouped by purpose)

Blob → commits that carry it is listed so identical code across branches is visible.

### 3.1 Hardware map — `RobotHardware` (17 blobs)

| Blob | Commits | Devices / key constants | Notes |
|---|---|---|---|
| `ae9fef` | `a7b8f4c` | `frontLeftDrive…`, `intakeMotor`, `indexerMotor`, `pollenShooterMotor`, `nectarShooterMotor`, `indicatorLight`, `nectarAlertLight`, `limelight`, `huskyLens` (+ `*_LEGACY_NAME`), `getDevice()` | **Winner** |
| `684154` | `37b7a16`, `206fa84` | `FL/FR/BL/BR`, `intake`, `indexer`, `P`, `N`, `limelight`, `pinpoint`; `SHOOTER_VELOCITY_PIDF (10,0.5,0,12)` | master |
| `2957f2` | `dae149e` `e455142` `a8c4215` `6fbf6e8` `c17992f` `64a1504` `e9ef5bb` `d4642a0` | same as master with `alejandro` | Mar line |
| `169592` | `2d92fba` `c73454f` | + `pera` servo | |
| `329450` | `889102d` | + `servoLeft/servoRight/stopServo/huskyLens` | flower |
| `fb8ff7` | `afbabf1` `9734d9c` `6b8ac1e` `b9c6e12` `ea8f5a1` | develop | |
| others (`06c8f2 10a9aa 32851a 449d2a 863304 8b6c5e ad501f c7bd2d d3fbd6 d42a51 e3c42d f915ac 981bda`) | early / experimental | | superseded |

### 3.2 Launcher (Shooter / Shooters) — 10 blobs + 1

| Blob | Commits | Public API | Key constants | Control |
|---|---|---|---|---|
| `ac639e` | vision-align | `update(8 bools)`, `fireP/reverseP/stopP`, `fireN/reverseN/stopN`, `stop`, `setTargetRpmP`, `getTargetRpmP/N`, `getCurrentRpmP/N`, `isPollenReady`, `isNectarReady`, `getAppliedKf`, `getStateP/N` | `INITIAL_RPM_P 3000`, `INITIAL_RPM_N 3600`, `KP 30`, `KF 15 @12 V`, `READY 100 / NOT_READY 200 RPM`, `KF_UPDATE 0.25 s` | SDK velocity PIDF, voltage-compensated kF, hysteresis |
| `ec491a` | master, autonomos-beta | as above minus ready/kF, plus `setTargetRpm(p,n)`, `atTargetRPM()` | `INITIAL_RPM_P 2900`, `INITIAL_SPEED_FRACTION 0.75`, `RPM_TOLERANCE 100` | fixed PIDF from RobotHardware |
| `9c2a72` | auto-shooter | = `ec491a` without `setTargetRpm` | | |
| `4b4267` | develop + 18 branches | `update`, fire/reverse/stop P/N, getters | 2900 / 0.75 | baseline "first physical test, everything worked" (`bc00121`) |
| `1d7082` | duopodriveNcam | = `4b4267` with `INITIAL_RPM_N 3200`, P 3000 | | |
| `d0a2bb` | renata/auto-align | `update(shoot, 4 bools)`, `isShooting`, `isReady`, desired/actual getters | `IDLE_RPM 1800`, `DEFAULT 3600`, `MAX 5400`, ramp 0.5 s | software ramp + idle pre-spin |
| `68256d` | renata/shooter | `update(Gamepad)`, `fire`, `isReady` | ramp 2500 RPM/s | gamepad inside subsystem ✗ |
| `9f3ac7` / `e7b425` (`Shooters`) | button-shooter, CSV / develop-button-shooter | `update(Gamepad)`, `fire`, `stop` | | gamepad inside subsystem ✗ |
| `e4c326` | mar/shoters | `update`, `isReady` | `COUNTS_PER_REV 537.7` ✗ | |
| `203747` | shooters-upgraded | `update(Gamepad)`, power % getters | 70 % default | open loop |
| `13b317` `PidShooter` (+ `PollenShooter`, `NectarShooter`) | autonomospaths, theautonomous | `shoot`, `spinUpAndWait`, `fire`, `hold`, `isReady` | 384.5 CPR, `LOOP_SLEEP_MS 10` | blocking software PID, feeder servo (non-existent) |
| `5af560` `ShooterLogger` | vision-align | `start`, `update(isFeedReady, indexerState)`, `markLastShot`, `getPollenStats/NectarStats`, `close` | 0.02 s sample, dip 200 RPM | diagnostics |

### 3.3 Indexer (Alejandro / Indexer) — 4 blobs

| Blob | Name | Commits | API | Notes |
|---|---|---|---|---|
| `74d3a7` | `Indexer` | vision-align | `update(feed, eject[, isPaused])`, `stop`, `getState`, `getPower`, `getTotalUnjams`, `getTotalJamStops`; states IDLE/FEEDING/PAUSED/EJECTING/UNJAMMING/JAMMED; `JAM_CURRENT_AMPS 5`, `JAM_TIME 0.5 s`, `MAX_UNJAM_ATTEMPTS 3` | **Winner** |
| `6a271f` | `Indexer` | master | `update(collect, eject)`; IDLE/INTAKING/EJECTING; 0.75 | rename only |
| `715f59` | `Indexer` | removing-Alejandro, flower-arm | same, `hardware.Indexer` | does not compile |
| `a20fa6` | `Alejandro` (person's name) | 19 commits (develop line, Mar line, luis/*) | = `6a271f` | Spanish/person name ✗ |

### 3.4 Intake — 2 blobs

| Blob | Commits | API | Only difference |
|---|---|---|---|
| `0aca19` | master, vision-align, develop line (19 commits) | `update(collect, eject)`, `stop`, `getState`, `getPower`; ramp 0.15 s | `INTAKE_POWER 0.75` |
| `e73f94` | early branches + `luis/{park,alliance,probe,naming}`, `mar/duopodrive` (18 commits) | identical | `INTAKE_POWER 1.0` (re-applied 2026-11-22 by Luis; `git log -L` shows `bc00121` set 0.75 on 2026-11-06 after the first physical test) |

### 3.5 Drivetrain (MecanumDrive / AutoDrive / GuardedStraightDrive) — 12 + 3 + 1 blobs

| Blob | Commits | API | Notes |
|---|---|---|---|
| `746e93` | vision-align | `drive`, `driveRamped`, `setPowerLimit`, `setBrake`, `setSlow/FastMode`, `isBraking`, `isSlowMode`, `getWheelPositions`, `resetEncoders` | separate accel/decel ramps, reversal-through-zero, stop cutoff, `@Config` — **winner** |
| `0ae95b` | master, auto-shooter | + `driveRaw`, `stop`, `setZeroPowerBehavior`, `getAverageForwardCm`; `TICKS_PER_REV 537.7`, `WHEEL_DIAMETER_CM 10.4` | needed by master `AutoDrive`/`OdometryCalibrationAuto` |
| `319d18` / `e8a53b` / `ddf686` / `6455c8` / `798792` | Mar line variants | `driveRaw`, `getAverageForwardCm`, (`798792`: `driveWithAutoTurn`) | |
| `e01fdd` / `c81636` / `c73f2b` / `5fe5c7` / `fcd92a` | develop / CSV / early / PID / auto-align | basic drive (+PID hold / straight-only) | |
| `AutoDrive@42327c` | master + 11 | `driveStraightCm`, `strafeRightCm`, `turnByDeg`, `turnToDeg`, `driveToPoseCm` | Pinpoint-based, non-blocking |
| `AutoDrive@86ed33` / `@22503e` | duopodriveNcam / onepodrive | same (+3 lines) / straight + turn only | |
| `GuardedStraightDrive@f7d1fd` | auto-leave-park, luis/park | `driveStraightCm`, `getLastPodTravelCm`, `getLastWheelTravelCm` | dual-sensor stop |
| `autosubsystems/AutoDrive@968aff` | autonomospaths | `moveMm`, `turnDeg` (blocking, sleeps) | DISCARD |

### 3.6 Odometry — `PinpointOdometry` 5 blobs, `AutoPod` 1

| Blob | Commits | API highlights |
|---|---|---|
| `13a0c3` | duopodriveNcam | + `getNorthCm/EastCm`, `saveCurrentPose`, `hasSavedAutonomousPose`, `syncFieldPositionCm`, `resetNorthHeading`; offsets 65 / -170 mm |
| `4abb7d` | auto-leave-park, hive-* | + `isConnected` |
| `d9e1d1` | master | `update`, `reset`, `setPoseCmDeg`, pod health, `getForwardCm/StrafeRightCm/HeadingDeg` |
| `65d5a9`, `feb667` | local duopodriveNcam, onepodrive | older |
| `AutoPod@3749f3` | autonomospaths | DISCARD |

### 3.7 Lights — IndicatorLight 5 blobs + NectarAlertLight

| Blob | Commits | API |
|---|---|---|
| `17e842` | vision-align | `setRed/Blue/White/Yellow/Off`, `blink(color)`, `setColor`, `getCurrentColor`; `COLOR_YELLOW 0.42` |
| `98d9b4` | master + Mar line | + `setGreen` (`COLOR_GREEN 0.50`) |
| `c1708a` (23 commits), `aa3ce2`, `746378` | develop/early | red/blue/off (white) |
| `NectarAlertLight@b99610` | vision-align | `update(BallVision)`, `off`, `getAlertColor`; `HOLD_SEC 0.6` |

### 3.8 Vision (HIVE)

| Class | Winner blob (VA) | Other blobs | Key API |
|---|---|---|---|
| `HiveVision` | `1a134f` | `7610b8` (master), `66d8eb` (Mar), `771a53`, `8e44cd` | `update`, `getBestPose`, `getPose`, `getAllianceTarget`, `getUpwardCell`, `getTipCount`, `getUprightVerdict`, `getRawTagDistanceMeters`, `isConnected`, `stop` |
| `HiveAlignController` | `6b6f19` | `d50f15` (master), `8585d0`, `fae6db`, `60060e`, `7828c9` (Mar hybrid) | `update`, `alignTo(cell)`, `isAligned`, `reset`, `evaluate`; `ENABLED=false`, `TARGET_FORWARD_METERS 1.03` (VA) vs `0.45` (master) |
| `HiveGeometry` | `7fbf90` | `bdcd02`, `e06786`, `db231c` | `reconstructRawCellPose`, `extractImageUpDeg` |
| `HiveCell` | `2d9fe8` | `0dad5d`, `43f4ef`, `63f41d` (Mar field coords) | `getAlliance`, `getDisplayName`, `indexOfTag` |
| `HiveClusterPose` | `1dc8a3` | `31e52b`, `4ded7a`, `f512e0` | value object |
| `HiveOrientation` / `HiveStateTracker` | `791c5d` / `7ef04c` | `192b59` | `isUpright`, `circularMeanDeg` / `update`, `getUpwardCell`, `getTipCount`, `selectTarget` |

### 3.9 Ball vision / counting

| Class | Blob | Branch | API | IDs |
|---|---|---|---|---|
| `BallVision` | `263911` | VA | `update`, `getBalls`, `getClosestBall`, `getAssistTurn`, `isConnected` | POLLEN 1, RED 2, BLUE 3 |
| `BallIntakeCounter` | `f80c96` | VA | `update(isIntaking)`, `getCount(color)`, `getTotal`, `summary` | – |
| `BallsCounting` | `e989e8` | Balls-counting, auto-test, removing-Alejandro, flower-arm | `update`, `getBallCount`, LED slots (stub) | POLLEN 1, BLUE 2, RED 3 ✗ |

### 3.10 Gates / FLOWER

| Class | Blob | Branch | API | Constants |
|---|---|---|---|---|
| `FlowerArm` | `428e23` | flower-arm | `deploy`, `retract`, `toggle`, `update(button)`, `isDeployed`; ctor misnamed `DualServo` | `POSITION_REST 0.0`, `POSITION_DEPLOYED 0.5` |
| `StopServo` | `9c7b50` | auto-align, flower-arm | same toggle API + `getCurrentPosition` | 0.0 / 0.5 |
| `Pera` | `2894ef` | duopodriveNcam | OPEN while firing | `SERVO_MAX_DEGREES 280`, `OPEN 10°`, `CLOSED -35°` ✗ |

### 3.11 Diagnostics / utilities

| Class | Blobs | Notes |
|---|---|---|
| `MotorDiagnosticsLogger` | `93a19c` (VA), `d2a593` (master), `1d088b` (develop + 18), `a42e61`, `d40969` | `start`, `update`, `close`, `getFileName`, `getSampleCount`; `/sdcard/FIRST/motor_logs` |
| `BatteryMonitor` `2c6210`, `MatchSummary` `c7a737`, `LogFiles` `9c9858`, `PoseLogger` `3e0997`, `FieldDrawing` `7c4dcb` | VA only | |
| `PIDController` `3345d1` | PID branch | basic |
| `Pid` `6aeb44` | autonomospaths | good PID implementation |
| `Constants` `43c1c9` | master | nested `Field`, `Tick`, `CenterLane`, `ShootTurnCollect` |
| `BiobuzzFieldMap` `e6038b` | duopodriveNcam | `seedOdometryPose`, `driveToFieldPose`, `computeShooterHeadingDeg`, `computeIntakeHeadingDeg`, zone tests |

---

## 4. Comparisons and merge plans

`git log -L` was run with regex ranges on the winning branch to see who last changed each key method.

### 4.1 Launcher velocity control

| | master `ec491a` | vision-align `ac639e` | renata/auto-align `d0a2bb` |
|---|---|---|---|
| Loop | SDK `setVelocity` | SDK `setVelocity` | SDK + software ramp |
| PIDF | fixed in RobotHardware | voltage-compensated kF, `@Config` | – |
| Ready check | `atTargetRPM()` both wheels ±100 | per wheel, hysteresis 100/200 | 5 % |
| Auto API | `setTargetRpm(p,n)` | `setTargetRpmP` only | – |
| History (`-L`) | `atTargetRPM`: `fcacb1c` → `50bbd6d` (2026-11-22) | `update`: `fcacb1c`, `b3812ab`, `bc00121`, `01b624e`, `f8e65b8`; `compensateForVoltage`: `01b624e`, `98ca35e` (2026-11-20) | – |

**Winner: vision-align `ac639e`.** Merge plan → `Launcher`: base `ac639e@a7b8f4c`; add `setTargetRpm(pollenRpm, nectarRpm)` and `atTargetRpm()` (rewritten on top of `isPollenReady/isNectarReady`) from `ec491a@37b7a16` so the Pedro autos keep working; optional idle pre-spin (`IDLE_RPM`) idea from `d0a2bb@3e733d4`; rename per `rename-map.md` (`firePollen`, `fireNectar`, …); move tunables to `LauncherConstants` (or keep `@Config` statics there). Decide initial RPM on the robot (VA: 3000/3600 with commit `21900d5` "pollen at 3000 RPM" and `a50ab10` "other cell at 2950 RPM").

### 4.2 Intake

| | `0aca19` (master, VA) | `e73f94` (luis/*, mar/duopodrive) |
|---|---|---|
| Power | 0.75 | 1.0 |
| Direction (RobotHardware) | master FORWARD / VA REVERSE | – |
| TeleOp binding | master RB collect / VA LB collect | – |

`git log -L` on `INTAKE_POWER`: `810c857` (2026-10-16) → `bc00121` (2026-11-06, 0.75 after first physical test) → `a8c4215` (2026-11-22, 1.0). **Winner: `0aca19` class + VA direction (REVERSE) and binding**; make `INTAKE_POWER` a tunable in `IntakeConstants` and confirm 0.75 vs 1.0 on the robot (the four 2026-11-22 "100 %" commits are the team's latest intent).

### 4.3 Indexer counting / feeding

VA `74d3a7` wins outright (`-L` on `update`: `bc00121` → `9c03b6c` jam reverse → `76bfab1` rename → `51e7920` pause below RPM → `dac50af` counters). master `6a271f` and `Alejandro@a20fa6` are the same 102-line ramp class. Ball **counting** lives in VA `BallIntakeCounter` (`62ac6f7`, 2026-11-24); Renata's `BallsCounting` (`74a8489`, `55f69ad`) is superseded — keep only the "show inventory on LEDs" idea, and **fix the HuskyLens ID table once** (recommend VA's POLLEN 1 / RED 2 / BLUE 3 and retrain the camera).

### 4.4 Alliance logic

| Location | Behaviour |
|---|---|
| VA `vision/Alliance@fcfd12` + `HiveVision.getAllianceTarget` (`8a51435`, `c1c25a3`) | INIT select X=blue / B=red; aim only at own upward cell |
| VA `pathing/AlliancePoses@582d51` | same Pedro path for both alliances (`d06ea2d`) |
| master `pathing/Alliance@6d3163` | authored on blue/left half, mirrored for red |
| auto-shooter `pathing/Alliance@116500` | authored on red, mirrored for blue |
| theautonomous `Alliance@a7450d` | red coordinates, mirrored strafes/turns |

**Plan:** one `common/Alliance` enum (RED/BLUE + HIVE cells from `fcfd12`); field mirroring policy is a Pedro-agent decision (three contradictory conventions exist).

### 4.5 Park

| Source | Approach | Tested? |
|---|---|---|
| `LeaveParkAuto@c67f79` (auto-leave-park `5681728`, `e455142`) | `GuardedStraightDrive`, from loading zone, intake against wall | commit messages imply field runs ("start … with the intake against the wall") |
| VA `PreloadRouteAuto@d21bf3` | Pinpoint odometry preload + park | **"validated: 4/4 preload shots and parked in ~12-14 s"** |
| VA `SafeMoveAuto@24d5a1` | wheel encoders only, back 40 cm + left 40 cm | fallback |
| master `ParkAuto@1789cd` | Pedro path | simulation only (`206fa84` "found in path simulation") |

**Plan:** keep VA `PreloadRouteAuto` as the primary park; keep `SafeMoveAuto` as fallback; port `LeaveParkAuto` + `GuardedStraightDrive` onto the VA drivetrain as the minimal 8-point auto; Pedro `ParkAuto` → Pedro agent.

### 4.6 Auto-align

| | master `d50f15` (= Mar `8585d0` + `driveRamped`) | Mar `7828c9@2d92fba` | VA `6b6f19` |
|---|---|---|---|
| Input | Limelight cluster pose | Limelight + Pinpoint (aim keeps working when tags lost) | Limelight, alliance-specific cell |
| Trigger | GP2 LB hold | GP1 LB / GP2 A | automatic while firing if target visible and driver idle |
| Setpoint | 0.45 m | captured pose | 1.03 m / 0.08 m (measured pollen spot, `3c93873`) |
| State | enabled | enabled | `ENABLED=false` (`21900d5`) |

**Winner: VA `6b6f19`**, merging Mar's odometry fallback (`syncOdometryWithVisibleHive`, `getAimErrorDegrees`) from `7828c9@2d92fba` and `HiveCell` field coordinates from `63f41d@2d92fba`. Must be calibrated before `ENABLED=true`.

### 4.7 FLOWER arm

Single source: `FlowerArm@428e23` (`889102d`, 2026-11-15). Plan: fix constructor name, add `FlowerArmConstants` (`REST_POSITION`, `DEPLOYED_POSITION`), state enum, servo names `flowerLeftServo`/`flowerRightServo` (right REVERSED as in `329450`), rebind away from GP1 bumpers (both bumpers are used by intake + ball assist in VA). Confirm the mechanism is on the robot.

### 4.8 Drivetrain

**Winner VA `746e93`** (`-L applyRamp`: `98cc9eb` → `64dfe81` damped decel → `8a51435` → `99a7233` quicker stops, 2026-11-23). If master `AutoDrive`/`OdometryCalibrationAuto` survive, re-add `getAverageForwardCm()` and `driveRaw()` from `0ae95b@37b7a16`. PID hold brake (`5fe5c7@6b8ac1e`) optional.

### 4.9 RobotHardware

**Winner VA `ae9fef`**; add `pinpoint` (`tryGet`) from `684154@37b7a16`, flower servos from `329450@889102d`; add `"indexer"` as a second legacy name (master renamed the DS device) or reconfigure every hub and drop legacy names.

### 4.10 TeleOp

**Winner VA `ad3c7d`** (feed gating on ready + aimed, jam rumble, shot marks, battery limit). Split into `TeleOpBase` + `TeleOpRed`/`TeleOpBlue` (alliance fixed instead of INIT button), move telemetry formatting to a helper, add FLOWER binding.

---

## 5. Criteria matrix (main candidates)

| Criterion | master `37b7a16` | vision-align `a7b8f4c` | duopodriveNcam `2d92fba` | flower-arm `889102d` | auto-leave-park `e455142` | theautonomous `125df22` |
|---|---|---|---|---|---|---|
| Matches current robot | partial (intake inverted, DS names changed) | **best** (latest DS names + legacy fallback, gate removed, intake fixed) | partial (has `pera` gate) | partial | yes | no (lift / feeders) |
| Compiles (SDK 12 + own deps) | yes | yes (verify Dashboard 0.6.0) | yes | **no** | yes | yes |
| Hardware separated from OpMode | yes | yes | yes | yes | yes | yes (own `AutoHardware`) |
| No magic numbers | fair (`Constants`) | good (`@Config` statics) | poor (Spanish inline values) | poor | fair | fair |
| No `sleep()` in TeleOp loops | yes | yes | yes | yes | n/a | n/a (autos sleep) |
| State machines | Intake/Indexer enums | Indexer 6-state, Launcher ready hysteresis | basic | toggle | sequential | blocking steps |
| Tested on robot (commit msgs) | autos: simulation only | yes ("validated 4/4", log-driven fixes 2026-11-23/24) | "worked as intended" (calibration) | no evidence | implied | no |
| Documented | Javadoc | Javadoc + CONVENTIONS + rename map + tests | Spanish Javadoc + md notes | minimal | Javadoc | md notes (Spanish) |

---

## 6. Findings: duplication, dead code, experiments, obsolete deps, Spanish names

### 6.1 Duplicated code
- `Alejandro` ≡ `Indexer@6a271f` ≡ `Intake` (same 100-line ramp class copied three times) → one `RampedMotor` helper or keep only two specialised classes.
- `Shooter` exists in 10 blob variants plus `Shooters` (subfolder copy of `9f3ac7`).
- `Constants.Tick` / `CenterLane` / `ShootTurnCollect` repeat identical RPM (2900/4500), spin-up (3.0 s) and feed (2.0 s) values.
- `DRIVE_MODES_REFERENCE.md` at root and in `docs/` (master).
- Three `Alliance` types (`vision/`, `pathing/`, `theautonomous/`) and three mirroring conventions.
- `HiveVision`/`HiveAlignController`/`HiveGeometry` re-implemented on vision-align instead of merged from Mar's branch (5–6 blobs each).
- `TopWallAuto` exists in 4 blobs (Pinpoint and Pedro variants).

### 6.2 Dead / unused code
- `IndicatorLight.setGreen` unused on VA (removed); `HiveAlignController.captureCurrentPoseAsTarget` (Mar) unused by VA.
- `ShooterState.REVERSING` for nectar never commanded on master (`reverseN` passed `false`).
- `BallsCounting.setPrismPixel` empty stub.
- `MecanumDrive.driveRaw/stop/getAverageForwardCm` only used by master's AutoDrive path.
- `subsystems/shooters/contextshooters` empty file; `cool.txt`.

### 6.3 Abandoned experiments
- `autosubsystems/*` + 16 `Red*/Blue*Auto` (fictional hardware, Spanish OpMode names).
- `theautonomous/*` (time-budget runner; concept only).
- Single-pod odometry (`onepodrive`), `Pera` gate, `StopServo` gate (removed `c1580df`).
- AGP 9.3.3 / Gradle 9.5–9.8 upgrade (`d3aa850`, `ff40234`, `9cf2c66`, `125df22`).
- Open-loop launcher (`203747`), 537.7-CPR launcher (`e4c326`).
- `WPILibAuto/` (FRC) on local `auto-shooter`.

### 6.4 Forgotten test / calibration OpModes (move to `opmodes/test` or `opmodes/tuning`)
`DriveWheelTest`, `OdometrySquareTestAuto`, `PedroPoseCheck`, `HiveStateProbe`, `OdometryCalibrationAuto`, `VisionCalibrationAuto`, `RedCornerToCellTestAuto`, `Auto-test` (broken), Pedro `tuning/Tests`.
Several are registered in group `"Competition"`; tests should not appear in the competition list.

### 6.5 Obsolete / risky dependencies
- `com.pedropathing:revhub:3.0.2`, `tuning:1.0.3` → Pedro agent.
- `com.acmerobotics.dashboard:dashboard:0.6.0` (VA) – confirm the coordinate exists; excluded `org.firstinspires.ftc` correctly.
- `androidx.appcompat:1.2.0` (SDK template default – leave as is).
- `junit:4.13.2` (fine for local unit tests).
- No `.github/workflows` CI on any branch → nothing guarantees a branch compiles.

### 6.6 Spanish / non-conforming names to rename
| Current | Where | Proposed |
|---|---|---|
| `Alejandro` (class, field, DS name `alejandro`) | develop line | `Indexer`, `indexerMotor` |
| `Pera` | duopodriveNcam | `NectarGate` |
| `BallsCounting` | Renata branches | `BallInventory` |
| `shooterP` / `shooterN`, `fireP()` / `fireN()`, `INITIAL_RPM_P` | all | `pollenLauncherMotor`, `firePollen()`, `POLLEN_INITIAL_RPM` |
| `Shooter` | all | `Launcher` (per brief) |
| `MecanumDrive` | all | `Drivetrain` |
| `MainTeleOp` | all | `TeleOpRed` / `TeleOpBlue` |
| `Auto-test.java` / `AutoPath` | auto-test | delete |
| `NOTAS_AUTONOMOS.md`, `juego-del-robot-biobuzz.md`, `mapa-cancha-odometria-biobuzz.md`, `autonomos-cancha.html` | theautonomous, duopodriveNcam, master | `docs/auto-notes.md`, `docs/game-rules.md`, `docs/field-coordinates.md`, `docs/auto-routes-field.html` (translate) |
| OpMode names "AUTO AZUL/ROJO - …", group "Competencia", "Auto Azul" | autonomospaths, theautonomous | delete |
| Spanish Javadoc/comments | `BiobuzzFieldMap`, `RedCornerToCellTestAuto`, `BallsCounting` ("Enlaza aquí…") | translate |
| Commit messages in Spanish ("todo sirve como debe", "autonomo de prueba") | history | n/a (history) |

### 6.7 Repository / process issues
- `origin/CSV` vs `origin/csv`: two different branches differing only by case — cannot coexist on Windows/macOS; delete one.
- `localcopy/bm` is an orphan root commit; `localcopy/auto-shooter` exists only on the laptop.
- The CONTRIBUTING flow (`develop → SIT → UAT → master`) is not followed: master is 18 ahead of develop; SIT is behind develop; no `UAT` branch exists.
- Branches are created per person (`feature/<name>/…`) and duplicated with an extra one-line commit instead of rebased.
- Markdown notes stored inside the Java package (`teamcode/*.md`, `opmodes/auto/*.md`).

---

## 7. Recommended mining order

1. Start the new tree from **vision-align `a7b8f4c`** mechanism code (RobotHardware, Drivetrain, Intake, Indexer, Launcher, lights, vision, diagnostics, tests, scripts, CONVENTIONS).
2. Bring from **master `37b7a16`**: `Launcher.setTargetRpm/atTargetRpm`, `pinpoint` mapping, `IndicatorLight.setGreen`, `Constants` (split), PinpointOdometry/AutoDrive only if the Pedro agent keeps a non-Pedro path.
3. Bring from **duopodriveNcam `2d92fba`**: odometry-assisted aim, HiveCell field coordinates, field/game notes (translated), BiobuzzFieldMap constants (converted to Pedro frame).
4. Bring **FlowerArm** (fixed) from `889102d` and **LeaveParkAuto + GuardedStraightDrive** from `e455142`.
5. Resolve on the robot: intake direction/power, indexer DS name, launcher RPM + PIDF, pod offsets, robot dimensions, HuskyLens IDs, whether the FLOWER arm and NECTAR gate are mounted.
6. Archive everything else (tag before deleting): `autonomospaths`, `theautonomous`, `renata/*` (except flower-arm), `luis/*` duplicates, `localcopy/*` backups, `CSV`/`csv`, `PID`, `Balls-counting`.
