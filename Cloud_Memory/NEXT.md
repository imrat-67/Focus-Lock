# NEXT (rewrite after every finished task)

## USER ACTION NEEDED
1. On the vivo: Settings > Security/Device admin apps > Focus Lock > Deactivate, then uninstall the old debug Focus Lock (adb cannot do it). Then tell Claude to install release/FocusLock-0.1.0.apk (or install it yourself).
2. Google Cloud console > OAuth consent screen: "In production" (or add friends as test users), else friends cannot sign in.
3. Share release/FocusLock-0.1.0.apk (sha256 in the .sha256 file). Friends: allow unknown sources, then follow README "First run on Android 13+".

## Do now
After the old app is gone: adb install release APK, smoke test sign-in + Lock now on the release build, then optional GitHub Release.

## Notes
- adb: ~/Android/Sdk/platform-tools/adb. Env: ANDROID_HOME=~/Android/Sdk JAVA_HOME=~/tools/jdk-21.0.13+9
- Firebase data is empty (fresh). Never share a debug APK (exported DEBUG_LOCK receiver).
