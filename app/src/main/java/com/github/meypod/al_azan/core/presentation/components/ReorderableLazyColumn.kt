package com.github.meypod.al_azan.core.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.github.meypod.al_azan.core.presentation.util.drawVerticalScrollbar
import com.github.meypod.al_azan.core.presentation.util.fadeScrollEdges
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * A reorderable [LazyColumn] where dragging starts only from a provided drag-handle modifier.
 *
 * The dragged item is rendered in a screen-level [Popup] overlay, so it can move freely
 * (not clipped to the list bounds). While dragging, the original list item can be rendered
 * as a placeholder (typically by hiding it).
 */
@Composable
fun <T> ReorderableLazyColumn(
    items: List<T>,
    key: (T) -> Any,
    onMove: (fromIndex: Int, toIndex: Int) -> Unit,
    modifier: Modifier = Modifier,
    listModifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
    overlayAlpha: Float = 0.85f,
    overlayScaleTarget: Float = 1.03f,
    overlayScaleAnimMillis: Int = 100,
    releaseAnimMillis: Int = 100,
    itemPlacementAnimMillis: Int = 180,
    itemContent: @Composable (
        item: T,
        isPlaceholder: Boolean,
        itemModifier: Modifier,
        dragHandleModifier: Modifier,
    ) -> Unit,
    overlayContent: (@Composable (item: T, modifier: Modifier) -> Unit)? = null,
) {
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current

    val reorderState = rememberReorderState(
        listState = listState,
        onMove = onMove,
        coroutineScope = coroutineScope,
    )

    Box(
        modifier = modifier.onGloballyPositioned { coords ->
            reorderState.updateContainerCoords(coords)
        },
    ) {
        LazyColumn(
            modifier = listModifier
                .fadeScrollEdges(listState, Orientation.Vertical)
                .drawVerticalScrollbar(listState),
            state = listState,
        ) {
            itemsIndexed(
                items = items,
                key = { _, item -> key(item) },
            ) { index, item ->
                val itemKey = key(item)

                SideEffect {
                    reorderState.updateItemIndex(itemKey, index)
                }

                val isPlaceholder = reorderState.isPlaceholderFor(itemKey)

                val itemModifier = Modifier
                    .animateItem(placementSpec = tween<IntOffset>(durationMillis = itemPlacementAnimMillis))
                    .onGloballyPositioned { coords ->
                        reorderState.updateItemCoords(itemKey, coords)
                    }

                val dragHandleModifier = Modifier
                    .onGloballyPositioned { coords ->
                        reorderState.updateHandleCoords(itemKey, coords)
                    }
                    .pointerInput(itemKey) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = { localStart ->
                                reorderState.startDrag(
                                    itemKey = itemKey,
                                    index = index,
                                    localPointerStart = localStart,
                                )
                            },
                            onDragCancel = { reorderState.releaseDrag(releaseAnimMillis) },
                            onDragEnd = { reorderState.releaseDrag(releaseAnimMillis) },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                reorderState.dragBy(
                                    itemKey = itemKey,
                                    deltaX = dragAmount.x,
                                    deltaY = dragAmount.y,
                                    localPointerPosition = change.position,
                                )
                            },
                        )
                    }

                itemContent(item, isPlaceholder, itemModifier, dragHandleModifier)
            }
        }

        val draggingKey = reorderState.draggingItemKey
        val draggedItem = if (draggingKey == null) null else items.firstOrNull { key(it) == draggingKey }
        val draggedItemSizePx = if (draggingKey == null) null else reorderState.itemSizePx(draggingKey)
        val showOverlay = draggingKey != null && draggedItem != null && reorderState.popupVisible && draggedItemSizePx != null

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

                val scaleAnim = remember(draggingKey) { Animatable(1f) }
                LaunchedEffect(draggingKey) {
                    scaleAnim.snapTo(1f)
                    scaleAnim.animateTo(overlayScaleTarget, animationSpec = tween(durationMillis = overlayScaleAnimMillis))
                }

                LaunchedEffect(reorderState.releaseToken) {
                    if (reorderState.isReleasing) {
                        scaleAnim.animateTo(1f, animationSpec = tween(durationMillis = releaseAnimMillis))
                    }
                }

                val draggedWidthDp = with(density) { draggedItemSizePx.width.toDp() }
                val draggedHeightDp = with(density) { draggedItemSizePx.height.toDp() }

                val finalModifier = Modifier
                    .graphicsLayer {
                        alpha = overlayAlpha
                        scaleX = scaleAnim.value
                        scaleY = scaleAnim.value
                    }
                    .width(draggedWidthDp)
                    .height(draggedHeightDp)

                val overlay = overlayContent
                if (overlay != null) {
                    overlay(draggedItem, finalModifier)
                } else {
                    itemContent(draggedItem, false, finalModifier, Modifier)
                }
            }
        }
    }
}

@Composable
private fun rememberReorderState(
    listState: LazyListState,
    onMove: (fromIndex: Int, toIndex: Int) -> Unit,
    coroutineScope: CoroutineScope,
): ReorderState {
    val state = remember(listState, coroutineScope) {
        ReorderState(
            listState = listState,
            coroutineScope = coroutineScope,
        )
    }
    state.updateOnMove(onMove)
    return state
}

private class ReorderState(
    private val listState: LazyListState,
    private val coroutineScope: CoroutineScope,
) {
    private var onMove: (fromIndex: Int, toIndex: Int) -> Unit = { _, _ -> }

    private val itemCoordsByKey = linkedMapOf<Any, LayoutCoordinates>()
    private val handleCoordsByKey = linkedMapOf<Any, LayoutCoordinates>()
    private val itemSizeByKey = linkedMapOf<Any, IntSize>()
    private val indexByKey = linkedMapOf<Any, Int>()

    private var containerWindowOffset by mutableStateOf(Offset.Zero)

    var draggingItemKey by mutableStateOf<Any?>(null)
        private set

    private var draggingGrabOffsetInItem by mutableStateOf(Offset.Zero)

    var draggingPopupOffset by mutableStateOf(IntOffset.Zero)
        private set

    var popupVisible by mutableStateOf(false)
        private set

    private var placeholderHidden by mutableStateOf(false)
    private var popupComposed by mutableStateOf(false)

    var isReleasing by mutableStateOf(false)
        private set

    var releaseToken by mutableStateOf(0)
        private set

    private var scrollJob: Job? = null
    private var hidePlaceholderJob: Job? = null
    private var releaseJob: Job? = null

    fun updateOnMove(onMove: (fromIndex: Int, toIndex: Int) -> Unit) {
        this.onMove = onMove
    }

    fun updateContainerCoords(coords: LayoutCoordinates) {
        containerWindowOffset = coords.localToWindow(Offset.Zero)
    }

    fun updateItemIndex(
        itemKey: Any,
        index: Int,
    ) {
        indexByKey[itemKey] = index
    }

    fun updateItemCoords(
        itemKey: Any,
        coords: LayoutCoordinates,
    ) {
        itemCoordsByKey[itemKey] = coords
        itemSizeByKey[itemKey] = coords.size
    }

    fun updateHandleCoords(
        itemKey: Any,
        coords: LayoutCoordinates,
    ) {
        handleCoordsByKey[itemKey] = coords
    }

    fun itemSizePx(itemKey: Any): IntSize? = itemSizeByKey[itemKey]

    fun isPlaceholderFor(itemKey: Any): Boolean = placeholderHidden && draggingItemKey == itemKey

    fun markPopupComposed() {
        if (!popupComposed) {
            popupComposed = true
            scheduleHidePlaceholder()
        }
    }

    fun startDrag(
        itemKey: Any,
        index: Int,
        localPointerStart: Offset,
    ) {
        releaseJob?.cancel()
        releaseJob = null
        isReleasing = false
        draggingItemKey = itemKey
        indexByKey[itemKey] = index
        popupVisible = false
        placeholderHidden = false
        popupComposed = false
        hidePlaceholderJob?.cancel()
        hidePlaceholderJob = null

        val itemCoords = itemCoordsByKey[itemKey]
        val handleCoords = handleCoordsByKey[itemKey]
        val itemWindow = itemCoords?.localToWindow(Offset.Zero)
        val handleWindow = handleCoords?.localToWindow(localPointerStart)

        if (itemWindow != null && handleWindow != null) {
            draggingGrabOffsetInItem = handleWindow - itemWindow
            val topLeft = handleWindow - draggingGrabOffsetInItem
            val localTopLeft = topLeft - containerWindowOffset
            draggingPopupOffset = IntOffset(localTopLeft.x.roundToInt(), localTopLeft.y.roundToInt())
            popupVisible = itemSizeByKey[itemKey] != null
        } else if (itemWindow != null) {
            draggingGrabOffsetInItem = Offset.Zero
            val localTopLeft = itemWindow - containerWindowOffset
            draggingPopupOffset = IntOffset(localTopLeft.x.roundToInt(), localTopLeft.y.roundToInt())
            popupVisible = itemSizeByKey[itemKey] != null
        } else {
            draggingGrabOffsetInItem = Offset.Zero
            draggingPopupOffset = if (handleWindow != null) {
                val localTopLeft = handleWindow - containerWindowOffset
                IntOffset(localTopLeft.x.roundToInt(), localTopLeft.y.roundToInt())
            } else {
                IntOffset.Zero
            }
        }
    }

    fun dragBy(
        itemKey: Any,
        deltaX: Float,
        deltaY: Float,
        localPointerPosition: Offset,
    ) {
        val currentIndex = indexByKey[itemKey] ?: return
        val draggedSize = itemSizeByKey[itemKey] ?: return

        val handleCoords = handleCoordsByKey[itemKey]
        val pointerWindow = handleCoords?.localToWindow(localPointerPosition)
        if (pointerWindow != null) {
            val topLeft = pointerWindow - draggingGrabOffsetInItem
            val localTopLeft = topLeft - containerWindowOffset
            draggingPopupOffset = IntOffset(localTopLeft.x.roundToInt(), localTopLeft.y.roundToInt())
            popupVisible = true
            scheduleHidePlaceholder()
        } else {
            draggingPopupOffset = draggingPopupOffset + IntOffset(deltaX.toInt(), deltaY.toInt())
        }

        val pointerLocalY = if (pointerWindow != null) pointerWindow.y - containerWindowOffset.y else return
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
            // update our fallback index immediately; indexByKey is refreshed via SideEffect
            indexByKey[itemKey] = targetIndex
        }

        maybeAutoScroll(currentItemStart, currentItemEnd)
    }

    fun endDrag() {
        draggingItemKey = null
        draggingPopupOffset = IntOffset.Zero
        draggingGrabOffsetInItem = Offset.Zero
        popupVisible = false
        placeholderHidden = false
        popupComposed = false
        isReleasing = false
        hidePlaceholderJob?.cancel()
        hidePlaceholderJob = null
        scrollJob?.cancel()
        scrollJob = null
        releaseJob?.cancel()
        releaseJob = null
    }

    fun releaseDrag(durationMillis: Int) {
        val itemKey = draggingItemKey ?: return
        if (isReleasing) return

        val itemCoords = itemCoordsByKey[itemKey]
        val itemWindow = itemCoords?.localToWindow(Offset.Zero)
        if (itemWindow == null) {
            endDrag()
            return
        }

        val targetLocalTopLeft = itemWindow - containerWindowOffset
        val targetOffset = IntOffset(targetLocalTopLeft.x.roundToInt(), targetLocalTopLeft.y.roundToInt())
        val startOffset = draggingPopupOffset

        isReleasing = true
        releaseToken += 1
        // Ensure placeholder stays hidden until release animation finishes.
        placeholderHidden = true

        releaseJob?.cancel()
        releaseJob = coroutineScope.launch {
            val progress = Animatable(0f)
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = durationMillis),
            ) {
                val x = lerpInt(startOffset.x, targetOffset.x, value)
                val y = lerpInt(startOffset.y, targetOffset.y, value)
                draggingPopupOffset = IntOffset(x, y)
            }

            endDrag()
        }
    }

    private fun lerpInt(
        start: Int,
        stop: Int,
        fraction: Float,
    ): Int = (start + (stop - start) * fraction).roundToInt()

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

    private fun maybeAutoScroll(
        currentItemStart: Float,
        currentItemEnd: Float,
    ) {
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
