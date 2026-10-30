package com.imrat.focuslock.service

import android.accessibilityservice.AccessibilityService
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.imrat.focuslock.R
import com.imrat.focuslock.data.LockMode
import com.imrat.focuslock.lock.LockEnforcer
import com.imrat.focuslock.lock.LockPolicy
import com.imrat.focuslock.lock.SessionCache

/**
 * Detects the foreground app and covers it with [LockActivity] when a lock applies (LOCK-2, LOCK-3, LOCK-6).
 * During any lock it also covers Settings screens that name Focus Lock, so the lock cannot be switched off (SEC-2).
 */
class LockAccessibilityService : AccessibilityService() {
    private lateinit var cache: SessionCache
    private var lastScan = 0L

    override fun onServiceConnected() {
        cache = SessionCache(this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val pkg = event.packageName?.toString() ?: return
        val now = SystemClock.elapsedRealtime()
        val sessions = cache.get()
        val active = LockPolicy.activeSessions(sessions, now)
        if (active.isEmpty()) return
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED && LockPolicy.isRecents(pkg, event.className?.toString())) {
            if (active.any { it.mode == LockMode.FULL }) { LockEnforcer.cover(this); return }
            if (LockPolicy.recentsExposesLocked(sessions, now, screenTexts(), labelsFor(active.flatMap { it.packages }))) {
                performGlobalAction(GLOBAL_ACTION_HOME)
                LockEnforcer.cover(this)
                return
            }
        }
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            LockEnforcer.shouldCover(this, pkg, event.className?.toString(), sessions, now)
        ) { LockEnforcer.cover(this); return }
        if (LockPolicy.isSettingsLike(pkg) && now - lastScan > SCAN_GAP_MS) {
            lastScan = now
            if (LockPolicy.isProtectedScreen(pkg, screenTexts(), getString(R.string.app_name))) {
                performGlobalAction(GLOBAL_ACTION_HOME)
                LockEnforcer.cover(this)
            }
        }
    }

    private val labelCache = HashMap<String, String>()

    /** App names for [packages], cached (looked up once per package). */
    private fun labelsFor(packages: Collection<String>): Map<String, String> {
        packages.toSet().forEach { p ->
            if (p !in labelCache) runCatching { packageManager.getApplicationInfo(p, 0).loadLabel(packageManager).toString() }.getOrNull()?.let { labelCache[p] = it }
        }
        return labelCache
    }

    /** Visible text of the active window, capped so a huge list cannot stall the service. */
    private fun screenTexts(): List<String> {
        val out = ArrayList<String>()
        val root = runCatching { rootInActiveWindow }.getOrNull() ?: return out
        val stack = ArrayDeque<AccessibilityNodeInfo>().apply { add(root) }
        var seen = 0
        while (stack.isNotEmpty() && seen < MAX_NODES) {
            val n = stack.removeLast(); seen++
            n.text?.let { out.add(it.toString()) }
            n.contentDescription?.let { out.add(it.toString()) }
            for (i in 0 until n.childCount) n.getChild(i)?.let { stack.add(it) }
        }
        return out
    }

    override fun onInterrupt() = Unit

    private companion object { const val SCAN_GAP_MS = 300L; const val MAX_NODES = 400 }
}
