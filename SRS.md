# Focus Lock — Software Requirements Specification (v2.0)

Platform: Android only (minSdk 26) · Backend: Firebase Spark (free) · Auth: Google Sign-In · Distribution: sideloaded APK (GitHub Releases)
Repo: git@github.com:imrat-67/Focus-Lock.git · Language: English UI (strings in resources, Bangla-ready)
Requirement IDs (e.g. LOCK-2) are referenced by Cloud_Memory/STATUS.md. Architecture/data model: docs/ARCHITECTURE.md.

## 1. Purpose
Helps a student stay focused by locking their phone (whole phone or chosen apps). Core idea: a trusted **Observer** (parent/friend) can lock the student's phone remotely, but only after the student explicitly consents. Everything is free: no ads, no premium, no cost to run.

## 2. Glossary
- **User**: owner of the phone being locked. **Observer**: person allowed to lock/unlock a User's phone.
- **Account**: one Google account = one lockable phone + may observe others. Roles are per-relationship, not per-account.
- **Session**: one active/pending lock (instant or one occurrence of a schedule). **App list**: named set of apps.
- **Full Lock**: whole phone blocked. **Apps Lock**: only selected apps blocked.

## 3. Functional requirements

### AUTH
- AUTH-1 Google Sign-In (Firebase Auth) is the only login.
- AUTH-2 One account manages exactly one phone ("My Device"). Signing in on a second phone is rejected with a clear message.
- AUTH-3 Sign-out and account deletion are disabled while a lock is active on this phone.

### LINK (Observer relationship)
- LINK-1 "Add Device" offers **My Device** or **Remote Device**. Both lead to the same lock/schedule/app-list screens.
- LINK-2 Remote Device: Observer enters the target's email or friend code. A request is created; the target sees it in-app (and as a local notification). Nothing can be locked until the target **accepts**.
- LINK-3 Lookup never reveals whether an email has an account (always "request sent if the account exists").
- LINK-4 A User has at most one Observer; an Observer can have many Users. A request to a User who already has an Observer is refused.
- LINK-5 Connection is permanent until removed. Two accounts may observe each other.
- LINK-6 Removal: the User sends a removal request; it completes only when the Observer accepts. If the Observer does not respond within 7 days it completes automatically. It also completes if the Observer deletes their account.
- LINK-7 Removal never takes effect while a lock is active; it completes after the lock ends.

### LOCK
- LOCK-1 Two modes: **Full Lock** and **Apps Lock**; two timings: **Instant** and **Scheduled**.
- LOCK-2 Full Lock: whenever the screen is unlocked, the Focus Lock lock screen covers everything. Allowed through: the lock screen itself, an incoming/ongoing call UI (dialer is allowed only while a call is active, so outgoing dialing stays blocked), a ringing alarm. Home and Recents cannot be used to escape: the lock screen returns immediately. (The phone's own system lock-screen emergency dialer is unaffected.)
- LOCK-3 Apps Lock: opening any blocked app shows the lock screen instead. Settings and all other apps work unless included. Home and Recents work normally in this mode (T14). Focus Lock itself is never covered in this mode; starting one shows an "App lock is on" toast (self) or notification (remote) instead (T14g).
- LOCK-4 Lock screen shows: live countdown, who locked, mode, password field. Nothing else: no Emergency-unlock button and no call buttons (removed in T14).
- LOCK-5 Maximum single session length: 24 hours (changed from 12 in T14).
- LOCK-6 Enforcement is local and works fully offline once a session is known to the phone.
- LOCK-7 Active sessions survive reboot and app/process death (restored from encrypted local cache at boot).
- LOCK-8 Tamper-resistant time: end time is computed from server time offset + monotonic clock; changing the device clock must not shorten a lock.
- LOCK-9 Normal system notifications are never blocked or hidden by the lock.

### SCH (Scheduling)
- SCH-1 Schedules can be one-time or repeating (days of week + start/end time); several allowed. A one-time schedule's date is chosen from a calendar (today or later).
- SCH-2 Whoever created a schedule owns it: Observer-created schedules can be edited/deleted only by the Observer; User-created ones only by the User.
- SCH-3 Each occurrence becomes a session at its start time, using exact alarms locally (no server needed at start time).
- SCH-4 Observer can cancel an upcoming or running instant/scheduled session at any time (see UNL-1).

### FMT (App lists)
- FMT-1 An App list = name + list of apps. No hard count limit (Firestore doc-size bound only).
- FMT-2 Both User and Observer can create/edit/delete App lists for that User's phone. Using an App list for a lock is one tap.
- FMT-3 The User's phone uploads its launchable app list (name + package only) so the Observer can choose apps. Readable only by that User and their Observer. Refreshed when the app opens and when apps are installed/removed.

### UNL (Unlocking)
- UNL-1 Observer can unlock remotely at any time; the session ends immediately (needs the User's phone online).
- UNL-2 Emergency password: 6 digits, new for every session, generated on the **Observer's** phone, visible only there. The User's phone only ever holds a salted slow hash. The Observer may copy/share it manually (e.g. via WhatsApp).
- UNL-3 Correct password ends and deletes that session. A new lock needs a new session.
- UNL-4 5 wrong attempts → 10-minute cooldown, persisted locally. No history/report is kept.
- UNL-5 Observer can regenerate the password for a live session.
- UNL-6 Repeating schedules: Observer's phone pre-generates hashes for the next 14 occurrences and tops up whenever it is online. If the pool runs out, that occurrence has no password (remote unlock only) and the Observer is told.
- UNL-7 Self-lock (My Device, no Observer involved): no password and no exit button; it ends only when the timer runs out (max 24 h). An Observer, if one is linked, can still unlock it by password or remote unlock. (Emergency exit removed in T14.)

### NOT (Notifications)
- NOT-1 Scheduled lock: one notification 10 minutes before start.
- NOT-2 Remote Instant lock: a 1-minute warning notification; lock starts after 60 s and the User cannot cancel it (they should contact the Observer). Self-started instant locks start immediately after a confirm dialog.
- NOT-3 Link request / removal request alerts: heads-up local notification plus a red dot on the Home profile icon while a request is pending (T14g).
- NOT-4 Unavoidable: Android requires one low-priority persistent notification for the always-on service. Keep it minimal and silent.
- NOT-5 No other Focus Lock notifications (the only extra one is the "App lock is on" notice when a remote Apps lock starts).
- UI-S Device status shows the lock type: Unlocked / Full lock / Selected apps locked. Instant lock duration defaults to 1 minute.

### SEC (Security)
- SEC-1 Device Admin is enabled so Focus Lock cannot be uninstalled during a lock.
- SEC-2 During any active lock the Accessibility service also blocks: Device-admin deactivation, Focus Lock's App-info/uninstall/force-stop/storage screens, and Accessibility settings for Focus Lock.
- SEC-3 Firestore Security Rules enforce all permissions server-side (see ARCHITECTURE.md); no client is trusted.
- SEC-4 No secrets or keys in the repo; `google-services.json` and keystores are git-ignored.
- SEC-5 Local secrets (session cache, Observer's plaintext passwords) live in Android Keystore–backed encrypted storage.
- SEC-6 Threat model: resists ordinary bypass (force-stop, uninstall, clock change, reboot, app switching, going offline). Cannot resist Safe Mode, ADB/PC tools, root, or factory reset — no normal app can. A determined offline brute-force of a 6-digit hash is possible for a technical user; PBKDF2 with high iterations makes it slow, not impossible.

### UI
- UI-1 Screens: Sign-in · Home (devices + requests) · Add Device · Device detail (lock now, schedules, app lists) · Lock screen · Permissions checklist.
- UI-2 First-run permissions checklist: Accessibility, Device Admin, Notifications, Exact alarms, Battery-optimization exemption, Usage access, Display-over-apps. Only permissions the app really uses (no phone-state permission; call exemption uses AudioManager mode). Grouped cards with granted/missing and a button.
- UI-3 Simple, clean, Material 3; dark mode follows system.

## 4. Non-functional
- NFR-1 Stays inside Firebase Spark limits (target: < 5k reads / 2k writes per active day for 10 users). Timers tick locally; listeners only on own documents.
- NFR-2 No Cloud Functions, no paid services, no card needed.
- NFR-3 Release APK < 15 MB (R8 on). Low battery impact: no polling, event-driven listeners.
- NFR-4 Must work on Android 8.0+ (API 26+); verify on the user's real phone; account for OEM background killers.

## 5. Out of scope (v1)
Quick-lock widget, statistics/reports/attempt history, iOS, premium, ads, Strict Mode, battery-saver mode, Play Store release, Cloud Functions.

## 6. Open items
Test-phone model/Android version (needed for T05+), Firebase project creation (user does console steps; see STATUS T03).
