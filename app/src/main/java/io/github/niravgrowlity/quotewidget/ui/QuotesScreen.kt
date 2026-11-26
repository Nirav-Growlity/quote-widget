@file:OptIn(ExperimentalMaterial3Api::class)

package io.github.niravgrowlity.quotewidget.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.niravgrowlity.quotewidget.Quote
import io.github.niravgrowlity.quotewidget.QuoteStore
import io.github.niravgrowlity.quotewidget.Rotation

@Composable
fun QuotesScreen(state: AppState, padding: PaddingValues, onAdd: () -> Unit) {
    val ctx = LocalContext.current
    state.tick // re-read the store whenever something changes
    val all = QuoteStore.all(ctx)
    if (all.isEmpty()) {
        EmptyQuotes(padding, onAdd)
        return
    }
    val rotation = QuoteStore.rotation(ctx)
    val current = QuoteStore.current(ctx)
    val pinnedCount = all.count { it.pinned }

    var showPinned by rememberSaveable { mutableStateOf(false) }
    var selected by remember { mutableStateOf<Quote?>(null) }
    var editing by remember { mutableStateOf<Quote?>(null) }
    val shown = if (showPinned) all.filter { it.pinned } else all

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp, end = 16.dp,
            top = padding.calculateTopPadding() + 20.dp,
            bottom = padding.calculateBottomPadding() + 96.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item(key = "title") {
            Text(
                "Quotes",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 4.dp, bottom = 12.dp),
            )
        }
        if (current != null) {
            item(key = "now") {
                NowShowing(
                    quote = current,
                    position = Math.floorMod(QuoteStore.index(ctx), rotation.size) + 1,
                    total = rotation.size,
                    pinned = pinnedCount > 0,
                    onNext = { Rotation.advance(ctx); state.changed(widget = false) },
                )
            }
        }
        item(key = "filters") {
            Row(
                Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FilterChip(
                    selected = !showPinned, onClick = { showPinned = false },
                    label = { Text("All  ${all.size}") },
                )
                FilterChip(
                    selected = showPinned, onClick = { showPinned = true },
                    label = { Text("Pinned  $pinnedCount") },
                    leadingIcon = { Icon(AppIcons.Pin, contentDescription = null, Modifier.size(16.dp)) },
                )
                Spacer(Modifier.weight(1f))
                if (pinnedCount > 0) {
                    TextButton(onClick = {
                        QuoteStore.clearPins(ctx)
                        state.changed()
                        state.say("Pins cleared. All quotes are back in rotation.")
                    }) { Text("Clear pins") }
                }
            }
        }
        if (showPinned && shown.isEmpty()) {
            item(key = "nopins") {
                Text(
                    "Nothing pinned yet. Pin quotes to rotate through only those.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 12.dp),
                )
            }
        }
        items(shown, key = { it.id }) { q ->
            QuoteRow(
                quote = q,
                isCurrent = q.id == current?.id,
                onClick = { selected = q },
                onTogglePin = {
                    QuoteStore.update(ctx, q.copy(pinned = !q.pinned))
                    state.changed()
                },
                modifier = Modifier.animateItem(),
            )
        }
    }

    selected?.let { q ->
        val inRotation = rotation.any { it.id == q.id }
        ModalBottomSheet(onDismissRequest = { selected = null }) {
            Column(Modifier.padding(bottom = 24.dp)) {
                Text(
                    q.text,
                    style = QuoteType.row,
                    maxLines = 6,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 12.dp),
                )
                SheetAction(
                    AppIcons.Widgets, "Show on widget now",
                    supporting = if (inRotation) null else "Only pinned quotes are in rotation. Pin this one first.",
                    enabled = inRotation,
                ) {
                    QuoteStore.showNow(ctx, q.id)
                    Rotation.schedule(ctx, force = true)
                    state.changed()
                    selected = null
                }
                SheetAction(AppIcons.Pin, if (q.pinned) "Unpin" else "Pin") {
                    QuoteStore.update(ctx, q.copy(pinned = !q.pinned))
                    state.changed()
                    selected = null
                }
                SheetAction(Icons.Rounded.Edit, "Edit") {
                    editing = q
                    selected = null
                }
                SheetAction(Icons.Rounded.Delete, "Delete", danger = true) {
                    val position = QuoteStore.delete(ctx, q.id)
                    state.changed()
                    selected = null
                    state.say("Quote deleted", "Undo") {
                        QuoteStore.restore(ctx, q, position)
                        state.changed()
                    }
                }
            }
        }
    }

    editing?.let { q -> EditQuoteDialog(q, onDismiss = { editing = null }) { text ->
        QuoteStore.update(ctx, q.copy(text = text))
        state.changed()
        editing = null
    } }
}

@Composable
private fun NowShowing(quote: Quote, position: Int, total: Int, pinned: Boolean, onNext: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(
        color = colors.primaryContainer,
        contentColor = colors.onPrimaryContainer,
        shape = RoundedCornerShape(32.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(start = 24.dp, end = 20.dp, bottom = 20.dp)) {
            Text(
                "“",
                style = QuoteType.mark,
                color = colors.primary,
                modifier = Modifier.offset(x = (-3).dp, y = 14.dp),
            )
            AnimatedContent(
                targetState = quote.text,
                transitionSpec = { fadeIn(tween(240)) togetherWith fadeOut(tween(120)) },
                label = "quote",
            ) { text ->
                Text(text, style = QuoteType.hero, maxLines = 10, overflow = TextOverflow.Ellipsis)
            }
            Row(Modifier.fillMaxWidth().padding(top = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (pinned) "On your widget, $position of $total pinned" else "On your widget, $position of $total",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.weight(1f).alpha(0.75f),
                )
                Button(onClick = onNext) {
                    Icon(AppIcons.Next, contentDescription = null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Next")
                }
            }
        }
    }
}

@Composable
private fun QuoteRow(
    quote: Quote,
    isCurrent: Boolean,
    onClick: () -> Unit,
    onTogglePin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = if (isCurrent) colors.secondaryContainer else colors.surfaceContainerLow,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(start = 18.dp, top = 12.dp, bottom = 12.dp, end = 4.dp)) {
            Column(Modifier.weight(1f).padding(top = 6.dp, bottom = 4.dp)) {
                Text(quote.text, style = QuoteType.row, maxLines = 4, overflow = TextOverflow.Ellipsis)
                if (isCurrent) {
                    Text(
                        "On your widget now",
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.primary,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
            IconToggleButton(checked = quote.pinned, onCheckedChange = { onTogglePin() }) {
                Icon(
                    AppIcons.Pin,
                    contentDescription = if (quote.pinned) "Unpin" else "Pin",
                    tint = if (quote.pinned) colors.primary else colors.onSurfaceVariant.copy(alpha = 0.35f),
                )
            }
        }
    }
}

@Composable
private fun SheetAction(
    icon: ImageVector,
    title: String,
    supporting: String? = null,
    enabled: Boolean = true,
    danger: Boolean = false,
    onClick: () -> Unit,
) {
    val tint = when {
        !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        danger -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurface
    }
    ListItem(
        headlineContent = { Text(title, color = tint) },
        supportingContent = if (supporting != null) {
            { Text(supporting) }
        } else {
            null
        },
        leadingContent = { Icon(icon, contentDescription = null, tint = tint) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.clickable(enabled = enabled, onClick = onClick).padding(horizontal = 8.dp),
    )
}

@Composable
private fun EditQuoteDialog(quote: Quote, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var text by rememberSaveable { mutableStateOf(quote.text) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit quote") },
        text = {
            OutlinedTextField(
                value = text, onValueChange = { text = it },
                textStyle = QuoteType.row, minLines = 3, maxLines = 10,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(onClick = { onSave(text.trim()) }, enabled = text.isNotBlank()) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun EmptyQuotes(padding: PaddingValues, onAdd: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(padding).padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            "“",
            style = QuoteType.mark.copy(fontSize = QuoteType.mark.fontSize * 1.6f, lineHeight = QuoteType.mark.lineHeight * 1.2f),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
        )
        Text(
            "Start your collection",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            "Paste quotes you’ve saved anywhere. Your widget starts rotating through them right away.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 12.dp, bottom = 28.dp),
        )
        Button(onClick = onAdd) {
            Icon(Icons.Rounded.Add, contentDescription = null, Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Add quotes")
        }
    }
}
