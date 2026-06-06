package com.github.meypod.al_azan.main.settings.calculation.utils

import android.content.res.Resources
import com.github.meypod.al_azan.R
import io.github.meypod.adhan_kotlin.CalculationMethod
import io.github.meypod.adhan_kotlin.CalculationParameters

fun CalculationMethod.i18n(resources: Resources) =
    when (this) {
        CalculationMethod.OTHER -> resources.getString(R.string.calc_method_other)
        CalculationMethod.MOON_SIGHTING_COMMITTEE -> resources.getString(R.string.calc_method_moonsighting_committee)
        CalculationMethod.MUSLIM_WORLD_LEAGUE -> resources.getString(R.string.calc_method_muslim_world_league)
        CalculationMethod.EGYPTIAN -> resources.getString(R.string.calc_method_egyptian)
        CalculationMethod.KARACHI -> resources.getString(R.string.calc_method_karachi)
        CalculationMethod.UMM_AL_QURA -> resources.getString(R.string.calc_method_umm_al_qura)
        CalculationMethod.NORTH_AMERICA -> resources.getString(R.string.calc_method_north_america)
        CalculationMethod.GULF -> resources.getString(R.string.calc_method_gulf)
        CalculationMethod.DUBAI -> resources.getString(R.string.calc_method_dubai)
        CalculationMethod.KUWAIT -> resources.getString(R.string.calc_method_kuwait)
        CalculationMethod.QATAR -> resources.getString(R.string.calc_method_qatar)
        CalculationMethod.SINGAPORE -> resources.getString(R.string.calc_method_singapore)
        CalculationMethod.FRANCE -> resources.getString(R.string.calc_method_france)
        CalculationMethod.FRANCE15 -> resources.getString(R.string.calc_method_france15)
        CalculationMethod.FRANCE18 -> resources.getString(R.string.calc_method_france18)
        CalculationMethod.TURKEY -> resources.getString(R.string.calc_method_turkey)
        CalculationMethod.RUSSIA -> resources.getString(R.string.calc_method_russia)
        CalculationMethod.JAFARI -> resources.getString(R.string.calc_method_jafari)
        CalculationMethod.TEHRAN -> resources.getString(R.string.calc_method_tehran)
        CalculationMethod.KEMENAG -> resources.getString(R.string.calc_method_kemenag)
        CalculationMethod.ALGERIA -> resources.getString(R.string.calc_method_algeria)
        CalculationMethod.BRUNEI -> resources.getString(R.string.calc_method_brunei)
        CalculationMethod.TUNISIA -> resources.getString(R.string.calc_method_tunisia)
    }

/**
 * True when the editable parameters (Fajr/Isha/Maghrib angles and Isha interval) diverge from the
 * canonical values of the selected [CalculationMethod].
 */
fun CalculationParameters.isMethodModified(): Boolean {
    val base = method.parameters
    return fajrAngle != base.fajrAngle ||
        ishaAngle != base.ishaAngle ||
        ishaInterval != base.ishaInterval ||
        maghribAngle != base.maghribAngle
}
