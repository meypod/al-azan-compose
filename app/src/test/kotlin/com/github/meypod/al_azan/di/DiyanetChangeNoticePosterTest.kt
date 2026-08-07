package com.github.meypod.al_azan.di

import com.github.meypod.al_azan.R
import com.github.meypod.al_azan.core.domain.model.TextResource
import com.github.meypod.al_azan.core.domain.model.notification.NotificationChannelConfig
import com.github.meypod.al_azan.core.domain.model.notification.NotificationConfig
import com.github.meypod.al_azan.core.domain.model.notification.NotificationPressAction
import com.github.meypod.al_azan.core.domain.model.settings.Settings
import com.github.meypod.al_azan.core.domain.repository.NotificationChannelManager
import com.github.meypod.al_azan.core.domain.repository.NotificationRepository
import com.github.meypod.al_azan.core.domain.repository.SettingsRepository
import com.github.meypod.al_azan.core.domain.usecase.EnsureNotificationChannelsUseCase
import com.github.meypod.al_azan.core.presentation.navigation.Route
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DiyanetChangeNoticePosterTest {
    private class FakeSettingsRepository(pending: Boolean) : SettingsRepository {
        private val state = MutableStateFlow(Settings(selectedLocale = "en", diyanetChangeNoticePending = pending))

        override val data: Flow<Settings> get() = state

        override suspend fun fetch(): Settings = state.value

        override suspend fun update(transform: (t: Settings) -> Settings) {
            state.value = transform(state.value)
        }
    }

    private class FakeNotificationRepository(
        private val allowed: Boolean,
    ) : NotificationRepository {
        val posted = mutableListOf<NotificationConfig>()

        override suspend fun notify(payload: NotificationConfig) {
            posted += payload
        }

        override suspend fun cancelNotification(notificationId: String) = Unit

        override suspend fun isNotificationsAllowed(): Boolean = allowed
    }

    private class RecordingChannelManager : NotificationChannelManager {
        val ensured = mutableListOf<String>()

        override fun ensureChannelsExist(configs: List<NotificationChannelConfig>) {
            ensured += configs.map { it.id }
        }

        override fun deleteChannel(channelId: String) = Unit
    }

    @Test
    fun `pending notice is posted on its own channel and the flag is cleared`() = runTest {
        val settingsRepository = FakeSettingsRepository(pending = true)
        val notificationRepository = FakeNotificationRepository(allowed = true)
        val channelManager = RecordingChannelManager()

        DiyanetChangeNoticePoster(
            settingsRepository,
            notificationRepository,
            EnsureNotificationChannelsUseCase(channelManager),
        ).postIfPending()

        assertEquals(1, notificationRepository.posted.size)
        val posted = notificationRepository.posted.single()
        assertEquals(DiyanetChangeNoticePoster.NOTIFICATION_ID, posted.id)
        assertEquals(TextResource.StringResId(R.string.diyanet_change_notice_body), posted.body)
        assertEquals(EnsureNotificationChannelsUseCase.IMPORTANT_NOTICE_CHANNEL_ID, posted.android?.channelId)
        assertEquals(
            NotificationPressAction.Route(Route.Main.Settings.Calculations),
            posted.android?.pressAction,
        )
        // the channel must exist before the notification lands on it
        assertTrue(channelManager.ensured.contains(EnsureNotificationChannelsUseCase.IMPORTANT_NOTICE_CHANNEL_ID))
        assertFalse(settingsRepository.fetch().diyanetChangeNoticePending)
    }

    @Test
    fun `the notice stays pending while notifications are not permitted`() = runTest {
        val settingsRepository = FakeSettingsRepository(pending = true)
        val notificationRepository = FakeNotificationRepository(allowed = false)

        DiyanetChangeNoticePoster(
            settingsRepository,
            notificationRepository,
            EnsureNotificationChannelsUseCase(RecordingChannelManager()),
        ).postIfPending()

        assertTrue(notificationRepository.posted.isEmpty())
        assertTrue(settingsRepository.fetch().diyanetChangeNoticePending)
    }

    @Test
    fun `nothing is posted when no notice is pending`() = runTest {
        val notificationRepository = FakeNotificationRepository(allowed = true)

        DiyanetChangeNoticePoster(
            FakeSettingsRepository(pending = false),
            notificationRepository,
            EnsureNotificationChannelsUseCase(RecordingChannelManager()),
        ).postIfPending()

        assertTrue(notificationRepository.posted.isEmpty())
    }
}
