package com.github.meypod.al_azan.core.presentation.feedback

import android.content.res.Resources
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalResources
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.github.meypod.al_azan.R
import com.github.meypod.al_azan.core.presentation.components.LocalSnackbarController
import com.github.meypod.al_azan.core.presentation.components.SnackbarController
import com.github.meypod.al_azan.core.presentation.mapper.reminderDisplayName
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Collects app-wide schedule feedback and surfaces it as root-level snackbars.
 */
@Composable
internal fun ObserveScheduleFeedback(
    scheduleFeedbackViewModel: ScheduleFeedbackViewModel = hiltViewModel(),
    snackbarController: SnackbarController = LocalSnackbarController.current,
) {
    CollectScheduleFeedback(
        scheduleFeedbackViewModel = scheduleFeedbackViewModel,
        snackbarController = snackbarController,
    )
}

@Composable
private fun CollectScheduleFeedback(
    scheduleFeedbackViewModel: ScheduleFeedbackViewModel,
    snackbarController: SnackbarController,
) {
    val resources = LocalResources.current

    LaunchedEffect(scheduleFeedbackViewModel, snackbarController, resources) {
        // Replace only within the same key: rapid reschedules of one thing (dragging a parameter)
        // collapse to the latest, while distinct signals (adhan vs a reminder, two reminders) queue
        // so neither clobbers the other.
        var shownKey: String? = null
        var shownIsAdjustment = false
        var showJob: Job? = null

        scheduleFeedbackViewModel.rescheduled.collect { info ->
            val active = showJob?.isActive == true
            when {
                // Live adjustment feedback owns the snackbar while the user tunes: a new adjustment
                // always preempts whatever is showing so each edit lands instantly, never waiting a
                // full snackbar duration behind an unrelated signal.
                info is ScheduleFeedbackInfo.Adjustment -> if (active) showJob?.cancel()

                // The reschedule echoes the same edit triggers (adhan/reminder shifting with the prayer)
                // are redundant while an adjustment is on screen - drop them so they don't clobber the
                // value the user is watching.
                active && shownIsAdjustment -> return@collect

                // Same key still on screen -> cancel its show() (which dismisses it) and reshow.
                info.key == shownKey && active -> showJob?.cancel()

                // Different key -> let the current one finish so it isn't overwritten.
                else -> showJob?.join()
            }

            shownKey = info.key
            shownIsAdjustment = info is ScheduleFeedbackInfo.Adjustment
            showJob = launch { snackbarController.show(info.message(resources)) }
        }
    }
}

private fun ScheduleFeedbackInfo.message(resources: Resources): String =
    when (this) {
        is ScheduleFeedbackInfo.Adhan ->
            resources.getString(
                R.string.prayer_times_rescheduled,
                resources.getString(prayer.stringRes),
                formattedTime,
            )

        is ScheduleFeedbackInfo.PrayerAdjusted ->
            resources.getString(
                R.string.prayer_time_adjusted,
                resources.getString(prayer.stringRes),
                formattedTime,
            )

        // Already a fully formatted Hijri date (calendar, locale, numbering baked in) - shown as-is.
        is ScheduleFeedbackInfo.HijriDateAdjusted -> formattedDate

        is ScheduleFeedbackInfo.Reminder ->
            resources.getString(
                R.string.reminder_rescheduled,
                reminderDisplayName(resources, label, duration, durationModifier, prayer),
                formattedTime,
            )

        is ScheduleFeedbackInfo.ReminderBatch ->
            resources.getString(R.string.reminders_rescheduled_batch, count)
    }
