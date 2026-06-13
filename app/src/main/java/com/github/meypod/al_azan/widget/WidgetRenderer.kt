package com.github.meypod.al_azan.widget

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.view.View
import android.widget.RemoteViews
import com.github.meypod.al_azan.MainActivity
import com.github.meypod.al_azan.R
import com.github.meypod.al_azan.core.data.locale.withAppLocale
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
