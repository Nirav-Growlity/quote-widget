@file:OptIn(ExperimentalFoundationApi::class)

package io.github.niravgrowlity.quotewidget.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.niravgrowlity.quotewidget.Quote
import io.github.niravgrowlity.quotewidget.QuoteFormat
import io.github.niravgrowlity.quotewidget.QuoteStore
import io.github.niravgrowlity.quotewidget.Rotation
import kotlinx.coroutines.launch

/**
 * The first thing you see: one quote filling the screen, in the widget's font. Swipe up or down
 * to browse every quote. Browsing never changes the widget; "Show on widget" does.
 */
@Composable
fun StageScreen(
    state: AppState,
    covered: Boolean,
    onQuotes: () -> Unit,
    onWidget: () -> Unit,
    onSettings: () -> Unit,
    onAdd: () -> Unit,
) {
    val ctx = LocalContext.current
    val tick = state.tick
    val quotes = remember(tick) { QuoteStore.all(ctx) }
    // A page is on top of the stage: keep screen readers off what's underneath.
    val hidden = if (covered) Modifier.clearAndSetSemantics { } else Modifier

    if (quotes.isEmpty()) {
        EmptyStage(hidden, onAdd, onQuotes, onWidget, onSettings)
        return
    }

    val currentId = remember(tick) { QuoteStore.current(ctx)?.id }
    val collections = remember(tick) { QuoteStore.collections(ctx) }
    val family = remember(tick) {
        FontFamily(QuoteStore.font(ctx).typeface(ctx) ?: QuoteStore.style(ctx).typeface())
    }

    val pager = rememberPagerState(
        initialPage = quotes.indexOfFirst { it.id == currentId }.coerceAtLeast(0),
    ) { quotes.size }

    // When the widget moves on by itself (timer, a tap on the home screen), follow it. Not when
    // a pin made here changed what's in rotation: the quote you just touched stays in front of you.
    val stay = remember { booleanArrayOf(false) }
    LaunchedEffect(currentId) {
        if (stay[0]) {
            stay[0] = false
            return@LaunchedEffect
        }
        val i = quotes.indexOfFirst { it.id == currentId }
        if (i >= 0 && i != pager.currentPage && !pager.isScrollInProgress) pager.scrollToPage(i)
    }

    // The one entrance in the app: the quote rises into place when the app opens.
    val enter = remember { Animatable(0f) }
    LaunchedEffect(Unit) { enter.animateTo(1f, tween(520, easing = FastOutSlowInEasing)) }

    Box(Modifier.fillMaxSize().then(hidden)) {
        VerticalPager(
            state = pager,
            modifier = Modifier.fillMaxSize(),
            key = { i -> quotes.getOrNull(i)?.id ?: -1L - i },
        ) { page ->
            val q = quotes.getOrNull(page) ?: return@VerticalPager
            StagePage(
                quote = q,
                family = family,
                collection = if (collections.size > 1) collections.firstOrNull { it.id == q.collection }?.name else null,
                onWidget = q.id == currentId,
                entrance = { enter.value },
                onPin = {
                    QuoteStore.update(ctx, q.copy(pinned = !q.pinned))
                    if (QuoteStore.current(ctx)?.id != currentId) stay[0] = true
                    state.changed()
                },
                onShow = {
                    if (QuoteStore.showNow(ctx, q.id)) {
                        Rotation.schedule(ctx, force = true)
                        state.changed()
                    } else {
                        state.say(notInRotationReason(ctx, q))
                    }
                },
            )
        }

        StageControls(Modifier.align(Alignment.BottomCenter), onQuotes, onWidget, onSettings)
    }
}

/** The way to everything else. White on any stage colour, so it never needs to adapt. */
@Composable
private fun StageControls(modifier: Modifier, onQuotes: () -> Unit, onWidget: () -> Unit, onSettings: () -> Unit) {
    Row(
        modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PopButton(onClick = onQuotes, color = Color.White, contentColor = Ink) {
            Row(
                Modifier.padding(start = 16.dp, end = 20.dp, top = 14.dp, bottom = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(AppIcons.Quote, contentDescription = null, Modifier.size(22.dp))
                Spacer(Modifier.width(8.dp))
                Text("Quotes", style = MaterialTheme.typography.titleSmall)
            }
        }
        Spacer(Modifier.weight(1f))
        PopButton(onClick = onWidget, modifier = Modifier.size(52.dp), color = Color.White, contentColor = Ink) {
            Icon(AppIcons.Widgets, contentDescription = "Widget")
        }
        PopButton(onClick = onSettings, modifier = Modifier.size(52.dp), color = Color.White, contentColor = Ink) {
            Icon(Icons.Rounded.Settings, contentDescription = "Settings")
        }
    }
}

/** Short quotes go big; long ones step down so they still fit. */
private fun stageSize(chars: Int) = when {
    chars <= 50 -> 44
    chars <= 100 -> 36
    chars <= 180 -> 29
    chars <= 300 -> 24
    else -> 20
}

@Composable
private fun StagePage(
    quote: Quote,
    family: FontFamily,
    collection: String?,
    onWidget: Boolean,
    entrance: () -> Float,
    onPin: () -> Unit,
    onShow: () -> Unit,
) {
    val c = StageColors.of(quote.id)
    val (body, author) = remember(quote.text) { QuoteFormat.split(quote.text) }
    val size = stageSize(body.length)

    Box(Modifier.fillMaxSize().background(c.field)) {
        if (collection != null) {
            Text(
                collection,
                style = MaterialTheme.typography.labelLarge,
                color = c.ink.copy(alpha = 0.6f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.statusBarsPadding().padding(start = 28.dp, end = 28.dp, top = 20.dp),
            )
        }
        Column(
            Modifier
                .align(Alignment.CenterStart)
                .fillMaxWidth()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(start = 28.dp, end = 28.dp, top = 56.dp, bottom = 96.dp)
                .graphicsLayer {
                    val t = entrance()
                    alpha = t
                    translationY = (1f - t) * 36.dp.toPx()
                },
        ) {
            Text(
                body,
                fontFamily = family,
                fontSize = size.sp,
                lineHeight = (size * 1.22f).sp,
                color = c.ink,
                maxLines = 12,
                overflow = TextOverflow.Ellipsis,
            )
            if (author != null) {
                Text(
                    author,
                    style = MaterialTheme.typography.titleMedium,
                    color = c.ink.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 16.dp),
                )
            }
            Row(
                Modifier.padding(top = 28.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StagePin(quote.pinned, c, onPin)
                if (onWidget) {
                    Icon(
                        AppIcons.Widgets, contentDescription = null,
                        tint = c.ink.copy(alpha = 0.7f),
                        modifier = Modifier.padding(start = 6.dp).size(18.dp),
                    )
                    Text("On your widget", style = MaterialTheme.typography.labelLarge, color = c.ink.copy(alpha = 0.7f))
                } else {
                    PopButton(onClick = onShow, color = c.ink.copy(alpha = 0.12f), contentColor = c.ink) {
                        Text(
                            "Show on widget",
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
                        )
                    }
                }
            }
        }
    }
}

/** The pin fills in, straightens up and bounces when you pin a quote. */
@Composable
private fun StagePin(pinned: Boolean, c: StageColor, onToggle: () -> Unit) {
    val scope = rememberCoroutineScope()
    val pop = remember { Animatable(1f) }
    val tilt by animateFloatAsState(
        targetValue = if (pinned) 0f else 35f,
        animationSpec = spring(dampingRatio = 0.45f, stiffness = 400f),
        label = "tilt",
    )
    val fill by animateColorAsState(if (pinned) c.ink else c.ink.copy(alpha = 0.12f), label = "fill")
    val tint by animateColorAsState(if (pinned) c.field else c.ink, label = "tint")
    PopButton(
        onClick = {
            onToggle()
            scope.launch {
                pop.snapTo(0.7f)
                pop.animateTo(1f, spring(dampingRatio = 0.35f, stiffness = 500f))
            }
        },
        modifier = Modifier.size(52.dp).graphicsLayer { scaleX = pop.value; scaleY = pop.value },
        shape = CircleShape,
        color = fill,
        contentColor = tint,
    ) {
        Icon(
            AppIcons.Pin,
            contentDescription = if (pinned) "Unpin" else "Pin",
            modifier = Modifier.graphicsLayer { rotationZ = tilt },
        )
    }
}

@Composable
private fun EmptyStage(
    modifier: Modifier,
    onAdd: () -> Unit,
    onQuotes: () -> Unit,
    onWidget: () -> Unit,
    onSettings: () -> Unit,
) {
    val c = StageColors.all[5]
    Box(modifier.fillMaxSize().background(c.field)) {
        StageControls(Modifier.align(Alignment.BottomCenter), onQuotes, onWidget, onSettings)
        Column(
            Modifier
                .align(Alignment.CenterStart)
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 28.dp),
        ) {
            Text(
                "Add your first quotes",
                style = MaterialTheme.typography.displaySmall.copy(fontSize = 48.sp, lineHeight = 50.sp),
                color = c.ink,
            )
            Row(
                Modifier.padding(top = 28.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PopButton(onClick = onAdd, color = Color.White, contentColor = Ink) {
                    Row(
                        Modifier.padding(start = 16.dp, end = 22.dp, top = 16.dp, bottom = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null, Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Add quotes", style = MaterialTheme.typography.titleSmall)
                    }
                }
                PopButton(onClick = onSettings, color = c.ink.copy(alpha = 0.16f), contentColor = c.ink) {
                    Text(
                        "Restore a backup",
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 17.dp),
                    )
                }
            }
        }
    }
}
