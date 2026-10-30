package com.imrat.focuslock.ui

import android.content.ClipData
import android.content.ClipboardManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.imrat.focuslock.AppContainer
import com.imrat.focuslock.R
import com.imrat.focuslock.data.DeviceStore
import com.imrat.focuslock.data.LinkRules
import com.imrat.focuslock.data.LinkStatus
import com.imrat.focuslock.data.UserProfile
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    profile: UserProfile, missingPerms: Int, lockActive: Boolean, container: AppContainer, store: DeviceStore,
    onAdd: () -> Unit, onOpenThis: () -> Unit, onOpenRemote: (String, String) -> Unit, onPerms: () -> Unit, onSignOut: () -> Unit,
) {
    val myDevice by store.myDevice.collectAsStateWithLifecycle()
    val remotes by store.remotes.collectAsStateWithLifecycle()
    val people by remember { container.links.observeUsersOf(profile.uid) }.collectAsStateWithLifecycle(emptyList())
    val incoming by remember { container.links.observe(profile.uid) }.collectAsStateWithLifecycle(null)
    var sheet by remember { mutableStateOf(false) }
    var naming by remember { mutableStateOf<String?>(null) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { delay(15_000); now = System.currentTimeMillis() } }

    AppScreen(
        stringResource(R.string.app_name), scroll = false,
        actions = { IconButton(onClick = { sheet = true }) { Icon(Icons.Filled.AccountCircle, stringResource(R.string.h_profile)) } },
    ) {
        Box(Modifier.weight(1f).fillMaxWidth()) {
            if (!myDevice && people.isEmpty()) {
                Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(R.string.h_empty_title), style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(8.dp))
                    Subtle(stringResource(R.string.h_empty_body), align = TextAlign.Center)
                }
            } else {
                LazyColumn(Modifier.fillMaxSize()) {
                    if (myDevice) item("this") {
                        DeviceCard(stringResource(R.string.h_this_device), null, Icons.Filled.Phone, lockActive, onOpenThis)
                    }
                    items(people, key = { it.userUid }) { l ->
                        RemoteRow(l, remotes.firstOrNull { it.id == l.userUid }, now, container, onOpenRemote) { naming = it }
                    }
                }
            }
        }
        PrimaryButton(stringResource(R.string.h_add_device), onAdd, Modifier.padding(bottom = 16.dp, top = 8.dp))
    }

    naming?.let { uid -> NameDialog(onSave = { store.addRemote(it, uid); naming = null }, onDismiss = { naming = null }) }

    if (sheet) {
        ModalBottomSheet(onDismissRequest = { sheet = false }, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
            ProfileSheet(profile, incoming, missingPerms, lockActive, container, onPerms = { sheet = false; onPerms() }, onSignOut = onSignOut)
        }
    }
}

@Composable
private fun ProfileSheet(
    profile: UserProfile, link: com.imrat.focuslock.data.Link?, missingPerms: Int, lockActive: Boolean,
    container: AppContainer, onPerms: () -> Unit, onSignOut: () -> Unit,
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 28.dp)) {
        Text(profile.displayName, style = MaterialTheme.typography.titleLarge)
        Subtle(profile.email)
        SectionLabel(stringResource(R.string.home_friend_code))
        FLCard {
            Text(profile.friendCode, style = MaterialTheme.typography.headlineMedium)
            Subtle(stringResource(R.string.home_friend_code_hint))
            Spacer(Modifier.height(10.dp))
            SecondaryButton(stringResource(R.string.h_copy_code), {
                ctx.getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText("code", profile.friendCode))
            })
        }
        if (link != null) {
            SectionLabel(stringResource(R.string.links_my_observer))
            val who = personName(link.observerUid, container.users)
            FLCard {
                when (link.status) {
                    LinkStatus.PENDING -> {
                        Text(stringResource(R.string.links_request_from, who), style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            PrimaryButton(stringResource(R.string.links_accept), { scope.launch { runCatching { container.links.accept(profile.uid) } } }, Modifier.weight(1f))
                            SecondaryButton(stringResource(R.string.links_decline), { scope.launch { runCatching { container.links.deletePending(profile.uid) } } }, Modifier.weight(1f))
                        }
                    }
                    LinkStatus.ACTIVE -> {
                        Text(stringResource(R.string.links_observer_is, who), style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(10.dp))
                        if (LinkRules.canRequestRemoval(link, lockActive)) {
                            SecondaryButton(stringResource(R.string.links_request_removal), { scope.launch { runCatching { container.links.requestRemoval(profile.uid) } } })
                        } else Subtle(stringResource(R.string.links_removal_locked))
                    }
                    LinkStatus.REMOVAL_REQUESTED -> {
                        Text(stringResource(R.string.links_observer_is, who), style = MaterialTheme.typography.titleMedium)
                        val days = LinkRules.daysUntilAutoRemoval(link, System.currentTimeMillis())
                        Subtle(stringResource(R.string.links_removal_waiting, days.toInt()))
                    }
                }
            }
        }
        if (missingPerms > 0) {
            Spacer(Modifier.height(16.dp))
            SecondaryButton(stringResource(R.string.perms_open, missingPerms), onPerms)
        }
        Spacer(Modifier.height(16.dp))
        SecondaryButton(stringResource(R.string.sign_out), onSignOut, enabled = !lockActive, danger = true)
        if (lockActive) Subtle(stringResource(R.string.sign_out_locked), Modifier.padding(top = 6.dp), TextAlign.Center)
    }
}

@Composable
fun AddDeviceScreen(myAdded: Boolean, onMine: () -> Unit, onRemote: () -> Unit, onBack: () -> Unit) {
    AppScreen(stringResource(R.string.h_add_device), onBack) {
        Spacer(Modifier.height(8.dp))
        FLCard(Modifier.padding(bottom = 12.dp), onClick = if (myAdded) null else onMine) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge(Icons.Filled.Phone)
                Column(Modifier.padding(start = 14.dp)) {
                    Text(stringResource(R.string.h_my_device), style = MaterialTheme.typography.titleMedium)
                    Subtle(stringResource(if (myAdded) R.string.h_already_added else R.string.h_my_device_sub))
                }
            }
        }
        FLCard(onClick = onRemote) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge(Icons.Filled.Person)
                Column(Modifier.padding(start = 14.dp)) {
                    Text(stringResource(R.string.h_remote_device), style = MaterialTheme.typography.titleMedium)
                    Subtle(stringResource(R.string.h_remote_device_sub))
                }
            }
        }
    }
}

@Composable
fun AddRemoteScreen(myUid: String, container: AppContainer, onSent: () -> Unit, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var target by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    AppScreen(stringResource(R.string.h_remote_device), onBack) {
        Spacer(Modifier.height(8.dp))
        Subtle(stringResource(R.string.h_remote_body))
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = target, onValueChange = { target = it }, singleLine = true, modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.links_target_label)) },
        )
        Spacer(Modifier.height(16.dp))
        PrimaryButton(stringResource(R.string.links_send), {
            val typed = target
            scope.launch {
                try {
                    // LINK-3: same outcome whether or not the account exists.
                    container.links.request(myUid, container.users.findUid(typed))
                    onSent()
                } catch (e: Exception) { error = true }
            }
        }, enabled = target.isNotBlank())
        if (error) Text(stringResource(R.string.links_error), color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable
fun ThisDeviceScreen(lockActive: Boolean, onInstant: () -> Unit, onSchedule: () -> Unit, onFormats: () -> Unit, onBack: () -> Unit) {
    AppScreen(stringResource(R.string.h_this_device), onBack, actions = { StatusPill(lockActive); Spacer(Modifier.padding(end = 12.dp)) }) {
        Spacer(Modifier.height(8.dp))
        FLCard(Modifier.padding(bottom = 12.dp), onClick = onInstant) {
            Text(stringResource(R.string.h_instant), style = MaterialTheme.typography.titleMedium)
            Subtle(stringResource(R.string.h_instant_sub))
        }
        FLCard(Modifier.padding(bottom = 12.dp), onClick = onSchedule) {
            Text(stringResource(R.string.h_schedule), style = MaterialTheme.typography.titleMedium)
            Subtle(stringResource(R.string.h_schedule_sub))
        }
        FLCard(onClick = onFormats) { Text(stringResource(R.string.formats_open), style = MaterialTheme.typography.titleMedium) }
    }
}
