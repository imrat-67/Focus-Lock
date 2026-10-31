package com.imrat.focuslock.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.produceState
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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

/** Small in-memory cache so scrolling the app list does not reload icons. */
private object IconCache {
    val map = android.util.LruCache<String, androidx.compose.ui.graphics.ImageBitmap>(200)
}

private fun loadIcon(pm: android.content.pm.PackageManager, pkg: String): androidx.compose.ui.graphics.ImageBitmap? = try {
    val d = pm.getApplicationIcon(pkg)
    val size = 96
    val bmp = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888)
    val c = android.graphics.Canvas(bmp)
    d.setBounds(0, 0, size, size)
    d.draw(c)
    bmp.asImageBitmap()
} catch (e: Exception) { null } // not installed on this phone (e.g. the other phone's list)

/** The app's own icon when it is installed here; otherwise a coloured initial so every row still lines up. */
@Composable
fun AppIcon(pkg: String, name: String, size: Int = 40) {
    val ctx = LocalContext.current
    val icon by produceState(IconCache.map.get(pkg), pkg) {
        if (value == null) {
            val loaded = withContext(Dispatchers.IO) { loadIcon(ctx.packageManager, pkg) }
            if (loaded != null) IconCache.map.put(pkg, loaded)
            value = loaded
        }
    }
    val shape = RoundedCornerShape((size * 0.25f).dp)
    val img = icon
    if (img != null) {
        Image(img, null, Modifier.size(size.dp).clip(shape))
    } else {
        Box(Modifier.size(size.dp).clip(shape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
            Text(
                name.firstOrNull { it.isLetterOrDigit() }?.uppercase() ?: "?",
                style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

/** Searchable multi-select list of apps with icons (FMT-1). */
@Composable
fun AppPicker(apps: List<AppEntry>, selected: Set<String>, onToggle: (String) -> Unit, modifier: Modifier = Modifier) {
    var query by remember { mutableStateOf("") }
    val shown = remember(apps, query) { apps.filter { it.name.contains(query, true) || it.pkg.contains(query, true) } }
    Column(modifier) {
        OutlinedTextField(
            value = query, onValueChange = { query = it }, singleLine = true, shape = MaterialTheme.shapes.medium,
            placeholder = { Text(stringResource(R.string.format_search)) }, textStyle = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.fillMaxWidth().height(48.dp),
        )
        Text(stringResource(R.string.format_selected, selected.size), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 8.dp))
        LazyColumn(Modifier.weight(1f)) {
            items(shown, key = { it.pkg }) { a ->
                Row(
                    Modifier.fillMaxWidth().clip(MaterialTheme.shapes.small).clickable { onToggle(a.pkg) }.padding(horizontal = 4.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AppIcon(a.pkg, a.name)
                    Spacer(Modifier.width(14.dp))
                    Text(a.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    Checkbox(checked = a.pkg in selected, onCheckedChange = { onToggle(a.pkg) })
                }
            }
        }
    }
}

/** Formats of [targetUid]'s phone; the User or their Observer can edit (FMT-2). [apps] is the choosable app list. */
@Composable
fun FormatsScreen(targetUid: String, myUid: String, apps: List<AppEntry>, repo: FormatRepository, onBack: () -> Unit) {
    val loaded by remember(targetUid) { repo.observe(targetUid) }.collectAsStateWithLifecycle<List<Format>?>(null)
    val formats = loaded.orEmpty()
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
    AppScreen(stringResource(R.string.formats_title), onBack, scroll = false) {
        if (loaded != null && formats.isEmpty()) Subtle(stringResource(R.string.formats_empty), Modifier.padding(top = 8.dp))
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(formats, key = { it.id }) { f ->
                FLCard {
                    Row(Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(f.name, style = MaterialTheme.typography.titleMedium)
                            Subtle(stringResource(R.string.formats_apps_count, f.packages.size))
                        }
                        TextButton(onClick = { editing = f }) { Text(stringResource(R.string.format_edit)) }
                        TextButton(onClick = { scope.launch { runCatching { repo.delete(targetUid, f.id) } } }) {
                            Text(stringResource(R.string.format_delete), color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
        PrimaryButton(stringResource(R.string.formats_new), { creating = true }, Modifier.padding(top = 12.dp, bottom = 16.dp))
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
    AppScreen(stringResource(if (existing == null) R.string.formats_new else R.string.format_edit), onCancel, scroll = false) {
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = name, onValueChange = { if (it.length <= FormatCodec.MAX_NAME) name = it }, singleLine = true, shape = MaterialTheme.shapes.medium,
            label = { Text(stringResource(R.string.format_name)) }, modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        AppPicker(apps, selected, { p -> selected = if (p in selected) selected - p else selected + p }, Modifier.weight(1f))
        if (failed) Text(stringResource(R.string.format_error), color = MaterialTheme.colorScheme.error)
        PrimaryButton(
            stringResource(R.string.format_save),
            { scope.launch { failed = !runCatching { onSave(name, selected) }.isSuccess } },
            Modifier.padding(top = 8.dp, bottom = 16.dp), enabled = FormatCodec.isValid(name, selected),
        )
    }
}
