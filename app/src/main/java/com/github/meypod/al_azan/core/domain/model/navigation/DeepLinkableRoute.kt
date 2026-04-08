package com.github.meypod.al_azan.core.domain.model.navigation

import com.github.meypod.al_azan.core.presentation.navigation.deeplink.getDeepLinkUriString

interface DeepLinkableRoute {
    fun toUriString(): String = getDeepLinkUriString(this)
}
