# NEXT (rewrite after every finished task)

## Do now: T14f (steps a-e DONE; debug build + 88 unit tests green at b404058)
Remaining: install on vivo (blocked, see below), device check of Full-lock Home/Recents re-cover, status dots, emulator-observer test, then finish T14 (mark DONE, README/SRS sweep, push).

## (old) T14 REDESIGN
User reported after final testing that UI is bad and flow/lock behaviour must change. Full spec: Cloud_Memory/REDESIGN.md (read it first).
Claude asked the user clarifying questions (self-lock without observer, Home-block method, test setup, where settings live). Apply the user's answers, then start T14 (theme first, then flow, then lock fixes).
T13 leftovers (release device pass, GitHub Release) are postponed until T14 is done.

## USER ACTION NEEDED
vivo has the signed RELEASE app installed (Device Admin active) so the new debug build can't install over it. On vivo: Settings > Security > Device admin apps > Focus Lock > Deactivate, then tell Claude. Claude then runs: adb uninstall com.imrat.focuslock && adb install -r app/build/outputs/apk/debug/app-debug.apk (re-sign-in, re-enable Accessibility + Admin after).
BACK UP ~/focuslock-keys/release.jks and keystore.properties.

## Notes
- adb: ~/Android/Sdk/platform-tools/adb (vivo connected over wireless adb).
- After any reinstall re-enable Settings > Accessibility > Focus Lock.
- Rules tests: cd rules-tests; export JAVA_HOME=~/tools/jdk-21.0.13+9; NODE_ENV=development npm i --include=dev; npm test.
