package com.github.meypod.al_azan.alarm

import com.github.meypod.al_azan.R
import com.github.meypod.al_azan.adhan.AdhanContract
import com.github.meypod.al_azan.core.data.locale.LocalizedResources
import com.github.meypod.al_azan.core.domain.model.TextResource
import com.github.meypod.al_azan.core.domain.model.adhan.Prayer
import com.github.meypod.al_azan.core.domain.repository.AlarmRepository
import com.github.meypod.al_azan.core.domain.repository.NotificationRepository
import com.github.meypod.al_azan.core.domain.repository.ReminderRepository
import com.github.meypod.al_azan.core.domain.repository.SettingsRepository
import com.github.meypod.al_azan.core.domain.util.formatMissedWhen
import com.github.meypod.al_azan.core.presentation.mapper.displayName
import com.github.meypod.al_azan.playback.missedNotificationConfig
import com.github.meypod.al_azan.reminder.ReminderContract
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Clock

/**
 * Posts a silent "missed" notice for each adhan/reminder whose armed time elapsed while the device was
 * powered off, so the user still learns the prayer/reminder passed. The OS drops all alarms on shutdown,
 * so a past-due alarm still sitting in the persisted set is one that never fired.
 *
 * Must run BEFORE the schedulers recompute: each scheduler overwrites the past-due armed alarm (same id)
 * with the next future occurrence, erasing the evidence. Only the single armed occurrence per id is
 * recoverable, so a multi-day power-off surfaces the most recent missed prayer per alarm, not every
 * elapsed one.
 */
@Singleton
class MissedAlarmCatchUp @Inject constructor(
    private val alarmRepository: AlarmRepository,
    private val settingsRepository: SettingsRepository,
    private val reminderRepository: ReminderRepository,
    private val notificationRepository: NotificationRepository,
    private val localizedResources: LocalizedResources,
) {
    private companion object {
        const val MISSED_WINDOW_NOTICE_ID = "missed_window_notice"

        // Past this, the soonest alarm we couldn't deliver is old enough that further occurrences almost
        // certainly came due during the outage too — but those were never armed (only the next one is
        // scheduled at a time), so they can't be listed. Worth one informational note explaining that.
        const val LONG_OUTAGE_THRESHOLD_MS = 4 * 60 * 60 * 1000L
    }

    suspend fun catchUpMissed() {
        val now = Clock.System.now().toEpochMilliseconds()
        val settings = settingsRepository.data.first()
        val delivered = settings.deliveredAlarmTimestamps
        // The earliest missed occurrence, used to gauge whether the outage likely spanned more than the
        // single armed alarm per id that we can actually surface.
        var oldestMissed: Long? = null
        alarmRepository.getScheduled()
            .filter { it.triggerAtMillis <= now }
            .forEach { alarm ->
                when (alarm.action) {
                    AdhanContract.ACTION_ADHAN -> {
                        // delivered floor < trigger means this exact occurrence never fired (would equal
                        // trigger if it had, since the firing handler marks it with the prayer time).
                        if ((delivered[AdhanContract.ADHAN_NOTIFICATION_ID] ?: 0L) >= alarm.triggerAtMillis) return@forEach
                        val prayer = alarm.extras[AdhanContract.EXTRA_PRAYER]
                            ?.let { runCatching { Prayer.valueOf(it) }.getOrNull() } ?: return@forEach
                        notificationRepository.notify(
                            missedNotificationConfig(
                                id = "missed_adhan_${prayer.name}",
                                title = TextResource.StringResId(prayer.stringRes),
                                body = TextResource.StringResIdWithArgs(
                                    R.string.missed_while_off_body,
                                    settings.formatMissedWhen(alarm.triggerAtMillis, now),
                                ),
                            ),
                        )
                        oldestMissed = minOf(oldestMissed ?: Long.MAX_VALUE, alarm.triggerAtMillis)
                        // Treat the missed notice as delivery: idempotent across a second reconcile, and
                        // advances the scheduler's re-fire floor past this occurrence.
                        settingsRepository.markDelivered(AdhanContract.ADHAN_NOTIFICATION_ID, alarm.triggerAtMillis)
                    }

                    ReminderContract.ACTION_REMINDER -> {
                        val reminderId = alarm.extras[ReminderContract.EXTRA_REMINDER_ID] ?: return@forEach
                        val key = ReminderContract.notificationId(reminderId)
                        if ((delivered[key] ?: 0L) >= alarm.triggerAtMillis) return@forEach
                        val reminder = reminderRepository.data.first().firstOrNull { it.id == reminderId }
                        val title = reminder?.displayName(localizedResources.current)
                            ?: localizedResources.current.getString(R.string.reminder)
                        notificationRepository.notify(
                            missedNotificationConfig(
                                id = "missed_$key",
                                title = TextResource.Literal(title),
                                body = TextResource.StringResIdWithArgs(
                                    R.string.missed_while_off_body,
                                    settings.formatMissedWhen(alarm.triggerAtMillis, now),
                                ),
                            ),
                        )
                        oldestMissed = minOf(oldestMissed ?: Long.MAX_VALUE, alarm.triggerAtMillis)
                        settingsRepository.markDelivered(key, alarm.triggerAtMillis)
                    }
                }
            }

        // One soft note when the gap is long enough that earlier occurrences, never armed, went unshown.
        val oldest = oldestMissed
        if (oldest != null && now - oldest > LONG_OUTAGE_THRESHOLD_MS) {
            notificationRepository.notify(
                missedNotificationConfig(
                    id = MISSED_WINDOW_NOTICE_ID,
                    title = TextResource.StringResId(R.string.missed_window_title),
                    body = TextResource.StringResId(R.string.missed_window_body),
                ),
            )
        }
    }
}
