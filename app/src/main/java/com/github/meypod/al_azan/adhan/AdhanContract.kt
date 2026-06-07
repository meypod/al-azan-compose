package com.github.meypod.al_azan.adhan

/** Shared identifiers for adhan (prayer-time) alarms. */
object AdhanContract {
    /** Broadcast action for a fired adhan alarm. */
    const val ACTION_ADHAN = "com.github.meypod.al_azan.action.ADHAN_ALARM"

    /** Broadcast action for the pre-adhan ("upcoming") reminder. */
    const val ACTION_PRE_ADHAN = "com.github.meypod.al_azan.action.PRE_ADHAN_ALARM"

    /** Broadcast action: cancel the upcoming adhan (from the pre-alarm's Cancel button). */
    const val ACTION_CANCEL_ADHAN = "com.github.meypod.al_azan.action.CANCEL_ADHAN"

    /** Broadcast action: the "Dismiss & silent" DND window ended — restore the interruption filter. */
    const val ACTION_UNSILENCE = "com.github.meypod.al_azan.action.UNSILENCE"

    /** Broadcast action: a "remind me later" timer elapsed — post the follow-up reminder notification. */
    const val ACTION_ADHAN_REMIND = "com.github.meypod.al_azan.action.ADHAN_REMIND"

    /** Tracked alarm ids (single next-adhan + its pre-alarm + a remind-later timer). */
    const val ADHAN_ALARM_ID = "adhan_alarm"
    const val PRE_ADHAN_ALARM_ID = "pre_adhan_alarm"
    const val REMIND_ALARM_ID = "adhan_remind_alarm"
    const val UNSILENCE_ALARM_ID = "adhan_unsilence_alarm"
    const val DEV_TEST_ALARM_ID = "adhan_dev_test_alarm"

    /** Notification ids. ADHAN id also keys [deliveredAlarmTimestamps] to avoid re-firing the same prayer. */
    const val ADHAN_NOTIFICATION_ID = "adhan_notification"
    const val PRE_ADHAN_NOTIFICATION_ID = "pre_adhan_notification"
    const val REMIND_NOTIFICATION_ID = "adhan_remind_notification"

    /** Posted when "silence after dismiss" couldn't run because DND access was revoked. */
    const val DND_REVOKED_NOTIFICATION_ID = "adhan_dnd_revoked_notification"

    /** Extras carried by the alarm broadcast / playback intents. */
    const val EXTRA_PLAY_SOUND = "adhan_play_sound"
    const val EXTRA_TIMESTAMP = "adhan_timestamp"
    const val EXTRA_REMIND_MINUTES = "adhan_remind_minutes"

    /** "Remind me later" durations offered on the full-screen alarm. */
    const val SHORT_REMIND_MINUTES = 15
    const val LONG_REMIND_MINUTES = 30

    /** How long "Dismiss & silent" suppresses adhan alarms. */
    const val DISMISS_SILENT_MINUTES = 30
}
