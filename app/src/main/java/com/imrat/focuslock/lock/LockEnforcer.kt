package com.imrat.focuslock.lock

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.PowerManager
import android.os.SystemClock
import android.telecom.TelecomManager
import android.view.inputmethod.InputMethodManager
import com.imrat.focuslock.security.LocalSession

/** Shared enforcement: decides and covers. Used by the accessibility service and the backup watcher. */
object LockEnforcer {
    fun shouldCover(ctx: Context, pkg: String, cls: String?, sessions: List<LocalSession>, now: Long): Boolean {
        if (LockPolicy.activeSessions(sessions, now).isEmpty()) return false
        val dialers = LockPolicy.DIALERS + listOfNotNull(ctx.getSystemService(TelecomManager::class.java)?.defaultDialerPackage)
        val imes = ctx.getSystemService(InputMethodManager::class.java)?.enabledInputMethodList?.map { it.packageName }.orEmpty().toSet()
        val env = LockPolicy.Env(ctx.packageName, CallGuard.isCallActive(ctx), imes, dialers)
        return LockPolicy.shouldBlock(pkg, cls, sessions, now, env)
    }

    fun cover(ctx: Context) {
        runCatching { ctx.startActivity(LockActivity.intent(ctx)) }
    }
}

/**
 * Backup to accessibility events (some phones drop or delay them): polls UsageStats for the app in front
 * while a lock is active and the screen is on. Needs Usage access; does nothing without it.
 */
class ForegroundWatcher(private val ctx: Context, private val cache: SessionCache) {
    private val usm = ctx.getSystemService(UsageStatsManager::class.java)
    private val power = ctx.getSystemService(PowerManager::class.java)
    private var lastQuery = System.currentTimeMillis() - 5_000
    private var pkg: String? = null
    private var cls: String? = null
    private var usageChecked = 0L
    private var usageOk = false

    /** Usage access, re-checked at most every 10 s (without it polling is pointless and only burns battery). */
    private fun hasUsageAccess(nowEl: Long): Boolean {
        if (nowEl - usageChecked > 10_000 || usageChecked == 0L) {
            usageOk = com.imrat.focuslock.security.Perms.isGranted(ctx, com.imrat.focuslock.security.Perm.USAGE_ACCESS)
            usageChecked = nowEl
        }
        return usageOk
    }

    /** One poll step. Returns the delay to wait before the next one. */
    fun step(): Long {
        val now = SystemClock.elapsedRealtime()
        val sessions = cache.get()
        if (LockPolicy.activeSessions(sessions, now).isEmpty() || !power.isInteractive || !hasUsageAccess(now)) {
            lastQuery = System.currentTimeMillis() - 2_000
            pkg = null
            return IDLE_MS
        }
        val wall = System.currentTimeMillis()
        runCatching {
            val ev = UsageEvents.Event()
            val events = usm.queryEvents(lastQuery - 1_000, wall)
            while (events.hasNextEvent()) {
                events.getNextEvent(ev)
                if (ev.eventType == UsageEvents.Event.ACTIVITY_RESUMED) { pkg = ev.packageName; cls = ev.className }
                else if (ev.eventType == UsageEvents.Event.ACTIVITY_PAUSED && ev.packageName == pkg) { /* keep: next resume replaces */ }
            }
        }
        lastQuery = wall
        val p = pkg
        if (p != null && shouldCoverNow(p, now, sessions)) LockEnforcer.cover(ctx)
        return ACTIVE_MS
    }

    private fun shouldCoverNow(p: String, now: Long, sessions: List<LocalSession>) = LockEnforcer.shouldCover(ctx, p, cls, sessions, now)

    companion object { const val ACTIVE_MS = 500L; const val IDLE_MS = 3_000L }
}
