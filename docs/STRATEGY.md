# STRATEGY

The match plan for FTC #30311 Iron Lion in BIOBUZZ, and how the code supports it. Game facts
come from the Competition Manual **TU10** as summarized in `docs/RULES_COMPLIANCE.md` (the
manual is the authority). Robot facts come from `docs/ROBOT_PROFILE.md`.

**Our robot in one line:** a mecanum robot that collects POLLEN at the front, stores up to 4,
and launches them into the up-facing HIVE cell with a rear flywheel; it can PARK in AUTO and at
the end. It scores **POLLEN only** for now (the NECTAR flywheel exists, the flower arm is
optional and not confirmed on the robot).

## 1. Where the points are (TU10, Table 10-2 and 10-3)

| Achievement | AUTO | TELEOP / end | Our plan |
|---|---|---|---|
| LEAVE (not touching the wall at the end of AUTO) | 3 | – | Every AUTO |
| PARK (partly in our LOADING ZONE) | 5 | 5 | Every AUTO, and at the end of every match |
| HIVE TIP | 20 | 20 | Our main scoring: launch POLLEN into the up cell |
| Element left in an up-facing CELL at match end | – | 2 each | Side effect of launching |
| Element in an owned FLOWER | – | 2 each | Only with the flower arm, last 60 s (G410) |
| Bottom NECTAR Bonus | – | 5 | Not planned |
| Element in GARDEN | – | 1 each | Not planned |

**Ranking points:** WIN 3, TIE 1, plus
- **SWARM**: alliance LEAVE + PARK points ≥ **16**. With both robots doing LEAVE (3) + AUTO PARK
  (5), the alliance gets exactly 16 in AUTO alone. **So every AUTO must LEAVE and PARK.**
- **POLLINATOR 1**: ≥ **4** TIPs; **POLLINATOR 2**: ≥ **7** TIPs.

These thresholds apply at Regional Championships and "all other events"; Premier Events may set
their own, and the FIRST Championship ones are TBA.

**Fouls to avoid:** MINOR = 5 points to the opponent, MAJOR = 20. The ones our robot can commit
are in section 5.

## 2. AUTO (30 s)

### 2.1 Which routine to pick

| Situation | OpMode | Why |
|---|---|---|
| Normal match, we start at the audience wall | **Auto Red Near / Auto Blue Near** | The only field-tested scoring route (4/4 preload shots and parked in ~12–14 s on 2026-11-23). |
| Our partner needs the audience-wall spot | **Auto Red Far / Auto Blue Far** | **UNTESTED.** The FAR pose is still an open question (D21, see `ROBOT_PROFILE.md` §0.3). Use only after a practice-field test. |
| Pinpoint, Limelight or launcher is not trusted today | **Auto Safe Park** (pick NEAR / FAR / beside zone in INIT) | LEAVE + PARK = 8 points, no shooting. Keeps SWARM alive. |
| Odometry is broken (Pinpoint not found, pose jumps) | **Auto Safe Park Encoders** | Wheel encoders only, 10 cm back: **LEAVE only (3 points)**. It cannot reach the zone for PARK. |

If the Pinpoint is missing in a scoring auto, the robot still shoots its preload but does not
drive (the start-pose check reports FAULT).

### 2.2 The NEAR routine (what the robot does)

1. Start at (57, 9) with the intake against the audience wall, 4 POLLEN preloaded.
2. **LEAVE:** back off 10 cm.
3. **Volley:** spin the POLLEN flywheel to 2950 RPM and feed the 4 balls one at a time, each
   when the RPM has recovered (`ShootingSequence`). The volley must end by 22 s.
4. **Decision** (`AutoPlanner`): cycle again only if a collect-and-shoot cycle **plus** the
   park after it still fit, keeping a 4 s park reserve, and the volley ended before 11 s (the
   tested backstop). With the current seed estimates, a volley ending after about 7 s skips the
   cycle. `MAX_CYCLES` = 1.
5. **Cycle** (if chosen): watch our cell for an early TIP (up to 4 s), turn, collect the row of
   POLLEN slowly, then shoot at our cell if it is still up, or drive around the west side of the
   HIVE to the other cell if it tipped (only before 15 s; that volley must end by 26.5 s).
6. **PARK** in the LOADING ZONE through the lane west of the HIVE, ending **2 in off the wall**
   (so LEAVE still counts) and partly in the zone.

**Time guard:** every loop, if the time left is less than the estimated park time plus the
reserve, the current step is cancelled and `EmergencyPark` drives straight to PARK without
turning. **Territory guard (G402):** if odometry says the robot crossed toward the opponent's
half, the drive stops for the rest of AUTO.

### 2.3 Alliance coordination and the start delay

Before every match, talk to the partner team:

1. **Who takes which start?** Only one robot can use the NEAR spot at x = 57 on the audience
   wall. If both want it, one of us uses FAR or Safe Park.
2. **Paths:** our near route uses the lane west of the HIVE (x ≈ 33 in) and parks in our
   LOADING ZONE. Ask where the partner drives and parks.
3. **Start delay:** in INIT, gamepad1 D-pad up/down sets a delay of 0–10 s in 0.5 s steps
   (`AutoConstants.START_DELAY_MAX_SECONDS`, `START_DELAY_STEP_SECONDS`). Use it to let the
   partner clear our lane first. The time planner counts the delay, so a long delay means the
   robot skips the cycle and goes to park after the preload.
4. **SWARM:** both robots must LEAVE and PARK. If the partner has no reliable AUTO, suggest a
   simple LEAVE + PARK; our Safe Park is an example they can copy.

## 3. TELEOP (2:00)

### 3.1 The cycle

1. **Collect** (driver 1, hold LB): drive over POLLEN. Ball assist turns toward the closest
   POLLEN or our own NECTAR (never toward opponent NECTAR). At 4 elements the intake refuses more (G407) and the
   driver feels a short rumble.
2. **Go to the shooting spot** about 1.3 m from the HIVE tags, facing the HIVE with the rear
   (the tested spot for 3000 RPM, `ROBOT_PROFILE.md` §4.3).
3. **Launch** (driver 2): hold RT to fire POLLEN (the indexer feeds only at speed), or tap A for
   an automatic volley of the balls the robot holds. D-pad up/down adjusts the RPM by ±50 if
   shots go long/short; X / Y / B marks a shot SHORT / LONG / WIDE in the match log.
4. **Watch the HIVE:** after a TIP the other cell of our alliance faces up. The TIP count is on
   the telemetry, so the human player knows how many NECTAR they may enter (G426/G427).
5. Repeat. Each TIP is 20 points; 4 TIPs give POLLINATOR 1.

### 3.2 Opponent NECTAR (G408)

If opponent NECTAR is at the intake, the intake **stops** by itself (it never pulls it in and
never pushes it out), gamepad1 rumbles and a banner appears. **Driver 1 must drive away.** The
intake resumes 0.3 s after the ball leaves the camera's view.

## 4. ENDGAME (last 60 s)

| Time left | What happens | What the drivers do |
|---|---|---|
| 1:05 | Both gamepads: 3 short blips | Plan the last cycle so the robot can still reach the LOADING ZONE |
| 1:00 | Long rumble, `*** ENDGAME ***` banner, speed-mode light blinks; the flower arm unlocks (G410) | If the flower arm is on the robot and holds NECTAR, it may now go into a FLOWER (gamepad1 B) |
| ~0:10 (team call) | — | Drive to PARK: at least part of the robot in our LOADING ZONE |
| 0:00 | — | **Press STOP** (G404: no powered movement after the match). The robot cannot see the field timer. |

**FLOWER rules:** NECTAR may enter a FLOWER only in the last 60 s (G410, MAJOR FOUL per NECTAR).
The code locks the arm until 59 s on its own clock (1 s margin because the robot clock starts
at START, not with the field timer). Elements enter a FLOWER only from the top; only POLLEN may
be removed, only from the bottom (G418). Whether POLLEN may go into a FLOWER before 1:00 is an
open question for the Q&A (`RULES_COMPLIANCE.md` §5).

## 5. Fouls our robot could commit

| Rule | Risk | Protection |
|---|---|---|
| G402 | Disrupting the opponent's AUTO | Routes stay on our half; `TerritoryGuard` stops the drive |
| G407 | Controlling more than 4 elements | `ElementCounter` blocks the intake; gamepad1 X resets a wrong count |
| G408 | Controlling opponent NECTAR | Intake stops; the driver drives away |
| G410 | NECTAR into a FLOWER before 1:00 | Flower arm locked by the code; launched NECTAR is the drivers' job |
| G417 | Changing the HIVE's motion except by launching into an up cell | AUTO keeps clear of the HIVE; in TELEOP do not ram the HIVE |
| G403 / G404 | Moving during the transition or after the match | Do not press INIT/START early; press STOP at 0:00 |
| G304 | Illegal start | Start poses are checked by tests; the drive coach checks the robot is touching the wall, not in the LOADING ZONE, 4 POLLEN touching |

## 6. What to improve next

- Test the Far autos, then decide whether they are worth keeping (D21).
- Tune the planner estimates from the AUTO pose logs, then raise `MAX_CYCLES` if a second cycle
  fits.
- Calibrate HIVE auto-align (currently off) to make TELEOP launching faster.
- Decide on NECTAR scoring (flower arm, second flywheel) for the ENDGAME.
