# NEXT (rewrite after every finished task)

## USER ACTION NEEDED
- Before T05 (lock engine): choose a test device. Preferred: real Android phone via Wi-Fi debugging (Developer options → Wireless debugging; no cable after pairing). Alternative: emulator (KVM present at /dev/kvm, ~2 GB RAM). Ask user for phone model + Android version.

## Do now: T02 Skeleton
1. Create Gradle Kotlin-DSL project in repo root: `settings.gradle.kts`, root `build.gradle.kts`, `gradle/libs.versions.toml`, `:app` module, package `com.imrat.focuslock`, minSdk 26, compile/target 36. `app/google-services.json` ALREADY EXISTS (git-ignored) — keep it; apply the google-services Gradle plugin.
2. Get a Gradle wrapper (download the Gradle distribution once, run `gradle wrapper`). Pin latest stable AGP/Kotlin/Compose BOM/Firebase BoM compatible with JDK 21 — verify versions by web search, don't guess.
3. `gradle.properties`: `org.gradle.jvmargs=-Xmx2g`, `android.useAndroidX=true`. Write `local.properties` (git-ignored) with `sdk.dir=/home/imtiaj-hossain-saikat/Android/Sdk`.
4. Minimal Compose "Focus Lock" screen; `./gradlew assembleDebug` must succeed. Record APK size in STATUS.
5. Commit `feat(T02): project skeleton`, push, mark T02 done, rewrite this file for T04 (Firestore rules + tests; real Google Sign-In UI via Credential Manager can go in T04/T05).

## Notes
Firebase: project `focuslock-imrat-67`, Firestore rules currently deny-all (placeholder). Deploy rules with `firebase deploy --only firestore:rules --project focuslock-imrat-67`. Firebase CLI is logged in as imtiajhossain7862@gmail.com (token can expire → `firebase login --reauth` by user).
