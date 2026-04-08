package com.github.meypod.al_azan.core.presentation.navigation.deeplink

import android.net.Uri
import com.github.meypod.al_azan.core.domain.model.navigation.DeepLinkableRoute
import com.github.meypod.al_azan.core.presentation.navigation.Route

private const val PATH_BASE = "al-azan://"

fun getDeepLinkUriString(route: DeepLinkableRoute): String = PATH_BASE + route::class.simpleName

internal fun parseUriToRoute(
    uri: Uri?,
    deepLinkPatterns: List<DeepLinkPattern<out Route>>,
): Route? =
    uri?.let {
        val request = DeepLinkRequest(uri)
        val match = deepLinkPatterns.firstNotNullOfOrNull { pattern ->
            DeepLinkMatcher(request, pattern).match()
        }
        match?.let {
            KeyDecoder(match.args)
                .decodeSerializableValue(match.serializer)
        }
    }
