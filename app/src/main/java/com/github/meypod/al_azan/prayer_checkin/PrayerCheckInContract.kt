package com.github.meypod.al_azan.prayer_checkin

import com.github.meypod.al_azan.core.domain.model.adhan.Prayer

object PrayerCheckInContract {
    const val ACTION_CHECKIN_PROMPT = "com.github.meypod.al_azan.action.PRAYER_CHECKIN_PROMPT"
    const val ACTION_CHECKIN_EXPIRED = "com.github.meypod.al_azan.action.PRAYER_CHECKIN_EXPIRED"
    const val ACTION_CHECKIN_PRAYED = "com.github.meypod.al_azan.action.PRAYER_CHECKIN_PRAYED"
    const val ACTION_CHECKIN_NOT_YET = "com.github.meypod.al_azan.action.PRAYER_CHECKIN_NOT_YET"

    const val EXTRA_PRAYER = "extra_prayer"
    const val EXTRA_DATE = "extra_date"
    const val EXTRA_TIMESTAMP = "extra_timestamp"

    const val PROMPT_ALARM_ID = "prayer_checkin_prompt"
    const val DELIVERED_KEY = "prayer_checkin_prompt_delivered"

    private const val NOTIFICATION_ID_PREFIX = "prayer_checkin_"
    private const val EXPIRED_ALARM_ID_PREFIX = "prayer_checkin_expired_"

    fun notificationId(prayer: Prayer): String = "$NOTIFICATION_ID_PREFIX${prayer.name}"

    fun expiredAlarmId(prayer: Prayer): String = "$EXPIRED_ALARM_ID_PREFIX${prayer.name}"
}
