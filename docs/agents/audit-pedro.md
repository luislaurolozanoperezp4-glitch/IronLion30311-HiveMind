# Hand-off: Pedro Pathing Auditor

Full report: `docs/AUDIT_PEDRO.md`. Everything was read-only. No git write commands were run, and no files were changed other than this note and the report.

## Key facts for the next agents
- **Pedro version**: `com.pedropathing:revhub:3.0.2` (latest on Maven Central, checked 2026-11-24). Tuning `com.pedropathing:tuning:1.0.3` (latest, vision-align only). FTC SDK 12.0.0 (latest). Repo `https://repo.dairy.foundation/releases/` is needed for tuning. **No library migration is needed.**
- **Localizer**: goBILDA Pinpoint `pinpoint` through `PinpointLocalizer`. Follower: `Foresight`, with **untuned placeholder gains everywhere** (`maxPathSpeed 0.6`).
- **Best constants**: `origin/feature/turrubiates/vision-align` → `subsystems/pathing/PedroFollowerFactory.java` (blob d937b54f): xPodOffset -6.79 in, yPodOffset -0.72 in, Y pod REVERSED, `manualBrakeMode true`. Its drive names (`frontLeftDrive…`) differ from beta (`FL…`).
- **Only field-tested auto**: vision-align `PreloadRouteAuto` (4/4 preload shots, parked in 12-14 s). It drives odometry legs through `follower.manual()`, not Pedro paths.
- **No BezierCurves exist.** Every path is `Paths.line`. Straight-line copies are identical except for the two `Paths.path` chains (Tick T3, Center Lane C4).

## Blocking issues (TO VERIFY with the Competition Manual)
1. Beta's `Alliance` ("blue = left, mirrorX(72) for red") conflicts with the team field map (`feature/mar/duopodriveNcam` `BiobuzzFieldMap`). That map shows red on the left (A-C), red LZ at x 0-11 / y 96.5-119.5, blue LZ at x 133-144 / y 24.5-47.5, i.e. **180° rotational symmetry**. Use identical alliance-relative poses (vision-align approach). Never mirror.
2. Tick and Center Lane (beta) drive under the HIVE (x 60, y 30-120). Old Center Lane crosses the HIVE frame. Re-route them west via x ≈ 33.
3. Several end poses sit 0.2-1.0 in from the wall (LEAVE risk). Shoot Turn Collect and Shoot And Collect have no PARK.

## Suggested next steps
- Migration agent: create `PedroConstants` / `PedroFollowerFactory` from vision-align, `AllianceFrame` (identity + rotate180), `FieldGeometry` (from BiobuzzFieldMap in the Pedro frame), then `AutoPreloadPark`, `AutoPreloadCollectPark`, `AutoOuterLane`, `AutoSafePark`, `AutoLeavePark`. Keep the originals and add `*Straight` copies per section 7 of the audit.
- On the robot: run AutoTune (Mecanum → Pinpoint → Localization test → Foresight → Line/Hold), then remove the 0.6 speed cap.
- Confirm: field size 144 vs 141.5 in; robot size (17.83 x 16.54 vs 18 x 17.5 in); the turn direction of `.linear()` across ±180°; whether `PoseFactory` mirror calls can be chained.
