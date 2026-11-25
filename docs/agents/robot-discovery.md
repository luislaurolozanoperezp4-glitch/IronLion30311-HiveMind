# Hand-off: Robot Discovery Agent (Phase 0)

Date: 2026-11-24. Output: `docs/ROBOT_PROFILE.md` (the main deliverable). This note is for the next agents.

## What I did
- Read-only study of `legacy/IronLion30311BIOBUZZInicioDeTemporada`. All 63 `origin/*` and `localcopy/*` refs point to
  41 unique tip commits. I extracted each tree's TeamCode + Markdown with `git archive` into my scratchpad
  (outside the repo) and grepped them. I also scanned the full history for every device-name string ever added.
- Checked that `legacy/pedro-pathing-biobuzz-analysis-a65cc9` = `feature/autonomos-beta` (206fa84). The files match after
  ignoring CRLF, except the snapshot diff (`@Disabled` on `ShootTurnCollectAuto`).
- No git write commands. No files changed except the two I own.

## Key conclusions
- **Current robot = `origin/feature/turrubiates/vision-align` (a7b8f4c, 2026-11-24).** It has field-tested values: intake
  REVERSE, Pinpoint Y pod REVERSED, fitted pod offsets, shooter PIDF 30/0/0/15 with voltage compensation, pollen
  2950–3000 RPM, nectar 3600 RPM, gate removed. It is NOT merged into `master`. The two lines have diverged since
  `develop` (afbabf1).
- `master` (Luis's Pedro autos) was only checked in a simulator. It still has older hardware assumptions (intake FORWARD,
  zero pod offsets, indexer only with nectar, `FL`/`indexer` names).
- The team already has naming conventions and a rename map (`CONVENTIONS.md`, `doc/naming/rename-map.md` on the
  turrubiates branches). New DS names are in use on `vision-align` with a legacy fallback. My §12 mapping builds on that
  and is marked PROPOSED.
- The robot is POLLEN-only this competition (`NectarAlertLight` javadoc). It has no placing mechanism in tested code.
- Pedro Pathing 3 constants were never tuned. The working autos use a custom Pinpoint `driveTo()` instead.

## Traps for later agents
- Same physical fact, different signs: intake direction and Y-pod direction differ between branches. Trust `vision-align`.
- Pod offsets: `vision-align` (fitted) and `duopodriveNcam` (measured) look swapped. Do not copy either until the team measures (Q4).
- HuskyLens IDs 2/3 (red/blue) are swapped between branches (Q11).
- `FlowerArm` on `feature/renata/flower-arm` does not compile (constructor named `DualServo`).
- `feature/luis/autonomospaths` / `theautonomous` hardware (lift, feeders, pollen2/nectar2, 435 RPM, 96 mm wheels) is
  speculative. Do not treat it as robot facts.
- No hub/port numbers exist anywhere in the sources (Q1).

## Open items
See "QUESTIONS FOR THE TEAM" (19 items) at the end of `docs/ROBOT_PROFILE.md`. The most blocking are Q2 (active DS names),
Q4 (pod offsets), Q6 (Pedro tuning), Q9 (ball path to the two flywheels) and Q18 (which branch to build on).
