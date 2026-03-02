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
) {
    @SerialName("fajr")
    Fajr(R.string.fajr),

    @SerialName("sunrise")
    Sunrise(R.string.sunrise),

    @SerialName("dhuhr")
    Dhuhr(R.string.dhuhr),

    @SerialName("asr")
    Asr(R.string.asr),

    @SerialName("sunset")
    Sunset(R.string.sunset),

    @SerialName("maghrib")
    Maghrib(R.string.maghrib),

    @SerialName("isha")
    Isha(R.string.isha),

    /** middle of the night */
    @SerialName("midnight")
    Midnight(R.string.midnight),

    /** last third of the night */
    @SerialName("tahajjud")
    Tahajjud(R.string.tahajjud),
}

@Composable
fun Prayer.i18n() = stringResource(stringRes)

val PRAYERS_IN_ORDER =
    listOf<Prayer>(
        Prayer.Fajr,
        Prayer.Sunrise,
        Prayer.Dhuhr,
        Prayer.Asr,
        Prayer.Sunset,
        Prayer.Maghrib,
        Prayer.Isha,
        Prayer.Midnight,
        Prayer.Tahajjud,
    )

val NON_PRAYERS_IN_ORDER =
    listOf<Prayer>(
        Prayer.Sunrise,
        Prayer.Sunset,
        Prayer.Midnight,
        Prayer.Tahajjud,
    )
