package com.github.meypod.al_azan.alarm

import android.app.AutomaticZenRule
import android.app.NotificationManager
import android.content.Context
import android.net.Uri
import android.os.Build
import android.service.notification.Condition
import androidx.annotation.RequiresApi
import androidx.core.content.getSystemService
import androidx.core.net.toUri
import com.github.meypod.al_azan.R
import com.github.meypod.al_azan.adhan.AdhanContract
import com.github.meypod.al_azan.core.domain.model.alarm.AlarmType
import com.github.meypod.al_azan.core.domain.model.alarm.ScheduledAlarm
import com.github.meypod.al_azan.core.domain.repository.AlarmRepository
import com.github.meypod.al_azan.core.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Clock

/**
 * Owns the "Dismiss & silent" total-silence Do Not Disturb window shared by adhans and reminders:
 * silences the phone (and suppresses both adhan and reminder playback via
 * [com.github.meypod.al_azan.core.domain.model.settings.Settings.silencedUntilMillis]) for a
 * window, then restores when it ends.
 *
 * The DND mechanism is version-split: apps targeting API 35+ can no longer set the global interruption
 * filter, so we contribute our own [AutomaticZenRule] (most-restrictive-policy-wins) and toggle it,
 * never touching the user's own modes. On API < 35 we set the global filter to total silence and
 * capture the previous one so [unsilence] can restore it.
 */
@Singleton
class DndSilenceController @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val alarmRepository: AlarmRepository,
) {
    /**
     * Suppress alarms for [minutes] and put the phone into total-silence DND for the same window,
     * arming an unsilence alarm to undo it. Returns whether DND was actually engaged (i.e. policy
     * access was granted); the suppression window is set regardless.
     */
    suspend fun silence(minutes: Int): Boolean {
        val until = Clock.System.now().toEpochMilliseconds() + minutes * 60_000L
        val nm = context.getSystemService<NotificationManager>()
            ?.takeIf { it.isNotificationPolicyAccessGranted }

        var restoreFilter: Int? = null
        var zenRuleId: String? = null
        if (nm != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                zenRuleId = nm.engageSilenceRule(settingsRepository.data.first().adhanSilenceZenRuleId)
            } else {
                restoreFilter = nm.currentInterruptionFilter
                nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_NONE)
            }
        }
        // DND engaged → an unsilence alarm must undo it. On API 35+ that hinges on the rule being created.
        val dndEngaged = nm != null &&
            (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM || zenRuleId != null)

        settingsRepository.update {
            it.copy(silencedUntilMillis = until, dndRestoreFilter = restoreFilter, adhanSilenceZenRuleId = zenRuleId)
        }
        if (dndEngaged) {
            alarmRepository.schedule(
                ScheduledAlarm(
                    id = AdhanContract.UNSILENCE_ALARM_ID,
                    triggerAtMillis = until,
                    action = AdhanContract.ACTION_UNSILENCE,
                    type = AlarmType.ExactAllowWhileIdle,
                ),
            )
        }
        return dndEngaged
    }

    /** The silence window ended: release the DND, clear the window, and cancel the unsilence alarm. */
    suspend fun unsilence() {
        val settings = settingsRepository.data.first()
        val nm = context.getSystemService<NotificationManager>()
        if (nm != null && nm.isNotificationPolicyAccessGranted) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                settings.adhanSilenceZenRuleId?.let { nm.releaseSilenceRule(it) }
            } else {
                nm.setInterruptionFilter(settings.dndRestoreFilter ?: NotificationManager.INTERRUPTION_FILTER_ALL)
            }
        }
        settingsRepository.update {
            it.copy(silencedUntilMillis = null, dndRestoreFilter = null, adhanSilenceZenRuleId = null)
        }
        alarmRepository.cancel(AdhanContract.UNSILENCE_ALARM_ID)
    }

    /**
     * Ensures the app's total-silence [AutomaticZenRule] exists (reusing [existingId] when still valid),
     * activates it, and returns its id — or null if the platform rejected the rule.
     */
    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    private fun NotificationManager.engageSilenceRule(existingId: String?): String? {
        val ruleId = runCatching {
            existingId?.takeIf { automaticZenRules.containsKey(it) } ?: addAutomaticZenRule(
                AutomaticZenRule.Builder(context.getString(R.string.app_name), SILENCE_CONDITION_ID)
                    .setType(AutomaticZenRule.TYPE_OTHER)
                    .setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_NONE)
                    .build(),
            )
        }.getOrNull() ?: return null
        runCatching { setAutomaticZenRuleState(ruleId, silenceCondition(Condition.STATE_TRUE)) }
        return ruleId
    }

    /** Deactivates the app's silence [AutomaticZenRule]; the rule is kept (disabled) for reuse. */
    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    private fun NotificationManager.releaseSilenceRule(ruleId: String) {
        runCatching { setAutomaticZenRuleState(ruleId, silenceCondition(Condition.STATE_FALSE)) }
    }

    private fun silenceCondition(state: Int): Condition =
        Condition(SILENCE_CONDITION_ID, context.getString(R.string.app_name), state)

    private companion object {
        /** Stable condition id tying the silence [AutomaticZenRule] to its toggle [Condition]. */
        val SILENCE_CONDITION_ID: Uri = "al_azan://dnd/adhan_silence".toUri()
    }
}
