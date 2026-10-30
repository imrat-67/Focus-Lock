package com.imrat.focuslock.ui

import android.content.ClipData
import android.content.ClipboardManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import com.imrat.focuslock.security.PasswordCrypto
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.imrat.focuslock.AppContainer
import com.imrat.focuslock.R
import com.imrat.focuslock.data.AppEntry
import com.imrat.focuslock.data.LockMode
import com.imrat.focuslock.data.LockSession
import com.imrat.focuslock.data.SessionKind
import com.imrat.focuslock.data.SessionStatus
import com.imrat.focuslock.time.ServerClock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class RemotePage { HOME, LOCK, FORMATS, SCHEDULES }

/** Observer's view of one linked User's phone: lock it, edit its formats, unlock it (UNL-1, NOT-2). */
@Composable
fun RemoteDeviceScreen(targetUid: String, myUid: String, name: String, container: AppContainer, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val clock = remember { ServerClock(ctx.applicationContext) }
    val scope = rememberCoroutineScope()
    var page by remember { mutableStateOf(RemotePage.HOME) }
    val apps by produceState(emptyList<AppEntry>(), targetUid) {
        value = try { withContext(Dispatchers.IO) { container.appList.get(targetUid) } } catch (e: Exception) { emptyList() }
    }

    when (page) {
        RemotePage.LOCK -> LockNowScreen(targetUid, apps, container.formats, { mode, pkgs, ms ->
            scope.launch {
                val start = clock.now() + 60_000
                val id = container.sessions.newId(targetUid)
                val pw = withContext(Dispatchers.Default) { PasswordCrypto.fresh() }
                val s = LockSession(
                    id, myUid, mode, SessionKind.INSTANT, pkgs, start, start + ms, SessionStatus.PENDING,
                    pw.hash, pw.salt, null, remoteInstant = true, passwordUsed = false,
                )
                container.vault.put(id, pw.plain) // keep the plaintext before the hash goes out (UNL-2)
                try {
                    container.sessions.createWithId(targetUid, id, s)
                } catch (e: Exception) {
                    container.vault.remove(id) // not created: shown as missing in the list
                }
                page = RemotePage.HOME
            }
        }) { page = RemotePage.HOME }
        RemotePage.FORMATS -> FormatsScreen(targetUid, myUid, apps, container.formats) { page = RemotePage.HOME }
        RemotePage.SCHEDULES -> SchedulesScreen(targetUid, apps, container.schedules, container.formats, myUid, container.poolTopUp) { page = RemotePage.HOME }
        RemotePage.HOME -> {
            val live by remember(targetUid) { container.sessions.observeLive(targetUid, clock.now()) }
                .collectAsStateWithLifecycle(emptyList())
            val fmt = remember { java.text.DateFormat.getTimeInstance(java.text.DateFormat.SHORT) }
            var pwTick by remember { mutableIntStateOf(0) }
            LaunchedEffect(live) { // finished sessions no longer need their password
                live.filter { it.status == SessionStatus.ENDED || it.passwordUsed }.forEach { container.vault.remove(it.id) }
            }
            val running = live.filter { it.status != SessionStatus.ENDED && !it.passwordUsed }
            val lockedNow = running.any { it.startAtMs <= clock.now() && it.endAtMs > clock.now() }
            AppScreen(name, onBack, actions = { StatusPill(lockedNow); Spacer(Modifier.padding(end = 12.dp)) }) {
                Spacer(Modifier.height(8.dp))
                FLCard(Modifier.padding(bottom = 12.dp), onClick = { page = RemotePage.LOCK }) {
                    Text(stringResource(R.string.h_instant), style = MaterialTheme.typography.titleMedium)
                    Subtle(stringResource(R.string.h_instant_sub))
                }
                FLCard(Modifier.padding(bottom = 12.dp), onClick = { page = RemotePage.SCHEDULES }) {
                    Text(stringResource(R.string.h_schedule), style = MaterialTheme.typography.titleMedium)
                    Subtle(stringResource(R.string.h_schedule_sub))
                }
                FLCard(onClick = { page = RemotePage.FORMATS }) { Text(stringResource(R.string.formats_open), style = MaterialTheme.typography.titleMedium) }

                SectionLabel(stringResource(R.string.remote_sessions))
                if (running.isEmpty()) Subtle(stringResource(R.string.remote_none))
                running.forEach { s ->
                    FLCard(Modifier.padding(bottom = 12.dp)) {
                        val modeName = stringResource(if (s.mode == LockMode.FULL) R.string.mode_full else R.string.mode_apps)
                        Text(stringResource(R.string.remote_session_line, modeName, fmt.format(java.util.Date(s.endAtMs))), style = MaterialTheme.typography.titleMedium)
                        if (s.createdBy == myUid && s.pwHash != null) {
                            val pw = remember(s.id, pwTick, s.pwHash) { container.vault.get(s.id) }
                            Spacer(Modifier.height(8.dp))
                            if (pw == null) {
                                Subtle(stringResource(R.string.remote_pw_missing))
                            } else {
                                Text(stringResource(R.string.remote_pw_label, pw), style = MaterialTheme.typography.headlineSmall)
                                Spacer(Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    SecondaryButton(stringResource(R.string.remote_pw_copy), {
                                        ctx.getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText("password", pw))
                                    }, Modifier.weight(1f))
                                    SecondaryButton(stringResource(R.string.remote_pw_regen), {
                                        scope.launch {
                                            val f = withContext(Dispatchers.Default) { PasswordCrypto.fresh() }
                                            // server first: if it fails nothing changed; then keep the new plaintext (UNL-5)
                                            val ok = runCatching { container.sessions.regenerate(targetUid, s.id, f.hash, f.salt) }.isSuccess
                                            if (ok) { container.vault.put(s.id, f.plain); pwTick++ }
                                        }
                                    }, Modifier.weight(1f))
                                }
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        PrimaryButton(stringResource(R.string.remote_unlock), {
                            scope.launch { runCatching { container.sessions.endByObserver(targetUid, s.id, myUid) } }
                        })
                    }
                }

                val scheds by remember(targetUid) { container.schedules.observe(targetUid) }.collectAsStateWithLifecycle(emptyList())
                val mine = scheds.filter { it.createdBy == myUid && it.enabled && it.isUsable() }
                LaunchedEffect(scheds) { // UNL-6: refill passwords whenever the Observer looks at this device
                    scheds.forEach { s -> runCatching { container.poolTopUp.topUp(targetUid, myUid, s) } }
                }
                if (mine.isNotEmpty()) {
                    SectionLabel(stringResource(R.string.remote_sched_pw))
                    val dt = remember { java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.SHORT, java.text.DateFormat.SHORT) }
                    mine.forEach { s ->
                        val next = com.imrat.focuslock.lock.SchedulePool.upcoming(s, clock.now()).firstOrNull() ?: return@forEach
                        val pw = remember(s.pwPool, next) { container.vault.get(com.imrat.focuslock.lock.SchedulePool.vaultKey(s.id, next)) }
                        val ready = pw != null && s.pwPool[next.toString()] != null
                        FLCard(Modifier.padding(bottom = 12.dp)) {
                            Subtle(dt.format(java.util.Date(next)))
                            if (!ready) {
                                Subtle(stringResource(R.string.remote_sched_pw_missing))
                            } else {
                                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text(stringResource(R.string.remote_pw_label, pw ?: ""), Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
                                    SecondaryButton(stringResource(R.string.remote_pw_copy), {
                                        ctx.getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText("password", pw))
                                    }, Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
                Subtle(stringResource(R.string.remote_unlock_note), Modifier.padding(vertical = 12.dp))
            }
        }
    }
}
