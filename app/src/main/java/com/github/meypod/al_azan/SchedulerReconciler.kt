package com.github.meypod.al_azan

import com.github.meypod.al_azan.adhan.AdhanScheduler
import com.github.meypod.al_azan.ramadan.RamadanNoticeScheduler
import com.github.meypod.al_azan.reminder.ReminderScheduler
import com.github.meypod.al_azan.widget.WidgetUpdater
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Re-derives every scheduled alarm from the current settings + permission state. Used by the events
 * that invalidate the platform's alarm set or its scheduling capability: boot, time/timezone change,
 * and exact-alarm permission changes.
 *
 * Because [com.github.meypod.al_azan.core.data.repository.AlarmRepositoryImpl] picks the alarm type at
 * registration (exact when allowed, inexact otherwise), re-running the schedulers here reconciles
 * out-of-order permission grants/revokes: a later exact-alarm grant upgrades the alarms to exact, and a
 * revoke degrades them to inexact — no matter which permission the user toggled first.
 */
@Singleton
class SchedulerReconciler @Inject constructor(
    private val adhanScheduler: AdhanScheduler,
    private val reminderScheduler: ReminderScheduler,
    private val ramadanNoticeScheduler: RamadanNoticeScheduler,
    private val widgetUpdater: WidgetUpdater,
) {
    suspend fun reconcileAll() {
        adhanScheduler.schedule()
        reminderScheduler.schedule()
        ramadanNoticeScheduler.schedule()
        widgetUpdater.update()
    }
}
