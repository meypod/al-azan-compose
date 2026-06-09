package com.github.meypod.al_azan.main.upcoming_alarms

sealed interface UpcomingAlarmsUiAction {
    /** Skip the armed alarm with [id]; the following occurrence fires instead. */
    data class OnSkip(
        val id: String,
    ) : UpcomingAlarmsUiAction

    /** Undo the skip of the [id] occurrence at [fireTimeMs], re-arming the original next firing. */
    data class OnReschedule(
        val id: String,
        val fireTimeMs: Long,
    ) : UpcomingAlarmsUiAction
}
