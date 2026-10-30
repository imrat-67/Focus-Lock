package com.imrat.focuslock.lock

import com.imrat.focuslock.data.LockMode
import com.imrat.focuslock.security.LocalSession

/** Pure decision logic: should the lock screen cover [pkg] right now? (LOCK-2, LOCK-3, LOCK-9) */
object LockPolicy {
    data class Env(
        val ownPackage: String,
        val callActive: Boolean,
        val imePackages: Set<String> = emptySet(),
        val dialerPackages: Set<String> = DIALERS,
    )

    /** System UI stays usable so notifications are never hidden (LOCK-9). */
    val ALWAYS_ALLOWED = setOf("com.android.systemui")
    val DIALERS = setOf(
        "com.android.incallui", "com.android.dialer", "com.google.android.dialer",
        "com.android.server.telecom", "com.samsung.android.incallui", "com.vivo.incallui",
    )
    val CLOCKS = setOf("com.android.deskclock", "com.google.android.deskclock", "com.vivo.alarmclock")

    fun activeSessions(all: List<LocalSession>, nowElapsedMs: Long): List<LocalSession> =
        all.filter { it.endElapsedMs > nowElapsedMs }

    fun shouldBlock(pkg: String, cls: String?, sessions: List<LocalSession>, nowElapsedMs: Long, env: Env): Boolean {
        val active = activeSessions(sessions, nowElapsedMs)
        if (active.isEmpty()) return false
        if (pkg == env.ownPackage || pkg in ALWAYS_ALLOWED || pkg in env.imePackages) return false
        if (pkg in env.dialerPackages && env.callActive) return false
        if (pkg in CLOCKS && isAlarmRinging(cls)) return false
        return active.any { it.mode == LockMode.FULL || pkg in it.packages }
    }

    /** Heuristic: the clock app's alarm alert screen (class name contains "alert"/"ring"). Recorded in DECISIONS. */
    fun isAlarmRinging(cls: String?): Boolean {
        val c = cls?.lowercase() ?: return false
        return "alert" in c || "ring" in c
    }

    /**
     * Home/Recents handling: during a FULL lock the lock screen must come back at once; during an Apps lock the user
     * may leave it (only the blocked apps are covered, by shouldBlock). User-requested in T14.
     */
    fun shouldRecoverOnLeave(sessions: List<LocalSession>, nowElapsedMs: Long): Boolean =
        activeSessions(sessions, nowElapsedMs).any { it.mode == LockMode.FULL }

    /** True for the Recents / app-switcher screen (lives in the launcher or System UI depending on the phone). */
    fun isRecents(pkg: String, cls: String?): Boolean {
        val c = cls?.lowercase() ?: return false
        return "recent" in c && (pkg == "com.android.systemui" || "launcher" in pkg.lowercase() || "home" in pkg.lowercase())
    }

    /**
     * Recents must be covered when it would show a locked app: always during a FULL lock, and during an Apps lock
     * when a blocked app's name appears on screen ([labels] maps package -> app name).
     */
    fun recentsExposesLocked(sessions: List<LocalSession>, nowElapsedMs: Long, texts: List<String>, labels: Map<String, String>): Boolean {
        val active = activeSessions(sessions, nowElapsedMs)
        if (active.any { it.mode == LockMode.FULL }) return true
        val blocked = active.flatMap { it.packages }.toSet()
        return blocked.any { p ->
            val name = labels[p] ?: return@any false
            texts.any { it.contains(name, ignoreCase = true) }
        }
    }

    /** Session shown on the lock screen: the one ending last. */
    fun displayed(sessions: List<LocalSession>, nowElapsedMs: Long): LocalSession? =
        activeSessions(sessions, nowElapsedMs).maxByOrNull { it.endElapsedMs }

    // ---- SEC-2: screens that could switch Focus Lock off while a lock is active ----
    private val SETTINGS_HINTS = listOf("settings", "packageinstaller", "permissioncontroller", "securitycenter", "iqoo.secure", "permissionmanager")

    /** True for Settings/installer packages (the only places that can deactivate, uninstall or force-stop an app). */
    fun isSettingsLike(pkg: String): Boolean = pkg != "com.android.systemui" && SETTINGS_HINTS.any { it in pkg.lowercase() }

    /**
     * True when a Settings-like screen shows our [appLabel] anywhere (device-admin, app info, uninstall, accessibility,
     * storage ... OEM skins rename the classes, so the name on screen is the reliable signal). Screens that never show
     * our name (Wi-Fi, battery ...) stay usable.
     */
    fun isProtectedScreen(pkg: String, texts: List<String>, appLabel: String): Boolean =
        isSettingsLike(pkg) && texts.any { it.contains(appLabel, ignoreCase = true) }
}
