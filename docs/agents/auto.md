# Hand-off: Pathing & Autonomous Agent, part 1 (`agent/auto`)

Scope: `pathing/` only. No competition auto OpModes yet (they come after the Subsystems merge).
Build: `./gradlew assembleDebug testDebugUnitTest` → BUILD SUCCESSFUL, 34 tests, 0 failures
(PathsTest 11, OdometryDriverTest 7, CycleBudgetTest 7, plus the scaffold tests).

## Files

| File | What |
|---|---|
| `config/AutoConstants.java` (new) | Tested near-route leg distances (cm, `final`), VA time deadlines, `PATH_TIMEOUT_SECONDS`, AutoPlanner estimates (`TODO: TUNE`) |
| `config/DriveConstants.java` | **Added only**: `AUTO_CARRIED_POWER_WINDOW_SECONDS` 0.15, `AUTO_MAX_LOOP_SECONDS` 0.1 (both from VA code) |
| `pathing/PedroConstants.java` | Configs + `createFollower(HardwareMap)`. `PedroFollowerFactory` is **merged** here (one place for Pedro values and the factory) |
| `pathing/OdometryDriver.java` | Tested VA driver, non-blocking |
| `pathing/AutoPlanner.java` | CYCLE_AGAIN / GO_PARK time-budget logic |
| `pathing/FieldDrawing.java`, `pathing/PoseLogger.java` | Dashboard overlay (+ planned path), pose-only CSV log |
| `pathing/paths/*` | One class per route + `NearRoutePoses`, `RoutePaths` base, `PathCatalog` |
| `pathing/tuning/*` | `Tuning` (@Tuner AutoTune), vendored Quickstart procedures, 3 test OpModes |
| `assets/images/biobuzz.webp` | Field image (Pedro Visualizer, Apache-2.0) |

## API

**PedroConstants**: `DRIVETRAIN_CONFIG` (names from `RobotConstants`, directions + brake from
`DriveConstants`, TESTED), `LOCALIZER_CONFIG` (`pinpoint`, 4-bar, X FORWARD, Y REVERSED TESTED,
`X_POD_OFFSET_INCHES -6.79`, `Y_POD_OFFSET_INCHES -0.72` FITTED), `FOLLOWER_CONFIG` (Foresight
placeholders, every line `TODO: TUNE`), `MAX_PATH_SPEED 0.6` (cap until AutoTune),
`createFollower(HardwareMap)`.

**OdometryDriver** (`new OdometryDriver(OdometryDriver.fromFollower(follower), MatchTimer.systemClock())`):
- start a move: `driveTo(Pose)`, `driveTo(name, Pose, maxPower)`, `driveThrough(name, Pose)`,
  `turnTo(name, headingRad)`, `hold(seconds)` (use `Double.POSITIVE_INFINITY` to stay parked), `cancel()`;
- every loop: `follower.update()` **then** `driver.periodic()` (the driver never calls update);
- status: `isBusy()`, `isDone()`, `getState()`, `getResult()` (REACHED, PASSED, PASSED_THROUGH,
  TIMED_OUT, ODOMETRY_LOST, CANCELLED), `getResultText()`, `getTarget()`, `isOdometryLost()`;
- guards: `checkStartPose(startPose)` → OK / REZEROED / FAULT (FAULT = never drives),
  `armPoseJumpGuard()`; a pose jump > 12 in / 30° latches ODOMETRY_LOST and zero power;
- helpers: `distance(a, b)`, `angleError(target, current)`.

**AutoPlanner** (`new AutoPlanner(autoTimer)`): `decideNextStep(cycleSec, parkAfterCycleSec,
cyclesDone, hasBallsAvailable)` → `CYCLE_AGAIN` only if `timer.hasTimeFor(cycle + park)` (keeps
the 4 s park reserve) and `cyclesDone < MAX_CYCLES`; `shouldParkNow(parkSec)`;
`isBeforeDeadline(latestMatchSec)` (VA 11 / 15 s backstops); `getShootTimeLeft(max, latestEnd)`;
estimates `estimateLegSeconds`, `estimateLegsSeconds(List<Pose>)`, `estimatePathsSeconds(List<Path>)`,
`estimateTurnSeconds`, `estimateShootSeconds(balls)`. Cruise 25 in/s + 0.2 s/leg is FITTED to the
measured ~6.3 s near park.

**Paths**: `new XxxPaths(alliance)`; each chain method returns one Pedro `Path` (compound for
multi-line chains) already converted with `Alliance.toField` (BLUE = RED rotated 180°). Odometry
routes also expose `getXxxWaypoints()` (List<Pose>) for `OdometryDriver`. `PathCatalog.getAll()`
/ `getAllowedForCompetition()` list `Entry` (route, methodName, source, `Use`
COMPETITION/CANDIDATE/ARCHIVE/TUNING, `Flag` HIVE/TERRITORY/WALL/FLOWER/NO_PARK,
`isStraightCopy`, `isParkPath`, `build(alliance)`).

## Path list (RED; old → new; source)

VA = `feature/turrubiates/vision-align @ a7b8f4c`, BETA = `feature/autonomos-beta @ 206fa84`.
Straight copy = one line from the chain start to the chain end, same heading interpolation.
"(same)" = the chain is already one line. ⛔ = `@Deprecated`, DO NOT USE IN COMPETITION.

| Old (source) | New class.method | Start → end | Straight copy | Flags |
|---|---|---|---|---|
| PreloadRouteAuto LEAVE (VA) | `PreloadParkPaths.leaveWall` | (57, 9, -90) → (57, 12.94, -90) | same | — (TESTED) |
| PreloadRouteAuto park legs (VA) | `PreloadParkPaths.park` | (57, 12.94) → (9.76, 105.46, -90) via (57, 14.91), (33.38, 14.91), (33.38, 105.46) | `parkStraight` ⛔ HIVE | WALL (edge ~0.8 in) |
| PreloadCollectAuto row (VA) | `PreloadCollectPaths.collectRow` | (57, 12.94, 180) → (9.76, 12.94, 180) | same (collinear) | WALL (front <1 in) |
| PreloadCollect return (VA) | `PreloadCollectPaths.returnToShootSpot` | (9.76, 12.94) → (57, 12.94, 180) | same | WALL |
| parkFromShootSide (VA) | `PreloadCollectPaths.parkFromShootSide` | (57, 12.94) → (33.38, 40.50) → (9.76, 105.46) | ⛔ HIVE | WALL |
| collect → other cell (VA) | `OtherCellPaths.fromCollect` | (9.76, 12.94) → (23.93, 29.87) → (33.38, 110.18) → (57, 125.16, 90) | ⛔ HIVE | WALL, FLOWER |
| shoot spot → other cell (VA) | `OtherCellPaths.fromShootSpot` | (57, 12.94) → (33.38, 40.50, 90) → (33.38, 110.18) → (57, 125.16) | ⛔ HIVE | FLOWER |
| other cell → park (VA) | `OtherCellPaths.toPark` | (57, 125.16, 90) → (9.76, 105.46, 90) | same | WALL |
| TickAuto T1 (BETA) | `TickPaths.offTheWall` | (60, 8.52, -90) → (60, 24, -85) | same | WALL |
| Tick T2 | `TickPaths.intoCorner` | (60, 24, 180) → (9.77, 16, 180) | same | WALL |
| Tick T3 | `TickPaths.sidewaysToShootSpot` ⛔ | (9.77, 16) → (60, 30) → (60, 120, 180) | ⛔ HIVE | HIVE (under it) |
| Tick T4 | `TickPaths.sidewaysToPark` | (60, 120, 90) → (9.92, 108, 90) | same | WALL |
| CenterLaneAuto C1 (BETA) | `CenterLanePaths.offTheWall` | (56, 8.52, -90) → (56, 24, -96) | same | WALL |
| C2 | `CenterLanePaths.intoCorner` | (56, 24) → (9.77, 12, 180) | same | WALL |
| C3 | `CenterLanePaths.upTheWall` | (9.77, 12) → (9.77, 30, 180) | same | WALL, FLOWER |
| C4 | `CenterLanePaths.underTheStructure` ⛔ | (9.77, 30) → (60, 40, 90) → (60, 120, 90) | ⛔ HIVE | HIVE (under it) |
| C5 | `CenterLanePaths.sidewaysToPark` | (60, 120, 90) → (9.92, 108, 90) | same | WALL |
| OuterLaneAuto O1–O6 (BETA) | `OuterLanePaths.backOffWall`, `toNearCorner`, `backToStartColumn`, `toShootPose`, `toOuterWall`, `toPark` | (56, 8, 90) → … → (10.08, 118.67, 93.8) | same (each) | WALL (corner sweep, park 0.8 in inside LZ edge), FLOWER |
| ParkAuto (BETA) | `SafeParkPaths.farStartToLoadingZone` | (8.96, 131.19, 90) → (9.5, 102.45, 90) reverseTangent | same | WALL (0.59 in) |
| ShootTurnCollectAuto (BETA) | `ShootCollectPaths.backOff`, `collect` | (56, 8.52) → (56, 13.52, -90); (56, 13.52, 90) → (56, 37.52) | same | NO_PARK, WALL |
| new (matrix §4 #23) | `FarCyclePaths.toOtherSpot` | (8.96, 131.19) → (33.38, 110.18) → (57, 125.16, 90) | `toOtherSpotStraight` (allowed) | WALL, FLOWER, UNTESTED |
| new | `FarCyclePaths.otherSpotToShootSpot` | (57, 125.16) → (33.38, 110.18) → (33.38, 40.50) → (57, 12.94, -90) | ⛔ HIVE | — |
| OdometrySquareTestAuto (VA) | `TestSquarePaths.square` / `side(i)` | (56, 8, 90) 24 in square | none (tuning) | WALL |

Not migrated (matrix §4 #18, #22): AS ShootAndCollect (wall turn), Start Routine, old Center
Lane, Top Wall, Renata Auto-test. LeaveParkAuto and SafeMoveAuto are not Pedro paths (encoder /
guarded moves): they belong to the competition-auto step with the Drivetrain subsystem.

`PathsTest` proves: the HIVE flag matches the geometry in both directions (turning 18 × 18 in
footprint vs. HIVE box), `@Deprecated` ⇔ HIVE/TERRITORY flag, every unflagged path keeps
`x + 9 ≤ 72` and stays on the field, park paths end inside the red LOADING ZONE, every original
has a straight copy that is one line with the same ends, BLUE = RED rotated 180°, and every public
path method is in the catalog.

## Tuning OpModes (group "Tuning")

`Test Pedro Pose` (TeleOp, drive by hand, A = reset to near start), `Test Odometry Square`
(Pedro paths, needs AutoTune first), `Test Odometry Driver` (runs the TESTED near legs with the
new non-blocking driver: **run this first on the robot to validate the port**). AutoTune web UI:
`Tuning` @Tuner procedures at http://192.168.43.1:10158. The vendored procedures use
`Thread.sleep` inside AutoTune runs (Quickstart code, not TeleOp loops; left unchanged).

## Findings

- Pedro 3.0.2 `linear(a, b)` takes the **short way** (checked: -96° → 180° turns 84° clockwise),
  answering AUDIT_PEDRO §3 item 5. Exactly 180° turns (`OtherCellPaths.fromShootSpot`,
  `FarCyclePaths.otherSpotToShootSpot`) have an undefined direction; both stay west of x 46.
- Pedro stores headings in [0, 2π) (-90° reads 270°). Compare headings with `angleError`.
- `Path.get(t)` takes t in [0, 1] for compound paths too, and throws if no heading is set.
- Another BIOBUZZ team (FTC 22526, BSD-3-Clause-Clear) also converts BLUE as RED rotated 180°
  about (72, 72): supports decision D13. Their "go park if the HIVE has not changed by a
  deadline" idea is credited in `AutoPlanner` (idea only, no code copied).

## Open questions

1. `FieldConstants.NEAR_SHOOT` is (57, 12.9); the tested geometry gives 12.937. Paths use the
   exact value; should `NEAR_SHOOT` be aligned (scaffold owns it)?
2. Several park ends are < 2 in from the red wall (WALL flag): move them to x ≥ 11 (LEAVE
   margin)? Kept original per instructions.
3. D21 far start legality and D22 under-HIVE crossing still decide Far Cycle / Tick / Center Lane.
4. Planner estimates are seeds; tune from `auto_logs/pose_*.csv`.
5. Matrix lists `PreloadParkPathsStraight` from backOff2; here the chain starts at the shooting
   spot (rule "chain start to chain end"). Both cut the HIVE.

## Next (part 2, after the Subsystems merge)

Competition autos in `opmodes/auto`: `AutoBase` (alliance, `MatchTimer`, `OdometryDriver`,
`PoseLogger`, `PoseStorage.save` at the end), Near Preload Park, Near Cycle (with `AutoPlanner`),
Far Cycle, Safe Park (+ encoders).
