# NEXT (rewrite after every finished task)

## USER ACTION NEEDED
1. Two-account test with a friend on release/FocusLock-0.1.0.apk: link request/accept, remote lock, password unlock, Observer unlock, scheduled lock, reboot during lock.
2. Report any failure to Claude (phone connected via adb for logs).
3. Share the APK with friends only after step 1 passes.

## Do now
Fix what the user reports. Optional: GitHub Release with APK + sha256.

## Notes
- adb: ~/Android/Sdk/platform-tools/adb. Env: ANDROID_HOME=~/Android/Sdk JAVA_HOME=~/tools/jdk-21.0.13+9
- OAuth consent is In production (External). Release SHA-1 registered. Firebase data fresh at T21.
