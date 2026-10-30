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

    /** Session shown on the lock screen: the one ending last. */
    fun displayed(sessions: List<LocalSession>, nowElapsedMs: Long): LocalSession? =
        activeSessions(sessions, nowElapsedMs).maxByOrNull { it.endElapsedMs }
}
