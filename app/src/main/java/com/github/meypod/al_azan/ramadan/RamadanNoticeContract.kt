package com.github.meypod.al_azan.ramadan

/** Shared identifiers for the background Ramadan-accuracy notice. */
object RamadanNoticeContract {
    /** Broadcast action: the daily check alarm fired — decide whether to post the notice. */
    const val ACTION_RAMADAN_CHECK = "com.github.meypod.al_azan.action.RAMADAN_CHECK"

    /** Broadcast action: "Remind me next year" button — suppress for the current Hijri year. */
    const val ACTION_RAMADAN_REMIND_NEXT_YEAR = "com.github.meypod.al_azan.action.RAMADAN_REMIND_NEXT_YEAR"

    /** Broadcast action: "Don't show again" button — suppress permanently. */
    const val ACTION_RAMADAN_DONT_SHOW_AGAIN = "com.github.meypod.al_azan.action.RAMADAN_DONT_SHOW_AGAIN"

    /** Tracked alarm id for the daily check. */
    const val CHECK_ALARM_ID = "ramadan_notice_check"

    /** Notification id for the posted notice. */
    const val NOTIFICATION_ID = "ramadan_notice_notification"

    /** Local hour of day (24h) at which the daily check runs. */
    const val CHECK_HOUR = 9
}
