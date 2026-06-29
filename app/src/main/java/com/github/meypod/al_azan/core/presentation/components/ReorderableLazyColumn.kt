package com.github.meypod.al_azan.core.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector2D
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.findRootCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.github.meypod.al_azan.core.presentation.util.drawVerticalScrollbar
import com.github.meypod.al_azan.core.presentation.util.fadeScrollEdges
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sign
import androidx.compose.runtime.key as composeKey

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
    contentPadding: PaddingValues = PaddingValues(0.dp),
    listState: LazyListState = rememberLazyListState(),
    /**
     * When false, items are laid out in a non-scrolling [Column] that grows to fit all items,
     * so the list can be nested inside an externally scrolling page. The internal [LazyColumn]
     * scroll, edge fade and scrollbar are disabled in this mode, and [contentPadding] is ignored.
     * Provide [pageScrollState] to enable edge auto-scroll of the host page during a drag.
     */
    scrollable: Boolean = true,
    /**
     * Host page scroll state, used only when [scrollable] is false: while dragging an item toward
     * the top/bottom window edge, this page is scrolled so off-screen items can be reached.
     */
    pageScrollState: ScrollState? = null,
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
    reverseLayout: Boolean = false,
    verticalArrangement: Arrangement.Vertical = if (!reverseLayout) Arrangement.Top else Arrangement.Bottom,
    footerContent: (@Composable () -> Unit)? = null,
) {
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current

    val reorderState = rememberReorderState(
        listState = listState,
        onMove = onMove,
        coroutineScope = coroutineScope,
        lazyMode = scrollable,
    )
    reorderState.updatePageScrollState(if (scrollable) null else pageScrollState)
    reorderState.updateViewportBounds(if (scrollable) null else LocalPageScrollViewportBounds.current)

    Box(
        modifier = modifier.onGloballyPositioned { coords ->
            reorderState.updateContainerCoords(coords)
        },
    ) {
        if (scrollable) {
            LazyColumn(
                modifier = listModifier
                    .fadeScrollEdges(listState, Orientation.Vertical)
                    .drawVerticalScrollbar(listState),
                state = listState,
                contentPadding = contentPadding,
                reverseLayout = reverseLayout,
                verticalArrangement = verticalArrangement,
            ) {
                itemsIndexed(
                    items = items,
                    key = { _, item -> key(item) },
                ) { index, item ->
                    ReorderableItemWrapper(
                        item = item,
                        itemKey = key(item),
                        index = index,
                        reorderState = reorderState,
                        // animateItem is only valid inside a LazyItemScope.
                        placementModifier = Modifier.animateItem(
                            placementSpec = tween<IntOffset>(durationMillis = itemPlacementAnimMillis),
                        ),
                        releaseAnimMillis = releaseAnimMillis,
                        itemContent = itemContent,
                    )
                }
                footerContent?.let {
                    item(key = "__footer__") { it() }
                }
            }
        } else {
            Column(
                modifier = listModifier,
                verticalArrangement = verticalArrangement,
            ) {
                items.forEachIndexed { index, item ->
                    val itemKey = key(item)
                    // Stable identity per item so reordering moves the composable (and its in-flight
                    // drag gesture) instead of re-keying the slot and cancelling the drag.
                    composeKey(itemKey) {
                        ReorderableItemWrapper(
                            item = item,
                            itemKey = itemKey,
                            index = index,
                            reorderState = reorderState,
                            // animateItem is LazyItemScope-only; mimic its placement animation in a
                            // plain Column by animating each item's offset toward its laid-out position.
                            placementModifier = Modifier.animatePlacement(
                                tween(durationMillis = itemPlacementAnimMillis),
                            ),
                            releaseAnimMillis = releaseAnimMillis,
                            itemContent = itemContent,
                        )
                    }
                }
                footerContent?.invoke()
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

/**
 * Renders a single reorderable item: index tracking, placeholder state, coordinate reporting and
 * the long-press drag handle. The placement-animation part of [baseItemModifier] differs per host
 * (lazy vs non-lazy), so it is supplied by the caller; everything else is shared.
 */
@Composable
private fun <T> ReorderableItemWrapper(
    item: T,
    itemKey: Any,
    index: Int,
    reorderState: ReorderState,
    placementModifier: Modifier,
    releaseAnimMillis: Int,
    itemContent: @Composable (
        item: T,
        isPlaceholder: Boolean,
        itemModifier: Modifier,
        dragHandleModifier: Modifier,
    ) -> Unit,
) {
    SideEffect {
        reorderState.updateItemIndex(itemKey, index)
    }

    val isPlaceholder = reorderState.isPlaceholderFor(itemKey)

    // onGloballyPositioned must sit OUTSIDE the placement animation so it reports the settled
    // target position, not the mid-animation offset. Hit-testing against the animated position
    // would re-trigger onMove as a neighbour slides under the finger and oscillate.
    val itemModifier = Modifier
        .onGloballyPositioned { coords ->
            reorderState.updateItemCoords(itemKey, coords)
        }
        .then(placementModifier)

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

/**
 * Animates a layout node toward its target position whenever the parent re-places it (e.g. on
 * reorder). A LookaheadScope-free stand-in for `LazyItemScope.animateItem` usable inside a plain
 * [Column]: [onPlaced] reports the natural target position, and [offset] lags the visual position
 * behind it until the [Animatable] catches up.
 */
private fun Modifier.animatePlacement(animationSpec: FiniteAnimationSpec<IntOffset>): Modifier =
    composed {
        val scope = rememberCoroutineScope()
        var targetOffset by remember { mutableStateOf(IntOffset.Zero) }
        var animatable by remember { mutableStateOf<Animatable<IntOffset, AnimationVector2D>?>(null) }
        this
            .onPlaced { coords ->
                targetOffset = coords.positionInParent().round()
            }
            .offset {
                val anim = animatable
                    ?: Animatable(targetOffset, IntOffset.VectorConverter).also { animatable = it }
                if (anim.targetValue != targetOffset) {
                    scope.launch { anim.animateTo(targetOffset, animationSpec) }
                }
                anim.value - targetOffset
            }
    }

@Composable
private fun rememberReorderState(
    listState: LazyListState,
    onMove: (fromIndex: Int, toIndex: Int) -> Unit,
    coroutineScope: CoroutineScope,
    lazyMode: Boolean,
): ReorderState {
    val state = remember(listState, coroutineScope, lazyMode) {
        ReorderState(
            listState = listState,
            coroutineScope = coroutineScope,
            lazyMode = lazyMode,
        )
    }
    state.updateOnMove(onMove)
    return state
}

private class ReorderState(
    private val listState: LazyListState,
    private val coroutineScope: CoroutineScope,
    private val lazyMode: Boolean,
) {
    private var onMove: (fromIndex: Int, toIndex: Int) -> Unit = { _, _ -> }
    private var pageScrollState: ScrollState? = null
    private var viewportBounds: PageScrollViewportBounds? = null

    private var rootHeightPx = 0

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

    private var hidePlaceholderJob: Job? = null
    private var releaseJob: Job? = null

    // Signed edge-scroll intensity in -1f..1f: sign is direction (negative = toward top), magnitude
    // is how far the dragged item has penetrated the edge band (0 at the band boundary, 1 at the
    // window edge). Updated from drag events; the continuous [autoScrollJob] loop reads it so
    // scrolling persists and tracks proximity while the finger is held still.
    private var autoScrollIntensity = 0f
    private var autoScrollJob: Job? = null

    // Last finger position in window space, so the auto-scroll loop can re-run the reorder hit-test
    // while the finger is held still and items scroll under it.
    private var lastPointerWindowY: Float? = null

    fun updateOnMove(onMove: (fromIndex: Int, toIndex: Int) -> Unit) {
        this.onMove = onMove
    }

    fun updatePageScrollState(state: ScrollState?) {
        pageScrollState = state
    }

    fun updateViewportBounds(bounds: PageScrollViewportBounds?) {
        viewportBounds = bounds
    }

    fun updateContainerCoords(coords: LayoutCoordinates) {
        containerWindowOffset = coords.localToWindow(Offset.Zero)
        rootHeightPx = coords.findRootCoordinates().size.height
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
        lastPointerWindowY = pointerWindow.y

        // Edge auto-scroll keys off the finger, not the item's extents: a tall item in a short
        // viewport would otherwise trip the bottom edge from the start (e.g. counter screen).
        maybeAutoScroll(pointerLocalY)
        tryReorder(itemKey, pointerWindow.y)
    }

    /**
     * Runs the hit-test for [itemKey] against the last finger window position and applies a move.
     * Called from drag events and, while edge auto-scrolling, once per frame so items still reorder
     * as they scroll under a finger that is being held still.
     */
    private fun tryReorder(
        itemKey: Any,
        pointerWindowY: Float,
    ) {
        val currentIndex = indexByKey[itemKey] ?: return
        val draggedSize = itemSizeByKey[itemKey] ?: return

        val pointerLocalY = pointerWindowY - containerWindowOffset.y
        val currentItemCenter = pointerLocalY - draggingGrabOffsetInItem.y + draggedSize.height / 2f

        val targetIndex = if (lazyMode) {
            listState.layoutInfo.visibleItemsInfo.firstOrNull { itemInfo ->
                itemInfo.index != currentIndex &&
                    currentItemCenter >= itemInfo.offset &&
                    currentItemCenter <= (itemInfo.offset + itemInfo.size)
            }?.index
        } else {
            // No LazyListState viewport in non-lazy mode; hit-test against tracked item coords,
            // mapped into the same container-local space as currentItemCenter.
            indexByKey.entries.firstOrNull { (otherKey, otherIndex) ->
                if (otherIndex == currentIndex) return@firstOrNull false
                val coords = itemCoordsByKey[otherKey] ?: return@firstOrNull false
                if (!coords.isAttached) return@firstOrNull false
                val size = itemSizeByKey[otherKey] ?: return@firstOrNull false
                val top = coords.localToWindow(Offset.Zero).y - containerWindowOffset.y
                currentItemCenter >= top && currentItemCenter <= top + size.height
            }?.value
        }

        if (targetIndex == null) return

        // Pin the viewport to its current top slot across the reorder, otherwise dragging the first
        // visible item moves its key and LazyColumn re-anchors to follow it, jumping. Skip while edge
        // auto-scrolling, where the list is meant to be moving and a pin would freeze the scroll.
        val pinViewport = lazyMode && autoScrollIntensity == 0f
        val anchorIndex = if (pinViewport) listState.firstVisibleItemIndex else 0
        val anchorOffset = if (pinViewport) listState.firstVisibleItemScrollOffset else 0
        onMove(currentIndex, targetIndex)
        // update our fallback index immediately; indexByKey is refreshed via SideEffect
        indexByKey[itemKey] = targetIndex
        if (pinViewport) {
            listState.requestScrollToItem(anchorIndex, anchorOffset)
        }
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
        autoScrollIntensity = 0f
        autoScrollJob?.cancel()
        autoScrollJob = null
        lastPointerWindowY = null
        releaseJob?.cancel()
        releaseJob = null
    }

    fun releaseDrag(durationMillis: Int) {
        val itemKey = draggingItemKey ?: return
        if (isReleasing) return

        // Halt edge auto-scroll the moment the finger lifts, before the landing animation.
        autoScrollIntensity = 0f
        autoScrollJob?.cancel()
        autoScrollJob = null

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

    /**
     * Sets the desired edge-scroll intensity from the finger position and ensures the continuous
     * [autoScrollJob] loop is running. Both lazy and non-lazy modes share the same loop (ramped,
     * proximity-scaled); they differ only in which viewport bounds and scroll target they use.
     */
    private fun maybeAutoScroll(pointerViewportY: Float) {
        autoScrollIntensity = if (lazyMode) {
            lazyEdgeIntensity(pointerViewportY)
        } else {
            pageEdgeIntensity(pointerViewportY)
        }
        if (autoScrollIntensity != 0f && autoScrollJob?.isActive != true) {
            autoScrollJob = coroutineScope.launch { runAutoScroll() }
        }
    }

    private fun lazyEdgeIntensity(pointerViewportY: Float): Float {
        val viewportStart = listState.layoutInfo.viewportStartOffset.toFloat()
        val viewportEnd = listState.layoutInfo.viewportEndOffset.toFloat()
        if (viewportEnd <= viewportStart) return 0f
        val topPenetration = (viewportStart + AUTO_SCROLL_EDGE_PX - pointerViewportY) / AUTO_SCROLL_EDGE_PX
        val bottomPenetration = (pointerViewportY - (viewportEnd - AUTO_SCROLL_EDGE_PX)) / AUTO_SCROLL_EDGE_PX
        return edgeIntensity(topPenetration, bottomPenetration)
    }

    /**
     * Edge bands measured against the scroll viewport (below the app bar / above the bottom bar)
     * when known, falling back to the raw window if the host didn't publish bounds.
     */
    private fun pageEdgeIntensity(pointerViewportY: Float): Float {
        val topEdgePx = viewportBounds?.topPx ?: 0f
        val bottomEdgePx = viewportBounds?.bottomPx ?: rootHeightPx.toFloat()
        if (pageScrollState == null || bottomEdgePx <= topEdgePx) return 0f
        val pointerWindowY = pointerViewportY + containerWindowOffset.y
        val topPenetration = (topEdgePx + AUTO_SCROLL_EDGE_PX - pointerWindowY) / AUTO_SCROLL_EDGE_PX
        val bottomPenetration = (pointerWindowY - (bottomEdgePx - AUTO_SCROLL_EDGE_PX)) / AUTO_SCROLL_EDGE_PX
        return edgeIntensity(topPenetration, bottomPenetration)
    }

    /** Penetration into an edge band, 0 at the boundary up to 1 at the edge; sign is direction. */
    private fun edgeIntensity(
        topPenetration: Float,
        bottomPenetration: Float,
    ): Float =
        when {
            topPenetration > 0f -> -topPenetration.coerceAtMost(1f)
            bottomPenetration > 0f -> bottomPenetration.coerceAtMost(1f)
            else -> 0f
        }

    private suspend fun runAutoScroll() {
        var lastFrameNanos = withFrameNanos { it }
        var rampMillis = 0f
        var previousSign = sign(autoScrollIntensity)
        // Re-check after scrolling roughly one (smallest) item height, so no item can slip past the
        // held finger between checks regardless of how short items are. Recomputed below as sizes change.
        var sinceReorderPx = Float.MAX_VALUE // re-check on the first frame

        while (autoScrollIntensity != 0f) {
            val frameNanos = withFrameNanos { it }
            val dtSeconds = ((frameNanos - lastFrameNanos) / 1_000_000_000f).coerceIn(0f, 0.05f)
            lastFrameNanos = frameNanos

            val currentSign = sign(autoScrollIntensity)
            if (currentSign != previousSign) {
                rampMillis = 0f
                previousSign = currentSign
            }
            rampMillis += dtSeconds * 1000f

            // Ease-in over time from a small floor, then scale by edge proximity: deeper finger = faster.
            val ramp = (rampMillis / AUTO_SCROLL_RAMP_MILLIS).coerceIn(0f, 1f)
            val rampFraction = AUTO_SCROLL_MIN_FRACTION + (1f - AUTO_SCROLL_MIN_FRACTION) * ramp * ramp
            val delta = autoScrollIntensity * AUTO_SCROLL_MAX_PX_PER_SEC * rampFraction * dtSeconds

            val consumed = if (lazyMode) {
                // The list scrolls under a fixed container, so the popup stays under the finger.
                listState.scrollBy(delta)
            } else {
                val moved = pageScrollState?.scrollBy(delta) ?: break
                if (moved != 0f) {
                    draggingPopupOffset += IntOffset(0, moved.roundToInt())
                }
                moved
            }

            // Reorder as items scroll under the held-still finger, mirroring drag-move behaviour.
            // Throttled by scrolled distance (not frames) so it never skips an item yet avoids a
            // per-frame hit-test; stalls naturally at the list end where consumed is 0.
            sinceReorderPx += abs(consumed)
            // Half the smallest item height: a normal drag swaps after ~h/2 + spacing of travel, and
            // detection bands have spacing gaps between them, so half-height clears those dead zones.
            val minItemPx = itemSizeByKey.values.minOfOrNull { it.height }?.toFloat()
                ?: AUTO_SCROLL_REORDER_FALLBACK_PX
            val reorderStepPx = (minItemPx * 0.5f).coerceAtLeast(1f)
            if (sinceReorderPx >= reorderStepPx) {
                sinceReorderPx = 0f
                val pointerWindowY = lastPointerWindowY
                val draggingKey = draggingItemKey
                if (pointerWindowY != null && draggingKey != null) {
                    tryReorder(draggingKey, pointerWindowY)
                }
            }
        }
        autoScrollJob = null
    }
}

private const val AUTO_SCROLL_EDGE_PX = 96f
private const val AUTO_SCROLL_MAX_PX_PER_SEC = 1600f
private const val AUTO_SCROLL_RAMP_MILLIS = 650f
private const val AUTO_SCROLL_MIN_FRACTION = 0.12f

// Fallback reorder-recheck distance (px) used only before any item size is known; normally the step
// is the smallest measured item height so no item can slip past the finger between checks.
private const val AUTO_SCROLL_REORDER_FALLBACK_PX = 36f
