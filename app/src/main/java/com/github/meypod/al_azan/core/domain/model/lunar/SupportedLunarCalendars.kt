package com.github.meypod.al_azan.core.domain.model.lunar

import android.content.res.Resources
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

fun SupportedLunarCalendars.i18n(resources: Resources): String =
    when (this) {
        SupportedLunarCalendars.Islamic -> resources.getString(R.string.lunar_islamic)
        SupportedLunarCalendars.IslamicUmmAlQura -> resources.getString(R.string.lunar_islamic_ummalqura)
        SupportedLunarCalendars.IslamicTabular -> resources.getString(R.string.lunar_islamic_tabular)
        SupportedLunarCalendars.IslamicCivil -> resources.getString(R.string.lunar_islamic_civil)
        SupportedLunarCalendars.IslamicSaudiArabiaSighting -> resources.getString(R.string.lunar_islamic_saudi_sighting)
    }
