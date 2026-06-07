package com.github.meypod.al_azan

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.github.meypod.al_azan.adhan.AdhanContract
import com.github.meypod.al_azan.adhan.AdhanFiringHandler
import com.github.meypod.al_azan.core.domain.model.adhan.Prayer
import com.github.meypod.al_azan.playback.PlaybackService
import com.github.meypod.al_azan.reminder.ReminderContract
import com.github.meypod.al_azan.reminder.ReminderFiringHandler
import com.github.meypod.al_azan.widget.WidgetContract
import com.github.meypod.al_azan.widget.WidgetUpdater
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Entry point for every fired [com.github.meypod.al_azan.core.domain.model.alarm.ScheduledAlarm].
 *
 * AlarmManager starts the process to deliver this broadcast even after a memory-kill, so the work runs
 * in the same wake-up — no deferrable worker in between.
 */
@AndroidEntryPoint
class AlarmReceiver : BroadcastReceiver() {

    private companion object {
        const val TAG = "AlarmReceiver"
    }

    @Inject
    lateinit var widgetUpdater: WidgetUpdater

    @Inject
    lateinit var adhanFiringHandler: AdhanFiringHandler

    @Inject
    lateinit var reminderFiringHandler: ReminderFiringHandler

    override fun onReceive(
        context: Context,
        intent: Intent?,
    ) {
        when (intent?.action) {
            WidgetContract.ACTION_WIDGET_UPDATE -> async { widgetUpdater.update() }

            AdhanContract.ACTION_ADHAN -> {
                val prayer = intent.prayer() ?: return
                val playSound = intent.getStringExtra(AdhanContract.EXTRA_PLAY_SOUND)?.toBoolean() ?: false
                val timestamp = intent.getStringExtra(AdhanContract.EXTRA_TIMESTAMP)?.toLongOrNull()
                    ?: System.currentTimeMillis()
                async { adhanFiringHandler.onAdhanFired(prayer, playSound, timestamp) }
            }

            AdhanContract.ACTION_PRE_ADHAN -> {
                val prayer = intent.prayer() ?: return
                val timestamp = intent.getStringExtra(AdhanContract.EXTRA_TIMESTAMP)?.toLongOrNull()
                    ?: System.currentTimeMillis()
                async { adhanFiringHandler.onPreAdhanFired(prayer, timestamp) }
            }

            AdhanContract.ACTION_CANCEL_ADHAN -> async { adhanFiringHandler.onCancelAdhan() }

            AdhanContract.ACTION_UNSILENCE -> async { adhanFiringHandler.onUnsilence() }

            AdhanContract.ACTION_ADHAN_REMIND -> {
                val prayer = intent.prayer() ?: return
                val minutes = intent.getStringExtra(AdhanContract.EXTRA_REMIND_MINUTES)?.toIntOrNull() ?: 0
                async { adhanFiringHandler.onAdhanRemindFired(prayer, minutes) }
            }

            ReminderContract.ACTION_REMINDER -> {
                val reminderId = intent.getStringExtra(ReminderContract.EXTRA_REMINDER_ID) ?: return
                val timestamp = intent.getStringExtra(ReminderContract.EXTRA_TIMESTAMP)?.toLongOrNull()
                    ?: System.currentTimeMillis()
                async { reminderFiringHandler.onReminderFired(reminderId, timestamp) }
            }

            ReminderContract.ACTION_PRE_REMINDER -> {
                val reminderId = intent.getStringExtra(ReminderContract.EXTRA_REMINDER_ID) ?: return
                val timestamp = intent.getStringExtra(ReminderContract.EXTRA_TIMESTAMP)?.toLongOrNull()
                    ?: System.currentTimeMillis()
                async { reminderFiringHandler.onPreReminderFired(reminderId, timestamp) }
            }

            ReminderContract.ACTION_CANCEL_REMINDER -> {
                val reminderId = intent.getStringExtra(ReminderContract.EXTRA_REMINDER_ID) ?: return
                async { reminderFiringHandler.onCancelReminder(reminderId) }
            }
        }
    }

    private fun Intent.prayer(): Prayer? =
        getStringExtra(PlaybackService.EXTRA_PRAYER)?.let { runCatching { Prayer.valueOf(it) }.getOrNull() }

    /** Runs [block] off the main thread while keeping the broadcast alive via goAsync(). */
    private fun async(block: suspend () -> Unit) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                block()
            } catch (e: Exception) {
                Log.e(TAG, "Alarm handling failed", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
