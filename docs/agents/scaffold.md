# Hand-off: Architect / Scaffold Agent (`agent/scaffold`)

## Done
- Repository history now starts from the official FtcRobotController `v12.0` tag (`e14c2ae`), so SDK updates can be merged from `upstream`.
- `build.dependencies.gradle`: SDK 12.0.0, FTC Dashboard 0.6.0, Pedro `revhub:3.0.2`, `tuning:1.0.3` (same set as vision-align, which compiled and ran on the robot).
- `TeamCode/build.gradle`: JUnit 4.13.2, `unitTests.returnDefaultValues = true`.
- Package tree with `package-info.java` Javadoc in every package.
- `config/`: RobotConstants, DriveConstants, IntakeConstants, IndexerConstants, LauncherConstants, VisionConstants, FieldConstants, MatchConstants — values from `docs/BEST_OF_MATRIX.md` §2.1, each with its status (TESTED / FITTED / `TODO: TUNE` / decision number).
- `subsystems/Subsystem.java` interface: `periodic()`, `stop()`, `addTelemetry(Telemetry t)`.
- First JUnit test: `FieldConstantsTest` (RED poses stay on the red half; PARK inside the loading zone).
- CI: `.github/workflows/build.yml` (assembleDebug + unit tests on push/PR to main/dev; branch-name check on PRs).
- PR and issue templates; `CLAUDE.md`; `docs/ARCHITECTURE.md`; `docs/BRANCHING.md`; short README; SDK readme moved to `docs/SDK_README.md`.
- Local build verified: `./gradlew assembleDebug testDebugUnitTest` → BUILD SUCCESSFUL, 2 tests passed.

## Decisions
- Constants split per mechanism (see ARCHITECTURE "Differences from the original plan").
- Driver Station names = vision-align names (D2/D15 defaults) with legacy fallbacks.
- `NEAR_SHOOT` / `OTHER_SHOOT` headings derived from PreloadRouteAuto (start heading, start + 180°).

## Open questions
- D13 (alliance frame), D16 (robot size), D19 (field size), D20/D21 (cells, far start) still affect `FieldConstants`.
- `docs/NAMING.md` is referenced but written by the Documentation Agent.

## For the next agents
- Subsystems agent: implement `hardware/RobotHardware` (fault-tolerant `getDevice` with legacy names) and the subsystems against these constants.
- Pathing agent: create `pathing/PedroConstants` from vision-align `PedroFollowerFactory` and use `FieldConstants` poses.
