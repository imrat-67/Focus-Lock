package com.imrat.focuslock.ui

import android.content.ActivityNotFoundException
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.imrat.focuslock.R
import com.imrat.focuslock.security.Perm
import com.imrat.focuslock.security.Perms

private fun iconOf(p: Perm): ImageVector = when (p) {
    Perm.ACCESSIBILITY -> Icons.Filled.Search
    Perm.DEVICE_ADMIN -> Icons.Filled.Lock
    Perm.NOTIFICATIONS -> Icons.Filled.Notifications
    Perm.EXACT_ALARMS -> Icons.Filled.DateRange
    Perm.BATTERY -> Icons.Filled.Refresh
    Perm.USAGE_ACCESS -> Icons.Filled.Info
    Perm.OVERLAY -> Icons.Filled.Phone
}

@Composable
fun PermissionsScreen(onDone: () -> Unit) {
    val ctx = LocalContext.current
    var tick by remember { mutableIntStateOf(0) }

    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val obs = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_RESUME) tick++ }
        owner.lifecycle.addObserver(obs)
        onDispose { owner.lifecycle.removeObserver(obs) }
    }

    fun openSettings(p: Perm) {
        try {
            ctx.startActivity(Perms.settingsIntent(ctx, p))
        } catch (_: ActivityNotFoundException) {
            ctx.startActivity(android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, android.net.Uri.parse("package:" + ctx.packageName)))
        }
    }

    var pending by remember { mutableStateOf<Perm?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        tick++
        val p = pending
        if (!granted && p != null) openSettings(p) // denied for good: send them to Settings
    }

    val states = remember(tick) { Perm.entries.map { it to Perms.isGranted(ctx, it) } }
    val done = states.count { it.second }

    AppScreen(stringResource(R.string.perms_title), scroll = false) {
        Box(Modifier.weight(1f).fillMaxWidth()) {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                Subtle(stringResource(R.string.perms_intro))
                Spacer(Modifier.height(14.dp))
                Text(stringResource(R.string.perms_progress, done, states.size), style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { if (states.isEmpty()) 1f else done.toFloat() / states.size },
                    modifier = Modifier.fillMaxWidth().height(6.dp),
                    color = if (done == states.size) UnlockedGreen else MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.outlineVariant,
                )
                Spacer(Modifier.height(16.dp))
                states.forEach { (p, ok) ->
                    FLCard(Modifier.padding(bottom = 10.dp)) {
                        Row(verticalAlignment = Alignment.Top) {
                            IconBadge(iconOf(p), if (ok) UnlockedGreen else MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text(stringResource(p.title), style = MaterialTheme.typography.titleMedium)
                                Spacer(Modifier.height(2.dp))
                                Subtle(stringResource(p.why))
                            }
                            Spacer(Modifier.width(12.dp))
                            if (ok) {
                                Icon(Icons.Filled.CheckCircle, stringResource(R.string.perm_granted), tint = UnlockedGreen, modifier = Modifier.size(28.dp))
                            }
                        }
                        if (!ok) {
                            Spacer(Modifier.height(12.dp))
                            SecondaryButton(stringResource(R.string.perm_grant), {
                                val rt = Perms.runtimePermission(p)
                                if (rt != null) { pending = p; launcher.launch(rt) } else openSettings(p)
                            })
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
        PrimaryButton(stringResource(R.string.perms_done), onDone, Modifier.padding(top = 8.dp, bottom = 16.dp))
    }
}
