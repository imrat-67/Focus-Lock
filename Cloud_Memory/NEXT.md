# NEXT (rewrite after every finished task)

## USER ACTION NEEDED
- Choose test device for sign-in and the lock engine: real phone via Wireless debugging (send model + Android version) or emulator. Sign-in (Credential Manager) is untested until then.

## Do now: T05 Permissions checklist + CoreService skeleton + SessionStore (UI-2, LOCK-6,7)
1. Read SRS UI-2, LOCK-6,7 and docs/ARCHITECTURE.md "Runtime pieces".
2. Permissions screen: accessibility, overlay/notifications, exact alarms, device admin status checks (no enforcement yet).
3. CoreService (foreground, silent notification) skeleton; SessionStore (encrypted local cache) with unit tests for pure parts.
4. Commit per sub-step; feat(T05) at end.

## Notes
- Rules tests: cd rules-tests; export JAVA_HOME=~/tools/jdk-21.0.13+9; NODE_ENV=development npm i --include=dev; npm test (shell sets NODE_ENV=production).
- Debug APK is 19 MB; release must be < 15 MB (T13).
- Formats/schedules/app-list models + repos are built in T07/T08 (rules already cover them).
- deviceId is immutable in rules (phone migration flow deferred to T13).
