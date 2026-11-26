package io.github.niravgrowlity.quotewidget.ui

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.niravgrowlity.quotewidget.ThemeMode

// Fallback palette for Android 8–11. On 12+ colours come from the wallpaper.
private val Light = lightColorScheme(
    primary = Color(0xFF3F5AA6), onPrimary = Color.White,
    primaryContainer = Color(0xFFDCE1FF), onPrimaryContainer = Color(0xFF00164E),
    secondaryContainer = Color(0xFFDFE1F9), onSecondaryContainer = Color(0xFF161B2C),
    tertiary = Color(0xFF8A4F7D), tertiaryContainer = Color(0xFFFFD7F1),
    background = Color(0xFFFBF8FD), surface = Color(0xFFFBF8FD), onSurface = Color(0xFF1B1B1F),
    onSurfaceVariant = Color(0xFF45464F), outlineVariant = Color(0xFFC6C5D0),
    surfaceContainerLow = Color(0xFFF5F2F7), surfaceContainer = Color(0xFFEFEDF1),
    surfaceContainerHigh = Color(0xFFE9E7EC), surfaceContainerHighest = Color(0xFFE4E1E6),
)
private val Dark = darkColorScheme(
    primary = Color(0xFFB6C4FF), onPrimary = Color(0xFF0F2357),
    primaryContainer = Color(0xFF2F3F6E), onPrimaryContainer = Color(0xFFDCE1FF),
    secondaryContainer = Color(0xFF3F4357), onSecondaryContainer = Color(0xFFDFE1F9),
    tertiary = Color(0xFFFFADE5), tertiaryContainer = Color(0xFF6E3864),
    background = Color(0xFF131316), surface = Color(0xFF131316), onSurface = Color(0xFFE4E2E6),
    onSurfaceVariant = Color(0xFFC6C5D0), outlineVariant = Color(0xFF45464F),
    surfaceContainerLow = Color(0xFF1B1B1F), surfaceContainer = Color(0xFF1F1F23),
    surfaceContainerHigh = Color(0xFF2A2A2D), surfaceContainerHighest = Color(0xFF353438),
)

@Composable
fun isDark(mode: ThemeMode) = when (mode) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}

@Composable
fun QuoteTheme(dark: Boolean, content: @Composable () -> Unit) {
    val ctx = LocalContext.current
    val scheme = when {
        Build.VERSION.SDK_INT >= 31 && dark -> dynamicDarkColorScheme(ctx)
        Build.VERSION.SDK_INT >= 31 -> dynamicLightColorScheme(ctx)
        dark -> Dark
        else -> Light
    }
    MaterialTheme(colorScheme = scheme, content = content)
}

/** Quotes are the content people read, so they get a serif; the rest of the UI stays sans. */
object QuoteType {
    val hero = TextStyle(fontFamily = FontFamily.Serif, fontSize = 23.sp, lineHeight = 32.sp)
    val row = TextStyle(fontFamily = FontFamily.Serif, fontSize = 17.sp, lineHeight = 25.sp)
    val mark = TextStyle(fontFamily = FontFamily.Serif, fontSize = 76.sp, lineHeight = 76.sp)
}

/** The few icons not in material-icons-core, as Material Symbols paths. */
object AppIcons {
    val Quote = icon("Quote", "M6,17h3l2,-4V7H5v6h3zM14,17h3l2,-4V7h-6v6h3z")
    val Widgets = icon(
        "Widgets",
        "M13,13v8h8v-8h-8zM3,21h8v-8H3v8zM3,3v8h8V3H3zM16.66,1.69L11,7.34 16.66,13l5.66,-5.66 -5.66,-5.65z",
    )
    val Pin = icon(
        "Pin",
        "M16,9V4h1c0.55,0 1,-0.45 1,-1s-0.45,-1 -1,-1H7C6.45,2 6,2.45 6,3s0.45,1 1,1h1v5c0,1.66 -1.34,3 -3,3v2h5.97v7l1,1 1,-1v-7H19v-2c-1.66,0 -3,-1.34 -3,-3z",
    )
    val Folder = icon(
        "Folder",
        "M10,4H4c-1.1,0 -1.99,0.9 -1.99,2L2,18c0,1.1 0.9,2 2,2h16c1.1,0 2,-0.9 2,-2V8c0,-1.1 -0.9,-2 -2,-2h-8l-2,-2z",
    )
    val Image = icon(
        "Image",
        "M21,19V5c0,-1.1 -0.9,-2 -2,-2H5c-1.1,0 -2,0.9 -2,2v14c0,1.1 0.9,2 2,2h14c1.1,0 2,-0.9 2,-2zM8.5,13.5l2.5,3.01L14.5,12l4.5,6H5l3.5,-4.5z",
    )
    val Next = icon("Next", "M6,18l8.5,-6L6,6v12zM16,6v12h2V6h-2z")
    val Paste = icon(
        "Paste",
        "M19,2h-4.18C14.4,0.84 13.3,0 12,0S9.6,0.84 9.18,2H5C3.9,2 3,2.9 3,4v16c0,1.1 0.9,2 2,2h14c1.1,0 2,-0.9 2,-2V4C21,2.9 20.1,2 19,2zM12,2c0.55,0 1,0.45 1,1s-0.45,1 -1,1 -1,-0.45 -1,-1S11.45,2 12,2zM19,20H5V4h2v3h10V4h2V20z",
    )

    private fun icon(name: String, path: String): ImageVector =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f)
            .addPath(addPathNodes(path), fill = SolidColor(Color.Black))
            .build()
}
