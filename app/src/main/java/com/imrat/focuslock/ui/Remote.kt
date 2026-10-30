package com.imrat.focuslock.ui

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

private enum class RemotePage { HOME, LOCK, FORMATS }

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
                val s = LockSession(
                    "", myUid, mode, SessionKind.INSTANT, pkgs, start, start + ms, SessionStatus.PENDING,
                    null, null, null, remoteInstant = true, passwordUsed = false,
                )
                try { container.sessions.create(targetUid, s) } catch (e: Exception) { /* shown as missing in the list */ }
                page = RemotePage.HOME
            }
        }) { page = RemotePage.HOME }
        RemotePage.FORMATS -> FormatsScreen(targetUid, myUid, apps, container.formats) { page = RemotePage.HOME }
        RemotePage.HOME -> {
            val live by remember(targetUid) { container.sessions.observeLive(targetUid, clock.now()) }
                .collectAsStateWithLifecycle(emptyList())
            val fmt = remember { java.text.DateFormat.getTimeInstance(java.text.DateFormat.SHORT) }
            Column(Modifier.fillMaxSize().systemBarsPadding().padding(16.dp).verticalScroll(rememberScrollState())) {
                Text(stringResource(R.string.remote_title, name), style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(16.dp))
                Button(onClick = { page = RemotePage.LOCK }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.remote_lock_now)) }
                Spacer(Modifier.height(8.dp))
                Button(onClick = { page = RemotePage.FORMATS }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.formats_open)) }
                Spacer(Modifier.height(24.dp))
                Text(stringResource(R.string.remote_sessions), style = MaterialTheme.typography.titleMedium)
                val running = live.filter { it.status != SessionStatus.ENDED }
                if (running.isEmpty()) Text(stringResource(R.string.remote_none))
                running.forEach { s ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                        val modeName = stringResource(if (s.mode == LockMode.FULL) R.string.mode_full else R.string.mode_apps)
                        Text(
                            stringResource(R.string.remote_session_line, modeName, fmt.format(java.util.Date(s.endAtMs))),
                            Modifier.weight(1f),
                        )
                        OutlinedButton(onClick = {
                            scope.launch { runCatching { container.sessions.endByObserver(targetUid, s.id, myUid) } }
                        }) { Text(stringResource(R.string.remote_unlock)) }
                    }
                }
                Text(stringResource(R.string.remote_unlock_note), style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(24.dp))
                OutlinedButton(onClick = onBack) { Text(stringResource(R.string.back)) }
            }
        }
    }
}
