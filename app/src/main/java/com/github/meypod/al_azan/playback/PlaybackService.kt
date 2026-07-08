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
import com.github.meypod.al_azan.MainActivity
import com.github.meypod.al_azan.R
import com.github.meypod.al_azan.adhan.AdhanContract
import com.github.meypod.al_azan.alarm.AlarmActivity
import com.github.meypod.al_azan.core.data.locale.withAppLocale
import com.github.meypod.al_azan.core.domain.model.alarm.VibrationMode
import com.github.meypod.al_azan.core.domain.usecase.EnsureNotificationChannelsUseCase
import com.github.meypod.al_azan.core.util.device.CallStateInspector
import com.github.meypod.al_azan.core.util.device.VibrationController
import com.github.meypod.al_azan.playback.PlaybackService.Companion.NUDGE_ECHO_WINDOW_MS
import com.github.meypod.al_azan.playback.PlaybackService.Companion.NUDGE_STREAMS
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

        // Streams the hardware volume keys might drive while the alarm plays (device-dependent). Used by
        // normal (mirror) mode, which can't assume which stream the keys hit.
        private val VOLUME_KEY_STREAMS = intArrayOf(
            AudioManager.STREAM_ALARM,
            AudioManager.STREAM_MUSIC,
            AudioManager.STREAM_RING,
            AudioManager.STREAM_NOTIFICATION,
            AudioManager.STREAM_SYSTEM,
        )

        // Streams to nudge off their edge so a key press is never a silent no-op. The adhan plays with
        // USAGE_ALARM (→ STREAM_ALARM) or USAGE_MEDIA (→ STREAM_MUSIC), so the keys drive one of these two
        // while it sounds. Both are independent — unlike RING/NOTIFICATION/SYSTEM, which many devices link
        // and collapse into a single broadcast, leaving stale echoes that could swallow a real press — so
        // each nudge here yields exactly one echo.
        private val NUDGE_STREAMS = intArrayOf(
            AudioManager.STREAM_ALARM,
            AudioManager.STREAM_MUSIC,
        )

        // Our nudge's echo broadcasts land within a few hundred ms; a real press comes seconds later (the
        // user reaches for the phone). Suppress echoes only inside this window, then always treat a
        // broadcast as a real press — so a nudge whose echo never fires can't silently eat the user's press.
        private const val NUDGE_ECHO_WINDOW_MS = 1500L

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
    private val clearNudgeEchoRunnable = Runnable { pendingNudgeEcho.clear() }

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
        // Now that playback is actually starting, remember what to leave behind if it ends on its own.
        lingerDetails = LingerDetails(title = title, body = body, timeLabel = timeLabel)
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
     * stops the alarm. The value match distinguishes our echo from a press (a press lands on a different
     * value); [NUDGE_ECHO_WINDOW_MS] bounds how long we suppress, so a nudge whose echo never arrives can't
     * leave a stale entry that swallows a much-later real press.
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
     * A key press while the driven stream sits at an edge is a no-op the VOLUME_CHANGED broadcast never
     * sees: at the max edge an up-press does nothing, at the min edge a down-press does nothing. So move
     * each [NUDGE_STREAMS] stream (the ones the adhan's usage routes the keys to) that's edge-pinned a step
     * toward the interior; from there any press in either direction fires the broadcast. Originals are
     * restored in [restoreVolumes].
     *
     * The min edge is not always 0 — STREAM_ALARM (and STREAM_RING/VOICE_CALL) floor at 1, so nudging to
     * a hardcoded 1 would be a no-op on those. Use each stream's real min and nudge to min + 1.
     */
    private fun nudgeVolumesOffEdge() {
        val am = audioManager ?: return
        for (stream in NUDGE_STREAMS) {
            val max = runCatching { am.getStreamMaxVolume(stream) }.getOrDefault(0)
            val min = runCatching { streamMinVolume(am, stream) }.getOrDefault(0)
            val cur = runCatching { am.getStreamVolume(stream) }.getOrDefault(-1)
            if (max - min <= 1 || cur < 0) continue // no interior value to move to
            val target = when {
                cur >= max -> max - 1

                // at the max edge: an up-press is a no-op
                cur <= min -> min + 1

                // at the min edge: a down-press is a no-op (alarm min is 1, not 0)
                else -> continue // already interior: any press fires a broadcast
            }
            nudgedVolumes[stream] = cur
            pendingNudgeEcho[stream] = target
            runCatching { am.setStreamVolume(stream, target, 0) }
        }
        // Bound echo suppression in time so a nudge whose echo never fires can't leave a stale entry.
        if (pendingNudgeEcho.isNotEmpty()) {
            mainHandler.postDelayed(clearNudgeEchoRunnable, NUDGE_ECHO_WINDOW_MS)
        }
    }

    /** Real per-stream minimum (STREAM_ALARM floors at 1). [AudioManager.getStreamMinVolume] is API 28+. */
    private fun streamMinVolume(
        am: AudioManager,
        stream: Int,
    ): Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) am.getStreamMinVolume(stream) else 0

    private fun restoreVolumes() {
        mainHandler.removeCallbacks(clearNudgeEchoRunnable)
        pendingNudgeEcho.clear()
        if (nudgedVolumes.isEmpty()) return
        val am = audioManager
        nudgedVolumes.forEach { (stream, original) ->
            runCatching { am?.setStreamVolume(stream, original, 0) }
        }
        nudgedVolumes.clear()
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
     * Stops playback and tears the service down. When [leaveLingering] is true (the adhan/reminder ended
     * on its own — playback finished, looped past the cap, errored, or lost audio focus — rather than the
     * user dismissing it) the foreground notification is detached and replaced with a quiet, dismissible
     * one so the passed prayer/reminder still leaves a trace, matching the old app. A user dismiss
     * (notification Stop, swipe, volume press, the firing handlers) removes it outright.
     */
    private fun cleanupAndStop(leaveLingering: Boolean = false) {
        if (stopped) return
        stopped = true
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
        val lingering = if (leaveLingering) buildLingeringNotification() else null
        // Always remove the foreground notification first: the trace lives on a different (silent)
        // channel, which a same-id re-post could never switch to.
        stopForeground(STOP_FOREGROUND_REMOVE)
        if (lingering != null) {
            // A fresh id per trace so successive passed prayers/reminders stack instead of replacing one
            // another. Posted exactly once per playback (guarded above), and the clock only moves forward,
            // so this stays unique even across a process restart while an earlier trace is still showing.
            NotificationManagerCompat.from(this).notify(System.currentTimeMillis().toInt(), lingering)
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
