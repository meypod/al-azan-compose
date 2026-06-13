package com.github.meypod.al_azan.playback

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.PowerManager
import android.telephony.PhoneStateListener
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.core.net.toUri
import com.github.meypod.al_azan.R
import com.github.meypod.al_azan.alarm.AlarmActivity
import com.github.meypod.al_azan.core.data.locale.withAppLocale
import com.github.meypod.al_azan.core.domain.model.alarm.VibrationMode
import com.github.meypod.al_azan.core.domain.usecase.EnsureNotificationChannelsUseCase
import com.github.meypod.al_azan.core.util.device.VibrationController
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
        const val ACTION_PLAY = "com.github.meypod.al_azan.action.ADHAN_PLAY"
        const val ACTION_STOP = "com.github.meypod.al_azan.action.ADHAN_STOP"

        const val EXTRA_PRAYER = "prayer"
        const val EXTRA_TITLE = "title"
        const val EXTRA_BODY = "body"
        const val EXTRA_SOUND_URI = "sound_uri"
        const val EXTRA_CHANNEL_ID = "channel_id"
        const val EXTRA_VOLUME_PERCENT = "volume_percent"
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

        // Streams the hardware volume keys might drive while the alarm plays (device-dependent).
        private val VOLUME_KEY_STREAMS = intArrayOf(
            AudioManager.STREAM_ALARM,
            AudioManager.STREAM_MUSIC,
            AudioManager.STREAM_RING,
            AudioManager.STREAM_NOTIFICATION,
            AudioManager.STREAM_SYSTEM,
        )

        // Emitted whenever playback stops (notification "Dismiss", loop cap, call, etc.) so a visible
        // full-screen AlarmActivity can close itself even when the stop didn't originate from its UI.
        private val _stopSignal = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
        val stopSignal: SharedFlow<Unit> = _stopSignal.asSharedFlow()

        // Looping is driven by the caller (EXTRA_LOOP): notification-style reminder tones loop, a full
        // adhan plays once. This cap stops a looped sound from playing forever if nothing dismisses it.
        private const val LOOP_CAP_MS = 5 * 60 * 1000L

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
    private val nudgedVolumes = mutableMapOf<Int, Int>()

    // stream -> the value our own nudge set it to; the matching broadcast echo is ignored exactly once.
    private val pendingNudgeEcho = mutableMapOf<Int, Int>()
    private var stopOnVolume = false
    private var playbackStream = AudioManager.STREAM_ALARM
    private var wasPlayingBeforeCall = false
    private var volumePercent = -1
    private var shouldLoop = false
    private val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private val loopCapRunnable = Runnable { cleanupAndStop() }

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

        val channelId = intent.getStringExtra(EXTRA_CHANNEL_ID)
        val title = intent.getStringExtra(EXTRA_TITLE).orEmpty()
        val body = intent.getStringExtra(EXTRA_BODY)
        val prayerName = intent.getStringExtra(EXTRA_PRAYER).orEmpty()
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
        shouldLoop = intent.getBooleanExtra(EXTRA_LOOP, false)
        val vibration = intent.getStringExtra(EXTRA_VIBRATION)?.let { runCatching { VibrationMode.valueOf(it) }.getOrNull() }
            ?: VibrationMode.Off

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
        if (volumeButtonStops) nudgeVolumesOffEdge() // nudge before listening (its echo is filtered)
        // Always listen, with or without a full-screen: stop-on-volume dismisses on a press; normal mode
        // mirrors a non-playback stream's change onto the adhan (the playback stream already self-scales).
        registerVolumeReceiver()
        VibrationController.vibrate(this, vibration)
        // Directly open the full-screen alarm when either:
        //  - the user forced it (some OEMs ignore the full-screen-intent over the lock screen), or
        //  - notifications are denied, so the OS suppresses the FGS notification AND its full-screen-intent,
        //    leaving no Stop control. In that case we launch even when "keep screen off" (fullScreen=false)
        //    is set — otherwise the adhan would be unstoppable from the UI.
        // When notifications are on and force-launch is off we rely on the FSI/heads-up + notification, so the
        // alarm screen doesn't needlessly take over while the phone is in active use. The adhan fires via
        // setAlarmClock, whose background-activity-launch exemption permits this; if unavailable (e.g. the
        // alternate ExactAllowWhileIdle alarm type) the launch is silently dropped.
        val notificationsEnabled = NotificationManagerCompat.from(applicationContext).areNotificationsEnabled()
        if (forceLaunchActivity || !notificationsEnabled) {
            runCatching {
                startActivity(alarmActivityIntent(prayerName, timeLabel, title, header, isReminder, volumeButtonStops))
            }
        }
        startPlayer(uri, useMediaUsage)
        return START_NOT_STICKY
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
        if (volumePercent in 0..100) {
            val v = volumePercent / 100f
            mp.setVolume(v, v)
        }
        if (shouldLoop) {
            mp.isLooping = true
            mainHandler.postDelayed(loopCapRunnable, LOOP_CAP_MS)
        }
        runCatching { mp.start() }
    }

    override fun onCompletion(mp: MediaPlayer) = cleanupAndStop()

    override fun onError(
        mp: MediaPlayer,
        what: Int,
        extra: Int,
    ): Boolean {
        cleanupAndStop()
        return true
    }

    override fun onAudioFocusChange(focusChange: Int) {
        when {
            focusChange == AudioManager.AUDIOFOCUS_GAIN -> if (wasPlayingBeforeCall) resume()
            focusChange == AudioManager.AUDIOFOCUS_LOSS -> if (!isCallActive()) cleanupAndStop()
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

    private fun isCallActive(): Boolean {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_PHONE_STATE)
            != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }
        val tm = telephonyManager ?: return false

        @Suppress("DEPRECATION")
        val state = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) tm.callStateForSubscription else tm.callState
        return state != TelephonyManager.CALL_STATE_IDLE
    }

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
     * Stops the alarm when the alarm-stream volume changes. Covers the no-activity case (screen off /
     * heads-up instead of full-screen). The full-screen activity's key handling covers the rest,
     * including the min/max edges this broadcast misses.
     */
    private fun registerVolumeReceiver() {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(
                c: Context?,
                i: Intent?,
            ) {
                if (i == null) return
                if (stopOnVolume) {
                    if (!isOwnNudgeEcho(i)) cleanupAndStop() // a real press dismisses the alarm
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
            ContextCompat.RECEIVER_NOT_EXPORTED,
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
     * True only for the VOLUME_CHANGED echo of our own nudge: the stream matches a pending nudge and the
     * reported value equals what we set it to. Consumed once, so a later real press on that stream still
     * stops the alarm. Identified by value (not a timer), so a slow echo on a loaded device can never be
     * mistaken for a press — we must never falsely stop an alarm.
     */
    private fun isOwnNudgeEcho(intent: Intent): Boolean {
        if (pendingNudgeEcho.isEmpty()) return false
        val stream = intent.getIntExtra("android.media.EXTRA_VOLUME_STREAM_TYPE", -1)
        val value = intent.getIntExtra("android.media.EXTRA_VOLUME_STREAM_VALUE", Int.MIN_VALUE)
        // Exact match: definitely our echo.
        if (pendingNudgeEcho[stream] == value) {
            pendingNudgeEcho.remove(stream)
            return true
        }
        // Extras missing (rare device): can't tell echo from press. While echoes are still outstanding,
        // assume echo and swallow one — missing an alarm is far worse than needing a second press.
        if (stream == -1 || value == Int.MIN_VALUE) {
            pendingNudgeEcho.remove(pendingNudgeEcho.keys.first())
            return true
        }
        return false
    }

    /**
     * The hardware volume keys may drive any of several streams depending on device/state, and a press
     * while that stream is at its max is a no-op the VOLUME_CHANGED broadcast never sees. We can't know
     * which stream the keys control, so move every plausible one down off its max edge; whichever the
     * keys hit then changes and fires the broadcast. Originals are restored in [restoreVolumes].
     */
    private fun nudgeVolumesOffEdge() {
        val am = audioManager ?: return
        for (stream in VOLUME_KEY_STREAMS) {
            val max = runCatching { am.getStreamMaxVolume(stream) }.getOrDefault(0)
            val cur = runCatching { am.getStreamVolume(stream) }.getOrDefault(-1)
            if ((max <= 1) || (cur < max)) continue // only the max edge is a problem
            nudgedVolumes[stream] = cur
            pendingNudgeEcho[stream] = max - 1
            runCatching { am.setStreamVolume(stream, max - 1, 0) }
        }
    }

    private fun restoreVolumes() {
        if (nudgedVolumes.isEmpty()) return
        val am = audioManager
        nudgedVolumes.forEach { (stream, original) ->
            runCatching { am?.setStreamVolume(stream, original, 0) }
        }
        nudgedVolumes.clear()
        pendingNudgeEcho.clear()
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
            .setContentText(body)
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

    private fun alarmActivityIntent(
        prayerName: String,
        timeLabel: String,
        title: String,
        header: String,
        isReminder: Boolean,
        volumeButtonStops: Boolean,
    ): Intent =
        Intent(this, AlarmActivity::class.java).apply {
            putExtra(EXTRA_PRAYER, prayerName)
            putExtra(EXTRA_TIME_LABEL, timeLabel)
            putExtra(EXTRA_TITLE, title)
            putExtra(EXTRA_HEADER, header)
            putExtra(EXTRA_IS_REMINDER, isReminder)
            putExtra(EXTRA_VOLUME_BUTTON_STOPS, volumeButtonStops)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

    private fun cleanupAndStop() {
        mainHandler.removeCallbacks(loopCapRunnable)
        player?.let { mp ->
            runCatching { mp.reset() }
            mp.release()
        }
        player = null
        abandonAudioFocus()
        unregisterCallStateListener()
        unregisterVolumeReceiver()
        restoreVolumes()
        VibrationController.stop(this)
        _stopSignal.tryEmit(Unit)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

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
