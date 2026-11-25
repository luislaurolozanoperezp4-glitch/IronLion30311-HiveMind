# Hand-off: Pathing & Autonomous Agent, part 2 (`agent/auto`) — competition autos

Scope: `opmodes/auto/`, new `config/AutoConstants` fields, tests in `src/test/.../opmodes/auto/`.
No file owned by another agent was edited. Build: `./gradlew assembleDebug testDebugUnitTest`
→ BUILD SUCCESSFUL, 131 tests, 0 failures (new: `StepSequenceTest` 6, `AutoSelectorTest` 6,
`AutoSafetyTest` 10).

Sources: VA = `feature/turrubiates/vision-align @ a7b8f4c` (`PreloadRouteAuto`, `PedroAutoBase`),
ALP = `feature/turrubiates/auto-leave-park @ e455142` (`LeaveParkAuto`, `GuardedStraightDrive`),
BETA = `feature/autonomos-beta @ 206fa84` (`ParkAuto`).

## OpModes (Driver Station list, group "Competition")

| DS name | Class | Start | What it does | Status |
|---|---|---|---|---|
| `Auto Red Near` / `Auto Blue Near` | `AutoRedNear`, `AutoBlueNear` (→ `ScoringAuto`) | NEAR (57, 9, −90°) | LEAVE 10 cm → 4 POLLEN at 2950 RPM (ends by 22 s) → planner: collect cycle or TESTED park legs. Cycle: early TIP watch → collect row → TIP seen: other cell (before 15 s) / no TIP: back, re-center, TIP check → shoot here or other cell → park | Preload + park TESTED 4/4; cycle part partly tested (VA logs) |
| `Auto Red Far` / `Auto Blue Far` | `AutoRedFar`, `AutoBlueFar` (→ `ScoringAuto`) | FAR (8.96, 131.19, 90°) | To the other-cell spot north of the HIVE → cell seen: Limelight approach + shoot 4 there; not seen: down the west lane to the near spot and shoot → planner: cycle or park | **UNTESTED** (D21) |
| `Auto Safe Park` | `AutoSafePark` | NEAR / FAR / LOADING ZONE (chosen in INIT) | LEAVE + PARK only, no shooting (NEAR: tested legs; FAR: down the wall; LZ: start BEHIND the zone tape (G304.E), back 10 cm, slide into the zone) | NEAR legs tested; FAR/LZ untested |
| `Auto Safe Park Encoders` | `AutoSafeParkEncoders` | front to the side wall, just behind the LOADING ZONE tape (G304.E forbids starting inside) | 10 cm back on wheel encoders (LEAVE only, no PARK), 0.6 s hard timeout; no Pinpoint/Pedro/cameras | ALP logic (field use unknown) |

`preselectTeleOp`: `TeleOp Red` / `TeleOp Blue` for the four alliance autos. The two Safe Park
autos choose the alliance in INIT, so they have no preselect (pick the TeleOp by hand).
No `@Deprecated` straight-copy path is used. No experimental/"Dev" OpMode was added.

## INIT selector (gamepad1, edges via `GamepadEx`)

| Button | Action |
|---|---|
| B / X | RED / BLUE (only Safe Park and Safe Park Encoders; the others fix the alliance) |
| D-pad left / right | start position (only Safe Park) |
| D-pad up / down | start delay 0–10 s in 0.5 s steps (`START_DELAY_*`) |
| A | set the odometry back to the start pose (robot moved after INIT) |

Telemetry shows the route, the choices, the live start-pose check (OK / will RE-ZERO / FAULT,
same limits as at START), missing hardware (`RobotHardware.getWarnings()`), Pedro/Pinpoint
failure, camera status and battery.

## Architecture

- `AutoBase extends OpMode` (iterative, never blocks). Loop: `clearBulkCache()` →
  `follower.update()` → G402 guard → time guard → `driver.periodic()` → `StepSequence.periodic()`
  → `colorSensing.countWhileIntaking` → subsystems → `PoseLogger` + telemetry.
- Steps (`AutoStep`): `driveTo`, `driveThrough`, `turnTo`, `settle`, `holdParked`, `shoot`
  (`ShootingSequence`), `watchForTip` (`TipWatch`), `lookForCell`, `waitFor`, instant decisions
  that call `StepSequence.addNext`. Drive uses the TESTED `OdometryDriver` (D6), not Pedro paths;
  waypoints come from `PreloadParkPaths`, `PreloadCollectPaths`, `OtherCellPaths`, `NearRoutePoses`
  (converted with `Alliance.toField`).
- Mechanism mode per drive step: IDLE / INTAKE / SPIN_UP (as VA `MechanismMode`).
- **Time-aware**: after every volley `AutoPlanner.decideNextStep(cycle, parkAfter, cyclesDone, true)`
  AND the TESTED 11 s backstop → CYCLE_AGAIN or GO_PARK (logged with the numbers). The cycle
  estimate counts driving + a 1-ball volley (`CYCLE_MIN_PLANNED_BALLS`), because each volley is
  time-boxed by its deadline (22 s / 26.5 s). The early TIP watch only uses the spare time.
- **Time guard**: every loop, if `planner.shouldParkNow(EmergencyPark.estimateSeconds(pose))`, the
  current step is cancelled, mechanisms stop and `EmergencyPark` drives: sideways into the west
  lane (x 33.4) → up the lane → PARK; straight to PARK when north of the HIVE. No turns (safe near
  walls). Tested: never closer than the robot circle to the HIVE, never past the territory line.
- **G402 guard** (`TerritoryGuard`): RED-frame center x > 72 − 9 (+ margin) → drive stopped for
  the rest of AUTO, mechanisms off. Routes stay at x ≤ 57, so it only trips on a fault.
- **G407 / G408**: `intake.setCollectGate(counter::canCollectMore)` (count starts at
  `PRELOAD_BALLS`), `intake.setRejectDetector(opponent NECTAR at intake)`.
- Start: `checkStartPose` (OK / REZEROED / FAULT = no drive, still shoots). Missing Pinpoint:
  the follower creation is caught, the robot only shoots.
- Stop: steps cancelled, all subsystems stopped, `PoseStorage.save(pose, alliance)` unless the
  odometry was lost (then the storage stays cleared). Pose CSV in `/sdcard/FIRST/auto_logs/`,
  with an event row for every step result and decision.

## Deviations from VA (on purpose)

1. TIP seen early but past 15 s: VA parked with the south-west diagonal from the end of the row
   (turning next to the wall); now `EmergencyPark` (west lane, no turn).
2. Collect decision = planner estimate AND the 11 s backstop (VA: backstop only).
3. Encoder LEAVE uses `Drivetrain.driveRamped` (0.1 s ramp) instead of raw power, and wheels only
   (ALP also stopped on the Pinpoint X pod).

## What to test on the robot (in this order)

1. `Test Odometry Driver` (Tuning) first, then `Auto Red Near` with delay 0: must match the
   2026-11-23 run (4/4, parked ~12–14 s). Check the planner line in the telemetry/CSV.
2. INIT selector: delay up/down, A re-zero, push the robot 5 cm → "will RE-ZERO", 30 cm → FAULT.
3. Unplug the Limelight: the near route must still shoot and park (TIP checks fall back).
4. Time guard: set `START_DELAY` to 10 s on `Auto Red Near` → it must skip the cycle and park.
5. `Auto Blue Near` on the blue side (D13 rotation).
6. `Auto Safe Park` (all 3 starts) and `Auto Safe Park Encoders` (measure that the front stays in the zone).
7. Far autos only after D21 is answered.

## Open questions

1. D21: is the FAR start legal (G304, touching the wall)? Far route and its vision fallback untested.
2. Fixed by the rules agent (team decision): every competition park now ends >= 2 in off the wall
   (near PARK `RIGHT_2_CM` 60 -> 56.5, FAR Safe Park x 12). Re-test on the robot. `LOADING_ZONE_START_*` must be measured.
3. Planner seeds (`CRUISE_INCHES_PER_SECOND`, `FEED_SECONDS_PER_BALL`, `CYCLE_MIN_PLANNED_BALLS`):
   tune from the CSV "Planner at …" lines. With the seeds (cycle ~13.9 s + park ~4.8 s + 4 s
   reserve) a preload volley ending after ~7.2 s skips the
   cycle (VA allowed up to 11 s).
4. G407: if shot detection misses a preload shot, the counter thinks the robot still holds a
   ball and the intake refuses that many balls in the cycle.
5. D20 cell mapping decides which cell is "scoring" vs "other" for the TIP logic.
6. TeleOp names `TeleOp Red` / `TeleOp Blue` must exist exactly (TeleOp agent).
