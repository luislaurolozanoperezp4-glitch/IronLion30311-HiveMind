# Contributing to IronLion30311-HiveMind

The full guide is in [`docs/CONTRIBUTING.md`](../docs/CONTRIBUTING.md). In short:

1. Branch from `dev` with a name like `fix/intake-jam` (`docs/BRANCHING.md`).
2. Follow `docs/ARCHITECTURE.md` and `docs/NAMING.md`; every number goes in `config/`, untested
   values get `// TODO: TUNE`, every Java file starts with `// Sources:`.
3. Run `./gradlew assembleDebug testDebugUnitTest` and `bash .github/scripts/check-code-rules.sh`.
4. Open a pull request into `dev`. Never push to `main`.

This file replaces the FTC SDK's generic contributing guide, which is about contributing to the
SDK itself (see the FIRST-Tech-Challenge/FtcRobotController repository for that).
