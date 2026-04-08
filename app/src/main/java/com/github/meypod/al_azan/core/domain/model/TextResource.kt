package com.github.meypod.al_azan.core.domain.model

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable

sealed interface TextResource {

    @Immutable
    data class StringResId(
        @param:StringRes val id: Int,
    ) : TextResource

    @Immutable
    class StringResIdWithArgs(
        @param:StringRes val id: Int,
        vararg val formatArgs: Any = arrayOf(),
    ) : TextResource {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as StringResIdWithArgs

            if (id != other.id) return false
            if (!formatArgs.contentEquals(other.formatArgs)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = id
            result = 31 * result + formatArgs.contentHashCode()
            return result
        }
    }

    @Immutable
    data class Literal(
        val value: String,
    ) : TextResource
}
