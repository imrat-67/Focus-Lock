package com.imrat.focuslock.security

import android.Manifest
import android.app.AlarmManager
import android.app.AppOpsManager
import android.app.NotificationManager
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.os.Process
import android.provider.Settings
import androidx.annotation.StringRes
import com.imrat.focuslock.R
import com.imrat.focuslock.service.AdminReceiver
import com.imrat.focuslock.service.LockAccessibilityService

/** Items of the first-run checklist (UI-2). "Display over apps" is not needed: the lock is a full-screen Activity. */
enum class Perm(@StringRes val title: Int, @StringRes val why: Int) {
    ACCESSIBILITY(R.string.perm_accessibility, R.string.perm_accessibility_why),
    DEVICE_ADMIN(R.string.perm_admin, R.string.perm_admin_why),
    NOTIFICATIONS(R.string.perm_notifications, R.string.perm_notifications_why),
    EXACT_ALARMS(R.string.perm_alarms, R.string.perm_alarms_why),
    BATTERY(R.string.perm_battery, R.string.perm_battery_why),
    USAGE_ACCESS(R.string.perm_usage, R.string.perm_usage_why),
    PHONE_STATE(R.string.perm_phone, R.string.perm_phone_why),
}

object Perms {

    fun isGranted(c: Context, p: Perm): Boolean = when (p) {
        Perm.ACCESSIBILITY -> accessibilityEnabled(c)
        Perm.DEVICE_ADMIN -> c.getSystemService(DevicePolicyManager::class.java)
            .isAdminActive(ComponentName(c, AdminReceiver::class.java))
        Perm.NOTIFICATIONS -> c.getSystemService(NotificationManager::class.java).areNotificationsEnabled()
        Perm.EXACT_ALARMS -> Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            c.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()
        Perm.BATTERY -> c.getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(c.packageName)
        Perm.USAGE_ACCESS -> usageAccessGranted(c)
        Perm.PHONE_STATE -> c.checkSelfPermission(Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED
    }

    fun missing(c: Context): List<Perm> = Perm.entries.filterNot { isGranted(c, it) }

    /** Runtime permission to ask in-app, or null when the item is opened through a Settings screen. */
    fun runtimePermission(p: Perm): String? = when (p) {
        Perm.NOTIFICATIONS -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Manifest.permission.POST_NOTIFICATIONS else null
        Perm.PHONE_STATE -> Manifest.permission.READ_PHONE_STATE
        else -> null
    }

    /** Settings screen that lets the person grant [p]. */
    fun settingsIntent(c: Context, p: Perm): Intent {
        val pkg = Uri.parse("package:${c.packageName}")
        return when (p) {
            Perm.ACCESSIBILITY -> Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            Perm.DEVICE_ADMIN -> Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
                .putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, ComponentName(c, AdminReceiver::class.java))
                .putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, c.getString(R.string.perm_admin_why))
            Perm.NOTIFICATIONS -> Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, c.packageName)
            Perm.EXACT_ALARMS -> Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, pkg)
            Perm.BATTERY -> Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, pkg)
            Perm.USAGE_ACCESS -> Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS, pkg)
            Perm.PHONE_STATE -> Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, pkg)
        }
    }

    private fun accessibilityEnabled(c: Context): Boolean {
        val me = ComponentName(c, LockAccessibilityService::class.java)
        val list = Settings.Secure.getString(c.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: return false
        return list.split(':').any { ComponentName.unflattenFromString(it) == me }
    }

    @Suppress("DEPRECATION")
    private fun usageAccessGranted(c: Context): Boolean {
        val ops = c.getSystemService(AppOpsManager::class.java)
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ops.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), c.packageName)
        } else {
            ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), c.packageName)
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }
}
