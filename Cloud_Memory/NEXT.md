# NEXT (rewrite after every finished task)

## USER ACTION NEEDED
1. BACK UP ~/focuslock-keys/release.jks and keystore.properties (lose them = cannot update the app).
2. Phone: sign in on release app (Accessibility + Device admin permissions after).

## Do now: T13 rest
Release APK installed on vivo (SHA-1 added). ANDROID_ID differs per signing key, so debug-era users/{uid}+lookup docs were deleted (2 test accounts) to let release re-register; links/formats stay. Known limit: deviceId changes if signing key changes (AUTH-2 phone-change flow is later).
Next: device pass (sign-in, lock, reboot, clock change, scheduled start, SEC-2), then GitHub Release (gh missing: user uploads APK on github.com Releases, tag v0.1.0).

## Notes
- Debug lock helpers only in debug build: DEBUG_LOCK/DEBUG_CLEAR via DebugSessionReceiver (adb at ~/Android/Sdk/platform-tools/adb).
- After any reinstall re-enable Settings > Accessibility > Focus Lock.
- Known: first tap on Unlock after typing only hides keyboard on vivo; LockScreen keeps `outcome` after session end; confirm boot rebase uses UnlockPolicy.afterBoot.
- Rules tests: cd rules-tests; export JAVA_HOME=~/tools/jdk-21.0.13+9; NODE_ENV=development npm i --include=dev; npm test.
