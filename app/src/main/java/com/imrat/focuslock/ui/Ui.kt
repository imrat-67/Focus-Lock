package com.imrat.focuslock.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.imrat.focuslock.R
import kotlinx.coroutines.flow.distinctUntilChanged

/** Standard screen: system-bar safe, optional back arrow and title, scrolling body. */
@Composable
fun AppScreen(
    title: String?,
    onBack: (() -> Unit)? = null,
    scroll: Boolean = true,
    actions: @Composable () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(Modifier.fillMaxSize().systemBarsPadding().imePadding()) {
        Row(Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            if (onBack != null) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) }
            } else Spacer(Modifier.width(12.dp))
            if (title != null) Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f), maxLines = 1)
            else Spacer(Modifier.weight(1f))
            actions()
        }
        val mod = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 20.dp)
        Column(if (scroll) mod.verticalScroll(rememberScrollState()) else mod, content = content)
    }
}

@Composable
fun FLCard(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    val base = Modifier.fillMaxWidth()
    Surface(
        modifier = modifier.then(base),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column((if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier).padding(18.dp), content = content)
    }
}

@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Button(
        onClick = onClick, enabled = enabled, shape = RoundedCornerShape(18.dp),
        modifier = modifier.fillMaxWidth().height(54.dp),
    ) { Text(text, style = MaterialTheme.typography.titleMedium) }
}

@Composable
fun SecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, danger: Boolean = false) {
    val c = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
    OutlinedButton(
        onClick = onClick, enabled = enabled, shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, if (danger) c else MaterialTheme.colorScheme.outline),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = c),
        modifier = modifier.fillMaxWidth().height(50.dp),
    ) { Text(text, style = MaterialTheme.typography.labelLarge) }
}

@Composable
fun SectionLabel(text: String) {
    Text(
        text.uppercase(), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.8.sp),
        color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 20.dp, bottom = 8.dp),
    )
}

@Composable
fun Subtle(text: String, modifier: Modifier = Modifier, align: TextAlign? = null) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = modifier, textAlign = align)
}

@Composable
fun StatusDot(color: Color, size: Int = 10) {
    val c by animateColorAsState(color, label = "dot")
    Box(Modifier.size(size.dp).background(c, CircleShape))
}

@Composable
fun IconBadge(icon: ImageVector, tint: Color = MaterialTheme.colorScheme.primary, size: Int = 46) {
    Box(
        Modifier.size(size.dp).background(tint.copy(alpha = 0.16f), RoundedCornerShape(14.dp)),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, null, tint = tint, modifier = Modifier.size((size * 0.5f).dp)) }
}

/** Vertical snapping wheel (like the Android clock). Reports the centred value. */
@Composable
fun WheelPicker(range: IntRange, value: Int, onChange: (Int) -> Unit, modifier: Modifier = Modifier, format: (Int) -> String = { it.toString() }, rows: Int = 5) {
    val itemH = 52.dp
    val pad = rows / 2 // empty rows above/below so the first/last value can reach the centre
    val count = range.last - range.first + 1
    val state = rememberLazyListState(initialFirstVisibleItemIndex = (value - range.first).coerceIn(0, count - 1))
    val density = androidx.compose.ui.platform.LocalDensity.current
    val itemPx = with(density) { itemH.toPx() }
    val selected by remember { derivedStateOf { Math.round((state.firstVisibleItemIndex * itemPx + state.firstVisibleItemScrollOffset) / itemPx).coerceIn(0, count - 1) } }
    LaunchedEffect(Unit) { snapshotFlow { selected }.distinctUntilChanged().collect { onChange(range.first + it) } }
    Box(modifier.height(itemH * rows), contentAlignment = Alignment.Center) {
        Box(Modifier.fillMaxWidth().height(itemH).background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(14.dp)))
        LazyColumn(state = state, flingBehavior = rememberSnapFlingBehavior(state), modifier = Modifier.fillMaxWidth()) {
            items(count + 2 * pad) { i ->
                val v = i - pad
                val dist = Math.abs(v - selected)
                Box(Modifier.fillMaxWidth().height(itemH), contentAlignment = Alignment.Center) {
                    if (v in 0 until count) {
                        Text(
                            format(range.first + v),
                            style = if (dist == 0) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.alpha(if (dist == 0) 1f else if (dist == 1) 0.5f else 0.25f),
                        )
                    }
                }
            }
        }
    }
}
