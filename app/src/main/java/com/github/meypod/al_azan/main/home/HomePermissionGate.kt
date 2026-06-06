package com.github.meypod.al_azan.main.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import com.github.meypod.al_azan.R
import com.github.meypod.al_azan.core.presentation.dialog.PermissionStep
import com.github.meypod.al_azan.core.presentation.dialog.SchedulingPermission
import com.github.meypod.al_azan.core.presentation.dialog.isSchedulingPermissionGranted
import com.github.meypod.al_azan.core.presentation.dialog.rememberSchedulingPermissionRequest

/** Inputs for the home permission re-check: which scheduled features are active. */
data class HomePermissionCheck(
    val adhanScheduled: Boolean,
    val hasScheduledAlarms: Boolean,
)

/**
 * On home load, re-requests the permissions that active schedules need but have lost (revoked).
 * Uses the shared scheduling permission flow with "don't ask again" enabled (home-only). When the
 * exact-alarm permission is resolved, tracked alarms are rescheduled (granted) or cleaned up (denied).
 *
 * Renders nothing itself; the flow overlays its own dialogs and snackbars.
 */
@Composable
fun HomePermissionGate(
    isDontAskAgain: (SchedulingPermission) -> Boolean,
    onDontAskAgain: (SchedulingPermission) -> Unit,
    onReschedule: () -> Unit,
    onCleanup: () -> Unit,
    getCheck: suspend () -> HomePermissionCheck,
) {
    val context = LocalContext.current
    val request = rememberSchedulingPermissionRequest(
        isDontAskAgain = isDontAskAgain,
        onDontAskAgain = onDontAskAgain,
        allowDontAskAgain = true,
        onComplete = { results ->
            results.outcome(SchedulingPermission.ExactAlarm)?.let { outcome ->
                when {
                    outcome.granted -> onReschedule()
                    outcome.asked -> onCleanup() // denied via dialog/settings (a suppressed skip leaves them)
                }
            }
        },
    )

    LaunchedEffect(Unit) {
        val check = getCheck()
        val steps = buildList {
            if (check.adhanScheduled) {
                if (!isSchedulingPermissionGranted(context, SchedulingPermission.Notification)) {
                    add(
                        PermissionStep(
                            SchedulingPermission.Notification,
                            R.string.home_notification_permission_rationale,
                            R.string.adhan_notification_permission_denied_text,
                        ),
                    )
                }
                if (!isSchedulingPermissionGranted(context, SchedulingPermission.PhoneState)) {
                    add(
                        PermissionStep(
                            SchedulingPermission.PhoneState,
                            R.string.adhan_phone_state_permission_rationale,
                            R.string.adhan_phone_state_permission_denied_text,
                        ),
                    )
                }
            }
            if (check.hasScheduledAlarms && !isSchedulingPermissionGranted(context, SchedulingPermission.ExactAlarm)) {
                add(
                    PermissionStep(
                        SchedulingPermission.ExactAlarm,
                        R.string.home_exact_alarm_permission_rationale,
                        R.string.adhan_exact_alarm_permission_denied_text,
                    ),
                )
            }
        }
        if (steps.isNotEmpty()) request(steps)
    }
}
