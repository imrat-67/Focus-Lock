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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.imrat.focuslock.R
import com.imrat.focuslock.data.AppEntry
import com.imrat.focuslock.data.FormatRepository
import com.imrat.focuslock.data.LockMode
import com.imrat.focuslock.data.Schedule
import com.imrat.focuslock.data.ScheduleRepository
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

private fun hhmm(min: Int) = "%02d:%02d".format(min / 60, min % 60)

/** Schedules list (SCH-1). Observer-created ones are read-only here (SCH-2). */
@Composable
fun SchedulesScreen(uid: String, apps: List<AppEntry>, repo: ScheduleRepository, formats: FormatRepository, myUid: String = uid, topUp: com.imrat.focuslock.lock.SchedulePoolTopUp? = null, onBack: () -> Unit) {
    val loaded by remember(uid) { repo.observe(uid) }.collectAsStateWithLifecycle<List<Schedule>?>(null)
    val list = loaded.orEmpty()
    var creating by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    if (creating) {
        ScheduleEditor(uid, myUid, apps, formats, onCancel = { creating = false }) { s ->
            scope.launch { runCatching { val id = repo.create(uid, s); topUp?.topUp(uid, myUid, s.copy(id = id)) }; creating = false }
        }
        return
    }
    AppScreen(stringResource(R.string.sched_title), onBack, scroll = false) {
        if (loaded != null && list.isEmpty()) Subtle(stringResource(R.string.sched_empty), Modifier.padding(top = 8.dp))
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(list, key = { it.id }) { s ->
                val mine = s.createdBy == myUid
                FLCard {
                    Row(Modifier.padding(start = 16.dp, top = 6.dp, bottom = 6.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            val whenText = s.oneTimeDate ?: s.days.sorted().joinToString(" ") { DayOfWeek.of(it).getDisplayName(TextStyle.SHORT, Locale.getDefault()) }
                            Text("$whenText  ${hhmm(s.startMin)}–${hhmm(s.endMin)}", style = MaterialTheme.typography.titleMedium)
                            val modeText = stringResource(if (s.mode == LockMode.FULL) R.string.mode_full else R.string.mode_apps)
                            Subtle(modeText + if (mine) "" else " · " + stringResource(R.string.sched_by_observer))
                        }
                        if (mine) {
                            Switch(s.enabled, modifier = Modifier.padding(end = 20.dp), onCheckedChange = { on ->
                                scope.launch { runCatching { val u = s.copy(enabled = on); repo.update(uid, u); if (on) topUp?.topUp(uid, myUid, u) } }
                            })
                            TextButton(onClick = { scope.launch { runCatching { repo.delete(uid, s.id) } } }) {
                                Text(stringResource(R.string.format_delete), color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
        PrimaryButton(stringResource(R.string.sched_new), { creating = true }, Modifier.padding(top = 12.dp, bottom = 16.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeField(label: String, minute: Int, onChange: (Int) -> Unit) {
    var open by remember { mutableStateOf(false) }
    OutlinedButton(onClick = { open = true }) { Text("$label ${hhmm(minute)}") }
    if (open) {
        val st = rememberTimePickerState(minute / 60, minute % 60, true)
        AlertDialog(
            onDismissRequest = { open = false },
            confirmButton = { TextButton(onClick = { onChange(st.hour * 60 + st.minute); open = false }) { Text(stringResource(R.string.format_save)) } },
            dismissButton = { TextButton(onClick = { open = false }) { Text(stringResource(R.string.format_cancel)) } },
            text = { TimePicker(st) },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateField(date: LocalDate, today: LocalDate, onChange: (LocalDate) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val fmt = remember { java.time.format.DateTimeFormatter.ofLocalizedDate(java.time.format.FormatStyle.MEDIUM) }
    OutlinedButton(onClick = { open = true }) { Text(stringResource(R.string.sched_date, date.format(fmt))) }
    if (open) {
        val zone = java.time.ZoneOffset.UTC
        val minMs = today.atStartOfDay(zone).toInstant().toEpochMilli()
        val st = rememberDatePickerState(
            initialSelectedDateMillis = date.atStartOfDay(zone).toInstant().toEpochMilli(),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis >= minMs
            },
        )
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton(onClick = {
                    st.selectedDateMillis?.let { onChange(java.time.Instant.ofEpochMilli(it).atZone(zone).toLocalDate()) }
                    open = false
                }) { Text(stringResource(R.string.format_save)) }
            },
            dismissButton = { TextButton(onClick = { open = false }) { Text(stringResource(R.string.format_cancel)) } },
        ) { DatePicker(st) }
    }
}

@Composable
private fun ScheduleEditor(uid: String, myUid: String, apps: List<AppEntry>, formats: FormatRepository, onCancel: () -> Unit, onSave: (Schedule) -> Unit) {
    var mode by remember { mutableStateOf(LockMode.FULL) }
    var repeating by remember { mutableStateOf(true) }
    var days by remember { mutableStateOf(setOf<Int>()) }
    val today = remember { LocalDate.now() }
    var date by remember { mutableStateOf(today) }
    var start by remember { mutableStateOf(9 * 60) }
    var end by remember { mutableStateOf(11 * 60) }
    var selected by remember { mutableStateOf(setOf<String>()) }
    val saved by remember(uid) { formats.observe(uid) }.collectAsStateWithLifecycle(emptyList())
    val draft = Schedule(
        "", myUid, mode, if (mode == LockMode.APPS) selected.toList() else emptyList(),
        if (repeating) days else emptySet(), if (repeating) null else date.toString(),
        start, end, ZoneId.systemDefault().id, true,
    )
    val ok = draft.isUsable()
    AppScreen(stringResource(R.string.sched_new), onCancel, scroll = false) {
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())) {
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(mode == LockMode.FULL, { mode = LockMode.FULL }, { Text(stringResource(R.string.mode_full)) })
            FilterChip(mode == LockMode.APPS, { mode = LockMode.APPS }, { Text(stringResource(R.string.mode_apps)) })
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(repeating, { repeating = true }, { Text(stringResource(R.string.sched_repeat)) })
            FilterChip(!repeating, { repeating = false }, { Text(stringResource(R.string.sched_once)) })
        }
        Spacer(Modifier.height(8.dp))
        if (repeating) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                DayOfWeek.entries.forEach { d ->
                    FilterChip(d.value in days, { days = if (d.value in days) days - d.value else days + d.value },
                        { Text(d.getDisplayName(TextStyle.NARROW, Locale.getDefault())) })
                }
            }
        } else {
            DateField(date, today) { date = it }
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TimeField(stringResource(R.string.sched_from), start) { start = it }
            TimeField(stringResource(R.string.sched_to), end) { end = it }
        }
        Text(stringResource(R.string.sched_rules), style = MaterialTheme.typography.bodySmall)
        if (mode == LockMode.APPS) {
            Spacer(Modifier.height(8.dp))
            AppSelectField(apps, selected, saved, { selected = it })
        }
        Spacer(Modifier.height(12.dp))
        }
        PrimaryButton(stringResource(R.string.format_save), { onSave(draft) }, Modifier.padding(top = 8.dp, bottom = 16.dp), enabled = ok)
    }
}
