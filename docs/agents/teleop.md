# TeleOp & Driver Experience agent hand-off

Branch `agent/teleop` (from `origin/dev` @ f11a02b). Build: `./gradlew assembleDebug testDebugUnitTest`
passes; new `TeleOpLogicTest` (8 tests, 0 failures).

Base source: `VA` = `feature/turrubiates/vision-align` @ a7b8f4c (`MainTeleOp`, `DriveWheelTest`,
`HiveStateProbe`). Nothing was changed in files owned by other agents, except an **addition** to
`config/ControlConstants.java` (TeleOp rumbles and options; the three existing values are unchanged).

## Files

| File | What |
|---|---|
| `opmodes/teleop/TeleOpBase.java` | Iterative OpMode with all TeleOp logic |
| `opmodes/teleop/TeleOpRed.java`, `TeleOpBlue.java` | `@TeleOp(name = "TeleOp Red" / "TeleOp Blue", group = "Competition")`, only set the alliance |
| `opmodes/teleop/TeleOpLogic.java` | Pure helpers (light choice, volley size, assist colors, clock text) |
| `opmodes/teleop/EndgameAlerts.java` | Fires the 65 s / 60 s warnings once each |
| `opmodes/tuning/TuneLauncherPidf.java` | "Tune Launcher PIDF" |
| `opmodes/tuning/TuneColorSensor.java` | "Tune Color Sensor" |
| `opmodes/tuning/TuneHiveVision.java` | "Tune Hive Vision" (port of VA `HiveStateProbe`) |
| `opmodes/tuning/TestDriveWheels.java` | "Test Drive Wheels" (port of VA `DriveWheelTest`) |
| `opmodes/tuning/TestIndexerIntake.java` | "Test Indexer Intake" |
| `config/ControlConstants.java` | + rumble values (from VA), telemetry/field-centric/odometry/log switches, auto-shoot time |
| `config/TuningConstants.java` (new) | Steps and powers of the tuning OpModes |
| `src/test/.../opmodes/teleop/TeleOpLogicTest.java` | Tests for `TeleOpLogic` and `EndgameAlerts` |

## Gamepad control map

**Driver 1 (gamepad1)**

| Input | Action | vs `MainTeleOp` |
|---|---|---|
| Left stick | Drive forward/back + strafe (deadzone + cubic) | same |
| Right stick X | Turn | same |
| LT (hold) | Brake (rumbles if the sticks move) | same |
| A / Y | Slow / fast mode | same |
| LB (hold) | Intake collect + ball assist turn (when not turning) | assist now ignores the opponent's NECTAR |
| RB (hold) | Intake eject | same |
| B (tap) | Flower arm deploy / retract. Deploy is refused (short rumble) before ENDGAME (G410) | **new** (B was "RED" in INIT) |
| X (tap) | Reset the element count to 0 (G407 counter override) | **new** (X was "BLUE" in INIT) |
| BACK (tap) | Field-centric on/off. Turning it on makes the current robot heading "away from the driver". Needs the Pinpoint | **new** |

**Driver 2 (gamepad2)**

| Input | Action | vs `MainTeleOp` |
|---|---|---|
| RT (hold) | Fire POLLEN (indexer feeds only when ready and aimed) | same |
| LT (hold) | Fire NECTAR | same |
| RB / LB (hold) | Reverse POLLEN / NECTAR | same |
| D-pad up / down | POLLEN target ±50 RPM | same |
| D-pad right / left | NECTAR target ±50 RPM | same |
| X / Y / B (tap) | Mark the last shot SHORT / LONG / WIDE (match log + short rumble) | same idea; marks go to the match CSV (the VA `ShooterLogger` is not ported) |
| A (tap) | Auto-shoot volley (`ShootingSequence`: spin up, feed one ball per ready window, shake a stuck ball, stop by itself). Tap again, or touch any intake/launcher input, to cancel | **new** |
| BACK (tap) | COMPETITION / DEBUG telemetry | **new** |

**Automatic**

| When | What |
|---|---|
| INIT | Light shows the alliance (red/blue); missing devices, cameras, odometry, AUTO pose handoff listed |
| 65 s left | Both gamepads: 3 blips |
| 60 s left | Both gamepads: long rumble; banner `*** ENDGAME ***`; flower arm unlocked; idle light **blinks** the speed-mode color |
| Battery low | Both gamepads rumble once, banner |
| Indexer jam stop | gamepad2 rumbles until fire is released |
| Robot full (4 elements, G407) | Collect refused (`Intake` FULL), gamepad1 short rumble, telemetry "FULL (gamepad1 X resets)" |
| Opponent NECTAR at the intake while collecting (G408) | Intake pushes it out for `REJECT_EJECT_SECONDS` |
| Firing with `VisionConstants.HIVE_ALIGN_ENABLED` | Auto-align to our upward cell while the sticks are idle (as VA, still disabled by default) |

Lights while running (VA): firing → white (aligning) / solid yellow (feeding) / blinking yellow
(spinning up); otherwise red = fast, blue = slow; in ENDGAME the speed color blinks.

## What changed vs `MainTeleOp` (and why)

1. One OpMode per alliance instead of X/B in INIT: the alliance can no longer be forgotten,
   and the AUTO agent preselects "TeleOp Red"/"TeleOp Blue". This freed gamepad1 X and B.
2. Iterative `OpMode` + subsystems + `hardware.clearBulkCache()` every loop (bulk caching is MANUAL).
3. New: G407 gate, G408 rejection, G410 flower arm gate, endgame alerts/banner, auto-shoot,
   competition telemetry, field-centric option, match CSV log, HIVE TIP counter on telemetry.
4. Ball assist only turns toward POLLEN and our own NECTAR.
5. Not ported: the VA `MotorDiagnosticsLogger`, `ShooterLogger` and `MatchSummary` files. The
   new `match_logs/match_logs_TeleOpRed_*.csv` has RPM, battery, shots, states, pose and events
   (marks, endgame, auto-shoot) at 10 Hz (`ControlConstants.MATCH_LOG_*`).

## TeleOp details

- **Loop order:** clear bulk cache → read gamepads → Pinpoint update → alerts → toggles → drive,
  aim, mechanisms or auto-shoot → `countWhileIntaking` + NECTAR alert → every `periodic()`
  (battery, color sensing, launcher, element counter, vision, intake, indexer, drivetrain, flower
  arm, lights) → log → telemetry.
- **Pose handoff:** if `PoseStorage.hasPose()`, the Pinpoint localizer (Pedro `PinpointLocalizer`
  only, no follower, so it does not fight the `Drivetrain`) starts from the AUTO pose; INIT warns
  if AUTO ran for the other alliance. `PoseStorage.clear()` runs in `start()`.
- **Telemetry:** COMPETITION (default, `START_IN_COMPETITION_TELEMETRY`) = banner + warnings,
  POLLEN RPM, elements, HIVE TIPs, drive mode, auto-shoot. DEBUG = every subsystem, pose, loop
  time, align state, marks, log file and `Graph/*` values for FTC Dashboard.

## OpMode list

| Name | Group | Purpose |
|---|---|---|
| TeleOp Red / TeleOp Blue | Competition | Matches |
| Tune Launcher PIDF | Tuning | Hold an RPM; A/B flywheel on, X select, D-pad target, Y gain, RB/LB ±, RT feed; Dashboard `Graph/TargetRpm` vs `Graph/Rpm`; CSV `launcher_logs` |
| Tune Color Sensor | Tuning | HuskyLens blocks (ID, position, size, area) and classification; LB/RB intake, X reset |
| Tune Hive Vision | Tuning | Orientation signals per cell, labeled per-tag CSV (`hive_logs`), slow Pedro drive |
| Test Drive Wheels | Tuning | LB/RB/LT/RT one wheel, A all; direction + encoder sign |
| Test Indexer Intake | Tuning | Stick power, A/B normal power, current + peak + time above jam threshold; CSV `current_logs` |

Not duplicated: Test Pedro Pose, Test Odometry Square, Test Odometry Driver, AutoTune (`pathing/tuning`).

## What to test on the robot

1. Test Drive Wheels: every wheel forward; A drives straight.
2. TeleOp Red: same feel as `Main TeleOp` (sticks, brake, slow/fast, LB/RB intake, RT/LT fire,
   D-pad RPM). Loop time in DEBUG telemetry (Pinpoint read adds a few ms; set
   `USE_ODOMETRY_IN_TELEOP = false` if it hurts).
3. gamepad2 A auto-shoot with 1–4 balls: stops by itself, manual input cancels it.
4. Rumbles at 1:05 and 1:00; flower arm refused before 1:00, works after.
5. G407: collect 4 balls → intake stops; check the count is right (Tune Color Sensor), X resets.
6. G408: opponent NECTAR in front of the intake is pushed out.
7. Field-centric: BACK while facing away from the drivers, then drive.
8. Pose handoff: run an AUTO, then TeleOp; INIT shows the AUTO pose.
9. Match CSV in `/sdcard/FIRST/match_logs`.
10. Test Indexer Intake: measure normal vs jam current; Tune Launcher PIDF: confirm P 30 / F 15 still holds 3000 RPM.

## Open questions

1. Are the new buttons OK for the drivers (gamepad1 B flower arm, X count reset, BACK
   field-centric; gamepad2 A auto-shoot, BACK telemetry)?
2. Field-centric "forward" is set by the driver (BACK). Should it come from the alliance and
   the field instead (needs the driver-station wall direction in the Pedro frame)?
3. Should G407 trust the HuskyLens count at all? A wrong count blocks collecting until X.
4. Auto-shoot uses POLLEN only (as `ShootingSequence`); is a NECTAR version needed for ENDGAME?
5. Endgame rumble pattern (3 blips at 65 s, long at 60 s) — drivers to confirm.
6. Shot marks are only in the CSV; port the VA `ShooterLogger` per-shot file if still used.
