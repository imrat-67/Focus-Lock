# NEXT (rewrite after every finished task)

## USER ACTION NEEDED
- none. (After any reinstall, re-enable Settings > Accessibility > Focus Lock.)

## Do now: T13 Polish, on-device test pass, release APK (R8, size < 15 MB), README, GitHub Release
First command: grep -n "T13\|Notes" Cloud_Memory/NEXT.md ; then fix Permissions title inset, build `./gradlew assembleRelease`.

## Notes
- LockActivity is NOT exported: `am start` fails. To show the lock in tests, broadcast DEBUG_LOCK then `am start -n com.imrat.focuslock/.MainActivity`. Extras: --es pw 123456, --ez self true. Read screen with `adb shell uiautomator dump`.
- T13: LockScreen keeps `outcome` after a session ends (reset on session id change). First tap on Unlock after typing only hides keyboard on vivo. Confirm boot rebase uses UnlockPolicy.afterBoot. Permissions screen title overlaps status bar. Release APK < 15 MB (debug is 20 MB). Device-test pass: reboot/clock-tamper/scheduled-start/notice.
- Debug lock: `adb shell am broadcast -a com.imrat.focuslock.DEBUG_LOCK -n com.imrat.focuslock/.DebugSessionReceiver --es mode full|apps --ei secs 120`; DEBUG_CLEAR ends it (adb wireless, ~/Android/Sdk/platform-tools/adb).
- Observer-made schedule sessions have no server doc (LINK-7 guard blind); Observer schedules use Observer timezone.
- Rules tests: cd rules-tests; export JAVA_HOME=~/tools/jdk-21.0.13+9; NODE_ENV=development npm i --include=dev; npm test.
