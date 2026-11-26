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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.ui.text.font.FontFamily
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Slider
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.Color
import io.github.niravgrowlity.quotewidget.CycleOrder
import io.github.niravgrowlity.quotewidget.PhotoBackground
import io.github.niravgrowlity.quotewidget.PhotoFit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.niravgrowlity.quotewidget.FontCategory
import io.github.niravgrowlity.quotewidget.QuoteFont
import androidx.compose.ui.viewinterop.AndroidView
import io.github.niravgrowlity.quotewidget.Align
import io.github.niravgrowlity.quotewidget.QuoteStore
import io.github.niravgrowlity.quotewidget.ROTATION_PRESETS
import io.github.niravgrowlity.quotewidget.Rotation
import io.github.niravgrowlity.quotewidget.StyleGroup
import io.github.niravgrowlity.quotewidget.TextSize
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
        val scope = rememberCoroutineScope()
        val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) {
                scope.launch {
                    val ok = withContext(Dispatchers.IO) { PhotoBackground.import(ctx, uri) }
                    if (ok) QuoteStore.setStyle(ctx, WidgetStyle.PHOTO) else state.say("Couldn't open that image")
                    state.changed()
                }
            }
        }
        val choosePhoto = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
        val pick: (WidgetStyle) -> Unit = {
            if (it.photo && !PhotoBackground.has(ctx)) {
                choosePhoto()
            } else {
                QuoteStore.setStyle(ctx, it)
                state.changed()
            }
        }
        StyleGroup.entries.forEach { group ->
            StyleRow(group.title, WidgetStyle.entries.filter { it.group == group }, style, state, pick)
        }
        if (style.photo) PhotoSettings(state, choosePhoto)
        Text(
            "Classic palettes use the published colours of Catppuccin, Rosé Pine, Nord, Gruvbox, Dracula, " +
                "Solarized, Tokyo Night, Everforest, Kanagawa and One Dark.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 20.dp),
        )

        SectionTitle("Font")
        val font = QuoteStore.font(ctx)
        val pickFont: (QuoteFont) -> Unit = {
            QuoteStore.setFont(ctx, it)
            state.changed()
        }
        FontRow(null, listOf(QuoteFont.THEME), font, style, pickFont)
        FontCategory.entries.forEach { cat ->
            FontRow(cat.title, QuoteFont.entries.filter { it.category == cat }, font, style, pickFont)
        }
        Text(
            "Open-source fonts from Google Fonts. Letters these fonts don't have, such as other scripts " +
                "and emoji, use your phone's font.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 20.dp),
        )

        SectionTitle("Appearance")
        Setting("Largest text size") {
            Segmented(TextSize.entries, QuoteStore.textSize(ctx), { it.label }) {
                QuoteStore.setTextSize(ctx, it)
                state.changed()
            }
        }
        Setting("Alignment") {
            if (style.leftAligned) {
                Text(
                    "${style.label} is designed around its left edge, so it always aligns left.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Segmented(Align.entries, QuoteStore.align(ctx), { it.label }) {
                    QuoteStore.setAlign(ctx, it)
                    state.changed()
                }
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
        val collections = QuoteStore.collections(ctx)
        val inRotation = QuoteStore.rotationCollections(ctx)
        if (collections.size > 1) {
            Setting("Collections on the widget") {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    collections.forEach { c ->
                        FilterChip(
                            selected = c.id in inRotation,
                            onClick = {
                                val next = if (c.id in inRotation) inRotation - c.id else inRotation + c.id
                                if (next.isNotEmpty()) {
                                    QuoteStore.setRotationCollections(ctx, next)
                                    state.changed()
                                }
                            },
                            label = { Text(c.name) },
                        )
                    }
                }
            }
        }
        Setting("Order") {
            val order = QuoteStore.cycleOrder(ctx)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CycleOrder.entries.forEach { o ->
                    FilterChip(
                        selected = order == o,
                        onClick = { QuoteStore.setCycleOrder(ctx, o); state.changed() },
                        label = { Text(o.label) },
                    )
                }
            }
            Text(
                order.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (inRotation.size > 1) {
            SwitchRow(
                "Alternate between collections",
                "One quote from each collection in turn, instead of one collection after another",
                QuoteStore.alternate(ctx),
            ) {
                QuoteStore.setAlternate(ctx, it)
                state.changed()
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
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            )
        }
    }
}

/** Photo background options, shown when the "Your photo" style is selected. */
@Composable
private fun PhotoSettings(state: AppState, onChoose: () -> Unit) {
    val ctx = LocalContext.current
    state.tick
    val has = PhotoBackground.has(ctx)
    val c = MaterialTheme.colorScheme
    Column(
        Modifier
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(c.surfaceContainerLow)
            .padding(vertical = 8.dp),
    ) {
        Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (has) "Photo background" else "Choose a photo for the background",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f),
            )
            if (has) {
                TextButton(onClick = {
                    PhotoBackground.remove(ctx)
                    QuoteStore.setStyle(ctx, WidgetStyle.EDITORIAL)
                    state.changed()
                }) { Text("Remove") }
            }
            FilledTonalButton(onClick = onChoose) {
                Icon(AppIcons.Image, contentDescription = null, Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(if (has) "Change" else "Choose")
            }
        }
        if (!has) return@Column
        fun changed() = state.changed()

        Setting("Fit") {
            Segmented(PhotoFit.entries, PhotoBackground.fit(ctx), { it.label }) { PhotoBackground.setFit(ctx, it); changed() }
            Text(
                "Fill crops the photo to the widget’s shape. Fit shows all of it, with the photo’s own colour around it.",
                style = MaterialTheme.typography.bodySmall,
                color = c.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        SliderSetting("Blur", PhotoBackground.blur(ctx), 20, { if (it == 0) "Off" else "$it" }) {
            PhotoBackground.setBlur(ctx, it); changed()
        }
        val swatches = PhotoBackground.swatches(ctx)
        Setting("Overlay") {
            ColorChoices(
                listOf<Int?>(0xFF000000.toInt(), 0xFFFFFFFF.toInt()) + swatches,
                PhotoBackground.overlayColor(ctx),
            ) { PhotoBackground.setOverlayColor(ctx, it ?: 0xFF000000.toInt()); changed() }
        }
        SliderSetting("Overlay strength", PhotoBackground.overlay(ctx), 80, { "$it%" }) {
            PhotoBackground.setOverlay(ctx, it); changed()
        }
        Setting("Text colour") {
            ColorChoices(
                listOf<Int?>(null, 0xFFFFFFFF.toInt(), 0xFF1C1B1F.toInt()) + swatches,
                PhotoBackground.textColor(ctx),
            ) { PhotoBackground.setTextColor(ctx, it); changed() }
        }
        SwitchRow(
            "Keep text readable",
            "Darkens or lightens the photo behind the text just enough for it to stand out",
            PhotoBackground.keepReadable(ctx),
        ) { PhotoBackground.setKeepReadable(ctx, it); changed() }
    }
}

/** A slider that saves when you let go, so the widget isn't redrawn on every tiny move. */
@Composable
private fun SliderSetting(label: String, value: Int, max: Int, describe: (Int) -> String, onSet: (Int) -> Unit) {
    var v by remember(value) { mutableFloatStateOf(value.toFloat()) }
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Text(describe(v.roundToInt()), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        }
        Slider(
            value = v,
            onValueChange = { v = it },
            valueRange = 0f..max.toFloat(),
            onValueChangeFinished = { onSet(v.roundToInt()) },
        )
    }
}

/** Colour swatches; null is "Auto". */
@Composable
private fun ColorChoices(options: List<Int?>, selected: Int?, onPick: (Int?) -> Unit) {
    val c = MaterialTheme.colorScheme
    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        options.distinct().forEach { color ->
            val isSel = color == selected
            Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (color == null) c.surfaceContainerHighest else Color(color))
                    .border(if (isSel) 3.dp else 1.dp, if (isSel) c.primary else c.outlineVariant, CircleShape)
                    .clickable { onPick(color) }
                    .semantics { this.selected = isSel },
                contentAlignment = Alignment.Center,
            ) {
                if (color == null) Text("Auto", style = MaterialTheme.typography.labelSmall)
                else if (isSel) Icon(
                    Icons.Rounded.Check, contentDescription = null,
                    tint = if (PhotoBackground.luminance(color) > 0.5) Color.Black else Color.White,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun FontRow(
    title: String?,
    fonts: List<QuoteFont>,
    current: QuoteFont,
    style: WidgetStyle,
    onPick: (QuoteFont) -> Unit,
) {
    if (title != null) {
        Text(
            title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 20.dp, top = 4.dp, bottom = 8.dp),
        )
    }
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.padding(bottom = 12.dp),
    ) {
        items(fonts, key = { it.name }) { f ->
            FontTile(f, style, selected = f == current) { onPick(f) }
        }
    }
}

/** A font shown by writing a short sample in it, with its name underneath. */
@Composable
private fun FontTile(font: QuoteFont, style: WidgetStyle, selected: Boolean, onClick: () -> Unit) {
    val ctx = LocalContext.current
    val c = MaterialTheme.colorScheme
    val family = remember(font, style) {
        FontFamily(font.typeface(ctx) ?: style.typeface())
    }
    Column(
        Modifier
            .width(124.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .semantics { this.selected = selected },
    ) {
        Box(
            Modifier
                .size(124.dp, 72.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(if (selected) c.primaryContainer else c.surfaceContainerHigh)
                .border(
                    width = if (selected) 2.dp else 0.dp,
                    color = if (selected) c.primary else c.primary.copy(alpha = 0f),
                    shape = RoundedCornerShape(20.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "Be kind.",
                fontFamily = family,
                fontSize = 24.sp,
                maxLines = 1,
                color = if (selected) c.onPrimaryContainer else c.onSurface,
            )
        }
        Row(Modifier.padding(start = 6.dp, top = 6.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            if (selected) {
                Icon(Icons.Rounded.Check, contentDescription = null, tint = c.primary, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
            }
            Text(
                font.label,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) c.primary else c.onSurface,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
internal fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 20.dp, top = 32.dp, bottom = 10.dp),
    )
}

@Composable
internal fun Setting(label: String, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(bottom = 8.dp))
        content()
    }
}

@Composable
internal fun <T> Segmented(options: List<T>, selected: T, label: (T) -> String, onSelect: (T) -> Unit) {
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
internal fun SwitchRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
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
