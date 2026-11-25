# CLAUDE.md

Guidance for AI coding agents working in IronLion30311-HiveMind (FTC team #30311 Iron Lion,
BIOBUZZ 2026–27).

- **Everything in English**: code, comments, telemetry, docs, branch names, commits.
- **Java only**, language level 8 (set by the FTC SDK): no `var`, records, or switch expressions.
- Read `docs/ROBOT_PROFILE.md` before changing robot code; code must match the real robot.
- Follow `docs/ARCHITECTURE.md` (layers, rules) and `docs/NAMING.md` (names).
- Driver Station names live only in `config/RobotConstants.java`.
- Every number goes in a `config/*Constants` class. Untested values get `// TODO: TUNE`.
- Never `sleep()` in TeleOp loops; use `ElapsedTime` and state machines.
- New or migrated files start with `// Sources: <repo> / branch <b> / <path> @ <commit> — adapted for BIOBUZZ`.
- Branches: `docs/BRANCHING.md`. Work from `dev`, open pull requests into `dev`; never push to `main`.
- Build and test before committing:
  `./gradlew assembleDebug testDebugUnitTest` (Windows: `gradlew.bat ...`; needs JDK 17+, e.g.
  Android Studio's bundled JBR).
- Pedro Pathing 3 API: poses are `com.pedropathing.math.Pose(x, y, headingRadians)`; the
  follower is built in `pathing/PedroConstants`. Check the Pedro docs before using an API you
  have not seen in this repo.
- AUTO driving is hybrid: multi-leg routes go through `AutoBase.routeLegs` / `parkSteps` with a
  `DriveRoute`; `config/HybridDriveConstants` decides Pedro path following vs the tested
  `OdometryDriver` (all odometry until Pedro is tuned, see `docs/PEDRO_TUNING.md`). Short precise
  moves always use the odometry driver.
- Field frame: inches, origin at the red/audience corner, poses written for RED.
- The source repo `ironlionrobotics/IronLion30311BIOBUZZInicioDeTemporada` is read-only.
