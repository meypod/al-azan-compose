package com.github.meypod.al_azan.core.util.device

import android.content.Context
import android.media.AudioAttributes
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.content.getSystemService
import com.github.meypod.al_azan.core.domain.model.alarm.VibrationMode

/** Shared vibration control used by the adhan playback service and the developer test screen. */
object VibrationController {
    private const val ONCE_DURATION_MS = 800L

    // Distinctive prayer-call cadence; loops from index 5 (matches the legacy app).
    private val CONTINUOUS_PATTERN = longArrayOf(0, 700, 2300, 1000, 2300, 1000, 1300, 1000, 1000)
    private const val CONTINUOUS_REPEAT_INDEX = 5

    // Tag as alarm usage so the vibration still plays under Do Not Disturb / silent mode.
    private val ALARM_ATTRS = AudioAttributes.Builder()
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .setUsage(AudioAttributes.USAGE_ALARM)
        .build()

    fun vibrate(
        context: Context,
        mode: VibrationMode,
    ) {
        if (mode == VibrationMode.Off) return
        val vibrator = vibrator(context) ?: return
        val effect = when (mode) {
            VibrationMode.Continuous ->
                VibrationEffect.createWaveform(CONTINUOUS_PATTERN, CONTINUOUS_REPEAT_INDEX)

            else -> VibrationEffect.createOneShot(ONCE_DURATION_MS, VibrationEffect.DEFAULT_AMPLITUDE)
        }
        runCatching { vibrator.vibrate(effect, ALARM_ATTRS) }
    }

    fun stop(context: Context) {
        runCatching { vibrator(context)?.cancel() }
    }

    private fun vibrator(context: Context): Vibrator? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService<VibratorManager>()?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService()
        }
}
