# Architecture (v1)

## Stack
Kotlin · Jetpack Compose (Material 3) · minSdk 26 · targetSdk = latest installed stable · Gradle Kotlin DSL + version catalog · manual DI (no Hilt, keep small) · Coroutines/Flow · Firebase Auth + Firestore (no FCM sender, no Functions) · Android Keystore (AES-256-GCM `KeystoreBox`) for local secrets · R8 on release.
Package: `com.imrat.focuslock`. Modules: single `:app` module, packages `ui/ data/ lock/ service/ security/ util/`.

## Runtime pieces
- **LockAccessibilityService** — detects foreground app/window changes; if a lock applies, launches `LockActivity` (full-screen, `singleInstance`, excluded from recents, shows over keyguard only after unlock). Also blocks sensitive Settings screens (SEC-2).
- **CoreService (foreground, low-priority silent notification)** — holds Firestore snapshot listeners (own `sessions`, own `links`, own `schedules`, link alerts, and for Observers their Users' links), schedules alarms, restores state at boot, runs a 30 s housekeeping tick and the 6-hourly password-pool top-up for Observers. On API < 34 it starts without a foreground-service type (taken from the manifest).
- **ForegroundWatcher** — backup detector run by CoreService: polls UsageStats every 0.5 s only while a lock is active, the screen is on and Usage access is granted.
- **SessionStore** — encrypted local cache of the active session(s): endElapsed (monotonic), mode, packages, pwHash/salt, cooldown state. Enforcement reads ONLY this, so it works offline.
- **ScheduleAlarmReceiver / SessionEndReceiver / BootReceiver** — exact alarms for schedule starts, pre-lock warnings, session end; re-arm after reboot. Before Android 12 exact alarms need no permission (`canExact`).
- **AdminReceiver (Device Admin)** — blocks uninstall during lock.
- **CallGuard** — `AudioManager` mode (in-call / ringtone); dialer/InCallUI packages allowed only when a call is active (no phone-state permission).

## Remote lock path (no server)
Observer writes a `sessions` doc under the User's uid → User's CoreService listener fires → SessionStore updated → enforcement starts (after 60 s warning for remote-instant). Unlock = Observer sets `status=ended`. Needs User online; password path works offline.

## Firestore data model (document ids in brackets)
- `users/{uid}`: displayName, email, friendCode, createdAt, deviceId (AUTH-2).
- `lookup/{sha256(emailLower)}` and `lookup/{friendCode}` → `{uid}`. get-only (no list), authed.
- `links/{userUid}` (one Observer per User by construction): observerUid, status `pending|active|removal_requested`, createdAt, removalRequestedAt.
- `users/{uid}/apps/list`: `{items:[{n,p}], updatedAt}` (readable by uid + active Observer).
- `users/{uid}/formats/{fid}`: name, packages[], createdBy.
- `users/{uid}/schedules/{sid}`: createdBy, mode, formatId|packages[], oneTimeDate|days[], startMin, endMin, tz, enabled, pwPool `{occKey:{h,s}}` (Observer-created only).
- `users/{uid}/sessions/{sid}`: createdBy, mode `full|apps`, packages[], kind `instant|scheduled`, startAt, endAt (ms, server-time based), status `pending|active|ended`, pwHash, pwSalt, endedBy, remoteInstant(bool).

## Security Rules principles
- Default deny. `isSelf(uid)`; `isObserverOf(uid)` = `get(links/uid).observerUid == auth.uid && status in [active, removal_requested]`.
- Only the target may move a link `pending→active` or request removal; only the Observer may complete removal.
- Sessions/schedules created by Observer: User read-only, cannot update/delete. User-created: User only (Observer read).
- Observer may set session `status=ended`, with `endedBy` forced by rules to equal `auth.uid`. Password unlock is verified locally only (rules cannot check a hash). The User's app then writes an informational `passwordUsed=true` (only that field). The enforcement listener MUST end a lock from server state only when `status=ended && endedBy==observerUid`; a User cannot forge this because rules force `endedBy==auth.uid`.
- Validate field types/sizes; cap docs. Write rules tests with the Firebase emulator (Java present).

## Password handling
Generate 6 digits with `SecureRandom` on Observer phone; salt 16B; PBKDF2-HMAC-SHA256, ≥100k iterations; store `h,s` in doc; plaintext only in Observer's encrypted local store, keyed by sessionId/occKey. Constant-time compare on User phone.

## Time
On sync, store `serverOffset = serverTime − System.currentTimeMillis()` and `anchorElapsed = SystemClock.elapsedRealtime()`. Remaining = endAt − (serverNow derived from elapsedRealtime). Detect clock/timezone broadcast changes and re-derive from monotonic clock. Across reboot (elapsed resets): re-sync from server if online, else fall back to wall clock plus guard: never allow end earlier than last known remaining.

## Free-tier budget
Listeners only on own docs; Observer listens to links where observerUid==me plus each User's active session. Timers are local. Apps list uploaded only when it actually changes (hash compare). Caps enforced by rules: 500 apps per list/schedule/session, 1500 in the uploaded installed-app list, 40 password-pool entries per schedule.

## Build & release
CLI Gradle (`./gradlew assembleDebug`), install with `adb`. Release: self-signed keystore kept OUT of repo (`keystore.properties`, git-ignored); its SHA-1 must be added to the Firebase Android app; publish APK via GitHub Releases. Debug and release builds have different ANDROID_IDs (AUTH-2).
