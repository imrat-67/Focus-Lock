# NEXT (rewrite after every finished task)

## USER ACTION NEEDED
- Before T05: plug in your Android phone, enable Developer options → USB debugging, approve the computer. Tell Claude the phone model + Android version.
- Before T03: (Claude will give click-by-click steps) create a Firebase project, enable Google sign-in.

## Do now: T01 Toolchain
1. Download Android command-line tools (Linux zip, from developer.android.com/studio#command-line-tools-only) to ~/Android/Sdk/cmdline-tools/latest.
2. `sdkmanager --licenses`; install `platform-tools`, latest stable `platforms;android-NN`, matching `build-tools;NN.x`.
3. Export ANDROID_HOME + PATH in ~/.bashrc; verify `adb version`.
4. Update STATUS (T01 done), commit `chore(T01): android sdk`, push, then rewrite this file for T02.

## Notes
Repo already has initial commit with docs. Network may be needed for downloads; if blocked, report the exact error to the user.
