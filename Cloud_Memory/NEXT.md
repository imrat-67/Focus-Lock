# NEXT (rewrite after every finished task)

## USER ACTION NEEDED
1. Test on phone: Recents button during Full lock and Apps lock (is the delay gone? are locked apps hidden?), running-lock countdown, saved-list dropdown, red dot, Instant lock screen layout. Send screenshots of anything off.
2. Re-enable Accessibility after each install if the lock does not cover apps.

## Do now
Fix what the user reports. Then T14 close-out and T13 leftovers (release SHA-1 in Firebase, release pass, GitHub Release).

## Notes
- adb: ~/Android/Sdk/platform-tools/adb (phone paired over wifi). Env: ANDROID_HOME=~/Android/Sdk JAVA_HOME=~/tools/jdk-21.0.13+9
- Rules tests: cd rules-tests; export JAVA_HOME=~/tools/jdk-21.0.13+9; NODE_ENV=development npm i --include=dev; npm test.
