package com.github.meypod.al_azan.core.data.repository

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.github.meypod.al_azan.AlarmReceiver
import com.github.meypod.al_azan.core.domain.model.alarm.AlarmType
import com.github.meypod.al_azan.core.domain.model.alarm.ScheduledAlarm
import com.github.meypod.al_azan.core.domain.repository.AlarmRepository
import com.github.meypod.al_azan.core.util.storage.MMKVDataStore
import kotlinx.coroutines.flow.first

class AlarmRepositoryImpl(
    private val context: Context,
    private val store: MMKVDataStore<List<ScheduledAlarm>>,
) : AlarmRepository {

    companion object {
        const val TAG = "AlarmRepositoryImpl"
        const val EXTRA_ALARM_ID = "alarm_id"
    }

    private val alarmManager by lazy { context.getSystemService(Context.ALARM_SERVICE) as AlarmManager }

    override val data get() = store.data

    override suspend fun schedule(alarm: ScheduledAlarm) {
        store.update { alarms -> alarms.filterNot { it.id == alarm.id } + alarm }
        register(alarm)
    }

    override suspend fun cancel(id: String) {
        // Reproduce the scheduled intent (action included) so PendingIntent.filterEquals matches.
        val existing = store.data.first().firstOrNull { it.id == id }
        store.update { alarms -> alarms.filterNot { it.id == id } }
        if (existing != null) {
            alarmManager.cancel(pendingIntentFor(existing.id, existing.action, existing.extras))
        }
    }

    override suspend fun cancelAll() {
        store.data.first().forEach { alarmManager.cancel(pendingIntentFor(it.id, it.action, it.extras)) }
        store.update { emptyList() }
    }

    override suspend fun getScheduled(): List<ScheduledAlarm> = store.data.first()

    override suspend fun rescheduleAll() {
        val now = System.currentTimeMillis()
        val (live, expired) = store.data.first().partition { it.triggerAtMillis > now }
        if (expired.isNotEmpty()) {
            store.update { alarms -> alarms.filterNot { stale -> expired.any { it.id == stale.id } } }
        }
        live.forEach { register(it) }
    }

    override fun canScheduleExact(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) alarmManager.canScheduleExactAlarms() else true

    private fun register(alarm: ScheduledAlarm) {
        val pendingIntent = pendingIntentFor(alarm.id, alarm.action, alarm.extras)
        // Fall back to inexact scheduling when the OS denies exact alarms, so the alarm still fires.
        val effectiveType =
            if (alarm.type != AlarmType.Inexact && !canScheduleExact()) AlarmType.Inexact else alarm.type
        val rtcType = if (alarm.wakeup) AlarmManager.RTC_WAKEUP else AlarmManager.RTC
        try {
            when (effectiveType) {
                AlarmType.AlarmClock ->
                    alarmManager.setAlarmClock(
                        AlarmManager.AlarmClockInfo(alarm.triggerAtMillis, pendingIntent),
                        pendingIntent,
                    )

                AlarmType.ExactAllowWhileIdle ->
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        alarm.triggerAtMillis,
                        pendingIntent,
                    )

                AlarmType.Exact ->
                    alarmManager.setExact(rtcType, alarm.triggerAtMillis, pendingIntent)

                AlarmType.Inexact ->
                    alarmManager.set(rtcType, alarm.triggerAtMillis, pendingIntent)
            }
        } catch (e: SecurityException) {
            // Exact-alarm permission can be revoked between the check and the call; degrade gracefully.
            Log.e(TAG, "Exact alarm denied for ${alarm.id}, retrying inexact", e)
            alarmManager.set(rtcType, alarm.triggerAtMillis, pendingIntent)
        }
    }

    /**
     * Builds the [PendingIntent] for an alarm. The request code is derived from [id] and the intent
     * carries [action] (extras are excluded from PendingIntent identity), so the same (id, action)
     * always maps to the same PendingIntent — enabling replace and cancel.
     */
    private fun pendingIntentFor(
        id: String,
        action: String,
        extras: Map<String, String> = emptyMap(),
    ): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            setAction(action)
            putExtra(EXTRA_ALARM_ID, id)
            extras.forEach { (k, v) -> putExtra(k, v) }
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getBroadcast(context, id.hashCode(), intent, flags)
    }
}
