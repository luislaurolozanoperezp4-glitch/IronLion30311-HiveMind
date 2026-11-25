# Hand-off: Source Sync Agent (`feat/source-sync`)

Branch `feat/source-sync` (from `origin/dev` @ `4362853`). Synced HiveMind with the team's
source repo `ironlionrobotics/IronLion30311BIOBUZZInicioDeTemporada`, `feature/turrubiates/
vision-align` `a7b8f4c..d93cec3` (= `master` `c98a668`). The source was read only, through the
local clone (`refs/legacy/*` fetched read-only into this worktree; nothing pushed there).

Build: `./gradlew --no-daemon assembleDebug testDebugUnitTest` → BUILD SUCCESSFUL, **241 tests**
(was 216), 0 failures. `bash .github/scripts/check-code-rules.sh` → OK.

## Decisions (details and reasons: `docs/SOURCE_SYNC.md`)

| Commit | Decision | Main HiveMind change |
|---|---|---|
| `28ed859` | PARTIAL | Pinpoint reboot recovery in `OdometryDriver` (26 s deadline superseded by `1e723f2`) |
| `023ae35` | PORT | Forward-only ball assist (`TeleOpLogic.shouldBallAssist`); `PinpointDiagnostics` (INIT warning, fault at START locks the drive, pose-log columns) |
| `b6913d9` | PARTIAL | Intake jam protection (`UNJAMMING`, G408 stop wins); motor logger not in HiveMind |
| `880cfaa` | PARTIAL | Facts → `ROBOT_PROFILE.md` §0.4 |
| `4fd2293` | PORT | Collect along the audience wall; angled other-cell spot (115°); Limelight aim (`AutoBase.aimSteps`) |
| `1e723f2` | PARTIAL | Fitted time model, park deadline/power, volley time, other-cell check (`AutoPlanner`); PARK at the zone's low edge but kept 2.1 in off the wall |
| `e13e052` | SKIP | Visualizer script parses source-repo Java files |
| `344254b` | PORT | `FlowerSweeper` + `TuneFlowerSweeper`; `FlowerArm` removed; gamepad1 B |
| `f26f8d0` | PORT (adapted) | `Auto Red/Blue Wait Tip`, `StartPosition.BACK_WALL`, `WaitTipPaths` |
| `854de85` | PORT | POLLEN idle spin (gamepad2 right stick button) |
| `19de992` | PORT | Feed window −50 / +80 RPM |
| `48de874` | PARTIAL | `docs/research/SHOOTING_AND_TELEOP_RESEARCH.md` |
| `2c65853`, `1110271`, `d93cec3` | SKIP | Merges / workflow note, no new content |

## Commits on this branch

1. `feat(launcher)`: asymmetric feed window and POLLEN idle spin
2. `feat(intake)`: jam protection on the motor current
3. `feat(teleop)`: forward-only ball assist; POLLEN idle toggle; log columns
4. `feat(odometry)`: Pinpoint reboot recovery and health logging
5. `feat(sweeper)`: FLOWER sweeper replaces the flower arm
6. `feat(auto)`: south-wall collect, Limelight aim, park time budget
7. `feat(auto)`: wait-TIP autos from the back wall
8. `docs`: README, MIGRATION, CODE_GUIDE, ARCHITECTURE, NAMING, RULES_COMPLIANCE, ROBOT_PROFILE,
   STRATEGY, MATCH_CHECKLIST, teleop hand-off, research summary, refs snapshot
9. `docs`: SOURCE_SYNC and this hand-off

## Needs a robot test (all UNTESTED in HiveMind)

1. Near park at y 95.4 (long leg 204.5 cm) still partly in the zone, 2.1 in off the wall.
2. Collect along the audience wall (side ~1 cm off it); intake turns on at x ~29 in.
3. Other cell: angled spot (46.8, 118.4, 115°), aim turn sign (`AIM_TURN_SIGN`), aim distance.
4. Park at power up to 1.0 when late (`PARK_MAX_POWER`); "Park plan" lines in the step log.
5. Feed window −50 / +80 (expect ~0.1 s more per shot); POLLEN idle spin current.
6. Intake jam reverse; Pinpoint recovery (look for `PINPOINT RESET recovered` and `pinpoint_*`).
7. FLOWER sweeper: calibrate with **Tune Flower Sweeper**, copy into `FlowerSweeperConstants`.
8. Auto Wait Tip from the back wall with a partner.

## Open questions for the team

See `docs/SOURCE_SYNC.md` §3: D21 FAR start vs the back-wall wait-TIP start; audience cell
up or down at the start; the 4 s emergency park reserve vs the new 1.5 s park margin; the
sweeper lock before 1:00; a HiveMind visualizer export.
