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
- [ ] T11 Emergency password (hash, cooldown, pool, regenerate) + self-lock emergency exit (UNL-2..7)
- [ ] T12 Hardening: Device Admin, block sensitive Settings screens, sign-out guard (SEC-1,2, AUTH-3)
- [ ] T13 Polish, on-device test pass, release APK (R8, size < 15 MB), README, GitHub Release

## CHECKPOINT (live; overwrite before each step)
Task: T11a = password for remote instant locks + unlock on lock screen + self-lock emergency exit; T11b = schedule pwPool (UNL-6)
Plan T11a:
[ ] 1 pure PasswordCrypto (6 digits, salt, PBKDF2 100k, const-time verify) + UnlockPolicy (5 wrong->10 min cooldown, boot keeps cooldown) + tests
[ ] 2 LocalSession.exitAtElapsedMs + codec; SessionStore tests still pass
[ ] 3 lock screen: verify password, success ends session (+markPasswordUsed, doneIds so it never restarts); cooldown msg; self-lock Emergency exit 5-min countdown
[ ] 4 Observer: PasswordVault (encrypted local), generate on remote lock, show/copy + Regenerate (UNL-5); RemoteSync handles hash updates + passwordUsed
[ ] 5 build, tests, install, device self-check with debug lock
Plan T11b: Schedule.pwPool field+codec, Observer top-up 14 occurrences, startScheduled uses pool
Files touched: none yet
Last commit: see git log
