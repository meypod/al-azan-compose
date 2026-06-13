package com.github.meypod.al_azan.core.data.mapping

import android.content.res.Resources
import com.github.meypod.al_azan.core.domain.model.TextResource

fun TextResource.asString(resources: Resources): String =
    when (this) {
        is TextResource.Literal -> value

        is TextResource.StringResId -> resources.getString(id)

        is TextResource.StringResIdWithArgs ->
            resources.getString(
                id,
                // A format arg may itself be a string resource (e.g. a prayer name inside a body
                // template); resolve it against the same (localized) resources.
                *formatArgs.map { if (it is TextResource) it.asString(resources) else it }.toTypedArray(),
            )
    }
