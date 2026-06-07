package com.github.meypod.al_azan.ramadan

import com.github.meypod.al_azan.core.domain.model.alarm.AlarmType
import com.github.meypod.al_azan.core.domain.model.alarm.ScheduledAlarm
import com.github.meypod.al_azan.core.domain.repository.AlarmRepository
import java.time.ZoneId
import java.time.ZonedDateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Arms a once-a-day alarm that lets [RamadanNoticeHandler] decide whether to post the Ramadan-accuracy
 * notice. (Re)scheduled on app start, boot, and time change; the handler re-arms it after each fire.
 *
 * Inexact + non-wakeup: a once-a-day informational notice doesn't justify exact scheduling or waking a
 * dozing device, and it survives the OS denying exact-alarm permission.
 */
@Singleton
class RamadanNoticeScheduler @Inject constructor(
    private val alarmRepository: AlarmRepository,
) {
    suspend fun schedule() {
        alarmRepository.schedule(
            ScheduledAlarm(
                id = RamadanNoticeContract.CHECK_ALARM_ID,
                triggerAtMillis = nextCheckMillis(),
                action = RamadanNoticeContract.ACTION_RAMADAN_CHECK,
                type = AlarmType.Inexact,
                wakeup = false,
            ),
        )
    }

    private fun nextCheckMillis(): Long {
        val now = ZonedDateTime.now(ZoneId.systemDefault())
        var next = now.withHour(RamadanNoticeContract.CHECK_HOUR).withMinute(0).withSecond(0).withNano(0)
        if (!next.isAfter(now)) next = next.plusDays(1)
        return next.toInstant().toEpochMilli()
    }
}
