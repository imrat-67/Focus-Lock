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
