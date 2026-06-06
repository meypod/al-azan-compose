package com.github.meypod.al_azan.core.data.repository

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.PendingIntentCompat
import androidx.core.net.toUri
import com.github.meypod.al_azan.R
import com.github.meypod.al_azan.core.data.mapping.asString
import com.github.meypod.al_azan.core.domain.model.navigation.DeepLinkableRoute
import com.github.meypod.al_azan.core.domain.model.notification.NotificationConfig
import com.github.meypod.al_azan.core.domain.model.notification.NotificationPressAction
import com.github.meypod.al_azan.core.domain.model.notification.toNotificationCompat
import com.github.meypod.al_azan.core.domain.repository.NotificationRepository
import com.github.meypod.al_azan.core.domain.util.randomString
import com.github.meypod.al_azan.core.presentation.navigation.Route
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class NotificationRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
    val activityClassForIntents: Class<*>,
) : NotificationRepository {

    companion object {
        const val TAG = "NotificationRepositoryImpl"
    }

    private val notifManagerCompact by lazy { NotificationManagerCompat.from(context) }

    @SuppressLint("MissingPermission")
    override suspend fun notify(payload: NotificationConfig) {
        if (!isNotificationsAllowed()) return
        val notificationId = payload.id.let { if (it.isNullOrBlank()) randomString(8) else it }.hashCode()

        val channelId =
            payload.android?.channelId
                ?: throw IllegalArgumentException("android notification config should define channelId")

        val hasCustomView =
            payload.android.customContentView != null || payload.android.customBigContentView != null

        val builder =
            NotificationCompat
                .Builder(context, channelId)
                .setSmallIcon(R.drawable.monochrome_notif)

        // Custom-view notifications (e.g. the prayer-times widget) render their own content and must
        // stay silent; only standard notifications get the default alerting behavior.
        if (hasCustomView) {
            builder.setStyle(NotificationCompat.DecoratedCustomViewStyle())
            payload.android.customContentView?.let { builder.setCustomContentView(it) }
            payload.android.customBigContentView?.let { builder.setCustomBigContentView(it) }
        } else {
            builder.setDefaults(NotificationCompat.DEFAULT_ALL)
        }

        payload.title?.let { builder.setContentTitle(it.asString(context)) }
        payload.subtitle?.let { builder.setSubText(it.asString(context)) }
        payload.body?.let { builder.setContentText(it.asString(context)) }
        payload.badgeCount?.let { builder.setNumber(it) }
        builder.setOngoing(payload.android.ongoing)
        builder.setCategory(payload.android.category.toNotificationCompat())
        builder.setOnlyAlertOnce(payload.android.onlyAlertOnce)
        builder.setShowWhen(payload.android.showTimestamp)
        payload.android.timestamp?.let { builder.setWhen(it) }
        builder.setGroup(payload.android.group)
        builder.setVisibility(payload.android.visibility.toNotificationCompat())
        builder.setAutoCancel(payload.android.autoCancel)
        payload.android.sortKey?.let { builder.setSortKey(it) }

        if (payload.android.pressAction == null) {
            builder.setContentIntent(null)
        } else {
            when (val action = payload.android.pressAction) {
                is NotificationPressAction.Route ->
                    builder.setContentIntent(
                        createNavDeepLinkPendingIntent(action.route),
                    )

                is NotificationPressAction.Default ->
                    builder.setContentIntent(
                        createNavDeepLinkPendingIntent(Route.Main.Home),
                    )

                is NotificationPressAction.Broadcast -> createBroadcastPendingIntent(action)

                null -> {
                    builder.setContentIntent(null)
                }
            }
        }

        payload.android.actions?.let { actionsList ->
            if (actionsList.isNotEmpty()) {
                actionsList.forEach { action ->
                    val pendingIntent: PendingIntent? = when (val pressAction = action.pressAction) {
                        is NotificationPressAction.Route -> createNavDeepLinkPendingIntent(pressAction.route)
                        is NotificationPressAction.Broadcast -> createBroadcastPendingIntent(pressAction)
                        else -> createNavDeepLinkPendingIntent(Route.Main.Home)
                    }
                    pendingIntent?.let {
                        builder.addAction(NotificationCompat.Action(null, action.title.asString(context), it))
                    }
                }
            }
        }

        try {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
        } catch (e: Exception) {
            Log.e(TAG, "Failed to push notification with ID $notificationId", e)
        }
    }

    override suspend fun cancelNotification(notificationId: String) {
        notifManagerCompact.cancel(notificationId.hashCode())
    }

    override suspend fun isNotificationsAllowed(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

    fun createNavDeepLinkPendingIntent(route: DeepLinkableRoute): PendingIntent {
        val link = route.toUriString()
        val intent =
            Intent(
                Intent.ACTION_VIEW,
                link.toUri(),
                context,
                activityClassForIntents,
            ).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }

        val pendingIntentFlags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

        val pendingIntent =
            PendingIntent.getActivity(
                context,
                link.hashCode(),
                intent,
                pendingIntentFlags,
            )

        return pendingIntent
    }

    fun createBroadcastPendingIntent(action: NotificationPressAction.Broadcast) =
        PendingIntentCompat.getBroadcast(
            context,
            action.requestCode,
            Intent(action.action),
            PendingIntent.FLAG_UPDATE_CURRENT,
            false,
        )
}
