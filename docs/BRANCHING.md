# BRANCHING

How we use Git and GitHub in IronLion30311-HiveMind.

## Permanent branches

| Branch | What it is | Who pushes |
|---|---|---|
| `main` | Stable code that is safe to run at an event. Protected: changes only by pull request. | nobody directly |
| `dev` | Integration branch. Every work branch starts here and merges back here. | pull requests |

## Work branches

Format: `<type>/<short-description>`, lowercase kebab-case, 2–4 words, max 30 characters.
An issue number is optional: `feat/12-launcher-pidf`. CI rejects pull requests from branches
that break the format.

| Type | Use it for | Example |
|---|---|---|
| `feat/` | a new feature | `feat/auto-shoot` |
| `fix/` | a bug fix | `fix/intake-jam` |
| `auto/` | an autonomous routine | `auto/red-near` |
| `path/` | Pedro paths | `path/hive-cycle` |
| `tune/` | tuning values | `tune/launcher-pidf` |
| `refactor/` | cleanup with no behavior change | `refactor/subsystems` |
| `test/` | JUnit tests | `test/match-timer` |
| `docs/` | documentation | `docs/readme` |
| `ci/` | GitHub Actions | `ci/gradle-build` |
| `chore/` | dependencies, SDK updates | `chore/update-sdk` |
| `hotfix/` | urgent fix at an event (from `main`) | `hotfix/park-timeout` |
| `agent/` | work by the AI agent team | `agent/subsystems` |

## Everyday workflow

```bash
git checkout dev
git pull
git checkout -b feat/auto-shoot
# ...edit, build, test on the robot...
git add -A
git commit -m "feat: add auto-shoot"
git push -u origin feat/auto-shoot
# open a pull request into dev on GitHub; after review, merge it.
# GitHub deletes the branch automatically after the merge.
```

Commit messages start with the branch type: `feat: ...`, `fix: ...`, `tune: ...`, `docs: ...`.

## Releasing to main

When `dev` is tested on the robot, open a pull request `dev` → `main`. After it merges, tag it.

## Events are tags, not branches

```bash
git checkout main
git pull
git tag -a v1.0-qual-1 -m "Code used at Qualifier 1"
git push origin v1.0-qual-1
```

Tag names: `v1.0-qual-1`, `v1.1-qual-2`, `v2.0-state`.

## Hotfix at an event

```bash
git checkout main
git pull
git checkout -b hotfix/park-timeout
# fix, build, test on the practice field
git commit -am "fix: shorten park timeout"
git push -u origin hotfix/park-timeout
# pull request into main, merge, tag (v1.0.1-qual-1), then merge main into dev:
git checkout dev && git pull && git merge main && git push
```

## Updating the FTC SDK

The history starts from the official `FtcRobotController` `v12.0` tag.

```bash
git remote add upstream https://github.com/FIRST-Tech-Challenge/FtcRobotController.git  # once
git checkout dev
git checkout -b chore/update-sdk
git fetch upstream --tags
git merge v12.1          # the new release tag
# fix conflicts (usually only build.dependencies.gradle), build, test, pull request into dev
```

## Rules

- Never push directly to `main`.
- Delete branches after merging (GitHub does it automatically in this repo).
- The old repo `IronLion30311BIOBUZZInicioDeTemporada` is read-only history: do not push there.
