# NEXT (rewrite after every finished task)

## USER ACTION NEEDED
- None. Phone connected via wireless debugging (re-pair if `adb devices` is empty).

## Do now: T06 Lock engine (LOCK-1..4,9)
1. `grep -n "LOCK-" SRS.md`; read ARCHITECTURE "Runtime pieces".
2. Write checkpoint in STATUS.md, then: LockAccessibilityService foreground detection, LockActivity, Full Lock, exemptions (calls, alarms, emergency calls).
3. Commit per sub-step; feat(T06) at end.

## Notes
- Rules tests: cd rules-tests; export JAVA_HOME=~/tools/jdk-21.0.13+9; NODE_ENV=development npm i --include=dev; npm test.
- Debug APK 19 MB; release < 15 MB (T13).
- Permissions screen title overlaps status bar: fix insets in T13.
