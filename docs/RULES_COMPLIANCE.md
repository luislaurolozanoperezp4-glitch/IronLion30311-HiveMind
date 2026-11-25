# Rules compliance — BIOBUZZ (FTC 2026–27)

Written by the Rules Compliance Agent on 2026-11-25 (branch `agent/rules`).

**Source:** the official *2026-2027 FIRST Tech Challenge Competition Manual, BIOBUZZ presented by
RTX, Version TU10*, downloaded from <https://ftc-resources.firstinspires.org/ftc/game/manual>.
The Game & Season Materials page (<https://ftc-resources.firstinspires.org/ftc/game>) lists
**Team Update 10 (2026-11-19)** as the latest (TU00–TU10). The manual PDF already includes all of
them. Team Updates come out on Thursdays, so check for a TU11 after 2026-11-26.

In this file, "§" means a manual section, and Gxxx / Rxxx are rule numbers. Rule text is
**paraphrased**: the manual itself is the authority. Q&A answers were not checked (the Q&A
needs a Lead Coach login).

## Status legend

| Status | Meaning |
|---|---|
| ENFORCED | Software prevents the violation (within sensor limits), and a JUnit test covers it |
| PARTIAL | Software helps, but it depends on a sensor estimate or a driver action |
| DRIVER RESPONSIBILITY | Software cannot enforce it; the drive team must |
| VERIFIED | A fact or number from the manual that the code uses; constants match it |
| UNVERIFIED | Not confirmed against the manual |

## 1. Rule table

| Rule | Manual (TU10), paraphrased | How the code enforces it | Test | Status |
|---|---|---|---|---|
| Match timing §10.1, §10.4 | 30 s AUTO, 8 s transition, then a 2:00 TELEOP. Glossary: AUTO = first 30 s. | `MatchConstants.AUTO_SECONDS` 30, `TRANSITION_SECONDS` 8 (new), `TELEOP_SECONDS` 120; `MatchTimer` | `MatchTimerTest.matchTimingMatchesTheManual` | VERIFIED |
| G403 | No powered movement during the AUTO→TELEOP transition. If INIT moves actuators, press INIT only after TELEOP starts. | AUTO `stop()` stops every subsystem. TeleOp INIT sets the flower-arm servos to rest (`FlowerArm` constructor), so they could twitch if they are not already there. | — | DRIVER RESPONSIBILITY |
| G404 | No powered movement after TELEOP ends. | Not enforced: the robot clock starts at the driver's START press, so it cannot know the real end. Press STOP at 0:00. | — | DRIVER RESPONSIBILITY |
| **G402** | During AUTO, do not disrupt the opposing alliance's AUTO. Red side = columns A–C, blue = D–F. Examples: controlling SCORING ELEMENTS on their side, or contacting their robot on their side. Crossing over is "risky" and may be seen as STRATEGIC. MAJOR FOUL per match (+ YELLOW CARD if STRATEGIC). | Conservative policy: never enter the opponent half. `TerritoryGuard.isPastLine` now uses the **heading-aware** footprint: `getHalfReachX(h) = L/2·|cos h| + W/2·|sin h|`, up to 12.7 in at 45°. Before, it used a fixed 9 in. `AutoBase.checkTerritory` stops the drive for the rest of AUTO. `FieldConstants.TERRITORY_LINE_X = 72` = 3 tiles. | `AutoSafetyTest.territoryGuard*` (incl. new `territoryGuardCountsTheCornersOfATurnedRobot`), `FieldConstantsTest.loadingZoneMatchesTheManual`, `PathsTest` | ENFORCED (relies on odometry) |
| **G407** | A robot may not CONTROL more than 4 SCORING ELEMENTS at once. VERBAL WARNING; MAJOR FOUL + YELLOW CARD if STRATEGIC. REPEATED 5+ is likely STRATEGIC, even with a "keep 4" sorter. Launched, bulldozed and deflected elements are not CONTROL. | `ElementCounter` (limit `MAX_CONTROLLED_ELEMENTS` = 4) gates `Intake.collect()` (state `FULL`). AUTO starts at the preload (4). **Fixed gap:** TeleOp used to restart the count at 0 while the robot could still hold up to 4 elements from AUTO. Now `AutoBase.stop` saves the count (`PoseStorage.saveElementCount`), and `TeleOpBase.start` loads it (`TeleOpLogic.initialElementCount`; falls back to the 4-ball preload when no AUTO ran). Driver override: gamepad1 X resets the count. | `ElementCounterTest.rejectsFifthElement`, `intakeRefusesToCollectWhenFull`, `TeleOpLogicTest.teleOpElementCountComesFromAuto`, `fifthElementAfterAutoIsRefused` | PARTIAL (the count is a HuskyLens + shot estimate; without a HuskyLens the gate only closes from the hand-off or preload count) |
| **G408** | A robot may not CONTROL the opponent's NECTAR. VERBAL WARNING; YELLOW CARD if STRATEGIC. REPEATED control is likely STRATEGIC *even with a sorter that discards it*. | **Changed (team decision 1):** when `ColorSensing.isOpponentNectarAtIntake` is true, `Intake` goes to state `OPPONENT_NECTAR`. It stops: it neither pulls the ball in nor pushes it out. It stays stopped `OPPONENT_NECTAR_HOLD_SECONDS` (0.3 s, TODO: TUNE) after the ball leaves the view, then collects again. TeleOp gives a short rumble on gamepad1 and a telemetry banner; the NECTAR alert light blinks in the NECTAR's color. Ball assist never turns toward opponent NECTAR (`TeleOpLogic.assistColors`). The old eject (`REJECTING`) was removed because pushing the ball away is itself CONTROL (herding), would be G405 if it left the field, and G402 in AUTO. | `ColorSensingTest.stopsIntakeForOpponentNectar`, `ownNectarDoesNotStopTheIntake`, `TeleOpLogicTest.assistNeverTargetsOpponentNectar` | PARTIAL (needs the HuskyLens; the driver must drive away) |
| **G410** | Robots may not enter NECTAR into the FLOWER scoring volume until the last 60 s of the MATCH. The primary FIELD timer is the cue. MAJOR FOUL **per NECTAR**. | `FlowerArm` deploy gate = `MatchTimer.canPlaceNectar()`. True only in TELEOP, with time left at or below `NECTAR_UNLOCK_SECONDS − NECTAR_UNLOCK_MARGIN_SECONDS` (60 − 1 s) and above 0. **New margin:** the robot clock starts at the driver's START press, not with the FIELD timer, so pressing START up to 1 s early still cannot unlock early. AUTO never unlocks. The banner says "allowed" only when the gate is open. | `MatchTimerTest.g410UnlockEdges`, `autoNeverAllowsNectar`, `blocksNectarBeforeLastMinute`, `FlowerArmTest.deployIsRefusedBeforeTheLastMinute` | ENFORCED for the flower arm; DRIVER RESPONSIBILITY for any other way NECTAR could reach a FLOWER (for example a launched NECTAR) |
| G304 start | The robot must be (A) fully on its own half (columns A–C red), (B) not attached to field elements, (C) touching the perimeter wall, (D) not touching a FLOWER or its scoring volume, (E) **not in the LOADING ZONE**, (F) in its STARTING CONFIGURATION, (G) touching exactly 4 preloaded POLLEN, and (H) motionless after INIT. | **Fixed violation:** `StartPosition.LOADING_ZONE` (Safe Park and Safe Park Encoders) started *inside* the LOADING ZONE, which G304.E forbids. It now starts just behind the zone tape (`LOADING_ZONE_START_Y` 108 → 129.5, robot y 120.5–138.5 vs zone ≤ 119.5). FLOWER centers were added to `FieldConstants`. | `StartPositionRulesTest.everyStartPoseIsLegal`, `blueStartsAreLegalForBlue` | ENFORCED for A, C, D, E (geometry); F, G, H are DRIVER RESPONSIBILITY |
| LEAVE §10.5.4 | 3 points (AUTO only), assessed at the end of AUTO: the robot is no longer touching the perimeter wall. | **Team decision 2:** every competition park ends ≥ `WALL_CLEARANCE_INCHES` (2 in) off every wall. Changes: near PARK `RIGHT_2_CM` 60 → 56.5 (x 9.756 → 11.134); Safe Park far `PARK` x 9.5 → 12.0 (the path ends ~6° off 90°); `RED_PARK` x 7.5 → 11. The Safe Park beside-zone route: back 10 cm, then slide to the zone center. | `AutoSafetyTest.competitionParkEndsLeaveAndPark`, `PathsTest.parkPathsEndInTheLoadingZone`, `StartPositionRulesTest.safeParkFromBesideTheZoneLeavesAndParks` | ENFORCED (geometry; re-test on the robot) |
| PARK §10.5.4 | 5 points in AUTO and 5 points at the end of the match: at least *partly* in the alliance's LOADING ZONE. The zone is 23 in × 11 in deep against the perimeter, tape included (§9.3). | Park tests check that the robot footprint overlaps the zone (`RuleGeometry.overlapsRedLoadingZone`). Zone constants: x 0–11, y 96.5–119.5. **Safe Park Encoders now scores LEAVE only:** it can no longer start in the zone, and an encoder-only straight move cannot reach the zone. | same as LEAVE, plus `FieldConstantsTest.parkIsPartlyInRedLoadingZoneAndOffTheWall` | ENFORCED |
| R102 / R105 | Start inside 18 × 18 × 18 in. After the start, robots may expand to at most 18 × 24 × 29 in tall, and this limit must be **mechanical** (software limits do not count). No weight limit (R104). | `RobotConstants` 18 × 18 in. Expansion is not a software topic. | `StartPositionRulesTest.robotFitsTheStartingConfiguration` | DRIVER RESPONSIBILITY (inspection) |
| G417 (HIVE) | Robots may not change the HIVE's motion except by LAUNCHING into an up-facing CELL. Ramming the frame, contacting the HIVE (also through a controlled element), and shooting at the cell's outside are examples. Accidental bumps are not STRATEGIC. **No rule forbids driving under the HIVE.** | Auto routes and the emergency park keep the 12.7 in robot circle clear of the HIVE frame box (x 47.3–96.7, y 52.5–91.5, matching §9.6.1 49.46 × 38.95 in). | `AutoSafetyTest.emergencyParkNeverTouchesTheHiveOrCrossesTheLine`, `PathsTest` HIVE flags | ENFORCED in AUTO; DRIVER RESPONSIBILITY in TELEOP |
| G409 | Do not catch or deflect elements released by a TIPPED HIVE before they touch something else. Waiting under the HIVE is likely STRATEGIC. | Nothing (no mechanism catches from above). | — | DRIVER RESPONSIBILITY |
| G405 | Do not deliberately eject SCORING ELEMENTS from the field. MAJOR FOUL per element. | The G408 change removes the automatic eject of opponent NECTAR. | `ColorSensingTest.stopsIntakeForOpponentNectar` (never runs outward) | PARTIAL |
| G418 | FLOWERS: enter POLLEN/NECTAR only through the top; remove only POLLEN, only from the bottom. | Flower arm only deploys (G410 gate); there is no removal mechanism. | — | DRIVER RESPONSIBILITY |
| G305 / §10.3.3 | Select and INIT an OpMode; an AUTO OpMode must have the 30 s timer enabled. | Competition autos are `@Autonomous`; the 30 s timer is a Driver Station setting. | — | DRIVER RESPONSIBILITY |
| G426 / G427 | Humans enter NECTAR only through the LOADING ZONE: 1 per own HIVE TIP, or all of it once ≤ 60 s remain. | `Vision.getTipCount()` is shown on telemetry, so the human player can count TIPs. | — | DRIVER RESPONSIBILITY |
| Scoring §10.5.5 | See section 2. | No software constant (strategy only). | — | VERIFIED |
| Field geometry (D13) | §9.2–9.5, Fig. 9-2/9-5/10-2: 144 in field, 6 × 6 tiles, columns A–F left→right from the audience, rows 1–6. Red ALLIANCE AREA on the **left** from the audience (column A wall), blue on the right. Red LOADING ZONE on the red wall, back half (y ≈ 96–119). Blue LOADING ZONE on the blue wall, audience half. Red GARDEN: audience wall, left corner. Blue GARDEN: back wall, right corner. FLOWERS: audience wall (x≈96), back wall (x≈48), red wall (y≈48), blue wall (y≈96). Each of these is the red one rotated 180° about the center ⇒ **180° rotational symmetry, not a mirror.** | `Alliance.toField` = `rotate180`; origin = red/audience corner (A1). | `PathsTest.bluePathsAreRedRotated180`, `StartPositionRulesTest.blueStartsAreLegalForBlue` | VERIFIED (D13 confirmed) |
| AprilTags (D20) | §9.9: 30–33 red CELL opposite the audience, 34–37 red audience CELL, 38–41 blue audience CELL, 42–45 blue CELL opposite the audience. | `HiveCell` renamed to `RED_BACK` / `RED_AUDIENCE` / `BLUE_AUDIENCE` / `BLUE_BACK`; `FieldConstants.RED_BACK_CELL` / `RED_AUDIENCE_CELL`. Behavior unchanged. | existing vision tests | VERIFIED |

## 2. Verified game facts (TU10)

- **Elements (§9.8, §10.3.1):** 40 POLLEN (yellow, 2.8 in); 8 red + 8 blue NECTAR (3.6 in).
  - POLLEN setup: 4 in each FLOWER, 4 in each GARDEN, 4 preloaded per robot.
  - NECTAR setup: 3 in each alliance's up-facing CELL, 5 in each ALLIANCE AREA.
- **Starting HIVE state (§10.3.1, Fig. 10-2):** the CELL that "points at" a FLOWER starts down. So at
  the start the **red AUDIENCE cell (34–37) is up** and the **blue BACK cell (42–45) is up**.
- **Points (Table 10-2):**

  | Achievement | AUTO | TELEOP / end |
  |---|---|---|
  | LEAVE | 3 | – |
  | PARK | 5 | 5 |
  | HIVE TIP | 20 | 20 |
  | POLLEN/NECTAR left in an up-facing CELL at match end | – | 2 each |
  | Bottom NECTAR Bonus (per FLOWER, alliance with the bottom-most own NECTAR) | – | 5 |
  | POLLEN/NECTAR in an owned FLOWER (owner = top-most own NECTAR; counts everyone's elements) | – | 2 each |
  | POLLEN/NECTAR in GARDEN (alliance of the GARDEN's color, whoever placed it) | – | 1 each |

  The GARDEN is not a protected zone (TU09).
- **Ranking points (Table 10-3):** WIN 3, TIE 1.
  - SWARM: LEAVE + PARK points ≥ **16**.
  - POLLINATOR 1: ≥ **4** TIPS. POLLINATOR 2: ≥ **7** TIPS.
  - These thresholds apply at Regional Championships and "all other events". The FIRST Championship
    thresholds are TBA, and Premier Events may set their own.
- **Fouls (Table 10-4):** MINOR FOUL = 5 points to the opponent, MAJOR FOUL = 20.
- **Audio (Table 9-1):** a "Chime" at 1:00 marks *FLOWER Ownership Unlocked*. The FIELD timer is
  the official cue, not the audio.

## 3. Mismatches with the original prompt's rules summary

| Prompt claim | Manual TU10 | Action |
|---|---|---|
| "G402: stay on own half in AUTO" | G402 forbids **disrupting** the opponent's AUTO. Being on their half is not itself a foul, but it is "risky" and may be STRATEGIC. | Kept the stricter own-half guard. It is a team policy, documented in `TerritoryGuard`. |
| "G408: never control opponent NECTAR" | Correct, but the penalty is a VERBAL WARNING (a YELLOW CARD if STRATEGIC), not points. A sorter that auto-discards does **not** make repeated control legal. | Intake now stops instead of ejecting (team decision 1). |
| "G410: last 60 s for NECTAR" (FLOWERS) | G410 covers **NECTAR only**. §10.5.2 adds that FLOWER scoring "cannot begin" before 1:00, and that anything scored earlier still counts but is "subject to penalties". | Flower arm is gated whatever it carries (conservative). See question 1. |
| "CELL 2 pts" | 2 points per element left in an **up-facing** CELL, assessed after everything comes to rest at match end; TELEOP column only. | None |
| "Ownership/bonus" | Owned FLOWER: 2 points per POLLEN **and** NECTAR inside, whoever placed them. Bottom NECTAR Bonus: 5 points. | None |
| "PARK 5+5" | Correct: AUTO PARK at the end of AUTO, TELEOP PARK at the end of the match. Partly inside the zone is enough. | None |
| Starting positions | G304 (A–H above). **Starting in the LOADING ZONE is illegal** (G304.E); the code had such a start. | Fixed (start moved behind the tape). |
| "Driving under the HIVE (G417?)" | No rule forbids driving under it. G417 forbids changing the HIVE's motion; G409 forbids catching from a TIPPED HIVE. | None (autos already avoid the HIVE box) |
| Field symmetry D13 | 180° rotation confirmed; red ALLIANCE AREA on the left from the audience. | Comments updated; tests already rotate. |
| `MatchConstants` values | 30 / 8 / 120 / 60 / 4 all match. | Added `TRANSITION_SECONDS`, `NECTAR_UNLOCK_MARGIN_SECONDS`. No tested value changed for a rule reason. |

## 4. Team decisions (2026-11-25)

1. **G408:** do not push or eject opponent NECTAR. The intake stops, alerts the driver and resumes
   when the ball is gone. Implemented in `Intake` (`OPPONENT_NECTAR` state,
   `setOpponentNectarDetector`), `IntakeConstants.STOP_FOR_OPPONENT_NECTAR` /
   `OPPONENT_NECTAR_HOLD_SECONDS`, and the TeleOp rumble and banner.
2. **Park end points:** every competition park ends ≥ 2 in off the wall and partly in the LOADING
   ZONE. This **changes the tested near PARK by ~1.4 in** (`RIGHT_2_CM` 60 → 56.5) and the Safe
   Park far end by 2.5 in. **Robot re-test needed.**
3. **D20:** the manual's tag layout is confirmed, and the names now follow it (`RED_BACK`,
   `RED_AUDIENCE`, `BLUE_AUDIENCE`, `BLUE_BACK`; `RED_BACK_CELL`, `RED_AUDIENCE_CELL`). Behavior is
   identical.
4. **D21:** the manual has no "near" or "far" start. Team definition: FAR = "the wall farthest from
   our LOADING ZONE". This is documented in the `FieldConstants` Javadoc, and the D21 TODOs are
   gone. `StartPositionRulesTest` checks every start against G304. The Safe Park "loading zone"
   start broke G304.E and was **fixed** (moved behind the zone tape).

## 5. Questions for the team

1. **POLLEN into FLOWERS before 1:00?** G410 names only NECTAR, but §10.5.2 says FLOWER scoring
   cannot begin before one minute and earlier scoring is "subject to penalties". Ask the Q&A
   before placing POLLEN in a FLOWER early. The arm stays locked until then anyway.
2. **First target cell vs. the manual's setup.** Per Fig. 10-2, the **red AUDIENCE cell (34–37)
   starts UP**. The tested routine treats the **BACK cell (30–33)** as its first target
   (`HiveCell.getBackCell`, used by `HiveStateTracker` and `ScoringAuto`). Was the practice field
   set up differently? Please check on a correctly set-up field. Behavior was not changed.
3. **D21 wording.** `FAR_START` (8.96, 131.19) is on the red wall right next to (behind) our
   LOADING ZONE, which is *not* the wall farthest from it. The current routines still use it.
   Confirm that this is the intended FAR start, or give the pose you mean.
4. **Safe Park Encoders** now only scores LEAVE (3 points), because it may not start in the zone.
   Should it get a short encoder strafe into the zone for PARK? It would need a strafe
   ticks-per-cm calibration.
5. **G410 margin:** 1 s (`NECTAR_UNLOCK_MARGIN_SECONDS`, TODO: TUNE). Do drivers ever press START
   earlier than that (during the 8 s transition)? If so, raise it.
6. **FLOWER keep-out radius:** 4 in (`FLOWER_KEEP_OUT_RADIUS_INCHES`) is a guess. Measure it from
   the field CAD.
7. **G404/G403:** should TeleOp cut all outputs automatically when its own clock reaches 0:00? It
   cannot see the FIELD timer, so pressing STOP is still required.
