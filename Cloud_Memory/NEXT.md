# NEXT (rewrite after every finished task)

## USER ACTION NEEDED
- none right now

## Do now: T09 link flow
Read SRS LINK-1..7, LinkRepository, rules; plan steps into CHECKPOINT; build pure logic first with tests.

## Notes
- Debug lock: `adb shell am broadcast -a com.imrat.focuslock.DEBUG_LOCK -n com.imrat.focuslock/.DebugSessionReceiver --es mode full|apps --ei secs 120 --es pkgs a.b,c.d` ; DEBUG_CLEAR ends it (phone: adb is wireless, ~/Android/Sdk/platform-tools/adb).
- T08 limits to handle later: Observer-created schedules run locally only (rules forbid User writing session createdBy=observer): T10 must cover Observer cancel (UNL-1/SCH-4). Boot rebase resets password cooldown (T11). Home placeholder text is stale (T13).
- Rules tests: cd rules-tests; export JAVA_HOME=~/tools/jdk-21.0.13+9; NODE_ENV=development npm i --include=dev; npm test.
- Debug APK 20 MB; release < 15 MB (T13). Permissions screen title overlaps status bar (T13).
