# IronLion30311-HiveMind

Iron Lion #30311 — BIOBUZZ 2026–27 competition code. The best of our season-start code and
Pedro Pathing work, unified into one clean Java codebase.

> **Status: work in progress (Phase 2 — scaffold).** The package structure, constants, Gradle
> setup and CI are in place. Subsystems, TeleOp and autonomous routines are being migrated next.
> The full README (build, deploy, controls, OpMode list) comes with the documentation phase.

| | |
|---|---|
| FTC SDK | FtcRobotController **v12.0** |
| Pedro Pathing | **3.0.2** (`com.pedropathing:revhub`) + AutoTune 1.0.3 |
| Language | Java only |

## Quick build

1. Install [Android Studio](https://developer.android.com/studio) (it includes the JDK and Android SDK).
2. `git clone https://github.com/ironlionrobotics/IronLion30311-HiveMind.git`
3. Open the folder in Android Studio and let Gradle sync (it downloads the FTC SDK, Pedro Pathing
   and FTC Dashboard automatically).
4. Build: **Build > Make Project**, or from a terminal `./gradlew assembleDebug testDebugUnitTest`.

## Docs

- [Robot profile](docs/ROBOT_PROFILE.md) — what the robot is, from the code
- [Architecture](docs/ARCHITECTURE.md) · [Branching](docs/BRANCHING.md)
- [Best-of matrix](docs/BEST_OF_MATRIX.md) · [Main audit](docs/AUDIT_MAIN.md) · [Pedro audit](docs/AUDIT_PEDRO.md)
- [FTC SDK README](docs/SDK_README.md) (original FtcRobotController readme)

## Credits

Built from Iron Lion's `IronLion30311BIOBUZZInicioDeTemporada` repository (all branches) and its
Pedro Pathing analysis work (`pedro-pathing-biobuzz-analysis-a65cc9`), on top of the official
[FtcRobotController](https://github.com/FIRST-Tech-Challenge/FtcRobotController) and
[Pedro Pathing](https://pedropathing.com).
