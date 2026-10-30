package com.imrat.focuslock.lock

import android.content.Context
import android.content.Intent
import com.imrat.focuslock.data.AppEntry
import com.imrat.focuslock.data.AppListCodec

/** Launchable apps on this phone (name + package only, FMT-3). */
object InstalledApps {
    fun load(ctx: Context): List<AppEntry> {
        val pm = ctx.packageManager
        val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val apps = pm.queryIntentActivities(launcher, 0).map { AppEntry(it.loadLabel(pm).toString(), it.activityInfo.packageName) }
        return AppListCodec.normalize(apps, excludePkg = ctx.packageName)
    }
}
