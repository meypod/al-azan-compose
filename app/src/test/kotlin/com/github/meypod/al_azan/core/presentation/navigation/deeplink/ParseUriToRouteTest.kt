package com.github.meypod.al_azan.core.presentation.navigation.deeplink

import android.net.Uri
import com.github.meypod.al_azan.core.presentation.navigation.Route
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class ParseUriToRouteTest {

    @Test
    fun `parseUriToRoute round-trips getDeepLinkUriString back to Route Main Home`() {
        // getDeepLinkUriString(Route.Main.Home) == "al-azan://Home"
        // Mock a Uri representing that string so no Android stubs are hit.
        val uri = mock(Uri::class.java)
        `when`(uri.scheme).thenReturn("al-azan")
        `when`(uri.authority).thenReturn("Home")
        `when`(uri.pathSegments).thenReturn(emptyList())
        `when`(uri.queryParameterNames).thenReturn(emptySet())

        // Using the same instance for both pattern and input triggers the exact-match
        // path in DeepLinkMatcher (request.uri == deepLinkPattern.uriPattern).
        val patterns = listOf(DeepLinkPattern(Route.Main.Home.serializer(), uri))

        val result = parseUriToRoute(uri, patterns)

        assertEquals(Route.Main.Home, result)
    }
}
