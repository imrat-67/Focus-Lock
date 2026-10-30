package com.imrat.focuslock.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.imrat.focuslock.R
import com.imrat.focuslock.data.AppEntry
import com.imrat.focuslock.data.FormatRepository
import com.imrat.focuslock.data.LockMode
import com.imrat.focuslock.time.TimeModel

const val MIN_LOCK_MINUTES = 5
val MAX_LOCK_MINUTES = (TimeModel.MAX_SESSION_MS / 60_000).toInt()

/** Self instant lock (LOCK-1, LOCK-5): choose mode, apps or a format, duration; then start. */
@Composable
fun LockNowScreen(
    uid: String,
    apps: List<AppEntry>,
    formats: FormatRepository,
    onStart: (LockMode, List<String>, Long) -> Unit,
    onBack: () -> Unit,
) {
    var mode by remember { mutableStateOf(LockMode.FULL) }
    var minutes by remember { mutableFloatStateOf(60f) }
    var selected by remember { mutableStateOf(setOf<String>()) }
    val saved by remember(uid) { formats.observe(uid) }.collectAsStateWithLifecycle(emptyList())
    val mins = minutes.toInt()
    Column(Modifier.fillMaxSize().systemBarsPadding().padding(16.dp)) {
        Text(stringResource(R.string.locknow_title), style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(mode == LockMode.FULL, { mode = LockMode.FULL }, { Text(stringResource(R.string.mode_full)) })
            FilterChip(mode == LockMode.APPS, { mode = LockMode.APPS }, { Text(stringResource(R.string.mode_apps)) })
        }
        Spacer(Modifier.height(12.dp))
        Text(stringResource(R.string.locknow_duration, mins / 60, mins % 60), style = MaterialTheme.typography.titleMedium)
        Slider(
            value = minutes, onValueChange = { minutes = (Math.round(it / 5f) * 5f).coerceAtLeast(MIN_LOCK_MINUTES.toFloat()) },
            valueRange = MIN_LOCK_MINUTES.toFloat()..MAX_LOCK_MINUTES.toFloat(),
        )
        if (mode == LockMode.APPS) {
            if (saved.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    saved.forEach { f ->
                        FilterChip(false, { selected = f.packages.toSet() }, { Text(f.name) })
                    }
                }
            }
            Text(stringResource(R.string.format_selected, selected.size), style = MaterialTheme.typography.labelLarge)
            AppPicker(apps, selected, { p -> selected = if (p in selected) selected - p else selected + p }, Modifier.weight(1f))
        } else {
            Spacer(Modifier.weight(1f))
            Text(stringResource(R.string.locknow_full_hint), style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.weight(1f))
        }
        Text(stringResource(R.string.locknow_warning), style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { onStart(mode, selected.toList(), mins * 60_000L) },
                enabled = mode == LockMode.FULL || selected.isNotEmpty(),
            ) { Text(stringResource(R.string.locknow_start)) }
            OutlinedButton(onClick = onBack) { Text(stringResource(R.string.format_back)) }
        }
    }
}
