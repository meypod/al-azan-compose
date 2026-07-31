package com.github.meypod.al_azan.playback

import android.annotation.SuppressLint
import android.app.ActivityOptions
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaMetadata
import android.media.MediaPlayer
import android.media.VolumeProvider
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.PowerManager
import android.os.SystemClock
import android.provider.Settings
import android.telephony.PhoneStateListener
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.core.net.toUri
import com.github.meypod.al_azan.MainActivity
import com.github.meypod.al_azan.R
import com.github.meypod.al_azan.adhan.AdhanContract
import com.github.meypod.al_azan.alarm.AlarmActivity
import com.github.meypod.al_azan.core.data.locale.withAppLocale
import com.github.meypod.al_azan.core.domain.model.alarm.VibrationMode
import com.github.meypod.al_azan.core.domain.usecase.EnsureNotificationChannelsUseCase
import com.github.meypod.al_azan.core.util.device.CallStateInspector
import com.github.meypod.al_azan.core.util.device.VibrationController
import com.github.meypod.al_azan.playback.PlaybackService.Companion.FADE_IN_MIN_DURATION_MS
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Foreground service that plays the adhan for a prayer. Extras-driven (the firing side resolves all
 * settings), so it builds its foreground notification with the right channel immediately. Holds the
 * MediaPlayer, requests alarm audio focus, pauses/resumes for phone calls, optionally stops on a
 * volume-button press, vibrates per the configured mode, and shows a full-screen alarm (unless
 * suppressed) with Dismiss / Snooze actions.
 */
class PlaybackService :
    Service(),
    MediaPlayer.OnPreparedListener,
    MediaPlayer.OnCompletionListener,
    MediaPlayer.OnErrorListener,
    AudioManager.OnAudioFocusChangeListener {

    companion object {
        private const val TAG = "PlaybackService"

        const val ACTION_PLAY = "com.github.meypod.al_azan.action.ADHAN_PLAY"
        const val ACTION_STOP = "com.github.meypod.al_azan.action.ADHAN_STOP"

        const val EXTRA_TITLE = "title"
        const val EXTRA_BODY = "body"
        const val EXTRA_SOUND_URI = "sound_uri"
        const val EXTRA_CHANNEL_ID = "channel_id"
        const val EXTRA_VOLUME_PERCENT = "volume_percent"
        const val EXTRA_FADE_IN_VOLUME = "fade_in_volume"
        const val EXTRA_USE_MEDIA_USAGE = "use_media_usage"
        const val EXTRA_FULL_SCREEN = "full_screen"
        const val EXTRA_FORCE_LAUNCH_ACTIVITY = "force_launch_activity"
        const val EXTRA_VIBRATION = "vibration"
        const val EXTRA_VOLUME_BUTTON_STOPS = "volume_button_stops"
        const val EXTRA_TIME_LABEL = "time_label"
        const val EXTRA_HEADER = "header"
        const val EXTRA_IS_REMINDER = "is_reminder"
        const val EXTRA_LOOP = "loop"
        const val EXTRA_LANGUAGE_TAGS = "language_tags"

        private const val NOTIFICATION_ID = 0xADA2

        // Streams the hardware volume keys might drive while the alarm plays (device-dependent). Used by
        // normal (mirror) mode, which can't assume which stream the keys hit.
        private val VOLUME_KEY_STREAMS = intArrayOf(
            AudioManager.STREAM_ALARM,
            AudioManager.STREAM_MUSIC,
            AudioManager.STREAM_RING,
            AudioManager.STREAM_NOTIFICATION,
            AudioManager.STREAM_SYSTEM,
        )

        // Arbitrary range for the stop-on-volume MediaSession's remote VolumeProvider. The value never
        // matters (any key press just stops the alarm); it sits mid-range so a press in either direction
        // yields an onAdjust callback.
        private const val VOLUME_PROVIDER_MAX = 100

        // Emitted whenever playback stops (notification "Dismiss", loop cap, call, etc.) so a visible
        // full-screen AlarmActivity can close itself even when the stop didn't originate from its UI.
        private val _stopSignal = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
        val stopSignal: SharedFlow<Unit> = _stopSignal.asSharedFlow()

        // Looping is driven by the caller (EXTRA_LOOP): notification-style reminder tones loop, a full
        // adhan plays once. This cap stops a looped sound from playing forever if nothing dismisses it.
        private const val LOOP_CAP_MS = 5 * 60 * 1000L

        // Gradual volume: the player gain ramps 0 → 1 over this window. The gain scales below the
        // stream level, so the ramp tops out at the target volume (custom or device) either way.
        private const val FADE_IN_MS = 5_000L
        private const val FADE_IN_STEP_MS = 200L

        // Note: the ramp only applies to long sounds. A short tone (a chime, a brief ringtone picked
        // as muezzin) would spend most of its playtime inside the ramp and be barely audible; 4× the
        // ramp keeps at least ~75% of the sound at the target volume. Looping sounds effectively play
        // for minutes, so they always ramp regardless of their single-cycle duration.
        private const val FADE_IN_MIN_DURATION_MS = FADE_IN_MS * 4

        fun start(
            context: Context,
            extras: Bundle,
        ) {
            val intent = Intent(context, PlaybackService::class.java).apply {
                action = ACTION_PLAY
                putExtras(extras)
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            context.startService(Intent(context, PlaybackService::class.java).setAction(ACTION_STOP))
        }
    }

    private var player: MediaPlayer? = null
    private var focusRequest: AudioFocusRequest? = null
    private var telephonyCallback: TelephonyCallback? = null
    private var phoneStateListener: PhoneStateListener? = null
    private var volumeReceiver: BroadcastReceiver? = null
    private var mediaSession: MediaSession? = null
    private var stopOnVolume = false
    private var playbackStream = AudioManager.STREAM_ALARM
    private var wasPlayingBeforeCall = false
    private var volumePercent = -1
    private var shouldLoop = false
    private var fadeInVolume = false

    // Custom volume is absolute: the playback stream's volume is set to volumePercent of its max for the
    // duration and restored on teardown. This is the level saved for that restore; -1 = nothing to restore.
    private var savedStreamVolume = -1

    // Millis timestamp the fade-in ramp started at; also the "ramp active" flag (0 = not ramping).
    private var fadeInStartedAt = 0L
    private val fadeInRunnable = object : Runnable {
        override fun run() {
            val mp = player ?: return
            val fraction = ((SystemClock.elapsedRealtime() - fadeInStartedAt).toFloat() / FADE_IN_MS).coerceIn(0f, 1f)
            runCatching { mp.setVolume(fraction, fraction) }
            if (fraction < 1f) {
                mainHandler.postDelayed(this, FADE_IN_STEP_MS)
            } else {
                fadeInStartedAt = 0L
            }
        }
    }

    // Continuous vibration must outlast a one-shot sound (a single chime, or the silent track): when the
    // audio ends we keep the service — and thus the vibration — alive until the user dismisses or the cap.
    private var continuousVibration = false

    // Captured from the PLAY intent so a natural end (the adhan/reminder finished, not a user dismiss)
    // can leave a quiet dismissible notification behind, mirroring the old app: the prayer still happened.
    // Main-thread confined — every service callback that touches it (onStartCommand, the MediaPlayer /
    // audio-focus / volume / telephony callbacks, the loop-cap Runnable, onDestroy) runs on the main looper.
    private var lingerDetails: LingerDetails? = null

    // onCompletion calls cleanupAndStop, whose stopSelf re-enters via onDestroy; guard so the second
    // pass can't wipe the lingering notification the first one just posted.
    private var stopped = false
    private val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private val loopCapRunnable = Runnable { cleanupAndStop(leaveLingering = true) }

    private val audioManager: AudioManager? by lazy { getSystemService() }
    private val telephonyManager: TelephonyManager? by lazy { getSystemService() }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        if (intent?.action != ACTION_PLAY) {
            cleanupAndStop()
            return START_NOT_STICKY
        }
        // This instance can be reused: a stop (or natural end) calls stopSelf, but a quick re-fire — a
        // dismiss then a new prayer, or an overlapping reminder — can deliver the next PLAY before onDestroy
        // runs. Release any leftovers and clear the stopped latch so this cycle owns clean resources and can
        // itself be dismissed; without the reset, stopped stays true and the new adhan is unstoppable. No-op
        // on a brand-new instance.
        teardownPlayback()
        stopped = false

        val channelId = intent.getStringExtra(EXTRA_CHANNEL_ID)
        val title = intent.getStringExtra(EXTRA_TITLE).orEmpty()
        val body = intent.getStringExtra(EXTRA_BODY)
        val prayerName = intent.getStringExtra(AdhanContract.EXTRA_PRAYER).orEmpty()
        val timeLabel = intent.getStringExtra(EXTRA_TIME_LABEL).orEmpty()
        val header = intent.getStringExtra(EXTRA_HEADER).orEmpty()
        val isReminder = intent.getBooleanExtra(EXTRA_IS_REMINDER, false)
        val fullScreen = intent.getBooleanExtra(EXTRA_FULL_SCREEN, true)
        val forceLaunchActivity = intent.getBooleanExtra(EXTRA_FORCE_LAUNCH_ACTIVITY, false)
        val volumeButtonStops = intent.getBooleanExtra(EXTRA_VOLUME_BUTTON_STOPS, false)
        val useMediaUsage = intent.getBooleanExtra(EXTRA_USE_MEDIA_USAGE, false)
        val languageTags = intent.getStringExtra(EXTRA_LANGUAGE_TAGS).orEmpty()
        playbackStream = if (useMediaUsage) AudioManager.STREAM_MUSIC else AudioManager.STREAM_ALARM
        // ACTION_PLAY always arrives via startForegroundService, so we MUST call startForeground (a
        // bail without it crashes). A blank channel id would itself crash startForeground (the channel
        // must exist on O+), so fall back to a guaranteed channel, then stop below for the bad intent.
        val safeChannelId = channelId?.takeIf { it.isNotEmpty() }
            ?: EnsureNotificationChannelsUseCase.ADHAN_CHANNEL_ID
        startForeground(
            NOTIFICATION_ID,
            buildNotification(
                safeChannelId,
                title,
                body,
                prayerName,
                timeLabel,
                header,
                isReminder,
                volumeButtonStops,
                fullScreen,
                languageTags,
            ),
        )
        if (channelId.isNullOrEmpty()) {
            cleanupAndStop()
            return START_NOT_STICKY
        }

        val uri = intent.getStringExtra(EXTRA_SOUND_URI)?.toUri()
        if (uri == null) {
            cleanupAndStop()
            return START_NOT_STICKY
        }
        volumePercent = intent.getIntExtra(EXTRA_VOLUME_PERCENT, -1)
        fadeInVolume = intent.getBooleanExtra(EXTRA_FADE_IN_VOLUME, false)
        shouldLoop = intent.getBooleanExtra(EXTRA_LOOP, false)
        val vibration = intent.getStringExtra(EXTRA_VIBRATION)?.let { runCatching { VibrationMode.valueOf(it) }.getOrNull() }
            ?: VibrationMode.Off
        continuousVibration = vibration == VibrationMode.Continuous

        if (isCallActive()) {
            cleanupAndStop()
            return START_NOT_STICKY
        }
        // Best-effort: an alarm must sound even when focus is denied. On Android 14+ the system's
        // HardeningEnforcer rejects focus requests from a background-started service (procState
        // SERVICE), so bailing on denial silenced the adhan whenever it fired with the screen locked.
        requestAudioFocus(useMediaUsage)
        registerCallStateListener()
        stopOnVolume = volumeButtonStops
        // Primary stop-on-volume path: an active MediaSession with a remote VolumeProvider receives the
        // hardware volume keys at the audio layer, so a press stops the alarm even with the screen off /
        // locked — the case the VOLUME_CHANGED broadcast never reaches. The broadcast is only a fallback for
        // when the session can't be created, and otherwise drives normal (mirror) mode.
        if (volumeButtonStops) {
            // Mirror the notification's labels onto the session so the prayer/reminder shows up on the lock
            // screen, Android Auto, and Bluetooth head units while it sounds.
            val sessionSubtitle = body?.takeIf { it.isNotEmpty() } ?: timeLabel
            setupVolumeKeyMediaSession(title, sessionSubtitle)
        }
        registerVolumeReceiver()
        VibrationController.vibrate(this, vibration)
        // Directly open the full-screen alarm when either:
        //  - the user forced it (some OEMs ignore the full-screen-intent over the lock screen), or
        //  - notifications are denied, so the OS suppresses the FGS notification AND its full-screen-intent,
        //    leaving no Stop control. In that case we launch even when "keep screen off" (fullScreen=false)
        //    is set — otherwise the adhan would be unstoppable from the UI.
        // When notifications are on and force-launch is off we rely on the FSI/heads-up + notification, so the
        // alarm screen doesn't needlessly take over while the phone is in active use. Starting an activity
        // from here is a background launch: on Android 15+ it needs the "display over other apps" grant,
        // which the force-launch setting is gated on.
        val notificationsEnabled = NotificationManagerCompat.from(applicationContext).areNotificationsEnabled()
        if (forceLaunchActivity || !notificationsEnabled) {
            launchAlarmActivity(
                alarmActivityIntent(prayerName, timeLabel, title, header, isReminder, volumeButtonStops),
                forced = forceLaunchActivity,
            )
        }
        // Now that playback is actually starting, remember what to leave behind if it ends on its own.
        lingerDetails = LingerDetails(title = title, body = body, timeLabel = timeLabel)
        applyAbsoluteStreamVolume()
        startPlayer(uri, useMediaUsage)
        return START_NOT_STICKY
    }

    /**
     * Custom volume: sets the playback stream to [volumePercent] of its max, independent of where the
     * user left the device volume. The pre-existing level is saved once and restored on teardown so the
     * alarm doesn't permanently change the device volume.
     */
    private fun applyAbsoluteStreamVolume() {
        if (volumePercent !in 0..100) return
        val am = audioManager ?: return
        if (savedStreamVolume == -1) {
            savedStreamVolume = runCatching { am.getStreamVolume(playbackStream) }.getOrDefault(-1)
        }
        val max = runCatching { am.getStreamMaxVolume(playbackStream) }.getOrDefault(0)
        if (max <= 0) return
        runCatching { am.setStreamVolume(playbackStream, (volumePercent * max + 50) / 100, 0) }
    }

    private fun restoreStreamVolume() {
        if (savedStreamVolume < 0) return
        runCatching { audioManager?.setStreamVolume(playbackStream, savedStreamVolume, 0) }
        savedStreamVolume = -1
    }

    private fun startPlayer(
        uri: Uri,
        useMediaUsage: Boolean,
    ) {
        player?.release()
        player = MediaPlayer().apply {
            setWakeMode(applicationContext, PowerManager.PARTIAL_WAKE_LOCK)
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(if (useMediaUsage) AudioAttributes.USAGE_MEDIA else AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build(),
            )
            setOnPreparedListener(this@PlaybackService)
            setOnCompletionListener(this@PlaybackService)
            setOnErrorListener(this@PlaybackService)
            val ok = runCatching {
                setDataSource(applicationContext, uri)
                prepareAsync()
            }.isSuccess
            if (!ok) cleanupAndStop()
        }
    }

    override fun onPrepared(mp: MediaPlayer) {
        if (shouldLoop) {
            mp.isLooping = true
            mainHandler.postDelayed(loopCapRunnable, LOOP_CAP_MS)
        }
        if (fadeInVolume && shouldFadeIn(mp)) {
            runCatching { mp.setVolume(0f, 0f) }
            fadeInStartedAt = SystemClock.elapsedRealtime()
            mainHandler.postDelayed(fadeInRunnable, FADE_IN_STEP_MS)
        }
        runCatching { mp.start() }
    }

    /** See [FADE_IN_MIN_DURATION_MS]: long (or looping) sounds only. Unknown duration = assume a full adhan. */
    private fun shouldFadeIn(mp: MediaPlayer): Boolean {
        if (shouldLoop) return true
        val durationMs = runCatching { mp.duration }.getOrDefault(-1)
        return durationMs < 0 || durationMs >= FADE_IN_MIN_DURATION_MS
    }

    override fun onCompletion(mp: MediaPlayer) {
        // A non-looping sound paired with continuous vibration: the audio is done, but the vibration must
        // keep going until dismissed. Release the finished player yet keep the foreground service (and the
        // full-screen alarm) alive, bounded by the same loop cap so it can't buzz forever unattended.
        if (continuousVibration) {
            runCatching { mp.reset() }
            runCatching { mp.release() }
            player = null
            mainHandler.removeCallbacks(loopCapRunnable)
            mainHandler.postDelayed(loopCapRunnable, LOOP_CAP_MS)
        } else {
            cleanupAndStop(leaveLingering = true)
        }
    }

    override fun onError(
        mp: MediaPlayer,
        what: Int,
        extra: Int,
    ): Boolean {
        cleanupAndStop(leaveLingering = true)
        return true
    }

    override fun onAudioFocusChange(focusChange: Int) {
        when {
            focusChange == AudioManager.AUDIOFOCUS_GAIN -> if (wasPlayingBeforeCall) resume()
            focusChange == AudioManager.AUDIOFOCUS_LOSS -> if (!isCallActive()) cleanupAndStop(leaveLingering = true)
            focusChange < 0 -> pauseForInterruption()
        }
    }

    private fun pauseForInterruption() {
        runCatching {
            if (player?.isPlaying == true) {
                player?.pause()
                wasPlayingBeforeCall = true
            }
        }
    }

    private fun resume() {
        wasPlayingBeforeCall = false
        runCatching { player?.start() }
    }

    private fun requestAudioFocus(useMediaUsage: Boolean): Boolean {
        val am = audioManager ?: return false
        val attrs = AudioAttributes.Builder()
            .setUsage(if (useMediaUsage) AudioAttributes.USAGE_MEDIA else AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()
        val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(attrs)
            .setOnAudioFocusChangeListener(this)
            .build()
        focusRequest = request
        return am.requestAudioFocus(request) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    private fun isCallActive(): Boolean = CallStateInspector.isCallActive(this)

    private fun registerCallStateListener() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_PHONE_STATE)
            != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val tm = telephonyManager ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val callback = object : TelephonyCallback(), TelephonyCallback.CallStateListener {
                override fun onCallStateChanged(state: Int) = onCallState(state)
            }
            telephonyCallback = callback
            tm.registerTelephonyCallback(mainExecutor, callback)
        } else {
            @Suppress("DEPRECATION")
            val listener = object : PhoneStateListener() {
                @Deprecated("Deprecated in Java")
                override fun onCallStateChanged(
                    state: Int,
                    phoneNumber: String?,
                ) = onCallState(state)
            }
            phoneStateListener = listener
            @Suppress("DEPRECATION")
            tm.listen(listener, PhoneStateListener.LISTEN_CALL_STATE)
        }
    }

    private fun onCallState(state: Int) {
        if (state == TelephonyManager.CALL_STATE_IDLE) {
            if (wasPlayingBeforeCall) resume()
        } else {
            pauseForInterruption()
        }
    }

    /**
     * Drives normal (mirror) mode, and is a fallback stop path only when the MediaSession (the reliable
     * stop-on-volume mechanism) couldn't be created — see the null-session check in onReceive. Fires when a
     * stream volume actually changes.
     */
    private fun registerVolumeReceiver() {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(
                c: Context?,
                i: Intent?,
            ) {
                if (i == null) return
                if (stopOnVolume) {
                    // The MediaSession is the reliable stop path and absorbs real key presses (so this
                    // broadcast rarely reflects one). Only fall back to it when the session couldn't be set
                    // up — otherwise an unrelated stream change could falsely cut the adhan short.
                    if (mediaSession == null) cleanupAndStop()
                } else {
                    mirrorVolumeToPlayer(i) // normal mode: follow the user's volume change live
                }
            }
        }
        volumeReceiver = receiver
        ContextCompat.registerReceiver(
            this,
            receiver,
            IntentFilter("android.media.VOLUME_CHANGED_ACTION"),
            // EXPORTED: some OEMs won't deliver the system VOLUME_CHANGED broadcast to a NOT_EXPORTED
            // runtime receiver while the screen is off. VOLUME_CHANGED_ACTION is a protected broadcast
            // (only the system can send it), so exporting doesn't let other apps spoof a stop.
            ContextCompat.RECEIVER_EXPORTED,
        )
    }

    private fun unregisterVolumeReceiver() {
        volumeReceiver?.let { runCatching { unregisterReceiver(it) } }
        volumeReceiver = null
    }

    /**
     * Normal mode: map a changed stream's level onto the MediaPlayer volume so volume keys adjust the
     * adhan even with no full-screen activity (the keys may drive a stream other than the playback one).
     * The playback stream is skipped — its own volume already scales the output, so mirroring it too
     * would double-attenuate.
     */
    private fun mirrorVolumeToPlayer(intent: Intent) {
        // While the fade-in ramp owns the player gain, a mirrored key press would fight it; the ramp
        // wins and normal mirroring resumes once it completes.
        if (fadeInStartedAt != 0L) return
        val stream = intent.getIntExtra("android.media.EXTRA_VOLUME_STREAM_TYPE", -1)
        if (stream == playbackStream || stream !in VOLUME_KEY_STREAMS) return
        val value = intent.getIntExtra("android.media.EXTRA_VOLUME_STREAM_VALUE", Int.MIN_VALUE)
        if (value == Int.MIN_VALUE) return
        val max = runCatching { audioManager?.getStreamMaxVolume(stream) ?: 0 }.getOrDefault(0)
        if (max <= 0) return
        val ratio = (value.toFloat() / max).coerceIn(0f, 1f)
        runCatching { player?.setVolume(ratio, ratio) }
    }

    /**
     * The reliable stop-on-volume path: an active [MediaSession] with a remote [VolumeProvider]. While a
     * session is active and playing, the framework routes the hardware volume keys to its VolumeProvider at
     * the audio layer — before they reach any window or change a stream — so a press dismisses the alarm
     * even with the screen off or locked, the case the VOLUME_CHANGED broadcast never reaches. Because the
     * provider owns the volume, an edge press still fires an onAdjust callback, so no volume nudging is
     * needed. The audio itself is played by the MediaPlayer, not this session; the session exists to capture
     * the keys and to surface the prayer/reminder as now-playing metadata on the lock screen / Auto / BT.
     */
    private fun setupVolumeKeyMediaSession(
        title: String,
        subtitle: String,
    ) {
        val session = runCatching { MediaSession(this, "adhan-volume") }.getOrNull() ?: return
        val provider = object : VolumeProvider(VOLUME_CONTROL_RELATIVE, VOLUME_PROVIDER_MAX, VOLUME_PROVIDER_MAX / 2) {
            override fun onAdjustVolume(direction: Int) {
                if (direction != 0) cleanupAndStop() // any up/down press dismisses the alarm
            }

            override fun onSetVolumeTo(volume: Int) = cleanupAndStop()
        }
        runCatching {
            // A callback (with an explicit main-thread handler) must exist for the platform MediaSession to
            // have a message handler to dispatch incoming volume adjustments onto — without it the framework
            // routes the key to our session but the VolumeProvider callback is silently dropped.
            session.setCallback(
                object : MediaSession.Callback() {
                    override fun onStop() = cleanupAndStop()
                },
                mainHandler,
            )
            session.setMetadata(
                MediaMetadata.Builder()
                    .putString(MediaMetadata.METADATA_KEY_TITLE, title)
                    .putString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE, title)
                    .putString(MediaMetadata.METADATA_KEY_ARTIST, subtitle)
                    .putString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE, subtitle)
                    .build(),
            )
            session.setPlaybackToRemote(provider)
            // A PLAYING state makes this the active session the framework routes volume keys to.
            session.setPlaybackState(
                PlaybackState.Builder()
                    .setState(PlaybackState.STATE_PLAYING, PlaybackState.PLAYBACK_POSITION_UNKNOWN, 1f)
                    .build(),
            )
            session.isActive = true
        }
        mediaSession = session
    }

    private fun releaseVolumeKeyMediaSession() {
        mediaSession?.let {
            runCatching { it.isActive = false }
            runCatching { it.release() }
        }
        mediaSession = null
    }

    @SuppressLint("FullScreenIntentPolicy")
    private fun buildNotification(
        channelId: String,
        title: String,
        body: String?,
        prayerName: String,
        timeLabel: String,
        header: String,
        isReminder: Boolean,
        volumeButtonStops: Boolean,
        fullScreen: Boolean,
        languageTags: String,
    ): android.app.Notification {
        val alarmActivityIntent =
            alarmActivityIntent(prayerName, timeLabel, title, header, isReminder, volumeButtonStops)
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            alarmActivityIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val stopIntent = PendingIntent.getService(
            this,
            0,
            Intent(this, PlaybackService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.monochrome_notif)
            .setContentTitle(title)
            .setSubText(timeLabel)
            .setContentText(body)
            .setShowWhen(false)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(contentIntent)
            // Swiping the notification away (where the OS allows it) stops playback too.
            .setDeleteIntent(stopIntent)
            // Service context doesn't carry the per-app locale on pre-API 33; resolve in the language
            // the launcher passed along (same source as the title/body the handler resolved).
            .addAction(R.drawable.outline_stop_24, withAppLocale(languageTags).getString(R.string.dismiss), stopIntent)
            .setOnlyAlertOnce(true)
        // Only attach the full-screen intent when we're actually allowed to use it (Android 14+ gates
        // it behind USE_FULL_SCREEN_INTENT). Posting one without the grant is wasted and degrades the
        // notification on some OEMs. Playback is independent of this — the adhan sounds either way.
        if (fullScreen && NotificationManagerCompat.from(applicationContext).canUseFullScreenIntent()) {
            builder.setFullScreenIntent(contentIntent, true)
        }
        return builder.build()
    }

    /**
     * Starts the full-screen alarm from the service, i.e. from the background.
     *
     * A background-activity-launch denial does NOT throw — the framework drops the start and only logs
     * it under its own tag — so catching alone would report success on a launch that never happened.
     * We log the overlay-permission state next to every attempt instead: it's the one non-expiring BAL
     * exemption, so its absence is the actionable explanation when users report "nothing opened".
     */
    private fun launchAlarmActivity(
        intent: Intent,
        forced: Boolean,
    ) {
        val canDrawOverlays = Settings.canDrawOverlays(this)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                sendAlarmActivityPendingIntent(intent)
            } else {
                startActivity(intent)
            }
            Log.i(TAG, "Alarm activity start requested (forced=$forced, canDrawOverlays=$canDrawOverlays)")
        } catch (e: Exception) { // includes PendingIntent.CanceledException, which isn't a RuntimeException
            Log.e(TAG, "Alarm activity start threw (forced=$forced, canDrawOverlays=$canDrawOverlays)", e)
            return
        }
        if (!canDrawOverlays) {
            Log.w(
                TAG,
                "No 'display over other apps' permission — the system may have blocked this start. " +
                    "Check logcat for a background activity launch denial.",
            )
        }
    }

    /**
     * Sends the alarm screen as a PendingIntent with the background-activity-start mode opted in.
     *
     * From API 34 the sender must declare that intent explicitly or the start isn't even considered a
     * background launch. It is only an opt-in, not a grant: the system still requires a real exemption
     * (the "display over other apps" permission), so this does not replace it.
     *
     * The PendingIntent matches the notification's content intent — same component and extras — so both
     * routes lead to the same activity instance.
     */
    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private fun sendAlarmActivityPendingIntent(intent: Intent) {
        val options = ActivityOptions.makeBasic()
            .setPendingIntentBackgroundActivityStartMode(backgroundActivityStartMode())
        PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        ).send(this, 0, null, null, null, null, options.toBundle())
    }

    /**
     * API 36 split the old blanket mode in two. We need ALLOW_ALWAYS: the alarm fires while the app is in
     * the background, which is exactly what ALLOW_IF_VISIBLE excludes. Both still only opt in — the system
     * keeps requiring a real exemption.
     */
    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    @Suppress("DEPRECATION") // MODE_BACKGROUND_ACTIVITY_START_ALLOWED: the only option below API 36.
    private fun backgroundActivityStartMode(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
            ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOW_ALWAYS
        } else {
            ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
        }

    private fun alarmActivityIntent(
        prayerName: String,
        timeLabel: String,
        title: String,
        header: String,
        isReminder: Boolean,
        volumeButtonStops: Boolean,
    ): Intent =
        Intent(this, AlarmActivity::class.java).apply {
            putExtra(AdhanContract.EXTRA_PRAYER, prayerName)
            putExtra(EXTRA_TIME_LABEL, timeLabel)
            putExtra(EXTRA_TITLE, title)
            putExtra(EXTRA_HEADER, header)
            putExtra(EXTRA_IS_REMINDER, isReminder)
            putExtra(EXTRA_VOLUME_BUTTON_STOPS, volumeButtonStops)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

    /**
     * Releases every playback resource (player, audio focus, listeners, volume MediaSession, vibration) and
     * cancels the loop cap. Idempotent and null-safe, so it is reused both to stop a cycle ([cleanupAndStop])
     * and to wipe a prior cycle's leftovers when this instance is reused for a new PLAY. Does NOT touch the
     * foreground notification, the stopped latch, or stopSelf — those belong to [cleanupAndStop].
     */
    private fun teardownPlayback() {
        mainHandler.removeCallbacks(loopCapRunnable)
        mainHandler.removeCallbacks(fadeInRunnable)
        fadeInStartedAt = 0L
        player?.let { mp ->
            runCatching { mp.reset() }
            mp.release()
        }
        player = null
        wasPlayingBeforeCall = false
        // Before abandoning focus so the restored level isn't heard by whatever resumes after us.
        restoreStreamVolume()
        abandonAudioFocus()
        unregisterCallStateListener()
        unregisterVolumeReceiver()
        releaseVolumeKeyMediaSession()
        VibrationController.stop(this)
    }

    /**
     * Stops playback and tears the service down. When [leaveLingering] is true (the adhan/reminder ended
     * on its own — playback finished, looped past the cap, errored, or lost audio focus — rather than the
     * user dismissing it) the foreground notification is detached and replaced with a quiet, dismissible
     * one so the passed prayer/reminder still leaves a trace, matching the old app. A user dismiss
     * (notification Stop, swipe, volume press, the firing handlers) removes it outright.
     */
    // The notify below is guarded by areNotificationsEnabled(), which lint doesn't accept as a
    // POST_NOTIFICATIONS check. It's also wrapped: the trace is cosmetic, and this runs during teardown,
    // so nothing it can throw (on any OEM build) should be able to take the service down with it.
    @SuppressLint("MissingPermission")
    private fun cleanupAndStop(leaveLingering: Boolean = false) {
        if (stopped) return
        stopped = true
        teardownPlayback()
        _stopSignal.tryEmit(Unit)
        // Notifications off (permission denied, or the user disabled them) means the trace can never be
        // shown, so don't build one either.
        val notificationsEnabled = NotificationManagerCompat.from(applicationContext).areNotificationsEnabled()
        val lingering = if (leaveLingering && notificationsEnabled) buildLingeringNotification() else null
        // Always remove the foreground notification first: the trace lives on a different (silent)
        // channel, which a same-id re-post could never switch to.
        stopForeground(STOP_FOREGROUND_REMOVE)
        if (lingering != null) {
            // A fresh id per trace so successive passed prayers/reminders stack instead of replacing one
            // another. Posted exactly once per playback (guarded above), and the clock only moves forward,
            // so this stays unique even across a process restart while an earlier trace is still showing.
            runCatching {
                NotificationManagerCompat.from(this).notify(System.currentTimeMillis().toInt(), lingering)
            }.onFailure { Log.w(TAG, "Could not post the lingering notification", it) }
        }
        stopSelf()
    }

    /**
     * The notification left in place after a natural end: same prayer/reminder title and time, but quiet
     * and dismissible (no Stop action, no full-screen, not ongoing) and on the silent "missed" channel.
     * Tapping it opens the app. Returns null if the PLAY intent never reached the point of capturing its
     * details (a misfire/early bail).
     */
    private fun buildLingeringNotification(): android.app.Notification? {
        val details = lingerDetails ?: return null
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return NotificationCompat.Builder(this, EnsureNotificationChannelsUseCase.MISSED_CHANNEL_ID)
            .setSmallIcon(R.drawable.monochrome_notif)
            .setContentTitle(details.title)
            .setSubText(details.timeLabel)
            .setContentText(details.body)
            .setShowWhen(false)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(false)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(contentIntent)
            .build()
    }

    /** What [buildLingeringNotification] needs from the PLAY intent to leave a trace after a natural end. */
    private data class LingerDetails(
        val title: String,
        val body: String?,
        val timeLabel: String,
    )

    private fun abandonAudioFocus() {
        focusRequest?.let { audioManager?.abandonAudioFocusRequest(it) }
        focusRequest = null
    }

    private fun unregisterCallStateListener() {
        val tm = telephonyManager ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            telephonyCallback?.let { tm.unregisterTelephonyCallback(it) }
            telephonyCallback = null
        } else {
            @Suppress("DEPRECATION")
            phoneStateListener?.let { tm.listen(it, PhoneStateListener.LISTEN_NONE) }
            phoneStateListener = null
        }
    }

    override fun onDestroy() {
        cleanupAndStop()
        super.onDestroy()
    }
}
