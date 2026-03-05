package com.github.meypod.al_azan.core.domain.model.adhan

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.github.meypod.al_azan.R
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class Prayer(
    @param:StringRes val stringRes: Int,
    val isNonPrayer: Boolean = false,
) {
    @SerialName("fajr")
    Fajr(R.string.fajr),

    @SerialName("sunrise")
    Sunrise(R.string.sunrise, true),

    @SerialName("dhuhr")
    Dhuhr(R.string.dhuhr),

    @SerialName("asr")
    Asr(R.string.asr),

    @SerialName("sunset")
    Sunset(R.string.sunset, true),

    @SerialName("maghrib")
    Maghrib(R.string.maghrib),

    @SerialName("isha")
    Isha(R.string.isha),

    /** middle of the night */
    @SerialName("midnight")
    Midnight(R.string.midnight, true),

    /** last third of the night */
    @SerialName("tahajjud")
    Tahajjud(R.string.tahajjud, true),
}

@Composable
fun Prayer.i18n() = stringResource(stringRes)

val SHARIA_TIMES_IN_ORDER: List<Prayer> = Prayer.entries.toList()

val NON_PRAYERS_IN_ORDER: List<Prayer> =
    Prayer.entries.filter { it.isNonPrayer }
