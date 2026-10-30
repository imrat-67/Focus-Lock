# DECISIONS (append-only, one line each: ID · decision · why)

D01 · Sideloaded APK via GitHub Releases, not Play Store · free; Play restricts Accessibility/Device Admin apps
D02 · Remote lock via Firestore snapshot listeners in a foreground service, no FCM sender/Cloud Functions · FCM send needs a secret key or Blaze billing
D03 · Lock UI = full-screen LockActivity launched by Accessibility service, not a plain overlay · overlays unreliable on Android 12+
D04 · Password: generated on Observer phone, only salted PBKDF2 hash on User phone, 5 tries → 10 min cooldown · Observer-only secrecy; slows brute force
D05 · Self-lock has no password; Emergency exit = 5-min countdown · prevents being trapped when no Observer
D06 · Max session 12 h · limits Observer abuse / mistakes
D07 · `links/{userUid}` doc id = User uid · enforces one Observer per User in data model
D08 · Recurring schedules: Observer phone pre-generates 14 occurrence password hashes · password per session without Observer being online at start
D09 · One account = one phone · simplifies device model
D10 · Formats stored under `users/{uid}/formats`, editable by User and Observer · simple per-pair semantics with one Observer
D11 · Removal request auto-completes after 7 days; blocked during active lock · prevents permanent stuck state without undermining a running lock
D12 · Single `:app` module, manual DI, no Hilt · small APK, fewer moving parts
D13 · Not 100% bypass-proof (Safe Mode/ADB/root/factory reset) · Android limitation; documented in SRS SEC-6
D14 · compileSdk/targetSdk 36, build-tools 36.1.0, SDK in ~/Android/Sdk (cmdline-tools build 14742923) · latest stable at setup time
D15 · Firebase project `focuslock-imrat-67` (app id 1:379550728749:android:fe14974400c5decbdc6ea4, pkg com.imrat.focuslock), Firestore region asia-south1, debug SHA1 87:49:8B:64:25:58:DC:16:33:35:06:F6:C0:FF:D7:7E:FE:49:AA:63 registered · created via CLI; app/google-services.json is git-ignored (re-download with `firebase apps:sdkconfig ANDROID <appId> --project focuslock-imrat-67 --out app/google-services.json`)
D16 · Versions (verified from Maven/Gradle on 2026-10-30): AGP 9.5.0, Gradle 9.8.2 (wrapper), Kotlin 2.4.21 (AGP 9 has built-in Kotlin: no kotlin-android plugin; compose compiler plugin only), Compose BOM 2026.09.00, activity-compose 1.13.0, google-services 4.5.0. Planned for later tasks: Firebase BoM 35.1.0, credentials 1.6.0, googleid 1.2.1 · latest stable at setup
D17 · compileSdk 37.2 (Compose BOM requires >=37), targetSdk 36, minSdk 26 · compileSdk only unlocks APIs; keeping target 36 avoids new Android 17 behavior changes in lock enforcement until tested
D18 · Build JDK = portable Temurin 21 at ~/tools/jdk-21.0.13+9 (system has only a JRE, sudo is blocked in Desktop Commander), set via ~/.gradle/gradle.properties (org.gradle.java.home, outside repo). Build: `cd project && ./gradlew assembleDebug --no-daemon` (~30 s warm, ~2.5 min cold). Run long builds in background: `(nohup ./gradlew ... > /tmp/b.log 2>&1 &)` then poll.
D19 · T04 versions (verified 2026-10-30, Google Maven/Central): Firebase BoM 35.1.0, credentials + credentials-play-services-auth 1.6.0 (1.7.0 is alpha), googleid 1.2.1 (group com.google.android.libraries.identity.googleid), lifecycle-viewmodel-compose/runtime-compose 2.11.0 (2.12 is alpha), kotlinx-coroutines-play-services 1.11.0, JUnit 4.13.2
D20 · Rules: link doc id = userUid (one Observer per User); Observer creates `pending`, User accepts. lookup docs are get-only; key must equal sha256(lower(email)) or the user's friendCode (checked via getAfter). LINK-7 (no removal during lock) cannot be checked in rules, enforced client-side. AUTH-2 deviceId check is client-side (rules cannot stop a second phone); phone-migration flow deferred to T13.
D21 · deviceId = ANDROID_ID, immutable in rules; LINK-3 enforced by swallowing PERMISSION_DENIED in LinkRepository.request (generic 'request sent' UI). Debug APK 19 MB.
