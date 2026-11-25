# IronLion30311-HiveMind

Competition code of **FTC #30311 Iron Lion** for **BIOBUZZ presented by RTX** (2026–27).
It unifies the best of our season-start code and our Pedro Pathing work into one clean,
tested Java codebase: TeleOp for both drivers, time-aware autonomous routines, game-rule
safety checks, and tuning tools.

## Contents

[Status](#status) · [Team](#team) · [The robot](#the-robot) · [Versions](#versions) ·
[Getting started](#getting-started-for-a-new-student) · [Hardware configuration](#hardware-configuration) ·
[Controls](#controls) · [OpModes](#opmodes) · [Autonomous selector](#autonomous-selector-init) ·
[FTC Dashboard and AutoTune](#ftc-dashboard-and-autotune) · [Tests and CI](#tests-and-ci) ·
[Repository structure](#repository-structure) · [Documentation](#documentation) ·
[Credits](#credits) · [License](#license)

## Status

The code builds and all **241 JUnit tests pass**. Most mechanism values come from field tests of
the old `vision-align` branch, but the new code itself has **not run on the robot yet**. It is
synced with the team's source repo up to `vision-align` @ `d93cec3` (= `master` `c98a668`,
2026-11-24); per-commit decisions are in [`docs/SOURCE_SYNC.md`](docs/SOURCE_SYNC.md). Test
these on the robot, in this order (collected from the agent hand-offs in `docs/agents/`):

1. **Hardware config:** INIT any OpMode; telemetry must list no missing devices.
   **Test Drive Wheels:** every wheel forward, A drives straight.
2. **Test Odometry Driver** (Tuning): the ported point-to-point driver must repeat the tested
   near legs.
3. **Auto Red Near**, delay 0: must match the 2026-11-23 run (4/4 preload, parked in ~12–14 s).
   The last park leg changed from 60 to 56.5 cm so the robot ends 2 in off the wall, and the
   long leg now ends at the LOADING ZONE's low edge (y 95.4, source sync 1e723f2):
   **check it is still partly in the LOADING ZONE**. Check the planner and park-plan lines in the
   telemetry/CSV. Then a collect cycle: the row is now collected along the audience wall, and the
   other cell uses the angled spot and the Limelight aim (check `AIM_TURN_SIGN`).
4. AUTO INIT: delay up/down, A re-zero; push the robot 5 cm (expect "will RE-ZERO") and 30 cm
   (expect FAULT). Unplug the Limelight: the near route must still shoot and park.
5. Time guard: `Auto Red Near` with a 10 s delay must skip the cycle and park.
6. **Auto Blue Near** on the blue side (180° rotation, D13).
7. **Auto Safe Park** (all three starts, measure the beside-zone start) and **Auto Safe Park
   Encoders**.
8. **TeleOp Red:** same feel as the old `Main TeleOp`; loop time in DEBUG telemetry.
9. gamepad2 **A** auto-volley with 1–4 balls; manual input cancels it.
10. Endgame rumbles at 1:05 and 1:00; FLOWER sweeper lowering refused before 1:00. Calibrate the
    sweeper first with **Tune Flower Sweeper**. POLLEN idle spin (2000 RPM) from START, toggled
    by gamepad2 right stick button; first volley ready in ~0.7 s instead of 1.4–3 s.
11. **G407:** collect 4 balls → intake stops (FULL); gamepad1 X resets. **G408:** opponent NECTAR
    in front of the intake → the intake **stops** (it must not push the ball).
12. Field-centric (gamepad1 BACK); AUTO → TeleOp pose hand-off shown in INIT; match CSV saved.
13. **Test Indexer Intake** (normal vs. jam current) and **Tune Launcher PIDF** (P 30 / F 15 still
    holds 3000 RPM).
14. **Auto Red/Blue Far** only after the FAR start pose is confirmed (D21). **Auto Red/Blue Wait
    Tip** (back-wall start) with an alliance partner that TIPs first.
15. Intake jam protection (6 A for 0.4 s → 0.4 s reverse) and the Pinpoint reboot recovery
    (pose log `pinpoint_*` columns).
16. Pedro **AutoTune** (Mecanum → Pinpoint → Foresight), then remove the 0.6 speed cap.

Open team questions are listed in `docs/ROBOT_PROFILE.md` §0.3 and `docs/RULES_COMPLIANCE.md` §5.

## Team

| | |
|---|---|
| Team | FTC **#30311 Iron Lion** |
| Season | BIOBUZZ presented by RTX, 2026–27 |
| City / country | `TODO (team): fill in` |
| Experience level | `TODO (team): fill in (e.g. rookie / second year)` |
| Repository | Hosted on a personal GitHub account for now; it will move to the team organization later. |

## The robot

Full details, sources and open questions: [`docs/ROBOT_PROFILE.md`](docs/ROBOT_PROFILE.md).

- **Drivetrain:** 4-wheel mecanum, goBILDA 312 RPM motors, 104 mm wheels. Robot-centric, with an
  optional field-centric mode in TeleOp.
- **Odometry:** goBILDA Pinpoint with two 4-bar pods (Pedro Pathing 3 localizer).
- **Front:** one intake motor; a HuskyLens sees POLLEN and NECTAR by color.
- **Middle:** one indexer motor feeds stored balls (up to 4) into the flywheels, one at a time,
  only when the flywheel is at speed.
- **Rear:** two flywheel launchers (POLLEN about 3000 RPM, NECTAR 3600 RPM) and a Limelight 3A
  that reads the HIVE AprilTags (IDs 30–45).
- **Lights:** a goBILDA RGB indicator light; an optional NECTAR alert light.
- **FLOWER sweeper:** a bar on two mirrored goBILDA Dual Mode servos (300°) that sweeps POLLEN
  off the FLOWERS (from `vision-align` 344254b). Lowering is locked until the last 60 s while
  `FlowerSweeperConstants.LOCK_DOWN_UNTIL_NECTAR_UNLOCK` is true. Positions not calibrated yet.
- **Scoring:** POLLEN into the up-facing HIVE cell; LEAVE and PARK in AUTO; PARK at the end.

## Versions

| Part | Version |
|---|---|
| FTC SDK (FtcRobotController) | **v12.0** (`org.firstinspires.ftc:*:12.0.0`) |
| Pedro Pathing | **3.0.2** (`com.pedropathing:revhub`) |
| Pedro AutoTune | **1.0.3** (`com.pedropathing:tuning`) |
| FTC Dashboard | **0.6.0** (`com.acmerobotics.dashboard:dashboard`) |
| Language | Java only, language level 8 |
| Build | Gradle 9.1.0 wrapper, needs JDK 17 or newer (Android Studio's bundled JBR works) |

## Getting started (for a new student)

### 1. Install the tools (once)

1. Install **[Android Studio](https://developer.android.com/studio)** (current stable version).
   It includes the JDK and the Android SDK. Accept the default options.
2. Install **[Git](https://git-scm.com/downloads)** (or use Android Studio's built-in Git).
3. Optional, for log pulls and Wi-Fi deploys: the `adb` tool comes with Android Studio in
   `<Android SDK>/platform-tools`.

### 2. Get the code

```bash
git clone https://github.com/luislaurolozanoperezp4-glitch/IronLion30311-HiveMind.git
cd IronLion30311-HiveMind
git checkout dev        # day-to-day work happens on dev (docs/BRANCHING.md)
```

Or in Android Studio: **File → New → Project from Version Control**, paste the URL.

### 3. Open and sync

1. In Android Studio: **File → Open**, choose the `IronLion30311-HiveMind` folder (the one with
   `settings.gradle`).
2. Wait for **Gradle sync** to finish (bottom status bar). The first sync needs internet: it
   downloads the FTC SDK, Pedro Pathing and FTC Dashboard.
3. If Android Studio asks to upgrade the Android Gradle Plugin, choose **"Don't ask again"**:
   the FTC SDK template must not be upgraded.

### 4. Build and test on the computer

**Build → Make Project**, or from a terminal in the project folder:

```bash
./gradlew assembleDebug testDebugUnitTest          # macOS / Linux / Git Bash
gradlew.bat assembleDebug testDebugUnitTest        # Windows Command Prompt / PowerShell
```

`BUILD SUCCESSFUL` means the app compiles and every test passed.

### 5. Connect to the Control Hub

- **USB:** plug the Control Hub's USB-C port into the computer. Android Studio shows
  "REV Robotics Control Hub" in the device list at the top.
- **Wi-Fi:** connect the computer to the Control Hub's Wi-Fi network, then in a terminal run
  `adb connect 192.168.43.1:5555`. The hub appears in the device list.

### 6. Deploy and run

1. In the run configuration drop-down choose **TeamCode**, then press **Run ▶**. Android Studio
   installs the Robot Controller app with our code on the Control Hub.
2. On the **Driver Station**, check the active configuration matches the
   [hardware table](#hardware-configuration).
3. Choose the OpMode from the **TeleOp** or **Autonomous** list (see [OpModes](#opmodes)),
   press **INIT**, read the telemetry, then press **START**.

## Hardware configuration

The Driver Station configuration **must** use these names (case-sensitive). They live only in
[`config/RobotConstants.java`](TeamCode/src/main/java/org/firstinspires/ftc/teamcode/config/RobotConstants.java).
Legacy names still work as a fallback in `RobotHardware`, but **Pedro Pathing only finds the
new drive names and `pinpoint`**, so use the new names. Hub/port numbers are not recorded yet.

| DS name | Device type | Mechanism | Legacy names accepted | Required? |
|---|---|---|---|---|
| `frontLeftDrive` | Motor (DcMotorEx) | Drive, front left (REVERSE) | `FL` | yes |
| `frontRightDrive` | Motor (DcMotorEx) | Drive, front right (FORWARD) | `FR` | yes |
| `backLeftDrive` | Motor (DcMotorEx) | Drive, back left (REVERSE) | `BL` | yes |
| `backRightDrive` | Motor (DcMotorEx) | Drive, back right (FORWARD) | `BR` | yes |
| `intakeMotor` | Motor (DcMotorEx) | Intake, front (REVERSE) | `intake` | yes |
| `indexerMotor` | Motor (DcMotorEx) | Indexer (FORWARD) | `alejandro`, `indexer` | yes |
| `pollenShooterMotor` | Motor (DcMotorEx) | POLLEN launcher flywheel (REVERSE) | `P` | yes |
| `nectarShooterMotor` | Motor (DcMotorEx) | NECTAR launcher flywheel (REVERSE) | `N` | yes |
| `indicatorLight` | Servo (goBILDA PWM RGB light) | Indicator light | `indicator` | yes |
| `nectarAlertLight` | Servo (goBILDA PWM RGB light) | NECTAR alert light | — | optional |
| `sweeperLeftServo` | Servo (goBILDA 2000-0025-0002, 500–2500 µs) | FLOWER sweeper, left | `flowerArmLeftServo`, `servoLeft` | optional |
| `sweeperRightServo` | Servo (goBILDA 2000-0025-0002, 500–2500 µs) | FLOWER sweeper, right (mirrored) | `flowerArmRightServo`, `servoRight` | optional |
| `pinpoint` | goBILDA Pinpoint (I²C) | Odometry + IMU (owned by Pedro) | — | yes for AUTO |
| `huskyLens` | HuskyLens (I²C) | Ball color, front | — | optional |
| `limelight` | Limelight 3A (USB) | HIVE AprilTags, rear | — | optional |

The launcher motors keep their old DS names `pollenShooterMotor` / `nectarShooterMotor`
(decision D15); in code they are `pollenLauncherMotor` / `nectarLauncherMotor`. A missing device
never crashes the code: it shows a warning in telemetry and that mechanism does nothing. The
battery voltage comes from the Control Hub's built-in voltage sensor.

## Controls

Checked against `opmodes/teleop/TeleOpBase.java`. Details and reasons: `docs/agents/teleop.md`.

### Driver 1 (gamepad1)

| Input | Action |
|---|---|
| Left stick | Drive forward/back and strafe (deadzone + cubic curve) |
| Right stick X | Turn |
| LT (hold) | Brake (rumbles if you move the sticks while braking) |
| A / Y | Slow mode / fast mode |
| LB (hold) | Intake collect, with ball assist (turns toward the closest POLLEN or our NECTAR only while you drive **forward** and do not turn, and never while a launcher is used) |
| RB (hold) | Intake eject |
| B (tap) | FLOWER sweeper down / up (slowed move). Lowering is refused (short rumble) before the last 60 s while `LOCK_DOWN_UNTIL_NECTAR_UNLOCK` is on |
| X (tap) | Reset the element count to 0 (G407 counter override) |
| BACK (tap) | Field-centric on/off (needs the Pinpoint). Turning it on makes the current heading "away from the driver" |

### Driver 2 (gamepad2)

| Input | Action |
|---|---|
| RT (hold) | Fire POLLEN (the indexer feeds only when the flywheel is ready) |
| LT (hold) | Fire NECTAR |
| RB / LB (hold) | Reverse the POLLEN / NECTAR flywheel |
| D-pad up / down | POLLEN target RPM +50 / −50 |
| D-pad right / left | NECTAR target RPM +50 / −50 |
| A (tap) | Auto-volley: spins up, feeds one ball per ready window, stops by itself. Tap again or touch any intake/launcher input to cancel |
| X / Y / B (tap) | Mark the last shot SHORT / LONG / WIDE in the match log |
| Right stick button (tap) | POLLEN idle spin off / on (2000 RPM between volleys; on at START, off while the battery is low) |
| BACK (tap) | Switch COMPETITION / DEBUG telemetry |

### Automatic

| When | What happens |
|---|---|
| INIT | Light shows the alliance; missing devices, cameras, odometry and the AUTO pose hand-off are listed |
| 1:05 left | Both gamepads: 3 short blips |
| START | POLLEN idle spin starts (never in INIT) |
| 1:00 left | Long rumble, `*** ENDGAME ***` banner, FLOWER sweeper unlocked, idle light blinks |
| Battery low | Both gamepads rumble once, banner |
| Indexer jam | gamepad2 rumbles until fire is released |
| Intake jam | More than 6 A for 0.4 s while collecting: the intake reverses 0.4 s, then collects again |
| Robot full (4 elements, G407) | Collect refused, short rumble, telemetry "FULL (gamepad1 X resets)" |
| Opponent NECTAR at the intake (G408) | The intake **stops** (never pushes it out), gamepad1 rumbles, banner. Drive away |
| Firing with HIVE align enabled | Auto-align to our up cell (`VisionConstants.HIVE_ALIGN_ENABLED`, **off** by default) |

Lights while running: firing → white (aligning) / blinking yellow (spinning up) / solid yellow
(feeding); otherwise red = fast mode, blue = slow mode; in ENDGAME the speed color blinks.

## OpModes

Every name below was checked against its `@TeleOp` / `@Autonomous` annotation.

| DS name | Type | Group | What it does |
|---|---|---|---|
| **TeleOp Red** | TeleOp | Competition | Competition TeleOp, RED alliance |
| **TeleOp Blue** | TeleOp | Competition | Competition TeleOp, BLUE alliance |
| **Auto Red Near** | Autonomous | Competition | Near start: LEAVE, 4 POLLEN preload, time-aware cycle or park. Preselects TeleOp Red. Preload + park TESTED |
| **Auto Blue Near** | Autonomous | Competition | Same for BLUE (RED poses rotated 180°). Preselects TeleOp Blue |
| **Auto Red Far** | Autonomous | Competition | Far start: drive north of the HIVE, shoot at the cell it sees, then cycle or park. **UNTESTED** (D21) |
| **Auto Blue Far** | Autonomous | Competition | Same for BLUE. **UNTESTED** |
| **Auto Red Wait Tip** | Autonomous | Competition | Back-wall start: LEAVE, pre-spin, wait for the partner's TIP (our audience cell up and still), Limelight aim, shoot 4, park in the upper LOADING ZONE. **UNTESTED** (source `FarSideWaitTipAuto`) |
| **Auto Blue Wait Tip** | Autonomous | Competition | Same for BLUE. **UNTESTED** |
| **Auto Safe Park** | Autonomous | Competition | LEAVE + PARK only, no shooting; start chosen in INIT. No TeleOp preselect |
| **Auto Safe Park Encoders** | Autonomous | Competition | Wheel encoders only, 10 cm back: LEAVE only. For when the Pinpoint fails. No TeleOp preselect |
| Test Drive Wheels | TeleOp | Tuning | LB/RB/LT/RT spin one wheel each, A all: checks ports, directions, encoder signs |
| Test Indexer Intake | TeleOp | Tuning | Run intake/indexer by hand, show current and peak, to tune jam detection |
| Tune Color Sensor | TeleOp | Tuning | Show what the HuskyLens sees and how each block is classified |
| Tune Hive Vision | TeleOp | Tuning | HIVE orientation signals per cell, labeled per-tag CSV, slow drive |
| Tune Launcher PIDF | TeleOp | Tuning | Hold a flywheel RPM and tune the gains live (graph on FTC Dashboard) |
| Tune Flower Sweeper | TeleOp | Tuning | Find the sweeper servos' UP / DOWN positions (left, right or both mirrored; coarse/fine steps) |
| Test Pedro Pose | TeleOp | Tuning | Drive by hand and compare the odometry with the field tiles (plus Pinpoint status, Hz, pod counts); A resets to the near start |
| Test Odometry Square | Autonomous | Tuning | One-tile square with Pedro paths (needs AutoTune first) |
| Test Odometry Driver | Autonomous | Tuning | Drives the tested near legs with the odometry driver, no shooting. Run this first |

The Pedro **AutoTune** procedures are not on the Driver Station list; they run from a web page
(see below).

## Autonomous selector (INIT)

In INIT, every AUTO shows a menu on the telemetry. Use **gamepad1**:

| Button | Action | Available in |
|---|---|---|
| B / X | Alliance RED / BLUE | Auto Safe Park, Auto Safe Park Encoders (the other autos fix the alliance) |
| D-pad left / right | Start position: NEAR, FAR, beside the LOADING ZONE | Auto Safe Park |
| D-pad up / down | Start delay, 0–10 s in 0.5 s steps | every AUTO |
| A | Set the odometry back to the start pose (if the robot was moved after INIT) | every odometry AUTO (not Safe Park Encoders) |

The telemetry also shows the live **start-pose check**: OK, "will RE-ZERO" (small offset, fixed
at START) or FAULT (the robot will not drive). See `docs/MATCH_CHECKLIST.md`.

## FTC Dashboard and AutoTune

Connect the computer to the Control Hub's Wi-Fi, with the Robot Controller app running:

| Tool | URL | Use |
|---|---|---|
| FTC Dashboard | http://192.168.43.1:8080/dash | Live telemetry and graphs (`Graph/*`), the field view with the robot pose, and every `@Config` constant tunable live. Live changes are lost when the app restarts: copy good values into `config/` |
| Pedro AutoTune | http://192.168.43.1:10158 | Run the tuning procedures (Mecanum, Pinpoint, Foresight, tests) and copy the printed values into `pathing/PedroConstants.java` |

## Tests and CI

- **Unit tests** run on the computer: `./gradlew testDebugUnitTest` (241 tests). They cover the
  mechanisms' logic, the time planner, every path against the field rules, start-pose legality
  (G304) and the rule gates (G402, G407, G408, G410).
- **Code rules:** `bash .github/scripts/check-code-rules.sh` (no `sleep()`, `// Sources:`
  header, English only, no `v2`-style file names, OpMode groups).
- **GitHub Actions** (`.github/workflows/build.yml`) runs `build` (APK + tests), `code-rules`
  and, on pull requests, `branch-name` for every push and pull request to `main` and `dev`.

## Repository structure

```
IronLion30311-HiveMind/
├── TeamCode/src/main/java/org/firstinspires/ftc/teamcode/
│   ├── config/        every name and number (Driver Station names in RobotConstants)
│   ├── hardware/      RobotHardware: the only class that touches hardwareMap
│   ├── subsystems/    one class per mechanism (+ vision/ for the HIVE AprilTags)
│   ├── statemachine/  non-blocking sequences: shooting, collecting, TIP watch
│   ├── pathing/       Pedro setup, odometry driver, time planner, paths/, tuning/
│   ├── opmodes/       auto/, teleop/, tuning/ (what you pick on the Driver Station)
│   └── util/          Alliance, MatchTimer, GamepadEx, PidfController, ...
├── TeamCode/src/main/assets/images/   field image for FTC Dashboard
├── TeamCode/src/test/java/...         JUnit tests
├── FtcRobotController/                FTC SDK app (do not edit)
├── docs/                              documentation (see below)
├── .github/                           CI workflow, code-rules script, PR and issue templates
├── build.dependencies.gradle          SDK, Pedro Pathing and Dashboard versions
├── CLAUDE.md                          rules for AI coding agents
└── LICENSE                            FTC SDK license
```

## Documentation

| Doc | What is in it |
|---|---|
| [`docs/CODE_GUIDE.md`](docs/CODE_GUIDE.md) | **Start here for the code:** every class in reading order, with diagrams |
| [`docs/ROBOT_PROFILE.md`](docs/ROBOT_PROFILE.md) | The real robot, team decisions, open questions |
| [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) | Layers, package map, rules |
| [`docs/NAMING.md`](docs/NAMING.md) | Naming conventions with examples |
| [`docs/STRATEGY.md`](docs/STRATEGY.md) | Match plan, AUTO choice, TeleOp cycle, endgame, ranking points |
| [`docs/MATCH_CHECKLIST.md`](docs/MATCH_CHECKLIST.md) | Pre-event, pre-match, during, post-match, pit fixes |
| [`docs/RULES_COMPLIANCE.md`](docs/RULES_COMPLIANCE.md) | Game rules (manual TU10) and how the code enforces them |
| [`docs/CONTRIBUTING.md`](docs/CONTRIBUTING.md) | How to contribute; how other teams can reuse the code |
| [`docs/BRANCHING.md`](docs/BRANCHING.md) | Branches, pull requests, CI checks, event tags |
| [`docs/MIGRATION.md`](docs/MIGRATION.md) | What came from which old branch, every rename |
| [`docs/SOURCE_SYNC.md`](docs/SOURCE_SYNC.md) | Each new commit of the team's source repo: ported, partly ported or skipped, and why |
| [`docs/research/SHOOTING_AND_TELEOP_RESEARCH.md`](docs/research/SHOOTING_AND_TELEOP_RESEARCH.md) | Shooting and TeleOp research (summary of the team's report) and what was implemented |
| [`docs/BEST_OF_MATRIX.md`](docs/BEST_OF_MATRIX.md) | How the best version of each piece was chosen |
| [`docs/AUDIT_MAIN.md`](docs/AUDIT_MAIN.md) · [`docs/AUDIT_PEDRO.md`](docs/AUDIT_PEDRO.md) | Audits of the old code and the Pedro work |
| [`docs/agents/`](docs/agents) | Hand-off notes of each agent that built this repo |
| [`docs/SDK_README.md`](docs/SDK_README.md) | The original FtcRobotController README |

## Credits

- **Iron Lion #30311** — `ironlionrobotics/IronLion30311BIOBUZZInicioDeTemporada`, all branches:
  the season-start code this repo is built from (mechanisms, vision, TeleOp, the field-tested
  autonomous). The base is the `feature/turrubiates/vision-align` branch.
- **Iron Lion Pedro Pathing analysis** — `pedro-pathing-biobuzz-analysis-a65cc9`, a worktree of
  the same repository at `feature/autonomos-beta`: the Pedro routes.
- **[FIRST Tech Challenge FtcRobotController](https://github.com/FIRST-Tech-Challenge/FtcRobotController)**
  — the SDK (v12.0).
- **[Pedro Pathing](https://pedropathing.com)** — the path-following library, and the
  **[Pedro Pathing Quickstart](https://github.com/Pedro-Pathing/Quickstart)** (BSD-3-Clause-Clear)
  for the vendored AutoTune tuners in `pathing/tuning/`.
- **[Pedro Pathing Visualizer](https://github.com/Pedro-Pathing/Visualizer)** (Apache-2.0) — the
  BIOBUZZ field image.
- **[FTC Dashboard](https://github.com/acmerobotics/ftc-dashboard)** by ACME Robotics.
- **FTC 22526 Fondy Fire Tech**, `Biobuzz_PedroPathing_V3` (BSD-3-Clause-Clear) — the idea
  "if the HIVE has not changed by a deadline, go straight to park" (idea only, no code copied).

## License

The FTC SDK is licensed under the terms in [`LICENSE`](LICENSE) (FIRST, BSD-style). Files
vendored from the Pedro Pathing Quickstart keep their BSD-3-Clause-Clear license (noted in each
file's header), and the field image keeps the Visualizer's Apache-2.0 license (noted in
`pathing/FieldDrawing.java`).
