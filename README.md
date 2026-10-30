# Focus Lock

Free Android app that helps students focus by locking their phone (whole phone or chosen apps), now or on a schedule — by themselves or remotely by one consenting Observer (parent/friend). An emergency 6-digit password or an Observer remote-unlock ends a lock.

No ads, no premium, no stats. Firebase Spark (free) only, no Cloud Functions.

## Features
- Full Lock or chosen apps; instant or scheduled; timer and 1-minute warning
- One Observer per student (consent-based link); Observer can lock/unlock remotely and manage schedules
- Per-session emergency password held only by the Observer
- Works offline from an encrypted local cache; survives reboot and clock changes
- Device Admin + Accessibility hardening: blocks uninstall, force-stop and sign-out during a lock

## Honest limits
Android cannot be 100% bypass-proof: Safe Mode, ADB/PC tools, root and factory reset defeat any normal app.

## Install
Android 8.0+. Download the APK from Releases, allow install from unknown sources, open the app, sign in with Google, then follow the permissions checklist (Accessibility, overlay, Device Admin, notifications, battery).

## Build
```
export ANDROID_HOME=~/Android/Sdk
./gradlew testDebugUnitTest assembleDebug   # debug APK
./gradlew assembleRelease                   # needs keystore.properties (git-ignored)
```
Your own Firebase project is required: put its `google-services.json` in `app/` (git-ignored) and deploy `firestore.rules`. Rules tests: `cd rules-tests && npm i --include=dev && npm test` (needs JDK 21).

## Docs
[SRS.md](SRS.md) · [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) · AI-assisted development entry point: [Cloud_Memory/START_HERE.md](Cloud_Memory/START_HERE.md)
