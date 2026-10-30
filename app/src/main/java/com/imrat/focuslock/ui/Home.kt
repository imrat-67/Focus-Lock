package com.imrat.focuslock.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.imrat.focuslock.AppContainer
import com.imrat.focuslock.R
import com.imrat.focuslock.data.DeviceStore
import com.imrat.focuslock.data.Link
import com.imrat.focuslock.data.LinkRules
import com.imrat.focuslock.data.LinkStatus
import com.imrat.focuslock.data.RemoteEntry
import com.imrat.focuslock.data.SessionStatus
import com.imrat.focuslock.data.UserProfile
import com.imrat.focuslock.data.UserRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** A person's display name; falls back to a neutral word when the rules do not allow reading it. */
@Composable
internal fun personName(uid: String, users: UserRepository): String {
    val fallback = stringResource(R.string.links_unknown_person)
    val n by produceState(fallback, uid) { value = try { users.displayName(uid) ?: fallback } catch (e: Exception) { fallback } }
    return n
}

@Composable
internal fun StatusPill(locked: Boolean) {
    val color = if (locked) LockedRed else UnlockedGreen
    Row(verticalAlignment = Alignment.CenterVertically) {
        StatusDot(color)
        Spacer(Modifier.width(6.dp))
        Text(stringResource(if (locked) R.string.h_locked else R.string.h_unlocked), style = MaterialTheme.typography.bodySmall, color = color)
    }
}

@Composable
internal fun DeviceCard(title: String, subtitle: String?, icon: ImageVector, locked: Boolean?, onClick: (() -> Unit)?, extra: @Composable ColumnScope.() -> Unit = {}) {
    FLCard(modifier = Modifier.padding(bottom = 12.dp), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(icon)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                if (subtitle != null) Subtle(subtitle)
            }
            if (locked != null) StatusPill(locked)
        }
        extra()
    }
}

@Composable
internal fun NameDialog(onSave: (String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.h_name_title)) },
        text = {
            OutlinedTextField(
                value = name, onValueChange = { name = it.take(30) }, singleLine = true,
                placeholder = { Text(stringResource(R.string.h_name_hint)) },
            )
        },
        confirmButton = { TextButton(enabled = name.isNotBlank(), onClick = { onSave(name.trim()) }) { Text(stringResource(R.string.h_name_save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.back)) } },
    )
}

@Composable
internal fun RemoteRow(l: Link, entry: RemoteEntry?, now: Long, container: AppContainer, onOpen: (String, String) -> Unit, onName: (String) -> Unit) {
    val scope = rememberCoroutineScope()
    val who = personName(l.userUid, container.users)
    when {
        l.status == LinkStatus.PENDING -> DeviceCard(who, stringResource(R.string.h_waiting), Icons.Filled.Person, null, null) {
            Spacer(Modifier.height(12.dp))
            SecondaryButton(stringResource(R.string.links_cancel), { scope.launch { runCatching { container.links.deletePending(l.userUid) } } })
        }
        entry == null -> DeviceCard(who, stringResource(R.string.h_accepted_name), Icons.Filled.Person, null, { onName(l.userUid) })
        else -> {
            val live by remember(l.userUid) { container.sessions.observeLive(l.userUid, System.currentTimeMillis()) }.collectAsStateWithLifecycle(emptyList())
            val locked = live.any { it.status != SessionStatus.ENDED && it.startAtMs <= now && it.endAtMs > now }
            val sub = if (l.status == LinkStatus.REMOVAL_REQUESTED) stringResource(R.string.h_release_asked) else null
            DeviceCard(entry.name, sub, Icons.Filled.Phone, locked, { onOpen(l.userUid, entry.name) }) {
                if (l.status == LinkStatus.REMOVAL_REQUESTED) {
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        // LINK-7: no release while a lock they can see is running.
                        PrimaryButton(stringResource(R.string.links_release), { scope.launch { runCatching { container.links.completeRemoval(l.userUid) } } }, Modifier.weight(1f), enabled = !locked)
                        SecondaryButton(stringResource(R.string.links_keep), { scope.launch { runCatching { container.links.declineRemoval(l.userUid) } } }, Modifier.weight(1f))
                    }
                }
            }
        }
    }
}
