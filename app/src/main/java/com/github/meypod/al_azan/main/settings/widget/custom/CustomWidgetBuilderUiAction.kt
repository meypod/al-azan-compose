package com.github.meypod.al_azan.main.settings.widget.custom

import com.github.meypod.al_azan.core.domain.model.adhan.Prayer
import com.github.meypod.al_azan.core.domain.model.widget.HeaderBlock

sealed interface CustomWidgetBuilderUiAction {
    data class OnBgColorChange(
        val color: Int?,
    ) : CustomWidgetBuilderUiAction

    data class OnTextColorChange(
        val color: Int?,
    ) : CustomWidgetBuilderUiAction

    data class OnHighlightColorChange(
        val color: Int?,
    ) : CustomWidgetBuilderUiAction

    /** The full row grid after a drag add / reorder / move-between-rows / remove. */
    data class OnRowsChange(
        val rows: List<List<Prayer>>,
    ) : CustomWidgetBuilderUiAction

    /** Number of prayer rows (1 or 2); re-shapes the grid. */
    data class OnRowCountChange(
        val count: Int,
    ) : CustomWidgetBuilderUiAction

    data class OnCountdownToggle(
        val enabled: Boolean,
    ) : CustomWidgetBuilderUiAction

    data class OnCountdownColorChange(
        val color: Int?,
    ) : CustomWidgetBuilderUiAction

    /** Per-section text-size multipliers over the baseline (see [CustomWidgetConfig.FONT_SCALE_RANGE]). */
    data class OnHeaderFontScaleChange(
        val scale: Float,
    ) : CustomWidgetBuilderUiAction

    data class OnPrayerFontScaleChange(
        val scale: Float,
    ) : CustomWidgetBuilderUiAction

    data class OnCountdownFontScaleChange(
        val scale: Float,
    ) : CustomWidgetBuilderUiAction

    data class OnLocationToggle(
        val id: String,
        val enabled: Boolean,
    ) : CustomWidgetBuilderUiAction

    data class OnTopStartChange(
        val block: HeaderBlock?,
    ) : CustomWidgetBuilderUiAction

    data class OnTopEndChange(
        val block: HeaderBlock?,
    ) : CustomWidgetBuilderUiAction

    /** Set both header slots at once (drag between slots, swap button). */
    data class OnHeaderSlotsChange(
        val topStart: HeaderBlock?,
        val topEnd: HeaderBlock?,
    ) : CustomWidgetBuilderUiAction
}
