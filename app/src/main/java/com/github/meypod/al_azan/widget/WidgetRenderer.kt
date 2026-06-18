package com.github.meypod.al_azan.widget

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import android.util.SizeF
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import com.github.meypod.al_azan.MainActivity
import com.github.meypod.al_azan.R
import com.github.meypod.al_azan.core.data.locale.withAppLocale
import com.github.meypod.al_azan.core.domain.model.widget.NextPrayerWidgetData
import com.github.meypod.al_azan.core.domain.model.widget.WidgetData

/**
 * Builds [RemoteViews] for the prayer-times widgets. Kotlin port of the legacy `WidgetUtils`,
 * driven by an already-computed [WidgetData] instead of a React bridge payload.
 */
object WidgetRenderer {

    private const val MAX_SLOTS = 6

    // Each prayer slot has a normal and an "active" variant; index = slot*2 (+1 for active).
    private val slotContainerIds = intArrayOf(
        R.id.prayer1,
        R.id.prayer1active,
        R.id.prayer2,
        R.id.prayer2active,
        R.id.prayer3,
        R.id.prayer3active,
        R.id.prayer4,
        R.id.prayer4active,
        R.id.prayer5,
        R.id.prayer5active,
        R.id.prayer6,
        R.id.prayer6active,
    )

    private val slotNameIds = intArrayOf(
        R.id.prayer1_name,
        R.id.prayer1active_name,
        R.id.prayer2_name,
        R.id.prayer2active_name,
        R.id.prayer3_name,
        R.id.prayer3active_name,
        R.id.prayer4_name,
        R.id.prayer4active_name,
        R.id.prayer5_name,
        R.id.prayer5active_name,
        R.id.prayer6_name,
        R.id.prayer6active_name,
    )

    private val slotTimeIds = intArrayOf(
        R.id.prayer1_time,
        R.id.prayer1active_time,
        R.id.prayer2_time,
        R.id.prayer2active_time,
        R.id.prayer3_time,
        R.id.prayer3active_time,
        R.id.prayer4_time,
        R.id.prayer4active_time,
        R.id.prayer5_time,
        R.id.prayer5active_time,
        R.id.prayer6_time,
        R.id.prayer6active_time,
    )

    fun build(
        context: Context,
        layoutResId: Int,
        data: WidgetData,
    ): RemoteViews {
        // Caller contexts (application context, receiver context) don't carry the per-app locale on
        // pre-API 33 — resolve prayer-name strings through a localized context instead.
        val localized = context.withAppLocale(data.locale)
        val views = RemoteViews(context.packageName, layoutResId)

        // Flip the widget's reading direction. RTL is the default (prayers laid out right-to-left);
        // the swap toggle forces LTR. Set explicitly each build so toggling off restores RTL.
        val direction = if (data.swapLayoutDirection) View.LAYOUT_DIRECTION_LTR else View.LAYOUT_DIRECTION_RTL
        views.setInt(R.id.widget_header, "setLayoutDirection", direction)
        views.setInt(R.id.widget_content, "setLayoutDirection", direction)

        // Header texts. Ids are absent in the small notification layout; RemoteViews ignores those.
        views.setTextViewText(R.id.top_start_text, data.topStartText)
        views.setTextViewText(R.id.top_end_text, data.topEndText)

        // Hide every slot first, then reveal the ones we use.
        slotContainerIds.forEach { views.setViewVisibility(it, View.GONE) }

        data.rows.take(MAX_SLOTS).forEachIndexed { index, row ->
            val variant = if (row.isActive) index * 2 + 1 else index * 2
            views.setViewVisibility(slotContainerIds[variant], View.VISIBLE)
            views.setTextViewText(slotNameIds[variant], localized.getString(row.prayer.stringRes))
            views.setTextViewText(slotTimeIds[variant], row.timeText)
        }

        if (data.showCountdown && data.countdown != null) {
            views.setTextViewText(
                R.id.countdown_label,
                "${localized.getString(data.countdown.prayer.stringRes)}: ",
            )
            val base = SystemClock.elapsedRealtime() + (data.countdown.baseMillis - System.currentTimeMillis())
            views.setChronometer(R.id.countdown, base, null, true)
        }

        views.setOnClickPendingIntent(R.id.screen_widget_layout, launchPendingIntent(context))
        return views
    }

    /** Builds the home-screen app-widget RemoteViews, picking the layout from [data]. */
    fun buildScreenWidget(
        context: Context,
        data: WidgetData,
    ): RemoteViews = build(context, screenWidgetLayout(data.adaptiveTheme, data.showCountdown), data)

    fun screenWidgetLayout(
        adaptive: Boolean,
        countdown: Boolean,
    ): Int =
        when {
            countdown && adaptive -> R.layout.screen_widget_countdown_adaptive
            countdown -> R.layout.screen_widget_countdown
            adaptive -> R.layout.screen_widget_adaptive
            else -> R.layout.screen_widget
        }

    /** Font sizes (sp) for the next-prayer widget, scaling with the widget's size: name to countdown. */
    private enum class NextPrayerSize(
        val nameSp: Float,
        val timeSp: Float,
    ) {
        Small(14f, 14f),
        Medium(22f, 20f),
        Large(30f, 28f),
    }

    // Width thresholds (dp) at which the next size bucket kicks in. ~1 launcher cell ≈ 56dp + spacing,
    // so ~110dp ≈ 2 cells wide, ~170dp ≈ 3 cells wide. The smallest key matches the widget's minResize.
    private const val MIN_WIDTH_DP = 40f
    private const val MEDIUM_MIN_WIDTH_DP = 110f
    private const val LARGE_MIN_WIDTH_DP = 170f

    /**
     * Builds the next-prayer widget: the targeted prayer name above a live countdown chronometer.
     * Resizable 1x1..3x3; the font grows with width. On API 31+ the launcher swaps between size buckets
     * as the user resizes (responsive [RemoteViews]); below that a single small rendering is used (a
     * static build can't read the widget's current size pre-31).
     */
    fun buildNextPrayerWidget(
        context: Context,
        data: NextPrayerWidgetData,
    ): RemoteViews {
        val layout = if (data.adaptiveTheme) R.layout.next_prayer_widget_adaptive else R.layout.next_prayer_widget
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return renderNextPrayer(context, data, NextPrayerSize.Small, layout)

        // Keyed on width; the shared min height lets the launcher select purely by how wide the widget is.
        return RemoteViews(
            mapOf(
                SizeF(MIN_WIDTH_DP, MIN_WIDTH_DP) to renderNextPrayer(context, data, NextPrayerSize.Small, layout),
                SizeF(MEDIUM_MIN_WIDTH_DP, MIN_WIDTH_DP) to renderNextPrayer(context, data, NextPrayerSize.Medium, layout),
                SizeF(LARGE_MIN_WIDTH_DP, MIN_WIDTH_DP) to renderNextPrayer(context, data, NextPrayerSize.Large, layout),
            ),
        )
    }

    /**
     * Builds the compact next-prayer layout for the notification widget. Uses notification-specific
     * layouts (transparent background + notification text theme) rather than the home-widget ones, which
     * carry a launcher background and colors. The size is fixed per view: the collapsed view is short and
     * only fits the small font; the expanded view has room for the larger one.
     */
    fun buildNextPrayerNotification(
        context: Context,
        data: NextPrayerWidgetData,
        expanded: Boolean,
    ): RemoteViews {
        val layout = if (data.adaptiveTheme) R.layout.notif_next_prayer_adaptive else R.layout.notif_next_prayer
        return renderNextPrayer(context, data, if (expanded) NextPrayerSize.Medium else NextPrayerSize.Small, layout)
    }

    private fun renderNextPrayer(
        context: Context,
        data: NextPrayerWidgetData,
        size: NextPrayerSize,
        layout: Int,
    ): RemoteViews {
        val localized = context.withAppLocale(data.locale)
        val base = SystemClock.elapsedRealtime() + (data.countdownBaseMillis - System.currentTimeMillis())
        return RemoteViews(context.packageName, layout).apply {
            setTextViewText(R.id.next_prayer_name, localized.getString(data.prayer.stringRes))
            setChronometer(R.id.next_prayer_countdown, base, null, true)
            setTextViewTextSize(R.id.next_prayer_name, TypedValue.COMPLEX_UNIT_SP, size.nameSp)
            setTextViewTextSize(R.id.next_prayer_countdown, TypedValue.COMPLEX_UNIT_SP, size.timeSp)
            setOnClickPendingIntent(R.id.next_prayer_widget_layout, launchPendingIntent(context))
        }
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
}
