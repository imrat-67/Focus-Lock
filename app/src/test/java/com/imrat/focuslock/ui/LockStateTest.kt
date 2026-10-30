package com.imrat.focuslock.ui

import com.imrat.focuslock.data.LockMode
import org.junit.Assert.assertEquals
import org.junit.Test

class LockStateTest {
    @Test fun none_is_unlocked() = assertEquals(LockState.UNLOCKED, LockState.of(emptyList()))
    @Test fun apps_only_is_apps() = assertEquals(LockState.APPS, LockState.of(listOf(LockMode.APPS)))
    @Test fun full_wins_over_apps() = assertEquals(LockState.FULL, LockState.of(listOf(LockMode.APPS, LockMode.FULL)))
}
