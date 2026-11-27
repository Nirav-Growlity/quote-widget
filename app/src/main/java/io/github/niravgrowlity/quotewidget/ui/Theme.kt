package io.github.niravgrowlity.quotewidget.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.niravgrowlity.quotewidget.R
import io.github.niravgrowlity.quotewidget.ThemeMode

// The app's own pages are ink on white (or the reverse), so the stage colours are the only loud thing.
internal val Ink = Color(0xFF16141D)
private val Paper = Color(0xFFF4F2F8)

private val Light = lightColorScheme(
    primary = Ink, onPrimary = Color.White,
    primaryContainer = Color(0xFFE9E7F0), onPrimaryContainer = Ink,
    secondary = Ink, onSecondary = Color.White,
    secondaryContainer = Ink, onSecondaryContainer = Color.White,
    tertiary = Color(0xFF7B4DFF), onTertiary = Color.White,
    tertiaryContainer = Color(0xFFE6DDFF), onTertiaryContainer = Color(0xFF22005D),
    background = Color.White, onBackground = Ink, surface = Color.White, onSurface = Ink,
    surfaceVariant = Color(0xFFECEAF2), onSurfaceVariant = Color(0xFF5F5C6B),
    outline = Color(0xFF8E8B99), outlineVariant = Color(0xFFDEDCE5),
    surfaceContainerLowest = Color.White, surfaceContainerLow = Color(0xFFF6F5F9),
    surfaceContainer = Color(0xFFF1F0F5), surfaceContainerHigh = Color(0xFFEBEAF0),
    surfaceContainerHighest = Color(0xFFE5E3EB),
    inverseSurface = Ink, inverseOnSurface = Color.White, inversePrimary = Color(0xFFFFD23F),
)
private val Dark = darkColorScheme(
    primary = Paper, onPrimary = Ink,
    primaryContainer = Color(0xFF2C2A36), onPrimaryContainer = Paper,
    secondary = Paper, onSecondary = Ink,
    secondaryContainer = Paper, onSecondaryContainer = Ink,
    tertiary = Color(0xFFB9A2FF), onTertiary = Color(0xFF22005D),
    tertiaryContainer = Color(0xFF3D2A7A), onTertiaryContainer = Color(0xFFE6DDFF),
    background = Color(0xFF121117), onBackground = Paper, surface = Color(0xFF121117), onSurface = Paper,
    surfaceVariant = Color(0xFF2C2A36), onSurfaceVariant = Color(0xFFA9A6B5),
    outline = Color(0xFF767382), outlineVariant = Color(0xFF34323E),
    surfaceContainerLowest = Color(0xFF0D0C11), surfaceContainerLow = Color(0xFF1A1921),
    surfaceContainer = Color(0xFF1F1E27), surfaceContainerHigh = Color(0xFF282730),
    surfaceContainerHighest = Color(0xFF32313B),
    inverseSurface = Paper, inverseOnSurface = Ink, inversePrimary = Color(0xFF5B35D6),
)

/** Page titles and dialog titles; everything else is the phone's sans. */
private val Display = FontFamily(Font(R.font.qf_instrument_serif))

private val AppTypography = Typography().let { t ->
    t.copy(
        displaySmall = t.displaySmall.copy(fontFamily = Display),
        headlineLarge = t.headlineLarge.copy(fontFamily = Display, fontSize = 40.sp, lineHeight = 44.sp),
        headlineMedium = t.headlineMedium.copy(fontFamily = Display),
        headlineSmall = t.headlineSmall.copy(fontFamily = Display),
    )
}

@Composable
fun isDark(mode: ThemeMode) = when (mode) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}

@Composable
fun QuoteTheme(dark: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (dark) Dark else Light, typography = AppTypography, content = content)
}

/** A colour field for the stage and the ink that reads on it. */
class StageColor(val field: Color, val ink: Color)

/** Each quote keeps one of these, so swiping moves through colour as well as words. */
object StageColors {
    val all = listOf(
        StageColor(Color(0xFFFF7A3D), Color(0xFF2A0F00)), // tangerine
        StageColor(Color(0xFF2F4BFF), Color.White), // cobalt
        StageColor(Color(0xFFFF8FC7), Color(0xFF3A0A26)), // bubblegum
        StageColor(Color(0xFFFFD23F), Color(0xFF2B2100)), // sunflower
        StageColor(Color(0xFF5BE3A8), Color(0xFF04291B)), // mint
        StageColor(Color(0xFF7B4DFF), Color.White), // grape
    )

    fun of(id: Long): StageColor = all[Math.floorMod(id, all.size.toLong()).toInt()]
}

/** A surface that dips when pressed and springs back. */
@Composable
fun PopButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = CircleShape,
    color: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    content: @Composable () -> Unit,
) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.92f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 700f),
        label = "press",
    )
    Surface(
        onClick = onClick,
        modifier = modifier.graphicsLayer { scaleX = scale; scaleY = scale },
        shape = shape,
        color = color,
        contentColor = contentColor,
        interactionSource = source,
    ) {
        Box(contentAlignment = Alignment.Center) { content() }
    }
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
