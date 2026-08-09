package com.github.meypod.al_azan.playback

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.os.Bundle
import android.os.Looper
import com.github.meypod.al_azan.adhan.AdhanContract
import com.github.meypod.al_azan.core.domain.usecase.EnsureNotificationChannelsUseCase
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ServiceController
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowMediaPlayer
import org.robolectric.shadows.util.DataSource

/**
 * The quiet trace left behind when an alarm ends without the user acting on it.
 *
 * The rule that needs guarding is *when* one is left: exactly once per alarm that was lost, and never for
 * one the user dismissed. Getting it wrong is either a silently vanished prayer or a shade full of
 * duplicates, and both only show up when the service instance is reused — which is the case a pure unit
 * test can't reach.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class PlaybackServiceLingerTest {

    private val context: Application = RuntimeEnvironment.getApplication()
    private lateinit var controller: ServiceController<PlaybackService>

    private val soundUri = "content://media/internal/audio/media/1"

    private companion object {
        const val MEDIA_DURATION_MS = 60_000L
    }

    @Before
    fun setUp() {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                EnsureNotificationChannelsUseCase.ADHAN_CHANNEL_ID,
                "Adhan",
                NotificationManager.IMPORTANCE_HIGH,
            ),
        )
        manager.createNotificationChannel(
            NotificationChannel(
                EnsureNotificationChannelsUseCase.MISSED_CHANNEL_ID,
                "Missed",
                NotificationManager.IMPORTANCE_DEFAULT,
            ),
        )
        // Without a registered source the player fails to prepare and the service stops itself, so no
        // playback would ever be "live" to replace.
        ShadowMediaPlayer.addMediaInfo(
            DataSource.toDataSource(context, android.net.Uri.parse(soundUri)),
            ShadowMediaPlayer.MediaInfo(MEDIA_DURATION_MS.toInt(), 0),
        )
        controller = Robolectric.buildService(PlaybackService::class.java).create()
    }

    @After
    fun tearDown() {
        PlaybackService.activeAlarm.value?.let { PlaybackService.markAlarmHandled(it.id) }
        ShadowMediaPlayer.resetStaticState()
    }

    private fun playIntent(title: String) =
        Intent(context, PlaybackService::class.java).apply {
            action = PlaybackService.ACTION_PLAY
            putExtras(
                Bundle().apply {
                    putString(PlaybackService.EXTRA_TITLE, title)
                    putString(PlaybackService.EXTRA_BODY, "body")
                    putString(PlaybackService.EXTRA_TIME_LABEL, "05:30")
                    putString(PlaybackService.EXTRA_CHANNEL_ID, EnsureNotificationChannelsUseCase.ADHAN_CHANNEL_ID)
                    putString(PlaybackService.EXTRA_SOUND_URI, soundUri)
                    putString(AdhanContract.EXTRA_PRAYER, "Fajr")
                },
            )
        }

    /** Delivered through the controller so the service goes through Robolectric's own lifecycle. */
    private fun play(title: String) {
        controller.withIntent(playIntent(title)).startCommand(0, 0)
    }

    private fun stop() {
        controller
            .withIntent(Intent(context, PlaybackService::class.java).setAction(PlaybackService.ACTION_STOP))
            .startCommand(0, 0)
    }

    /** Lets the shadow player prepare, start, and run to the end of its media, firing onCompletion. */
    private fun playToCompletion(title: String) {
        play(title)
        shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(MEDIA_DURATION_MS + 1_000L))
    }

    /** Bodies of the traces sitting in the shade, in the order they were posted. */
    private fun traceBodies(): List<String> =
        shadowOf(context.getSystemService(NotificationManager::class.java))
            .activeNotifications
            .filter { it.notification.channelId == EnsureNotificationChannelsUseCase.MISSED_CHANNEL_ID }
            .map { it.notification.extras.getString("android.text").orEmpty() }

    /** Titles of the traces sitting in the shade, in the order they were posted. */
    private fun traceTitles(): List<String> =
        shadowOf(context.getSystemService(NotificationManager::class.java))
            .activeNotifications
            .filter { it.notification.channelId == EnsureNotificationChannelsUseCase.MISSED_CHANNEL_ID }
            .map { it.notification.extras.getString("android.title").orEmpty() }

    @Test
    fun `a first alarm leaves no trace while it is still playing`() {
        play("Fajr")

        assertEquals(emptyList<String>(), traceTitles())
    }

    @Test
    fun `an alarm replaced by the next one leaves its trace`() {
        play("Fajr")
        play("Dhuhr")

        assertEquals(listOf("Fajr"), traceTitles())
    }

    /** One trace per lost alarm: the second replacement must not re-post the first alarm's. */
    @Test
    fun `each replaced alarm leaves exactly one trace`() {
        play("Fajr")
        play("Dhuhr")
        play("Asr")

        assertEquals(listOf("Fajr", "Dhuhr"), traceTitles())
    }

    /** The user acted on it, so there is nothing to catch up on. */
    @Test
    fun `a dismissed alarm leaves no trace, and neither does the next alarm starting`() {
        play("Fajr")
        stop()
        play("Dhuhr")

        assertEquals(emptyList<String>(), traceTitles())
    }

    /** Ending on its own already leaves a trace; the next alarm must not add a second one for it. */
    @Test
    fun `an alarm that ended on its own is not traced twice`() {
        playToCompletion("Fajr")

        assertEquals(listOf("Fajr"), traceTitles())

        play("Dhuhr")

        assertEquals(listOf("Fajr"), traceTitles())
    }

    /**
     * Never sounded because the user was on a call — lost through no fault of theirs.
     *
     * Only the race window reaches here: the firing handlers check for a call first and post their own
     * notice instead of starting playback, so this backstop is the one path that would otherwise be silent.
     */
    @Test
    fun `an alarm arriving during a call leaves a trace`() {
        shadowOf(context).grantPermissions(android.Manifest.permission.READ_PHONE_STATE)
        shadowOf(context.getSystemService(android.telecom.TelecomManager::class.java)).setIsInCall(true)

        play("Fajr")

        assertEquals(listOf("Fajr"), traceTitles())
    }

    /** The same wording the firing handlers use, so the race doesn't read differently from the common path. */
    @Test
    fun `the call trace says it passed during a call`() {
        shadowOf(context).grantPermissions(android.Manifest.permission.READ_PHONE_STATE)
        shadowOf(context.getSystemService(android.telecom.TelecomManager::class.java)).setIsInCall(true)

        play("Fajr")

        assertEquals(
            listOf(context.getString(com.github.meypod.al_azan.R.string.missed_during_call_body, "05:30")),
            traceBodies(),
        )
    }

    /**
     * A sound that can't be opened — a deleted custom file, a revoked uri permission — is the alarm most
     * likely to have been genuinely missed, so it has to leave a record too.
     */
    @Test
    fun `an alarm whose sound cannot be opened leaves a trace`() {
        controller
            .withIntent(
                playIntent("Fajr").apply {
                    putExtra(PlaybackService.EXTRA_SOUND_URI, "content://media/external/audio/media/999999")
                },
            )
            .startCommand(0, 0)

        assertEquals(listOf("Fajr"), traceTitles())
    }
}
