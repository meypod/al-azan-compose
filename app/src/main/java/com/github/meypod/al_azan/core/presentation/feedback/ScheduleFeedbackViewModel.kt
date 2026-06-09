package com.github.meypod.al_azan.core.presentation.feedback

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharedFlow
import javax.inject.Inject

/**
 * Exposes [ScheduleFeedback] to the navigation root composable via [hiltViewModel], so the app-wide
 * snackbar can react to reschedules without screens hoisting the signal themselves.
 */
@HiltViewModel
class ScheduleFeedbackViewModel @Inject constructor(
    scheduleFeedback: ScheduleFeedback,
) : ViewModel() {
    val rescheduled: SharedFlow<ScheduleFeedbackInfo> = scheduleFeedback.events
}
