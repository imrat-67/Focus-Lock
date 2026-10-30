package com.imrat.focuslock.ui

import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.imrat.focuslock.data.Format
import com.imrat.focuslock.data.FormatCodec
import com.imrat.focuslock.data.FormatRepository
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch

/** Searchable multi-select list of apps (FMT-1). */
@Composable
fun AppPicker(apps: List<AppEntry>, selected: Set<String>, onToggle: (String) -> Unit, modifier: Modifier = Modifier) {
    var query by remember { mutableStateOf("") }
    val shown = remember(apps, query) { apps.filter { it.name.contains(query, true) || it.pkg.contains(query, true) } }
    Column(modifier) {
        OutlinedTextField(
            value = query, onValueChange = { query = it }, singleLine = true,
            label = { Text(stringResource(R.string.format_search)) }, modifier = Modifier.fillMaxWidth(),
        )
        Text(stringResource(R.string.format_selected, selected.size), style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(vertical = 4.dp))
        LazyColumn(Modifier.weight(1f)) {
            items(shown, key = { it.pkg }) { a ->
                Row(Modifier.fillMaxWidth().clickable { onToggle(a.pkg) }, verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = a.pkg in selected, onCheckedChange = { onToggle(a.pkg) })
                    Text(a.name)
                }
            }
        }
    }
}

/** Formats of [targetUid]'s phone; the User or their Observer can edit (FMT-2). [apps] is the choosable app list. */
@Composable
fun FormatsScreen(targetUid: String, myUid: String, apps: List<AppEntry>, repo: FormatRepository, onBack: () -> Unit) {
    val formats by remember(targetUid) { repo.observe(targetUid) }.collectAsStateWithLifecycle(emptyList())
    var editing by remember { mutableStateOf<Format?>(null) }
    var creating by remember { mutableStateOf(false) }
    if (creating || editing != null) {
        FormatEditor(editing, apps, onCancel = { creating = false; editing = null }) { name, pkgs ->
            val e = editing
            if (e == null) repo.create(targetUid, name, pkgs, myUid) else repo.update(targetUid, e, name, pkgs)
            creating = false; editing = null
        }
        return
    }
    val scope = rememberCoroutineScope()
    Column(Modifier.fillMaxSize().systemBarsPadding().padding(16.dp)) {
        Text(stringResource(R.string.formats_title), style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        if (formats.isEmpty()) Text(stringResource(R.string.formats_empty), style = MaterialTheme.typography.bodyMedium)
        LazyColumn(Modifier.weight(1f)) {
            items(formats, key = { it.id }) { f ->
                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(f.name, style = MaterialTheme.typography.titleMedium)
                        Text(stringResource(R.string.formats_apps_count, f.packages.size), style = MaterialTheme.typography.bodySmall)
                    }
                    TextButton(onClick = { editing = f }) { Text(stringResource(R.string.format_edit)) }
                    TextButton(onClick = { scope.launch { runCatching { repo.delete(targetUid, f.id) } } }) { Text(stringResource(R.string.format_delete)) }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { creating = true }) { Text(stringResource(R.string.formats_new)) }
            OutlinedButton(onClick = onBack) { Text(stringResource(R.string.format_back)) }
        }
    }
}

@Composable
private fun FormatEditor(
    existing: Format?, apps: List<AppEntry>, onCancel: () -> Unit, onSave: suspend (String, Set<String>) -> Unit,
) {
    var name by remember { mutableStateOf(existing?.name.orEmpty()) }
    var selected by remember { mutableStateOf(existing?.packages.orEmpty().toSet()) }
    var failed by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    Column(Modifier.fillMaxSize().systemBarsPadding().padding(16.dp)) {
        OutlinedTextField(
            value = name, onValueChange = { if (it.length <= FormatCodec.MAX_NAME) name = it }, singleLine = true,
            label = { Text(stringResource(R.string.format_name)) }, modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        AppPicker(apps, selected, { p -> selected = if (p in selected) selected - p else selected + p }, Modifier.weight(1f))
        if (failed) Text(stringResource(R.string.format_error), color = MaterialTheme.colorScheme.error)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
            Button(
                enabled = FormatCodec.isValid(name, selected),
                onClick = { scope.launch { failed = !runCatching { onSave(name, selected) }.isSuccess } },
            ) { Text(stringResource(R.string.format_save)) }
            OutlinedButton(onClick = onCancel) { Text(stringResource(R.string.format_cancel)) }
        }
    }
}
