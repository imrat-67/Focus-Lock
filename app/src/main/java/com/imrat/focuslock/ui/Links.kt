package com.imrat.focuslock.ui

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.imrat.focuslock.R
import com.imrat.focuslock.data.Link
import com.imrat.focuslock.data.LinkRepository
import com.imrat.focuslock.data.LinkRules
import com.imrat.focuslock.data.LinkStatus
import com.imrat.focuslock.data.SessionRepository
import com.imrat.focuslock.data.SessionStatus
import com.imrat.focuslock.data.UserRepository
import kotlinx.coroutines.launch

/** One person's name; falls back to a neutral word when the rules do not allow reading it. */
@Composable
private fun nameOf(uid: String, users: UserRepository): String {
    val fallback = stringResource(R.string.links_unknown_person)
    val name by produceName(uid, users, fallback)
    return name
}

@Composable
private fun produceName(uid: String, users: UserRepository, fallback: String) =
    androidx.compose.runtime.produceState(fallback, uid) {
        value = try { users.displayName(uid) ?: fallback } catch (e: Exception) { fallback }
    }

/** Link management: my Observer (LINK-2/4/6/7) and the people I observe. */
@Composable
fun LinksScreen(myUid: String, links: LinkRepository, users: UserRepository, sessions: SessionRepository, lockActive: Boolean, onOpenPerson: (String, String) -> Unit, onBack: () -> Unit) {
    val myLink by remember { links.observe(myUid) }.collectAsStateWithLifecycle(null)
    val people by remember { links.observeUsersOf(myUid) }.collectAsStateWithLifecycle(emptyList())
    val scope = rememberCoroutineScope()
    var target by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<Int?>(null) }

    // LINK-6/7: finish a silent removal request after 7 days, but never while a lock runs.
    LaunchedEffect(myLink, lockActive) {
        val l = myLink ?: return@LaunchedEffect
        if (LinkRules.shouldAutoComplete(l, System.currentTimeMillis(), lockActive)) {
            try { links.completeRemoval(myUid) } catch (e: Exception) { /* server clock says not yet; try next time */ }
        }
    }

    fun run(block: suspend () -> Unit) {
        scope.launch {
            try { block() } catch (e: Exception) { message = R.string.links_error }
        }
    }

    Column(Modifier.fillMaxSize().systemBarsPadding().padding(16.dp).verticalScroll(rememberScrollState())) {
        Text(stringResource(R.string.links_title), style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))

        Text(stringResource(R.string.links_my_observer), style = MaterialTheme.typography.titleMedium)
        val l: Link? = myLink
        if (l == null) {
            Text(stringResource(R.string.links_none))
        } else {
            val who = nameOf(l.observerUid, users)
            when (l.status) {
                LinkStatus.PENDING -> {
                    Text(stringResource(R.string.links_request_from, who))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { run { links.accept(myUid) } }) { Text(stringResource(R.string.links_accept)) }
                        OutlinedButton(onClick = { run { links.deletePending(myUid) } }) { Text(stringResource(R.string.links_decline)) }
                    }
                }
                LinkStatus.ACTIVE -> {
                    Text(stringResource(R.string.links_observer_is, who))
                    if (LinkRules.canRequestRemoval(l, lockActive)) {
                        OutlinedButton(onClick = { run { links.requestRemoval(myUid) } }) { Text(stringResource(R.string.links_request_removal)) }
                    } else {
                        Text(stringResource(R.string.links_removal_locked), style = MaterialTheme.typography.bodySmall)
                    }
                }
                LinkStatus.REMOVAL_REQUESTED -> {
                    Text(stringResource(R.string.links_observer_is, who))
                    val days = LinkRules.daysUntilAutoRemoval(l, System.currentTimeMillis())
                    Text(stringResource(R.string.links_removal_waiting, days.toInt()))
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        Text(stringResource(R.string.links_people), style = MaterialTheme.typography.titleMedium)
        if (people.isEmpty()) Text(stringResource(R.string.links_people_empty))
        people.forEach { p ->
            Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                val who = if (p.status == LinkStatus.PENDING) stringResource(R.string.links_pending_out) else nameOf(p.userUid, users)
                when (p.status) {
                    LinkStatus.PENDING -> {
                        Text(who)
                        OutlinedButton(onClick = { run { links.deletePending(p.userUid) } }) { Text(stringResource(R.string.links_cancel)) }
                    }
                    LinkStatus.ACTIVE -> {
                        Text(who)
                        Button(onClick = { onOpenPerson(p.userUid, who) }) { Text(stringResource(R.string.links_manage)) }
                    }
                    LinkStatus.REMOVAL_REQUESTED -> {
                        Text(stringResource(R.string.links_removal_from, who))
                        OutlinedButton(onClick = { onOpenPerson(p.userUid, who) }) { Text(stringResource(R.string.links_manage)) }
                        // LINK-7: no release while a lock they can see is running (schedule-made locks have no server record yet).
                        val live by remember(p.userUid) { sessions.observeLive(p.userUid, System.currentTimeMillis()) }
                            .collectAsStateWithLifecycle(emptyList())
                        val locked = live.any { it.status != SessionStatus.ENDED && it.endAtMs > System.currentTimeMillis() }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(enabled = !locked, onClick = { run { links.completeRemoval(p.userUid) } }) { Text(stringResource(R.string.links_release)) }
                            OutlinedButton(onClick = { run { links.declineRemoval(p.userUid) } }) { Text(stringResource(R.string.links_keep)) }
                        }
                        if (locked) Text(stringResource(R.string.links_release_locked), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        Text(stringResource(R.string.links_add_remote), style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = target, onValueChange = { target = it }, singleLine = true,
            label = { Text(stringResource(R.string.links_target_label)) }, modifier = Modifier.fillMaxWidth(),
        )
        Button(
            enabled = target.isNotBlank(),
            onClick = {
                val typed = target
                run {
                    // LINK-3: same message whether or not the account exists.
                    links.request(myUid, users.findUid(typed))
                    target = ""
                    message = R.string.links_sent
                }
            },
        ) { Text(stringResource(R.string.links_send)) }
        message?.let { Text(stringResource(it), style = MaterialTheme.typography.bodySmall) }

        Spacer(Modifier.height(24.dp))
        OutlinedButton(onClick = onBack) { Text(stringResource(R.string.back)) }
    }
}
