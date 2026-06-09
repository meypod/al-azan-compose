package com.github.meypod.al_azan.alarm

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.AnchoredDraggableDefaults
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.snapTo
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.github.meypod.al_azan.R
import com.github.meypod.al_azan.core.domain.model.settings.ThemeColor
import com.github.meypod.al_azan.core.presentation.AlAzanTheme
import com.github.meypod.al_azan.core.presentation.components.ChevronsUpAnimated
import com.github.meypod.al_azan.core.presentation.util.rememberPatternImageBitmap
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private val BG_GRADIENT_START = Color(0xFF00AC83)
private val BG_GRADIENT_END = Color(0xFF006876)
private val ACCENT_DARK = Color(0xFF00585A)
private const val PATTERN_ALPHA = 0.05f
private val DRAG_MENU_THRESHOLD_DP = 160.dp
private const val MORPH_DURATION_MS = 260

private enum class DismissAnchor { Rest, Menu }

/**
 * Fullscreen alarm screen — preview-only.
 *
 * States:
 *   Pill          : initial. Press → morphs to Circle.
 *   Circle        : round button. Drag up past positional/velocity threshold → Menu.
 *                   Release below threshold → springs back to Rest, pill returns.
 *   Menu          : action menu. Tap any action → reset to Pill.
 */
@Composable
fun AlarmFullscreenScreen(
    uiState: AlarmFullscreenUiState,
    onAction: (AlarmFullscreenUiAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    var menuVisible by remember { mutableStateOf(false) }
    var isPressed by remember { mutableStateOf(false) }
    val density = androidx.compose.ui.platform.LocalDensity.current
    val menuThresholdPx = with(density) { DRAG_MENU_THRESHOLD_DP.toPx() }
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current

    val dragState = remember {
        AnchoredDraggableState(
            initialValue = DismissAnchor.Rest,
            anchors = DraggableAnchors { DismissAnchor.Rest at 0f },
        )
    }
    val flingBehavior = AnchoredDraggableDefaults.flingBehavior(
        state = dragState,
        positionalThreshold = { it * 0.5f },
        animationSpec = tween(200),
    )
    LaunchedEffect(menuThresholdPx) {
        dragState.updateAnchors(
            DraggableAnchors {
                DismissAnchor.Rest at 0f
                DismissAnchor.Menu at -menuThresholdPx
            },
        )
    }
    LaunchedEffect(dragState.settledValue) {
        if (dragState.settledValue == DismissAnchor.Menu && !menuVisible) {
            menuVisible = true
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            scope.launch {
                dragState.animateTo(DismissAnchor.Rest, tween(MORPH_DURATION_MS))
            }
        }
    }
    LaunchedEffect(isPressed) {
        if (isPressed) {
            haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
        }
    }

    var settling by remember { mutableStateOf(false) }
    LaunchedEffect(isPressed) {
        val off = dragState.offset
        if (!isPressed && !off.isNaN() && off != 0f) {
            settling = true
            snapshotFlow { dragState.offset }.first { it == 0f || it.isNaN() }
            settling = false
        }
    }
    val offsetNonZero by remember {
        derivedStateOf {
            val o = dragState.offset
            !o.isNaN() && o != 0f
        }
    }
    val isCircle = !menuVisible && (isPressed || settling || offsetNonZero)

    val classic = uiState.themeColor.isClassic()
    val darkClassic = uiState.themeColor == ThemeColor.ClassicDark
    val contentColor = if (classic) MaterialTheme.colorScheme.onBackground else Color.White
    val pattern = rememberPatternImageBitmap(R.drawable.pattern)

    val pillWidth by animateDpAsState(
        if (isCircle) 95.dp else 220.dp,
        tween(MORPH_DURATION_MS),
        label = "pillWidth",
    )
    val pillHeight by animateDpAsState(if (isCircle) 95.dp else 95.dp, tween(MORPH_DURATION_MS), label = "pillHeight")
    val corner by animateDpAsState(if (isCircle) 999.dp else 999.dp, tween(MORPH_DURATION_MS), label = "corner")
    val pressAlpha by animateFloatAsState(
        targetValue = if (isCircle) 1f else 0f,
        animationSpec = tween(durationMillis = MORPH_DURATION_MS),
        label = "pressAlpha",
    )
    val hintAlpha = 1f - pressAlpha
    val chevronAlpha = pressAlpha

    fun resetToPill() {
        menuVisible = false
        scope.launch { dragState.snapTo(DismissAnchor.Rest) }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .then(
                if (classic) {
                    Modifier.background(MaterialTheme.colorScheme.background)
                } else {
                    Modifier
                        .background(Brush.verticalGradient(listOf(BG_GRADIENT_START, BG_GRADIENT_END)))
                        .background(
                            ShaderBrush(ImageShader(pattern, TileMode.Repeated, TileMode.Repeated)),
                            alpha = PATTERN_ALPHA,
                        )
                },
            ),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing),
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(dimensionResource(R.dimen.page_padding)),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.element_padding)),
            ) {
                Spacer(Modifier.height(30.dp))
                Text(
                    uiState.header,
                    color = contentColor,
                    letterSpacing = 2.sp,
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(uiState.title, color = contentColor, style = MaterialTheme.typography.displayMedium)
                Text(uiState.timeLabel, color = contentColor, style = MaterialTheme.typography.headlineLarge)
            }

            if (menuVisible) {
                Dialog(
                    onDismissRequest = { resetToPill() },
                ) {
                    AlarmActionMenu(
                        darkClassic = darkClassic,
                        dismissAndSilentMinutes = uiState.dismissAndSilentMinutes,
                        shortRemindMinutes = uiState.shortRemindMinutes,
                        longRemindMinutes = uiState.longRemindMinutes,
                        autoSilentOnDismiss = uiState.autoSilentOnDismiss,
                        onDismissAndSilent = {
                            onAction(AlarmFullscreenUiAction.OnDismissAndSilent)
                            resetToPill()
                        },
                        onShortRemind = {
                            onAction(AlarmFullscreenUiAction.OnShortRemind)
                            resetToPill()
                        },
                        onLongRemind = {
                            onAction(AlarmFullscreenUiAction.OnLongRemind)
                            resetToPill()
                        },
                        onJustDismiss = {
                            onAction(AlarmFullscreenUiAction.OnDismiss)
                            resetToPill()
                        },
                    )
                }
            }

            Column(
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 56.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(contentAlignment = Alignment.BottomCenter) {
                    ChevronsUpAnimated(
                        tint = contentColor,
                        modifier = Modifier.alpha(chevronAlpha),
                    )
                    Text(
                        stringResource(R.string.alarm_swipe_up_hint),
                        color = contentColor,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.alpha(hintAlpha),
                    )
                }
                Spacer(Modifier.height(dimensionResource(R.dimen.element_padding)))

                Box(contentAlignment = Alignment.Center) {
                    if (!isCircle && !menuVisible) {
                        PulseHalo(
                            baseWidth = pillWidth,
                            baseHeight = pillHeight,
                            baseCorner = corner,
                            color = contentColor,
                        )
                    }
                    Surface(
                        color = Color.White,
                        shape = RoundedCornerShape(corner),
                        shadowElevation = 6.dp,
                        modifier = Modifier
                            .graphicsLayer {
                                val o = dragState.offset
                                translationY = if (o.isNaN()) 0f else o
                            }
                            .width(pillWidth)
                            .height(pillHeight)
                            // Initial-pass press observer. Does not consume, so anchoredDraggable
                            // still receives all events. Drives morph-on-touch.
                            .pointerInput(Unit) {
                                awaitPointerEventScope {
                                    while (true) {
                                        val event = awaitPointerEvent(PointerEventPass.Initial)
                                        val pressed = event.changes.any { it.pressed }
                                        if (pressed != isPressed) isPressed = pressed
                                    }
                                }
                            }
                            .anchoredDraggable(
                                state = dragState,
                                orientation = Orientation.Vertical,
                                flingBehavior = flingBehavior,
                            )
                            // Tapping the pill dismisses, same as "just dismiss" in the swipe-up menu.
                            .pointerInput(Unit) {
                                detectTapGestures(onTap = { onAction(AlarmFullscreenUiAction.OnDismiss) })
                            },
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.icon_padding)),
                            ) {
                                Icon(
                                    painterResource(R.drawable.alarm_turn_off),
                                    contentDescription = null,
                                    tint = ACCENT_DARK,
                                    modifier = Modifier.size(34.dp),
                                )
                                AnimatedVisibility(
                                    visible = !isCircle,
                                    enter = fadeIn(tween(MORPH_DURATION_MS)) +
                                        expandHorizontally(tween(MORPH_DURATION_MS)),
                                    exit = fadeOut(tween(MORPH_DURATION_MS)) +
                                        shrinkHorizontally(tween(MORPH_DURATION_MS)),
                                ) {
                                    Text(
                                        stringResource(R.string.alarm_dismiss),
                                        color = ACCENT_DARK,
                                        style = MaterialTheme.typography.headlineMedium,
                                        maxLines = 1,
                                        softWrap = false,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Expanding ring behind dismiss button. Uses [graphicsLayer] scale so expansion
 * does not move the button. Absolute expansion in dp is applied to both axes
 * (not a uniform scale) so the pill's wider X does not get stretched disproportionately.
 * Two staggered pulses loop so the second fires while the first is still animating.
 */
@Composable
private fun PulseHalo(
    baseWidth: Dp,
    baseHeight: Dp,
    baseCorner: Dp,
    expandBy: Dp = 35.dp,
    pulseDurationMs: Int = 1200,
    staggerMs: Long = 600L,
    longPauseMs: Long = 1200L,
    ringWidth: Dp = 4.dp,
    startAlpha: Float = 0.85f,
    color: Color = Color.White,
) {
    val p1 = remember { androidx.compose.animation.core.Animatable(0f) }
    val p2 = remember { androidx.compose.animation.core.Animatable(0f) }

    LaunchedEffect(Unit) {
        while (true) {
            p1.snapTo(0f)
            p2.snapTo(0f)
            kotlinx.coroutines.coroutineScope {
                launch { p1.animateTo(1f, tween(pulseDurationMs, easing = LinearEasing)) }
                launch {
                    delay(staggerMs)
                    p2.animateTo(1f, tween(pulseDurationMs, easing = LinearEasing))
                }
            }
            delay(longPauseMs)
        }
    }

    Box(contentAlignment = Alignment.Center) {
        Ring(baseWidth, baseHeight, baseCorner, expandBy, ringWidth, color, startAlpha, p1.value)
        Ring(baseWidth, baseHeight, baseCorner, expandBy, ringWidth, color, startAlpha, p2.value)
    }
}

@Composable
private fun Ring(
    baseWidth: Dp,
    baseHeight: Dp,
    baseCorner: Dp,
    expandBy: Dp,
    ringWidth: Dp,
    color: Color,
    startAlpha: Float,
    t: Float,
) {
    val density = androidx.compose.ui.platform.LocalDensity.current
    val basePxW = with(density) { baseWidth.toPx() }
    val basePxH = with(density) { baseHeight.toPx() }
    val expandPx = with(density) { expandBy.toPx() }
    val scaleX = (basePxW + expandPx * t) / basePxW
    val scaleY = (basePxH + expandPx * t) / basePxH
    val alpha = (1f - t) * startAlpha
    Box(
        modifier = Modifier
            .width(baseWidth)
            .height(baseHeight)
            .graphicsLayer {
                this.scaleX = scaleX
                this.scaleY = scaleY
            }
            .border(
                width = ringWidth,
                color = color.copy(alpha = alpha),
                shape = RoundedCornerShape(baseCorner),
            ),
    )
}

@Composable
private fun AlarmActionMenu(
    darkClassic: Boolean,
    dismissAndSilentMinutes: Int,
    shortRemindMinutes: Int,
    longRemindMinutes: Int,
    autoSilentOnDismiss: Boolean,
    onDismissAndSilent: () -> Unit,
    onShortRemind: () -> Unit,
    onLongRemind: () -> Unit,
    onJustDismiss: () -> Unit,
) {
    Surface(
        color = if (darkClassic) MaterialTheme.colorScheme.surface else Color(0xFFEDEDED),
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 6.dp,
        modifier = Modifier.padding(24.dp),
    ) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Hidden unless the app holds DND policy access (minutes gated to 0 in the ViewModel).
            if (dismissAndSilentMinutes > 0) {
                MenuPillButton(
                    darkClassic = darkClassic,
                    text = stringResource(R.string.alarm_dismiss_and_silent, dismissAndSilentMinutes),
                    iconRes = R.drawable.alarm_silent,
                    onClick = onDismissAndSilent,
                )
            }
            if (shortRemindMinutes > 0) {
                MenuPillButton(
                    darkClassic = darkClassic,
                    text = stringResource(R.string.alarm_remind, shortRemindMinutes),
                    iconRes = R.drawable.alarm_snooze,
                    onClick = onShortRemind,
                )
            }
            if (longRemindMinutes > 0) {
                MenuPillButton(
                    darkClassic = darkClassic,
                    text = stringResource(R.string.alarm_remind, longRemindMinutes),
                    iconRes = R.drawable.alarm_snooze,
                    onClick = onLongRemind,
                )
            }
            Button(
                onClick = onJustDismiss,
                shape = RoundedCornerShape(40.dp),
                colors = if (darkClassic) {
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                } else {
                    ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFB8E0DE),
                        contentColor = ACCENT_DARK,
                    )
                },
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
            ) {
                Text(
                    stringResource(
                        if (autoSilentOnDismiss) R.string.alarm_just_dismiss_silence else R.string.alarm_just_dismiss,
                    ),
                )
            }
        }
    }
}

@Composable
private fun MenuPillButton(
    darkClassic: Boolean,
    text: String,
    iconRes: Int,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(40.dp),
        colors = if (darkClassic) {
            ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            )
        } else {
            ButtonDefaults.buttonColors(
                containerColor = ACCENT_DARK,
                contentColor = Color.White,
            )
        },
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
        modifier = Modifier.wrapContentWidth(),
    ) {
        Icon(painterResource(iconRes), contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text(text)
    }
}

@Preview(showBackground = true, heightDp = 700, widthDp = 360, name = "Initial pill")
@Composable
private fun AlarmFullscreenInitialPreview() {
    AlAzanTheme(ThemeColor.Light) {
        AlarmFullscreenScreen(
            uiState = AlarmFullscreenUiState(
                header = stringResource(R.string.alarm_azan_header),
                title = "Maghrib",
                timeLabel = "19:38",
                dismissAndSilentMinutes = 15,
                shortRemindMinutes = 15,
                longRemindMinutes = 30,
            ),
            onAction = {},
        )
    }
}

@Preview(showBackground = true, heightDp = 700, widthDp = 360, name = "Classic light")
@Composable
private fun AlarmFullscreenClassicLightPreview() {
    AlAzanTheme(ThemeColor.ClassicLight) {
        AlarmFullscreenScreen(
            uiState = AlarmFullscreenUiState(
                header = stringResource(R.string.alarm_azan_header),
                title = "Maghrib",
                timeLabel = "19:38",
                dismissAndSilentMinutes = 15,
                shortRemindMinutes = 15,
                longRemindMinutes = 30,
                themeColor = ThemeColor.ClassicLight,
            ),
            onAction = {},
        )
    }
}

@Preview(showBackground = true, heightDp = 700, widthDp = 360, name = "Classic dark")
@Composable
private fun AlarmFullscreenClassicDarkPreview() {
    AlAzanTheme(ThemeColor.ClassicDark) {
        AlarmFullscreenScreen(
            uiState = AlarmFullscreenUiState(
                header = stringResource(R.string.alarm_azan_header),
                title = "Maghrib",
                timeLabel = "19:38",
                dismissAndSilentMinutes = 15,
                shortRemindMinutes = 15,
                longRemindMinutes = 30,
                themeColor = ThemeColor.ClassicDark,
            ),
            onAction = {},
        )
    }
}

@Preview(showBackground = true, heightDp = 700, widthDp = 360, name = "Menu open")
@Composable
private fun AlarmFullscreenMenuPreview() {
    AlAzanTheme(ThemeColor.Light) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(BG_GRADIENT_START, BG_GRADIENT_END))),
        ) {
            Box(Modifier.align(Alignment.Center)) {
                AlarmActionMenu(
                    darkClassic = false,
                    dismissAndSilentMinutes = 15,
                    shortRemindMinutes = 15,
                    longRemindMinutes = 30,
                    autoSilentOnDismiss = false,
                    onDismissAndSilent = {},
                    onShortRemind = {},
                    onLongRemind = {},
                    onJustDismiss = {},
                )
            }
        }
    }
}

@Preview(showBackground = true, heightDp = 700, widthDp = 360, name = "Menu classic light")
@Composable
private fun AlarmMenuClassicLightPreview() {
    ClassicMenuPreview(ThemeColor.ClassicLight)
}

@Preview(showBackground = true, heightDp = 700, widthDp = 360, name = "Menu classic dark")
@Composable
private fun AlarmMenuClassicDarkPreview() {
    ClassicMenuPreview(ThemeColor.ClassicDark)
}

@Composable
private fun ClassicMenuPreview(themeColor: ThemeColor) {
    AlAzanTheme(themeColor) {
        Box(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
        ) {
            Box(Modifier.align(Alignment.Center)) {
                AlarmActionMenu(
                    darkClassic = themeColor == ThemeColor.ClassicDark,
                    dismissAndSilentMinutes = 15,
                    shortRemindMinutes = 15,
                    longRemindMinutes = 30,
                    autoSilentOnDismiss = false,
                    onDismissAndSilent = {},
                    onShortRemind = {},
                    onLongRemind = {},
                    onJustDismiss = {},
                )
            }
        }
    }
}
