package com.github.meypod.al_azan.core.data.audio

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.telephony.PhoneStateListener
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.core.net.toUri
import com.github.meypod.al_azan.R
import com.github.meypod.al_azan.core.data.audio.AdhanPreviewPlaybackService.Companion.playingId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Foreground service that previews a single adhan/audio entry. Holds a [MediaPlayer], shows an
 * ongoing notification with a Stop action while playing, and yields to phone calls via audio focus
 * and telephony state. Only one entry plays at a time; [playingId] mirrors the active entry id.
 */
class AdhanPreviewPlaybackService :
    Service(),
    MediaPlayer.OnPreparedListener,
    MediaPlayer.OnCompletionListener,
    MediaPlayer.OnErrorListener,
    AudioManager.OnAudioFocusChangeListener {

    companion object {
        private const val ACTION_PLAY = "com.github.meypod.al_azan.action.PREVIEW_PLAY"
        private const val ACTION_STOP = "com.github.meypod.al_azan.action.PREVIEW_STOP"
        private const val EXTRA_URI = "uri"
        private const val EXTRA_ID = "id"
        private const val EXTRA_LABEL = "label"

        private const val CHANNEL_ID = "adhan_preview_playback"
        private const val NOTIFICATION_ID = 0xADA1

        private val _playingId = MutableStateFlow<String?>(null)

        /** Emits the id of the entry currently previewing, or null when idle. Process-global. */
        val playingId: StateFlow<String?> = _playingId.asStateFlow()

        fun play(
            context: Context,
            uri: Uri,
            id: String,
            label: String,
        ) {
            val intent = Intent(context, AdhanPreviewPlaybackService::class.java).apply {
                action = ACTION_PLAY
                putExtra(EXTRA_URI, uri.toString())
                putExtra(EXTRA_ID, id)
                putExtra(EXTRA_LABEL, label)
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, AdhanPreviewPlaybackService::class.java).apply {
                action = ACTION_STOP
            }
            // already-stopped service: a plain startService is fine, onStartCommand cleans up.
            context.startService(intent)
        }
    }

    private var player: MediaPlayer? = null
    private var focusRequest: AudioFocusRequest? = null

    private var telephonyCallback: TelephonyCallback? = null
    private var phoneStateListener: PhoneStateListener? = null

    private val audioManager: AudioManager? by lazy { getSystemService() }
    private val telephonyManager: TelephonyManager? by lazy { getSystemService() }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        when (intent?.action) {
            ACTION_PLAY -> {
                val uri = intent.getStringExtra(EXTRA_URI)?.toUri()
                val id = intent.getStringExtra(EXTRA_ID)
                val label = intent.getStringExtra(EXTRA_LABEL).orEmpty()
                if (uri != null && id != null) {
                    startPlayback(uri, id, label)
                } else {
                    cleanupAndStop()
                }
            }

            else -> cleanupAndStop()
        }
        return START_NOT_STICKY
    }

    private fun startPlayback(
        uri: Uri,
        id: String,
        label: String,
    ) {
        // Must call startForeground promptly after startForegroundService.
        startForeground(NOTIFICATION_ID, buildNotification(label))

        if (isCallActive()) {
            cleanupAndStop()
            return
        }
        if (!requestAudioFocus()) {
            cleanupAndStop()
            return
        }
        registerCallStateListener()

        player?.release()
        player = MediaPlayer().apply {
            setWakeMode(applicationContext, PowerManager.PARTIAL_WAKE_LOCK)
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build(),
            )
            setOnPreparedListener(this@AdhanPreviewPlaybackService)
            setOnCompletionListener(this@AdhanPreviewPlaybackService)
            setOnErrorListener(this@AdhanPreviewPlaybackService)
            val ok = runCatching {
                setDataSource(applicationContext, uri)
                prepareAsync()
            }.isSuccess
            if (!ok) {
                cleanupAndStop()
                return
            }
        }
        _playingId.value = id
    }

    override fun onPrepared(mp: MediaPlayer) {
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
        // Any loss (transient or permanent, e.g. an incoming call grabbing focus) stops the preview.
        if (focusChange <= 0) cleanupAndStop()
    }

    private fun requestAudioFocus(): Boolean {
        val am = audioManager ?: return false
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()
        val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
            .setAudioAttributes(attributes)
            .setOnAudioFocusChangeListener(this)
            .build()
        focusRequest = request
        return am.requestAudioFocus(request) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    private fun abandonAudioFocus() {
        val am = audioManager ?: return
        focusRequest?.let { am.abandonAudioFocusRequest(it) }
        focusRequest = null
    }

    private fun hasPhoneStatePermission(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) ==
            PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    private fun isCallActive(): Boolean {
        if (!hasPhoneStatePermission()) return false
        val tm = telephonyManager ?: return false

        @Suppress("DEPRECATION")
        val state = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) tm.callStateForSubscription else tm.callState
        return state != TelephonyManager.CALL_STATE_IDLE
    }

    private fun registerCallStateListener() {
        if (!hasPhoneStatePermission()) return
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

    private fun onCallState(state: Int) {
        if (state != TelephonyManager.CALL_STATE_IDLE) cleanupAndStop()
    }

    private fun buildNotification(label: String): android.app.Notification {
        ensureChannel()
        val stopIntent = PendingIntent.getService(
            this,
            0,
            Intent(this, AdhanPreviewPlaybackService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.monochrome_notif)
            .setContentTitle(getString(R.string.adhan_preview_playing))
            .setContentText(label)
            .setOngoing(true)
            .setSilent(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .addAction(R.drawable.outline_stop_24, getString(R.string.stop), stopIntent)
            .build()
    }

    private fun ensureChannel() {
        val channel = NotificationChannelCompat.Builder(
            CHANNEL_ID,
            NotificationManagerCompat.IMPORTANCE_LOW,
        )
            .setName(getString(R.string.adhan_preview_channel_name))
            .setShowBadge(false)
            .build()
        NotificationManagerCompat.from(this).createNotificationChannel(channel)
    }

    private fun cleanupAndStop() {
        player?.let { mp ->
            runCatching { mp.reset() }
            mp.release()
        }
        player = null
        abandonAudioFocus()
        unregisterCallStateListener()
        _playingId.value = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        cleanupAndStop()
        super.onDestroy()
    }
}
