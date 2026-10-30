package com.imrat.focuslock.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.imrat.focuslock.FocusLockApp
import com.imrat.focuslock.data.AppListCodec
import com.imrat.focuslock.lock.InstalledApps
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Uploads the launchable-app list when it changed: on start and whenever apps are installed/removed (FMT-3). */
class AppListSync(private val ctx: Context, private val scope: CoroutineScope) {
    private var job: Job? = null
    private val prefs = ctx.getSharedPreferences("applist", Context.MODE_PRIVATE)
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context, i: Intent) = requestSync()
    }

    fun start() {
        val f = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED); addAction(Intent.ACTION_PACKAGE_REMOVED); addDataScheme("package")
        }
        ctx.registerReceiver(receiver, f)
        requestSync()
    }

    fun stop() {
        runCatching { ctx.unregisterReceiver(receiver) }
        job?.cancel()
    }

    /** Debounced; skips the upload when the list is unchanged. */
    fun requestSync() {
        job?.cancel()
        job = scope.launch {
            delay(2_000)
            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@launch
            val apps = InstalledApps.load(ctx)
            val fp = uid + AppListCodec.fingerprint(apps)
            if (prefs.getString("fp", null) == fp) return@launch
            try {
                (ctx.applicationContext as FocusLockApp).container.appList.upload(uid, apps)
                prefs.edit().putString("fp", fp).apply()
                Log.i("AppListSync", "uploaded ${apps.size} apps")
            } catch (e: Exception) {
                Log.w("AppListSync", "upload failed: ${e.message}")
            }
        }
    }
}
