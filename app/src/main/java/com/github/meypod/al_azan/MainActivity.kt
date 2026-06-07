package com.github.meypod.al_azan

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.github.meypod.al_azan.core.domain.repository.SettingsRepository
import com.github.meypod.al_azan.core.presentation.AlAzanTheme
import com.github.meypod.al_azan.core.presentation.navigation.NavigationRoot
import com.github.meypod.al_azan.core.presentation.navigation.deepLinkPatterns
import com.github.meypod.al_azan.core.presentation.navigation.deeplink.parseUriToRoute
import com.github.meypod.al_azan.di.LanguageSync
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var languageSync: LanguageSync

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val initialSettings = runBlocking { settingsRepository.fetch() }

        val startingRoute = intent.data?.let {
            intent = null // consume
            // MainActivity is exported, so the URI is attacker-reachable; a malformed deep link must
            // not crash launch. Fall back to the default start destination on any parse failure.
            runCatching { parseUriToRoute(it, deepLinkPatterns) }.getOrNull()
        }

        setContent {
            LaunchedEffect(Unit) {
                // ensures our view of settings stays up to date with user's selected language
                // in case they decide to change it outside the app
                languageSync.run()
            }
            val settings by settingsRepository.data.collectAsState(initial = initialSettings)

            AlAzanTheme(settings.themeColor) {
                NavigationRoot(
                    appIntroDone = initialSettings.appIntroDone,
                    startingRoute = startingRoute,
                )
            }
        }
    }
}
