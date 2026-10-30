package com.imrat.focuslock.service

import android.accessibilityservice.AccessibilityService
import android.os.SystemClock
import android.telecom.TelecomManager
import android.view.accessibility.AccessibilityEvent
import android.view.inputmethod.InputMethodManager
import com.imrat.focuslock.lock.CallGuard
import com.imrat.focuslock.lock.LockActivity
import com.imrat.focuslock.lock.LockPolicy
import com.imrat.focuslock.lock.SessionCache

/** Detects the foreground app and covers it with [LockActivity] when a lock applies (LOCK-2, LOCK-3, LOCK-6). */
class LockAccessibilityService : AccessibilityService() {
    private lateinit var cache: SessionCache

    override fun onServiceConnected() {
        cache = SessionCache(this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        val now = SystemClock.elapsedRealtime()
        val sessions = cache.get()
        if (LockPolicy.activeSessions(sessions, now).isEmpty()) return
        val dialers = LockPolicy.DIALERS + listOfNotNull(getSystemService(TelecomManager::class.java)?.defaultDialerPackage)
        val imes = getSystemService(InputMethodManager::class.java)?.enabledInputMethodList?.map { it.packageName }.orEmpty().toSet()
        val env = LockPolicy.Env(packageName, CallGuard.isCallActive(this), imes, dialers)
        if (LockPolicy.shouldBlock(pkg, event.className?.toString(), sessions, now, env)) {
            startActivity(LockActivity.intent(this))
        }
    }

    override fun onInterrupt() = Unit
}
