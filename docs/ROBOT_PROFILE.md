# ROBOT PROFILE — FTC #30311 "Iron Lion" (BIOBUZZ 2026–27)

Phase 0 output of the robot discovery agent, written 2026-11-24.
Read-only study of every branch of the legacy repo and of the Pedro worktree copy.

## How to read this file

- **CONFIRMED**: clearly stated in code, config or a commit. Cited as `ref:path`
  (path is under `TeamCode/src/main/java/org/firstinspires/ftc/teamcode/` unless it says otherwise).
- **INFERRED**: my conclusion from indirect evidence. The reason is given.
- **UNKNOWN**: nothing in the sources. Each UNKNOWN or conflict has an entry in
  **QUESTIONS FOR THE TEAM** at the end.

Ref shorthand used below:

| Short | Git ref (tip commit, date) | Line of work |
|---|---|---|
| `VA` | `origin/feature/turrubiates/vision-align` (a7b8f4c, 2026-11-24) | Newest. Tested on the real robot 2026-11-19 → 11-24 |
| `MASTER` | `origin/master` (37b7a16, 2026-11-22) = merge of `feature/autonomos-beta` (206fa84) | Pedro autos checked in a path simulator only |
| `WT` | `legacy/pedro-pathing-biobuzz-analysis-a65cc9` | Same files as 206fa84, plus one uncommitted change: `@Disabled` on `ShootTurnCollectAuto` (see `docs/snapshots/pedro-worktree-uncommitted.diff`) |
| `NCAM` | `origin/feature/mar/duopodriveNcam` (2d92fba, 2026-11-21) | Mar's odometry + camera line (local copy c73454f, 11-19) |
| `DUO` | `origin/feature/mar/duopodrive` (38caa7f, 2026-11-22) | Two pods, not calibrated |
| `ONE` | `origin/feature/mar/onepodrive` (898e0da, 2026-11-11) | One pod only (Y pod broken at the time) |
| `DEV` | `origin/develop` (afbabf1, 2026-11-06) | Last common base of VA and MASTER |
| `FLOWER` | `origin/feature/renata/flower-arm` (889102d, 2026-11-15) | Renata: flower arm, stop servo, HuskyLens |
| `ALIGN` | `origin/feature/renata/auto-align` (3e733d4, 2026-11-07) | Renata: stopServo |
| `BALLS` | `origin/Balls-counting` (55f69ad, 2026-11-09) | Renata: HuskyLens ball counting + LED strip |
| `LUISAUTO` | `origin/feature/luis/autonomospaths` (9cf2c66, 10-22) and `origin/feature/theautonomous` (125df22, 11-14) | Luis: early speculative autos ("while the robot is still being built") |
| `NAMING` | `origin/feature/turrubiates/naming-conventions` (ea8f5a1) | `CONVENTIONS.md` + `doc/naming/rename-map.md` |

All 63 refs point to 41 distinct tip commits. I read all 41 trees (TeamCode + Markdown) and the
history of every device-name string. The SDK sample code was skipped.

---

## 1. The robot in plain English

Iron Lion's BIOBUZZ robot is an **18 in-class mecanum robot with rear flywheel shooters**.
(CONFIRMED in `VA:opmodes/auto/PreloadRouteAuto.java` "the 18 x 18 in robot"; the exact size is in conflict, see §11.)

- **Front = intake side. Rear = shooters + Limelight side.** CONFIRMED in `NAMING:CONVENTIONS.md`
  ("Robot orientation") and in `MASTER:Constants.java` ("Front = intake, rear = shooters").
- **Drivetrain:** 4 goBILDA Yellow Jacket 312 RPM (19.2:1, 537.7 ticks/rev) motors with 104 mm goBILDA
  GripForce mecanum wheels. Robot-centric. CONFIRMED in `MASTER:subsystems/MecanumDrive.java`
  and `NCAM:PROJECT_CONTEXT.md`.
- **Localization:** goBILDA Pinpoint with two goBILDA 4-bar pods plus its own IMU. The hub IMU
  is not used. CONFIRMED in `VA:subsystems/pathing/PedroFollowerFactory.java` and `NAMING:doc/naming/rename-map.md`.
- **Intake (front):** 1 motor that pulls balls in and pushes a row of stored balls back toward
  the indexer. CONFIRMED in `VA:RobotHardware.java` and `VA:opmodes/auto/PedroAutoBase.java` ("the intake only pushes the row while the indexer feeds").
- **Indexer:** 1 motor that feeds balls one at a time from the intake into the flywheels. It
  pauses while the flywheel is below target RPM. CONFIRMED in `VA:subsystems/Indexer.java`.
- **Shooter (rear):** 2 independent flywheels, each with 1 motor under closed-loop velocity control.
  `pollenShooterMotor` (legacy `P`) shoots POLLEN at ~2950–3000 RPM. `nectarShooterMotor` (legacy `N`)
  shoots NECTAR at 3600 RPM. CONFIRMED in `VA:subsystems/Shooter.java`.
- **Vision:** a Limelight 3A on the rear reads the HIVE AprilTags (IDs 30–45). A HuskyLens on the
  front sees POLLEN/NECTAR by color. CONFIRMED in `VA:RobotHardware.java` and `VA:subsystems/vision/*`.
- **Lights:** a goBILDA PWM RGB indicator light (`indicatorLight`) shows the speed mode and the
  shooter state. A second light (`nectarAlertLight`, optional) blinks when NECTAR is at the intake.
  CONFIRMED in `VA:subsystems/IndicatorLight.java` and `VA:subsystems/NectarAlertLight.java`.
- **No arm or lift on the current robot.** A "flower arm" (2 servos) was coded only on `FLOWER`. A
  lift and feeder servos were coded only on `LUISAUTO`. Neither reached the tested line. INFERRED
  from §9.
- **No shooter gate on the current robot.** The gate servo (`pera` / `shooterGateServo`) "was never
  mounted during the 2026-11-20 tests" and was removed. CONFIRMED by commit c1580df.

### Match cycle (current robot, `VA`)

1. **Start (AUTO):** the robot starts with its **front (intake) against the south (audience) wall**.
   Its rear shooters and Limelight face the HIVE. Start pose is (57, 9) in, heading −90° (corner on tile vertex W,0).
   It holds 4 preloaded POLLEN. CONFIRMED in `VA:opmodes/auto/PreloadRouteAuto.java`.
2. **Leave + launch:** it backs off 10 cm (LEAVE). It spins the pollen flywheel to 2950 RPM and feeds
   the 4 preloads one at a time. Each ball waits for the RPM to recover. Validated: "4/4 preload shots
   and parked in ~12-14 s". CONFIRMED in `VA:opmodes/auto/PreloadRouteAuto.java`.
3. **Collect → store:** (`Preload + Collect + Park` only) it turns 90° to face west and drives slowly
   (power 0.2) with the intake on over a row of 4 POLLEN. The balls are held between the intake and the
   indexer. The HuskyLens counts them by color. CONFIRMED in `VA:opmodes/auto/PreloadRouteAuto.java` and commit 62ac6f7.
4. **Launch again / TIP logic:** it goes back to the shooting spot and checks whether its scoring HIVE
   cell has tipped (its tags are gone for 0.6 s). If the cell is up, it shoots there. If the cell is down,
   it drives around the west side to the mirrored spot. It shoots at the other cell (audience cell, IDs
   34–37 for red) at 2950 RPM. CONFIRMED in commit c1c59db and in `VA:opmodes/auto/PreloadRouteAuto.java`.
5. **Park:** it drives into the LOADING ZONE (route 5 cm back, 60 cm right, 230 cm back, 60 cm right).
   It must be done by 30 s. CONFIRMED in `VA:opmodes/auto/PreloadRouteAuto.java`.
6. **TELEOP:** driver 1 drives and runs the intake. Driver 2 holds a trigger to spin a flywheel. The
   indexer feeds only when the flywheel is at target RPM. Auto-align to the HIVE exists but is
   **disabled** (`HiveAlignController.ENABLED = false`, "Off until the alignment is calibrated").
   CONFIRMED in `VA:opmodes/teleop/MainTeleOp.java` and `VA:subsystems/vision/HiveAlignController.java`.
7. **"Place":** the current code has **no** mechanism for placing balls in a FLOWER or GARDEN. The NECTAR
   alert says "this competition the robot only scores POLLEN". CONFIRMED in `VA:subsystems/NectarAlertLight.java`.
   The flower arm (`FLOWER`) was the only attempt at placing. See Q14.
8. **Endgame park:** the code has no special endgame routine. The driver parks by hand. INFERRED (no code found).

---

## 2. Drivetrain

| Fact | Value | Status | Source |
|---|---|---|---|
| Type | 4-wheel mecanum, robot-centric (no field-centric anywhere) | CONFIRMED | `VA:subsystems/MecanumDrive.java` "Robot-centric mecanum drivetrain"; no field-centric code in any of the 41 trees |
| Motors | goBILDA 5203 Yellow Jacket 19.2:1, 312 RPM, 537.7 ticks/rev | CONFIRMED (docs + constant) | `MASTER:subsystems/MecanumDrive.java` `TICKS_PER_REV = 537.7`; `NCAM:PROJECT_CONTEXT.md` |
| Conflicting motor claim | 435 RPM (13.7:1, 384.5 ticks/rev) and 96 mm wheels | CONFLICT (old, speculative) | `LUISAUTO:subsystems/autosubsystems/AutoHardware.java`, `AutoDrive.java` (`Math.PI * 96.0`). See Q3 |
| Wheels | goBILDA GripForce mecanum, Ø104 mm (circumference 32.673 cm → 16.457 ticks/cm) | CONFIRMED | `MASTER:subsystems/MecanumDrive.java` `WHEEL_DIAMETER_CM = 10.4`; `NCAM:PROJECT_CONTEXT.md` |
| Measured ticks/cm | Starting estimate only: forward 16.5, strafe 18.5. "TODO: calibrate on the field" | CONFIRMED (estimate) | `VA:opmodes/auto/SafeMoveAuto.java` |
| Config names (current) | `frontLeftDrive`, `frontRightDrive`, `backLeftDrive`, `backRightDrive` (legacy fallback `FL`/`FR`/`BL`/`BR`) | CONFIRMED | `VA:RobotHardware.java` |
| Directions | Left side REVERSE (FL, BL), right side FORWARD (FR, BR) | CONFIRMED, the same in every branch | `VA:RobotHardware.java`, `VA:subsystems/pathing/PedroFollowerFactory.java`, `MASTER:RobotHardware.java` |
| Zero power | **BRAKE** on `VA` (commit 64dfe81, "so the robot stops when the sticks are released") and in Pedro (`manualBrakeMode = true`) | CONFIRMED | `VA:RobotHardware.java` |
| Zero power (conflict) | FLOAT on `MASTER`/`DEV`; BRAKE on `NCAM` | CONFLICT | `MASTER:RobotHardware.java` `configureDriveMotor` |
| Run mode | `VA`: RUN_WITHOUT_ENCODER (open loop, encoders still read). `MASTER`/`NCAM`: RUN_USING_ENCODER | CONFIRMED (differs) | `VA:RobotHardware.java`; `MASTER:RobotHardware.java` |
| Encoders | Wired on all 4 drive motors | CONFIRMED | `DEV:PROJECT_CONTEXT.md` "4 goBILDA motors with encoders connected"; `VA:opmodes/auto/SafeMoveAuto.java` uses them |
| Speed modes | FAST 1.0 (default), SLOW 0.4 | CONFIRMED | `VA:subsystems/MecanumDrive.java` |
| Ramps | accel 0.3 s, decel 0.2 s, power below 0.25 cut to 0 on stop, align ramp 0.1 s | CONFIRMED (tuned on robot: "0.1 … risk tipping over; 0.25 … slid") | `VA:subsystems/MecanumDrive.java` |
| Stick shaping | deadzone 0.05, cubic x³ | CONFIRMED | `DEV:PROJECT_CONTEXT.md`; `MainTeleOp.shapeStickInput` |
| Max speed | UNKNOWN. Pedro placeholders: 60 in/s forward, 45 in/s strafe, `maxPathSpeed = 0.6` "Safety cap while untuned" | UNKNOWN (placeholders) | `VA:subsystems/pathing/PedroFollowerFactory.java`. See Q6 |
| Battery protection | below 8.5 V the drive is capped at 0.5 for 0.5 s; low warning at 11.0 V for 3 s | CONFIRMED | `VA:subsystems/BatteryMonitor.java` |

## 3. Localization

| Fact | Value | Status | Source |
|---|---|---|---|
| Device | goBILDA Pinpoint Odometry Computer, config name `pinpoint` | CONFIRMED | `VA:subsystems/pathing/PedroFollowerFactory.java` `c.name.set("pinpoint")` |
| Pods | **2** goBILDA 4-Bar pods (Ø32 mm, 2000 CPR → 198.94 ticks/cm), X = forward pod, Y = strafe pod | CONFIRMED | `VA:subsystems/pathing/PedroFollowerFactory.java`; `NCAM:subsystems/PinpointOdometry.java` |
| History of pod count | `ONE` (11-11): 1 pod, "one pod broken → left out" (`DEV:PROJECT_CONTEXT.md`); `DUO`/`NCAM`/`VA`: 2 pods | CONFIRMED | commits 898e0da, 020d326 |
| Heading source | Pinpoint internal IMU. Expansion Hub IMU (`imu 1` on the DS) is "configured but not used by the code" | CONFIRMED | `NAMING:doc/naming/rename-map.md` §1; `VA:CONVENTIONS.md` |
| Hub IMU orientation | Only in `LUISAUTO` (logo UP, USB FORWARD, "Adjust … to how the hub is physically mounted"). Never used on the tested line | UNKNOWN | `LUISAUTO:subsystems/autosubsystems/AutoHardware.java`. See Q5 |
| X pod direction | FORWARD (all branches) | CONFIRMED | `VA:subsystems/pathing/PedroFollowerFactory.java` |
| Y pod direction | **REVERSED** in Pedro on `VA` ("with FORWARD the robot strafed right while the Pinpoint read left", test 2026-11-23) | CONFIRMED (tested) | `VA:subsystems/pathing/PedroFollowerFactory.java`, commit 927e4df |
| Y pod direction (other) | FORWARD in `MASTER`/`NCAM` `PinpointOdometry` (which negates Y in software for "strafe right") | CONFLICT (may be the same physical fact under different sign conventions) | `NCAM:subsystems/PinpointOdometry.java`. See Q4 |
| Pod offsets (`VA`, Pedro) | `xPodOffset = -6.79 in`, `yPodOffset = -0.72 in`. **Fitted** from an in-place 92° turn, not measured; "TODO(team): confirm with a tape measure (X pod ~17 cm right of the center)" | CONFIRMED (fitted) | `VA:subsystems/pathing/PedroFollowerFactory.java`, commit 12377ee |
| Pod offsets (`NCAM`, measured by Mar) | X pod at (+170 mm fwd, 65 mm left) → offset +65 mm; Y pod at (−170 mm back, 65 mm left) → offset −170 mm; Pinpoint at (+82 mm fwd, +40 mm right) | CONFLICT with `VA` | `NCAM:subsystems/PinpointOdometry.java`. See Q4 |
| Pod offsets (`MASTER`, `DUO`) | 0 / 0 (TODO) | CONFIRMED placeholder | `MASTER:subsystems/pathing/PedroFollowerFactory.java` |
| Pod offsets (`LUISAUTO`) | X −84 mm, Y −168 mm | Speculative | `LUISAUTO:subsystems/autosubsystems/AutoPod.java` |
| Distance scale | `NCAM`: measured 101 cm reported for 100 cm → scale 1.01 (200.933 ticks/cm). Not carried to Pedro on `VA` | CONFIRMED on `NCAM` only | `NCAM:subsystems/PinpointOdometry.java` |
| Pedro version | Pedro Pathing 3: `com.pedropathing:revhub:3.0.2`, `tuning:1.0.3`, Foresight follower | CONFIRMED | `VA:build.dependencies.gradle` (repo root) |
| Pedro follower constants | **All placeholders, never tuned.** forward/strafe translational P 0.03 (→0.1 above 2.5 in), coast/brake PF 0.01, heading P 1.0, heading brake (0.1, 0.01), linear brake 0.05, quadratic 0.001, max fwd 60 / strafe 45 in/s, natural decel 40, `maxPathSpeed 0.6` | CONFIRMED (placeholders) | `VA:` and `MASTER:subsystems/pathing/PedroFollowerFactory.java` "MUST be replaced with the Java code that AutoTune … prints" |
| Mass, zero-power accel | UNKNOWN. Pedro 3 Foresight has no mass field here. No tuner output is committed | UNKNOWN | See Q6 |
| How `VA` autos actually drive | Its own point-to-point `driveTo()` on the Pinpoint pose (max 0.85, min 0.25, slow zone 18 in, tolerance 0.6 in, heading kP 1.0) through `follower.manual()`, "instead of the untuned Pedro follower, which was slow" | CONFIRMED | `VA:opmodes/auto/PedroAutoBase.java`, commit 927e4df |
| Field frame | Pedro: inches, origin at the red/audience corner | CONFIRMED | `VA:opmodes/auto/PreloadRouteAuto.java` |
| Field frame (other) | `NCAM` map: origin at field center, cm, 0° toward the blue wall; `NCAM` `PinpointOdometry` heading: 0° = north, clockwise positive | CONFLICT (different conventions) | `NCAM:mapa-cancha-odometria-biobuzz.md`, `NCAM:PROJECT_CONTEXT.md` |
| Known odometry faults | Two runs where the Pinpoint read 5 m sideways before moving or reset to (0,0,0): "points to a bad pod or Pinpoint connection". Now: refuse to drive if the start pose is off by more than 2 in / 5°; re-zero if off by up to 12 in / 10°; freeze the drive if the pose jumps more than 12 in / 30° | CONFIRMED | commits 3fbcbb8, 03cb6df, 61dd9ef |

## 4. Mechanisms

### 4.1 Intake (front)
| Fact | Value | Status | Source |
|---|---|---|---|
| Hardware | 1 `DcMotorEx`, `intakeMotor` (legacy `intake`) | CONFIRMED | `VA:RobotHardware.java` |
| Motor model | UNKNOWN (`LUISAUTO` comment guesses "e.g. 312 RPM") | UNKNOWN | `origin/csv:subsystems/MotorDiagnosticsLogger.java` `TPR_INTAKE = 537.7 // Intake motor (e.g. 312 RPM)`. See Q7 |
| Direction | **REVERSE** on `VA` ("on the robot FORWARD spun outward … the autonomous expelled the preloaded balls") | CONFIRMED (tested 11-23) | `VA:RobotHardware.java`, commit 927e4df |
| Direction (other) | FORWARD on `MASTER`, `NCAM`, `DEV` | CONFLICT → `VA` is the tested value | `MASTER:RobotHardware.java` |
| Mode / zero power | RUN_WITHOUT_ENCODER, FLOAT | CONFIRMED | `VA:RobotHardware.java` |
| Power | 0.75, ramp 0.15 s | CONFIRMED | `VA:subsystems/Intake.java` |
| Power (other) | 1.0 on `feature/luis/{naming,probe,alliance,park}` (commit "run intake at 100% power", 11-22) | CONFLICT | `origin/feature/luis/naming:subsystems/Intake.java`. See Q8 |
| Behavior | IDLE / INTAKING (+) / EJECTING (−). Collect wins if both are pressed. In auto it pushes the stored row only while the indexer feeds. It "shakes" (reverses 0.35 s, up to 3 times) if a ball does not reach the flywheel within 0.8 s | CONFIRMED | `VA:subsystems/Intake.java`, `VA:opmodes/auto/PedroAutoBase.java`, commit 7004eba |

### 4.2 Indexer
| Fact | Value | Status | Source |
|---|---|---|---|
| Hardware | 1 `DcMotorEx`, `indexerMotor` (legacy `alejandro`; `MASTER` uses `indexer`) | CONFIRMED | `VA:RobotHardware.java`; `MASTER:RobotHardware.java` |
| Name origin | "Confirmed with the team (2026-11-15): the `alejandro` motor is the indexer that feeds the shooter" | CONFIRMED | `NAMING:doc/naming/rename-map.md` |
| Direction / mode | FORWARD, FLOAT, RUN_WITHOUT_ENCODER | CONFIRMED | `VA:RobotHardware.java` |
| Power | 0.75, ramp 0.15 s; in TeleOp runs forward only | CONFIRMED | `VA:subsystems/Indexer.java` |
| Activation | `VA`/`NCAM`/`DEV`: with either shooter. `MASTER` TeleOp: with the **nectar** shooter only (`indexer.update(fireNectar, false)`) | CONFLICT | `MASTER:opmodes/teleop/MainTeleOp.java`. See Q9 |
| RPM gating | While firing it pauses until the flywheel is ready: ready within 100 RPM of target, not ready again past 200 RPM off. "It meters one ball per shot … it replaced the gate servo" | CONFIRMED (tested: "29 of 46 scored") | `VA:subsystems/Indexer.java`, `VA:subsystems/Shooter.java`, commit c1580df |
| Jam protection | more than 5 A for 0.5 s = jam → reverse 0.3 s (+0.2 s per repeated jam). Stops after 3 jams in a row until the trigger is released, and rumbles gamepad2. Normal draw is ~1–2 A. A logged stall was 14 s at ~7.5 A | CONFIRMED (from logs) | `VA:subsystems/Indexer.java`, commit 9c03b6c |

### 4.3 Shooter / launcher (rear)
| Fact | Value | Status | Source |
|---|---|---|---|
| Hardware | 2 `DcMotorEx`: `pollenShooterMotor` (legacy `P`, field `shooterP`) and `nectarShooterMotor` (legacy `N`, field `shooterN`). One motor per flywheel | CONFIRMED | `VA:RobotHardware.java` |
| Older names | `shooterPollen`/`shooterNectar` (d7c10b2, 10-17), `shooterP`/`shooterN` (86054f0 "shorter names"; still on `ALIGN`/`localcopy/feature/mar/shoters`), then `P`/`N` | CONFIRMED (history) | git history |
| Speculative variant | `pollen`+`pollen2`, `nectar`+`nectar2` (2 motors per flywheel) + `pollenFeeder`/`nectarFeeder` servos | Speculative, never on the tested line | `LUISAUTO:subsystems/autosubsystems/AutoHardware.java` |
| Direction / mode | REVERSE, FLOAT, RUN_USING_ENCODER, velocity control (`setVelocity`) | CONFIRMED | `VA:RobotHardware.java` |
| Encoder | `TICKS_PER_REV = 28` (bare-motor encoder, "direct-drive flywheels with no gearbox"), "TODO: calibrate against the actual goBILDA motor model" | CONFIRMED value, motor model UNKNOWN | `VA:subsystems/Shooter.java`. See Q7 |
| Flywheel motor | INFERRED to be a goBILDA 1:1 (6000 RPM) motor, from 28 ticks/rev and `MAX_RPM = 6000`. The nectar wheel tops out ~4000 RPM at ~11 V | INFERRED | `VA:subsystems/Shooter.java` |
| RPM targets (`VA`, tested) | Pollen TeleOp 3000 ("at 2900 … 11 of 25 scored, every ball left at ~2880"); pollen auto 2950 ("both shots went long at 3000"); other-cell pollen 2950 ("2800 left 2 of 3 shots short"); nectar 3600 ("4500 was unreachable") | CONFIRMED (field-tested) | `VA:subsystems/Shooter.java`, `VA:opmodes/auto/PreloadRouteAuto.java` |
| RPM targets (other) | `MASTER`: pollen 2900, nectar 0.75 × 6000 = 4500. `NCAM`: 3000 / 3200. `LUISAUTO`: 3200 / 2600 (custom power PID) | CONFLICT (older or untested) | `MASTER:subsystems/Shooter.java`, `NCAM:subsystems/Shooter.java` |
| Velocity PIDF (`VA`) | P 30, I 0, D 0, F 15.0 at 12 V, rescaled by battery voltage every 0.25 s. "I wound up on every shot dip" (I 0.5 → 3470 RPM overshoot) | CONFIRMED (tuned from logs) | `VA:subsystems/Shooter.java` |
| Velocity PIDF (`MASTER`) | (10, 0.5, 0, 12) fixed | CONFLICT (older) | `MASTER:RobotHardware.java` |
| Tuning | D-pad tap = ±50 RPM, clamp 0–6000 | CONFIRMED | `VA:subsystems/Shooter.java` |
| Measured shooting spot | About 1.3 m from the camera to the tags (floor distance 1.03 m, strafe +0.08 m). The 3000 RPM pollen test "scored 29 balls" | CONFIRMED (field test 11-20) | `VA:subsystems/vision/HiveAlignController.java` |
| Shooter-to-flywheel path | One indexer feeds both flywheels. Which ball goes to which flywheel (sorting) is not in code | UNKNOWN | See Q9 |

### 4.4 Gates / stoppers (all removed or never mounted)
| Device | Behavior | Status | Source |
|---|---|---|---|
| `pera` → `shooterGateServo` | Angular stopper by the shooters. `NCAM`: open 10° / closed −35° on a 280° servo. **Bug:** −35° clips to position 0.0. Opens while either shooter fires; gamepad2 X toggles it. `VA` (01b624e): opens 50° on a 270° servo. **Removed 2026-11-20**: "never mounted during the 2026-11-20 tests" | CONFIRMED removed | `NCAM:subsystems/Pera.java`, commit c1580df |
| `stopServo` | Renata's stopper: rest 0.0, engaged 0.5. Gamepad2 X toggles it | Branch-only (`ALIGN`, `FLOWER`) | `ALIGN:subsystems/StopServo.java` |
| Same servo? | "Still to confirm with the team: whether it is the same physical servo as `stopServo`" | UNKNOWN | `NAMING:doc/naming/rename-map.md`. See Q10 |

### 4.5 Flower arm (Renata, 2026-11-15)
| Fact | Value | Status | Source |
|---|---|---|---|
| Hardware | 2 servos `servoLeft` (FORWARD) and `servoRight` (REVERSE), "looking to each other face to face" | Branch-only | `FLOWER:RobotHardware.java`, commit 889102d |
| Behavior | Toggles 0.0 (rest) ↔ 0.5 (deployed, "90° on a 180° servo") on a gamepad1 right-bumper press | Branch-only | `FLOWER:subsystems/FlowerArm.java` |
| Code state | **Does not compile**: the constructor is named `DualServo` inside class `FlowerArm`. In the same TeleOp, gamepad1 left trigger is both brake and intake collect | CONFIRMED bug | `FLOWER:subsystems/FlowerArm.java`, `FLOWER:opmodes/teleop/MainTeleOp.java` |
| Is it on the robot? | UNKNOWN | UNKNOWN | See Q14 |

### 4.6 Lift (speculative)
`lift` motor (RUN_TO_POSITION, BRAKE, up 900 ticks) exists only in `LUISAUTO:subsystems/autosubsystems/AutoLift.java`.
The code itself says the devices are mapped as optional "while the robot is still being built". INFERRED: it is not a real mechanism.

### 4.7 Lights
| Device | Behavior | Status | Source |
|---|---|---|---|
| `indicatorLight` (legacy `indicator`) | goBILDA PWM RGB light SKU 3118-0808-0002 on a servo port. Positions: off 0.0, red 0.33 (fast mode), yellow 0.42 (pollen; "TODO fine-tune"), blue 0.63 (slow mode), white 1.0 (aligning). While firing: white = aligning, blinking yellow = spinning up, solid yellow = feeding. In auto INIT it shows the alliance | CONFIRMED | `VA:subsystems/IndicatorLight.java` |
| Green "aligned" | Used on `MASTER`/`NCAM` | Branch-only | `MASTER:opmodes/teleop/MainTeleOp.java` |
| `nectarAlertLight` | Second goBILDA PWM light (optional). Blinks red/blue (0.25 s period, held 0.6 s) when the HuskyLens sees NECTAR | CONFIRMED in code; mounted? UNKNOWN | `VA:subsystems/NectarAlertLight.java`. See Q12 |
| goBILDA Prism 6-LED strip | Shows the ball count (4 slots). Not mapped in `RobotHardware` | Branch-only, incomplete | `BALLS:subsystems/BallsCounting.java` |

## 5. Sensors and cameras

| Sensor | Details | Status | Source |
|---|---|---|---|
| Limelight 3A `limelight` | Rear (shooter side). Pipeline 0 = AprilTag 36h11, polled at 100 Hz, EMA α 0.45, frames older than 200 ms ignored, 0.15 s dropout hold. Uses "Full 3D" with a 2D fallback. Optional (`tryGet`) | CONFIRMED | `VA:subsystems/vision/HiveVision.java`, `HiveGeometry.java` |
| Camera pitch | 40° up, "TODO(team): measure on the robot" | Guessed | `VA:subsystems/vision/HiveGeometry.java` |
| Camera height | `NCAM`: tag height above the camera 0.523 m. Not used on `VA` | Branch-only | `NCAM:subsystems/vision/HiveGeometry.java` |
| HIVE tags | 4 tags per cell, 3.25 in: RED_SCORING 30–33, RED_AUDIENCE 34–37, BLUE_AUDIENCE 38–41, BLUE_SCORING 42–45. Tag spacing fallback 0.1143 m (`VA`) vs 0.0825 m (`NCAM`, = the tag size) | CONFIRMED (spacing conflict) | `VA:subsystems/vision/HiveCell.java`; `NCAM:subsystems/vision/HiveGeometry.java` |
| TIP detection | Cell counts as tipped when its tags are not seen for 0.6 s from the shooting spot (robot within 10 cm / 8°). Early watch up to 4 s after the volley (gone for 0.8 s) | CONFIRMED (field-checked) | `VA:opmodes/auto/PreloadRouteAuto.java`, commits c1c59db, a7b8f4c |
| HuskyLens `huskyLens` | Front (intake side), I2C, COLOR_RECOGNITION, 320×240. Optional | CONFIRMED | `VA:subsystems/vision/BallVision.java` |
| HuskyLens learned IDs | `VA`: 1 = POLLEN yellow, **2 = RED NECTAR, 3 = BLUE NECTAR**. `BALLS`: 1 = pollen, **2 = BLUE, 3 = RED** | CONFLICT | `VA:subsystems/vision/BallVision.java` vs `BALLS:subsystems/BallsCounting.java`. See Q11 |
| HuskyLens thresholds | min block 150 px², read every 0.05 s, ball-assist turn kP 0.35 (max 0.3), pickup counter thresholds in `BallIntakeCounter` | CONFIRMED | `VA:subsystems/vision/BallVision.java`, `BallIntakeCounter.java` |
| Battery voltage | first `voltageSensor` (Control Hub) | CONFIRMED | `VA:RobotHardware.java` |
| Motor current | indexer current for jams. All 8 motors logged to CSV | CONFIRMED | `VA:subsystems/MotorDiagnosticsLogger.java` |
| Color / distance / touch sensors | **None** in any branch | CONFIRMED (absence) | grep of all 41 trees |
| Webcam | `teamwebcamcalibrations.xml` is the empty SDK template. No webcam is used | CONFIRMED | `MASTER:TeamCode/src/main/res/xml/teamwebcamcalibrations.xml` |

## 6. Hardware configuration table

The DS "type" is the SDK class used in code. No branch records hub or port numbers. They are UNKNOWN everywhere (Q1).

| Config name | Device type | Mechanism | Hub/port | Direction | Notes | Branches where seen |
|---|---|---|---|---|---|---|
| `frontLeftDrive` | DcMotorEx | drive FL | UNKNOWN | REVERSE | BRAKE, RUN_WITHOUT_ENCODER; also used by Pedro | VA (since a2854e4/3ac6a93) |
| `frontRightDrive` | DcMotorEx | drive FR | UNKNOWN | FORWARD | | VA |
| `backLeftDrive` | DcMotorEx | drive BL | UNKNOWN | REVERSE | | VA |
| `backRightDrive` | DcMotorEx | drive BR | UNKNOWN | FORWARD | | VA |
| `FL` / `FR` / `BL` / `BR` | DcMotorEx | drive (legacy) | UNKNOWN | L REVERSE / R FORWARD | VA keeps them as fallback | every branch since 10-13 |
| `intakeMotor` | DcMotorEx | intake | UNKNOWN | REVERSE (VA) | FLOAT, 0.75 power | VA |
| `intake` | DcMotorEx | intake (legacy) | UNKNOWN | FORWARD (MASTER/NCAM/DEV) | | all since 10-16 (8f831dd) |
| `indexerMotor` | DcMotorEx | indexer | UNKNOWN | FORWARD | | VA |
| `alejandro` | DcMotorEx | indexer (legacy) | UNKNOWN | FORWARD | the team's nickname for the indexer | DEV, SIT, NCAM, DUO, ONE, VA (fallback), luis/*, turrubiates/*, renata/auto-test, Balls-counting, PID, auto-shooter |
| `indexer` | DcMotorEx | indexer (MASTER name) | UNKNOWN | FORWARD | MASTER: nectar shooter only | MASTER, autonomos-beta, FLOWER, renata/removing-Alejandro |
| `pollenShooterMotor` | DcMotorEx | pollen flywheel | UNKNOWN | REVERSE | RUN_USING_ENCODER, 28 ticks/rev | VA |
| `nectarShooterMotor` | DcMotorEx | nectar flywheel | UNKNOWN | REVERSE | | VA |
| `P` / `N` | DcMotorEx | flywheels (legacy) | UNKNOWN | REVERSE | | all since 10-24 except ALIGN, `localcopy/feature/mar/shoters` |
| `shooterP` / `shooterN` | DcMotorEx | flywheels (older) | UNKNOWN | — | | ALIGN, localcopy/feature/mar/shoters (d3aa850) |
| `shooterPollen` / `shooterNectar` | DcMotorEx | flywheels (oldest) | UNKNOWN | — | | history only (d7c10b2, 10-17) |
| `indicatorLight` | Servo (PWM) | RGB light | UNKNOWN (servo port) | — | goBILDA 3118-0808-0002 | VA |
| `indicator` | Servo (PWM) | RGB light (legacy) | UNKNOWN (servo port) | — | | every branch since 10-13 |
| `nectarAlertLight` | Servo (PWM) | NECTAR alert light | UNKNOWN | — | optional (`tryGet`) | VA (3f1795b, 11-24) |
| `limelight` | Limelight3A | HIVE AprilTags | USB (INFERRED, Limelight 3A is USB) | — | rear; optional on VA, required on MASTER | VA, MASTER, NCAM, luis/*, turrubiates/*, auto-shooter |
| `huskyLens` | HuskyLens | ball color | I2C bus UNKNOWN | — | front; optional on VA | VA, FLOWER, Balls-counting, renata/auto-test, renata/removing-Alejandro |
| `pinpoint` | GoBildaPinpointDriver | odometry + IMU | I2C bus UNKNOWN | X FORWARD, Y REVERSED (VA Pedro) | offsets in §3 | VA, MASTER, NCAM, DUO, ONE, LUISAUTO, luis/*, turrubiates/* |
| `imu 1` | REV internal IMU (BNO055) | Expansion Hub IMU | Expansion Hub | — | on the DS, never read by code | NAMING doc only |
| `imu` | IMU | hub IMU | UNKNOWN | logo UP / USB FORWARD (guess) | optional | LUISAUTO only |
| `pera` / `shooterGateServo` | Servo | shooter gate | UNKNOWN | — | removed 11-20, never mounted | NCAM (pera); VA history 01b624e–c1580df |
| `stopServo` | Servo | stopper | UNKNOWN | — | 0.0 / 0.5 | ALIGN, FLOWER |
| `servoLeft` / `servoRight` | Servo | flower arm | UNKNOWN | FORWARD / REVERSE | 0.0 / 0.5 | FLOWER |
| `pollen`, `pollen2`, `nectar`, `nectar2` | DcMotorEx | flywheels (speculative) | — | F / R | own power PID | LUISAUTO |
| `pollenFeeder`, `nectarFeeder` | Servo | feeders (speculative) | — | — | | LUISAUTO |
| `lift` | DcMotorEx | lift (speculative) | — | FORWARD | RUN_TO_POSITION | LUISAUTO |
| (voltage sensor) | VoltageSensor | battery | Control Hub | — | `voltageSensor.iterator().next()` | all since 10-24 |

**Which names are on the robot's DS today?** INFERRED: the **new** names (`frontLeftDrive` …, `pinpoint`) are at least on the
hub used for the `VA` tests. The Pedro follower looks up only `frontLeftDrive`/…/`pinpoint` ("Pedro does not know the legacy ones").
`Preload + Park (Odometry)` ran through that follower and was "validated: 4/4 preload shots and parked". The `MASTER` code
(`FL`…, `indexer`) would not find `frontLeftDrive` and would crash if the DS had only the new names. `VA` mentions "some Control Hubs
still use the old configuration". See Q2.

## 7. Controls

### Current: `VA:opmodes/teleop/MainTeleOp.java` (CONFIRMED)
| Input | Action |
|---|---|
| gp1 X / B (INIT only) | alliance blue / red (for auto-align to "our" upward cell) |
| gp1 left stick | forward/back + strafe (cubic) |
| gp1 right stick X | turn |
| gp1 left trigger (hold) | brake (rumbles if sticks move) |
| gp1 A / Y | slow / fast mode |
| gp1 left bumper (hold) | intake collect, plus HuskyLens ball-assist turn when not turning |
| gp1 right bumper (hold) | intake eject |
| gp2 right trigger (hold) | fire pollen flywheel (indexer feeds when ready) |
| gp2 right bumper (hold) | reverse pollen flywheel |
| gp2 left trigger (hold) | fire nectar flywheel |
| gp2 left bumper (hold) | reverse nectar flywheel |
| gp2 D-pad up / down | pollen RPM ±50 |
| gp2 D-pad right / left | nectar RPM ±50 |
| gp2 X / Y / B | mark last shot SHORT / LONG / WIDE in the log |
| (auto) | auto-align while firing if `HiveAlignController.ENABLED` (default **false**) |

### Differences in other branches (CONFIRMED in each file)
| Branch | Differences |
|---|---|
| `MASTER` / `WT` / `auto-shooter` | **Right bumper collect, left bumper eject** (with intake FORWARD — physically the same as VA, see Q8); gp2 left bumper = hold auto-align (white/green light); indexer only with nectar; no nectar reverse |
| `DEV`, `DUO`, `ONE`, `luis/*` | Same as MASTER intake mapping; gp2 LB = reverse nectar; indexer with either shooter |
| `NCAM` | gp1 **right bumper hold = slow mode**; gp1 **left bumper or gp2 A = auto-aim** (rotation only, vision + odometry); gp1 **right trigger collect / left trigger eject**; gp2 X toggles `pera` |
| `FLOWER` | gp1 left trigger = brake **and** intake collect (conflict bug); gp1 left bumper eject; gp1 right bumper toggles flower arm; gp2 X toggles stopServo |

## 8. Strategy clues (autonomous)

| OpMode (ref) | What it does | Tested? |
|---|---|---|
| `Preload + Park (Odometry)` (`VA`) | Start (57, 9, −90°) front to the south wall. Back 10 cm (LEAVE), shoot 4 POLLEN at 2950 RPM (max 10 s), then 5 cm back, 60 cm right, 230 cm back, 60 cm right into the loading zone | **Yes**: "validated: 4/4 preload shots and parked in ~12-14 s" |
| `Preload + Collect + Park (Odometry)` (`VA`) | Same preload, watches for an early TIP (≤4 s), turns 90° CW, collects 4 POLLEN along the west row (60→120 cm, power 0.2), then shoots at its scoring cell or goes around to the other cell. Park by ~26–30 s. Time guards: skip collecting after 11 s, skip the other cell after 15 s, last volley by 22 s / 26.5 s | Partially (runs on 11-23/24 logged; tuning still "TODO(team)") |
| `Safe Move (Encoders)` (`VA`) | Back 40 cm, left 40 cm on wheel encoders only, a fallback if the Pinpoint/Pedro fail | Uncalibrated estimate |
| `Leave + Park` (`turrubiates/auto-leave-park`, `luis/park`) | "for MXMOQ" (an event, INFERRED). Starts in the LOADING ZONE, rear against the wall, drives 10 cm for LEAVE + PARK, with a start delay | Unknown |
| `Tick`, `Center Lane`, `Shoot Turn Collect`, `Park`, `Outer Lane` (archived) (`MASTER`) | Pedro paths: shoot one tile off the wall, sweep the corner, cross **under** the central structure ("TODO: confirm the robot (16.54 in tall) clears it"), shoot again, park. Pollen 2900 / nectar 4500 RPM, 2 s feed | **No**: "Fix wall collisions found in path simulation" (206fa84). `WT` disables `Shoot Turn Collect` |
| `Start Routine`, `Center Lane`, `Outer Lane`, `Top Wall` (`VA`) | Ports of the autonomos-beta Pedro paths with Limelight aim + HuskyLens pickup (a2854e4) | No evidence of tests |
| `THE AUTONOMOUS` + 16 `AUTO ROJO/AZUL …` (`LUISAUTO`) | Early route library ("Precargas y Park", "Doble Ciclo", "Bajo Balanzas"…) for hardware that did not exist | No (speculative) |
| `Red Corner -> Closest Cell Test`, `Top Wall`, `Camera & Align Test` (`NCAM`) | Mar's odometry + camera tests | "cam and odometry calibration (worked as intended) (missing odometry deg calibration)" |

Key strategy facts:
- **POLLEN-only scoring this competition.** CONFIRMED in `VA:subsystems/NectarAlertLight.java`.
- **The same path for both alliances.** "In BIOBUZZ the same path works for either alliance, so AlliancePoses no longer mirrors". CONFIRMED by commit d06ea2d. `MASTER` still mirrors left/right and calls the left half "blue". The game notes say columns A–C (left) are **red**. CONFLICT, see Q13.
- Robot capacity: 4 balls (4 preloads, a 4-ball collect row, `BALLS` `MAX_BALLS = 4`). INFERRED.

## 9. Evolution and known issues (from commits and diffs)

| Date | Event | Source |
|---|---|---|
| 10-11 → 10-16 | Base SDK, drive, indicator light (Renata), intake integration (Mar) | 0cf9a7c…8f831dd |
| 10-17 → 10-24 | Shooters `shooterPollen/Nectar` → `shooterP/N` → `P/N`; Renata `using_encoders` | d7c10b2, 86054f0, 1d2c839 |
| 10-18 → 10-22 | Luis's speculative autos (lift, feeders, 435 RPM, 96 mm) | 468c4ee, 9cf2c66 |
| 10-28 → 10-31 | CSV motor logger | 2eb4c59, 6f297f1 |
| 11-06 | "first physical test, everything worked" (Mar, improveChassis) → `develop` | bc00121, afbabf1 |
| 11-07 → 11-09 | Renata: stopServo, HuskyLens ball counting, PID controller | f2e91fe, 74a8489, 105a3d5 |
| 11-07 → 11-11 | Mar: odometry "haven't tested it physically"; one pod; two pods "hasn't been calibrated" | f53eb1a, 898e0da, 020d326 |
| 11-14 | Vision (Limelight HIVE) merged; Luis "THE AUTONOMOUS"; Mar "everything works as it should" | 654445d, f5ab023, 182e03d |
| 11-15 | Naming conventions + rename map (source of truth = NCAM @182e03d "the code running on the robot as of 2026-11-14"); flower arm; Leave+Park for MXMOQ | 9d2aae7, 889102d, 5681728 |
| 11-19 | Mar: "cam and odometry calibration (worked as intended) (missing odometry deg calibration)"; added `pera` | f291bfb |
| 11-20 | Turrubiates: new DS names with legacy fallback; BRAKE; shooter PIDF from logs; gate added then **removed**; indexer jam + RPM pause; auto-align **disabled**; pollen 3000 RPM | 3ac6a93…dac50af |
| 11-21 | Mar: field map + auto-aim; odometry X/Y offsets measured | 2d92fba |
| 11-22 | Luis: Pedro autos (Tick/Center Lane/…) merged to `master` after simulation; intake 100% on luis/* | 37b7a16, b9c6e12 |
| 11-23 → 11-24 | Turrubiates: odometry autos tested on the field; intake REVERSE; Y pod REVERSED; pod offsets fitted; TIP detection; HuskyLens counting; NECTAR alert light | 3417819…a7b8f4c |

Known bugs, hacks and TODOs:
1. Pedro follower constants never tuned (`maxPathSpeed 0.6` cap). `VA` autos bypass Pedro path following with its own `driveTo()`. CONFIRMED.
2. Pod offsets were fitted, not measured, and conflict with Mar's measurement (§3). CONFIRMED TODO.
3. Possible intermittent Pinpoint/pod connection fault (5 m jump, reset to 0,0,0). CONFIRMED in 3fbcbb8.
4. `turnTo()` was given radians instead of degrees (fixed). CONFIRMED in 12377ee.
5. Auto-align overshoot toward the HIVE with Full 3D → disabled until calibrated. CONFIRMED in 21900d5.
6. `Pera` closed angle −35° clips to 0.0 (`NCAM`). CONFIRMED from the code.
7. `FlowerArm` does not compile; the `FLOWER` TeleOp double-binds left trigger. CONFIRMED.
8. Camera pitch 40° "measure on the robot". Indicator yellow 0.42 "fine-tune". Shooter `TICKS_PER_REV` "calibrate". CONFIRMED TODOs.
9. Battery sag: the nectar flywheel cannot reach 4500 RPM at ~11 V. Brownout protection was added. CONFIRMED.
10. Preloaded row jams when the intake pushes while the indexer waits → feed one at a time + intake shake. CONFIRMED in bfd8bc5, 7004eba.

## 10. Which branch is the current robot?

**My judgment: `origin/feature/turrubiates/vision-align` (a7b8f4c, 2026-11-24) is the best description of the physical robot today.**

1. It is the newest line. Its commits from 11-19 to 11-24 cite real field runs with timestamps and logs
   (e.g. "PreloadRouteAuto test 2026-11-23", "16:56 run", "29 of 46 scored").
2. It corrects values on the real robot that the other branches still have wrong: intake direction, Y pod
   direction, pod offsets, shooter PIDF, nectar RPM, and the gate removal.
3. Its rename map was built from `NCAM`@182e03d, "the code running on the robot as of 2026-11-14". It folded
   in Mar's confirmed devices (Pinpoint, Limelight, `alejandro` = indexer, `imu 1`, `pera`).
4. `MASTER` (11-22) is the other candidate. Its autos were only simulated, it still has intake FORWARD and
   zero pod offsets, and its indexer runs with nectar only. It is **not** merged with `VA`: the two have
   diverged since `develop` (afbabf1).

Caveat: `VA` is not merged anywhere. The task brief said the latest commits are from about 11-22, but `VA` has commits from 11-23 and 11-24.
The flower arm (`FLOWER`) and Mar's newest odometry work (`NCAM` 11-21) are not in `VA`.

## 11. Differences between branches about the robot

| Topic | VA (current) | MASTER | NCAM | Others |
|---|---|---|---|---|
| Drive names | `frontLeftDrive`… (+ legacy) | `FL`… | `FL`… | `FL`… |
| Drive zero power | BRAKE | FLOAT | BRAKE | FLOAT (DEV) |
| Drive mode | RUN_WITHOUT_ENCODER | RUN_USING_ENCODER | RUN_USING_ENCODER | — |
| Wheel / motor | 104 mm / 312 RPM | 104 mm / 312 RPM | 104 mm / 312 RPM | 96 mm / 435 RPM (LUISAUTO) |
| Robot size | "18 x 18 in" | 17.83 W × 16.54 L in (16.54 "tall" in a comment) | 17.5 W × 18.0 L in (`BiobuzzFieldMap`) | — |
| Pods | 2, Y REVERSED, offsets −6.79 / −0.72 in (fitted) | 2, F/F, 0/0 | 2, F/F, +65 / −170 mm (measured), scale 1.01 | 1 pod (ONE) |
| Intake dir | REVERSE | FORWARD | FORWARD | FORWARD |
| Intake power | 0.75 | 0.75 | 0.75 | 1.0 (luis/*) |
| Indexer name / trigger | `indexerMotor` / either shooter | `indexer` / nectar only | `alejandro` / either | `alejandro` |
| Gate | none (removed) | none | `pera` | `stopServo` (renata) |
| Flywheel RPM P / N | 3000 (auto 2950) / 3600 | 2900 / 4500 | 3000 / 3200 | 3200 / 2600 (LUISAUTO) |
| Shooter PIDF | 30/0/0/15 @12 V, voltage-compensated | 10/0.5/0/12 | — | own power PID (LUISAUTO) |
| Vision | Limelight + HuskyLens | Limelight | Limelight | HuskyLens (renata) |
| HuskyLens IDs | 2 = red, 3 = blue | — | — | 2 = blue, 3 = red (BALLS) |
| Alliance mirroring | none (same path) | mirrored, "blue = left" | center-origin map | — |

## 12. PROPOSED hardware name mapping (needs team approval)

**PROPOSED — NOT APPROVED.** Every row changes the Driver Station configuration on every Control Hub. The team must approve it before any code uses it.

Note: the team already adopted a rename (`NAMING:CONVENTIONS.md`, `doc/naming/rename-map.md`), and `VA` uses it with legacy fallback.
Its names are already lowerCamelCase position+function. To avoid a third renaming, the column
"Proposed name" keeps the adopted name wherever it fits the rule. Shorter alternatives are listed for the team to choose from (Q15).

| Old name(s) seen | Already adopted on VA | Proposed name | Alternative (shorter style) | Notes |
|---|---|---|---|---|
| `FL` | `frontLeftDrive` | `frontLeftDrive` | `frontLeft` | |
| `FR` | `frontRightDrive` | `frontRightDrive` | `frontRight` | |
| `BL` | `backLeftDrive` | `backLeftDrive` | `backLeft` | |
| `BR` | `backRightDrive` | `backRightDrive` | `backRight` | |
| `intake` | `intakeMotor` | `intakeMotor` | — | |
| `alejandro`, `indexer` | `indexerMotor` | `indexerMotor` | — | |
| `P`, `shooterP`, `shooterPollen` | `pollenShooterMotor` | `pollenLauncher` | keep `pollenShooterMotor` | function-based, not left/right: the flywheels are per ball type, not physical left/right twins. Left/right position UNKNOWN (Q9) |
| `N`, `shooterN`, `shooterNectar` | `nectarShooterMotor` | `nectarLauncher` | keep `nectarShooterMotor` | same |
| `indicator` | `indicatorLight` | `indicatorLight` | — | |
| — | `nectarAlertLight` | `nectarAlertLight` | — | only if mounted (Q12) |
| `limelight` | `limelight` | `limelight` | — | product name for a unique coprocessor |
| `huskyLens` | `huskyLens` | `huskyLens` | `frontCamera` | |
| `pinpoint` | `pinpoint` | `pinpoint` | — | |
| `imu 1` | `expansionHubImu` | `expansionHubImu` | `imu` | the space in `imu 1` is fragile; unused by code |
| `pera`, `stopServo` | `shooterGateServo` (removed) | `shooterGateServo` | `gateServo` | only if a gate comes back (Q10) |
| `servoLeft`, `servoRight` | — | `flowerArmLeftServo`, `flowerArmRightServo` | `armLeft`, `armRight` | only if the flower arm is on the robot (Q14) |
| `lift`, `pollen2`, `nectar2`, `pollenFeeder`, `nectarFeeder` | — | (do not add) | — | speculative hardware (LUISAUTO) |

## QUESTIONS FOR THE TEAM

1. **Hub and port map.** No branch records which Control Hub / Expansion Hub port each motor, servo and I2C device uses. Please send a photo or screenshot of the active Driver Station configuration, including the I2C bus for `pinpoint` and `huskyLens`.
2. **Active DS names.** Which names does the robot's configuration use today: new (`frontLeftDrive`, `intakeMotor`, `indexerMotor`, `pollenShooterMotor`…) or legacy (`FL`, `intake`, `alejandro`, `P`…)? Is there more than one Control Hub or configuration file ("some Control Hubs still use the old configuration")?
3. **Drive motors and wheels.** Please confirm 4× goBILDA 5203 312 RPM (19.2:1) with 104 mm GripForce wheels. Luis's early code assumed 435 RPM and 96 mm.
4. **Odometry pods.** Please tape-measure both pod positions from the robot's center of rotation. Mar measured X pod +170 mm forward / 65 mm left and Y pod 170 mm back / 65 mm left. The `VA` turn-fit says X pod 6.79 in (172 mm) right and Y pod 0.72 in back. These may be swapped. Also confirm the Y pod direction with a push test, and whether the 1.01 distance scale still applies.
5. **IMU.** Is the Pinpoint IMU the only heading source? How is the Control Hub mounted (logo / USB directions), in case the hub IMU is ever used?
6. **Pedro tuning.** Has AutoTune (Pinpoint / Foresight tuners) ever been run? If yes, where are the printed values? What is the robot's mass, and its measured max forward/strafe velocity?
7. **Motor models.** Which motors drive the intake, the indexer and each flywheel? Are the flywheels direct-drive (28 ticks/rev, 6000 RPM 1:1)? What are the flywheel wheel diameters and compression?
8. **Intake direction and power.** `VA` says FORWARD spun outward, so the motor is REVERSE and the left bumper collects. Is that right on the robot today? Should intake power be 0.75 (`VA`) or 1.0 (luis/* "run intake at 100% power")?
9. **Ball path.** How do POLLEN and NECTAR reach their own flywheel from the single indexer (sorting / two lanes / one lane)? Where is each flywheel on the robot (left/right/top/bottom)? Should the indexer run for both shooters (`VA`) or only for nectar (`MASTER`)?
10. **Gate.** Is there any gate or stopper servo on the robot now? Were `pera` and `stopServo` the same servo?
11. **HuskyLens IDs.** Which learned IDs are on the HuskyLens now: 2 = red / 3 = blue (`VA`) or 2 = blue / 3 = red (`Balls-counting`)?
12. **Second light.** Is `nectarAlertLight` mounted and configured? Is the goBILDA Prism LED strip from `Balls-counting` on the robot?
13. **Field frame and alliances.** Please confirm that the same autonomous path is used for both alliances (`VA`, d06ea2d) and that columns A–C (left from the audience) are red. `MASTER` mirrors its paths and calls the left half "blue".
14. **Flower arm / placing.** Is Renata's flower arm (`servoLeft`/`servoRight`, 0 → 90°) on the robot? Is there any plan to place balls in FLOWERS/GARDENS, or is the robot POLLEN-shooting only this event?
15. **Naming style.** Please approve the §12 mapping. Do you keep the already adopted `…Drive` / `…ShooterMotor` names, or switch to shorter names (`frontLeft`, `pollenLauncher`)? The DS config must change to match.
16. **Robot size.** Is the robot 18 × 18 in (`VA`), 17.83 × 16.54 in (`MASTER`), or 17.5 × 18 in (`NCAM`)? How tall is it? (`MASTER` paths drive under the central structure.)
17. **Camera mount.** What are the measured Limelight pitch (code guesses 40°) and height? What is the tag spacing (0.1143 vs 0.0825 m)? Is "Full 3D" enabled on pipeline 0?
18. **Which branch to build on.** Can the team confirm that `feature/turrubiates/vision-align` is what runs on the robot now? Should `master` (Luis's Pedro paths) and `feature/mar/duopodriveNcam` (Mar's field map / auto-aim) be merged into it or retired?
19. **MXMOQ.** What is "MXMOQ" (event?), and which autonomous was run there?
