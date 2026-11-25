# MATCH CHECKLIST

Print this page for the pit. Tick every box. Strategy and the reasons behind each step are in
`docs/STRATEGY.md`; the controls are in `README.md`.

## 1. Before the event (at home, the week before)

**Code**
- [ ] `dev` merged into `main` by pull request, CI green (`build` and `code-rules`).
- [ ] The exact code on the robot is tagged on `main`, e.g. `v1.0-qual-1` (`docs/BRANCHING.md`).
- [ ] A second laptop can build and deploy (Android Studio installed, repo cloned, Gradle synced
      once with internet — the event Wi-Fi may be slow or missing).

**Driver Station configuration** — the names must match `config/RobotConstants.java` exactly
(case-sensitive). Open the active configuration on the Driver Station and check:
- [ ] Motors: `frontLeftDrive`, `frontRightDrive`, `backLeftDrive`, `backRightDrive`,
      `intakeMotor`, `indexerMotor`, `pollenShooterMotor`, `nectarShooterMotor`.
- [ ] Servo ports: `indicatorLight` (goBILDA RGB light). Optional: `nectarAlertLight`,
      `flowerArmLeftServo`, `flowerArmRightServo` (only if the flower arm is mounted).
- [ ] I²C: `pinpoint` (goBILDA Pinpoint), `huskyLens` (HuskyLens).
- [ ] USB: `limelight` (Limelight 3A).
- [ ] Run **TeleOp Red** INIT: the telemetry lists missing devices. There must be none (or only
      the optional ones you do not have).
- [ ] Take a photo of the configuration screen and keep it in the team drive (hub/port map is
      still unknown in the docs, ROBOT_PROFILE Q1).

**Firmware and software**
- [ ] Control Hub OS, Robot Controller app and Driver Station app updated with the REV Hardware
      Client to the versions required for FTC SDK 12.0. (The app version comes from this repo:
      deploying from Android Studio installs it.)
- [ ] Expansion Hub firmware updated (if used).
- [ ] Limelight: pipeline 0 is AprilTag 36h11 (`VisionConstants.LIMELIGHT_PIPELINE`).
- [ ] HuskyLens: COLOR_RECOGNITION trained with ID 1 = POLLEN (yellow), 2 = RED NECTAR,
      3 = BLUE NECTAR (`VisionConstants.HUSKY_*_ID`). Check with **Tune Color Sensor**.
- [ ] Driver Station: AUTO 30 s timer enabled (G305).

**Batteries**
- [ ] All batteries charged and labeled; keep a log of which battery ran which match.
- [ ] A battery under about 12.5 V at rest goes back on the charger. (Rule of thumb, not
      measured on this robot. The code warns below 11.0 V under load,
      `RobotConstants.BATTERY_LOW_VOLTS`.)
- [ ] Spare battery for the Driver Station phone/tablet, USB cables, and a spare gamepad.

**Robot checks on the practice field** (in this order)
- [ ] **Test Drive Wheels**: each wheel spins forward; A drives straight.
- [ ] **Test Odometry Driver**: the near legs end in the LOADING ZONE.
- [ ] **Tune Launcher PIDF**: 3000 RPM holds steady.
- [ ] **Auto Red Near** with delay 0: 4/4 preload, parks 2 in off the wall.

## 2. Before each match (in the queue)

- [ ] Fresh battery in; note its label.
- [ ] Talk to the alliance partner: who starts NEAR, which paths, which **start delay**
      (see `docs/STRATEGY.md` §2.3).
- [ ] Select the AUTO for **our alliance color** (`Auto Red Near`, `Auto Blue Near`, …) and
      press INIT.
- [ ] INIT telemetry: alliance, route and start are right; no missing devices; camera status OK.
- [ ] For `Auto Safe Park` / `Auto Safe Park Encoders`: gamepad1 **B = RED, X = BLUE**; for Safe
      Park also D-pad left/right = start (NEAR / FAR / beside the zone).
- [ ] Start delay set with gamepad1 D-pad up/down (shown on telemetry).
- [ ] Place the robot: touching the perimeter wall, fully on our half, **not in the LOADING
      ZONE**, not touching a FLOWER, inside 18 × 18 × 18 in, touching exactly 4 POLLEN (G304).
- [ ] **Start-pose check** on the telemetry reads **OK**. "will RE-ZERO" is acceptable (small
      offset). **FAULT** = move the robot to the start mark; if it stays FAULT, press gamepad1
      **A** to set the odometry back to the start pose with the robot on the mark.
- [ ] Do not touch the robot after INIT (G304.H: motionless).
- [ ] After AUTO the Driver Station preselects `TeleOp Red` / `TeleOp Blue`. For the two Safe
      Park autos, **select the TeleOp by hand** (no preselect).

## 3. During the match

- [ ] AUTO: hands off. Watch whether it parks.
- [ ] Transition (8 s): **do not press INIT/START before TELEOP starts** (G403).
- [ ] TELEOP INIT: check the alliance and that the AUTO pose was handed over.
- [ ] Driver 1: drive, collect (LB), drive away from opponent NECTAR when the intake stops.
- [ ] Driver 2: fire (RT) or auto-volley (A); RPM ±50 with the D-pad if shots miss.
- [ ] 1:05 three blips, 1:00 long rumble = ENDGAME. Flower arm (gamepad1 B) only now.
- [ ] Park partly in our LOADING ZONE before 0:00, then **press STOP** (G404).

## 4. After the match

- [ ] Write down: battery label, AUTO result (shots, parked?), TIPs, problems, fouls.
- [ ] Battery on the charger.
- [ ] Pull the logs from the Control Hub (USB or Wi-Fi with `adb connect 192.168.43.1:5555`):

      adb pull /sdcard/FIRST/match_logs  ./logs/
      adb pull /sdcard/FIRST/auto_logs   ./logs/

  Other folders written by the tuning OpModes: `launcher_logs`, `hive_logs`, `current_logs`.
  There is no log script in this repo yet; these two commands are enough.
- [ ] Look at `auto_logs/pose_*.csv`: did the robot follow the route? The "Planner at …" event
      rows show why it chose to cycle or park.

## 5. Quick pit fixes

| Symptom | First thing to check | Where in the code |
|---|---|---|
| Telemetry lists a missing device | DS configuration name (section 1), cable, port | `RobotConstants`, `RobotHardware` |
| AUTO does not drive, start check says FAULT | Robot on the start mark? Pinpoint cable/connector | `OdometryDriver.checkStartPose`, `DriveConstants` guard limits |
| AUTO stops moving mid-route | Pose jump (`ODOMETRY_LOST`) or territory guard tripped: check the pod wheels and Pinpoint cable | `OdometryDriver`, `TerritoryGuard` |
| Shots go long / short | Battery voltage first; then adjust RPM ±50 with gamepad2 D-pad; change `LauncherConstants.POLLEN_*_RPM` later from FTC Dashboard | `LauncherConstants` |
| Indexer stops and gamepad2 rumbles | Jam: release the fire trigger to clear it; check for a stuck ball | `IndexerConstants.JAM_*` |
| Intake will not collect, telemetry says FULL | The element count is wrong: gamepad1 **X** resets it | `ElementCounter` |
| Intake stops near a NECTAR | G408 stop for opponent NECTAR (correct). If it happens with our own NECTAR, check the HuskyLens IDs | `ColorSensing`, `VisionConstants.HUSKY_*_ID` |
| Robot drives the wrong way on one wheel | **Test Drive Wheels**; fix the direction in `DriveConstants` | `DriveConstants` |
| Low battery banner / rumble | Swap the battery | `RobotConstants.BATTERY_*` |
| Flower arm will not deploy | It is locked until 1:00 (G410); servos configured? | `FlowerArm`, `MatchTimer.canPlaceNectar` |

Change values from FTC Dashboard (http://192.168.43.1:8080/dash) to test them, then copy the
value into the `config/` class and commit — Dashboard changes are lost when the app restarts.
