package com.github.meypod.al_azan.core.presentation

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.res.stringResource

sealed interface UiText {

    @Immutable
    data class DynamicString(
        val value: String,
    ) : UiText

    @Immutable
    class StringResourceId(
        @param:StringRes val id: Int,
        vararg val formatArgs: Any = arrayOf(),
    ) : UiText

    @Composable
    fun asString(): String =
        when (this) {
            is DynamicString -> value

            is StringResourceId -> if (formatArgs.isNotEmpty()) {
                stringResource(id, *formatArgs)
            } else {
                stringResource(id)
            }
        }
}
