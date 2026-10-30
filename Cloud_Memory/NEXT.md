# NEXT (rewrite after every finished task)

## USER ACTION NEEDED
1. Look at the launcher icon (logo D) and the redesigned screens on the phone; send screenshots of anything that still looks off (Schedules, Formats, Remote device, Permissions).
2. Re-enable Accessibility after each install if the lock does not cover apps.

## Do now
Wait for user test feedback and fix what they report. Then T14 close-out and T13 leftovers (release SHA-1 in Firebase, release pass, GitHub Release).

## Notes
- adb: ~/Android/Sdk/platform-tools/adb (phone paired over wifi). Env: ANDROID_HOME=~/Android/Sdk JAVA_HOME=~/tools/jdk-21.0.13+9
- Rules tests: cd rules-tests; export JAVA_HOME=~/tools/jdk-21.0.13+9; NODE_ENV=development npm i --include=dev; npm test.
