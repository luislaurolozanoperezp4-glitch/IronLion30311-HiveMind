# NAMING

The naming rules for IronLion30311-HiveMind. `CLAUDE.md` and `docs/ARCHITECTURE.md` point here.
Every example below is a real name from this repository.

**The three golden rules**

1. **English only.** Code, comments, telemetry, file names, branches and commits. CI checks for
   common Spanish words (`.github/scripts/check-code-rules.sh`, check 4).
2. **One concept, one name, everywhere.** The flywheels are the **Launcher** in code,
   telemetry and docs. Do not write "shooter" in one file and "launcher" in another. (The only
   exception is the Driver Station names, see section 8.)
3. **Say what it is, not when you wrote it.** No `v2`, `New`, `Old`, `Final`, `Copy`, `Backup`,
   `Test2` in file names. CI rejects them (check 3). Git keeps the history for you.

## 1. Folders and packages

| Rule | Examples |
|---|---|
| Java packages are all lowercase, one word, under `org.firstinspires.ftc.teamcode` (the SDK and Pedro AutoTune scan that root). | `config`, `hardware`, `subsystems`, `subsystems.vision`, `statemachine`, `pathing`, `pathing.paths`, `pathing.tuning`, `opmodes.auto`, `opmodes.teleop`, `opmodes.tuning`, `util` |
| A package is named after a **layer** or a **group of related classes**, never after a person or a date. | `opmodes.tuning`, not `opmodes.mar` |
| Every package has a `package-info.java` with a Javadoc that says what belongs there. | `subsystems/package-info.java` |
| Repository folders outside Java are lowercase. | `docs/`, `docs/agents/`, `docs/snapshots/`, `.github/scripts/` |

## 2. Classes, interfaces and enums

| Kind | Rule | Examples |
|---|---|---|
| Class | `UpperCamelCase` noun, named after what it is | `Drivetrain`, `Launcher`, `ColorSensing`, `OdometryDriver` |
| Subsystem | The mechanism's name, no `Subsystem` suffix | `Intake`, `Indexer`, `FlowerSweeper`, `Lights` |
| Interface | Noun or adjective, no `I` prefix | `Subsystem` |
| Constants class | `<Mechanism>Constants`, in `config/` | `LauncherConstants`, `IntakeConstants`, `AutoConstants` |
| State machine / sequence | `<Action>Sequence` or `<Thing>Watch` | `ShootingSequence`, `CollectSequence`, `TipWatch` |
| Route of paths | `<RouteName>Paths`, in `pathing/paths/` | `PreloadParkPaths`, `OtherCellPaths`, `FarCyclePaths` |
| Enum | Singular noun; values in `UPPER_SNAKE_CASE` | `BallColor.POLLEN`, `HiveCell.RED_BACK`, `StartPosition.NEAR` |
| Pure helper (no hardware) | Ends in `Logic`, `Planner`, `Guard`, `Selector` … | `TeleOpLogic`, `AutoPlanner`, `TerritoryGuard`, `AutoSelector` |
| Acronyms | Treat as a word: only the first letter is a capital | `PidfController`, not `PIDFController` |

## 3. Methods

| Rule | Examples |
|---|---|
| `lowerCamelCase`, starts with a verb | `collect()`, `spinUpPollen()`, `driveTo(...)`, `adjustPollenRpm(delta)` |
| A method that returns a `boolean` starts with `is`, `has`, `can` or `should` | `isPollenReady()`, `hasPinpoint()`, `canPlaceNectar()`, `isCellDown(...)` |
| Getters start with `get` | `getPollenRpm()`, `getRemainingSeconds()`, `getUpwardCell()` |
| Every subsystem has the same three lifecycle methods (from the `Subsystem` interface) | `periodic()` (once per loop), `stop()`, `addTelemetry(Telemetry t)` |
| Non-blocking sequences use the same shape | `start(...)`, `periodic()`, `isDone()`, `cancel()` |
| No gamepad objects inside subsystems. OpModes read buttons and call command methods | `intake.collect()`, not `intake.update(gamepad1)` |

## 4. Fields, parameters and local variables

| Rule | Examples |
|---|---|
| `lowerCamelCase` nouns | `drivetrain`, `launcher`, `shootDistanceMeters` |
| Booleans start with `is`, `has`, `can`, `should`, `was` | `isFieldCentric`, `hasTarget`, `isAligning`, `wasAligning`, `shouldPause` |
| Hardware fields match the Driver Station concept, plus the device type | `frontLeftDrive`, `intakeMotor`, `pollenLauncherMotor`, `indicatorLight`, `sweeperLeftServo` |
| Values with a unit end with the unit | `startDelaySeconds`, `shootDistanceMeters`, `goneSec` (short form allowed for parameters) |
| No one-letter names except loop counters and the standard `t` for `Telemetry` | `for (int i = 0; ...)`, `addTelemetry(Telemetry t)` |

## 5. Constants

| Rule | Examples |
|---|---|
| `UPPER_SNAKE_CASE`, in a `config/*Constants` class | `POLLEN_TELEOP_RPM`, `JAM_CURRENT_AMPS` |
| End with the unit, spelled out | `_SECONDS`, `_INCHES`, `_CM`, `_METERS`, `_DEGREES`, `_RPM`, `_AMPS`, `_VOLTS`, `_MILLIS`, `_PIXELS` → `SHAKE_REVERSE_SECONDS`, `WALL_CLEARANCE_INCHES`, `RIGHT_2_CM`, `CAMERA_PITCH_DEGREES` |
| Driver Station names are `UPPER_SNAKE_CASE` of the device, legacy lists end in `_LEGACY` | `INTAKE_MOTOR = "intakeMotor"`, `INDEXER_MOTOR_LEGACY = {"alejandro", "indexer"}` |
| Tunable values are `public static` (not `final`) so FTC Dashboard can change them live; the class has `@Config` | `public static double COLLECT_POWER = 0.75;` |
| Fixed facts (field size, ticks per revolution, rule limits) are `public static final` | `FIELD_SIZE_INCHES`, `TICKS_PER_REV`, `MAX_CONTROLLED_ELEMENTS` |
| A value not tested on the robot carries `// TODO: TUNE` | `CRUISE_INCHES_PER_SECOND` |
| A value's status is in its Javadoc: TESTED, FITTED, `TODO: TUNE`, or a decision number | `/** TESTED in the 4/4 preload run ... */` |

Some older field-coordinate constants have no unit suffix (`TERRITORY_LINE_X`, `HIVE_MIN_X`,
`RED_LOADING_ZONE_MIN_X`, `LOADING_ZONE_START_X`). They are all inches. New constants must use
the suffix; renaming these is a separate refactor (see `docs/agents/qa-ci.md` §6).

## 6. OpModes

| Item | Rule | Examples |
|---|---|---|
| Class name | `<Kind><Alliance><Variant>`: `Auto…`, `TeleOp…`, `Tune…` (changes a value), `Test…` (checks hardware) | `AutoRedNear`, `AutoBlueFar`, `TeleOpRed`, `TuneLauncherPidf`, `TestDriveWheels` |
| Driver Station name (`name =`) | The class name split into words, so students can find the class from the DS | `"Auto Red Near"`, `"TeleOp Blue"`, `"Tune Launcher PIDF"`, `"Test Drive Wheels"` |
| Group (`group =`) | Only `"Competition"`, `"Tuning"` or `"Dev"` (experiments). CI rejects anything else (check 5) | `@Autonomous(name = "Auto Red Near", group = "Competition", preselectTeleOp = "TeleOp Red")` |
| `preselectTeleOp` | Every alliance-fixed AUTO preselects the TeleOp of the same alliance, with the exact DS name | `preselectTeleOp = "TeleOp Blue"` |
| Shared logic | An abstract base class ends in `Base` | `AutoBase`, `TeleOpBase`, `ScoringAuto` (abstract, shared by the 4 scoring autos) |
| Annotation | `@TeleOp` / `@Autonomous` from the SDK. Do not leave `@Disabled` OpModes in `Competition` | — |

## 7. Tests

| Rule | Examples |
|---|---|
| Test class = class under test + `Test`, in the same package under `src/test/java` | `LauncherLogicTest`, `IndexerTest`, `MatchTimerTest` |
| A test of a rule or a whole feature may be named after it | `StartPositionRulesTest`, `AutoSafetyTest`, `CycleBudgetTest`, `PathsTest` |
| Test methods are `lowerCamelCase` sentences that say what must be true | `rejectsFifthElement`, `stopsIntakeForOpponentNectar`, `bluePathsAreRedRotated180` |
| Shared test helpers live in `testutil/` | `FakeClock`, `RuleGeometry` |

## 8. Driver Station (hardware configuration) names

| Rule | Examples |
|---|---|
| `lowerCamelCase`: position + function + device type | `frontLeftDrive`, `intakeMotor`, `indicatorLight`, `sweeperLeftServo` |
| Product name for a unique coprocessor or sensor | `limelight`, `huskyLens`, `pinpoint` |
| No spaces, no nicknames, no single letters | not `imu 1`, not `alejandro`, not `P` |
| Every name is written **once**, in `config/RobotConstants.java`. The README table copies it | `RobotConstants.POLLEN_LAUNCHER_MOTOR` |

Known exception: the launcher motors are still named `pollenShooterMotor` and
`nectarShooterMotor` on the Driver Station, because renaming them means changing every Control
Hub configuration (decision D15). In code they are `POLLEN_LAUNCHER_MOTOR` /
`pollenLauncherMotor`. The old → new table is in `docs/MIGRATION.md`.

## 9. Docs and other files

| Rule | Examples |
|---|---|
| Top-level docs in `docs/` are `UPPER_SNAKE_CASE.md` | `ROBOT_PROFILE.md`, `CODE_GUIDE.md`, `MATCH_CHECKLIST.md` |
| Agent hand-off notes are `docs/agents/<agent-name>.md`, lowercase kebab-case | `docs/agents/qa-ci.md`, `docs/agents/auto-opmodes.md` |
| Scripts are lowercase kebab-case | `.github/scripts/check-code-rules.sh` |
| Every Java file starts with a `// Sources:` line (CI check 2) | `// Sources: <repo> / branch <b> / <path> @ <commit> — adapted for BIOBUZZ`, or `// Sources: new for IronLion30311-HiveMind (...)` |

## 10. Branches

Format: `<type>/<short-description>`, lowercase kebab-case, 2–4 words, at most 30 characters.
An issue number is optional (`feat/12-launcher-pidf`). Never a person's name.

Types: `feat`, `fix`, `auto`, `path`, `tune`, `refactor`, `test`, `docs`, `ci`, `chore`,
`hotfix`, `agent`. The permanent branches are `main` and `dev`.

CI (`branch-name` job in `.github/workflows/build.yml`) checks pull-request branches with:

```
^(main|dev|(feat|fix|auto|path|tune|refactor|test|docs|ci|chore|hotfix|agent)/[a-z0-9]+(-[a-z0-9]+)*)$
```

plus a length limit of 30 characters. Good: `feat/auto-shoot`, `fix/intake-jam`, `agent/docs`.
Bad: `Feature/AutoShoot`, `luis-test`, `fix/intake_jam`. Details: `docs/BRANCHING.md`.

## 11. Tags

Events are tags on `main`, never branches: `v<major>.<minor>[.<patch>]-<event>`.

| Example | Meaning |
|---|---|
| `v1.0-qual-1` | Code used at Qualifier 1 |
| `v1.0.1-qual-1` | A hotfix made at Qualifier 1 |
| `v1.1-qual-2` | Code used at Qualifier 2 |
| `v2.0-state` | Code used at the state/regional championship |

## 12. Commits

`<type>: <what changed, in the present tense>`, using the same types as branches. Keep the first
line under about 72 characters; add a body to explain *why*.

```
feat: add auto-shoot volley on gamepad2 A
fix(rules): enforce G407/G408/G410/G402 per manual TU10
tune: launcher P 30 / F 15 from the 2026-11-20 logs
docs: add match checklist
```

An optional scope in parentheses names the area: `fix(auto): ...`, `docs(rules): ...`.
