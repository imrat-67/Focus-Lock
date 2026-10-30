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
- [x] T09 Links screen (request by email/code, accept/decline, removal + 7-day auto, LINK-7 lock guard) + LinkRules tests (3); installed on vivo; request/accept CONFIRMED working by user, 'My Device' vs 'Remote Device' chooser deferred to T10
- [x] T10 Remote instant lock + Observer unlock + 1-min warning: RemoteSync (4 tests), CoreService session listener, RemoteDeviceScreen (lock now, formats, unlock list); CONFIRMED working by user on devices. NOT-1 10-min warning already existed from T08
- [x] T10b NOT-3 alerts (LinkAlerts, 3 tests); SCH-4 Observer deleting/disabling a schedule ends its running session (ScheduleCancel, 3 tests); Observer can manage target's schedules (RemoteDevice); LINK-7 Release guard (server-visible locks only). Self-checked: app+service run on vivo without crash, 30 emulator rules tests pass (5 new for app query shapes), debug lock re-covers after Home. Two-account end-to-end not run by Claude (no 2nd login)
- [x] T11 Emergency password (UnlockPolicy, PasswordVault, regenerate, Emergency exit, schedule pwPool + SchedulePoolTopUp + RemoteDevice list; 92+ unit tests; T11a device-verified, T11b builds+installs on vivo, 2-account pool flow NOT run by Claude)
- [x] T12 Hardening: SEC-1 Device Admin; SEC-2 LockPolicy.isProtectedScreen + accessibility scan (windowContent) + tests; AUTH-3 sign-out disabled during lock (VM guard + UI). 89 unit tests pass, installed on vivo. SEC-2 device-verified on vivo (App info covered by LockActivity during lock)
- [~] T13 Done: perms inset, signed release APK 3.2 MB (R8, keystore ~/focuslock-keys, keystore.properties ignored), README. Left: add release SHA-1 to Firebase, on-device pass, GitHub Release

- [~] T14 (a-g done: T14g = request notif+red dot, 'App list' rename, Apps lock never covers FL, lock-type pill, 1-min default, calendar date) REDESIGN (see REDESIGN.md): new UI/theme, new flow (Home/Add device/My vs Remote), lock-screen fixes, hour+minute picker, Home/Recents block, remove emergency buttons, two-device testing. Waiting for user answers to Claude's clarifying questions.

## CHECKPOINT (live; overwrite before each step)
Task: T15 user feedback #2. Done+built+92 unit tests green: d,e,b,c,f, SRS UI-2 updated. Logo options A-D shown to user in chat; WAITING for pick. Left: [ ] a apply chosen logo as adaptive icon (res/drawable ic_launcher_fg/bg xml + res/mipmap-anydpi-v26/ic_launcher.xml, manifest android:icon/roundIcon) [ ] g install on vivo when phone connected, push
