# NEXT (rewrite after every finished task)

## USER ACTION NEEDED
- none. Optional 2-account check of T11a: Observer "Lock now" on User -> password shows under Remote sessions (Copy / New password); User phone accepts it on the lock screen; after New password the old one fails.

## Do now: T11b schedule password pool (UNL-6)
T11a (lock-screen unlock, cooldown, Emergency exit, PasswordVault, regenerate) is committed and device-verified. Next: see CHECKPOINT plan in STATUS.md. First command: grep -n "pw\|class Schedule" app/src/main/java/com/imrat/focuslock/data/Schedules.kt firestore.rules. After T11b mark T11 DONE on the board, then T12.

## Notes
- LockActivity is NOT exported: `am start` fails. To show the lock in tests, broadcast DEBUG_LOCK then `am start -n com.imrat.focuslock/.MainActivity` (window change makes the enforcer cover). Extras: --es pw 123456, --ez self true. Read screen with `adb shell uiautomator dump` (screenshots were unreliable).
- T13: LockScreen keeps `outcome` state after a session ends (stale "no emergency password" text seen once); reset it when session id changes. First tap on Unlock after typing only hides the keyboard on the vivo (needs 2nd tap) - consider IME handling.
- Boot rebase now keeps cooldown/exit (UnlockPolicy.afterBoot) - confirm it is wired in boot rebase (T13 reboot test).
- Debug lock: `adb shell am broadcast -a com.imrat.focuslock.DEBUG_LOCK -n com.imrat.focuslock/.DebugSessionReceiver --es mode full|apps --ei secs 120`; DEBUG_CLEAR ends it (adb wireless, ~/Android/Sdk/platform-tools/adb).
- Observer-made schedule sessions have no server doc (LINK-7 guard blind); Observer schedules use Observer timezone.
- Rules tests: cd rules-tests; export JAVA_HOME=~/tools/jdk-21.0.13+9; NODE_ENV=development npm i --include=dev; npm test.
- Debug APK 20 MB; release < 15 MB (T13). Permissions screen title overlaps status bar (T13).
