# ISSUES (open problems only; delete when fixed)

- RISK: OEM battery killers (Xiaomi/Oppo/Vivo/Samsung) may stop CoreService; need battery-exemption + autostart guidance in permissions checklist (T05). Test phone model unknown.
- RISK: Accessibility service can be disabled by the user in Settings when no lock is active; app must detect and nag/relock (T12).
- RISK: Offline brute-force of a 6-digit PBKDF2 hash is possible for a technical user (accepted, SEC-6).
