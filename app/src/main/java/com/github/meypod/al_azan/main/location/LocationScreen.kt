package com.github.meypod.al_azan.main.location

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.github.meypod.al_azan.R
import com.github.meypod.al_azan.core.domain.model.calculation.CalculationLocationDetail
import com.github.meypod.al_azan.core.domain.model.favorite_location.FavoriteLocation
import com.github.meypod.al_azan.core.domain.model.geo.CityGeoInfo
import com.github.meypod.al_azan.core.domain.model.geo.CountryGeoInfo
import com.github.meypod.al_azan.core.domain.model.settings.ThemeColor
import com.github.meypod.al_azan.core.presentation.AlAzanTheme
import com.github.meypod.al_azan.core.presentation.components.ACard
import com.github.meypod.al_azan.core.presentation.components.InformationCard
import com.github.meypod.al_azan.core.presentation.components.PrimaryButton
import com.github.meypod.al_azan.core.presentation.util.bottomBorder
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun LocationScreen(
    uiState: LocationUiState,
    onAction: (LocationUiAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier.padding(dimensionResource(R.dimen.page_padding)),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        InformationCard(Modifier.fillMaxWidth()) {
            Column {
                Text(stringResource(R.string.location_description_l1))
                Text(
                    stringResource(
                        R.string.location_description_l2,
                        stringResource(R.string.add_new_location_button),
                    ),
                )
                Text(stringResource(R.string.location_description_l3))
            }
        }

        Spacer(Modifier.height(dimensionResource(R.dimen.element_padding)))

        ACard(Modifier.fillMaxWidth()) { paddingValues ->
            Column(
                Modifier.padding(paddingValues),
                verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.element_padding)),
            ) {
                Text(
                    text = stringResource(R.string.locations_list_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                if (uiState.locations.isEmpty()) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.icon_padding)),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            stringResource(R.string.locations_list_empty_state),
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                        )
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        PrimaryButton(
                            onClick = { onAction(LocationUiAction.OnNewLocationClick) },
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.add),
                                contentDescription = null,
                            )
                            Spacer(Modifier.width(dimensionResource(R.dimen.icon_padding)))
                            Text(stringResource(R.string.add_new_location_button))
                        }
                    }
                } else {
                    LocationList(
                        list = uiState.locations,
                        onAction = onAction,
                    )
                }
            }
        }
    }
}

@Composable
private fun LocationListItem(
    item: FavoriteLocation,
    selected: Boolean = false,
    onAction: (LocationUiAction) -> Unit,
    menuExpanded: Boolean = false,
    modifier: Modifier = Modifier,
    dragHandleModifier: Modifier = Modifier,
    dragging: Boolean = false,
) {
    var expanded by remember(item.id) { mutableStateOf(menuExpanded) }

    ListItem(
        modifier = modifier
            .zIndex(if (dragging) 1f else 0f)
            .graphicsLayer {
                alpha = if (dragging) 0f else 1f
            }
            .bottomBorder(MaterialTheme.colorScheme.outlineVariant, 2.dp),
        headlineContent = {
            val location = item.locationDetail
            val city = location.city?.selectedName ?: location.city?.name
            val country = location.country?.selectedName ?: location.country?.name
            val label = location.label

            Text(
                text = when {
                    !label.isNullOrBlank() -> label
                    !city.isNullOrBlank() && !country.isNullOrBlank() -> "$city, $country"
                    else -> item.id
                },
            )
        },
        supportingContent = {
            Text(item.locationDetail.toDisplayString())
        },
        leadingContent = {
            Icon(
                painter = painterResource(R.drawable.grip),
                contentDescription = stringResource(R.string.drag_handle_description),
                modifier = dragHandleModifier,
            )
        },
        trailingContent = {
            Row(horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.element_padding))) {
                if (selected) {
                    Icon(painterResource(R.drawable.baseline_check_24), contentDescription = stringResource(R.string.selected))
                }

                Column(horizontalAlignment = Alignment.End) {
                    IconButton(onClick = { expanded = true }) {
                        Icon(
                            painter = painterResource(R.drawable.menu_more_h),
                            contentDescription = stringResource(R.string.see_options),
                        )
                    }

                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.set_as_default)) },
                            trailingIcon = {
                                Icon(
                                    painter = painterResource(R.drawable.baseline_check_24),
                                    contentDescription = null,
                                )
                            },
                            onClick = {
                                expanded = false
                                onAction(LocationUiAction.OnSetAsDefaultClick(item.id))
                            },
                        )

                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.delete_location)) },
                            trailingIcon = {
                                Icon(
                                    painter = painterResource(R.drawable.delete),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                )
                            },
                            onClick = {
                                expanded = false
                                onAction(LocationUiAction.OnDeleteLocationClick(item.id))
                            },
                        )
                    }
                }
            }
        },
        colors = ListItemDefaults.colors().copy(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.surfaceContainerHighest
            } else {
                MaterialTheme.colorScheme.surfaceContainer
            },
        ),
    )
}

@Composable
private fun LocationList(
    list: List<FavoriteLocation>,
    selectedId: String? = null,
    onAction: (LocationUiAction) -> Unit,
) {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val density = androidx.compose.ui.platform.LocalDensity.current

    val reorderState = rememberLocationReorderState(
        listState = listState,
        onMove = { fromIndex, toIndex ->
            onAction(LocationUiAction.OnMoveLocation(fromIndex = fromIndex, toIndex = toIndex))
        },
        coroutineScope = coroutineScope,
    )

    Box(
        modifier = Modifier.onGloballyPositioned { coords ->
            reorderState.updateContainerCoords(coords)
        },
    ) {
        LazyColumn(
            modifier = Modifier.shadow(2.dp),
            state = listState,
        ) {
            itemsIndexed(
                items = list,
                key = { _, item -> item.id },
            ) { index, item ->
                SideEffect {
                    reorderState.updateItemIndex(item.id, index)
                }
                val isOverlayActiveForItem = reorderState.isOverlayActiveFor(item.id)
                LocationListItem(
                    item = item,
                    selected = item.id == selectedId,
                    onAction = onAction,
                    dragging = isOverlayActiveForItem,
                    modifier = Modifier.onGloballyPositioned { coords ->
                        reorderState.updateItemCoords(item.id, coords)
                    },
                    dragHandleModifier = Modifier
                        .onGloballyPositioned { coords ->
                            reorderState.updateHandleCoords(item.id, coords)
                        }
                        .pointerInput(item.id) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = { localStart ->
                                    reorderState.startDrag(itemId = item.id, index = index, localPointerStart = localStart)
                                },
                                onDragCancel = { reorderState.endDrag() },
                                onDragEnd = { reorderState.endDrag() },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    reorderState.dragBy(
                                        itemId = item.id,
                                        deltaX = dragAmount.x,
                                        deltaY = dragAmount.y,
                                        localPointerPosition = change.position,
                                    )
                                },
                            )
                        },
                )
            }
        }

        val draggingId = reorderState.draggingItemId
        val draggedItem = if (draggingId == null) null else list.firstOrNull { it.id == draggingId }
        val draggedItemSizePx = if (draggingId == null) null else reorderState.itemSizePx(draggingId)
        val showOverlay = draggingId != null && draggedItem != null && reorderState.popupVisible && draggedItemSizePx != null
        if (showOverlay) {
            Popup(
                alignment = Alignment.TopStart,
                offset = reorderState.draggingPopupOffset,
                properties = PopupProperties(
                    focusable = false,
                    dismissOnBackPress = false,
                    dismissOnClickOutside = false,
                    clippingEnabled = false,
                ),
            ) {
                SideEffect {
                    reorderState.markPopupComposed()
                }
                val scaleAnim = remember(draggingId) { Animatable(1f) }
                LaunchedEffect(draggingId) {
                    scaleAnim.snapTo(1f)
                    scaleAnim.animateTo(1.03f, animationSpec = tween(durationMillis = 100))
                }

                val draggedWidthDp = with(density) { draggedItemSizePx.width.toDp() }
                val draggedHeightDp = with(density) { draggedItemSizePx.height.toDp() }
                LocationListItem(
                    item = draggedItem,
                    selected = draggedItem.id == selectedId,
                    onAction = {},
                    modifier = Modifier.graphicsLayer {
                        alpha = 0.85f
                        scaleX = scaleAnim.value
                        scaleY = scaleAnim.value
                    }
                        .width(draggedWidthDp)
                        .height(draggedHeightDp),
                )
            }
        }
    }
}

private class LocationReorderState(
    private val listState: LazyListState,
    private val coroutineScope: kotlinx.coroutines.CoroutineScope,
) {
    private var onMove: (fromIndex: Int, toIndex: Int) -> Unit = { _, _ -> }

    private val itemCoordsById = linkedMapOf<String, androidx.compose.ui.layout.LayoutCoordinates>()
    private val handleCoordsById = linkedMapOf<String, androidx.compose.ui.layout.LayoutCoordinates>()
    private val itemSizeById = linkedMapOf<String, IntSize>()
    private val indexById = linkedMapOf<String, Int>()

    private var containerWindowOffset by mutableStateOf(androidx.compose.ui.geometry.Offset.Zero)

    var draggingIndex by mutableIntStateOf(-1)
        private set

    var draggingItemId by mutableStateOf<String?>(null)
        private set

    var draggingPopupOffset by mutableStateOf(IntOffset.Zero)
        private set

    private var draggingGrabOffsetInItem by mutableStateOf(androidx.compose.ui.geometry.Offset.Zero)

    var popupVisible by mutableStateOf(false)
        private set

    private var placeholderHidden by mutableStateOf(false)

    private var popupComposed by mutableStateOf(false)

    var draggingOffsetY by mutableFloatStateOf(0f)
        private set

    private var scrollJob: Job? = null
    private var hidePlaceholderJob: Job? = null

    fun updateOnMove(onMove: (fromIndex: Int, toIndex: Int) -> Unit) {
        this.onMove = onMove
    }

    fun updateContainerCoords(coords: androidx.compose.ui.layout.LayoutCoordinates) {
        containerWindowOffset = coords.localToWindow(androidx.compose.ui.geometry.Offset.Zero)
    }

    fun updateItemCoords(itemId: String, coords: androidx.compose.ui.layout.LayoutCoordinates) {
        itemCoordsById[itemId] = coords
        itemSizeById[itemId] = coords.size
    }

    fun itemSizePx(itemId: String): IntSize? = itemSizeById[itemId]

    fun updateItemIndex(itemId: String, index: Int) {
        indexById[itemId] = index
    }

    fun isOverlayActiveFor(itemId: String): Boolean {
        return placeholderHidden && draggingItemId == itemId
    }

    private fun scheduleHidePlaceholder() {
        if (!popupVisible) return
        if (!popupComposed) return
        if (placeholderHidden) return
        hidePlaceholderJob?.cancel()
        hidePlaceholderJob = coroutineScope.launch {
            withFrameNanos { }
            placeholderHidden = true
        }
    }

    fun markPopupComposed() {
        if (!popupComposed) {
            popupComposed = true
            scheduleHidePlaceholder()
        }
    }

    fun updateHandleCoords(itemId: String, coords: androidx.compose.ui.layout.LayoutCoordinates) {
        handleCoordsById[itemId] = coords
    }

    fun startDrag(itemId: String, index: Int, localPointerStart: androidx.compose.ui.geometry.Offset) {
        draggingItemId = itemId
        draggingIndex = index
        draggingOffsetY = 0f
        popupVisible = false
        placeholderHidden = false
        popupComposed = false
        hidePlaceholderJob?.cancel()
        hidePlaceholderJob = null

        val itemCoords = itemCoordsById[itemId]
        val handleCoords = handleCoordsById[itemId]
        val itemWindow = itemCoords?.localToWindow(androidx.compose.ui.geometry.Offset.Zero)
        val handleWindow = handleCoords?.localToWindow(localPointerStart)
        val itemSize = itemSizeById[itemId]

        if (itemWindow != null && handleWindow != null) {
            draggingGrabOffsetInItem = handleWindow - itemWindow
            val topLeft = handleWindow - draggingGrabOffsetInItem
            val localTopLeft = topLeft - containerWindowOffset
            draggingPopupOffset = IntOffset(localTopLeft.x.roundToInt(), localTopLeft.y.roundToInt())
            popupVisible = itemSize != null
        } else if (itemWindow != null) {
            draggingGrabOffsetInItem = androidx.compose.ui.geometry.Offset.Zero
            val localTopLeft = itemWindow - containerWindowOffset
            draggingPopupOffset = IntOffset(localTopLeft.x.roundToInt(), localTopLeft.y.roundToInt())
            popupVisible = itemSize != null
        } else {
            draggingGrabOffsetInItem = androidx.compose.ui.geometry.Offset.Zero
            draggingPopupOffset = if (handleWindow != null) {
                val localTopLeft = handleWindow - containerWindowOffset
                IntOffset(localTopLeft.x.roundToInt(), localTopLeft.y.roundToInt())
            } else {
                IntOffset.Zero
            }
        }

        // Placeholder hiding is triggered only after the popup has actually been composed.
    }

    fun dragBy(itemId: String, deltaX: Float, deltaY: Float, localPointerPosition: androidx.compose.ui.geometry.Offset) {
        val currentIndex = indexById[itemId] ?: draggingIndex
        if (currentIndex < 0) return

        val draggedSize = itemSizeById[itemId] ?: return

        val handleCoords = handleCoordsById[itemId]
        val pointerWindow = handleCoords?.localToWindow(localPointerPosition)
        if (pointerWindow != null) {
            val topLeft = pointerWindow - draggingGrabOffsetInItem
            val localTopLeft = topLeft - containerWindowOffset
            draggingPopupOffset = IntOffset(localTopLeft.x.roundToInt(), localTopLeft.y.roundToInt())
            popupVisible = true
            scheduleHidePlaceholder()
        } else {
            // Best-effort fallback
            draggingPopupOffset = draggingPopupOffset + IntOffset(deltaX.toInt(), deltaY.toInt())
        }

        val pointerLocalY = if (pointerWindow != null) {
            pointerWindow.y - containerWindowOffset.y
        } else {
            0f
        }

        val currentItemStart = pointerLocalY - draggingGrabOffsetInItem.y
        val currentItemEnd = currentItemStart + draggedSize.height
        val currentItemCenter = currentItemStart + draggedSize.height / 2f

        val target = listState.layoutInfo.visibleItemsInfo.firstOrNull { itemInfo ->
            itemInfo.index != currentIndex &&
                currentItemCenter >= itemInfo.offset &&
                currentItemCenter <= (itemInfo.offset + itemInfo.size)
        }

        if (target != null) {
            val targetIndex = target.index
            onMove(currentIndex, targetIndex)
            draggingIndex = targetIndex
        }

        maybeAutoScroll(currentItemStart, currentItemEnd)
    }

    fun endDrag() {
        draggingIndex = -1
        draggingItemId = null
        draggingOffsetY = 0f
        draggingPopupOffset = IntOffset.Zero
        draggingGrabOffsetInItem = androidx.compose.ui.geometry.Offset.Zero
        popupVisible = false
        placeholderHidden = false
        hidePlaceholderJob?.cancel()
        hidePlaceholderJob = null
        scrollJob?.cancel()
        scrollJob = null
    }

    private fun maybeAutoScroll(currentItemStart: Float, currentItemEnd: Float) {
        val viewportStart = listState.layoutInfo.viewportStartOffset
        val viewportEnd = listState.layoutInfo.viewportEndOffset
        val edgePx = 80f
        val scrollDelta = when {
            currentItemStart < viewportStart + edgePx -> -18f
            currentItemEnd > viewportEnd - edgePx -> 18f
            else -> 0f
        }

        if (scrollDelta == 0f) {
            scrollJob?.cancel()
            scrollJob = null
            return
        }

        if (scrollJob?.isActive == true) return
        scrollJob = coroutineScope.launch {
            listState.scrollBy(scrollDelta)
        }
    }
}

@Composable
private fun rememberLocationReorderState(
    listState: LazyListState,
    onMove: (fromIndex: Int, toIndex: Int) -> Unit,
    coroutineScope: kotlinx.coroutines.CoroutineScope,
): LocationReorderState {
    val state = remember(listState, coroutineScope) {
        LocationReorderState(
            listState = listState,
            coroutineScope = coroutineScope,
        )
    }
    state.updateOnMove(onMove)
    return state
}

private val demoLocations = listOf(
    FavoriteLocation(
        "canada",
        CalculationLocationDetail(56.1304, 106.3468, null, null, "Canada"),
    ),
    FavoriteLocation(
        "baqdad",
        CalculationLocationDetail(
            33.312805,
            44.361488,
            CityGeoInfo("Baqdad", "-", 1.0, 1.0, "IQ", "Baqdad"),
            CountryGeoInfo("IQ", "", "Iraq", "Iraq"),
            null,
        ),
    ),
)

@Preview(showBackground = true, backgroundColor = 0xFF00585A)
@Composable
private fun LocationListItemPreview() {
    AlAzanTheme {
        Column(Modifier.padding(vertical = 30.dp, horizontal = 10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            LocationListItem(demoLocations[0], onAction = {}, menuExpanded = true)
            LocationListItem(demoLocations[1], onAction = {})
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF00585A)
@Composable
private fun LocationListItemDarkPreview() {
    AlAzanTheme(ThemeColor.Dark) {
        Column(Modifier.padding(vertical = 30.dp, horizontal = 10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            LocationListItem(demoLocations[0], onAction = {}, menuExpanded = true)
            LocationListItem(demoLocations[1], onAction = {})
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun LocationListPreview() {
    AlAzanTheme {
        val locations = remember {
            mutableStateListOf<FavoriteLocation>().apply { addAll(demoLocations) }
        }

        Column(Modifier.padding(15.dp)) {
            LocationList(
                list = locations,
                selectedId = "canada",
                onAction = { action ->
                    when (action) {
                        is LocationUiAction.OnMoveLocation -> {
                            val from = action.fromIndex
                            val to = action.toIndex
                            if (from in locations.indices && to in locations.indices && from != to) {
                                val item = locations.removeAt(from)
                                locations.add(to, item)
                            }
                        }

                        else -> Unit
                    }
                },
            )
        }
    }
}

@Preview(
    showBackground = true,
    backgroundColor = 0xFF00585A,
)
@Preview(
    showBackground = true,
    backgroundColor = 0xFF00585A,
    device = Devices.TABLET,
)
@Composable
private fun LocationScreenPreview() {
    AlAzanTheme {
        LocationScreen(
            uiState = LocationUiState(),
            onAction = {},
        )
    }
}

@Preview(
    showBackground = true,
    backgroundColor = 0xFF00585A,
)
@Composable
private fun LocationScreenWithLocationsPreview() {
    AlAzanTheme {
        val locations = remember {
            mutableStateListOf<FavoriteLocation>().apply { addAll(demoLocations) }
        }
        LocationScreen(
            uiState = LocationUiState(locations.toList()),
            onAction = { action ->
                when (action) {
                    is LocationUiAction.OnMoveLocation -> {
                        val from = action.fromIndex
                        val to = action.toIndex
                        if (from in locations.indices && to in locations.indices && from != to) {
                            val item = locations.removeAt(from)
                            locations.add(to, item)
                        }
                    }

                    else -> Unit
                }
            },
        )
    }
}
