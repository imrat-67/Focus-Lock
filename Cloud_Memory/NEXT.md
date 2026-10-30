# NEXT (rewrite after every finished task)

## USER ACTION NEEDED
- Re-test on vivo (new build installed): Lock now 5 min Full; press Home and Recents several times. Lock screen should now return in well under 1 s. Also test the emergency 112/999 button: the call screen must still show (lock must not cover it). Tell me the result.

## Do now: finish T08 step f (fix for "Home lets you browse then lock pops late" is installed, needs re-test)
1. After user feedback: fix issues, then mark T08 DONE in STATUS (one line), commit feat(T08), push.
2. Not yet verified: boot rebase, clock-change tamper, scheduled start, notice notification, Firestore upload of self session (check Firestore users/{uid}/sessions).
3. Then T09 (link flow).

## Notes
- Debug lock: `adb shell am broadcast -a com.imrat.focuslock.DEBUG_LOCK -n com.imrat.focuslock/.DebugSessionReceiver --es mode full|apps --ei secs 120 --es pkgs a.b,c.d` ; DEBUG_CLEAR ends it (phone: adb is wireless, ~/Android/Sdk/platform-tools/adb).
- T08 limits to handle later: Observer-created schedules run locally only (rules forbid User writing session createdBy=observer): T10 must cover Observer cancel (UNL-1/SCH-4). Boot rebase resets password cooldown (T11). Home placeholder text is stale (T13).
- Rules tests: cd rules-tests; export JAVA_HOME=~/tools/jdk-21.0.13+9; NODE_ENV=development npm i --include=dev; npm test.
- Debug APK 20 MB; release < 15 MB (T13). Permissions screen title overlaps status bar (T13).
