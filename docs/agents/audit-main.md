# Hand-off — audit-main (robot / mechanism code audit)

**Output:** `docs/AUDIT_MAIN.md` (branch inventory, file inventory, class/method inventory, comparisons, findings).
**Source audited (read-only):** `legacy/IronLion30311BIOBUZZInicioDeTemporada`, 63 refs / 41 distinct heads / 143 commits. No git write commands were run. No build was run, so every "compiles?" verdict is a static judgement.

## Key facts for the next agents
- **Two unmerged "current" lines.** `origin/master` @ `37b7a16` (2026-11-22) and `origin/feature/turrubiates/vision-align` @ `a7b8f4c` (2026-11-24, the newest commit, 18 behind / 57 ahead of master). Both branch off `afbabf1` (develop).
- **Winners per mechanism (all from vision-align unless noted):** `RobotHardware@ae9fef`, `Shooter@ac639e` → `Launcher` (add `setTargetRpm`/`atTargetRPM` from master `ec491a`), `Indexer@74d3a7`, `Intake@0aca19`, `MecanumDrive@746e93` → `Drivetrain`, `IndicatorLight@17e842` (+ `setGreen` from master `98d9b4`), vision `Hive*` (+ Mar's odometry-assisted aim `7828c9` and `HiveCell@63f41d` from `2d92fba`), `BallVision`/`BallIntakeCounter`, the loggers, `BatteryMonitor`, the JUnit tests, `MainTeleOp@ad3c7d` → `TeleOpRed`/`TeleOpBlue`.
- **Code that exists in only one place:** `FlowerArm@428e23` (renata/flower-arm, does not compile because the constructor is named `DualServo`), `LeaveParkAuto` + `GuardedStraightDrive` (turrubiates/auto-leave-park), `BiobuzzFieldMap` (mar/duopodriveNcam), `ShootAndCollectAuto` + `WPILibAuto` (localcopy/auto-shooter, **local only**).
- **Discard:** `autosubsystems/*` + 16 Red/Blue autos + `theautonomous/*` (written for a lift and feeder servos the robot never had), plus every `localcopy/*` backup and the luis/* duplicate branches.

## Questions to settle on the robot before merging
1. Intake direction and power. master uses FORWARD with right bumper to collect. vision-align uses REVERSE with left bumper, and its commit `927e4df` says FORWARD ejects. Power is 0.75 in both lines, but the four 2026-11-22 commits on `feature/luis/*` and `mar/duopodrive` set 1.0.
2. Indexer Driver Station name: `alejandro`, `indexer` (master) or `indexerMotor` (vision-align)?
3. Launcher PIDF and initial RPM. master uses (10, 0.5, 0, 12) at 2900/4500. vision-align uses P 30 and F 15 at 12 V, voltage-compensated, at 3000/3600.
4. Pinpoint pod offsets. master's Pedro factory has 0/0. vision-align has -6.79 / -0.72 in. Mar's `PinpointOdometry` has 65 / -170 mm.
5. Robot size. `Constants` says 17.83 × 16.54 in and `BiobuzzFieldMap` says 18.0 × 17.5 in.
6. HuskyLens IDs. `BallVision` uses RED=2, BLUE=3 and `BallsCounting` uses BLUE=2, RED=3.
7. Is the FLOWER arm mounted? Is the NECTAR gate (`Pera`/`StopServo`) gone for good? vision-align commit `c1580df` removed it.

## For the Pedro agent
- There are three conflicting alliance mirroring conventions: master `pathing/Alliance@6d3163` (blue-authored and mirrored), auto-shooter `@116500` (red-authored and mirrored) and vision-align `AlliancePoses@582d51` (same path for both alliances).
- vision-align has its own Pedro stack: `PedroAutoBase@b24b02` (1126 lines), `PedroFollowerFactory@d937b5` with fitted pod offsets, AutoTune `tuning:1.0.3`, FTC Dashboard 0.6.0, and the odometry autos `PreloadRouteAuto`/`PreloadCollectAuto`/`SafeMoveAuto`. Commit messages say these were validated on the robot.

## Repo hygiene to fix in the new repo
- Remove the case-colliding `origin/CSV` and `origin/csv` branches.
- Remove the orphan `localcopy/bm`.
- Remove committed `.idea/` and `.artifacts/`, `cool.txt` and the empty `contextshooters`.
- Do not take the AGP 9.3.3 / Gradle 9.5–9.8 upgrades.
- Move the Markdown notes out of the Java package and translate the Spanish names and docs (full list in AUDIT_MAIN §6.6).
