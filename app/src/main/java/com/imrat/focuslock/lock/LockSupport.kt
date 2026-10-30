package com.imrat.focuslock.lock

import android.content.Context
import android.media.AudioManager
import android.os.SystemClock
import com.imrat.focuslock.security.KeystoreBox
import com.imrat.focuslock.security.LocalSession
import com.imrat.focuslock.security.SessionStore
import java.io.File

/** Reads the encrypted session file, re-decrypting only when it changed. */
class SessionCache(ctx: Context) {
    private val file = File(ctx.applicationContext.filesDir, "sessions.bin")
    private val store = SessionStore(file, KeystoreBox.create())
    private var stamp = Pair(-1L, -1L)
    private var cached: List<LocalSession> = emptyList()

    @Synchronized
    fun get(): List<LocalSession> {
        val now = Pair(file.lastModified(), file.length())
        if (now != stamp) {
            cached = store.load()
            stamp = now
        }
        return cached
    }
}

object CallGuard {
    @Suppress("DEPRECATION")
    fun isCallActive(ctx: Context): Boolean {
        val audio = ctx.getSystemService(AudioManager::class.java)
        return audio != null && audio.mode in setOf(AudioManager.MODE_IN_CALL, AudioManager.MODE_RINGTONE)
    }
}
