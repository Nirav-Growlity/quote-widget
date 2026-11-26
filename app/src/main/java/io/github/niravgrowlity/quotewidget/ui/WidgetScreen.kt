@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package io.github.niravgrowlity.quotewidget.ui

import android.util.Log
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import io.github.niravgrowlity.quotewidget.Align
import io.github.niravgrowlity.quotewidget.BuildConfig
import io.github.niravgrowlity.quotewidget.QuoteStore
import io.github.niravgrowlity.quotewidget.ROTATION_PRESETS
import io.github.niravgrowlity.quotewidget.Rotation
import io.github.niravgrowlity.quotewidget.TextSize
import io.github.niravgrowlity.quotewidget.ThemeMode
import io.github.niravgrowlity.quotewidget.WidgetRenderer
import io.github.niravgrowlity.quotewidget.WidgetStyle

private const val SAMPLE = "Small steps, every day."

@Composable
fun WidgetScreen(state: AppState, padding: PaddingValues) {
    val ctx = LocalContext.current
    state.tick
    val style = QuoteStore.style(ctx)
    val period = QuoteStore.periodMinutes(ctx)
    var customOpen by remember { mutableStateOf(false) }

    fun setPeriod(minutes: Long) {
        QuoteStore.setPeriodMinutes(ctx, minutes)
        Rotation.schedule(ctx, force = true)
        state.changed(widget = false)
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(top = padding.calculateTopPadding() + 20.dp, bottom = padding.calculateBottomPadding() + 24.dp),
    ) {
        Text(
            "Widget",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 20.dp, bottom = 12.dp),
        )

        // Live preview on a wallpaper-like backdrop, rendered by the same code as the real widget.
        Backdrop(Modifier.padding(horizontal = 16.dp).fillMaxWidth(), corner = 32) {
            BoxWithConstraints(Modifier.padding(20.dp).fillMaxWidth().height(168.dp)) {
                WidgetPreview(state, style, maxWidth.value.toInt(), 168, Modifier.fillMaxSize())
            }
        }
        Text(
            "On your home screen, tap the quote to skip ahead and tap the corner text to open this app.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 10.dp),
        )

        SectionTitle("Style")
        val pick: (WidgetStyle) -> Unit = {
            QuoteStore.setStyle(ctx, it)
            state.changed()
        }
        StyleRow("Colour themes", WidgetStyle.entries.filter { !it.wallpaper }, style, state, pick)
        StyleRow("Match my wallpaper", WidgetStyle.entries.filter { it.wallpaper }, style, state, pick)

        SectionTitle("Appearance")
        Setting("Theme") {
            Segmented(ThemeMode.entries, state.themeMode, { it.label }) { state.setTheme(it) }
            if (!style.wallpaper) {
                Text(
                    "${style.label} keeps its own colours. Light and dark change the app and wallpaper styles.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
        Setting("Largest text size") {
            Segmented(TextSize.entries, QuoteStore.textSize(ctx), { it.label }) {
                QuoteStore.setTextSize(ctx, it)
                state.changed()
            }
        }
        Setting("Alignment") {
            Segmented(Align.entries, QuoteStore.align(ctx), { it.label }) {
                QuoteStore.setAlign(ctx, it)
                state.changed()
            }
        }
        SwitchRow("Show position", "Adds “3 of 12” in the corner", QuoteStore.showCounter(ctx)) {
            QuoteStore.setShowCounter(ctx, it)
            state.changed()
        }

        SectionTitle("Rotation")
        Setting("Change quote every") {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ROTATION_PRESETS.forEach { (minutes, label) ->
                    FilterChip(selected = period == minutes, onClick = { setPeriod(minutes) }, label = { Text(label) })
                }
                val custom = ROTATION_PRESETS.none { it.first == period }
                FilterChip(
                    selected = custom,
                    onClick = { customOpen = true },
                    label = { Text(if (custom) "Every ${describe(period)}" else "Custom") },
                )
            }
        }
        SwitchRow("Shuffle", "Mix up the order each time it goes through your quotes", QuoteStore.shuffle(ctx)) {
            QuoteStore.setShuffle(ctx, it)
            state.changed()
        }

        SectionTitle("Updates")
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("Version ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.bodyLarge)
                Text(
                    state.updateStatus ?: "New versions install from inside the app",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            FilledTonalButton(onClick = { state.updates.checkForUpdate(manual = true) }, enabled = !state.updating) {
                Text("Check now")
            }
        }
    }

    if (customOpen) {
        CustomPeriodDialog(current = period, onDismiss = { customOpen = false }) {
            setPeriod(it)
            customOpen = false
        }
    }
}

/** Renders the real widget RemoteViews into the app (no tap actions). */
@Composable
private fun WidgetPreview(state: AppState, style: WidgetStyle, wDp: Int, hDp: Int, modifier: Modifier, sample: String? = null) {
    val ctx = LocalContext.current
    AndroidView(
        factory = { FrameLayout(it) },
        modifier = modifier,
        update = { box ->
            // Reading these here makes the preview redraw when settings change.
            state.tick
            state.themeMode
            box.removeAllViews()
            runCatching {
                WidgetRenderer.build(ctx, wDp, hDp, style = style, interactive = false, sampleText = sample)
                    .apply(ctx, box)
            }.onSuccess { box.addView(it) }
                .onFailure { Log.w("QuoteWidget", "preview failed", it) }
        },
    )
}

@Composable
private fun Backdrop(modifier: Modifier, corner: Int, content: @Composable () -> Unit) {
    val c = MaterialTheme.colorScheme
    Box(
        modifier
            .clip(RoundedCornerShape(corner.dp))
            .background(Brush.linearGradient(listOf(c.primary, c.tertiary))),
    ) { content() }
}

@Composable
private fun StyleRow(
    title: String,
    styles: List<WidgetStyle>,
    current: WidgetStyle,
    state: AppState,
    onPick: (WidgetStyle) -> Unit,
) {
    Text(
        title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 20.dp, top = 4.dp, bottom = 8.dp),
    )
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.padding(bottom = 12.dp),
    ) {
        items(styles, key = { it.name }) { s ->
            StyleTile(state, s, selected = s == current) { onPick(s) }
        }
    }
}

@Composable
private fun StyleTile(state: AppState, style: WidgetStyle, selected: Boolean, onClick: () -> Unit) {
    val c = MaterialTheme.colorScheme
    Column(
        Modifier
            .width(148.dp)
            .clip(RoundedCornerShape(24.dp))
            .clickable(onClick = onClick)
            .semantics { this.selected = selected },
    ) {
        Backdrop(
            Modifier
                .size(148.dp, 104.dp)
                .border(
                    width = if (selected) 3.dp else 0.dp,
                    color = if (selected) c.primary else c.primary.copy(alpha = 0f),
                    shape = RoundedCornerShape(24.dp),
                ),
            corner = 24,
        ) {
            WidgetPreview(state, style, 128, 84, Modifier.padding(10.dp).fillMaxSize(), sample = SAMPLE)
        }
        Row(Modifier.padding(start = 6.dp, top = 8.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            if (selected) {
                Icon(Icons.Rounded.Check, contentDescription = null, tint = c.primary, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
            }
            Text(
                style.label,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) c.primary else c.onSurface,
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 20.dp, top = 32.dp, bottom = 10.dp),
    )
}

@Composable
private fun Setting(label: String, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(bottom = 8.dp))
        content()
    }
}

@Composable
private fun <T> Segmented(options: List<T>, selected: T, label: (T) -> String, onSelect: (T) -> Unit) {
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        options.forEachIndexed { i, option ->
            SegmentedButton(
                selected = option == selected,
                onClick = { onSelect(option) },
                shape = SegmentedButtonDefaults.itemShape(i, options.size),
            ) { Text(label(option)) }
        }
    }
}

@Composable
private fun SwitchRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .toggleable(value = checked, onValueChange = onChange, role = Role.Switch)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).padding(end = 16.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = null)
    }
}

private val UNITS = listOf("Minutes" to 1L, "Hours" to 60L, "Days" to 1440L)

private fun describe(minutes: Long): String {
    fun plural(n: Long, word: String) = if (n == 1L) "$n $word" else "$n ${word}s"
    return when {
        minutes % 1440 == 0L -> plural(minutes / 1440, "day")
        minutes % 60 == 0L -> plural(minutes / 60, "hour")
        else -> plural(minutes, "minute")
    }
}

@Composable
private fun CustomPeriodDialog(current: Long, onDismiss: () -> Unit, onSave: (Long) -> Unit) {
    val startUnit = when {
        current % 1440 == 0L -> 2
        current % 60 == 0L -> 1
        else -> 0
    }
    var unit by remember { mutableIntStateOf(startUnit) }
    var value by remember { mutableStateOf((current / UNITS[startUnit].second).toString()) }
    val minutes = value.toLongOrNull()?.times(UNITS[unit].second)
    val valid = minutes != null && minutes >= QuoteStore.MIN_PERIOD_MINUTES

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Change quote every") },
        text = {
            Column {
                OutlinedTextField(
                    value = value,
                    onValueChange = { v -> value = v.filter { it.isDigit() }.take(4) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = !valid,
                    supportingText = {
                        if (!valid) Text("The shortest is ${QuoteStore.MIN_PERIOD_MINUTES} minutes, to save battery")
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(top = 4.dp)) {
                    UNITS.forEachIndexed { i, (label, _) ->
                        SegmentedButton(
                            selected = unit == i,
                            onClick = { unit = i },
                            shape = SegmentedButtonDefaults.itemShape(i, UNITS.size),
                        ) { Text(label) }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onSave(minutes!!) }, enabled = valid) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
