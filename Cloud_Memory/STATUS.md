# STATUS

Legend: [ ] todo · [~] in progress · [x] done. Finished tasks keep ONE line only.

## Task board (in order)
- [x] T00 Docs, repo init, Cloud_Memory v2 (SRS v2, ARCHITECTURE)
- [x] T01 Android SDK at ~/Android/Sdk (platform 36, build-tools 36.1.0, adb 1.0.41), ANDROID_HOME in ~/.bashrc
- [x] T02 Skeleton builds: debug APK OK (Compose hello screen, google-services plugin applied); versions in D16-D18
- [x] T03 Firebase live: project, Android app, SHA-1, Google sign-in enabled, Firestore (asia-south1), placeholder deny-all rules deployed, app/google-services.json has oauth clients (see D15)
- [x] T04 Data layer + sign-in + rules: models/repos (User, Link, Session), Google sign-in, rules deployed, 25 emulator tests + 7 unit tests pass (D19-D20); sign-in VERIFIED on vivo Y29 (profile + friend code created in Firestore)
- [x] T05 Permissions checklist, CoreService (foreground, verified on vivo), encrypted SessionStore + tests; checklist UI title overlaps status bar (fix inset, T13)
- [x] T06 Lock engine: LockPolicy+tests, LockActivity, accessibility enforcement; verified on emulator (Full/Apps lock, call exemption, emergency button, expiry, back/home); real-phone recheck in T13
- [x] T07 Installed-app list + upload (AppListSync), Formats model/repo/UI + picker; formats CRUD confirmed by user on phone; upload log not captured (verify in T09 when Observer reads list)
- [x] T08 Instant + Scheduled locks, alarms, timer, boot rebase, ServerClock; Lock now VERIFIED on vivo (needed overlay perm D24 + re-cover on stop); reboot/clock-tamper/scheduled-start/notice NOT yet device-tested -> T13 pass
- [ ] T09 Link flow: request/accept/remove, lookup, 7-day auto-complete (LINK-1..7)
- [ ] T10 Remote lock/unlock via live listeners, warnings/notifications (SCH-4, UNL-1, NOT-1..5)
- [ ] T11 Emergency password (hash, cooldown, pool, regenerate) + self-lock emergency exit (UNL-2..7)
- [ ] T12 Hardening: Device Admin, block sensitive Settings screens, sign-out guard (SEC-1,2, AUTH-3)
- [ ] T13 Polish, on-device test pass, release APK (R8, size < 15 MB), README, GitHub Release

## CHECKPOINT (live; overwrite before each step)
Task: T09
Plan/steps: [ ] a) read SRS LINK-1..7 + existing LinkRepository/rules
Files touched: none yet
Last commit: see git log
