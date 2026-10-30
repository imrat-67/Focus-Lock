# NEXT (rewrite after every finished task)

## USER ACTION NEEDED
- none. Optional 2-account check of T11b: Observer creates a schedule on User's phone (Remote device > Schedules); Observer's Remote device screen shows "Next scheduled locks" with a password + Copy; when that lock runs on User's phone the password ends it.

## Do now: T12 Hardening (SEC-1,2, AUTH-3)
Device Admin, block sensitive Settings screens during lock, sign-out guard. First command: grep -n "SEC-\|AUTH-3" SRS.md

## Notes
- LockActivity is NOT exported: `am start` fails. To show the lock in tests, broadcast DEBUG_LOCK then `am start -n com.imrat.focuslock/.MainActivity`. Extras: --es pw 123456, --ez self true. Read screen with `adb shell uiautomator dump`.
- T13: LockScreen keeps `outcome` after a session ends (reset on session id change). First tap on Unlock after typing only hides keyboard on vivo. Confirm boot rebase uses UnlockPolicy.afterBoot. Permissions screen title overlaps status bar. Release APK < 15 MB (debug is 20 MB).
- Debug lock: `adb shell am broadcast -a com.imrat.focuslock.DEBUG_LOCK -n com.imrat.focuslock/.DebugSessionReceiver --es mode full|apps --ei secs 120`; DEBUG_CLEAR ends it (adb wireless, ~/Android/Sdk/platform-tools/adb).
- Observer-made schedule sessions have no server doc (LINK-7 guard blind); Observer schedules use Observer timezone.
- Rules tests: cd rules-tests; export JAVA_HOME=~/tools/jdk-21.0.13+9; NODE_ENV=development npm i --include=dev; npm test.
