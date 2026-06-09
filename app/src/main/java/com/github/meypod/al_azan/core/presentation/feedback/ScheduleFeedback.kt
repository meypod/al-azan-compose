package com.github.meypod.al_azan.core.presentation.feedback

import com.github.meypod.al_azan.core.domain.model.adhan.Prayer
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * What was just (re)scheduled, for transient snackbar feedback. [prayer]/[label] stay as raw values so
 * the consuming composable localizes them; [formattedTime] is pre-formatted by the emitter, which holds
 * the settings (locale, numbering system, 24-hour preference) needed to format it.
 *
 * [key] scopes snackbar replacement: a new signal only replaces a visible one with the same key (rapid
 * reschedules of the same thing), so an adhan never clobbers a reminder, distinct reminders never clobber
 * each other, and unrelated signals queue instead.
 */
sealed interface ScheduleFeedbackInfo {
    val key: String

    data class Adhan(
        val prayer: Prayer,
        val formattedTime: String,
    ) : ScheduleFeedbackInfo {
        override val key = "adhan"
    }

    data class Reminder(
        val label: String,
        val prayer: Prayer,
        // duration/durationModifier let the UI build a "N min before/after <prayer>" name when label is blank.
        val duration: Int,
        val durationModifier: Int,
        val formattedTime: String,
    ) : ScheduleFeedbackInfo {
        override val key = REMINDER_KEY
    }

    /** Several reminders rescheduled at once (e.g. a location/calculation change shifting them all). */
    data class ReminderBatch(
        val count: Int,
    ) : ScheduleFeedbackInfo {
        override val key = REMINDER_KEY
    }

    private companion object {
        // Shared by all reminder signals so any reminder replaces any other (only adhan stays separate).
        const val REMINDER_KEY = "reminder"
    }
}

/**
 * App-wide one-shot signal emitted each time the next adhan or a reminder is (re)scheduled because the
 * user changed a setting. Surfaced as a transient snackbar at the navigation root so users get immediate
 * feedback while adjusting parameters; rapid emissions replace the visible snackbar rather than queueing,
 * so the UI never lags behind nor spams.
 */
@Singleton
class ScheduleFeedback @Inject constructor() {
    // replay=0: a signal sent while no UI is collecting (background reschedule) is dropped, so no stale
    // snackbar appears later. DROP_OLDEST + buffer 1 keeps the latest of a rapid burst from a
    // non-suspending emitter.
    private val _events =
        MutableSharedFlow<ScheduleFeedbackInfo>(
            replay = 0,
            extraBufferCapacity = 1,
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
        )
    val events: SharedFlow<ScheduleFeedbackInfo> = _events.asSharedFlow()

    fun notify(info: ScheduleFeedbackInfo) {
        _events.tryEmit(info)
    }
}
