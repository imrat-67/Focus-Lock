package com.imrat.focuslock.service

import android.accessibilityservice.AccessibilityService
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import com.imrat.focuslock.lock.LockEnforcer
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
        if (LockEnforcer.shouldCover(this, pkg, event.className?.toString(), sessions, now)) LockEnforcer.cover(this)
    }

    override fun onInterrupt() = Unit
}
