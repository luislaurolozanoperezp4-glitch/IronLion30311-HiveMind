# Hand-off: QA & CI Agent (`agent/qa-ci`)

## Summary

| Item | Before | After |
|---|---|---|
| JUnit tests (`testDebugUnitTest`) | 139, all pass | 203, all pass (+64) |
| javac `-Xlint:all` warnings in TeamCode | 4 (`this-escape`) | 1 (`FlowerArm`, left for the Rules agent) |
| Android lint warnings in TeamCode code | 11 (9 DefaultLocale, 2 SdCardPath) | 5 (3 in upstream `MecanumTuner`, 2 SdCardPath) |
| GitHub Actions runs | 0 ever | first runs: dispatch run `45558725272` = **success** (build + code-rules) |

## 1. CI investigation

Checked with `gh api`:
- Repo public, not archived/disabled; `actions/permissions` = enabled, `allowed_actions: all`;
  default workflow token permissions `read`; no self-hosted runners needed.
- Workflow `Build` (id 429750004) is `active`, registered 2026-11-24 19:23 (-06:00).
- Before this branch: `actions/runs` total 0. The dev tip (`20f2e55`, merge of PR #5) has only
  the `vercel` and `claude` check suites; no `github-actions` check suite was ever created, so
  the push/PR events after the workflow landed (PR #4 and #5 merges at 00:01 and 00:05) did
  not start any run. GitHub did not report an error anywhere we can read.
- The YAML was valid (reviewed by hand; actionlint is not installed). The `on:` filters were
  right for those events, `gradlew` is committed `100755` with LF endings.
- After adding `workflow_dispatch:` and pushing `agent/qa-ci`,
  `gh workflow run build.yml --ref agent/qa-ci` worked at once:
  - run `45558721891`: cancelled (replaced by the second dispatch through `concurrency`);
  - run `45558725272`: **completed / success** (`build` and `code-rules` jobs; `branch-name`
    skipped as designed outside PRs).
- Conclusion: Actions works for this repo now. The most likely cause of the missing runs is an
  account-side gate that was lifted later (new accounts are sometimes held until GitHub
  verifies them) — this cannot be confirmed through the API. **Next check:** the next PR into
  `dev` should show a `Build` check; if it does not, open Settings -> Actions on the repo and
  the account's billing/verification page.

Workflow changes (`.github/workflows/build.yml`):
- `workflow_dispatch` (manual run), `permissions: contents: read`, `concurrency` per ref,
  `timeout-minutes`.
- JDK 21 Temurin (AGP 8 / Gradle 9.1 need 17+), `gradle/actions/setup-gradle@v4` (cache +
  wrapper validation), test reports and JUnit XML uploaded as the `test-reports` artifact.
- New job `code-rules` running `.github/scripts/check-code-rules.sh` (no build, about 5 s).

## 2. Code-rules check (`.github/scripts/check-code-rules.sh`)

Runs in CI and locally (`bash .github/scripts/check-code-rules.sh`). Fails on:
1. `sleep(` / `Thread.sleep(` in `opmodes/`, `subsystems/`, `statemachine/`, `util/`,
   `hardware/` (the upstream Pedro tuners in `pathing/tuning/` are allowed to block).
2. A TeamCode `.java` file whose first line is not `// Sources: ...`.
3. File names ending in `V2`, `_old`, `Old`, `Final`, `Copy`, `Backup`, `Test2`, ...
4. Common Spanish words in TeamCode code or comments (Sources lines skipped).
5. `@TeleOp` / `@Autonomous` without `group = "Competition" | "Tuning" | "Dev"`.

Verified both ways: passes on the current code; a planted bad file triggered all 5 checks.

## 3. Tests added

| File | What it checks |
|---|---|
| `util/GamepadExTest` | just-pressed / just-released fire once, re-tap fires again, trigger threshold, stick deadzone + cube + forward sign, null gamepad is safe |
| `util/PoseStorageTest` | save / replace / clear of the AUTO -> TeleOp pose and alliance |
| `util/ControllerEdgeCasesTest` | PIDF: no I/D on first call, long pause clamped to 0.1 s, tiny dt ignored, reset clears history, static FF sign, negative limits, live gain change; slew: negative dt, negative ramp, zero-ramp reversal stops at 0, decel backwards, live ramp change |
| `subsystems/BallCounterTest` | count after lost-confirm time, far ball not counted, intake off, next-ball jump, min pickup interval, nearest = lowest, reset, summary |
| `subsystems/BatteryMonitorTest` | start/min volts, invalid reading ignored, read interval, low after time filter, hysteresis clear, short dip, sag hold + event count |
| `subsystems/vision/HiveCellTest` | every HIVE tag maps to exactly one cell and index, out-of-range tags, scoring/audience cells per alliance, `BallColor` opponent/own NECTAR |
| `statemachine/CollectSequenceTest` | idle before start, approach + turn clamp, drive-on after the ball goes under the camera, no ball, time up, cancel, ball count from start, restart; `StateMachine` same-state vs restart timer, previous state |
| `pathing/AutoPlannerTest` | deadline includes the exact second, shoot time capped and never negative, empty/one-point legs, leg sum with overhead, turn sign, spin-up only volley |

`com.qualcomm.robotcore.hardware.Gamepad` can be built in JVM tests (fields are public), so
`GamepadEx` is tested with the real class, no fake.

## 4. Bugs found

None in the tested logic: every new test passed against the existing code. Notes (not
changed, behavior questions for the owners):
- `PidfController.setIntegralLimit` clamps the accumulated error-seconds, not `kI * integral`
  as its Javadoc ("largest absolute value the I term may reach") suggests. Same result only
  when `kI = 1`. Fix either the doc or the code when the launcher PIDF is tuned.
- `BallCounter`: once a near ball is remembered, it stays remembered even if it moves back up
  the image (robot reversing); it is then counted when it leaves the view. Estimate only, as
  the class says.

## 5. Warnings and lint fixed (no behavior change)

- `TeamCode/build.gradle`: `-Xlint:all` (minus `options`, `processing`, `classfile`) for
  TeamCode, so new javac warnings show in every build.
- `this-escape`: made the setters called from constructors `final`
  (`PidfController.setGains`, `SlewRateLimiter.setRampSeconds`, `RobotHardware.getDevice`).
- Android lint `DefaultLocale`: `String.format(Locale.US, ...)` / `toLowerCase(Locale.US)` in
  `AutoSafeParkEncoders`, `TeleOpBase`, `TuneColorSensor`, `TuneLauncherPidf` (decimal points
  stay `.` on phones set to Spanish).

## 6. Naming check

Fixed (safe, local names only):
- Boolean parameters/locals: `OdometryDriver.startLeg(.., shouldPassThrough)`,
  `Drivetrain.setBrake(isBrakeOn)`, `Indexer.setPaused(shouldPause)`,
  `HiveAligner.alignTo` local `isOnTarget`, `HiveVision` local `hasHiveTag`.
- `// Sources:` header added to the 11 `package-info.java` files.

Clean already: no Spanish words, no `v2/_old/final/Test2` names, every OpMode is in group
`Competition` or `Tuning`, subsystems use `periodic()` / `addTelemetry(Telemetry t)`.

Not renamed (risky: Dashboard-visible constants used by AUTO/rules code other agents own):
- Field-coordinate constants without a unit suffix: `AutoConstants.LOADING_ZONE_START_X/Y`,
  `FieldConstants.TERRITORY_LINE_X`, `HIVE_MIN_X/MAX_X/MIN_Y/MAX_Y`,
  `RED_LOADING_ZONE_MIN_X/...` (all inches -> suggest `_IN`).
- `docs/NAMING.md` is referenced by `CLAUDE.md` and `ARCHITECTURE.md` but does not exist yet.

## 7. Other

- `.editorconfig`: UTF-8, final newline, 4-space indent (2 for YAML/JSON), LF for `*.sh` and
  `gradlew`, CRLF for `*.bat`; Java max line 100.

## Remaining issues

- `FlowerArm` constructor calls the overridable `retract()` (`this-escape`): left alone to avoid
  conflicts with the Rules agent; fix by making `retract()` final or the class final.
- Android lint `SdCardPath` in `DataLogger` / `PoseLogger` (`/sdcard/FIRST/...` is the FTC
  convention; safe to keep). `MecanumTuner` (upstream Pedro) DefaultLocale left as is.
- Confirm CI starts on the next push/PR to `dev` (see section 1).
