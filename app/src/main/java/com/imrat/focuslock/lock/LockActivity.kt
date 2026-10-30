package com.imrat.focuslock.lock

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.imrat.focuslock.R
import com.imrat.focuslock.data.LockMode
import com.imrat.focuslock.security.LocalSession
import kotlinx.coroutines.delay

/** The lock screen (LOCK-4): countdown, who locked, mode, password field, emergency call. Nothing else. */
class LockActivity : ComponentActivity() {
    private lateinit var cache: SessionCache

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cache = SessionCache(this)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = Unit
        })
        setContent {
            MaterialTheme {
                var session by remember { mutableStateOf<LocalSession?>(null) }
                var remaining by remember { mutableLongStateOf(0L) }
                LaunchedEffect(Unit) {
                    while (true) {
                        val now = SystemClock.elapsedRealtime()
                        val s = LockPolicy.displayed(cache.get(), now)
                        if (s == null) { finishAndRemoveTask(); break }
                        session = s
                        remaining = s.endElapsedMs - now
                        delay(500)
                    }
                }
                session?.let { LockScreen(it, remaining, ::emergencyCall) }
            }
        }
    }

    /** Pressing Home/Recents stops this screen: bring it straight back while a lock is active (unless a call is in progress). */
    override fun onStop() {
        super.onStop()
        val now = SystemClock.elapsedRealtime()
        if (LockPolicy.displayed(cache.get(), now) != null && !CallGuard.isCallActive(this) && !isChangingConfigurations) {
            LockEnforcer.cover(this)
        }
    }

    private fun emergencyCall(number: String) {
        LockState.dialerGraceUntilElapsedMs = SystemClock.elapsedRealtime() + 30_000
        val uri = Uri.parse("tel:$number")
        try {
            startActivity(Intent(Intent.ACTION_CALL, uri))
        } catch (_: SecurityException) {
            startActivity(Intent(Intent.ACTION_DIAL, uri))
        }
    }

    companion object {
        fun intent(ctx: Context): Intent = Intent(ctx, LockActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION)
    }
}

@Composable
private fun LockScreen(s: LocalSession, remainingMs: Long, onCall: (String) -> Unit) {
    var pw by remember { mutableStateOf("") }
    var msg by remember { mutableStateOf<Int?>(null) }
    val mine = FirebaseAuth.getInstance().currentUser?.uid == s.createdBy
    val total = (remainingMs.coerceAtLeast(0) + 999) / 1000
    Column(
        Modifier.fillMaxSize().systemBarsPadding().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(R.string.lock_title), style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text(stringResource(if (s.mode == LockMode.FULL) R.string.lock_mode_full else R.string.lock_mode_apps))
        Text(stringResource(if (mine) R.string.lock_by_you else R.string.lock_by_observer))
        Spacer(Modifier.height(24.dp))
        Text(stringResource(R.string.lock_remaining))
        Text("%02d:%02d:%02d".format(total / 3600, total % 3600 / 60, total % 60), style = MaterialTheme.typography.displayMedium)
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(
            value = pw,
            onValueChange = { if (it.length <= 6 && it.all(Char::isDigit)) pw = it },
            label = { Text(stringResource(R.string.lock_password_hint)) },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        Button(onClick = { msg = R.string.lock_password_pending }, enabled = pw.length == 6, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.lock_unlock))
        }
        msg?.let { Text(stringResource(it), textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.error) }
        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { onCall("112") }) { Text(stringResource(R.string.lock_call_112)) }
            OutlinedButton(onClick = { onCall("999") }) { Text(stringResource(R.string.lock_call_999)) }
        }
    }
}
