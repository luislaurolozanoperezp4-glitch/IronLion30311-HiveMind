# Hand-off: Best-of Consolidation Agent

Date: 2026-11-24. Output: `docs/BEST_OF_MATRIX.md`. Read-only work: no git write commands, and no files changed other than these two.

## What I did
- Read `ROBOT_PROFILE.md`, `AUDIT_MAIN.md`, `AUDIT_PEDRO.md`, all `docs/agents/*.md` and `docs/snapshots/*`.
- Spot-checked these in the legacy clone with `git show` / `ls-tree` / `grep`: the VA tree and OpMode list, every VA constant used in §2–§3 (Shooter, Intake, Indexer, MecanumDrive, IndicatorLight, BatteryMonitor, BallVision, HiveAlignController, HiveVision, HiveGeometry, RobotHardware names, PedroFollowerFactory, PreloadRouteAuto, SafeMoveAuto), the VA dependency block, MASTER `Constants` and OpModes, the master/VA method lists, FlowerArm (confirms the `DualServo` constructor bug), ALP LeaveParkAuto/GuardedStraightDrive, NCAM BiobuzzFieldMap / HiveAlignController / PinpointOdometry, and the TA TimeModel.

## Key decisions in the matrix
- Base = `VA` @ a7b8f4c for every mechanism. MASTER adds `setTargetRpm`/`isAtTargetRpm`, `setGreen`, the route values and the `pinpoint` mapping. NCAM adds the field map, the odometry-assisted aim and `PoseStorage`. ALP adds GuardedStraightMove + Leave/Park.
- No `Placer` class. FlowerArm is held until Q14 (D14).
- The Pinpoint is owned by Pedro only. Mar's `PinpointOdometry`/`AutoDrive` are discarded (two owners of one device).
- Competition autos keep VA's tested `driveTo` (`pathing/OdometryDriver`) until AutoTune is run (D6).
- Alliance: identical poses, `rotate180` only for drawing. Never `mirrorX` (D13).
- Every path has a `…Straight` copy. The copies of PreloadPark, Other Cell, Tick T3 and Center Lane C4 cut into the HIVE. Do not use them.

## Traps found
- In the local clone, `localcopy/feature/mar/duopodriveNcam` is the stale `c73454f`. Use `origin/feature/mar/duopodriveNcam` (`2d92fba`).
- HIVE cell naming conflicts: VA's `RED_SCORING`/`RED_AUDIENCE` (tags 30–33 / 34–37) do not obviously match Mar's "audience cell = south" (new D20).
- "Far" start pose is undefined in code (new D21).

## Blocking decisions (see §8)
D2 (DS names), D4 (pod offsets), D6 (Pedro tuning), D13 (alliance frame, highest rule risk), D16 (robot size), D18 (sign-off on the VA base), D20 (cell ↔ tag mapping), D21 (far start).

## Next agent (migration)
Build `config/` and `hardware/` first, then the subsystems in the order Drivetrain → Intake → Indexer → Launcher → ColorSensing → Vision → Lights. Then `util/`, `pathing/` (PedroConstants, OdometryDriver, paths), and `statemachine/`. Finish with `AutoRed/BlueNearPreloadPark` (port of the tested auto) before anything else. Add `PathsTest` and `CycleBudgetTest`.
