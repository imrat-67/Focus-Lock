# NEXT (rewrite after every finished task)

## USER ACTION NEEDED
1. On phone: Settings > Security (or Device admin apps / Special access) > Device admin apps > Focus Lock > Deactivate. (adb cannot remove it: non-test admin.) If a lock session is active, end it first (Emergency password/Observer).
2. BACK UP ~/focuslock-keys/release.jks and keystore.properties (lose them = cannot update the app).
Then tell Claude "admin off".

## Do now: T13 rest
Release SHA-1 DONE (google-services.json has 2 hashes), release APK rebuilt (app/build/outputs/apk/release/app-release.apk, 3.2 MB).
Next: `adb uninstall com.imrat.focuslock && adb install app/build/outputs/apk/release/app-release.apk`, re-enable Accessibility + Device admin, device pass (sign-in, lock, reboot, clock change, scheduled start, SEC-2), then GitHub Release (gh missing: user uploads APK on github.com Releases, tag v0.1.0).

## Notes
- Debug lock helpers only in debug build: DEBUG_LOCK/DEBUG_CLEAR via DebugSessionReceiver (adb at ~/Android/Sdk/platform-tools/adb).
- After any reinstall re-enable Settings > Accessibility > Focus Lock.
- Known: first tap on Unlock after typing only hides keyboard on vivo; LockScreen keeps `outcome` after session end; confirm boot rebase uses UnlockPolicy.afterBoot.
- Rules tests: cd rules-tests; export JAVA_HOME=~/tools/jdk-21.0.13+9; NODE_ENV=development npm i --include=dev; npm test.
