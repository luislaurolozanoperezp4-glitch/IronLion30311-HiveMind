# Shooting and TeleOp research

An English summary of the team's research report on shooting and TeleOp improvements, with the
actions that matter for our robot and what HiveMind already does.

**Source and credit:** `reports/Mejoras de tiro y TeleOp FTC.md` and its four notes
(`flywheel_control.md`, `aim_assist_and_ranging.md`, `driver_assist.md`,
`reliability_diagnostics.md`) in `ironlionrobotics/IronLion30311BIOBUZZInicioDeTemporada`,
branch `feature/turrubiates/vision-align` @ `48de874` (2026-11-24), written for Iron Lion by
the `vision-align` author. The report cites public sources (gm0, FTC Docs, REV and Limelight
documentation, goBILDA manuals, WPILib, Chief Delphi threads and other teams' public repos).
This file paraphrases it; it does not copy it. Numbers marked "team data" come from our own
logs; others come from the cited sources and are not checked on our robot.

## 1. The main idea

Our robot has no turret and no adjustable hood, so it has only two "aiming" actuators: the
**robot heading** and the **flywheel RPM**. The report argues that the biggest wins are:

1. keep the POLLEN flywheel spinning between volleys (it is ready much sooner);
2. an aim assist that turns **only the heading** toward a goal stored in **field coordinates**,
   while the driver keeps full control of translation;
3. a **distance → RPM table** so the robot can shoot from more than one spot;
4. a stricter **fire gate** (only feed when everything is right);
5. a **reliable pose and battery**: every assist above is only as good as the Pinpoint pose and
   the battery voltage.

Items 2–5 form a package: the aim needs the table, the table needs the pose, and the pose needs
a healthy Pinpoint and battery.

## 2. Ranked recommendations (benefit vs. effort)

| # | Recommendation | Benefit | Effort | HiveMind status |
|---|---|---|---|---|
| 1 | POLLEN idle spin (~2000 RPM) from TeleOp START, off at the end | High: 0.7–2.3 s earlier per volley (team data: ready in 0.7 s from ~2000 RPM vs 1.4–3.0 s from a stop) | Very low | **Done** (`Launcher.setPollenIdle`, `LauncherConstants.POLLEN_IDLE_RPM`, gamepad2 right stick button, off on low battery) |
| 2 | Heading-only aim assist toward a goal remembered in field coordinates | Very high: shoot from any angle | Medium | Not done. `HiveAligner` still drives three axes and is off (`HIVE_ALIGN_ENABLED = false`) |
| 3 | Interpolated distance → RPM table (5–7 points) plus live D-pad trim | High: shoot from any distance | Low–medium (one calibration session) | Not done. First point known (team data: 29 scored at 3000 RPM from ~1.03 m floor distance) |
| 4 | Compound fire gate: heading in tolerance, low turn rate, **asymmetric RPM window with dwell**, distance inside the table | High: fewer missed shots | Low | **Partly done:** asymmetric window −50 / +80 RPM (`READY_BELOW_RPM`, `READY_ABOVE_RPM`); no dwell, turn-rate or distance check yet |
| 5 | Protect the Pinpoint physically and detect/recover resets in code | High: no lost aim/park after a hit | Low–medium | **Partly done:** reboot recovery in `OdometryDriver`, `PinpointDiagnostics` (status, Hz, pod counts, INIT warning, fault at START stops the drive). Cable strain relief is a hardware task |
| 6 | Battery program by internal resistance; fix the power path | Medium–high: less sag | Low (~US$50 analyzer + wiring) | Hardware task (team data: ~0.13 Ω total path, sag to 9–10 V at ~20 A) |
| 7 | Faster loop: from 40–55 ms toward 15–20 ms | Medium: steadier heading control | Low | Partly: MANUAL bulk caching, rate-limited HuskyLens and battery reads already in HiveMind; measure loop time (`Loop` in DEBUG telemetry) |
| 8 | Own flywheel controller (feedforward + P at loop rate, or bang-bang + feedforward) with a kick when a ball is fed | Medium: shorter recovery | Medium | Not done (risk of a regression before an event; the hub PIDF is TESTED) |
| 9 | Field-centric with an alliance offset, heading reset through the pose, AUTO pose handed to TeleOp | Medium | Low | **Done** (field-centric toggle, `PoseStorage` hand-off); alliance offset not added |
| 10 | Heading-rate feedforward to keep aiming while strafing | Medium | Medium | Not done (verify the sign on the robot first) |
| 11 | Endgame rumbles; auto-park button that aborts on stick input | Low–medium | Low | Rumbles **done** (`EndgameAlerts`); auto-park button not done |
| 12 | Shooting while moving (virtual goal = goal − velocity × flight time) | Low for this chassis | High | Not planned; a "barely moving" check in the fire gate gets most of the benefit |

## 3. Details worth keeping

### 3.1 Flywheel

- Idle spin: at steady speed the motor only fights friction and air. Start it at TeleOp START,
  **never in INIT** and never during the AUTO→TELEOP transition (G403), and cut power when the
  match ends (G404). Drop it while the battery is low.
- A shot dip from 3000 to 2230–2500 RPM removes 31–45 % of the wheel's kinetic energy. More
  wheel mass makes shots more consistent; it does not shorten the recovery (the motor does).
  Plot target, measured RPM and motor output: if the output never nears 100 % during recovery,
  power is being left unused.
- The hub's velocity PIDF runs at a fixed rate and our voltage compensation rescales only F.
  A loop-rate `kS + kV·v + kP·error` (scaled by 12 V / filtered battery) is the textbook
  alternative; do not stack it on top of `setVelocity`.
- Encoder resolution: with 28 counts per revolution, 3000 RPM is 1400 ticks/s and one tick/s is
  ~2.14 RPM. Keep using the hub's `getVelocity()` (or average 40–60 ms); a home-made
  position difference in a 12 ms loop would be ~180 RPM per step.
- Feed gate: an asymmetric window with a short **dwell** (40–60 ms inside the window) avoids
  feeding during the overshoot after a recovery. The old symmetric 100/200 RPM hysteresis let a
  ball go up to 200 RPM low.

### 3.2 Aim and range

- Store the goal in field coordinates: whenever the Limelight sees our correct cell, convert its
  floor center (from `HiveGeometry`) to the field frame with the current Pinpoint pose and keep
  it. Aim with `atan2` to that point while the tags are hidden. This works even if the HIVE
  cells move (tags on moving parts are not good for relocalizing the robot).
- The launcher and the Limelight face the rear: the heading target is the goal bearing **+ 180°**.
  Always wrap the error to (−180°, 180°]; take the derivative from the measured turn rate, not
  from the error.
- Make the "aimed" tolerance depend on distance and arrival angle (half the effective opening
  divided by the distance); published teams use 2–5°. Ours is `VisionConstants.ALIGN_YAW_TOLERANCE_DEGREES` = 2.5°.
- Ranging from the tag area (`ta`) degrades on diagonal shots; use the floor distance from the
  pose or from the tag cluster geometry instead.

### 3.3 Pose and battery reliability

- The Pinpoint loses its configuration and returns to (0, 0, 0) when it loses power, and it
  recalibrates its IMU for ~0.25 s at power-up (the robot must be still). A brief 5 V dropout on
  a JST connector after a hit, or ESD, matches what we saw.
- Hardware: strain relief 2–3 cm from both JST ends with a service loop, keep the cable away
  from the flywheel motor and chassis edges, a ferrite near the hub, mount the Pinpoint on a
  non-conductive plate, use an I²C port other than 0, Limelight on the USB 3.0 port.
- Software: on a non-READY status or an impossible jump, freeze the last good pose, wait for
  READY, re-send the configuration, restore the pose, log the event with the voltage.
- Battery: goBILDA suggests retiring high-current packs above ~130 mΩ internal resistance; use
  the lowest-resistance packs at events and recharge after every match. Measure R = ΔV/ΔI at the
  pack and at the hub to separate pack from wiring.

### 3.4 Loop time

The time goes into hub communication, not CPU: MANUAL bulk reads, read the HuskyLens only while
collecting or every ~100 ms, do not read the hub IMU when the Pinpoint gives the heading, read
the battery every 100–250 ms, send telemetry at 4–10 Hz. Measure each block before optimizing.

## 4. Open questions from the report

1. The exact G403 / G404 wording for a coasting flywheel (the report relies on another team's
   reading; check the manual).
2. Are the HIVE AprilTags fixed to the field? If yes, the Limelight can correct the Pinpoint x/y.
3. The largest useful diagonal shooting angle for our robot (measure it).

## 5. Actionable next steps for HiveMind (not yet done)

1. Add a **dwell** (~50 ms) to the feed window in `Launcher.updateReady` and a "robot nearly
   still" check to the TeleOp fire gate. Test with the shot marks in the match log.
2. Build the **distance → RPM table** (`LauncherConstants`, 5–7 points from a calibration
   session with `Tune Launcher PIDF` and the Limelight distance) with gamepad2 D-pad trim on top.
3. Write a **heading-only aim** mode for `HiveAligner` (driver translates, controller turns the
   rear to the goal), using a goal remembered in field coordinates; keep it behind a switch.
4. Extend the Pinpoint recovery to TeleOp (re-send the configuration after a reboot) once the
   AUTO recovery has been seen working in the logs.
5. Battery program and Pinpoint cable work (hardware, see §3.3).
