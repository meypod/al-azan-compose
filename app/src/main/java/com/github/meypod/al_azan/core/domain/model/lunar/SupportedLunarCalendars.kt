package com.github.meypod.al_azan.core.domain.model.lunar

import android.content.Context
import com.github.meypod.al_azan.R

enum class SupportedLunarCalendars(
    val icuValue: String,
) {
    Islamic("islamic"),
    IslamicUmmAlQura("islamic-umalqura"),
    IslamicTabular("islamic-tbla"),
    IslamicCivil("islamic-civil"),
    IslamicSaudiArabiaSighting("islamic-rgsa"),
}

fun SupportedLunarCalendars.i18n(context: Context): String =
    when (this) {
        SupportedLunarCalendars.Islamic -> context.getString(R.string.lunar_islamic)
        SupportedLunarCalendars.IslamicUmmAlQura -> context.getString(R.string.lunar_islamic_ummalqura)
        SupportedLunarCalendars.IslamicTabular -> context.getString(R.string.lunar_islamic_tabular)
        SupportedLunarCalendars.IslamicCivil -> context.getString(R.string.lunar_islamic_civil)
        SupportedLunarCalendars.IslamicSaudiArabiaSighting -> context.getString(R.string.lunar_islamic_saudi_sighting)
    }
