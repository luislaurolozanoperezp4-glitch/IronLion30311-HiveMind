# Hand-off: Documentation Agent (`agent/docs`)

Branch `agent/docs` (from `origin/dev` @ `770240f`). Build: `./gradlew assembleDebug
testDebugUnitTest` → BUILD SUCCESSFUL, 216 tests, 0 failures. `check-code-rules.sh` → OK.

## Written / updated

| File | What |
|---|---|
| `README.md` | Full rewrite: status + robot test list, team (city/country and level left as `TODO (team)`), robot, versions, step-by-step setup/deploy, hardware table (every `RobotConstants` name + legacy names), controls (checked against `TeleOpBase`), OpMode list (checked against every annotation), AUTO selector, Dashboard/AutoTune URLs, tests/CI, tree, docs index, credits, license |
| `docs/CODE_GUIDE.md` | New: every main and test class in reading order (config → hardware → util → subsystems → statemachine → pathing → opmodes → tests), with source and connections; mermaid layer and match-cycle diagrams |
| `docs/MIGRATION.md` | New: source repos/branches/commits per class (from the `// Sources:` headers), class/method/enum/constant/OpMode renames, DS name old → new, behavior changes |
| `docs/NAMING.md` | New: full naming convention (packages, classes, methods, fields, constants, OpModes, DS names, tests, docs, branches + CI regex, tags, commits) |
| `docs/STRATEGY.md` | New: points and ranking points (TU10), AUTO choice, NEAR routine, alliance coordination + start delay, TeleOp cycle, G408, endgame/FLOWER rules, fouls |
| `docs/MATCH_CHECKLIST.md` | New: pre-event (DS names, firmware, batteries, robot checks), pre-match, during, post-match (adb log pull — no script exists), pit fixes |
| `docs/CONTRIBUTING.md` | New: team workflow and rules, CI, files not to edit, fork/reuse guide, inspiring repositories |
| `.github/CONTRIBUTING.md` | Replaced the SDK's generic guide with a short pointer to `docs/CONTRIBUTING.md` |
| `docs/ROBOT_PROFILE.md` | New §0: team decisions D18, D13, D20, D21 (definition), G408, parks ≥ 2 in, repo location as CONFIRMED; current-code summary; open questions; Q13/Q18 marked CLOSED, items 20–23 added |
| `docs/ARCHITECTURE.md` | Package map verified against the tree; "Placer" replaced by `FlowerArm`; rules and the real loop order updated |
| `docs/BRANCHING.md` | CI checks table, tag rules |
| `docs/agents/teleop.md` | G408 row corrected (intake stops, never ejects) |
| Javadoc | All public classes/methods in `TeamCode/src/main` already had Javadoc (checked by script; vendored tuners skipped). Fixed two stale Javadocs: last park leg is 56.5 cm, not 60 (`PreloadParkPaths`, `ScoringAuto`). No code changed. There is no Javadoc Gradle task for the Android module. |

## Open items for the team

1. Fill in city/country and experience level in `README.md` → Team.
2. FAR start pose does not match the D21 definition (`ROBOT_PROFILE.md` §0.3).
3. First target cell vs. manual setup (BACK vs AUDIENCE cell).
4. Hub/port map (Q1) — the hardware table has no port numbers.
5. The robot test list in `README.md` → Status has not been run yet.
