# NEXT (rewrite after every finished task)

## USER ACTION NEEDED
- none (Claude self-checks what it can; only a 2nd-account run is left, optional). Old note: test T10b with 2 accounts: (1) Observer Manage > Schedules > create a schedule that is running now, then delete it -> User phone lock should lift; (2) send a link request / removal request and look for the notification; (3) while a remote lock runs, Observer's Release button should be disabled.

## Do now: T11 emergency password
Read SRS UNL-2..7 and docs/ARCHITECTURE.md password section. Needs: 6-digit per-session password generated on Observer phone, salted slow hash (pwHash/pwSalt) in session doc, check on User lock screen, 5 wrong -> 10 min cooldown (persisted), regenerate (UNL-5), schedule pool of 14 (UNL-6), self-lock emergency exit 5-min countdown (UNL-7). Start with pure logic + tests.

## Notes
- DEBUG_LOCK while Chrome is foreground does not cover until the next window change (accessibility event); T13 check that Lock now/remote start always shows LockActivity.
- Debug lock: `adb shell am broadcast -a com.imrat.focuslock.DEBUG_LOCK -n com.imrat.focuslock/.DebugSessionReceiver --es mode full|apps --ei secs 120 --es pkgs a.b,c.d` ; DEBUG_CLEAR ends it (adb is wireless, ~/Android/Sdk/platform-tools/adb).
- Boot rebase resets password cooldown (fix in T11). Home placeholder text is stale (T13). Observer-made schedules use the Observer's timezone (assume same tz).
- Observer-made schedule sessions have no server doc, so LINK-7 Release guard cannot see them.
- Rules tests: cd rules-tests; export JAVA_HOME=~/tools/jdk-21.0.13+9; NODE_ENV=development npm i --include=dev; npm test.
- Debug APK 20 MB; release < 15 MB (T13). Permissions screen title overlaps status bar (T13).
