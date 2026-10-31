# NEXT (rewrite after every finished task)

## USER ACTION NEEDED
1. Confirm the T20 audit report so Claude can build the shareable release APK (R8, signed, ~3 MB).
2. Accounts first used with the DEBUG build will show "other device" on the RELEASE build (ANDROID_ID is per signing key). Either use fresh accounts for the release test, or let Claude delete their users/{uid} docs with the Firebase CLI.
3. Google Cloud console > OAuth consent screen: make sure it is "In production" (or add friends as test users), otherwise friends cannot sign in.
4. Not eyeballed yet on phone: Schedules rows, notification icon/alerts, two-account Observer flow.

## Do now (after user confirms)
./gradlew testDebugUnitTest assembleRelease  (needs keystore.properties), verify APK signature with apksigner, install release on phone, smoke test sign-in + Lock now, copy APK to ~/Documents/project/Focus Lock/release/ (git-ignored) or GitHub Release.

## Notes
- adb: ~/Android/Sdk/platform-tools/adb (phone paired over wifi). Env: ANDROID_HOME=~/Android/Sdk JAVA_HOME=~/tools/jdk-21.0.13+9
- Rules tests: cd rules-tests; export JAVA_HOME=~/tools/jdk-21.0.13+9; npm test.
- Debug build has an exported DEBUG_LOCK receiver (src/debug only). Never share a debug APK.
