package com.github.meypod.al_azan.main.upcoming_alarms

import com.github.meypod.al_azan.core.domain.model.alarm.SkippedAlarm

sealed interface UpcomingAlarmsUiAction {
    /** Skip [occurrence]; the scheduler arms the next non-skipped firing instead. */
    data class OnSkip(
        val occurrence: SkippedAlarm,
    ) : UpcomingAlarmsUiAction

    /** Undo the skip of [occurrence], re-arming it. */
    data class OnReschedule(
        val occurrence: SkippedAlarm,
    ) : UpcomingAlarmsUiAction
}
