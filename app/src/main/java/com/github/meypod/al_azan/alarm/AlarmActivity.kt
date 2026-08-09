package com.github.meypod.al_azan.alarm

import android.content.Context
import android.content.Intent
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
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * Full-screen alarm shown while an adhan or reminder sounds. Shows over the lock screen and turns the
 * screen on, hosting [AlarmFullscreenScreen]. Dismiss/snooze run through [AlarmFullscreenViewModel]
 * (which delegates to the firing handler) and finish the activity.
 *
 * Carries no extras: it renders whichever alarm is sounding, read from
 * [com.github.meypod.al_azan.playback.PlaybackService.activeAlarm]. That keeps it correct when a second
 * alarm replaces the first while this screen is up — a `singleTask` activity is reused rather than
 * recreated, so extras baked in at launch would go stale.
 */
@AndroidEntryPoint
class AlarmActivity : AppCompatActivity() {

    companion object {
        fun intent(context: Context): Intent =
            Intent(context, AlarmActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
    }

    private val viewModel: AlarmFullscreenViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        showOverLockScreen()
        // Back must not make a sounding alarm go away: it would leave the adhan playing with its screen
        // gone. Only an explicit Dismiss (here or on the notification) ends it. Enabled only while an
        // alarm is actually sounding, so this screen can never trap the user with nothing to dismiss.
        val backBlocker = onBackPressedDispatcher.addCallback(this, enabled = false) { }

        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            AlAzanTheme(uiState.themeColor) {
                AlarmFullscreenScreen(
                    uiState = uiState,
                    onAction = viewModel::onAction,
                )
            }
        }

        // Close when nothing is sounding any more, whatever ended it: this screen's own Dismiss, the
        // notification's, a volume-button press, or the sound finishing. Scoped to CREATED, not STARTED:
        // with the screen off the activity sits STOPPED behind the lock screen, and a STARTED-only
        // collector would leave a stale alarm screen for the user to find when the screen wakes.
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                viewModel.isSounding.collect { sounding ->
                    backBlocker.isEnabled = sounding
                    if (!sounding) finish()
                }
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
