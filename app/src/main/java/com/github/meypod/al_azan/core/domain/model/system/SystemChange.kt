package com.github.meypod.al_azan.core.domain.model.system

import androidx.compose.runtime.Immutable

sealed interface SystemChange {

    /**
     * initial value for shared flow
     */
    @Immutable
    object Nothing : SystemChange

    @Immutable
    object TimeChanged : SystemChange

    @Immutable
    data class TimeZoneChanged(
        val newZoneId: String,
    ) : SystemChange
}
