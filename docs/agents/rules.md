# Hand-off: Rules Compliance Agent (`agent/rules`)

Scope: check the code against the official BIOBUZZ Competition Manual **TU10** (the latest on
2026-11-25) and enforce G402 / G407 / G408 / G410 / G304 in software, plus the team's four
decisions. The full report is in `docs/RULES_COMPLIANCE.md`.

Build: `./gradlew assembleDebug testDebugUnitTest` → BUILD SUCCESSFUL, 152 tests, 0 failures.

## What changed (behavior)

| Area | Change | Robot re-test? |
|---|---|---|
| G408 | `Intake` no longer ejects opponent NECTAR. It **stops** (state `OPPONENT_NECTAR`) while the opponent's NECTAR is at the intake, plus 0.3 s after (`IntakeConstants.OPPONENT_NECTAR_HOLD_SECONDS`), then collects again. TeleOp: short rumble on gamepad1 and a telemetry banner. API rename: `setRejectDetector` → `setOpponentNectarDetector`, `getRejectCount` → `getOpponentNectarStopCount`, `REJECT_OPPONENT_NECTAR` → `STOP_FOR_OPPONENT_NECTAR`, `REJECT_EJECT_SECONDS` removed. | Yes (HuskyLens zone) |
| G410 | `MatchTimer.canPlaceNectar()` unlocks at 59 s left, not 60 s (`MatchConstants.NECTAR_UNLOCK_MARGIN_SECONDS` = 1.0, TODO: TUNE), because the robot clock starts at START, not with the FIELD timer. The ENDGAME rumble still fires at 60 s. | No |
| G407 | AUTO saves its element count (`PoseStorage.saveElementCount`). TeleOp starts from it, or from the 4-ball preload if no AUTO ran (`TeleOpLogic.initialElementCount`). Before, TeleOp always started at 0. | No |
| G402 | `TerritoryGuard` uses the heading-aware footprint (`getHalfReachX`; 9 in square, 12.7 in at 45°). No competition pose trips it. | No |
| G304.E | The `StartPosition.LOADING_ZONE` start was **inside** the LOADING ZONE (illegal). It is now just behind the zone tape (`LOADING_ZONE_START_Y` 108 → 129.5), and the DS label reads "Beside loading zone". Safe Park: LEAVE back 10 cm, then slide to the zone center (`AutoSafePark.loadingZoneParkPose`). **Safe Park Encoders now scores LEAVE only.** | Yes (measure the start) |
| Parks (team decision 2) | Every competition park ends ≥ 2 in off the wall. Near route `AutoConstants.RIGHT_2_CM` 60 → **56.5** (PARK x 9.756 → 11.134, **~1.4 in change from the TESTED end**). `SafeParkPaths.PARK` x 9.5 → 12.0. `FieldConstants.RED_PARK` x → 11. | **Yes** |
| D20 rename (decision 3) | `HiveCell.RED_SCORING` / `BLUE_SCORING` → `RED_BACK` / `BLUE_BACK`; `getScoringCell` → `getBackCell`; `FieldConstants.RED_NORTH_CELL` / `RED_SOUTH_CELL` → `RED_BACK_CELL` / `RED_AUDIENCE_CELL`. Display names "Red Back (30-33)". Behavior identical. | No |
| D21 (decision 4) | `FieldConstants` Javadoc documents the G304 start rules and the team's FAR definition. Added FLOWER centers and `FLOWER_KEEP_OUT_RADIUS_INCHES` (TODO: TUNE). | No |

## New tests

- `opmodes/auto/StartPositionRulesTest` (G304 A/C/D/E for every `StartPosition`, BLUE included; R102 size; Safe Park beside-zone LEAVE + PARK).
- `testutil/RuleGeometry` (footprint, wall gap, LOADING ZONE overlap, FLOWER keep-out).
- `AutoSafetyTest.competitionParkEndsLeaveAndPark`, `territoryGuardCountsTheCornersOfATurnedRobot`.
- `MatchTimerTest.g410UnlockEdges`, `autoNeverAllowsNectar`, `matchTimingMatchesTheManual`.
- `ColorSensingTest.stopsIntakeForOpponentNectar`, `ownNectarDoesNotStopTheIntake`.
- `TeleOpLogicTest.teleOpElementCountComesFromAuto`, `fifthElementAfterAutoIsRefused`.
- `FieldConstantsTest.parkIsPartlyInRedLoadingZoneAndOffTheWall`, `loadingZoneMatchesTheManual`.
- `PathsTest.parkPathsEndInTheLoadingZone` now checks footprint overlap, plus ≥ 2 in wall clearance for COMPETITION park paths.

## Open items (details in `docs/RULES_COMPLIANCE.md` §5)

1. The manual's setup has the **red AUDIENCE cell (34–37) up at the start**, but the tested routine treats the BACK cell as its first target. Check on a correctly set-up field.
2. `FAR_START` sits right behind our LOADING ZONE, which does not match "the wall farthest from our LOADING ZONE". Confirm the pose.
3. Safe Park Encoders PARK would need an encoder strafe.
4. Is POLLEN into a FLOWER before 1:00 allowed? Ask the Q&A.
5. Check for TU11 (Thursday 2026-11-26).
