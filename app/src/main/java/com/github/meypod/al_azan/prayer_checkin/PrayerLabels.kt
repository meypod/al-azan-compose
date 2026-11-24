package com.github.meypod.al_azan.prayer_checkin

import androidx.annotation.StringRes
import com.github.meypod.al_azan.R
import com.github.meypod.al_azan.core.domain.model.adhan.Prayer

/**
 * Name to show when asking about, logging or counting a prayer. For Fajr this is the prayer itself
 * rather than the time name (in Turkish "Imsak" is the start of the fasting day, the prayer is
 * "Sabah namazi").
 */
@get:StringRes
val Prayer.performedLabelRes: Int
    get() = if (this == Prayer.Fajr) R.string.fajr_prayer else stringRes
