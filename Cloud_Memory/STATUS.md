# STATUS

Legend: [ ] todo · [~] in progress · [x] done. Finished tasks keep ONE line only.

## Task board (in order)
- [x] T00 Docs, repo init, Cloud_Memory v2 (SRS v2, ARCHITECTURE)
- [x] T01 Android SDK at ~/Android/Sdk (platform 36, build-tools 36.1.0, adb 1.0.41), ANDROID_HOME in ~/.bashrc
- [ ] T02 Skeleton: Gradle KTS + version catalog + Compose app `com.imrat.focuslock`, builds debug APK, .gitignore
- [~] T03 Firebase: project+Android app+SHA-1+config DONE (see D15). PENDING USER: enable Google sign-in + create Firestore DB (asia-south1, production mode) in console; then re-download google-services.json (needs oauth client), write firestore.rules/firebase.json, deploy rules (AUTH-1,2)
- [ ] T04 Data layer: models, repositories, Firestore rules + emulator rules tests (SEC-3)
- [ ] T05 Permissions checklist + CoreService skeleton + SessionStore (UI-2, LOCK-6,7)
- [ ] T06 Lock engine: Accessibility detect, LockActivity, Full Lock, call/alarm/emergency-call exemptions (LOCK-1..4,9)
- [ ] T07 Apps Lock + app-list upload + Formats (LOCK-3, FMT-1..3)
- [ ] T08 Instant + Scheduled, alarms, timer, reboot persistence, time-tamper guard (LOCK-5,7,8, SCH-1..3)
- [ ] T09 Link flow: request/accept/remove, lookup, 7-day auto-complete (LINK-1..7)
- [ ] T10 Remote lock/unlock via live listeners, warnings/notifications (SCH-4, UNL-1, NOT-1..5)
- [ ] T11 Emergency password (hash, cooldown, pool, regenerate) + self-lock emergency exit (UNL-2..7)
- [ ] T12 Hardening: Device Admin, block sensitive Settings screens, sign-out guard (SEC-1,2, AUTH-3)
- [ ] T13 Polish, on-device test pass, release APK (R8, size < 15 MB), README, GitHub Release

## CHECKPOINT (live; overwrite before each step)
Task: none active.
Plan/steps: —
Files touched: —
Last commit: —
