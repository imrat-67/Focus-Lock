package com.imrat.focuslock.ui

import android.content.ActivityNotFoundException
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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

@Composable
fun PermissionsScreen(onDone: () -> Unit) {
    val ctx = LocalContext.current
    var tick by remember { mutableIntStateOf(0) }
    var pending by remember { androidx.compose.runtime.mutableStateOf<Perm?>(null) }

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
            ctx.startActivity(Perms.settingsIntent(ctx, Perm.PHONE_STATE)) // app details as a fallback
        }
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        tick++
        val p = pending
        if (!granted && p != null) openSettings(p) // denied for good: send them to Settings
    }

    val states = remember(tick) { Perm.entries.map { it to Perms.isGranted(ctx, it) } }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
        Text(stringResource(R.string.perms_title), style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(4.dp))
        Text(stringResource(R.string.perms_intro), style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(16.dp))
        states.forEach { (p, ok) ->
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(p.title), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(p.why), style = MaterialTheme.typography.bodySmall)
                    Text(
                        stringResource(if (ok) R.string.perm_granted else R.string.perm_missing),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (ok) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    )
                }
                if (!ok) {
                    Spacer(Modifier.padding(4.dp))
                    OutlinedButton(onClick = {
                        val rt = Perms.runtimePermission(p)
                        if (rt != null) {
                            pending = p
                            launcher.launch(rt)
                        } else {
                            openSettings(p)
                        }
                    }) { Text(stringResource(R.string.perm_grant)) }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.perms_done)) }
    }
}
