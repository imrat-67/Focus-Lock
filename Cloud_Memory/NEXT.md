# NEXT (rewrite after every finished task)

## USER ACTION NEEDED
- Before T05 (lock engine): choose test device. Preferred: real phone via Wi-Fi debugging (Developer options → Wireless debugging, pair once, no cable). Alternative: emulator (/dev/kvm present, ~2 GB RAM). Need phone model + Android version.

## Do now: T04 Data layer + Firestore rules
1. Add deps: Firebase BoM 35.1.0 (firebase-auth, firebase-firestore), kotlinx-coroutines-play-services if needed, credentials 1.6.0 + credentials-play-services-auth + googleid 1.2.1 (verify exact artifact names at dl.google.com/dl/android/maven2 before use).
2. Implement Google Sign-In (Credential Manager → Firebase Auth) with a simple Sign-in screen + signed-in Home placeholder; creates/updates `users/{uid}` (displayName, email, friendCode, deviceId) and `lookup/*` docs per docs/ARCHITECTURE.md.
3. Write real `firestore.rules` per ARCHITECTURE "Security Rules principles" + emulator tests (firebase emulators need Java: use ~/tools/jdk-21*; node 22 present). Deploy with `firebase deploy --only firestore:rules --project focuslock-imrat-67`.
4. Data models + repositories (no UI beyond sign-in). Unit tests for pure logic.
5. Commit per sub-step (`wip(T04)`), `feat(T04)` at end, update STATUS/NEXT.

## Notes
Firebase project `focuslock-imrat-67`; rules currently deny-all placeholder. CLI login may need `firebase login --reauth` by user. Never commit app/google-services.json.
