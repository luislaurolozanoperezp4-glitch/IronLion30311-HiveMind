# PEDRO TUNING AND HYBRID DRIVE

How to tune Pedro Pathing on the robot and then switch the autonomous routes, one at a time,
from the odometry driver to Pedro path following.

**Status today:** Pedro is **not tuned**. Its follower gains in `pathing/PedroConstants.java` are
placeholders and path speed is capped at `MAX_PATH_SPEED = 0.6`. Every competition route drives
with the odometry driver (`pathing/OdometryDriver.java`), which is the method used in the 4/4
field test. Nothing changes on the field until the team finishes this guide.

## How the hybrid drive works

| Part | Who drives it |
|---|---|
| Long multi-leg routes (west lane to PARK, around the HIVE to the other cell, parks) | **Pedro** path following *if switched on*, otherwise the odometry driver |
| Short precise moves (10 cm LEAVE, aim turns and aim distance, the last inches into PARK, holding still while shooting) | **Always the odometry driver** |

- Both use the same Pinpoint position, so the G402 territory guard, the time-to-park guard and
  the odometry-jump/reset guards work the same way with either.
- With Pedro on, a route becomes: one Pedro path through all its waypoints, without stopping
  at the corners, then a short odometry-driver move onto the last waypoint, then settle and hold.
- If Pedro takes longer than `PEDRO_TIMEOUT_BASE_SECONDS + length / PEDRO_TIMEOUT_INCHES_PER_SECOND`,
  it is stopped and the odometry driver finishes the route. If the drive is locked (G402) or
  time runs short, the Pedro step is cancelled like any other step.
- The switches live in `config/HybridDriveConstants.java` (also editable live in FTC Dashboard):

| Switch | Route |
|---|---|
| `IS_PEDRO_TUNED` | **Master safety switch.** Pedro is used only when this is `true` AND the route switch is `true` |
| `PEDRO_PRELOAD_PARK` | Shooting spot up the west lane into PARK (the tested route). Best first candidate |
| `PEDRO_CYCLE_PARK` | Park after a collect cycle |
| `PEDRO_OTHER_CELL_TRANSIT` | Around the HIVE to the other-cell spot, and back down the west lane |
| `PEDRO_OTHER_SIDE_PARK` | From the other-cell spot into PARK |
| `PEDRO_WAIT_TIP_PARK` | Wait Tip autos into the upper LOADING ZONE |
| `PEDRO_SAFE_PARK` | Auto Safe Park |
| `PEDRO_EMERGENCY_PARK` | Time guard's emergency park. Switch it on last |

## Step 1: AutoTune (on the robot)

You need: a charged battery (above 12.5 V), 3 free tiles in front of the robot and 3 to its left,
a laptop or phone on the robot Wi-Fi.

1. Install the app (`Run 'TeamCode'` in Android Studio) and start the Robot Controller.
2. Open **http://192.168.43.1:10158** in a browser on the robot Wi-Fi (Pedro AutoTune page).
3. Run the procedures **in this order**. After each one, copy the Java code it prints into the
   matching config in `pathing/PedroConstants.java`, rebuild and install:

| # | Procedure | What it finds | Paste into |
|---|---|---|---|
| 1 | **Mecanum Procedure** | Direction of each drive motor | `DRIVETRAIN_CONFIG` |
| 2 | **Pinpoint Procedure** (push forward, push left, spin 180° counter-clockwise) | Pod directions and pod offsets | `LOCALIZER_CONFIG` (also update `X_POD_OFFSET_INCHES` / `Y_POD_OFFSET_INCHES`) |
| 3 | **Tests > Localization / Odometry** | Forward must increase x, left must increase y | (check only) |
| 4 | **Foresight Procedure** (the robot drives by itself) | Max velocities, braking, follower gains | `FOLLOWER_CONFIG` (replace every `// TODO: TUNE`) |
| 5 | **Tests > Line, Curve, Hold** | The tuned follower drives straight, curves and holds | (check only) |

4. When the tests in step 5 look good, raise `MAX_PATH_SPEED` from `0.6` to `1.0` and run
   Tests > Line again.
5. Measure the pod offsets with a tape measure and compare them with what AutoTune found
   (decision D4 in `docs/ROBOT_PROFILE.md`).

## Step 2: check the whole chain

1. Run **Test Odometry Square** (Tuning group). The robot follows a square with Pedro and must
   return within about 1 in of where it started.
2. Run **Test Pedro Pose** and push the robot around: the pose on the Driver Station must match
   the field.
3. Only now set `HybridDriveConstants.IS_PEDRO_TUNED = true` in the code (not only in the
   Dashboard: Dashboard changes are lost when the app restarts), rebuild and install.

## Step 3: switch routes one at a time

For each route, starting with `PEDRO_PRELOAD_PARK`:

1. Run the auto 3 times with the switch **off** and write down from the step log / CSV
   (`pose_logs`): time to PARK and the final pose error.
2. Turn the switch **on** and run it 3 times again.
3. Keep Pedro for that route only if it is **faster** and ends **within about 1 in** of the
   odometry driver's end. Otherwise switch it back off.
4. Write the result in the table below and commit (`tune: pedro on preload park`).

| Route | Odometry driver (time / end error) | Pedro (time / end error) | Decision | Date |
|---|---|---|---|---|
| PRELOAD_PARK | | | | |
| OTHER_CELL_TRANSIT | | | | |
| CYCLE_PARK | | | | |
| OTHER_SIDE_PARK | | | | |
| WAIT_TIP_PARK | | | | |
| SAFE_PARK | | | | |
| EMERGENCY_PARK | | | | |

## If something goes wrong at an event

Set `IS_PEDRO_TUNED = false` (one line in `config/HybridDriveConstants.java`, or live in FTC
Dashboard for that session). Every route immediately goes back to the tested odometry driver.
