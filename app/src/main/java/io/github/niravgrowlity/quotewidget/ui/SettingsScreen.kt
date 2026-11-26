package io.github.niravgrowlity.quotewidget.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.niravgrowlity.quotewidget.BuildConfig
import io.github.niravgrowlity.quotewidget.QuoteStore
import io.github.niravgrowlity.quotewidget.ThemeMode

/**
 * App-wide settings: theme, updates and about. Widget looks stay in the Widget tab.
 * New app-level options belong here.
 */
@Composable
fun SettingsScreen(state: AppState, padding: PaddingValues) {
    val ctx = LocalContext.current
    val uri = LocalUriHandler.current
    state.tick
    val repo = "https://github.com/${BuildConfig.UPDATE_REPO}"

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(top = padding.calculateTopPadding() + 20.dp, bottom = padding.calculateBottomPadding() + 24.dp),
    ) {
        Text(
            "Settings",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 20.dp, bottom = 4.dp),
        )

        SectionTitle("Appearance")
        Setting("Theme") {
            Segmented(ThemeMode.entries, state.themeMode, { it.label }) { state.setTheme(it) }
            Hint("Applies to the app and the “Match my wallpaper” widget styles. Palette styles keep their own colours.")
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
        SwitchRow(
            "Check when the app opens",
            "Shows a popup when a new version is out. Uses the network only while the app is open.",
            QuoteStore.autoUpdateCheck(ctx),
        ) {
            QuoteStore.setAutoUpdateCheck(ctx, it)
            state.changed(widget = false)
        }

        SectionTitle("About")
        LinkRow("Source code and releases", "github.com/${BuildConfig.UPDATE_REPO}") { uri.openUri(repo) }
        LinkRow("Font licences", "Open-source fonts from Google Fonts") { uri.openUri("$repo/blob/main/FONT_LICENSES.txt") }
        LinkRow(
            "Theme credits",
            "Catppuccin, Rosé Pine, Nord, Gruvbox, Dracula, Solarized, Tokyo Night, Everforest, Kanagawa, One Dark",
        ) { uri.openUri("$repo#credits") }
    }
}

@Composable
private fun Hint(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 8.dp),
    )
}

@Composable
private fun LinkRow(title: String, subtitle: String, onClick: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
