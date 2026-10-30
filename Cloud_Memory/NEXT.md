# NEXT (rewrite after every finished task)

## USER ACTION NEEDED
- None.

## Do now: T08 Instant + Scheduled locks, alarms, timer, reboot persistence, time-tamper guard (LOCK-5,7,8, SCH-1..3)
1. `grep -n "SCH-\|LOCK-5\|LOCK-7\|LOCK-8" SRS.md`; read ARCHITECTURE "Time".
2. Write checkpoint. Pure first (with unit tests): time model (serverOffset+elapsed anchor), schedule expansion, 12 h cap.
3. Then: start-lock UI (self lock: mode, format/apps, duration), BootReceiver, AlarmReceiver, clock-change guard.
4. Test lock logic on emulator (D23); Firebase-dependent parts on the phone.

## Notes
- Debug lock: `adb shell am broadcast -a com.imrat.focuslock.DEBUG_LOCK -n com.imrat.focuslock/.DebugSessionReceiver --es mode full|apps --ei secs 120 --es pkgs a.b,c.d` ; DEBUG_CLEAR ends it.
- Emulator: see D23. Phone: wireless debugging; sign in via app (emulator cannot).
- Rules tests: cd rules-tests; export JAVA_HOME=~/tools/jdk-21.0.13+9; NODE_ENV=development npm i --include=dev; npm test.
- Debug APK 19 MB; release < 15 MB (T13). Permissions screen title overlaps status bar (T13).
