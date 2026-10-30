package com.imrat.focuslock.util

import android.content.Context
import android.provider.Settings

object DeviceId {
    /** Stable per phone (and per app signing key), survives reinstall; changes on factory reset. */
    fun get(context: Context): String =
        Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID).orEmpty().ifEmpty { "unknown" }
}
