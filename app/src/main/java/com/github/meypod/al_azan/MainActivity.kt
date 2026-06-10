package com.github.meypod.al_azan

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.github.meypod.al_azan.core.domain.repository.SettingsRepository
import com.github.meypod.al_azan.core.presentation.AlAzanTheme
import com.github.meypod.al_azan.core.presentation.navigation.NavigationController
import com.github.meypod.al_azan.core.presentation.navigation.NavigationRoot
import com.github.meypod.al_azan.core.presentation.navigation.Route
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
        // Reconcile the app locale with stored settings before composing: applies the migrated/selected
        // language and its layout direction (RTL). Done here (not Application.onCreate) so it lands at
        // the lifecycle point where autoStoreLocales actually persists setApplicationLocales.
        val initialSettings = runBlocking {
            languageSync.reconcile()
            settingsRepository.fetch()
        }

        val startingRoute = routeFromIntent(intent)
        intent = null // consume

        setContent {
            val settings by settingsRepository.data.collectAsState(initial = initialSettings)

            AlAzanTheme(settings.themeColor) {
                NavigationRoot(
                    appIntroDone = initialSettings.appIntroDone,
                    startingRoute = startingRoute,
                )
            }
        }
    }

    // A deep link / DND-rule tap that arrives while the Activity is already running comes here (the
    // PendingIntent is SINGLE_TOP | CLEAR_TOP), not through onCreate — route it onto the live backstack.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        routeFromIntent(intent)?.let { NavigationController.navigateTo(it) }
    }

    // MainActivity is exported, so the URI is attacker-reachable; a malformed deep link must not crash
    // launch. Fall back to the default start destination on any parse failure.
    private fun routeFromIntent(launchIntent: Intent?): Route? =
        launchIntent?.data?.let { runCatching { parseUriToRoute(it, deepLinkPatterns) }.getOrNull() }
}
