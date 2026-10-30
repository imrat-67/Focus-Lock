# NEXT (rewrite after every finished task)

## USER ACTION NEEDED
- None now. (Final real-phone test is T13; enable Accessibility via the app's Grant button then.)

## Do now: T07 Apps Lock + app-list upload + Formats (LOCK-3, FMT-1..3)
1. `grep -n "FMT-\|LOCK-3" SRS.md`; read ARCHITECTURE data model.
2. Write checkpoint, then: installed-app list (PackageManager, launchable apps), app picker UI, upload list for Observer, Formats model/repo.
3. Test on emulator: `./gradlew installDebug` with `ANDROID_SERIAL=emulator-5554` after starting it (see D23); enable a11y with `adb shell settings put secure enabled_accessibility_services com.imrat.focuslock/com.imrat.focuslock.service.LockAccessibilityService`.

## Notes
- Debug lock: `adb shell am broadcast -a com.imrat.focuslock.DEBUG_LOCK -n com.imrat.focuslock/.DebugSessionReceiver --es mode full|apps --ei secs 120 --es pkgs a.b,c.d` ; DEBUG_CLEAR ends it.
- Rules tests: cd rules-tests; export JAVA_HOME=~/tools/jdk-21.0.13+9; NODE_ENV=development npm i --include=dev; npm test.
- Debug APK 19 MB; release < 15 MB (T13). Permissions screen title overlaps status bar (T13).
