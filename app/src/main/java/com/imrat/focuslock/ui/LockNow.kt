package com.imrat.focuslock.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Alignment
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.text.style.TextAlign
import com.imrat.focuslock.R
import com.imrat.focuslock.data.AppEntry
import com.imrat.focuslock.data.FormatRepository
import com.imrat.focuslock.data.LockMode
import com.imrat.focuslock.time.TimeModel

const val MIN_LOCK_MINUTES = 1
val MAX_LOCK_MINUTES = (TimeModel.MAX_SESSION_MS / 60_000).toInt()

/** Self instant lock (LOCK-1, LOCK-5): choose mode, apps or a format, hours + minutes; then start. */
@Composable
fun LockNowScreen(
    uid: String,
    apps: List<AppEntry>,
    formats: FormatRepository,
    onStart: (LockMode, List<String>, Long) -> Unit,
    onBack: () -> Unit,
) {
    var mode by remember { mutableStateOf(LockMode.FULL) }
    var hours by remember { mutableIntStateOf(0) }
    var minutes by remember { mutableIntStateOf(1) }
    var selected by remember { mutableStateOf(setOf<String>()) }
    val saved by remember(uid) { formats.observe(uid) }.collectAsStateWithLifecycle(emptyList())
    val total = hours * 60 + minutes
    val validTime = total in MIN_LOCK_MINUTES..MAX_LOCK_MINUTES
    val wheelRows = 5 // fixed: switching Full/Apps never moves the timer
    AppScreen(stringResource(R.string.h_instant), onBack, scroll = false) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(mode == LockMode.FULL, { mode = LockMode.FULL }, { Text(stringResource(R.string.mode_full)) })
            FilterChip(mode == LockMode.APPS, { mode = LockMode.APPS }, { Text(stringResource(R.string.mode_apps)) })
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Subtle(stringResource(R.string.lock_hours))
                WheelPicker(0..(MAX_LOCK_MINUTES / 60), hours, { hours = it }, Modifier.fillMaxWidth(), rows = wheelRows)
            }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Subtle(stringResource(R.string.lock_minutes))
                WheelPicker(0..59, minutes, { minutes = it }, Modifier.fillMaxWidth(), format = { "%02d".format(it) }, rows = wheelRows)
            }
        }
        if (!validTime) Text(stringResource(R.string.lock_time_invalid), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        if (mode == LockMode.APPS) {
            Spacer(Modifier.height(8.dp))
            AppSelectField(apps, selected, saved, { selected = it })
        }
        Spacer(Modifier.weight(1f))
        Subtle(stringResource(R.string.locknow_warning), Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 12.dp), align = TextAlign.Center)
        PrimaryButton(
            stringResource(R.string.locknow_start), { onStart(mode, selected.toList(), total * 60_000L) },
            Modifier.padding(bottom = 20.dp), enabled = validTime && (mode == LockMode.FULL || selected.isNotEmpty()),
        )
    }
}
