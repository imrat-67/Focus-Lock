package com.imrat.focuslock.lock

import android.app.AlarmManager
import android.os.Build

/** Exact alarms need no permission before Android 12 (API 31); `canScheduleExactAlarms` does not exist there. */
fun canExact(am: AlarmManager): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()
