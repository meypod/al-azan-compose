package com.github.meypod.al_azan.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.SystemClock
import android.text.TextUtils
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import androidx.annotation.ColorInt
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.github.meypod.al_azan.MainActivity
import com.github.meypod.al_azan.R
import com.github.meypod.al_azan.core.data.locale.withAppLocale
import com.github.meypod.al_azan.core.domain.model.widget.CustomWidgetData
import com.github.meypod.al_azan.core.domain.model.widget.CustomWidgetPrayerCell
import com.github.meypod.al_azan.core.presentation.navigation.Route
import com.github.meypod.al_azan.widget.CustomWidgetRenderer.buildEmptyHint
import java.util.Locale

/**
 * Builds [RemoteViews] for the user-authored custom widget. Unlike [WidgetRenderer] (a fixed layout
 * with theme-driven colors), this composes the widget dynamically: header cells and prayer rows are
 * inflated as child [RemoteViews] and appended with [RemoteViews.addView], and every color is applied
 * programmatically from [CustomWidgetData], falling back to the theme-adaptive default color resources.
 *
 * With more than one location ([CustomWidgetData.pages]) the widget shows ONE full-width location page
 * plus a nav bar of ‹ prev / dots / next › arrows. A widget can't receive horizontal swipe (the
 * launcher owns it) and `StackView` only does a scaled vertical-fling stack, so locations are switched
 * by tapping the arrows — each fires a broadcast that re-renders the widget at the next page (the same
 * pattern the Google Weather widget uses). [pageIndex] is the page to show; [appWidgetId] targets the
 * arrows' broadcasts at this specific widget instance.
 */
object CustomWidgetRenderer {

    const val ACTION_PAGE = "com.github.meypod.al_azan.action.CUSTOM_WIDGET_PAGE"
    const val EXTRA_DELTA = "com.github.meypod.al_azan.extra.CUSTOM_WIDGET_PAGE_DELTA"

    fun build(
        context: Context,
        data: CustomWidgetData,
        pageIndex: Int = 0,
        appWidgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID,
    ): RemoteViews {
        val colors = Colors(data.bgColor, data.textColor, data.highlightColor, data.countdownColor)
        return if (data.pages.size > 1) {
            buildPaged(context, data, colors, pageIndex, appWidgetId)
        } else {
            buildInline(context, data, colors)
        }
    }

    /**
     * The user's chosen colors, each nullable: null means "match theme", and is applied by NOT setting
     * a programmatic color so the block layout's `@color/custom_widget_*` default (which has a -night
     * variant) stands and the launcher re-resolves it on a light/dark switch — no app re-render needed.
     * A non-null value is a fixed color the user picked and is applied programmatically.
     */
    private class Colors(
        @param:ColorInt val bg: Int?,
        @param:ColorInt val text: Int?,
        @param:ColorInt val highlight: Int?,
        @param:ColorInt val countdown: Int?,
    )

    /** Set the background color only when the user picked one; otherwise keep the theme-adaptive default. */
    private fun RemoteViews.applyBgColor(
        viewId: Int,
        @ColorInt color: Int?,
    ) {
        if (color != null) setInt(viewId, "setBackgroundColor", color)
    }

    /** Set the text color only when the user picked one; otherwise keep the theme-adaptive default. */
    private fun RemoteViews.applyTextColor(
        viewId: Int,
        @ColorInt color: Int?,
    ) {
        if (color != null) setTextColor(viewId, color)
    }

    private fun buildInline(
        context: Context,
        data: CustomWidgetData,
        colors: Colors,
    ): RemoteViews {
        val localized = context.withAppLocale(data.locale)
        val views = RemoteViews(context.packageName, R.layout.custom_widget)
        views.applyBgColor(R.id.custom_widget_root, colors.bg)

        val direction = layoutDirection(data.locale)
        views.setInt(R.id.custom_widget_header, "setLayoutDirection", direction)
        views.setInt(R.id.custom_widget_content, "setLayoutDirection", direction)

        addHeaderCells(
            context,
            views,
            R.id.custom_widget_header,
            data.topStartText,
            data.topEndText,
            colors.text,
            BASE_TEXT_SP * data.headerFontScale,
        )
        addPrayerRows(
            context,
            views,
            R.id.custom_widget_content,
            data.prayerRows,
            colors,
            localized,
            BASE_TEXT_SP * data.prayerFontScale,
        )
        applyCountdown(views, data, colors, localized, BASE_TEXT_SP * data.countdownFontScale)

        // Hide any empty weighted band so the visible ones split the leftover height (balanced spacing);
        // the countdown band's own visibility is set in applyCountdown.
        val hasHeader = data.topStartText != null || data.topEndText != null
        views.setViewVisibility(R.id.custom_widget_header, if (hasHeader) View.VISIBLE else View.GONE)
        views.setViewVisibility(R.id.custom_widget_content, if (data.prayerRows.isNotEmpty()) View.VISIBLE else View.GONE)

        // Nothing placed anywhere → point the user at the builder screen (many won't know it exists).
        val empty = data.prayerRows.isEmpty() &&
            data.topStartText == null && data.topEndText == null &&
            !(data.showCountdown && data.countdown != null)
        if (empty) {
            views.setViewVisibility(R.id.cw_empty_hint, View.VISIBLE)
            views.setTextViewText(R.id.cw_empty_hint, localized.getString(R.string.custom_widget_empty_hint))
            views.applyTextColor(R.id.cw_empty_hint, colors.text)
        } else {
            views.setViewVisibility(R.id.cw_empty_hint, View.GONE)
        }

        // When the widget is empty (only the hint), tapping it jumps straight to the builder; otherwise
        // it opens the app.
        views.setOnClickPendingIntent(
            R.id.custom_widget_root,
            if (empty) builderPendingIntent(context) else launchPendingIntent(context),
        )
        return views
    }

    /** Widget with nothing placed yet: the "set me up" hint, tapping through to the builder. */
    fun buildEmptyHint(context: Context): RemoteViews =
        buildHint(context, context.getString(R.string.custom_widget_empty_hint), builderPendingIntent(context))

    /**
     * Widget IS configured, but the app can't compute times yet — no calculation method or no location.
     * Shows [messageRes] (reuse the home screen's "Set location" / "Set calculation method" prompts) and
     * taps through to the app to fix it, unlike [buildEmptyHint] which points at the builder.
     */
    fun buildConfigHint(
        context: Context,
        @StringRes messageRes: Int,
    ): RemoteViews = buildHint(context, context.getString(messageRes), launchPendingIntent(context))

    private fun buildHint(
        context: Context,
        message: String,
        onClick: PendingIntent,
    ): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.custom_widget)
        views.removeAllViews(R.id.custom_widget_header)
        views.removeAllViews(R.id.custom_widget_content)
        // These are weighted bands; hide them so the hint (the only visible child) centers in the widget.
        views.setViewVisibility(R.id.custom_widget_header, View.GONE)
        views.setViewVisibility(R.id.custom_widget_content, View.GONE)
        views.setViewVisibility(R.id.custom_widget_countdown_row, View.GONE)
        // No colors set: the layout's theme-adaptive @color defaults stand (this hint has no user config).
        views.setViewVisibility(R.id.cw_empty_hint, View.VISIBLE)
        views.setTextViewText(R.id.cw_empty_hint, message)
        views.setOnClickPendingIntent(R.id.custom_widget_root, onClick)
        return views
    }

    private fun buildPaged(
        context: Context,
        data: CustomWidgetData,
        colors: Colors,
        pageIndex: Int,
        appWidgetId: Int,
    ): RemoteViews {
        val localized = context.withAppLocale(data.locale)
        val pageCount = data.pages.size
        val index = ((pageIndex % pageCount) + pageCount) % pageCount // wrap (cycles) and clamp
        val page = data.pages[index]

        val views = RemoteViews(context.packageName, R.layout.custom_widget_paged)
        views.applyBgColor(R.id.cw_paged_root, colors.bg)
        val direction = layoutDirection(data.locale)
        views.setInt(R.id.cw_paged_header, "setLayoutDirection", direction)
        views.setInt(R.id.cw_paged_content, "setLayoutDirection", direction)
        addHeaderCells(
            context,
            views,
            R.id.cw_paged_header,
            page.topStartText,
            page.topEndText,
            colors.text,
            BASE_TEXT_SP * data.headerFontScale,
        )
        addPrayerRows(
            context,
            views,
            R.id.cw_paged_content,
            page.prayerRows,
            colors,
            localized,
            BASE_TEXT_SP * data.prayerFontScale,
        )
        // Countdown (primary location's next prayer) — shown on every page, so multi-location + countdown
        // can both be on. The row reuses the inline ids, present in custom_widget_paged.xml too.
        applyCountdown(views, data, colors, localized, BASE_TEXT_SP * data.countdownFontScale)

        // Hide an empty header/content band so the visible one(s) fill the space above the nav bar.
        val hasHeader = page.topStartText != null || page.topEndText != null
        views.setViewVisibility(R.id.cw_paged_header, if (hasHeader) View.VISIBLE else View.GONE)
        views.setViewVisibility(R.id.cw_paged_content, if (page.prayerRows.isNotEmpty()) View.VISIBLE else View.GONE)

        // Dots need a concrete color (the lit one plus a dimmed unlit alpha), so resolve the theme default
        // here when the user left a color unset; they won't follow a light/dark switch without a re-render,
        // but they're a small secondary indicator (the header/rows/bg above still adapt on their own).
        val dotLit = colors.highlight ?: ContextCompat.getColor(context, R.color.custom_widget_highlight)
        val dotUnlit = withAlpha(colors.text ?: ContextCompat.getColor(context, R.color.custom_widget_text), 0x66)
        views.removeAllViews(R.id.cw_paged_dots)
        repeat(pageCount) { dotIndex ->
            val dot = RemoteViews(context.packageName, R.layout.custom_widget_dot)
            dot.setTextColor(R.id.cw_dot, if (dotIndex == index) dotLit else dotUnlit)
            dot.setViewPadding(R.id.cw_dot, DOT_GAP_PX, 0, DOT_GAP_PX, 0)
            views.addView(R.id.cw_paged_dots, dot)
        }

        views.applyTextColor(R.id.cw_paged_prev, colors.text)
        views.applyTextColor(R.id.cw_paged_next, colors.text)
        views.setOnClickPendingIntent(R.id.cw_paged_prev, pageIntent(context, appWidgetId, -1))
        views.setOnClickPendingIntent(R.id.cw_paged_next, pageIntent(context, appWidgetId, +1))
        // Tapping the body (not the arrows) opens the app.
        views.setOnClickPendingIntent(R.id.cw_paged_root, launchPendingIntent(context))
        return views
    }

    /** Header: one weighted cell per non-empty slot (0, 1 or 2 cells). */
    private fun addHeaderCells(
        context: Context,
        target: RemoteViews,
        containerId: Int,
        topStartText: String?,
        topEndText: String?,
        @ColorInt text: Int?,
        sizeSp: Float,
    ) {
        // The launcher reuses the inflated view across updates, so a fresh addView would append to the
        // already-populated container (duplicated header/rows). Clear it first.
        target.removeAllViews(containerId)
        listOfNotNull(topStartText, topEndText).forEach { cellText ->
            val cell = RemoteViews(context.packageName, R.layout.custom_widget_header_cell)
            cell.setTextViewText(R.id.cw_header_text, cellText)
            cell.applyTextColor(R.id.cw_header_text, text)
            cell.setTextViewTextSize(R.id.cw_header_text, TypedValue.COMPLEX_UNIT_SP, sizeSp)
            target.addView(containerId, cell)
        }
    }

    /** One weighted column per prayer, split across the given rows; active cells use the highlight color. */
    private fun addPrayerRows(
        context: Context,
        target: RemoteViews,
        containerId: Int,
        rows: List<List<CustomWidgetPrayerCell>>,
        colors: Colors,
        localized: Context,
        sizeSp: Float,
    ) {
        target.removeAllViews(containerId) // clear stale children from a reused view (see addHeaderCells)
        rows.forEach { row ->
            val rowViews = RemoteViews(context.packageName, R.layout.custom_widget_prayer_row)
            row.forEach { cell ->
                // An active cell with a "match theme" highlight uses the active-column layout, whose text
                // defaults to the @color/custom_widget_highlight resource (so it adapts on a theme switch);
                // any explicitly-picked color is applied over the normal column instead.
                val color = if (cell.isActive) colors.highlight else colors.text
                val layout = if (cell.isActive && colors.highlight == null) {
                    R.layout.custom_widget_prayer_col_active
                } else {
                    R.layout.custom_widget_prayer_col
                }
                val col = RemoteViews(context.packageName, layout)
                col.setTextViewText(R.id.cw_col_name, localized.getString(cell.prayer.stringRes))
                col.setTextViewText(R.id.cw_col_time, cell.timeText)
                col.applyTextColor(R.id.cw_col_name, color)
                col.applyTextColor(R.id.cw_col_time, color)
                col.setTextViewTextSize(R.id.cw_col_name, TypedValue.COMPLEX_UNIT_SP, sizeSp)
                col.setTextViewTextSize(R.id.cw_col_time, TypedValue.COMPLEX_UNIT_SP, sizeSp)
                rowViews.addView(R.id.cw_prayer_row, col)
            }
            target.addView(containerId, rowViews)
        }
    }

    private fun applyCountdown(
        views: RemoteViews,
        data: CustomWidgetData,
        colors: Colors,
        localized: Context,
        sizeSp: Float,
    ) {
        if (data.showCountdown && data.countdown != null) {
            views.setViewVisibility(R.id.custom_widget_countdown_row, View.VISIBLE)
            views.setTextViewText(
                R.id.cw_countdown_label,
                "${localized.getString(data.countdown.prayer.stringRes)}: ",
            )
            // Label follows the text color; the chronometer follows the countdown color, whose layout
            // default is @color/custom_widget_highlight — both left as resources when "match theme".
            views.applyTextColor(R.id.cw_countdown_label, colors.text)
            views.applyTextColor(R.id.cw_countdown, colors.countdown)
            views.setTextViewTextSize(R.id.cw_countdown_label, TypedValue.COMPLEX_UNIT_SP, sizeSp)
            views.setTextViewTextSize(R.id.cw_countdown, TypedValue.COMPLEX_UNIT_SP, sizeSp)
            val base = SystemClock.elapsedRealtime() + (data.countdown.baseMillis - System.currentTimeMillis())
            views.setChronometer(R.id.cw_countdown, base, null, true)
        } else {
            views.setViewVisibility(R.id.custom_widget_countdown_row, View.GONE)
        }
    }

    @ColorInt
    private fun withAlpha(
        @ColorInt color: Int,
        alpha: Int,
    ): Int = (color and 0x00FFFFFF) or (alpha shl 24)

    /**
     * Lay the widget out to match the app locale (so header slots and prayer order mirror the builder),
     * not the launcher host's locale — the widget resolves its strings via the app locale too.
     */
    private fun layoutDirection(locale: String): Int =
        if (TextUtils.getLayoutDirectionFromLocale(Locale.forLanguageTag(locale)) == View.LAYOUT_DIRECTION_RTL) {
            View.LAYOUT_DIRECTION_RTL
        } else {
            View.LAYOUT_DIRECTION_LTR
        }

    /** Broadcast to [CustomWidget] that flips this widget by [delta] pages. */
    private fun pageIntent(
        context: Context,
        appWidgetId: Int,
        delta: Int,
    ): PendingIntent {
        val intent = Intent(context, CustomWidget::class.java).apply {
            action = ACTION_PAGE
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            putExtra(EXTRA_DELTA, delta)
            // Distinct data per (widget, direction) so the PendingIntents don't collapse into one.
            data = Uri.parse("alazan://custom-widget/$appWidgetId/$delta")
        }
        return PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun launchPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    /** Opens the app directly on the custom-widget builder (via its deep link); used by the empty hint. */
    private fun builderPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            data = Route.Main.Settings.WidgetSettings.CustomBuilder.toUriString().toUri()
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        return PendingIntent.getActivity(
            context,
            1,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private const val DOT_GAP_PX = 4

    /** The layouts' baseline content text size (sp); [CustomWidgetData.fontScale] multiplies it. */
    private const val BASE_TEXT_SP = 13f
}
