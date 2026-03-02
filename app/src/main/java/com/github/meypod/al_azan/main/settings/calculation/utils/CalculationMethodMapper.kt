package com.github.meypod.al_azan.main.settings.calculation.utils

import android.content.Context
import com.github.meypod.al_azan.R
import io.github.meypod.adhan_kotlin.CalculationMethod

fun CalculationMethod.i18n(context: Context) =
    when (this) {
        CalculationMethod.OTHER -> context.getString(R.string.calc_method_other)
        CalculationMethod.MOON_SIGHTING_COMMITTEE -> context.getString(R.string.calc_method_moonsighting_committee)
        CalculationMethod.MUSLIM_WORLD_LEAGUE -> context.getString(R.string.calc_method_muslim_world_league)
        CalculationMethod.EGYPTIAN -> context.getString(R.string.calc_method_egyptian)
        CalculationMethod.KARACHI -> context.getString(R.string.calc_method_karachi)
        CalculationMethod.UMM_AL_QURA -> context.getString(R.string.calc_method_umm_al_qura)
        CalculationMethod.NORTH_AMERICA -> context.getString(R.string.calc_method_north_america)
        CalculationMethod.GULF -> context.getString(R.string.calc_method_gulf)
        CalculationMethod.DUBAI -> context.getString(R.string.calc_method_dubai)
        CalculationMethod.KUWAIT -> context.getString(R.string.calc_method_kuwait)
        CalculationMethod.QATAR -> context.getString(R.string.calc_method_qatar)
        CalculationMethod.SINGAPORE -> context.getString(R.string.calc_method_singapore)
        CalculationMethod.FRANCE -> context.getString(R.string.calc_method_france)
        CalculationMethod.FRANCE15 -> context.getString(R.string.calc_method_france15)
        CalculationMethod.FRANCE18 -> context.getString(R.string.calc_method_france18)
        CalculationMethod.TURKEY -> context.getString(R.string.calc_method_turkey)
        CalculationMethod.RUSSIA -> context.getString(R.string.calc_method_russia)
        CalculationMethod.JAFARI -> context.getString(R.string.calc_method_jafari)
        CalculationMethod.TEHRAN -> context.getString(R.string.calc_method_tehran)
        CalculationMethod.KEMENAG -> context.getString(R.string.calc_method_kemenag)
        CalculationMethod.ALGERIA -> context.getString(R.string.calc_method_algeria)
        CalculationMethod.BRUNEI -> context.getString(R.string.calc_method_brunei)
        CalculationMethod.TUNISIA -> context.getString(R.string.calc_method_tunisia)
    }
