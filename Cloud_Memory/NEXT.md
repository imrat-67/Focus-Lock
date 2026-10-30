# NEXT (rewrite after every finished task)

## USER ACTION NEEDED
1. Firebase console > Project settings > Android app > Add fingerprint SHA-1: 52:42:11:0A:7C:6F:A2:C4:31:F2:7F:DF:C9:3E:19:D5:C2:D3:AB:E8 (release key). Then re-download google-services.json into app/ and tell Claude.
2. BACK UP ~/focuslock-keys/release.jks and keystore.properties (lose them = cannot update the app).
3. Release APK: app/build/outputs/apk/release/app-release.apk (3.2 MB). Installing it needs `adb uninstall com.imrat.focuslock` first (different signing key than debug).

## Do now: T13 rest
Rebuild release with new google-services.json, install, device pass (sign-in, lock, reboot, clock change, scheduled start, SEC-2), then GitHub Release (gh missing: user uploads APK on github.com Releases, tag v0.1.0).

## Notes
- Debug lock helpers only in debug build: DEBUG_LOCK/DEBUG_CLEAR via DebugSessionReceiver (adb at ~/Android/Sdk/platform-tools/adb).
- After any reinstall re-enable Settings > Accessibility > Focus Lock.
- Known: first tap on Unlock after typing only hides keyboard on vivo; LockScreen keeps `outcome` after session end; confirm boot rebase uses UnlockPolicy.afterBoot.
- Rules tests: cd rules-tests; export JAVA_HOME=~/tools/jdk-21.0.13+9; NODE_ENV=development npm i --include=dev; npm test.
