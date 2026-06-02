package com.github.meypod.al_azan.main.counter

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.github.meypod.al_azan.R
import com.github.meypod.al_azan.core.domain.model.counter.COUNTER_ID_ASR
import com.github.meypod.al_azan.core.domain.model.counter.COUNTER_ID_DHUHR
import com.github.meypod.al_azan.core.domain.model.counter.COUNTER_ID_FAJR
import com.github.meypod.al_azan.core.domain.model.counter.COUNTER_ID_FAST
import com.github.meypod.al_azan.core.domain.model.counter.COUNTER_ID_ISHA
import com.github.meypod.al_azan.core.domain.model.counter.COUNTER_ID_MAGHRIB
import com.github.meypod.al_azan.core.domain.model.counter.Counter

@Composable
fun counterDisplayLabel(counter: Counter): String {
    counter.label?.takeIf { it.isNotBlank() }?.let { return it }
    val res = when (counter.id) {
        COUNTER_ID_FAJR -> R.string.fajr
        COUNTER_ID_DHUHR -> R.string.dhuhr
        COUNTER_ID_ASR -> R.string.asr
        COUNTER_ID_MAGHRIB -> R.string.maghrib
        COUNTER_ID_ISHA -> R.string.isha
        COUNTER_ID_FAST -> R.string.counter_fast
        else -> return counter.id
    }
    return stringResource(res)
}
