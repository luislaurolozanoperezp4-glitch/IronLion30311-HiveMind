# AUDIT_PEDRO — Pedro Pathing audit, FTC #30311 Iron Lion (BIOBUZZ 2026-27)

Auditor: Pedro Pathing Auditor Agent · Date: 2026-11-24
Sources (read-only): `legacy/pedro-pathing-biobuzz-analysis-a65cc9` (worktree copy of `feature/autonomos-beta` @ 206fa84 + one uncommitted change: the only uncommitted change was one `@Disabled` line on `ShootTurnCollectAuto`; the diff was kept local, not committed) and git history in `legacy/IronLion30311BIOBUZZInicioDeTemporada` (refs `origin/*`, `localcopy/*`).

Conventions in this document:
- **Pedro frame**: inches, origin bottom-left corner, X right, Y up, 144 x 144 in (the team's value; Pedro's own docs use a 141.5 in field, TO VERIFY), heading in degrees, counter-clockwise from +X.
- **Front = intake, rear = shooters + Limelight** on every branch.
- Blob hashes are the first 8 characters of the git blob, so identical files on different branches can be matched.
- **TO VERIFY** means the claim needs checking against the official BIOBUZZ Competition Manual or on the real field.

---

## 0. Executive summary

| Topic | Finding |
|---|---|
| Pedro version | `com.pedropathing:revhub:3.0.2` (Pedro Pathing 3) on every Pedro branch. On 2026-11-24 this is the **latest** version on Maven Central (3.0.0, 3.0.1, 3.0.2). AutoTune `com.pedropathing:tuning:1.0.3` (latest) is only on `feature/turrubiates/vision-align`. |
| FTC SDK | 12.0.0 on every branch. This is the latest on Maven Central. |
| Localizer | goBILDA Pinpoint (`pinpoint`, two 4-bar pods + IMU) through Pedro's `PinpointLocalizer`. No OTOS, two-wheel or three-wheel localizer exists in any branch. |
| Follower | `Foresight` (Pedro 3) with **placeholder gains on every branch**. No branch has run the Foresight tuner, and `maxPathSpeed` is capped at 0.6. |
| Best constants | `origin/feature/turrubiates/vision-align` (a7b8f4c). Pod offsets x=-6.79 in, y=-0.72 in, Y pod REVERSED and brake on manual stop, all fitted from runs on 2026-11-23. Its `PreloadRouteAuto` is the only autonomous with a logged result ("4/4 preload shots and parked in ~12-14 s"). It drives custom odometry legs through `follower.manual()`, not Pedro paths. |
| Paths | Every path in every branch is a `Paths.line(...)` (a straight BezierLine). There are no BezierCurves and no control points. The only multi-segment chains are `Paths.path(line, line)` in Tick and Center Lane (beta). |
| Biggest rule risk | **The alliance transform in `feature/autonomos-beta` looks wrong.** See section 5.2. The team's own field map (`feature/mar/duopodriveNcam`, "Manual TU10") describes a field with 180° rotational symmetry: red on the left (columns A-C), red LOADING ZONE at tile A5, blue LOADING ZONE at tile F2. Beta authors its poses as "blue = left" and mirrors them across x = 72 for red. As a result the default alliance (RED) runs a route that ends outside any LOADING ZONE, and picking the wrong button drives the robot into the opponent's half (G-rule on AUTO territory, TO VERIFY number, maybe G402). |
| Second rule risk | Tick and Center Lane (beta) drive "physically UNDER the central structure" (HIVE frame floor box ≈ x 47-97, y 52-92). The old visualizer Center Lane (auto-shooter, vision-align) crosses the HIVE frame outright. Both risk a collision with the HIVE and G417 (no contact with or forcing of the HIVE), TO VERIFY. |

---

## 1. Branch inventory (pathing / autonomous / localization)

"Pedro" means the branch imports `com.pedropathing`.

| Branch (ref) | Head | Date | Author | Message | Purpose | Pedro? | Worth mining |
|---|---|---|---|---|---|---|---|
| origin/feature/autonomos-beta (= localcopy/, = worktree) | 206fa84 | 2026-11-22 | luislaurolozanoperezp4-glitch | Fix wall collisions found in path simulation (Park, Shoot Turn Collect) | Pedro 3 autos: Tick, Center Lane, Outer Lane (disabled), Park, Shoot Turn Collect; `Constants.java`; field diagram | Yes | **High**: route structure, `PedroAutoBase`, mechanism modes, documented poses |
| origin/master | 37b7a16 | 2026-11-22 | luislaurolozanoperezp4-glitch | Merge PR #14 from feature/autonomos-beta | Same tree as autonomos-beta (`git diff` is empty) | Yes | Same as beta |
| worktree uncommitted | (diff) | - | - | adds `@Disabled` to `ShootTurnCollectAuto` | Hides Shoot Turn Collect | Yes | Low (one annotation) |
| localcopy/auto-shooter | d4642a0 | 2026-11-22 | luislaurolozanoperezp4-glitch | Add Shoot And Collect autonomous and WPILib reference routine | Fork of beta at bf35771 (red-side authoring, before the Tick rework) + `ShootAndCollectAuto` + `WPILibAuto/` reference | Yes | Medium: `ShootAndCollectAuto`, pre-rework StartRoutine/CenterLane/OuterLane/TopWall poses |
| origin/feature/turrubiates/vision-align | a7b8f4c | 2026-11-24 | turrubiates | feat(auto): watch up to 4 s after the preload for the slow TIP | Pedro + AutoTune + FTC Dashboard; tested `PreloadRouteAuto` / `PreloadCollectAuto`; Limelight TIP logic; pose logger; tuning OpModes | Yes | **Highest**: tuned localizer, tested routines, safety guards, tuners |
| origin/feature/renata/auto-test (also flower-arm, removing-Alejandro: same blob 67fce555) | 7c1e75a | 2026-11-14 | RenataP0911 | autonomo de prueba | Pedro visualizer export using Ivy commands (`com.pedropathing.ivy`) | Imports only | Low: does not compile (no Pedro dependency, class `AutoPath` in file `Auto-test.java`, `Constants.create` missing) |
| origin/feature/mar/duopodriveNcam | 2d92fba | 2026-11-21 | estra | improvements en odometria y auto apuntado. se empezo a mapear coordenadas claves | Non-Pedro `PinpointOdometry` + `AutoDrive`; **`BiobuzzFieldMap.java` (829 lines, field geometry from the manual)**; `RedCornerToCellTestAuto`; `juego-del-robot-biobuzz.md` (rules summary) | No | **High** for field geometry and rules; medium for odometry calibration (scale 1.01) |
| localcopy/feature/mar/duopodriveNcam | c73454f | 2026-11-19 | estra | cam and odometry calibration (worked as intended) | Older; `TopWallAuto` converted from Pedro poses to `AutoDrive` | No (mentions Pedro) | Low |
| origin/feature/mar/vision | dae149e | 2026-11-14 | estradamar | Merge duopodriveNcam into vision | Base of beta (`PinpointOdometry`, `AutoDrive`, `OdometryCalibrationAuto`) | No | Low |
| origin/feature/mar/duopodrive | 38caa7f | 2026-11-22 | luislaurolozanoperezp4-glitch | update | Two-pod `PinpointOdometry` (same blob as beta d9e1d163) | No | Low |
| origin/feature/mar/onepodrive | 898e0da | 2026-11-11 | estra | firstcommit, this only uses 1 odometrypod | One-pod odometry prototype | No | Discard |
| origin/feature/turrubiates/auto-leave-park (= luis/park LeaveParkAuto blob c67f790c) | e455142 | 2026-11-15 | turrubiates | fix(auto): start Leave + Park with the intake against the wall | `LeaveParkAuto`: start inside the LOADING ZONE, back 10 cm → LEAVE (3) + PARK (5) | No | Medium: safest fallback auto |
| origin/feature/luis/park | a8c4215 | 2026-11-22 | luislaurolozanoperezp4-glitch | feat(intake): run intake at 100% power | Contains LeaveParkAuto | No | Same as above |
| origin/feature/theautonomous | 125df22 | 2026-11-14 | luislaurolozanoperezp4-glitch | Mover las notas de los autonomos junto al codigo | Encoder-based "THE AUTONOMOUS": 9 routes, MatchClock/TimeModel, PLAN.md, NOTAS_AUTONOMOS.md | No | Medium: time-budget idea (MatchClock, park-exit per phase) |
| origin/feature/luis/autonomospaths | 9cf2c66 | 2026-10-22 | luislaurolozanoperezp4-glitch | autogeneral | 16 Red/Blue encoder autos (`AutoBase`, `autosubsystems/`) | No | Low (superseded by theautonomous) |
| origin/feature/renata/auto-align | 3e733d4 | 2026-11-07 | RenataP0911 | Button-declared | TeleOp auto-align (vision); no pathing | No | None for pathing |
| localcopy/claude/pedro-pathing-biobuzz-analysis-a65cc9 (= localcopy/master) | 160e1b9 | 2026-10-11 | estra | first commit | Ref of the worktree name. Points at the first commit, not the worktree head | No | None |

Every other ref (develop, SIT, PID, csv, Balls-counting, luis/alliance, naming, probe, shooters-upgraded, renata/shooter, turrubiates/hive-*, naming-conventions, the backup-*/button-* refs) has **0 Pedro hits** (`git grep -l -i pedro`) and no pathing code beyond the FTC samples.

### 1.1 History of autonomos-beta (Pedro commits only)

| Commit | Date | Change |
|---|---|---|
| 5beb414 | 11-14 | Adds Pedro 3 dependency, `PedroFollowerFactory`, `Alliance` (authored **red** side, mirror x=72 for blue), `StartRoutinePaths`, Red/Blue Start Routine OpModes |
| 30fc502 | 11-14 | `PedroAutoBase`; `CenterLanePaths` (notes that path 4 crosses the structure) |
| 72754d4 | 11-14 | `OuterLanePaths` + shot at point 4 (-97°) |
| 73e688c | 11-14 | `TopWallPaths` |
| 54d58b7 | 11-14 | One `@Autonomous` file per route; INIT alliance selector; Red/Blue files deleted |
| bf35771 | 11-22 | Preload shot (`shootPreload`, -95°), aim-at-goal turn, field diagram HTML |
| 50bbd6d | 11-22 | Rework: **authoring side switched to "blue = left, mirror for red"**, `Constants.java`, Shoot Turn Collect, TopWall → Park, Outer Lane `@Disabled`, Alejandro → Indexer |
| 72387df | 11-22 | Start Routine → Tick (crosses under the structure); Center Lane without in-place turns; `PedroAutoBase` → `opmodes/base` |
| 206fa84 | 11-22 | Park end x 9.5 (was in the wall); Shoot Turn Collect backs off 5 in before its 180° turn |

### 1.2 History of vision-align (Pedro and odometry commits only)

a2854e4 (Pedro routes + Limelight aim + HuskyLens pickup) → 311d259 (export to visualizer) → d06ea2d ("use the same path for both alliances in BIOBUZZ") → 877c61d (AutoTune + odometry test OpModes) → 3417819 (SafeMoveAuto) → 0f0f91c (PreloadRouteAuto) → 927e4df (Pinpoint strafe pod REVERSED) → 52f3305 (LEAVE 10 cm) → a65a310 (brake on stop; long leg 230 cm) → 12377ee (**Pinpoint pod offsets fitted**) → 91efe51 (split Preload+Park / Preload+Collect+Park) → 3fbcbb8 (refuse to drive when the odometry is off at start) → d490986 (real start pose (W,0)) → c1c59db (TIP detection, other cell) → d832230 (clear the FLOWER) → 3caa5da (pass-through corners) → 03cb6df (re-zero at start) → a7b8f4c.

---

## 2. Inventory of Pedro artifacts (by branch, deduplicated by blob)

Paths are relative to `TeamCode/src/main/java/org/firstinspires/ftc/teamcode/`.

### 2.1 Files

| File | Blob → branches | Notes |
|---|---|---|
| `subsystems/pathing/PedroFollowerFactory.java` | 04feea2e → beta, master, auto-shooter · d937b54f → vision-align | Builds `new Follower(PinpointLocalizer, Mecanum, Foresight)`. vision-align: public configs, new device names, tuned pod offsets, Y REVERSED, `manualBrakeMode` |
| `subsystems/pathing/Alliance.java` | 6d31631e → beta (blue authored, mirrorX(72) for red) · 11650090 → auto-shooter (red authored, mirrorX(72) for blue) | vision-align replaced it with `AlliancePoses` (582d5193): identity `PoseFactory.degrees()` for both alliances, plus `subsystems/vision/Alliance` (scoring/audience cell) |
| `opmodes/base/PedroAutoBase.java` | 882a98b2 → beta | ~330 lines: INIT alliance selector, `followPath`, `holdFor`, `shootPreload`, `shoot` + `aimAtGoal`, `spinUpUntilReady`, `turnBy`, `driveForward`, `shootWhenReady`, `keepIntakeAndIndexerOn`, `stopMechanisms` |
| `opmodes/auto/PedroAutoBase.java` | 110812e6 → auto-shooter · b24b0247 → vision-align (53 KB, @Config) | vision-align adds `driveTo/driveThrough/driveLeg` (P-controller on the Pinpoint pose through `follower.manual`), `turnTo`, `settle`, `holdTarget`, `lookForCell`, `watchForTip`, `shootPollenPreload`, `alignAndShoot`, `collectBalls`, start-pose check, odometry-jump freeze |
| `Constants.java` | 43c1c9d6 → beta | `Field`, `Tick`, `CenterLane`, `ShootTurnCollect` (see 2.3) |
| `opmodes/auto/TickAuto.java` | 6c41f8d7 → beta | Was StartRoutineAuto |
| `opmodes/auto/CenterLaneAuto.java` | 8770b833 → beta · b0806802 → auto-shooter · cbcb502a → vision-align | Beta = new route; the other two = old visualizer route |
| `opmodes/auto/OuterLaneAuto.java` | 7dcbf8c6 → beta (@Disabled) · 2af3675f → auto-shooter · 728c37a1 → vision-align | Same poses; backOff heading differs |
| `opmodes/auto/ParkAuto.java` | 1789cd5a → beta | Was TopWallAuto |
| `opmodes/auto/TopWallAuto.java` | 7072a1fe → auto-shooter · ddd088a6 → vision-align · 3b618915 → lc/duopodriveNcam (AutoDrive) · c422f0ad → duopodriveNcam (AutoDrive 73 cm) | |
| `opmodes/auto/StartRoutineAuto.java` | 570ee533 → auto-shooter · f1914b5b → vision-align | |
| `opmodes/auto/ShootTurnCollectAuto.java` | 05c2c330 → beta (+ uncommitted `@Disabled`) | |
| `opmodes/auto/ShootAndCollectAuto.java` | 162bd464 → auto-shooter | |
| `opmodes/auto/PreloadRouteAuto.java` | d21bf3ef → vision-align | Tested |
| `opmodes/auto/PreloadCollectAuto.java` | 6a9cc9c6 → vision-align | Subclass, `isCollecting() = true` |
| `opmodes/auto/OdometrySquareTestAuto.java` | 9fc8333f → vision-align | 24 in square, constant heading |
| `opmodes/auto/SafeMoveAuto.java` | 24d5a138 → vision-align | Encoders only (no Pedro) |
| `opmodes/calibration/PedroPoseCheck.java` | fdf9d6d9 → vision-align | TeleOp, drive by hand, watch Pedro pose |
| `subsystems/pathing/tuning/{Tuning,MecanumTuner,PinpointTuner,ForesightTuner,Tests}.java` | 19d9fc89, 614dfc2d, 08a097f5, 51544345, 314d7545 → vision-align | AutoTune procedures (`@Tuner`), web UI at http://192.168.43.1:10158 |
| `subsystems/pathing/{PoseLogger,FieldDrawing}.java` | 3e099780, 7c4dcba0 → vision-align | CSV pose log; FTC Dashboard field drawing |
| `opmodes/teleop/Auto-test.java` | 67fce555 → renata/auto-test, flower-arm, removing-Alejandro | Broken (see section 1) |
| `docs/autonomos-cancha.html` | 6aa75b0e → beta · 95792a76 → auto-shooter | Spanish field diagram of the routes |
| `scripts/odometry_routes_to_visualizer.py`, `scripts/routes_to_visualizer.py` | 29af7c6f (+ one more) → vision-align | Export routes to Pedro visualizer |
| Removed in beta history | `subsystems/pathing/{StartRoutinePaths,CenterLanePaths,OuterLanePaths,TopWallPaths}.java`, `opmodes/auto/{Red,Blue}{StartRoutine,CenterLane,OuterLane,TopWall}Auto.java` | Exist only at 5beb414..73e688c (deleted in 54d58b7) |

Non-Pedro localization files on the same branches: `subsystems/PinpointOdometry.java` (blobs d9e1d163 beta/auto-shooter/duopodrive/vision, 65d5a901 lc/duopodriveNcam, 13a0c324 duopodriveNcam, 4abb7d98 luis/park + auto-leave-park, feb667fb onepodrive), `subsystems/AutoDrive.java` (42327c8e common, 86ed333f duopodriveNcam, 22503e3c onepodrive), `opmodes/calibration/OdometryCalibrationAuto.java` (854c7221 beta) and `opmodes/auto/OdometryCalibrationAuto.java` (d8c3018d, 6c2f1b69, 21a1edd0).

### 2.2 Pedro API used (Pedro 3 names, identical on every branch)

`com.pedropathing.follower.Follower` (`new Follower(localizer, drivetrain, algorithm)`, `setPose`, `update`, `follow(Path)`, `following()`, `hold(Pose)`, `holding()`, `manual(fwd, strafe, turn)`, `stop()`, `pose()`, `currentPath()`, `distanceToEndpoint()`, `pathIndex()`) · `com.pedropathing.api.Paths` (`line(a,b)`, `path(p1,p2)`, `curve` imported only in tuning `Tests`) · heading interpolation on a Path: `.linear(a,b)`, `.constant(pose)`, `.tangent()`, `.reverseTangent()` · `com.pedropathing.api.PoseFactory` (`degrees()`, `of(x,y,h)`, `mirrorX(72)`) · `com.pedropathing.math.{Pose, Matrix, Vector2D}` (`Pose.withHeading`, `x()`, `y()`, `heading()`) · `com.pedropathing.algorithm.{Foresight, ForesightConfig}` · `com.pedropathing.controllers.Controller` (`piecewise`, `proportional`, `proportionalFeedforward`) · `com.pedropathing.revhub.drivetrains.{Mecanum, MecanumConfig}` · `com.pedropathing.revhub.localizers.{PinpointLocalizer, PinpointConfig}` · tuning: `@Tuner`, `Procedure` · Renata only: `com.pedropathing.ivy.*` (Command, Scheduler, `PedroCommands.follow`, `Groups.sequential`).

### 2.3 Tuning and config constants

**MecanumConfig**

| Field | beta / auto-shooter | vision-align |
|---|---|---|
| FL/FR/BL/BR names | `FL`,`FR`,`BL`,`BR` | `frontLeftDrive`,`frontRightDrive`,`backLeftDrive`,`backRightDrive` (RobotHardware keeps the legacy names as a fallback; Pedro does not) |
| Directions | FL REVERSE, BL REVERSE, FR FORWARD, BR FORWARD | same |
| manualBrakeMode | default (FLOAT) | **true** (FLOAT rolled ~12 cm past a 240 cm leg) |

**PinpointConfig** (inches)

| Field | beta / auto-shooter | vision-align (tested 2026-11-23) | Mar `PinpointOdometry` (duopodriveNcam, non-Pedro, "worked as intended") |
|---|---|---|---|
| name | `pinpoint` | `pinpoint` | `pinpoint` |
| pod type | goBILDA_4_BAR_POD | same | resolution set manually: 198.9437 x 1.01 = 200.933 ticks/cm |
| xPodDirection | FORWARD | FORWARD | FORWARD |
| yPodDirection | FORWARD | **REVERSED** (strafed right but read left) | FORWARD |
| xPodOffset | 0.0 (TODO) | **-6.79 in** (fitted from a 92° spin, residual 0.08 in) | X_POD_OFFSET_MM = 65 (2.56 in) |
| yPodOffset | 0.0 (TODO) | **-0.72 in** | Y_POD_OFFSET_MM = -170 (-6.69 in) |

These sources disagree. Mar's values look swapped relative to Turrubiates': both report a ~170 mm magnitude, but on different axes. Use vision-align's values: they were fitted on the robot and the commit says so. Then confirm them with the AutoTune Pinpoint procedure and a tape measure (TO VERIFY). Pedro's `PinpointConfig` exposes no ticks-per-unit scale here, so Mar's 1.01 scale cannot be applied directly (TO VERIFY in the API).

**ForesightConfig**: identical placeholder values on every branch, never tuned. forward/strafe translational = piecewise(P 0.03, >2.5 → P 0.1); coast = PF 0.01; brake = PF 0.01; headingFeedback = P 1.0; headingBrake (0.1, 0.01); linearBrake diag(0.05,0.05); quadraticBrake diag(0.001,0.001); maxForwardVel 60; maxStrafeVel 45; naturalForwardDecel 40; naturalStrafeDecel 40; **maxPathSpeed 0.6**.

**PedroAutoBase constants**

- beta: PATH_TIMEOUT 5 s, PRELOAD_HEADING -95°, PRELOAD_SPIN_UP 1 s, PRELOAD_SHOOT 2 s, GOAL (60, 62), AIM_TOLERANCE 3°, AIM_TIMEOUT 1.5 s.
- vision-align (all `public static`, @Config): PATH_TIMEOUT 8 s, DRIVE_MAX_POWER 0.85, DRIVE_MIN_POWER 0.25, DRIVE_SLOW_ZONE 18 in, DRIVE_TOLERANCE 0.6 in, DRIVE_ACCEL 3.0/s, DRIVE_KP_HEADING 1.0, DRIVE_MAX_TURN 0.45, DRIVE_CORNER_POWER 0.45, DRIVE_PASS_THROUGH 4 in, TURN_KP 1.2, TURN_MAX 0.5, TURN_MIN 0.15, TURN_TOL 2°, TURN_TIMEOUT 2.5 s, HOLD_KP 0.12, HOLD_MAX 0.25, HOLD_DEADBAND 0.3 in, POSE_JUMP_LIMIT 12 in / 30°, START_POSE_TOL 2 in, START_REZERO_MAX 12 in / 10°, START_HEADING_TOL 5°, DRIVE_TIMEOUT 1.5 s + d/20 in/s, intake wiggle 0.8/0.35 s x3, ball approach 0.3.
- vision-align PreloadRouteAuto (tested values): LEAVE_BACK 10 cm, SECOND_BACK 5 cm, RIGHT_1 60 cm, LONG_BACK 230 cm, RIGHT_2 60 cm, COLLECT_TURN 90° CW, COLLECT_START 60 cm, COLLECT_END 120 cm, COLLECT_POWER 0.2, RETURN_POWER 0.6, PRELOAD_POLLEN_RPM 2950, OTHER_CELL_POLLEN_RPM 2950, PRELOAD_BALLS 4, SHOOT_MAX 10 s, LATEST_SHOOT_END 22 s, COLLECT_LATEST_START 11 s, OTHER_CELL_LATEST_START 15 s, OTHER_CELL_SHOOT_END 26.5 s, EARLY_TIP_WATCH 4 s, TIP_CHECK 0.6 s, OTHER_SPOT_SHORTEN 15 cm.
- beta `Constants`: shooter RPM P 2900 / N 4500 (all routes, TODO), SPIN_UP_TIMEOUT 3 s, FEED 2 s, turns ±3° / 2 s, Field 144, ROBOT_WIDTH 17.83, ROBOT_LENGTH 16.5354 (Mar's field map uses 18.0 x 17.5 in, TO VERIFY with the real robot).

### 2.4 Analysis notes found

- Worktree `PROJECT_CONTEXT.md` (Pedro section, lines 34-97), `DRIVE_MODES_REFERENCE.md` (twice: root and `docs/`), `docs/autonomos-cancha.html` (Spanish route diagram). `doc/legal` and `doc/media` are FTC SDK boilerplate.
- vision-align `CONVENTIONS.md` (naming; front = intake against the wall at the start; inches for path planning) and `doc/naming/rename-map.md`.
- duopodriveNcam `juego-del-robot-biobuzz.md` (Spanish rules summary: G304 start, AUTO territory, LEAVE 3 / PARK 5, G407, G417) and `BiobuzzFieldMap.java`.
- theautonomous `NOTAS_AUTONOMOS.md` and `theautonomous/PLAN.md` (30 s time budget, park-exit per phase).

---

## 3. Versions and migrations

| Branch | Pedro | Tuning | Repos | FTC SDK |
|---|---|---|---|---|
| autonomos-beta, master, auto-shooter | revhub 3.0.2 | none | mavenCentral, repo.dairy.foundation/releases, google | 12.0.0 |
| vision-align | revhub 3.0.2 | tuning 1.0.3 | + maven.brott.dev (FTC Dashboard 0.6.0) | 12.0.0 |
| renata/* | none (but imports Pedro + Ivy) | none | default | 12.0.0 |
| all others | none | none | default | 12.0.0 |

Checked online on 2026-11-24:
- Maven Central `com.pedropathing:revhub` lists 3.0.0, 3.0.1 and 3.0.2, so 3.0.2 is the latest.
- `com.pedropathing:tuning` latest is 1.0.3.
- `org.firstinspires.ftc:RobotCore` latest is 12.0.0.
- The pedropathing.com installation page still lists `maven { url 'https://repo.dairy.foundation/releases/' }`.
- The PoseFactory docs list only `degrees()`, `radians()`, `mirrorX(line)`, `mirrorY(line)` and `of()`, with an example field of **141.5 in** (center 70.75).

**No library migration is needed.** The required migrations are:
1. Use one dependency block: `revhub:3.0.2` + `tuning:1.0.3` + the dairy repo (needed by tuning/Sloth) + FTC Dashboard if it stays.
2. Pick **one** set of Driver Station device names. The Pedro `MecanumConfig` and `PinpointConfig` names must match `RobotHardware` exactly, because Pedro has no fallback to the legacy names.
3. Field size: the team code uses 144 in, Pedro's docs use 141.5 in. Choose one and use it in every mirror or rotate call (TO VERIFY with the manual's inner field dimension; Mar's map says 365.76 cm = 144 in).
4. Alliance transform (see 5.2). Pedro has no rotate function, so a 180° rotation needs `mirrorX(c).mirrorY(c)` (if the factory calls chain, TO VERIFY), or the same alliance-relative poses for both alliances (vision-align approach).
5. `.linear(a, b)` from -96° to 180° (Center Lane C2) may turn the long way. Check which way Pedro 3 interpolates (TO VERIFY).
6. Run AutoTune: Mecanum → Pinpoint → Tests/Localization → Foresight → Line/Curve/Hold tests. Then remove `maxPathSpeed 0.6`.
7. Renata's Ivy code needs an Ivy artifact that no branch declares (TO VERIFY the artifact name). Recommendation: do not adopt it.

---

## 4. Cross-branch comparison

- **Best-tuned constants**: vision-align `PedroFollowerFactory` (pod offsets, Y REVERSED, brake). The Foresight gains are placeholders everywhere. Beta's values are all TODO.
- **Most validated routine**: vision-align `PreloadRouteAuto` ("validated: 4/4 preload shots and parked in ~12-14 s"; RPM 2950 because 3000 "went long, test 2026-11-23"; LEAVE 10 cm because 5 cm was not clearly enough). `PreloadCollectAuto` is tuned from logged runs (11-24 16:12, 16:56, 17:12-17:15), but there is no full-success claim yet.
- **Most complete Pedro path set**: beta (Tick, Center Lane, Park, Shoot Turn Collect, Outer Lane). Collisions were checked in simulation (206fa84), but nothing was field-tested and there is no commit saying "tested".
- **Safest geometry**:
  - Outer Lane stays at x ≤ 56 and avoids the HIVE.
  - PreloadRoute stays at x ≤ 57 and passes west of the HIVE through x ≈ 33.
  - Park / LeaveParkAuto stay at the wall.
- **Old visualizer routes** (StartRoutine / CenterLane / TopWall in auto-shooter and vision-align) are superseded. Old Center Lane crosses the HIVE frame, and TopWall spins 180° against the wall.

---

## 5. BIOBUZZ rules check (all TO VERIFY against the Competition Manual)

### 5.1 Field model (from `BiobuzzFieldMap`, duopodriveNcam, converted to the Pedro frame with x = East + 72, y = North + 72)

| Element | Pedro frame |
|---|---|
| Red territory | columns A-C → x 0-72 (left). Blue territory D-F → x 72-144 |
| Audience wall | y = 0. Back wall y = 144 |
| HIVE frame floor box | x 47.3-96.7, y 52.5-91.5 (it spans both territories) |
| HIVE cells | red audience ≈ (59.25, 57.4) starts UP; red back ≈ (59.25, 86.6) starts DOWN; blue back ≈ (84.75, 86.6) starts UP; blue audience ≈ (84.75, 57.4) starts DOWN |
| Red LOADING ZONE (A5) | x 0-11, y 96.5-119.5 |
| Blue LOADING ZONE (F2) | x 133-144, y 24.5-47.5 |
| GARDENS | A1 (red), F6 (blue) |
| FLOWERS | audience (96, ≈1.9); back wall (48, ≈142.1); red wall (≈1.9, 48); blue wall (≈142.1, 96) |
| Rule summary | G304: start fully inside own territory, touching the perimeter wall, within 18 in. AUTO: no entering the rival territory. LEAVE = 3 points (no wall contact at end of AUTO). PARK = 5 points (any part of the robot in own LOADING ZONE at end of AUTO). AUTO lasts 30 s. |

The field is **rotationally symmetric (180°)**, not mirror symmetric. Beta's comment "loading zone on this side ≈ x 0-13, y 96-120" matches the **red** LOADING ZONE.

### 5.2 Alliance transform: HIGH risk

- **Beta** (`Alliance`, blob 6d31631e) says blue plays left and mirrors x → 144 - x for red. Under the model in 5.1:
  - The authored left-side poses are red geometry: the goal (60, 62) is the red audience cell (UP at start), and the park is in the red LOADING ZONE.
  - Selecting **BLUE** in INIT runs the red route. A blue robot placed legally on the right then drives about 75 in to the left into red territory (AUTO territory violation, likely a Major Foul).
  - Selecting **RED** (the default) mirrors to x ≈ 134 and y ≈ 108. That is not the blue LOADING ZONE (y 24.5-47.5), so the robot loses PARK. It aims at the blue audience cell, which starts DOWN. A red robot placed legally on the left drives right toward x ≈ 130, into blue territory.
  - Net effect: the alliance labels are swapped and the mirror is the wrong transform.
- **auto-shooter** (red authored, mirror for blue): the red labels are right, but blue still parks at (≈134, ≈118), outside the blue LOADING ZONE, so blue loses PARK.
- **vision-align** (`AlliancePoses`): identity for both alliances. This is correct **if** the Pedro frame is alliance-relative, meaning each robot's start is "its own" (57, 9, -90). Rotational symmetry makes that work, and only the HIVE cell (tag IDs) changes. Absolute field drawings (FTC Dashboard, AprilTag relocalization) must rotate blue by 180°.
- **Recommendation**: author in the red-absolute frame, which is also the alliance-relative frame. Use the same poses for both alliances. Apply `rotate180` (x → 144 - x, y → 144 - y, h → h + 180°) only when converting to absolute field coordinates. Never use a plain `mirrorX`.

### 5.3 Per-route checks

Robot footprint: half-length ≈ 8.3-9.0 in, half-width ≈ 8.75-8.9 in, turning radius ≈ 12.2-12.6 in.

| Route (branch) | Start (G304) | Max x / opponent half | HIVE / FLOWER | LEAVE | PARK in LZ | Est. time |
|---|---|---|---|---|---|---|
| Tick (beta) | (60, 8.52, -90): x span 51-69 OK; 0.25 in gap to the wall, so it may not be "touching" (TO VERIFY) | Turns in place at (60, 24) and (60, 120) sweep to x ≈ 72.2, **marginal** | **Drives under the HIVE** x 60, y 30→120 (frame base 52.5-91.5). **HIGH risk**: collision, G417 | Yes | (9.92, 108) red LZ OK (edge 1.0 in off the wall) | ≈ 22-28 s (two shots ≤ 5 s each + turns + ≈ 260 in at 0.6 cap; the 5 s path timeout may cut the 90 in leg) |
| Center Lane (beta) | (56, 8.52, -90) OK | crossBottom/shoot2 x 60, edge ≈ 68.9 OK | **Under the HIVE** x 60, y 40→120, HIGH risk. Front at x ≈ 1.5 near the red-wall FLOWER (≈1.9, 48) while the robot reaches y ≈ 39 (≈ 5 in clear) | Yes | (9.92, 108) OK | ≈ 20-26 s |
| Outer Lane (beta @Disabled; auto-shooter; vision-align) | (56, 8, 90) OK | ≤ 56 + 9, OK | Clear of the HIVE (diagonal at x ≈ 35 for y 48); passes ≈ 10 in from the red-wall FLOWER | Yes | (10.08, 118.67) inside the LZ y limit 119.5, **marginal** | ≈ 20-25 s |
| Park (beta) | (8.96, 131.19, 90) at the red wall, tile A6, OK | OK | - | Edge 0.59 in off the wall, **marginal** | (9.5, 102.45) OK | ≈ 2-3 s |
| Shoot Turn Collect (beta) | (56, 8.52, -90) OK | OK | - | Yes | **No PARK** (ends at (56, 37.5)) | ≈ 8-10 s |
| Shoot And Collect (auto-shooter) | (56, 8, -95) | OK | **Turns in place at y = 8 (wall collision)**, the bug fixed in beta 206fa84 | Yes | **No PARK** | ≈ 8 s |
| Start Routine (auto-shooter / vision-align) | (56, 8, 90) OK | Alignment at x 56.5 plus a turn reaches ≈ 68.7, OK | topApproach (56.4, 129.0, -145°) reaches y ≈ 141 near x 48-56, a **back-wall FLOWER (48, 142) risk** | Yes | (9.10, 117.56) OK; edge ≈ 0.2 in from the wall, **LEAVE risk** | ≈ 22-28 s |
| Center Lane old (auto-shooter / vision-align) | (56, 8, 90) OK | centerTop (63.3, 130.0, 86.6°) edge ≈ 72.3; spin ≈ 177° at (63, 126.8) sweeps to x ≈ 75.5, **opponent half** | **Crosses the HIVE frame** (x 56→63 for y 52-92), DISCARD | Yes | (9.94, 116.94) OK, wall edge ≈ 1 in | - |
| Top Wall (auto-shooter / vision-align) | (8.96, 131.19, 90) | OK | vision-align version spins 180° against the wall (corner collision) | End x 8.77, **in wall contact, no LEAVE** | Inside the LZ | ≈ 3 s |
| Renata Auto-test | (9.50, 133.18, 90) | OK | Constant -90 from a 90 start, so it spins at the wall | End x 8.90, **no LEAVE** | Inside the LZ | - |
| PreloadRoute (vision-align, tested) | (57, 9, -90) tile vertex (W,0), front to the audience wall, OK | ≤ 57 + 9 = 66, OK | Passes west of the HIVE through x 33.4 | 10 cm back, tested | right2 (9.76, 105.46) OK, edge ≈ 0.8 in (LEAVE **marginal**) | **12-14 s measured** |
| PreloadCollect (vision-align) | same | ≤ 66, OK. otherSpot (57, 125.2, 90) north of the HIVE, edge ≈ 66 | Shortened 15 cm to clear the back-wall FLOWER | Yes | Same park | ≈ 26 s planned, with time guards at 11 / 15 / 22 / 26.5 s |
| LeaveParkAuto (auto-leave-park) | Inside the LZ, front to the wall | OK | - | 10 cm | Yes | < 1 s plus delay |

General rule notes:
- End every route with **≥ 2 in** between the robot and the wall so LEAVE is unambiguous (several end 0.2-1.0 in off the wall).
- Every route must end in its own LOADING ZONE before 30 s. A route-level time guard (vision-align or theautonomous MatchClock) is required.
- HIVE targets: beta aims the rear at (60, 62), which is the red audience cell (≈ 59.25, 57.4, UP at start). After a TIP the scoring cell swaps. vision-align handles this with a TIP watch and the "other cell".

---

## 6. Relation to the rest of repo #1

| Branch | Shared hardware names | Subsystems used by autos | Relation |
|---|---|---|---|
| autonomos-beta / master | `FL FR BL BR`, `indicator`, `intake`, `indexer` (was `alejandro`), `P`, `N`, `limelight`, `pinpoint` (`RobotHardware`) | `IndicatorLight.setRed/setBlue`, `Intake.update/stop`, `Indexer.update/stop`, `Shooter.fireP/fireN/stop/atTargetRPM/setTargetRpm/getCurrentRpmP/N`; Pedro `Follower` owns the Pinpoint in auto; `PinpointOdometry` + `AutoDrive` still exist for `OdometryCalibrationAuto` and TeleOp `HiveAlignController` | Pedro is layered on top of the Mar vision base (merge 654445d). TeleOp does not use Pedro |
| auto-shooter | same as beta (Alejandro era) | `Shooter` RPM-gated FEED | Fork of beta at bf35771 plus a WPILib reference |
| vision-align | **renamed** `frontLeftDrive…`, `indicatorLight`, `intakeMotor`, `indexerMotor`, `pollenShooterMotor`, `nectarShooterMotor`, `limelight`, `huskyLens`, `nectarAlertLight`, `pinpoint` (legacy fallback in RobotHardware only) | `HiveVision` / `HiveStateTracker` (TIP), `BallVision` / `BallIntakeCounter`, `Shooter` pollen RPM, `ShooterLogger`, `MatchSummary`, `BatteryMonitor`, `LogFiles`, FTC Dashboard | Most advanced. Diverged from develop on 11-15 (naming conventions). `MainTeleOp` also references Pedro |
| duopodriveNcam | `FL…`, `pinpoint` | `PinpointOdometry` (center-origin, North/East cm), `AutoDrive.driveToPoseCm/turnToDeg`, `HiveAlignController` (vision + odometry hybrid) | Its own coordinate system (center origin, heading 0 = North, CW+), **incompatible** with Pedro (bottom-left, CCW+). The field map must be converted |
| luis/park, auto-leave-park | `FL…`, `pinpoint` | `PinpointOdometry`, `GuardedStraightDrive` | Fallback auto that works without Pedro |
| theautonomous, luis/autonomospaths | `autosubsystems/AutoHardware` | Encoder legs | Independent pre-Pedro system; contributes the time-budget concept |

---

## 7. Full path catalogue

For each path the straight-line copy is defined as: same endpoints, `Paths.line(start, end)`, and the original heading interpolation, or `.linear(start, end)` where noted. All originals are already BezierLines with **no control points**. Only the two-segment chains change geometry: their straight copy is one line from the first start to the last end, which skips the intermediate pose. Keep the originals and add copies named `<route>Straight`.

Poses are in the Pedro frame, inches and degrees, authored side = left (red per 5.1).

### 7.1 autonomos-beta @206fa84 (Constants-driven)

**Tick** (`TickAuto`, `Constants.Tick`): start (60, 8.5177, -90)

| # | Segment | Start | End | Heading interp | Mech | Straight copy |
|---|---|---|---|---|---|---|
| T1 | offTheWall | (60, 8.5177, -90) | shoot1 (60, 24, -85) | linear -90→-85 | IDLE | same |
| - | shoot (3 s spin + 2 s feed), turnBy -95° → 180 | | | | | |
| T2 | intoCorner | shoot1Turned (60, 24, 180) | corner (9.7677, 16, 180) | constant 180 | INTAKE | same |
| T3a | sidewaysToShootSpot seg 1 | corner (9.7677, 16) | crossBottom (60, 30) | constant 180 (backwards) | IDLE | T3 straight: (9.7677, 16) → (60, 120), constant 180. **It cuts diagonally through the HIVE, so do not use it** |
| T3b | seg 2 | crossBottom (60, 30) | shoot2Approach (60, 120, 180) | constant 180 (strafe) | IDLE | |
| - | turnBy -90° → 90, shoot | | | | | |
| T4 | sidewaysToPark | shoot2 (60, 120, 90) | park (9.915, 108, 90) | constant 90 | IDLE | same |

**Center Lane** (`CenterLaneAuto`, `Constants.CenterLane`): start (56, 8.5177, -90)

| # | Segment | Start | End | Interp | Mech |
|---|---|---|---|---|---|
| C1 | offTheWall | (56, 8.5177, -90) | shoot1 (56, 24, -96) | linear | IDLE (then shoot) |
| C2 | intoCorner | shoot1 | corner (9.7677, 12, 180) | linear -96→180 (direction TO VERIFY) | INTAKE |
| C3 | upTheWall | corner | wallSweepEnd (9.7677, 30, 180) | constant 180 | INTAKE |
| C4a | underTheStructure 1 | wallSweepEnd | crossBottom (60, 40, 90) | linear 180→90 | INTAKE |
| C4b | underTheStructure 2 | crossBottom | shoot2 (60, 120, 90) | constant 90 | INTAKE (then shoot) |
| C5 | sidewaysToPark | shoot2 | park (9.915, 108, 90) | constant 90 | IDLE |

Straight copy of C4: (9.7677, 30) → (60, 120), linear 180→90. It also crosses the HIVE.

**Outer Lane** (`OuterLaneAuto`, @Disabled): start (56, 8, 90); backOff (56.0449, 14.1062, -95 [beta, auto-shooter] / 89.5791 [vision-align]); nearCorner (9.1714, 9.1132, -173.9198); startColumn (54.8148, 16.8376, -170.3946); shootPose (54.8518, 20.287, -97); outerWall (12.6814, 79.1872, 125.6013); park (10.0773, 118.6701, 93.7734).
- O1 start→backOff: linear (vision-align: tangent), then preload shot.
- O2 backOff→nearCorner: tangent, INTAKE.
- O3 nearCorner→startColumn: reverseTangent, INTAKE.
- O4 startColumn→shootPose: linear, SPIN_UP, then 1 s + 2 s shot.
- O5 shootPose→outerWall: tangent, IDLE.
- O6 outerWall→park: tangent, IDLE.

**Park** (`ParkAuto`): P1 (8.9568, 131.191, 90) → (9.5, 102.4463, 90), reverseTangent (drives backwards, no turn). Straight copy is identical.

**Shoot Turn Collect** (`ShootTurnCollectAuto`; @Disabled only in the uncommitted diff): start (56, 8.5177, -90).
- Spin up and feed.
- `driveForward(-5)` → (56, 13.5177, -90), constant.
- `turnBy(180)` → 90 (sign flipped for the mirrored alliance).
- `driveForward(24)` → (56, 37.5177, 90), constant, INTAKE.
- No park.

### 7.2 auto-shooter @d4642a0 (red-authored, mirror for blue)

- **Start Routine**: start (56, 8, 90); backOff (56, 15.1326, -95); nearCorner (11.4045, 9.4349, -172.7192); sideLane (35.6066, 114.61, -102.9589); topApproach (56.355, 129.0259, -145.2086); alignment (56.5326, 125.8006, -86.8487); park (9.1034, 117.5577, -170.1408).
  - S1 linear (vision-align: backOff heading 90, tangent).
  - S2 tangent.
  - S3 nearCorner→sideLane reverseTangent (vision-align starts at `currentPose()`).
  - S4 reverseTangent.
  - S5 tangent.
  - S6 alignment→park tangent (vision-align starts at `currentPose()`).
- **Center Lane (old)**: start (56, 8, 90); backOff (56, 18.4274, -95 / vision-align 180); nearCorner (10.2977, 10.0145, -169.5697); wallReturn (56.1694, 10.1307, 0.145); centerTop (63.3433, 130.0328, 86.576); alignment (62.9901, 126.783, -96.2026); park (9.9449, 116.9363, -169.4839).
  - Segments: linear, tangent, tangent, tangent (**crosses the HIVE**), tangent (177° spin), tangent.
- **Top Wall**: (8.9568, 131.191, 90) → (8.7737, 102.4463, 89.6351), reverseTangent. The vision-align version ends at -90.3649 with tangent, which is a 180° spin.
- **Shoot And Collect**: start (56, 8, -95).
  - turnTo (56, 8, 85).
  - Line → (58.092, 31.909, 85), tangent, INTAKE.
- **Outer Lane**: as in 7.1.

### 7.3 vision-align @a7b8f4c

- **Preload + Park (Odometry)** and **Preload + Collect + Park**. These are odometry legs (`driveTo` / `driveThrough`), not Pedro paths. Converted to the Pedro frame from the start (57, 9, -90), where x = 57 - right/2.54 and y = 9 + back/2.54:

| Pose | x | y | h | Used by |
|---|---|---|---|---|
| start | 57.000 | 9.000 | -90 | both |
| shootSpot (LEAVE 10 cm) | 57.000 | 12.937 | -90 | both |
| shootSpotFacingCollect | 57.000 | 12.937 | 180 | collect |
| collectStart (60 cm) | 33.378 | 12.937 | 180 | collect (through) |
| collectEnd (120 cm) | 9.756 | 12.937 | 180 | collect, INTAKE at power 0.2 |
| collectExit | 23.929 | 29.866 | 180 | collect, TIP path (through) |
| southWaypoint | 33.378 | 40.496 | -90 or 90 | collect (through) |
| northWaypoint | 33.378 | 110.181 | 90 | collect (through) |
| otherSpot | 57.000 | 125.157 | 90 | collect, other cell |
| backOff2 | 57.000 | 14.906 | -90 | park |
| right1 | 33.378 | 14.906 | -90 | park |
| longBack | 33.378 | 105.457 | -90 | park |
| right2 = PARK | 9.756 | 105.457 | -90 | both |
| otherPark | 9.756 | 105.457 | 90 | collect |

  - Park leg order: start→shootSpot, shoot 4, →backOff2, →right1, →longBack, →right2, hold.
  - Each leg is already a straight line, so the Pedro migration is `Paths.line(a, b).constant(b)`, or `.linear(a, b)` where the heading changes (collectExit→northWaypoint, southWaypoint with the 180° flip).
- **Odometry Square Test**: (56, 8, 90) → (56, 32, 90) → (32, 32, 90) → (32, 8, 90) → start, all constant.
- **Start Routine / Center Lane (old) / Outer Lane / Top Wall**: see 7.2. Center Lane's own Javadoc warns that it crosses the structure.
- **Safe Move (Encoders)**: back 40 cm, then left 40 cm. No Pedro.

### 7.4 renata/auto-test

R1: (9.5022, 133.181, 90) → (8.9022, 105.6139, -90), constant(-90).

---

## 8. Decisions

| Item (source) | Decision | Proposed English name | Reason |
|---|---|---|---|
| `PedroFollowerFactory` (vision-align d937b54f; beta 04feea2e) | **MERGE**: vision-align values + final DS names; constants split out | `PedroConstants` (Mecanum/Pinpoint/Foresight configs) + `PedroFollowerFactory` | Tuned offsets; configs public for the tuners |
| AutoTune set (`Tuning`, tuners, `Tests`) | **KEEP** | `pathing/tuning/PedroTuning` + tuners | Needed to tune Foresight |
| `Alliance` mirrorX (beta, auto-shooter) | **REPLACE** | `AllianceFrame` (identity poses; `toFieldAbsolute()` = rotate 180° for blue) | Mirror is wrong for a rotationally symmetric field (5.2) |
| `AlliancePoses` (vision-align) | **MERGE** into `AllianceFrame` | - | Correct idea |
| `PedroAutoBase` beta (882a98b2) + vision-align (b24b0247) | **REFACTOR / MERGE**: keep beta's small API (followPath / holdFor / modes / shootWhenReady); add vision-align guards (start-pose check, re-zero, odometry jump freeze, time guards, brake). Split the 53 KB file | `PedroAutoBase`, `AutoMechanisms`, `AutoSafety`, `MatchTimer` | Coding rule "no long files" |
| `Constants.java` (beta) | **REFACTOR** into per-route path classes + field geometry | `TickPaths`, `CenterLanePaths`, `FieldGeometry` | One place per route |
| `BiobuzzFieldMap` (duopodriveNcam) | **REFACTOR**: convert to the Pedro frame | `FieldGeometry` (LZ boxes, HIVE box, cells, flowers, territory line x = 72) | Needed for rule checks and aiming |
| PreloadRouteAuto (vision-align) | **KEEP** (primary competition auto), later migrate legs to Pedro lines | `AutoPreloadPark` + `PreloadPaths` | Field-tested 12-14 s |
| PreloadCollectAuto (vision-align) | **KEEP** (stretch auto) | `AutoPreloadCollectPark` | Tuned from logs, time-guarded |
| Outer Lane (beta, disabled) | **KEEP / REFACTOR**: re-enable, end park at y ≤ 117, add a time guard | `AutoOuterLane` + `OuterLanePaths` | Legal-safe multi-shot route |
| Tick (beta) | **REFACTOR**: re-route west of the HIVE (via x ≈ 33 like PreloadRoute) unless driving under the HIVE is proven legal and physically possible | `AutoTwoShotWestLane` (original kept as `TickPaths`) | Under-HIVE risk |
| Center Lane (beta) | **REFACTOR** (same as Tick) | `AutoCenterLane` | Under-HIVE risk |
| Park (beta) | **KEEP**: move end x to ≥ 11 for LEAVE margin | `AutoSafePark` | Simple, legal |
| LeaveParkAuto (auto-leave-park) | **KEEP** as the no-Pedro fallback | `AutoLeavePark` | Works without the localizer |
| SafeMoveAuto (vision-align) | **KEEP** as the encoder fallback | `AutoEncoderLeave` | No-odometry fallback |
| Shoot Turn Collect (beta) + Shoot And Collect (auto-shooter) | **MERGE** and add a park, or DISCARD | `AutoShootCollect` | No PARK; auto-shooter version hits the wall |
| Start Routine (auto-shooter / vision-align) | **DISCARD** (geometry superseded by Outer Lane / PreloadRoute); keep poses in the archive | - | FLOWER and wall risks |
| Center Lane old (auto-shooter / vision-align) | **DISCARD** | - | Crosses the HIVE and the centre line |
| Top Wall (all) | **DISCARD** (replaced by Park) | - | Wall contact, 180° spin |
| Renata Auto-test | **DISCARD** | - | Does not compile; Ivy not declared |
| OdometrySquareTest, PedroPoseCheck, PoseLogger, FieldDrawing, visualizer scripts | **KEEP** | `PedroSquareTest`, `PedroPoseCheck`, `PoseLogger`, `FieldDrawing` | Testing and debugging |
| `docs/autonomos-cancha.html` | **REFACTOR**: fix the alliance labels, draw territory, LZs, flowers and the HIVE box | `docs/auto-routes.html` | The diagram repeats the beta assumptions |
| theautonomous MatchClock / TimeModel | **MERGE** the concept only | `MatchTimer` | 30 s guard per phase |
| `PinpointOdometry` / `AutoDrive` (Mar) | Out of scope for Pedro. Do not run it alongside Pedro on the same Pinpoint in AUTO | - | Two owners of one device |
