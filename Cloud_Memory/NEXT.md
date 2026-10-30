# NEXT (rewrite after every finished task)

## Do now: finish T14
User device-tests T14g on vivo (debug build installed). Apply any feedback, then mark T14 DONE, README sweep, push.
Then T13 leftovers: release SHA-1 in Firebase, release device pass, GitHub Release.

## USER ACTION NEEDED
Test on vivo: (1) send link request from 2nd account -> heads-up notification + red dot on profile icon (allow Notifications permission; new channel alerts_v2) (2) Apps lock from Instant lock does NOT cover Focus Lock, toast shows (3) status pill: Full lock / Selected apps locked (4) default 1 min (5) one-time schedule shows calendar.
Back up ~/focuslock-keys/release.jks + keystore.properties.

## Notes
- adb: ~/Android/Sdk/platform-tools/adb. After reinstall re-enable Accessibility.
- Rules tests: cd rules-tests; export JAVA_HOME=~/tools/jdk-21.0.13+9; NODE_ENV=development npm i --include=dev; npm test.
