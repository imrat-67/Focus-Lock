package com.imrat.focuslock.lock

import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
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
import com.imrat.focuslock.ui.FocusLockTheme
import com.imrat.focuslock.ui.PrimaryButton
import com.imrat.focuslock.ui.Subtle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** The lock screen (LOCK-4): countdown, who locked, mode, password field. Nothing else (no exit, no call buttons). */
class LockActivity : ComponentActivity() {
    private lateinit var cache: SessionCache
    private lateinit var engine: SessionEngine

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cache = SessionCache(this)
        engine = SessionEngine(this)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = Unit
        })
        setContent {
            FocusLockTheme {
                var session by remember { mutableStateOf<LocalSession?>(null) }
                var remaining by remember { mutableLongStateOf(0L) }
                var nowEl by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
                LaunchedEffect(Unit) {
                    // Only while visible: a stopped lock screen (Apps lock, user on Home) must not keep ticking.
                    lifecycle.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                        while (true) {
                            val now = SystemClock.elapsedRealtime()
                            nowEl = now
                            val s = LockPolicy.displayed(cache.get(), now)
                            if (s == null) { finishAndRemoveTask(); break }
                            session = s
                            remaining = s.endElapsedMs - now
                            delay(500)
                        }
                    }
                }
                session?.let {
                    LockScreen(
                        it, remaining, nowEl,
                        onUnlock = { pw, done ->
                            lifecycleScope.launch { done(withContext(Dispatchers.Default) { engine.attemptUnlock(it.id, pw) }) }
                        },
                    )
                }
            }
        }
    }

    /** Fires the instant Home/Recents is pressed (before the launcher is even drawn): during a FULL lock bring the lock screen back at once. */
    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        recoverIfFullLock()
    }

    /** Backup for paths that skip onUserLeaveHint (gesture to Recents on some skins, task switch). */
    override fun onPause() {
        super.onPause()
        recoverIfFullLock()
    }

    override fun onStop() {
        super.onStop()
        recoverIfFullLock()
    }

    /** FULL lock only; an Apps lock lets the user go Home (LOCK-3). Never while a call is active or the screen is off. */
    private fun recoverIfFullLock() {
        if (isChangingConfigurations || isFinishing) return
        if (getSystemService(PowerManager::class.java)?.isInteractive == false) return
        val now = SystemClock.elapsedRealtime()
        if (LockPolicy.shouldRecoverOnLeave(cache.get(), now) && !CallGuard.isCallActive(this)) LockEnforcer.cover(this)
    }

    companion object {
        fun intent(ctx: Context): Intent = Intent(ctx, LockActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION)
    }
}

@Composable
private fun LockScreen(
    s: LocalSession, remainingMs: Long, nowEl: Long,
    onUnlock: (String, (UnlockPolicy.Outcome) -> Unit) -> Unit,
) {
    var pw by remember { mutableStateOf("") }
    var outcome by remember { mutableStateOf<UnlockPolicy.Outcome?>(null) }
    var busy by remember { mutableStateOf(false) }
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
        val cooldownLeft = ((s.cooldownUntilElapsedMs - nowEl).coerceAtLeast(0) + 999) / 1000
        Button(
            onClick = { busy = true; onUnlock(pw) { outcome = it; busy = false; pw = "" } },
            enabled = pw.length == 6 && !busy && cooldownLeft == 0L,
            modifier = Modifier.fillMaxWidth(),
        ) { Text(stringResource(R.string.lock_unlock)) }
        val err = MaterialTheme.colorScheme.error
        if (cooldownLeft > 0) {
            Text(stringResource(R.string.lock_cooldown, cooldownLeft / 60, cooldownLeft % 60), textAlign = TextAlign.Center, color = err)
        } else when (val o = outcome) {
            is UnlockPolicy.Outcome.Wrong -> Text(stringResource(R.string.lock_wrong, o.attemptsLeft), textAlign = TextAlign.Center, color = err)
            UnlockPolicy.Outcome.NoPassword -> Text(stringResource(R.string.lock_no_password), textAlign = TextAlign.Center, color = err)
            else -> Unit
        }
    }
}
