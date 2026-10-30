# NEXT (rewrite after every finished task)

## USER ACTION NEEDED
1. Pick logo A, B, C or D (shown in chat). Then apply as adaptive icon, build, push.
2. Connect vivo via USB (debugging on) so the T15 build can be installed; re-enable Accessibility after install.
3. Send the screenshots of the lower parts of pages (not received) if Formats/Schedules/Remote look off.

## Do now: T15 finish
Apply chosen logo -> res/mipmap-anydpi-v26/ic_launcher.xml (+vector drawables), manifest icon. ./gradlew assembleDebug testDebugUnitTest, install, then mark T15 DONE, push.
Then T14 close-out and T13 leftovers (release SHA-1 in Firebase, release pass, GitHub Release).

## Notes
- adb: ~/Android/Sdk/platform-tools/adb. Env: ANDROID_HOME=~/Android/Sdk JAVA_HOME=~/tools/jdk-21.0.13+9
- Rules tests: cd rules-tests; export JAVA_HOME=~/tools/jdk-21.0.13+9; NODE_ENV=development npm i --include=dev; npm test.
