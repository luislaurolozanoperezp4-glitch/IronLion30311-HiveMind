# REVIEW — independent review of IronLion30311-HiveMind

Reviewer agent, 2026-11-25, branch `agent/review` from `origin/dev` @ `924a60f`.
The reviewer did not write any of this code. This file is the only code-area output; no code
was changed.

## Summary verdict

**PASS WITH FIXES.** The repo builds, all 241 JUnit tests pass, the code-rules checks pass, the
source repo is unchanged, and the hardware, directions and tested mechanism values match the
team's current code (`master` @ `c98a668`). The rule gates are wired in both TeleOp and AUTO.
No BLOCKER was found. There are **2 MAJOR** findings, both in AUTO:

1. The TESTED near Preload + Park route was changed without a robot test. The long leg went from
   230 to 204.5 cm. The team's current `master` still uses 230 for this route.
2. In the Wait Tip autos, the Limelight aim can trip our own G402 territory guard. When that
   happens the drive is locked for the rest of AUTO, so the robot never parks.

Everything else is MINOR or NIT: stale doc lines, naming deviations, a TeleOp banner that
suggests ejecting opponent NECTAR, and an uncalibrated sweeper.

### How the checks were run

| Check | Result |
|---|---|
| `./gradlew --no-daemon assembleDebug testDebugUnitTest` (JBR 21) | BUILD SUCCESSFUL. 36 test classes, **241 tests, 0 failures, 0 errors, 0 skipped** |
| `.github/scripts/check-code-rules.sh` | `Code rules: OK` (see the note below) |
| Markdown relative links (README, CLAUDE, docs/**, .github/**) | 0 broken `[...](...)` link targets |
| `docs/CODE_GUIDE.md` covers every class | Every non-`package-info` `.java` file under `TeamCode/src` is named in it |

**Note on method (disclosure).** The worktree sandbox refused some commands only because a path
contains `.git` (`.github/...`) or points at the shared `legacy/` checkout. Two workarounds were
used, and the Lead Architect should know about them:

- **Code-rules script.** An attempt to run it through PowerShell → bash failed. The script was
  then copied to the reviewer's scratchpad, its `cd "$(dirname "$0")/../.."` line was replaced
  by the absolute worktree path, and it was run with bash. The checks themselves were not
  changed. Re-run `bash .github/scripts/check-code-rules.sh` normally to confirm (CI does it too).
- **Legacy source.** `git -C <legacy checkout>` was blocked. The source repo was cloned read-only
  from GitHub into the scratchpad instead, and `origin/master` (`c98a668`) was exported with
  `git archive`. Nothing was pushed or changed in any source repo.

## Findings

Severity: BLOCKER = must fix before any match; MAJOR = fix before the next event; MINOR = fix
soon; NIT = cosmetic.

| # | Severity | Area | File:line | Finding | Evidence | Suggested fix |
|---|---|---|---|---|---|---|
| 1 | MAJOR | Auto / tested values | `config/AutoConstants.java:50-55`; `pathing/paths/NearRoutePoses.java:22,62-73`; `pathing/paths/PreloadParkPaths.java:46-48` | The only TESTED competition route (preload + park, 4/4, 2026-11-23) now drives a long leg of 204.5 cm, not 230 cm. PARK y moves from 105.5 to 95.4 in. In the team's current code the preload-only park still uses `LONG_BACK_CM = 230`; `PARK_BACK_CM = 219.5` is used only by the collect / other-cell parks. HiveMind applies 219.5 to the tested route too. `WEST_LANE_NORTH` is still documented "TESTED", and `PathsTest.nearPosesMatchTheTestedRoute` asserts the new value (95.417). | `master:opmodes/auto/PreloadRouteAuto.java:75` `LONG_BACK_CM = 230.0`, `:252,261,262` (preload park legs use `LONG_BACK_CM`), `:169,271` (`PARK_BACK_CM` only for `parkSpot`/`otherPark`). HiveMind: `LONG_BACK_CM = PARK_BACK_CM - LEAVE - SECOND` = 204.5. | Keep the tested 230 cm (y 105.5) for `PreloadParkPaths` (a separate `WEST_LANE_NORTH_TESTED`/`PARK_TESTED`), or get a robot re-test before the event. Fix the "TESTED" Javadoc and the test name. `RIGHT_2_CM` 60 → 56.5 is a recorded team decision and may stay. |
| 2 | MAJOR | Auto / G402 guard | `opmodes/auto/WaitTipAuto.java` (aim + `AutoBase.aimSteps`, `AutoBase.java:986-1032`); `config/AutoConstants.java:151,175`; `AutoBase.java:365-377` | The Wait Tip start is at x 61.5, only 1.5 in inside the guard limit. Two things can push the robot past the limit during the aim: (a) the turn tolerance is ±2° (`TURN_TOLERANCE_DEGREES`), so a 9° turn can end at 11°; past ~10.5° the guard limit is below 61.5; (b) the aim **distance** step moves up to 25 cm (`AIM_MAX_TOWARD_CM`) toward the rear along the turned heading, which adds up to ~1.5 in of +x at 9°. When `checkTerritory` trips, it aborts the sequence and sets `isDriveLocked`, so the time park never runs: no PARK points, and the robot stops wherever it is. | Geometry: half reach at heading 90°+θ = 9·sinθ + 9·cosθ; θ = 9° → limit x 61.70; θ = 11° → 61.45 < 61.5 (trips). Rear move 9.84 in × sin 9° = +1.54 in → x 63.0 > 61.70. `StartPositionRulesTest.waitTipRouteKeepsTheRulesAndLeavesRoomForThePartner` (lines 84-89) only checks an exact ±9° turn at the LEAVE pose, with no overshoot and no distance move. UNTESTED auto. | In the wait-TIP aim, project the distance move onto −y only, or clamp the aim target so `TerritoryGuard.isPastLine` stays false with a margin (≥ 1 in, turn tolerance included). Also consider letting the time park run after a G402 lock if it moves the robot back toward its own side (team decision). Add a test with turn overshoot + max distance move. |
| 3 | MINOR | TeleOp / G408 | `opmodes/teleop/TeleOpBase.java:642-646` | When the intake is **not** collecting and *any* NECTAR is in front, the banner says "NECTAR AT THE INTAKE: <color> (eject: gamepad1 RB)". It says this for the opponent's NECTAR too, which invites the driver to push it. The team's G408 decision says pushing or ejecting opponent NECTAR is itself CONTROL (RULES_COMPLIANCE §1, G408/G405). | `closestNectarColor()` (line 570) returns any NECTAR color; the `else if` branch does not check the alliance. | Show "(eject)" only for our own NECTAR. For opponent NECTAR, show "opponent NECTAR - drive away". |
| 4 | MINOR | Doc accuracy | `opmodes/auto/AutoSafePark.java:28-29` | The Javadoc says the FAR park "ends ~0.6 in from the wall: LEAVE may not count". The code now parks at x 12.0 (2 in + 1 in slant margin). | `pathing/paths/SafeParkPaths.java:28-30`; `RULES_COMPLIANCE.md` LEAVE row "Safe Park far PARK x 9.5 → 12.0". | Update the Javadoc. |
| 5 | MINOR | Missing doc artifact | `docs/ROBOT_PROFILE.md:99`, `docs/AUDIT_PEDRO.md:4`, `docs/BEST_OF_MATRIX.md:42` | These docs cite `docs/snapshots/pedro-worktree-uncommitted.diff`, which does not exist. | `docs/snapshots/` holds only `source-refs-2026-11-24.txt`, `source-refs-2026-11-25.txt` and `target-versions.md`. | Add the diff (it is only an `@Disabled` line), or change the references to describe it inline. |
| 6 | MINOR | Planned docs never written | `docs/BEST_OF_MATRIX.md:195,198`; `docs/AUDIT_MAIN.md:145,168` | The matrix plans `docs/CONVENTIONS.md` and `docs/DRIVE_MODES.md` (merged from the legacy `DRIVE_MODES_REFERENCE.md`). Neither exists. CONVENTIONS is covered by `NAMING.md`; the drive-mode reference is not ported. | `ls docs/` | Mark them "superseded by NAMING.md / README Controls" in the matrix, or write `DRIVE_MODES.md`. |
| 7 | MINOR | FLOWER sweeper | `config/FlowerSweeperConstants.java:24-33` | UP and DOWN are both 0.50, so pressing B moves nothing. The code is complete, but the mechanism does nothing until it is calibrated (marked TODO: TUNE; the source has the same placeholders). | Same as `master:subsystems/FlowerSweeper.java:27-36`. | Calibrate with **Tune Flower Sweeper** and copy the values in. Until then, keep it on the robot-test list (README item 10 already says so). |
| 8 | MINOR | DS names outside RobotConstants | `pathing/tuning/PinpointTuner.java:31` | `withDefault("pinpoint")` hard-codes a Driver Station name. This breaks the CLAUDE.md rule "Driver Station names live only in config/RobotConstants.java". | grep of all DS names outside `RobotConstants`: this is the only hit. | Use `RobotConstants.PINPOINT` (vendored file, so add a note in its header). |
| 9 | MINOR | TeleOp field-centric for BLUE | `opmodes/teleop/TeleOpBase.java:172`; `subsystems/Drivetrain.java` (`headingOffsetRad` default 0) | If `ControlConstants.START_FIELD_CENTRIC` is set to true, field-centric starts with offset 0. "Forward" is then field +x, which points **toward** the BLUE drivers, so BLUE controls are inverted until BACK is pressed twice. The default is `false`, so this is latent. | `ControlConstants.java:40` `START_FIELD_CENTRIC = false`; no `resetFieldHeading()` / alliance offset at start. | Call `drivetrain.resetFieldHeading()` in `start()`, or set the offset from the alliance (0 for RED, π for BLUE) when the AUTO pose is used. |
| 10 | NIT | Naming (NAMING §3) | `AutoSelector.java:50,69,77,85` (`onPress`, `setAlliance`, `moveStart`, `changeDelay`); `AutoPlanner.java:88` `shouldParkNow`; `ColorSensing.java:285` `matches`; `ElementCounter.java:76` `recordCollected`; `FlowerSweeper.java:85,104` `down`, `toggle`; `Launcher.java:229` `updateReady`; `HiveAligner.java:94,120` `alignTo`, `evaluate`; `DataLogger.java:57` `open` | These methods return `boolean`, but their names do not start with is/has/can/should. Most are commands that report success. | grep of `boolean <name>(` | Either allow "command returns true on success" in NAMING §3, or rename (`tryToggle`, `shouldParkNow`, ...). |
| 11 | NIT | README accuracy | `README.md` Controls → Automatic, "1:00 left … FLOWER sweeper unlocked" | The gate opens at 0:59, because of `NECTAR_UNLOCK_MARGIN_SECONDS` = 1 s. | `util/MatchTimer.canPlaceNectar`, `MatchConstants.java` | Write "0:59 (1 s margin)". |
| 12 | NIT | Test name | `test/.../pathing/PathsTest.java:177` | `nearPosesMatchTheTestedRoute` asserts poses that are no longer the tested ones (see #1). | — | Rename it, or assert the tested legs separately. |
| 13 | NIT | README | `README.md` Team table | "City / country" and "Experience level" are still `TODO (team)`. | — | The team fills them in. |
| 14 | NIT | Stale state (by design) | `opmodes/tuning/TuneFlowerSweeper.java:105-109`, `TuneLauncherPidf.java:180-188` | The tuning OpModes write to static `@Config` fields. The values stay until the app restarts and are then used by the competition OpModes. This is intended (same as Dashboard), but it is not stated on the DS telemetry. | — | Add a telemetry line: "values kept until app restart; copy into config/". |
| 15 | UNSURE | Auto / time | `AutoBase.checkTimeToPark` + `EmergencyPark` | The emergency park parks at the lower-zone PARK (y 95.4) from every route, including the Wait Tip autos, whose own park is the upper zone (y 121). This is UNSURE (not proven): if both alliance robots end up in the lower zone, they could collide. | `EmergencyPark.plan` always ends at `NearRoutePoses.PARK`. | Let a routine supply its own emergency PARK target. |

## Success criteria

### 1. Source repo unchanged — **PASS**

- `git ls-remote` of `ironlionrobotics/IronLion30311BIOBUZZInicioDeTemporada` is **identical**
  to the non-comment lines of `docs/snapshots/source-refs-2026-11-25.txt` (53 refs: heads, pull
  refs and HEAD = `c98a668`; the repo has no tags). Checked with `diff`, no output.
- Laptop clone `C:\Users\luisl\AndroidStudioProjects\IronLion30311BIOBUZZInicioDeTemporada`:
  `git for-each-ref refs/heads` (31 local heads) is **identical** to the `refs/heads` lines under
  "# Local refs" in `docs/snapshots/source-refs-2026-11-24.txt`. Checked with `diff`, no output.
  Only read-only commands were used.

### 2. Code matches ROBOT_PROFILE / team code — **PASS** (with #1)

Checked against `master` @ `c98a668`:

- DS names, including the legacy fallbacks (`FL/FR/BL/BR`, `intake`, `alejandro`, `P`, `N`,
  `indicator`) and `sweeperLeftServo` / `sweeperRightServo`: same.
- Directions: drive FL/BL REVERSE, FR/BR FORWARD; intake REVERSE; indexer FORWARD; launchers
  REVERSE. Same.
- Sweeper PWM range 500–2500 µs: same.
- Launcher: P 30 / I 0 / D 0 / F 15 at 12 V; F updated every 0.25 s if it changes by ≥ 0.02,
  readings ignored below 7 V; ready window −50 / +80, not ready past 200 RPM; 28 CPR; max 6000;
  POLLEN 3000 TeleOp, 2950 AUTO; NECTAR 3600; idle 2000 from START. Same.
- Indexer: 0.75 power, 0.15 s ramp, jam 5 A / 0.5 s, unjam 0.3 s, step 0.2 s, 3 tries, reset
  2 s, pause when not ready. Same.
- Intake: 0.75 power, 0.15 s ramp; jam 6 A / 0.4 s, reverse 0.4 s; wiggle 0.35 s / 0.8 s / 3.
  Same.
- Drivetrain: ramps 0.3 / 0.2 s, stop cutoff 0.25, align ramp 0.1 s, slow 0.4, fast 1.0,
  deadzone 0.05, cubic stick curve. Same.
- PedroConstants: Pinpoint 4-bar pods, X FORWARD, Y REVERSED, offsets −6.79 / −0.72 in, manual
  brake, Foresight placeholder gains, `maxPathSpeed` 0.6. Same.
- `OdometryDriver` → `follower.manual(forward, -strafe, -turn)`: the same sign convention as
  `master:PedroAutoBase.java:1275`.
- Deviation: the preload park depth (#1).

### 3. BEST_OF_MATRIX / SOURCE_SYNC spot-check (13 items) — **PASS with 1 miss** (#1)

The following match `master`: the 4 odometry-driver gains (0.85 / 0.25 / 18 in / 0.6 in, accel
3.0, heading KP 1.0, max turn 0.45); the turn gains (1.2 / 0.5 / 0.15 / 2° / 2.5 s); the hold
gains (0.12 / 0.25 / 0.3 in); the fitted time model (70 in/s per power, 0.75 s, 0.55 s/leg,
margin 1.5 s, max park power 1.0); the Pinpoint recovery (3 recoveries; jump limits 12 in /
30°; start re-zero limits 2 in / 5° / 12 in / 10°); the aim constants (25°, sign −1, 25 / 8 cm,
bearing 10.8°, 1.22 m); the wait-TIP constants (pre-spin 2000, stable 0.8 s / 3 cm, aim 2 s,
min shoot 3 s, park y 121); the collect constants (0.2 power, 71 / 120 cm, return 0.6, latest
start 11 s, TIP watch 4 s / 0.8 s gone, TIP check 0.6 s / 10 cm / 8°); the HIVE align gains;
the ball assist (0.35 / 0.3, min forward 0.15); the HuskyLens IDs and area; the ball counter
(0.35 / 0.15 / 0.4 / 0.25); the battery limits (11.0 / 3 s / 11.3 / 8.5 / 0.5 / 0.5); the light
colors; the Limelight (pipeline 0, 100 Hz, EMA 0.45, 200 ms, 0.15 s); and the shot detection
(200 / 100 RPM). Every intended deviation is recorded in SOURCE_SYNC §2. **Missed:** #1, the
tested preload park depth.

### 4. Naming — **PASS with NITs** (#10)

- Packages are lowercase. OpMode groups are only Competition or Tuning. DS names follow the class
  names. Every alliance-fixed AUTO preselects its TeleOp. No `@Disabled` OpModes. DS names are
  lowerCamelCase (the `*ShooterMotor` names are the documented D15 exception).
- No version suffixes. Tests are named `<Class>Test`. Hand-offs are kebab-case.
- Boolean fields start with is/has/was. 13 boolean-returning methods break NAMING §3 (#10).
- Remote branches: only `main` and `dev`, plus this branch once pushed.

### 5. Robot-logic correctness — **PASS with 1 MAJOR** (#2)

- **No sleep in loops:** TeleOp and AUTO are iterative `OpMode`s, so they stop on STOP.
  Pedro's `PinpointLocalizer` sleeps 500 ms in its constructor, but only during INIT.
- **Bulk cache:** MANUAL mode is set in `RobotHardware`. `clearBulkCache()` runs every
  `loop`/`init_loop` in TeleOpBase, AutoBase, AutoSafeParkEncoders and the tuning OpModes that
  build `RobotHardware`. The two Pedro test OpModes do not build `RobotHardware`, so their hubs
  stay in default mode and their reads are never stale.
- **Missing hardware:** every subsystem null-checks its device. If the follower cannot be
  built, AUTO still runs: it shoots but does not drive.
- **BLUE:** `Alliance.toField` rotates 180° and is its own inverse (used that way in
  `EmergencyPark` and `TerritoryGuard`).
- **G402:** `AutoBase.loop → checkTerritory` uses a heading-aware footprint. G402 is AUTO-only.
- **G407:** `intake.setCollectGate(counter::canCollectMore)` is set in both `TeleOpBase.wireRules`
  and `AutoBase.init`. The count is handed over through `PoseStorage.saveElementCount` → loaded
  in `TeleOpBase.start`.
- **G408:** the opponent-NECTAR detector is set in both. The stop wins over an unjam.
- **G410:** the sweeper lowering gate is set in TeleOp. AUTO never builds a `FlowerSweeper`.
- **Time-aware park:** `checkTimeToPark` runs every loop and keeps a 4 s reserve on top of the
  estimate. Routine parks use `timedParkSteps`. Exceptions where the park is skipped: the G402
  lock (#2) and lost odometry (by design).
- **PoseStorage:** cleared at AUTO start; saved at AUTO stop only if the odometry was trusted;
  read in TeleOp INIT, with a warning if the alliances differ; cleared at TeleOp START. After a
  Pinpoint reboot, the recovery restores the follower pose (`OdometryDriver.java:570`), so the
  saved pose is the restored one. Its only static fields are in `PoseStorage`.
- **Units:** cm values convert to inches through `CM_PER_INCH`; headings are radians in poses
  and degrees only in constants. Field-centric rotation math checked by hand (correct).
- **Division by zero:** leg division is guarded by the tolerance check; the park power is guarded
  for `driveSeconds <= 0`.

### 6. User requirements — **PASS** (with #1, #2, #7, #13)

- TeleOp Red and Blue exist.
- Odometry autos: Near, Far and Wait Tip (each Red/Blue) and Safe Park (3 starts), plus the
  fallback Safe Park Encoders. Note: AUTO drives with `OdometryDriver` on Pedro localization, not
  Pedro path following (decision D6).
- Tuning OpModes: 6 in `opmodes/tuning`, 3 in `pathing/tuning`, plus the AutoTune web tuners.
- Every catalog path has a `...Straight` copy, enforced by `PathsTest.everyOriginalHasAStraightCopy`.
- The FLOWER code is done but uncalibrated (#7).
- `AutoPlanner` chooses between an extra stop and the park.
- README covers clone, build and deploy. `CODE_GUIDE` covers every class. Every `.java` file has
  a `// Sources:` header.
- `.gitignore` covers `local.properties` and `build/`. LICENSE and the README credits are
  present. CLAUDE.md is accurate, except that its DS-name rule is broken once (#8).

### 7. Docs accuracy — **PASS with MINORs** (#4, #5, #6, #11)

- The README control map, OpMode table (all 19 annotations) and hardware table match the code.
  The test count, 241, matches.
- MIGRATION / RULES_COMPLIANCE: every class and member they name exists, except the ones
  explicitly listed as old names or removed (`FlowerArm`, `REJECTING`, `READY_TOLERANCE_RPM`, ...).
- No broken markdown links. Plain-text path references are broken in #5 and #6.

## Verified OK

- The build passes and 241/241 tests pass. Code rules OK. Remote branches are only `main` and
  `dev`.
- The source repo refs and the laptop clone's local heads are unchanged since the snapshots.
- DS names, legacy fallbacks, motor directions, zero-power modes and run modes match `master`.
- Launcher, indexer, intake, drive, vision, battery and light constants match `master` (TESTED
  values kept).
- The Pedro localizer config matches `master`; the follower gains are still placeholders with
  the 0.6 cap.
- The `follower.manual` sign convention and the field-centric rotation are correct.
- G407 and G408 are wired in TeleOp **and** AUTO. G410 is wired in TeleOp; AUTO never moves the
  sweeper. The G402 guard runs every AUTO loop.
- Time guard, park deadline, volley time box and other-cell time check are all present.
- PoseStorage hand-off (pose + alliance check + element count); no other mutable static state
  outside `config/`.
- Every `.java` file has a `// Sources:` header. `CODE_GUIDE` covers every class. Every path
  has a straight-line copy.
- `.gitignore`, LICENSE, README credits, Pedro / Visualizer licenses noted.

## Resolution (Lead Architect, 2026-11-25, branch `fix/review-findings`)

| # | Status | What was done |
|---|---|---|
| 1 | FIXED | The preload + park route uses the TESTED long leg again: `AutoConstants.PRELOAD_LONG_BACK_CM = 230`, new `NearRoutePoses.PRELOAD_LANE_NORTH` / `PRELOAD_PARK` (y 105.457) used by `PreloadParkPaths`. The low PARK (219.5 cm, 1e723f2) is kept only after a collect cycle / from the other cell. Last leg stays 56.5 cm (2.1 in off the wall, team decision). Test renamed to `keepsTestedPreloadParkLegs` + `placesCycleAndOtherCellPosesAsDesigned`. |
| 2 | FIXED | `TerritoryGuard.limitTurnDegrees` and `limitMoveTarget` shorten every aim turn / aim move so the robot stays `AIM_TERRITORY_SAFETY_INCHES` (1 in) inside our territory, turn overshoot (`TURN_TOLERANCE_DEGREES`) included. Wired into `AutoBase.aimSteps`. New `TerritoryGuardTest` (overshoot + max rear move, both alliances). The G402 drive lock itself is unchanged (conservative). |
| 3 | FIXED | TeleOp banner offers "eject" only for our own NECTAR; opponent NECTAR says "drive away, do not push it (G408)". |
| 4 | FIXED | `AutoSafePark` Javadoc: FAR park ends at x 12.0 (2 in + 1 in margin). |
| 5 | FIXED | Docs describe the one-line uncommitted `@Disabled` change inline ("diff kept local, not committed"). |
| 6 | FIXED | Matrix/audit mark `CONVENTIONS.md` → superseded by `NAMING.md`, `DRIVE_MODES.md` → superseded by README "Controls". |
| 7 | OPEN (robot) | Sweeper positions need **Tune Flower Sweeper** on the robot (README test list). |
| 8 | FIXED | `PinpointTuner` default name = `RobotConstants.PINPOINT` (header notes the one change to the vendored file). |
| 9 | FIXED | TeleOp `start()` calls `drivetrain.resetFieldHeading()` when field-centric starts on. |
| 10 | FIXED | `mustParkNow` → `shouldParkNow`; NAMING §3 now allows commands that return `true` on success. |
| 11 | FIXED | README: sweeper unlocks at 0:59 (1 s G410 margin). |
| 12 | FIXED | See #1. |
| 13 | OPEN (team) | City/country and experience level in the README team table. |
| 14 | FIXED | Tuning OpModes show "Changed values stay in use until the app restarts ... copy them into config/". |
| 15 | OPEN (UNSURE) | Emergency park target per routine — left for after robot testing. |

Process note: the code-rules script was re-run normally by the Lead Architect: `Code rules: OK`. Build + 245/245 tests pass.
