# NEXT (rewrite after every finished task)

## USER ACTION NEEDED
- Before T05: plug in your Android phone, enable Developer options → USB debugging, approve this computer. Tell Claude the phone model + Android version.
- T03: in Firebase console enable Authentication→Google, and create Firestore DB (asia-south1, production mode). Links: https://console.firebase.google.com/project/focuslock-imrat-67/authentication and .../firestore. Then Claude re-downloads app/google-services.json and checks `oauth_client` is non-empty.

## Do now: T02 Skeleton
1. Create Gradle Kotlin-DSL project in the repo root: `settings.gradle.kts`, root `build.gradle.kts`, `gradle/libs.versions.toml`, `:app` module, package `com.imrat.focuslock`, minSdk 26, compile/target 36.
2. Get a Gradle wrapper (install Gradle via SDKMAN or download the distribution zip once, run `gradle wrapper`), pin latest stable AGP/Kotlin/Compose BOM compatible with JDK 21 (verify versions by web search; don't guess).
3. `gradle.properties`: `org.gradle.jvmargs=-Xmx2g`, `android.useAndroidX=true`. Write `local.properties` (git-ignored) with `sdk.dir`.
4. Minimal Compose "Focus Lock" screen; `./gradlew assembleDebug` must succeed. Record APK size in STATUS.
5. Commit `feat(T02): project skeleton`, push, mark T02 done, rewrite this file for T03 (Firebase; include exact console steps for the user).

## Notes
Phone not yet connected; no device testing before T05. Firebase CLI is installed (`firebase`), login still needed at T03.
