@file:OptIn(ExperimentalMaterial3Api::class)

package io.github.niravgrowlity.quotewidget.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.niravgrowlity.quotewidget.CycleOrder
import io.github.niravgrowlity.quotewidget.Quote
import io.github.niravgrowlity.quotewidget.QuoteCollection
import io.github.niravgrowlity.quotewidget.QuoteFormat
import io.github.niravgrowlity.quotewidget.QuoteStore
import io.github.niravgrowlity.quotewidget.Rotation
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@Composable
fun QuotesScreen(state: AppState, padding: PaddingValues, onAdd: () -> Unit) {
    val ctx = LocalContext.current
    state.tick // re-read the store whenever something changes
    val collections = QuoteStore.collections(ctx)
    val all = QuoteStore.all(ctx)
    if (all.isEmpty() && collections.size == 1) {
        EmptyQuotes(padding, onAdd)
        return
    }
    val collection = collections.firstOrNull { it.id == state.collection } ?: collections.first()
    val inCollection = all.filter { it.collection == collection.id }
    val rotation = QuoteStore.rotation(ctx)
    val current = QuoteStore.current(ctx)
    val pinnedInCollection = inCollection.count { it.pinned }

    var showPinned by rememberSaveable { mutableStateOf(false) }
    var reordering by rememberSaveable { mutableStateOf(false) }
    var selected by remember { mutableStateOf<Quote?>(null) }
    var editing by remember { mutableStateOf<Quote?>(null) }
    var moving by remember { mutableStateOf<Quote?>(null) }
    var naming by remember { mutableStateOf<QuoteCollection?>(null) } // id 0 = new collection
    var deleting by remember { mutableStateOf<QuoteCollection?>(null) }

    // While reordering, the list is a local copy that follows the finger, saved when a drag ends.
    var order by remember(collection.id, state.tick, reordering) { mutableStateOf(inCollection) }
    val shown = when {
        reordering -> order
        showPinned -> inCollection.filter { it.pinned }
        else -> inCollection
    }

    val listState = rememberLazyListState()
    val reorderState = rememberReorderableLazyListState(listState) { from, to ->
        val f = order.indexOfFirst { it.id == from.key }
        val t = order.indexOfFirst { it.id == to.key }
        if (f >= 0 && t >= 0) order = order.toMutableList().apply { add(t, removeAt(f)) }
    }

    LazyColumn(
        state = listState,
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
        if (current != null && !reordering) {
            item(key = "now") {
                NowShowing(
                    quote = current,
                    position = Math.floorMod(QuoteStore.index(ctx), rotation.size) + 1,
                    total = rotation.size,
                    pinned = rotation.any { it.pinned },
                    onNext = { Rotation.advance(ctx); state.changed(widget = false) },
                )
            }
        }
        item(key = "collections") {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 16.dp),
            ) {
                items(collections, key = { it.id }) { c ->
                    FilterChip(
                        selected = c.id == collection.id,
                        onClick = { state.collection = c.id; reordering = false },
                        label = { Text("${c.name}  ${all.count { it.collection == c.id }}") },
                    )
                }
                item(key = "new") {
                    AssistChip(
                        onClick = { naming = QuoteCollection(0, "") },
                        label = { Text("New collection") },
                        leadingIcon = { Icon(Icons.Rounded.Add, contentDescription = null, Modifier.size(18.dp)) },
                    )
                }
            }
        }
        item(key = "toolbar") {
            Row(
                Modifier.fillMaxWidth().padding(bottom = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (reordering) {
                    Text(
                        "Drag the handles to reorder",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    Button(onClick = { reordering = false }) { Text("Done") }
                } else {
                    FilterChip(selected = !showPinned, onClick = { showPinned = false }, label = { Text("All") })
                    FilterChip(
                        selected = showPinned, onClick = { showPinned = true },
                        label = { Text("Pinned  $pinnedInCollection") },
                        leadingIcon = { Icon(AppIcons.Pin, contentDescription = null, Modifier.size(16.dp)) },
                    )
                    Spacer(Modifier.weight(1f))
                    CollectionMenu(
                        canReorder = inCollection.size > 1,
                        canDelete = collections.size > 1,
                        isFirst = collections.first().id == collection.id,
                        isLast = collections.last().id == collection.id,
                        hasPins = pinnedInCollection > 0,
                        onReorder = { showPinned = false; reordering = true },
                        onRename = { naming = collection },
                        onMove = { delta -> QuoteStore.moveCollection(ctx, collection.id, delta); state.changed() },
                        onClearPins = {
                            inCollection.filter { it.pinned }.forEach { QuoteStore.update(ctx, it.copy(pinned = false)) }
                            state.changed()
                            state.say("Pins cleared in ${collection.name}")
                        },
                        onDelete = { deleting = collection },
                    )
                }
            }
        }
        if (reordering && QuoteStore.cycleOrder(ctx) != CycleOrder.MY_ORDER) {
            item(key = "orderhint") {
                Text(
                    "The widget is set to “${QuoteStore.cycleOrder(ctx).label}”. Choose “My order” in the Widget tab to use this order.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                )
            }
        }
        if (shown.isEmpty()) {
            item(key = "none") {
                Text(
                    if (showPinned) "Nothing pinned here. Pin quotes to rotate through only those."
                    else "No quotes in ${collection.name} yet. Tap Add quotes to fill it.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 12.dp),
                )
            }
        }
        items(shown, key = { it.id }) { q ->
            ReorderableItem(reorderState, key = q.id, enabled = reordering) { dragging ->
                QuoteRow(
                    quote = q,
                    isCurrent = q.id == current?.id,
                    elevated = dragging,
                    onClick = { if (!reordering) selected = q },
                    trailing = {
                        if (reordering) {
                            IconButton(
                                onClick = {},
                                modifier = Modifier.draggableHandle(
                                    onDragStopped = {
                                        QuoteStore.reorder(ctx, order.map { it.id })
                                        state.changed()
                                    },
                                ),
                            ) { Icon(Icons.Rounded.Menu, contentDescription = "Drag to reorder") }
                        } else {
                            PinButton(q.pinned) {
                                QuoteStore.update(ctx, q.copy(pinned = !q.pinned))
                                state.changed()
                            }
                        }
                    },
                )
            }
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
                    supporting = if (inRotation) null else notInRotationReason(ctx, q),
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
                if (collections.size > 1) {
                    SheetAction(AppIcons.Folder, "Move to another collection") {
                        moving = q
                        selected = null
                    }
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

    editing?.let { q ->
        EditQuoteDialog(q, onDismiss = { editing = null }) { text ->
            QuoteStore.update(ctx, q.copy(text = text))
            state.changed()
            editing = null
        }
    }

    moving?.let { q ->
        MoveDialog(collections, q.collection, onDismiss = { moving = null }) { target ->
            QuoteStore.moveToCollection(ctx, q.id, target.id)
            state.changed()
            moving = null
            state.say("Moved to ${target.name}")
        }
    }

    naming?.let { c ->
        NameDialog(
            title = if (c.id == 0L) "New collection" else "Rename collection",
            initial = c.name,
            confirm = if (c.id == 0L) "Create" else "Save",
            onDismiss = { naming = null },
        ) { name ->
            if (c.id == 0L) {
                state.collection = QuoteStore.addCollection(ctx, name).id
            } else {
                QuoteStore.renameCollection(ctx, c.id, name)
            }
            state.changed()
            naming = null
        }
    }

    deleting?.let { c ->
        val count = all.count { it.collection == c.id }
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Delete “${c.name}”?") },
            text = {
                Text(
                    if (count == 0) "This collection is empty."
                    else "Its ${if (count == 1) "1 quote" else "$count quotes"} will be deleted too. You can undo this right after.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val before = QuoteStore.snapshot(ctx)
                    if (QuoteStore.deleteCollection(ctx, c.id)) {
                        state.collection = QuoteStore.collections(ctx).first().id
                        state.changed()
                        state.say("Deleted ${c.name}", "Undo") {
                            QuoteStore.restoreSnapshot(ctx, before)
                            state.collection = c.id
                            state.changed()
                        }
                    }
                    deleting = null
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancel") } },
        )
    }
}

/** Why "Show on widget now" isn't possible for this quote. */
private fun notInRotationReason(ctx: android.content.Context, q: Quote): String {
    if (q.collection !in QuoteStore.rotationCollections(ctx)) {
        return "This collection isn't in the widget's rotation. Add it in the Widget tab."
    }
    return "Only pinned quotes are in rotation. Pin this one first."
}

@Composable
private fun CollectionMenu(
    canReorder: Boolean,
    canDelete: Boolean,
    isFirst: Boolean,
    isLast: Boolean,
    hasPins: Boolean,
    onReorder: () -> Unit,
    onRename: () -> Unit,
    onMove: (Int) -> Unit,
    onClearPins: () -> Unit,
    onDelete: () -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) { Icon(Icons.Rounded.MoreVert, contentDescription = "Collection options") }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            if (canReorder) {
                DropdownMenuItem(
                    text = { Text("Reorder quotes") },
                    leadingIcon = { Icon(Icons.Rounded.Menu, contentDescription = null) },
                    onClick = { open = false; onReorder() },
                )
            }
            DropdownMenuItem(
                text = { Text("Rename collection") },
                leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null) },
                onClick = { open = false; onRename() },
            )
            if (!isFirst) {
                DropdownMenuItem(
                    text = { Text("Move collection left") },
                    leadingIcon = { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = null) },
                    onClick = { open = false; onMove(-1) },
                )
            }
            if (!isLast) {
                DropdownMenuItem(
                    text = { Text("Move collection right") },
                    leadingIcon = { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null) },
                    onClick = { open = false; onMove(1) },
                )
            }
            if (hasPins) {
                DropdownMenuItem(
                    text = { Text("Clear pins") },
                    leadingIcon = { Icon(AppIcons.Pin, contentDescription = null) },
                    onClick = { open = false; onClearPins() },
                )
            }
            if (canDelete) {
                DropdownMenuItem(
                    text = { Text("Delete collection", color = MaterialTheme.colorScheme.error) },
                    leadingIcon = { Icon(Icons.Rounded.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                    onClick = { open = false; onDelete() },
                )
            }
        }
    }
}

@Composable
private fun NowShowing(quote: Quote, position: Int, total: Int, pinned: Boolean, onNext: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val ctx = LocalContext.current
    // The widget's chosen font, so the card previews it.
    val font = QuoteStore.font(ctx)
    val heroStyle = remember(font) {
        font.typeface(ctx)?.let { QuoteType.hero.copy(fontFamily = FontFamily(it)) } ?: QuoteType.hero
    }
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
                val (body, author) = QuoteFormat.split(text)
                Column {
                    Text(body, style = heroStyle, maxLines = 10, overflow = TextOverflow.Ellipsis)
                    if (author != null) {
                        Text(
                            "— $author",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 12.dp),
                        )
                    }
                }
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
    elevated: Boolean,
    onClick: () -> Unit,
    trailing: @Composable () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = when {
            elevated -> colors.surfaceContainerHighest
            isCurrent -> colors.secondaryContainer
            else -> colors.surfaceContainerLow
        },
        shadowElevation = if (elevated) 6.dp else 0.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(start = 18.dp, top = 12.dp, bottom = 12.dp, end = 4.dp)) {
            Column(Modifier.weight(1f).padding(top = 6.dp, bottom = 4.dp)) {
                val (body, author) = QuoteFormat.split(quote.text)
                Text(body, style = QuoteType.row, maxLines = 4, overflow = TextOverflow.Ellipsis)
                if (author != null) {
                    Text(
                        "— $author",
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.onSurfaceVariant,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
                if (isCurrent) {
                    Text(
                        "On your widget now",
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.primary,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
            trailing()
        }
    }
}

@Composable
private fun PinButton(pinned: Boolean, onToggle: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    IconToggleButton(checked = pinned, onCheckedChange = { onToggle() }) {
        Icon(
            AppIcons.Pin,
            contentDescription = if (pinned) "Unpin" else "Pin",
            tint = if (pinned) colors.primary else colors.onSurfaceVariant.copy(alpha = 0.35f),
        )
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
private fun MoveDialog(
    collections: List<QuoteCollection>,
    from: Long,
    onDismiss: () -> Unit,
    onMove: (QuoteCollection) -> Unit,
) {
    var target by remember { mutableStateOf(collections.firstOrNull { it.id != from }) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Move to") },
        text = {
            Column {
                collections.filter { it.id != from }.forEach { c ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .selectable(selected = target?.id == c.id, onClick = { target = c }, role = Role.RadioButton)
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = target?.id == c.id, onClick = null)
                        Spacer(Modifier.width(12.dp))
                        Text(c.name, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { target?.let(onMove) }, enabled = target != null) { Text("Move") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
internal fun NameDialog(title: String, initial: String, confirm: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var name by rememberSaveable { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(40) },
                singleLine = true,
                placeholder = { Text("For example: Stoics, Work, Morning") },
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = { TextButton(onClick = { onSave(name.trim()) }, enabled = name.isNotBlank()) { Text(confirm) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
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
            "Paste quotes you’ve saved anywhere, or restore a backup in Settings. Your widget starts rotating through them right away.",
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
