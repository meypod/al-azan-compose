package com.github.meypod.al_azan.core.domain.model.prayer_log

import androidx.annotation.StringRes
import com.github.meypod.al_azan.R
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Recorded completion state for an obligatory prayer on a given day. */
@Serializable
enum class PrayerRecordStatus(
    @param:StringRes val stringRes: Int,
) {
    @SerialName("prayed")
    Prayed(R.string.prayer_status_prayed),

    @SerialName("missed")
    Missed(R.string.prayer_status_missed),
}
