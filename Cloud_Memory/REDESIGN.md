# REDESIGN spec (user feedback after final testing; source of truth for T14+)

## A. Visual design
Current UI is plain white/bare. Want polished, smooth, standard mobile-app feel (like ChatGPT/Claude/Grok/Facebook): Material 3 custom theme, modern colors, rounded cards, smooth transitions, comfortable spacing. Claude chooses style.

## B. Flow
1. Launch -> Sign in -> Home: blank page, only title "Focus Lock" on top, "Add device" button at bottom.
2. Add device -> page with two options: "My device" / "Remote device".
3. My device -> added to Home list directly.
4. Remote device -> page: send request by account ID or Gmail; observer accepts; requester sees "accepted"; requester must give the device a NAME; then it appears on Home.
5. Home = list of devices: "This device" + named remote devices. Each row shows live STATUS (locked / unlocked).
6. Open a device -> two main options: Instant lock, Schedule lock.
7. Observer instant lock -> target gets notification 1 min before (with 1-min countdown timer in notification if possible), locks after 1 min.
8. When a device is locked, a password is generated; ONLY the observer sees it (never the user). Observer can also unlock directly.

## C. Lock fixes
- Instant lock time picker: replace the tiny slider with TWO controls: hours + minutes (1 min must be selectable, default not 5).
- Full phone lock: Home / Recents must NOT let the user escape; remove the 1-2 s delay before the lock screen re-covers. Home/Recents allowed only in App-lock mode.
- REMOVE "Emergency unlock" button from lock screen (unlock = password or observer only).
- REMOVE emergency call buttons from lock screen.

## D. Testing
Claude should test without relying only on the user's phone: emulator/other device or local simulation. Two devices (user+observer) are needed for real end-to-end.
