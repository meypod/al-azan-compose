package com.github.meypod.al_azan

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.github.meypod.al_azan.core.domain.repository.SettingsRepository
import com.github.meypod.al_azan.core.presentation.AlAzanTheme
import com.github.meypod.al_azan.core.presentation.navigation.NavigationController
import com.github.meypod.al_azan.core.presentation.navigation.NavigationRoot
import com.github.meypod.al_azan.core.presentation.navigation.Route
import com.github.meypod.al_azan.core.presentation.navigation.deepLinkPatterns
import com.github.meypod.al_azan.core.presentation.navigation.deeplink.parseUriToRoute
import com.github.meypod.al_azan.di.LanguageSync
import com.github.meypod.al_azan.playback.PlaybackService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
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

            AlAzanTheme(settings.themeColor, settings.displayScale) {
                NavigationRoot(
                    appIntroDone = initialSettings.appIntroDone,
                    startingRoute = startingRoute,
                )
            }
        }

        openAlarmScreenWhileSounding()
    }

    /**
     * A sounding adhan or reminder takes over the app: whether it started while we were away or while the
     * user was mid-use, the alarm screen is what they need in front of them.
     *
     * Re-checked on every return to the foreground, so leaving the alarm screen without acting on it (Home,
     * a notification, the recents switcher) and coming back brings it up again. Dismissing or snoozing
     * retires the alarm, which is what stops this from re-opening what the user just dealt with.
     */
    private fun openAlarmScreenWhileSounding() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                PlaybackService.activeAlarm.collect { alarm ->
                    if (alarm != null) startActivity(PlaybackService.alarmActivityIntent(this@MainActivity, alarm))
                }
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
