# SOURCE SYNC

How HiveMind followed the team's source repository after it was built. HiveMind was built from
`ironlionrobotics/IronLion30311BIOBUZZInicioDeTemporada`, branch
`feature/turrubiates/vision-align` @ `a7b8f4c` (`VA`). On 2026-11-24 the team pushed new commits
to that branch and made it the new `master` (PR #15, `master` = `c98a668`, same tree as
`vision-align` @ `d93cec3`). This file records, for each new commit, whether it was **PORT**ed,
**PARTIAL**ly ported or **SKIP**ped, where it went and why.

- Sync date: **2026-11-25**, branch `feat/source-sync` (sync agent, hand-off
  `docs/agents/source-sync.md`). Source read-only, through a local clone.
- Range: `a7b8f4c..d93cec3` (`VA+` in the other docs). Refs recorded in
  `docs/snapshots/source-refs-2026-11-25.txt`.
- Tie-breakers (from `docs/BEST_OF_MATRIX.md`): matches the current robot, tested on the robot
  (per the commit message), compiles, follows the BIOBUZZ rules and the team decisions (G408 the
  intake stops for opponent NECTAR and never ejects; every park ends ≥ 2 in off the wall and
  partly in the LOADING ZONE; G304 starts; D21 FAR = the wall farthest from our LOADING ZONE;
  G402 = do not disrupt the opponent's AUTO, plus our territory guard).
- Every ported file names the commit in its `// Sources:` header (`@ <commit>`).

## 1. Commit table

All commits are by turrubiates (Héctor Turrubiates), 2026-11-24 (US Central time).

| Commit | Summary | Decision | Where it went in HiveMind | Why |
|---|---|---|---|---|
| `28ed859` | fix(auto): recover from Pinpoint resets (a jump to exactly (0, 0, 0) restores the last good pose, up to 3 times); the other-cell volley ends by 26 s | **PARTIAL** | `pathing/OdometryDriver` (`updatePose`, `isPinpointResetPose`, `getResetRecoveries`), `DriveConstants.PINPOINT_RESET_*`, `AutoBase` (logs each recovery), `OdometryDriverTest` | Recovery ported as is (it fixed a lost park in a logged run). The 26 s deadline was itself replaced two commits later by the time model of `1e723f2`, which is what HiveMind ported |
| `023ae35` | fix(teleop): ball assist only while driving forward, never while shooting; Pinpoint health (status, Hz, pod counts) in logs, INIT, START, summaries | **PORT** | `TeleOpLogic.shouldBallAssist` + `VisionConstants.BALL_ASSIST_ENABLED / MIN_FORWARD` (`TeleOpBase`), new `pathing/PinpointDiagnostics` (device kept in `RobotHardware.pinpoint`), `AutoBase` (INIT warning, fault at START → `OdometryDriver.lockOdometry`, end summary), `PoseLogger` `pinpoint_*` columns, `TestPedroPose` line; tests `TeleOpLogicTest`, `PinpointDiagnosticsTest`, `OdometryDriverTest` | Fixes a real TeleOp problem seen on the robot (assist turned the robot at the shooting spot). The VA `MatchSummary` columns are not ported (HiveMind has no `MatchSummary`; the step log gets one summary line) |
| `b6913d9` | feat(intake): jam protection (> 6 A for 0.4 s → reverse 0.4 s); motor log every 0.25 s | **PARTIAL** | `Intake` (`UNJAMMING`, `checkJam`, `getUnjamCount`), `IntakeConstants.JAM_*`, new `IntakeTest`; TeleOp match log column `intake_unjams` | Jam protection ported; **the G408 stop wins over an unjam** (an opponent NECTAR is never pushed out). `MotorDiagnosticsLogger` was never ported to HiveMind (BEST_OF_MATRIX §7); the TeleOp match log (`DataLogger`) already samples every 0.1 s and flushes every 1 s, which is what the 0.25 s change aimed for |
| `880cfaa` | docs: refresh `PROJECT_CONTEXT.md`, add `doc/architecture.md` (Mermaid graphs) | **PARTIAL** | Facts in `docs/ROBOT_PROFILE.md` §0.4 | The documents describe the source repo's own classes and macOS workflow; HiveMind has its own `ARCHITECTURE.md` / `CODE_GUIDE.md`. Robot facts were mined (mass, battery resistance, Pinpoint reboots, untested items) |
| `4fd2293` | feat(auto): collect along the south (audience) wall; Limelight aim at the other cell from an angled spot (~46.7, 118.6, 115°); park mid-zone | **PORT** (park superseded) | `AutoConstants.COLLECT_LINE_BACK_CM` / `COLLECT_START_CM` 71 / `OTHER_SPOT_*` / `AIM_*` / `DEFAULT_SHOOT_BEARING_DEGREES`, `NearRoutePoses` (`COLLECT_START`, `COLLECT_END`, `OTHER_SPOT`, `OTHER_HEADING_DEG` 115), `FieldConstants.OTHER_SHOOT`, `AutoBase.aimSteps` (turn + move along the launcher axis), `ScoringAuto` (bearing + distance measured at the first volley); tests `PathsTest`, `AutoSafetyTest` | Matches the manual's ball positions and the team's visualizer spot; replaces the distance-only "vision approach". Not tested on the robot yet (TODO: TUNE, check `AIM_TURN_SIGN`). The mid-zone park was replaced by `1e723f2` |
| `1e723f2` | feat(auto): time-budget decisions (fitted leg model 70 in/s per power + 0.75 s + 0.55 s/leg; park deadline 1.5 s before the end; volleys end when only the park time is left; faster park up to power 1.0 when late; other cell only if it fits); park at the zone's low edge (9.8, 95.4) | **PARTIAL** | `AutoPlanner` (`estimateRouteSeconds`, `getParkDeadlineSeconds`, `getParkPower`, `getVolleyTimeLeft`, `hasTimeForOtherCell`), `AutoConstants` (`ESTIMATE_*`, `PARK_MARGIN_SECONDS`, `PARK_MAX_POWER`, `OTHER_CELL_AIM_SECONDS`, `OTHER_CELL_MIN_SHOOT_SECONDS`, `PARK_BACK_CM` 219.5), `AutoBase.timedParkSteps`, `OdometryDriver.driveThrough(…, power)`, `ScoringAuto`; tests `AutoPlannerTest`, `CycleBudgetTest`, `PathsTest` | Ported, but: (1) PARK **y** moved to the zone's low edge (95.4) — the robot still overlaps the zone by ~8 in, so this is compatible with the team decision; PARK **x** stays 11.1 (2.1 in off the wall) instead of the source's 9.8 (0.8 in): **the team decision "≥ 2 in off the wall" wins** (LEAVE). (2) The other-cell park ends square to the wall (heading 90, not 115): the angled 18 in robot would touch the wall. `EmergencyPark` squares its last leg for the same reason. (3) The emergency time guard keeps its 4 s reserve as a backstop (open question). The fitted model replaces the untuned `CRUISE_INCHES_PER_SECOND` / `LEG_OVERHEAD_SECONDS`, and the fixed 15 s / 26.5 s other-cell limits |
| `e13e052` | docs(scripts): park label in `odometry_routes_to_visualizer.py` | **SKIP** | — | The script parses `PreloadRouteAuto.java` / `FarSideWaitTipAuto.java` constants by regex and writes Pedro visualizer files from them. HiveMind's poses live in `NearRoutePoses` / `AutoConstants` / `WaitTipPaths` with other names, so the script would not work. The same applies to the script parts of `4fd2293` and `f26f8d0`. A HiveMind version could read `PathCatalog` instead (future task) |
| `344254b` | feat(sweeper): FLOWER sweeper bar on two mirrored goBILDA Dual Mode servos (500–2500 µs, per-servo UP/DOWN, 0.5 s slowed moves, no move at INIT); `SweeperServoCalibration`; gamepad2 A toggles it | **PORT** | New `subsystems/FlowerSweeper`, `config/FlowerSweeperConstants`, `RobotConstants.SWEEPER_*` (legacy `flowerArm*`, `servoLeft/Right`), `RobotHardware.sweeperLeftServo/RightServo` (`ServoImplEx`, PWM range), `opmodes/tuning/TuneFlowerSweeper` (group Tuning), `TeleOpBase` (gamepad1 B), `FlowerSweeperTest`. **`FlowerArm`, `FlowerArmConstants`, `FlowerArmTest` removed** | The sweeper is the team's real FLOWER mechanism; the flower arm came from a branch that never ran on the robot (D14) and used the same button. Changes: gamepad1 **B** (gamepad2 A is HiveMind's auto-shoot); lowering is gated by the G410 unlock while `LOCK_DOWN_UNTIL_NECTAR_UNLOCK` is true (conservative, see RULES_COMPLIANCE §5 Q1); positions are still 0.5 / 0.5 placeholders (TODO: TUNE) |
| `f26f8d0` | feat(auto): far-side autonomous that waits for the partner's TIP (`FarSideWaitTipAuto`): back-wall start next to the FLOWER, LEAVE, pre-spin 2000, wait for our audience cell up and still, Limelight aim, shoot, park at the zone's top; aim code moved to the base | **PORT** (adapted) | New `opmodes/auto/WaitTipAuto`, `AutoRedWaitTip`, `AutoBlueWaitTip` ("Auto Red/Blue Wait Tip", Competition), `StartPosition.BACK_WALL`, `pathing/paths/WaitTipPaths` (+ straight copies, `PathCatalog`), `AutoBase.waitForStableCell` / `aimSteps`, `AutoConstants.WAIT_TIP_*`; test `StartPositionRulesTest.waitTipRouteKeepsTheRulesAndLeavesRoomForThePartner` | Checked against G304: the source start (59.36, 132.06) assumed a 141.5 in field and sat ~0.3 in from the FLOWER; HiveMind uses (61.5, 135.0, 90°): touches the 144 in back wall (D19), keeps the 4 in FLOWER keep-out, 1.5 in inside our half. PARK x 11.1 (≥ 2 in off the wall) instead of 9.5. The aim turn is limited to 9° here, because a robot at x 61.5 turned more than ~9.6° reaches past x 72 and the G402 guard would stop the drive. **Not called "Far":** it does not meet the D21 definition (open question). UNTESTED |
| `854de85` | feat(shooter): POLLEN idle spin (2000 RPM) between TeleOp volleys; from START, gamepad2 BACK toggles, off on low battery | **PORT** | `Launcher.setPollenIdle` / `isPollenIdleEnabled` (IDLE state, never ready, off in `stop()`), `LauncherConstants.POLLEN_IDLE_RPM` / `POLLEN_IDLE_AT_START`, `TeleOpBase` (gamepad2 **right stick button**, starts at START, off on low battery, telemetry, log column `pollen_idle`); `LauncherLogicTest` | Measured benefit (ready in 0.7 s vs 1.4–3.0 s). gamepad2 BACK is HiveMind's telemetry toggle, so the toggle moved to the right stick button |
| `19de992` | fix(shooter): feed only inside an asymmetric RPM window (−50 / +80); readiness still ends beyond 200 RPM | **PORT** | `Launcher.updateReady`, `LauncherConstants.READY_BELOW_RPM` / `READY_ABOVE_RPM` (replace `READY_TOLERANCE_RPM`); `LauncherLogicTest.feedWindowIsAsymmetric` | Fitted from 72 logged shots. The hysteresis stays coherent: ready only inside the window, not ready after a > 200 RPM dip, so each ball waits for the window again (TeleOp feed and `ShootingSequence` both use `isPollenReady`) |
| `48de874` | docs: project context, architecture graph and the Spanish research report "Mejoras de tiro y TeleOp FTC" (+ 4 notes) | **PARTIAL** | `docs/research/SHOOTING_AND_TELEOP_RESEARCH.md` (English summary, ranked actions, what is implemented), `docs/ROBOT_PROFILE.md` §0.4 | Report summarized in English (no Spanish text copied), credited to the source. Its first and fourth items are already ported (idle spin, asymmetric window) |
| `2c65853` | chore: merge `master` into `vision-align`, keeping the `vision-align` tree | **SKIP** | — | No content change (the merge kept the VA tree). The `master` commits it brings into the history (`37b7a16`, `206fa84`, … `feature/autonomos-beta` and the 11-14 branches) were already sources of HiveMind (`MASTER`, `BETA` in `docs/MIGRATION.md`) |
| `1110271` | Merge pull request #15: `vision-align` becomes `master` | **SKIP** | `docs/snapshots/source-refs-2026-11-25.txt` | Merge commit, no new content. `master` @ `c98a668` = this tree |
| `d93cec3` | docs: `master` holds this code; use `gh` for pull requests | **SKIP** | — | Workflow note for the source repo only |

## 2. Team decisions that overrode the source

| Source behavior | HiveMind | Rule / decision |
|---|---|---|
| PARK at x 9.8 (robot 0.8 in from the red wall), wait-TIP PARK at x 9.5 | x 11.1 (2.1 in off the wall) | Team decision: every park ≥ 2 in off the wall (LEAVE) |
| Other-cell park keeps the angled 115° heading | Square to the wall (90°); `EmergencyPark` squares its last leg | Same decision: an angled 18 in robot would touch the wall |
| Far-side start (59.36, 132.06) | (61.5, 135.0) | G304: touch the wall on a 144 in field (D19), clear the FLOWER |
| Aim turn up to 25° everywhere | 9° in the wait-TIP auto | G402 territory guard (corners must stay at x ≤ 72) |
| Sweeper usable any time | Lowering only after the G410 unlock (switch) | Conservative FLOWER policy (§10.5.2), Q&A pending |
| Intake unjam reverses whatever is in front | The G408 stop wins over an unjam | G408 team decision: never push opponent NECTAR |

## 3. Open questions

1. **D21 / FAR start.** The team's "far side" auto starts at the BACK wall (next to the FLOWER).
   Our `FAR_START` (8.96, 131.19) is on the red wall behind the LOADING ZONE. Neither is "the wall
   farthest from our LOADING ZONE" (that would be the audience wall, i.e. the NEAR wall). Which
   start does the team want for the second robot position?
2. **Audience cell at the start.** The wait-TIP auto assumes our audience cell (34–37) starts DOWN
   (it waits for it to come up). The manual figure says it starts UP (RULES_COMPLIANCE §5 Q2). If
   it is up, the auto shoots right away.
3. **Time guard reserve.** The new time model parks by 28.5 s, but the emergency guard still keeps
   `AUTO_PARK_RESERVE_SECONDS` = 4 s, so in practice volleys can be cut ~2.5 s earlier than in the
   source. Lower it after the park at power 1.0 has been tested?
4. **Sweeper lock.** Keep `LOCK_DOWN_UNTIL_NECTAR_UNLOCK` true until the Q&A answers?
5. **Visualizer export.** Rewrite `scripts/odometry_routes_to_visualizer.py` for HiveMind
   (from `PathCatalog`)?
