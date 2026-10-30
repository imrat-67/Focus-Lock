package com.imrat.focuslock.service

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

/** Skeleton (T05): exists so the person can enable it. Foreground-app detection and blocking arrive in T06. */
class LockAccessibilityService : AccessibilityService() {
    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit
    override fun onInterrupt() = Unit
}
