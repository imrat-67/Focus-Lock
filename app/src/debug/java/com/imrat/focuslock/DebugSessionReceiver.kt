package com.imrat.focuslock

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import com.imrat.focuslock.data.LockMode
import com.imrat.focuslock.security.KeystoreBox
import com.imrat.focuslock.security.LocalSession
import com.imrat.focuslock.security.SessionStore
import com.google.firebase.auth.FirebaseAuth
import com.imrat.focuslock.security.PasswordCrypto
import java.io.File

/** DEBUG BUILDS ONLY. adb shell am broadcast -a com.imrat.focuslock.DEBUG_LOCK -n com.imrat.focuslock/.DebugSessionReceiver --es mode full --ei secs 120 --es pkgs a.b,c.d ; DEBUG_CLEAR removes it. Extras: --es pw 123456 (emergency password), --ez self true (made by this user, no password = emergency exit). */
class DebugSessionReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        val store = SessionStore(File(ctx.filesDir, "sessions.bin"), KeystoreBox.create())
        if (intent.action == "com.imrat.focuslock.DEBUG_CLEAR") { store.save(emptyList()); return }
        val secs = intent.getIntExtra("secs", 120)
        val mode = if (intent.getStringExtra("mode") == "apps") LockMode.APPS else LockMode.FULL
        val pkgs = intent.getStringExtra("pkgs")?.split(",")?.filter { it.isNotBlank() }.orEmpty()
        val self = intent.getBooleanExtra("self", false)
        val by = if (self) FirebaseAuth.getInstance().currentUser?.uid ?: "debug" else "debug"
        val pw = intent.getStringExtra("pw")
        val salt = pw?.let { PasswordCrypto.newSalt() }
        val hash = pw?.let { PasswordCrypto.hash(it, salt!!) }
        store.save(listOf(LocalSession("debug", by, mode, pkgs, System.currentTimeMillis() + secs * 1000L,
            SystemClock.elapsedRealtime() + secs * 1000L, hash, salt)))
    }
}
