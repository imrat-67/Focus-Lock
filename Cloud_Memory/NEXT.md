# NEXT (rewrite after every finished task)

## USER ACTION NEEDED
- Optional: test T09 with a 2nd Google account (other phone/emulator): send request by friend code, accept, request removal. Report anything odd.

## Do now: T10 remote lock/unlock
Read SRS SCH-4, UNL-1, NOT-1..5, SessionRepository, sessions rules; plan steps into CHECKPOINT; Observer-side Add Device chooser (My/Remote) + remote device screens (Formats/Lock/Schedules for target uid) come here.

## Notes
- Debug lock: `adb shell am broadcast -a com.imrat.focuslock.DEBUG_LOCK -n com.imrat.focuslock/.DebugSessionReceiver --es mode full|apps --ei secs 120 --es pkgs a.b,c.d` ; DEBUG_CLEAR ends it (phone: adb is wireless, ~/Android/Sdk/platform-tools/adb).
- T08 limits: Observer-created schedules run locally only (rules forbid User writing session createdBy=observer): T10 must cover Observer cancel (UNL-1/SCH-4). Boot rebase resets password cooldown (T11). Home placeholder text is stale (T13).
- T09 limit: Observer 'Release' does not check whether the User has an active lock (Observer cannot see it yet); T10 live session listener should guard LINK-7 there.
- Rules tests: cd rules-tests; export JAVA_HOME=~/tools/jdk-21.0.13+9; NODE_ENV=development npm i --include=dev; npm test.
- Debug APK 20 MB; release < 15 MB (T13). Permissions screen title overlaps status bar (T13).
