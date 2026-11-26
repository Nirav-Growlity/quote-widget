@file:OptIn(ExperimentalMaterial3Api::class)

package io.github.niravgrowlity.quotewidget.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import io.github.niravgrowlity.quotewidget.QuoteParser
import io.github.niravgrowlity.quotewidget.QuoteStore

private val SPLIT_MODES = listOf(
    "Auto" to "Splits on blank lines if there are any, otherwise on each line.",
    "Each line" to "Every line becomes its own quote.",
    "Blank lines" to "Lines stay together until a blank line, so a quote can have its author underneath.",
)

@Composable
fun AddQuotesScreen(state: AppState, onClose: () -> Unit) {
    val ctx = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var text by rememberSaveable { mutableStateOf("") }
    var mode by rememberSaveable { mutableIntStateOf(0) }
    var replace by rememberSaveable { mutableStateOf(false) }
    var confirmReplace by remember { mutableStateOf(false) }
    val existing = remember { QuoteStore.all(ctx).size }

    val parsed = remember(text, mode) {
        val blank = when (mode) {
            1 -> false
            2 -> true
            else -> QuoteParser.hasBlankLines(text)
        }
        QuoteParser.parse(text, blank)
    }

    fun commit() {
        if (replace) {
            QuoteStore.replaceAll(ctx, parsed)
            state.say("Replaced your list with ${parsed.size} quotes")
        } else {
            val added = QuoteStore.addAll(ctx, parsed)
            state.say(
                when (added) {
                    0 -> "Those quotes are already in your list"
                    1 -> "Added 1 quote"
                    else -> "Added $added quotes"
                },
            )
        }
        state.changed()
        onClose()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add quotes") },
                navigationIcon = {
                    IconButton(onClick = onClose) { Icon(Icons.Rounded.Close, contentDescription = "Close") }
                },
                actions = {
                    Button(
                        onClick = { if (replace && existing > 0) confirmReplace = true else commit() },
                        enabled = parsed.isNotEmpty(),
                        modifier = Modifier.padding(end = 12.dp),
                    ) { Text(if (parsed.isEmpty()) "Add" else "Add ${parsed.size}") }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).imePadding().padding(horizontal = 16.dp),
        ) {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                SPLIT_MODES.forEachIndexed { i, (label, _) ->
                    SegmentedButton(
                        selected = mode == i,
                        onClick = { mode = i },
                        shape = SegmentedButtonDefaults.itemShape(i, SPLIT_MODES.size),
                    ) { Text(label) }
                }
            }
            Text(
                SPLIT_MODES[mode].second,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, top = 8.dp),
            )
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text("Paste or type your quotes", style = QuoteType.row) },
                textStyle = QuoteType.row,
                modifier = Modifier.fillMaxWidth().weight(1f).padding(top = 12.dp),
            )
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                AssistChip(
                    onClick = {
                        val clip = clipboard.getText()?.text.orEmpty()
                        if (clip.isNotBlank()) text = if (text.isBlank()) clip else text.trimEnd() + "\n" + clip
                    },
                    label = { Text("Paste from clipboard") },
                    leadingIcon = { Icon(AppIcons.Paste, contentDescription = null, Modifier.size(18.dp)) },
                )
                Spacer(Modifier.weight(1f))
                if (parsed.isNotEmpty()) {
                    Text(
                        if (parsed.size == 1) "1 quote found" else "${parsed.size} quotes found",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            if (existing > 0) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .toggleable(value = replace, onValueChange = { replace = it }, role = Role.Switch)
                        .padding(vertical = 8.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Replace my current list", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "Removes your $existing quotes and pins first",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(checked = replace, onCheckedChange = null)
                }
            }
        }
    }

    if (confirmReplace) {
        AlertDialog(
            onDismissRequest = { confirmReplace = false },
            title = { Text("Replace all quotes?") },
            text = { Text("Your $existing quotes and pins will be removed and replaced with these ${parsed.size}.") },
            confirmButton = { TextButton(onClick = { confirmReplace = false; commit() }) { Text("Replace") } },
            dismissButton = { TextButton(onClick = { confirmReplace = false }) { Text("Cancel") } },
        )
    }
}
