# CONTRIBUTING

How to change the IronLion30311-HiveMind code, for team members and for other teams who want to
reuse it. Read `README.md` first (build and deploy), then `docs/ARCHITECTURE.md` and
`docs/NAMING.md`.

## 1. For team members

### 1.1 Before you write code

1. Read `docs/ROBOT_PROFILE.md`: the code must match the real robot. If you are not sure how
   the robot is built, ask; do not guess.
2. Find the class you need in `docs/CODE_GUIDE.md`.
3. Check the competition rules your change touches in `docs/RULES_COMPLIANCE.md`.

### 1.2 The workflow

```bash
git checkout dev
git pull
git checkout -b fix/intake-jam          # <type>/<short-description>, see docs/BRANCHING.md
# ...edit...
./gradlew assembleDebug testDebugUnitTest     # Windows: gradlew.bat assembleDebug testDebugUnitTest
bash .github/scripts/check-code-rules.sh      # the same checks CI runs
git add <files>
git commit -m "fix: reverse the indexer for 0.3 s on a jam"
git push -u origin fix/intake-jam
```

Then open a **pull request into `dev`** on GitHub and fill in the template
(`.github/PULL_REQUEST_TEMPLATE.md`): what changed, why, how it was tested. Never push to
`main`; `main` only receives pull requests from `dev` (or a `hotfix/` branch at an event).

### 1.3 Rules every change must follow

| Rule | How |
|---|---|
| English only | Code, comments, telemetry, commits. CI checks for common Spanish words. |
| Java 8 | No `var`, records, or switch expressions (the FTC SDK sets language level 8). |
| Layers | OpModes call subsystems; subsystems use `RobotHardware`; only `RobotHardware` uses `hardwareMap` (`docs/ARCHITECTURE.md`). |
| Names | `docs/NAMING.md`. Driver Station names only in `config/RobotConstants.java`. |
| No magic numbers | Every number goes in a `config/*Constants` class. A value you have not tested on the robot gets `// TODO: TUNE`. Write its status in the Javadoc: TESTED, FITTED, `TODO: TUNE`, or a decision number. |
| No blocking | Never `sleep()` in TeleOp, AUTO, subsystems or state machines. Use `ElapsedTime`/`MatchTimer` and `start()/periodic()/isDone()`. |
| Missing hardware | A missing device must give a telemetry warning, never a crash. |
| Sources header | Every new or migrated Java file starts with a `// Sources:` line: `// Sources: <repo> / branch <b> / <path> @ <commit> — adapted for BIOBUZZ`, or `// Sources: new for IronLion30311-HiveMind (<what>)`. If you took an idea from another team, name the repo and license and write "idea only, no code copied" when that is true. |
| Tests | Logic without hardware gets a JUnit test in `TeamCode/src/test/java` (same package). Use `testutil/FakeClock` for anything time-based. |
| Docs | If you change controls, OpMode names, DS names or behavior, update `README.md` and the matching `docs/` file in the same pull request. |
| Rules of the game | If a change touches G402, G407, G408, G410 or G304, keep the test that covers it passing and update `docs/RULES_COMPLIANCE.md`. |

### 1.4 CI checks (must be green before merging)

| Job | Checks |
|---|---|
| `build` | `./gradlew assembleDebug testDebugUnitTest` on JDK 21 |
| `code-rules` | `.github/scripts/check-code-rules.sh`: no `sleep()` in robot code, `// Sources:` first line, no `v2`/`_old`/`Final`/`Copy` file names, no Spanish words, OpMode group is `Competition`, `Tuning` or `Dev` |
| `branch-name` | Pull-request branch matches `docs/BRANCHING.md` (max 30 characters) |

### 1.5 Testing on the robot

A pull request that changes robot behavior says how it was tested: "built and unit-tested
only", or "ran `<OpMode>` on the robot, result …". Values tuned live from FTC Dashboard are lost
when the app restarts: copy the final value into `config/` and commit it.

### 1.6 Files you should not edit

- `pathing/tuning/MecanumTuner.java`, `PinpointTuner.java`, `ForesightTuner.java`, `Tests.java`:
  vendored from the Pedro Pathing Quickstart. Update them from upstream instead.
- `FtcRobotController/`, `build.common.gradle`, `gradle/`: the FTC SDK template. Update the SDK
  with the steps in `docs/BRANCHING.md`.
- The old repository `IronLion30311BIOBUZZInicioDeTemporada` is read-only history.

## 2. For other teams (fork and reuse)

You are welcome to read, fork and reuse this code for your own BIOBUZZ robot.

1. **Fork** the repository on GitHub and clone your fork.
2. **Change the hardware names** in `config/RobotConstants.java` to your Driver Station names,
   or name your devices like ours (table in `README.md`).
3. **Check every constant** in `config/`. Our values (motor directions, RPMs, PIDF, pod offsets,
   ramps) were tuned for *our* robot; most will be wrong for yours. Start from the values marked
   `TODO: TUNE` and the tuning OpModes (group "Tuning").
4. **Run Pedro AutoTune** for your drivetrain and odometry (http://192.168.43.1:10158 with the
   Robot Controller running) before you trust any Pedro path.
5. **Keep the license and credits.** The FTC SDK license is in `LICENSE`. Files from the Pedro
   Pathing Quickstart are BSD-3-Clause-Clear. Keep the `// Sources:` headers when you copy a
   file, and add your own line saying what you changed.
6. Found a bug that also affects us? Open an issue or a pull request against `dev` in this
   repository.

## 3. Repositories that inspired this code

People contributing should study the repositories this code was built from or learned from:

| Repository | What we used | License |
|---|---|---|
| `ironlionrobotics/IronLion30311BIOBUZZInicioDeTemporada` (all branches) | Our season-start code: every mechanism, vision, TeleOp and the field-tested AUTO. Read-only history. | Team repository |
| `pedro-pathing-biobuzz-analysis-a65cc9` | A worktree of the same repo at `feature/autonomos-beta`: the Pedro routes (Tick, Center Lane, Outer Lane, Park, Shoot Turn Collect). | Team (local copy) |
| [FIRST-Tech-Challenge/FtcRobotController](https://github.com/FIRST-Tech-Challenge/FtcRobotController) | The FTC SDK v12.0 this repo is built on. | `LICENSE` (FIRST, BSD-style) |
| [Pedro-Pathing/Quickstart](https://github.com/Pedro-Pathing/Quickstart) | AutoTune tuner procedures (vendored). | BSD-3-Clause-Clear |
| [Pedro-Pathing/Visualizer](https://github.com/Pedro-Pathing/Visualizer) | BIOBUZZ field image for FTC Dashboard. | Apache-2.0 |
| [FTC22526FondyFireTech/Biobuzz_PedroPathing_V3](https://github.com/FTC22526FondyFireTech/Biobuzz_PedroPathing_V3) | Idea only: "if the HIVE has not changed by a deadline, go straight to park"; also confirmed the 180° alliance rotation. No code copied. | BSD-3-Clause-Clear |

Library documentation: [Pedro Pathing](https://pedropathing.com),
[FTC Dashboard](https://acmerobotics.github.io/ftc-dashboard/), and the official
[FTC docs](https://ftc-docs.firstinspires.org).
