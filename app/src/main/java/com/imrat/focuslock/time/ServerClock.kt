package com.imrat.focuslock.time

import android.content.Context
import android.os.SystemClock
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/**
 * Server time derived from the monotonic clock (LOCK-8). Offset is learned from an HTTPS `Date` header
 * and persisted with its anchor; changing the device clock afterwards does not move [now].
 */
class ServerClock(ctx: Context) {
    private val prefs = ctx.applicationContext.getSharedPreferences("server_clock", Context.MODE_PRIVATE)

    /** True once at least one sync happened since the last boot (anchor is valid on this boot's monotonic clock). */
    val synced: Boolean
        get() = prefs.getLong("anchorEl", -1L) in 0..SystemClock.elapsedRealtime()

    /** Call from the boot receiver: the monotonic clock restarted, so the old anchor is meaningless. */
    fun invalidate() = prefs.edit().remove("anchorEl").apply()

    fun now(): Long {
        val el = SystemClock.elapsedRealtime()
        return if (synced) {
            TimeModel.serverNow(prefs.getLong("offset", 0L), prefs.getLong("anchorEl", 0L), prefs.getLong("anchorWall", 0L), el)
        } else {
            System.currentTimeMillis()
        }
    }

    /** Blocking network call: run off the main thread. Returns true on success. */
    fun sync(): Boolean = try {
        val c = URL("https://www.google.com/generate_204").openConnection() as HttpURLConnection
        c.requestMethod = "HEAD"; c.connectTimeout = 6000; c.readTimeout = 6000; c.instanceFollowRedirects = false
        val t0 = SystemClock.elapsedRealtime()
        c.connect(); c.responseCode
        val rtt = SystemClock.elapsedRealtime() - t0
        val date = c.getHeaderField("Date"); c.disconnect()
        val server = parse(date) ?: return false
        val el = SystemClock.elapsedRealtime()
        val wall = System.currentTimeMillis()
        prefs.edit()
            .putLong("offset", server + rtt / 2 - wall).putLong("anchorEl", el).putLong("anchorWall", wall)
            .apply()
        true
    } catch (_: Exception) {
        false
    }

    companion object {
        fun parse(date: String?): Long? = try {
            if (date == null) null else SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US)
                .apply { timeZone = TimeZone.getTimeZone("GMT") }.parse(date)?.time
        } catch (_: Exception) {
            null
        }
    }
}
