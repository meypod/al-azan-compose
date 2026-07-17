package com.github.meypod.al_azan.main.settings.widget.custom

import android.widget.FrameLayout
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.github.meypod.al_azan.R
import com.github.meypod.al_azan.core.domain.model.adhan.Prayer
import com.github.meypod.al_azan.core.domain.model.adhan.SHARIA_TIMES_IN_ORDER
import com.github.meypod.al_azan.core.domain.model.adhan.i18n
import com.github.meypod.al_azan.core.domain.model.widget.CustomWidgetConfig
import com.github.meypod.al_azan.core.domain.model.widget.CustomWidgetData
import com.github.meypod.al_azan.core.domain.model.widget.DateCalendar
import com.github.meypod.al_azan.core.domain.model.widget.HeaderBlock
import com.github.meypod.al_azan.core.domain.model.widget.withPrayerPlaced
import com.github.meypod.al_azan.core.domain.model.widget.withPrayerRemoved
import com.github.meypod.al_azan.core.presentation.AlAzanThemePreview
import com.github.meypod.al_azan.core.presentation.components.ACard
import com.github.meypod.al_azan.core.presentation.components.BottomSelect
import com.github.meypod.al_azan.core.presentation.components.ColorPickerField
import com.github.meypod.al_azan.core.presentation.components.DragAndDropContainer
import com.github.meypod.al_azan.core.presentation.components.DragAndDropState
import com.github.meypod.al_azan.core.presentation.components.ScreenScaffold
import com.github.meypod.al_azan.core.presentation.components.SettingHeader
import com.github.meypod.al_azan.core.presentation.components.SettingSwitch
import com.github.meypod.al_azan.core.presentation.components.dragSource
import com.github.meypod.al_azan.core.presentation.components.dropTarget
import com.github.meypod.al_azan.core.presentation.components.rememberDragAndDropState
import com.github.meypod.al_azan.core.presentation.navigation.NavigationController
import com.github.meypod.al_azan.widget.CustomWidgetRenderer
import kotlin.math.roundToInt

@Composable
fun CustomWidgetBuilderScreen(
    uiState: CustomWidgetBuilderUiState,
    onAction: (CustomWidgetBuilderUiAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dnd = rememberDragAndDropState()
    var pinned by rememberSaveable { mutableStateOf(true) }
    val scrollState = rememberScrollState()
    val spacing = Arrangement.spacedBy(dimensionResource(R.dimen.element_padding))

    DragAndDropContainer(dnd, modifier.fillMaxWidth()) {
        ScreenScaffold(
            title = stringResource(R.string.widget_section_custom_appearance),
            onBackClick = { NavigationController.navigateBack() },
            scrollable = false,
        ) {
            if (pinned) {
                PreviewSection(uiState.previewData, pinned = true, onTogglePin = { pinned = it })
                Column(
                    Modifier
                        .weight(1f)
                        .verticalScroll(scrollState),
                    verticalArrangement = spacing,
                ) {
                    OptionCards(uiState, onAction, dnd)
                }
            } else {
                Column(
                    Modifier
                        .weight(1f)
                        .verticalScroll(scrollState),
                    verticalArrangement = spacing,
                ) {
                    PreviewSection(uiState.previewData, pinned = false, onTogglePin = { pinned = it })
                    OptionCards(uiState, onAction, dnd)
                }
            }
        }
    }
}

@Composable
private fun PreviewSection(
    data: CustomWidgetData?,
    pinned: Boolean,
    onTogglePin: (Boolean) -> Unit,
) {
    Column {
        FilterChip(
            selected = pinned,
            onClick = { onTogglePin(!pinned) },
            label = { Text(stringResource(R.string.custom_widget_pin_preview)) },
            leadingIcon = {
                AnimatedVisibility(
                    visible = pinned,
                    enter = fadeIn() + expandHorizontally(),
                    exit = fadeOut() + shrinkHorizontally(),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.outline_push_pin_24),
                        contentDescription = null,
                        modifier = Modifier.size(FilterChipDefaults.IconSize),
                    )
                }
            },
            modifier = Modifier.align(Alignment.End),
        )
        val previewHeight = if (data != null && data.pages.size > 1) 180.dp else 130.dp
        CustomWidgetPreview(
            data = data,
            modifier = Modifier
                .fillMaxWidth()
                .height(previewHeight)
                .clip(MaterialTheme.shapes.medium),
        )
    }
}

@Composable
private fun CustomWidgetPreview(
    data: CustomWidgetData?,
    modifier: Modifier = Modifier,
) {
    if (data == null || LocalInspectionMode.current) {
        Box(modifier.clip(MaterialTheme.shapes.medium), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.custom_widget_no_locations))
        }
        return
    }
    if (data.pages.size > 1) PagedPreview(data, modifier) else SingleWidgetView(data, modifier)
}

/**
 * Renders the REAL multi-location widget (fills height, has its own name/header/rows/‹›/dots). The
 * widget's arrow taps are broadcasts that can't fire without a live widget id, so transparent tap
 * zones over the arrows drive the preview instead; a full-size zone swallows body taps.
 */
@Composable
private fun PagedPreview(
    data: CustomWidgetData,
    modifier: Modifier = Modifier,
) {
    val pageCount = data.pages.size
    var index by remember(pageCount) { mutableStateOf(0) }
    Box(modifier) {
        WidgetView(data = data, pageIndex = index, modifier = Modifier.fillMaxSize())
        Box(
            Modifier
                .matchParentSize()
                .pointerInput(Unit) { detectTapGestures { } },
        )
        Box(
            Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth(0.35f)
                .height(44.dp)
                .pointerInput(pageCount) { detectTapGestures { index = (index - 1 + pageCount) % pageCount } },
        )
        Box(
            Modifier
                .align(Alignment.BottomEnd)
                .fillMaxWidth(0.35f)
                .height(44.dp)
                .pointerInput(pageCount) { detectTapGestures { index = (index + 1) % pageCount } },
        )
    }
}

/** Hosts the widget's real RemoteViews at [pageIndex] via apply() — byte-identical to the launcher. */
@Composable
private fun WidgetView(
    data: CustomWidgetData,
    pageIndex: Int,
    modifier: Modifier = Modifier,
) {
    AndroidView(
        modifier = modifier,
        factory = { ctx -> FrameLayout(ctx) },
        update = { frame ->
            frame.removeAllViews()
            frame.addView(CustomWidgetRenderer.build(frame.context, data, pageIndex).apply(frame.context, frame))
        },
    )
}

/** Hosts the real widget RemoteViews so the preview is byte-for-byte what the launcher shows. */
@Composable
private fun SingleWidgetView(
    data: CustomWidgetData,
    modifier: Modifier = Modifier,
) {
    AndroidView(
        modifier = modifier,
        factory = { ctx -> FrameLayout(ctx) },
        update = { frame ->
            frame.removeAllViews()
            val remoteViews = CustomWidgetRenderer.build(frame.context, data)
            frame.addView(remoteViews.apply(frame.context, frame))
        },
    )
}

@Composable
private fun ColumnScope.OptionCards(
    uiState: CustomWidgetBuilderUiState,
    onAction: (CustomWidgetBuilderUiAction) -> Unit,
    dnd: DragAndDropState,
) {
    val context = LocalContext.current
    val config = uiState.config
    val defaultBg = ContextCompat.getColor(context, R.color.custom_widget_bg)
    val defaultText = ContextCompat.getColor(context, R.color.custom_widget_text)
    val defaultHighlight = ContextCompat.getColor(context, R.color.custom_widget_highlight)
    // The countdown defaults to the table widget's countdown color (its normal label color), not the
    // highlight — so "match theme" mirrors the table in both light and dark.
    val defaultCountdown = ContextCompat.getColor(context, R.color.secondary_text_color)

    // Colors.
    ACard { cardPadding ->
        Column(
            Modifier.padding(cardPadding),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.element_padding)),
        ) {
            ColorPickerField(
                label = stringResource(R.string.custom_widget_background_color),
                colorArgb = config.bgColor,
                defaultArgb = defaultBg,
                onColorChanged = { onAction(CustomWidgetBuilderUiAction.OnBgColorChange(it)) },
            )
            ColorPickerField(
                label = stringResource(R.string.custom_widget_text_color),
                colorArgb = config.textColor,
                defaultArgb = defaultText,
                onColorChanged = { onAction(CustomWidgetBuilderUiAction.OnTextColorChange(it)) },
            )
            ColorPickerField(
                label = stringResource(R.string.custom_widget_highlight_color),
                colorArgb = config.highlightColor,
                defaultArgb = defaultHighlight,
                onColorChanged = { onAction(CustomWidgetBuilderUiAction.OnHighlightColorChange(it)) },
            )
        }
    }

    // Header slots: drop a date/location block into each; drag chips from the palette below.
    ACard { cardPadding ->
        Column(
            Modifier.padding(cardPadding),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.element_padding)),
        ) {
            SettingHeader(
                title = stringResource(R.string.custom_widget_header_slots_title),
                subtitle = stringResource(R.string.custom_widget_header_help),
            )
            val swapLabel = stringResource(R.string.custom_widget_swap_header)
            Row(
                horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.element_padding_compact)),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                HeaderSlotBox(
                    dnd = dnd,
                    slotIndex = 0,
                    block = config.topStart,
                    emptyLabel = stringResource(R.string.custom_widget_header_start),
                    onDrop = { onAction(headerDropAction(config, targetSlot = 0, item = it)) },
                    onClear = { onAction(CustomWidgetBuilderUiAction.OnTopStartChange(null)) },
                    modifier = Modifier.weight(1f),
                )
                IconButton(
                    onClick = { onAction(CustomWidgetBuilderUiAction.OnHeaderSlotsChange(config.topEnd, config.topStart)) },
                    enabled = config.topStart != null || config.topEnd != null,
                ) {
                    Icon(
                        painterResource(R.drawable.baseline_swap_horiz_24),
                        contentDescription = swapLabel,
                    )
                }
                HeaderSlotBox(
                    dnd = dnd,
                    slotIndex = 1,
                    block = config.topEnd,
                    emptyLabel = stringResource(R.string.custom_widget_header_end),
                    onDrop = { onAction(headerDropAction(config, targetSlot = 1, item = it)) },
                    onClear = { onAction(CustomWidgetBuilderUiAction.OnTopEndChange(null)) },
                    modifier = Modifier.weight(1f),
                )
            }
            // Hide blocks already placed in a header slot; animate add/remove.
            val availableHeaderBlocks = HEADER_BLOCK_OPTIONS.filterNot { it == config.topStart || it == config.topEnd }
            val addLabel = stringResource(R.string.custom_widget_add)
            // Tap fills the first empty slot (start, then end); accessible alternative to dragging.
            val fillHeaderSlot: (HeaderBlock) -> Unit = { block ->
                if (config.topStart == null) {
                    onAction(CustomWidgetBuilderUiAction.OnTopStartChange(block))
                } else {
                    onAction(CustomWidgetBuilderUiAction.OnTopEndChange(block))
                }
            }
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 40.dp)
                    // Dropping a header block here removes it from the slot it came from.
                    .dropTarget(dnd, "cw_header_palette") { payload ->
                        (payload as? CwDrag.HeaderItem)?.let {
                            when (it.fromSlot) {
                                0 -> onAction(CustomWidgetBuilderUiAction.OnTopStartChange(null))
                                1 -> onAction(CustomWidgetBuilderUiAction.OnTopEndChange(null))
                            }
                        }
                    },
                horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.element_padding_compact)),
            ) {
                items(availableHeaderBlocks, key = { headerBlockKey(it) }) { block ->
                    val label = headerLabel(block)
                    Box(rtlSafeAnimateItem()) {
                        DraggableChip(
                            dnd = dnd,
                            payload = CwDrag.HeaderItem(block),
                            dragLabel = label,
                            ghost = { ChipSurface { ChipText(label) } },
                            content = {
                                Box(
                                    Modifier
                                        .clip(MaterialTheme.shapes.small)
                                        .clickable(onClickLabel = addLabel) { fillHeaderSlot(block) },
                                ) {
                                    ChipSurface { ChipText(label) }
                                }
                            },
                        )
                    }
                }
            }
            FontSizeSlider(
                scale = config.headerFontScale,
                onChange = { onAction(CustomWidgetBuilderUiAction.OnHeaderFontScaleChange(it)) },
            )
        }
    }

    // Prayer times: choose 1–2 rows, then drag prayers into a specific row (position = order); tap ✕
    // to remove. Row count lives here too.
    ACard { cardPadding ->
        Column(
            Modifier.padding(cardPadding),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.element_padding)),
        ) {
            SettingHeader(
                title = stringResource(R.string.custom_widget_prayers),
                subtitle = stringResource(R.string.custom_widget_prayers_help),
            )
            BottomSelect(
                modifier = Modifier.fillMaxWidth(),
                options = (1..CustomWidgetConfig.MAX_PRAYER_ROWS).toList(),
                optionKey = { it.toString() },
                optionLabel = { it.toString() },
                selectedKey = config.rows.size.toString(),
                onSelect = { onAction(CustomWidgetBuilderUiAction.OnRowCountChange(it)) },
                label = { Text(stringResource(R.string.custom_widget_rows_label)) },
                placeholder = stringResource(R.string.custom_widget_rows_label),
                supportingText = { Text(stringResource(R.string.custom_widget_rows_help)) },
            )
            config.rows.forEachIndexed { rowIndex, _ ->
                PlacedRow(
                    dnd = dnd,
                    rows = config.rows,
                    rowIndex = rowIndex,
                    times = uiState.prayerTimes,
                    label = if (config.rows.size > 1) stringResource(R.string.custom_widget_row_n, rowIndex + 1) else null,
                    onChange = { onAction(CustomWidgetBuilderUiAction.OnRowsChange(it)) },
                )
            }
            val placed = config.rows.flatten().toSet()
            PrayerPaletteRow(
                dnd = dnd,
                // Only offer prayers not already placed in a row.
                prayers = SHARIA_TIMES_IN_ORDER.filterNot { it in placed },
                times = uiState.prayerTimes,
                // Tap-to-add drops into the last row; accessible alternative to long-press dragging.
                onAdd = { onAction(CustomWidgetBuilderUiAction.OnRowsChange(config.rows.withPrayerPlaced(it, config.rows.lastIndex))) },
                // Dragging a placed prayer back onto the palette removes it from its row.
                onRemove = { onAction(CustomWidgetBuilderUiAction.OnRowsChange(config.rows.withPrayerRemoved(it))) },
            )
            FontSizeSlider(
                scale = config.prayerFontScale,
                onChange = { onAction(CustomWidgetBuilderUiAction.OnPrayerFontScaleChange(it)) },
            )
        }
    }

    // Countdown: its own toggle, plus color + font size that slide in only while it's enabled.
    ACard { cardPadding ->
        Column(Modifier.padding(cardPadding)) {
            SettingSwitch(
                title = stringResource(R.string.show_countdown_timer),
                subtitle = null,
                checked = config.showCountdown,
                onCheckedChange = { onAction(CustomWidgetBuilderUiAction.OnCountdownToggle(it)) },
            )
            AnimatedVisibility(
                visible = config.showCountdown,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                Column(
                    Modifier.padding(top = dimensionResource(R.dimen.element_padding)),
                    verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.element_padding)),
                ) {
                    ColorPickerField(
                        label = stringResource(R.string.custom_widget_countdown_color),
                        colorArgb = config.countdownColor,
                        defaultArgb = defaultCountdown,
                        onColorChanged = { onAction(CustomWidgetBuilderUiAction.OnCountdownColorChange(it)) },
                    )
                    FontSizeSlider(
                        scale = config.countdownFontScale,
                        onChange = { onAction(CustomWidgetBuilderUiAction.OnCountdownFontScaleChange(it)) },
                    )
                }
            }
        }
    }

    // Locations.
    ACard { cardPadding ->
        Column(Modifier.padding(cardPadding)) {
            SettingHeader(
                title = stringResource(R.string.custom_widget_locations),
                subtitle = stringResource(R.string.custom_widget_locations_help),
            )
            if (uiState.locations.isEmpty()) {
                Text(
                    stringResource(R.string.custom_widget_no_locations),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = dimensionResource(R.dimen.element_padding)),
                )
            } else {
                // Multi-select: the sheet stays open, each row toggles its own location (green tick =
                // enabled), and search keeps it usable when the user has many favorites.
                val enabled = uiState.locations.filter { it.enabled }
                // The travel-mode entry stores bare GPS coords as its name; show a recognisable label.
                val travelLabel = stringResource(R.string.location_traveling_mode)
                val display = { loc: LocationToggle -> if (loc.isTravelMode) travelLabel else loc.name }
                val triggerLabel = when {
                    // Nothing chosen → the widget falls back to the app's active location.
                    enabled.isEmpty() -> stringResource(R.string.default_value)

                    enabled.size == 1 -> display(enabled.first())

                    else -> stringResource(R.string.custom_widget_multiple_locations)
                }
                BottomSelect(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = dimensionResource(R.dimen.element_padding)),
                    options = uiState.locations,
                    optionKey = { it.id },
                    optionLabel = { display(it) },
                    selectedKey = null,
                    searchable = true,
                    selectedLabelOverride = triggerLabel,
                    itemContent = { entry, _, _, _ ->
                        val location = entry.value.first
                        LocationCheckItem(
                            name = display(location),
                            selected = location.enabled,
                            onToggle = {
                                onAction(
                                    CustomWidgetBuilderUiAction.OnLocationToggle(location.id, !location.enabled),
                                )
                            },
                        )
                    },
                )
                // With more than one location on, list them all as removable chips in a wrapping FlowRow;
                // tapping one turns it off. The whole row slides in/out at the >1 threshold, and each chip
                // animates in/out individually as it's toggled. Spacing lives on each chip (not the row's
                // arrangement) so a hidden chip contributes no phantom gap.
                val chipGap = dimensionResource(R.dimen.element_padding_compact)
                AnimatedVisibility(
                    visible = enabled.size > 1,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically(),
                ) {
                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = chipGap),
                    ) {
                        uiState.locations.forEach { location ->
                            AnimatedVisibility(
                                visible = location.enabled,
                                enter = fadeIn() + expandHorizontally(),
                                exit = fadeOut() + shrinkHorizontally(),
                            ) {
                                Box(Modifier.padding(end = chipGap, bottom = chipGap)) {
                                    RemovableChip(
                                        onRemove = {
                                            onAction(CustomWidgetBuilderUiAction.OnLocationToggle(location.id, false))
                                        },
                                    ) {
                                        ChipText(display(location))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Content font-size control: scales the widget's text relative to its baseline, shown as a percentage. */
@Composable
private fun FontSizeSlider(
    scale: Float,
    onChange: (Float) -> Unit,
) {
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(stringResource(R.string.custom_widget_font_size))
            Text(
                "${(scale * 100).roundToInt()}%",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Slider(
            value = scale,
            onValueChange = onChange,
            valueRange = CustomWidgetConfig.FONT_SCALE_RANGE,
            // Snap to 10% increments across the 50–200% range.
            steps = 14,
        )
    }
}

/** One row inside the Locations multi-select sheet: label plus a green tick when enabled. Tapping
 *  toggles the location without dismissing the sheet, so the user can pick several in one pass. */
@Composable
private fun LocationCheckItem(
    name: String,
    selected: Boolean,
    onToggle: () -> Unit,
) {
    DropdownMenuItem(
        text = { Text(name, style = MaterialTheme.typography.bodyMedium) },
        onClick = onToggle,
        modifier = Modifier.semantics { toggleableState = ToggleableState(selected) },
        trailingIcon = {
            if (selected) {
                Icon(
                    painter = painterResource(R.drawable.baseline_check_24),
                    contentDescription = null,
                    tint = colorResource(R.color.checkmark_green),
                )
            }
        },
    )
}

/**
 * [Modifier.animateItem] with the fade-out dropped under RTL. Compose mispositions a *disappearing*
 * LazyRow item in RTL — it fades at the row's logical end (the far visual side) instead of in place —
 * so there a removed chip just vanishes; LTR keeps the fade. Appearance/placement are unaffected.
 */
@Composable
private fun LazyItemScope.rtlSafeAnimateItem(): Modifier {
    val ltr = LocalLayoutDirection.current == LayoutDirection.Ltr
    return Modifier.animateItem(fadeOutSpec = if (ltr) spring(stiffness = Spring.StiffnessMediumLow) else null)
}

/**
 * One widget row: the drag-reorder target. While a prayer chip is dragged over this row, the chips
 * reorder live to preview the result; dropping commits that order. Uses ReorderableLazyColumn's
 * stable hit-test — settled [LazyListState.layoutInfo] offsets + centre-in-slot containment — so the
 * row doesn't oscillate as chips shift under the finger (a naive "insert before the hovered chip"
 * loops: the hovered chip slides away, flipping the target every frame).
 */
@Composable
private fun PlacedRow(
    dnd: DragAndDropState,
    rows: List<List<Prayer>>,
    rowIndex: Int,
    times: Map<Prayer, String>,
    label: String?,
    onChange: (List<List<Prayer>>) -> Unit,
) {
    val committed = rows[rowIndex]
    val draggedPrayer = (dnd.payload as? CwDrag.PrayerItem)?.prayer
    val listState = rememberLazyListState()
    var rowBounds by remember { mutableStateOf(Rect.Zero) }
    var lazyBounds by remember { mutableStateOf(Rect.Zero) }
    // Live-preview order for THIS row during a drag; null = show the committed order.
    var preview by remember { mutableStateOf<List<Prayer>?>(null) }
    val density = LocalDensity.current
    // RTL mirrors the horizontal axis: the list's start edge is the right, and item offsets grow leftward.
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl

    // Per-frame while dragging: edge auto-scroll + the stable reorder hit-test. The dragged chip stays
    // in the list as a real slot; each frame we find the settled slot whose extent contains the finger
    // and move it there. Because its centre then sits mid-slot, a neighbour sliding under the finger
    // can't immediately re-trigger — this is what stops the oscillation.
    LaunchedEffect(dnd.isDragging, draggedPrayer, rowIndex) {
        if (!dnd.isDragging || draggedPrayer == null) {
            preview = null
            return@LaunchedEffect
        }
        val edge = with(density) { 40.dp.toPx() }
        val maxStep = with(density) { 14.dp.toPx() }
        while (dnd.isDragging) {
            withFrameNanos { }
            val f = dnd.pointerWindowPosition
            val overRow = f.y >= rowBounds.top && f.y <= rowBounds.bottom
            if (!overRow) {
                // Finger is over another row / the palette: show THIS row's committed content unchanged.
                // Crucially, do NOT drop the dragged chip from its source row — removing it disposes the
                // chip node that owns the long-press drag gesture, which cancels the whole drag. It stays
                // (faded) in place; the actual move is applied on drop into the target row.
                preview = null
                continue
            }
            // Edge auto-scroll toward whichever side the finger is nearest (reach off-screen slots).
            // scrollBy(+) advances toward the list end (higher indices); map each window edge to the
            // start/end edge it represents, which flips in RTL (start = right, end = left).
            val leftPen = ((rowBounds.left + edge - f.x) / edge).coerceIn(0f, 1f)
            val rightPen = ((f.x - (rowBounds.right - edge)) / edge).coerceIn(0f, 1f)
            val startPen = if (isRtl) rightPen else leftPen
            val endPen = if (isRtl) leftPen else rightPen
            val dir = when {
                startPen > 0f -> -startPen
                endPen > 0f -> endPen
                else -> 0f
            }
            if (dir != 0f) listState.scrollBy(dir * maxStep)

            // Working order always contains the dragged chip (append on first entry into this row).
            val current = preview?.takeIf { draggedPrayer in it }
                ?: if (draggedPrayer in committed) committed else committed + draggedPrayer
            val draggedIndex = current.indexOf(draggedPrayer)
            // Ghost is centred on the finger, so the dragged chip's centre ≈ finger x (viewport-local).
            // Measure from the list's START edge to match LazyListItemInfo.offset — mirrored in RTL.
            val centerX = if (isRtl) lazyBounds.right - f.x else f.x - lazyBounds.left
            val target = listState.layoutInfo.visibleItemsInfo.firstOrNull {
                it.index != draggedIndex && centerX >= it.offset && centerX <= it.offset + it.size
            }?.index
            preview = if (target != null && target != draggedIndex) {
                current.toMutableList().apply { add(target, removeAt(draggedIndex)) }
            } else {
                current
            }
        }
    }

    // Commit the current preview globally: withPrayerPlaced first removes the dragged chip from any other
    // row, so a cross-row move reproduces the previewed order exactly.
    fun commitPreview() {
        val p = preview ?: return
        val dragged = draggedPrayer ?: return
        onChange(rows.withPrayerPlaced(dragged, rowIndex, before = p.getOrNull(p.indexOf(dragged) + 1)))
    }

    val display = preview ?: committed
    Column(verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.element_padding_compact))) {
        if (label != null) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Box(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp)
                .clip(MaterialTheme.shapes.medium)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.medium)
                .onGloballyPositioned { rowBounds = it.boundsInWindow() }
                .dropTarget(dnd, "cw_row_$rowIndex") { payload ->
                    (payload as? CwDrag.PrayerItem)?.let {
                        if (preview != null) commitPreview() else onChange(rows.withPrayerPlaced(it.prayer, rowIndex))
                    }
                }
                .padding(dimensionResource(R.dimen.element_padding_compact)),
            contentAlignment = if (display.isEmpty()) Alignment.Center else Alignment.CenterStart,
        ) {
            if (display.isEmpty()) {
                Text(
                    stringResource(R.string.custom_widget_drop_prayers),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                LazyRow(
                    state = listState,
                    modifier = Modifier.onGloballyPositioned { lazyBounds = it.boundsInWindow() },
                    horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.element_padding_compact)),
                ) {
                    items(display, key = { it }) { prayer ->
                        val name = prayer.i18n()
                        val time = times[prayer] ?: PLACEHOLDER_TIME
                        Box(
                            rtlSafeAnimateItem()
                                // Fade the dragged chip in place; the floating ghost tracks the finger.
                                .graphicsLayer { alpha = if (prayer == draggedPrayer) 0.3f else 1f }
                                .dropTarget(dnd, prayer) { payload ->
                                    (payload as? CwDrag.PrayerItem)?.let {
                                        // Over this row the preview already positions the chip; commit it.
                                        // Otherwise (no active preview) insert before the hovered chip.
                                        if (preview != null) {
                                            commitPreview()
                                        } else {
                                            onChange(rows.withPrayerPlaced(it.prayer, rowIndex, prayer))
                                        }
                                    }
                                },
                        ) {
                            DraggableChip(
                                dnd = dnd,
                                payload = CwDrag.PrayerItem(prayer),
                                dragLabel = "$name $time",
                                // Ghost is the plain visual; the placed chip taps anywhere to remove and
                                // long-presses to drag.
                                ghost = { ChipWithRemove { ChipColumn(name, time) } },
                                content = {
                                    RemovableChip(onRemove = { onChange(rows.withPrayerRemoved(prayer)) }) {
                                        ChipColumn(name, time)
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PrayerPaletteRow(
    dnd: DragAndDropState,
    prayers: List<Prayer>,
    times: Map<Prayer, String>,
    onAdd: (Prayer) -> Unit,
    onRemove: (Prayer) -> Unit,
) {
    val addLabel = stringResource(R.string.custom_widget_add)
    Box(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp),
    ) {
        LazyRow(
            modifier = Modifier
                .fillMaxSize()
                // Dropping a placed prayer here removes it from its row (returns it to the palette).
                .dropTarget(dnd, "cw_prayer_palette") { payload ->
                    (payload as? CwDrag.PrayerItem)?.let { onRemove(it.prayer) }
                },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.element_padding_compact)),
        ) {
            items(prayers, key = { it }) { prayer ->
                val name = prayer.i18n()
                val time = times[prayer] ?: PLACEHOLDER_TIME
                Box(rtlSafeAnimateItem()) {
                    DraggableChip(
                        dnd = dnd,
                        payload = CwDrag.PrayerItem(prayer),
                        dragLabel = "$name $time",
                        ghost = { ChipSurface { ChipColumn(name, time) } },
                    ) {
                        Box(
                            Modifier
                                .clip(MaterialTheme.shapes.medium)
                                .clickable(onClickLabel = addLabel) { onAdd(prayer) },
                        ) {
                            ChipSurface { ChipColumn(name, time) }
                        }
                    }
                }
            }
        }
        // Every prayer is placed → the palette is empty; fade in a hint box (like the rows'). The drop
        // target still lives on the LazyRow beneath, so a chip can be dragged back onto this area.
        AnimatedVisibility(
            visible = prayers.isEmpty(),
            modifier = Modifier.matchParentSize(),
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .clip(MaterialTheme.shapes.medium)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.medium),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    stringResource(R.string.custom_widget_all_prayers_used),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun HeaderSlotBox(
    dnd: DragAndDropState,
    slotIndex: Int,
    block: HeaderBlock?,
    emptyLabel: String,
    onDrop: (CwDrag.HeaderItem) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val clearLabel = stringResource(R.string.custom_widget_remove)
    val label = block?.let { headerLabel(it) }
    Box(
        modifier
            .heightIn(min = 56.dp)
            .clip(MaterialTheme.shapes.medium)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.medium)
            .dropTarget(dnd, "cw_slot_$slotIndex") { payload ->
                (payload as? CwDrag.HeaderItem)?.let { onDrop(it) }
            }
            // A filled slot can be dragged (to the other slot to swap) and tapped to clear.
            .then(
                if (block != null && label != null) {
                    Modifier
                        .dragSource(
                            dnd,
                            CwDrag.HeaderItem(block, fromSlot = slotIndex),
                            ghost = { ChipSurface { ChipText(label) } },
                        )
                        .clickable(onClickLabel = clearLabel, onClick = onClear)
                } else {
                    Modifier
                },
            )
            .padding(dimensionResource(R.dimen.element_padding_compact)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label ?: emptyLabel,
            color = if (block == null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
    }
}

/** Resolves a header block dropped onto [targetSlot] (0=start, 1=end) — swaps when dragged from the other slot. */
private fun headerDropAction(
    config: CustomWidgetConfig,
    targetSlot: Int,
    item: CwDrag.HeaderItem,
): CustomWidgetBuilderUiAction =
    if (targetSlot == 0) {
        val newEnd = if (item.fromSlot == 1) config.topStart else config.topEnd
        CustomWidgetBuilderUiAction.OnHeaderSlotsChange(topStart = item.block, topEnd = newEnd)
    } else {
        val newStart = if (item.fromSlot == 0) config.topEnd else config.topStart
        CustomWidgetBuilderUiAction.OnHeaderSlotsChange(topStart = newStart, topEnd = item.block)
    }

/** A long-press-draggable chip. [ghost] is the floating image; [content] is what stays in place. */
@Composable
private fun DraggableChip(
    dnd: DragAndDropState,
    payload: CwDrag,
    dragLabel: String,
    ghost: @Composable () -> Unit,
    content: @Composable () -> Unit,
) {
    Box(
        Modifier
            .dragSource(dnd, payload, ghost = ghost)
            .semantics(mergeDescendants = true) { contentDescription = dragLabel },
    ) { content() }
}

@Composable
private fun ChipSurface(content: @Composable () -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
    ) {
        Box(Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) { content() }
    }
}

@Composable
private fun ChipColumn(
    name: String,
    time: String,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(name, style = MaterialTheme.typography.bodyMedium)
        Text(time, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ChipText(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium)
}

/** The ✕ shown at the end of a removable chip — a visual cue only; the whole chip is the tap target. */
@Composable
private fun ChipRemoveIcon() {
    Text(
        text = "✕",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 6.dp),
    )
}

/** A chip's visual: [content] plus the ✕ cue. Used for drag ghosts and inside [RemovableChip]. */
@Composable
private fun ChipWithRemove(content: @Composable () -> Unit) {
    ChipSurface {
        Row(verticalAlignment = Alignment.CenterVertically) {
            content()
            ChipRemoveIcon()
        }
    }
}

/** A chip that removes itself when tapped anywhere (not just the ✕). */
@Composable
private fun RemovableChip(
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val removeLabel = stringResource(R.string.custom_widget_remove)
    Box(
        modifier
            .clip(MaterialTheme.shapes.small)
            .clickable(onClickLabel = removeLabel, onClick = onRemove),
    ) {
        ChipWithRemove(content)
    }
}

/** Payloads dragged inside the builder. */
private sealed interface CwDrag {
    data class PrayerItem(
        val prayer: Prayer,
    ) : CwDrag

    /** [fromSlot] is null when dragged from the palette, 0/1 when dragged out of a header slot. */
    data class HeaderItem(
        val block: HeaderBlock,
        val fromSlot: Int? = null,
    ) : CwDrag
}

private const val PLACEHOLDER_TIME = "--:--"

private val HEADER_BLOCK_OPTIONS: List<HeaderBlock> = buildList {
    add(HeaderBlock.LocationName)
    // Every calendar the app supports, each offered plain and with the weekday prefixed.
    for (calendar in DateCalendar.entries) {
        add(HeaderBlock.Date(calendar, withDayName = false))
        add(HeaderBlock.Date(calendar, withDayName = true))
    }
}

/** Stable, non-composable identity for a palette block (used as the LazyRow item key). */
private fun headerBlockKey(block: HeaderBlock): String =
    when (block) {
        is HeaderBlock.LocationName -> "location"
        is HeaderBlock.Date -> "date:${block.calendar.name}:${block.withDayName}"
    }

/** App-wide display name for each calendar the header can show; reuses the shared calendar name keys. */
@StringRes
private fun calendarNameRes(calendar: DateCalendar): Int =
    when (calendar) {
        DateCalendar.Hijri -> R.string.calendar_lunar
        DateCalendar.Gregorian -> R.string.calendar_gregorian
        DateCalendar.Persian -> R.string.calendar_persian
        DateCalendar.Ethiopic -> R.string.calendar_ethiopic
        DateCalendar.Buddhist -> R.string.calendar_buddhist
    }

@Composable
private fun headerLabel(block: HeaderBlock): String =
    when (block) {
        is HeaderBlock.LocationName -> stringResource(R.string.custom_widget_header_location)

        // "<calendar> date [+ weekday]" — reuses each calendar's already-translated name.
        is HeaderBlock.Date -> {
            val name = stringResource(calendarNameRes(block.calendar))
            if (block.withDayName) {
                stringResource(R.string.custom_widget_header_date_day, name)
            } else {
                stringResource(R.string.custom_widget_header_date, name)
            }
        }
    }

@Preview
@Composable
private fun CustomWidgetBuilderScreenPreview() {
    AlAzanThemePreview {
        CustomWidgetBuilderScreen(uiState = CustomWidgetBuilderUiState(), onAction = {})
    }
}
