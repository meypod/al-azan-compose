package com.github.meypod.al_azan.di

import android.util.Log
import com.github.meypod.al_azan.R
import com.github.meypod.al_azan.core.domain.model.TextResource
import com.github.meypod.al_azan.core.domain.model.notification.AndroidNotificationCategory
import com.github.meypod.al_azan.core.domain.model.notification.AndroidNotificationConfig
import com.github.meypod.al_azan.core.domain.model.notification.NotificationConfig
import com.github.meypod.al_azan.core.domain.model.notification.NotificationPressAction
import com.github.meypod.al_azan.core.domain.repository.NotificationRepository
import com.github.meypod.al_azan.core.domain.repository.SettingsRepository
import com.github.meypod.al_azan.core.domain.usecase.EnsureNotificationChannelsUseCase
import com.github.meypod.al_azan.core.presentation.navigation.Route
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi

/**
 * Delivers the notice that [DiyanetParamsMigrationRunner] flagged: the user's Diyanet times moved and
 * their minute adjustments were cleared, so they should look at their times and pick between the
 * Turkey and Europe variants.
 *
 * The pending flag is only cleared once the notification has actually gone out. A migration can well
 * land on a start where notifications are not permitted — the user has not opened the app since
 * updating, or denied the permission — and a notice about times silently changing is exactly the one
 * that must not be dropped.
 */
@Singleton
class DiyanetChangeNoticePoster
@Inject
constructor(
    private val settingsRepository: SettingsRepository,
    private val notificationRepository: NotificationRepository,
    private val ensureNotificationChannels: EnsureNotificationChannelsUseCase,
) {
    companion object {
        const val NOTIFICATION_ID = "diyanet_change_notice"
    }

    suspend fun postIfPending() {
        if (!settingsRepository.fetch().diyanetChangeNoticePending) return
        if (!notificationRepository.isNotificationsAllowed()) return

        ensureNotificationChannels()
        notificationRepository.notify(
            NotificationConfig(
                id = NOTIFICATION_ID,
                title = TextResource.StringResId(R.string.diyanet_change_notice_title),
                body = TextResource.StringResId(R.string.diyanet_change_notice_body),
                android = AndroidNotificationConfig(
                    channelId = EnsureNotificationChannelsUseCase.IMPORTANT_NOTICE_CHANNEL_ID,
                    category = AndroidNotificationCategory.CATEGORY_STATUS,
                    autoCancel = true,
                    pressAction = NotificationPressAction.Route(Route.Main.Settings.Calculations),
                ),
            ),
        )
        settingsRepository.update { it.copy(diyanetChangeNoticePending = false) }
    }
}

/**
 * Applies the Diyanet fix to settings that predate it, then delivers the notice it raises.
 *
 * Off the main thread, unlike the legacy v2 migration: the sync initializers react to the corrected
 * settings when they land, so the only cost of not blocking startup is that a first schedule may use the
 * old times for a moment before being redone.
 */
@Singleton
class DiyanetFixInitializer @Inject constructor(
    private val diyanetParamsMigrationRunner: DiyanetParamsMigrationRunner,
    private val diyanetChangeNoticePoster: DiyanetChangeNoticePoster,
) {
    private companion object {
        const val TAG = "DiyanetFixInitializer"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @OptIn(ExperimentalAtomicApi::class)
    private val started = AtomicBoolean(false)

    @OptIn(ExperimentalAtomicApi::class)
    fun start() {
        if (!started.compareAndSet(expectedValue = false, newValue = true)) return
        scope.launch {
            runCatching { diyanetParamsMigrationRunner.run() }
                .onFailure { Log.e(TAG, "Diyanet fix failed. Will retry on next launch.", it) }
            diyanetChangeNoticePoster.postIfPending()
        }
    }
}
