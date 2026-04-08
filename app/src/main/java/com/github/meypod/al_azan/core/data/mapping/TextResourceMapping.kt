package com.github.meypod.al_azan.core.data.mapping

import android.content.Context
import com.github.meypod.al_azan.core.domain.model.TextResource

fun TextResource.asString(context: Context) =
    when (this) {
        is TextResource.Literal -> value
        is TextResource.StringResId -> context.getString(id)
        is TextResource.StringResIdWithArgs -> context.getString(id, *this.formatArgs)
    }
