package com.github.meypod.al_azan.reminder

/** Shared identifiers for reminder alarms (mirrors AdhanContract). */
object ReminderContract {
    /** Broadcast action for a fired reminder alarm. */
    const val ACTION_REMINDER = "com.github.meypod.al_azan.action.REMINDER_ALARM"

    /** Broadcast action for the pre-reminder ("upcoming") notification. */
    const val ACTION_PRE_REMINDER = "com.github.meypod.al_azan.action.PRE_REMINDER_ALARM"

    /** Broadcast action: cancel an upcoming reminder (from the pre-reminder's Cancel button). */
    const val ACTION_CANCEL_REMINDER = "com.github.meypod.al_azan.action.CANCEL_REMINDER"

    /** Per-reminder alarm/notification id prefixes (one scheduled alarm per enabled reminder). */
    const val ALARM_ID_PREFIX = "reminder_alarm_"
    const val PRE_ALARM_ID_PREFIX = "reminder_prealarm_"
    const val NOTIFICATION_ID_PREFIX = "reminder_notification_"
    const val PRE_NOTIFICATION_ID_PREFIX = "reminder_pre_notification_"

    const val EXTRA_REMINDER_ID = "reminder_id"
    const val EXTRA_TIMESTAMP = "reminder_timestamp"

    fun alarmId(reminderId: String) = "$ALARM_ID_PREFIX$reminderId"

    fun preAlarmId(reminderId: String) = "$PRE_ALARM_ID_PREFIX$reminderId"

    fun notificationId(reminderId: String) = "$NOTIFICATION_ID_PREFIX$reminderId"

    fun preNotificationId(reminderId: String) = "$PRE_NOTIFICATION_ID_PREFIX$reminderId"
}
