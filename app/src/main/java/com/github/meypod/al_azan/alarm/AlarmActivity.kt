package com.github.meypod.al_azan.alarm

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.github.meypod.al_azan.core.presentation.AlAzanTheme
import com.github.meypod.al_azan.playback.PlaybackService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * Full-screen alarm shown when an adhan with sound fires (and the screen-on option is enabled). Shows
 * over the lock screen and turns the screen on, hosting [AlarmFullscreenScreen]. Dismiss/snooze run
 * through [AlarmFullscreenViewModel] (which delegates to the firing handler) and finish the activity.
 */
@AndroidEntryPoint
class AlarmActivity : AppCompatActivity() {

    private val viewModel: AlarmFullscreenViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        showOverLockScreen()
        // Back must not make a sounding alarm go away: it would leave the adhan playing with its screen
        // gone. Only an explicit Dismiss (here or on the notification) ends it, and the stop collector
        // below closes this screen when it does.
        onBackPressedDispatcher.addCallback(this) { }

        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            AlAzanTheme(uiState.themeColor) {
                AlarmFullscreenScreen(
                    uiState = uiState,
                    onAction = viewModel::onAction,
                )
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.finish.collect { finish() }
            }
        }

        // Close the full-screen alarm when playback stops elsewhere (notification "Dismiss", a volume-button
        // press, natural end). Scoped to CREATED, not STARTED: with the screen off the activity sits STOPPED
        // behind the lock screen, and a STARTED-only collector would miss the stop (SharedFlow, no replay),
        // leaving a stale alarm screen when the screen wakes. CREATED stays subscribed the whole lifetime.
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                PlaybackService.stopSignal.collect { finish() }
            }
        }
    }

    private fun showOverLockScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON,
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
}
