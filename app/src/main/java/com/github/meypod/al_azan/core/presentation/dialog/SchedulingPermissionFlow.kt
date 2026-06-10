package com.github.meypod.al_azan.core.presentation.dialog

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.widget.Toast
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.core.app.ActivityCompat
import androidx.core.net.toUri
import com.github.meypod.al_azan.R
import com.github.meypod.al_azan.core.domain.model.settings.Settings
import com.github.meypod.al_azan.core.presentation.components.LocalSnackbarController
import com.github.meypod.al_azan.core.presentation.components.PermissionRationaleDialog
import kotlinx.coroutines.launch
import android.provider.Settings as AndroidSettings

/** Permissions a prayer-time feature may need. */
enum class SchedulingPermission {
    /** POST_NOTIFICATIONS (Android 13+), runtime. Required. */
    Notification,

    /** READ_PHONE_STATE, runtime. Optional — only for future adhan call interruption; never blocks. */
    PhoneState,

    /** Exact alarm scheduling (Android 12+), granted via a settings screen. Required. */
    ExactAlarm,

    /**
     * Full-screen intent (Android 14+), granted via a special-access settings screen. Optional —
     * lets the adhan alarm show over the lock screen; never blocks (sound still plays without it).
     */
    FullScreenIntent,

    /** Do Not Disturb policy access, granted via a special-access settings screen. */
    DndAccess,

    /**
     * Battery optimization exemption ("keep running in the background"), granted via a system dialog.
     * Optional — improves delivery reliability for background alarms; never blocks.
     */
    BatteryOptimization,
}

/** One permission to request, with the rationale/denied copy that fits the calling feature. */
@Immutable
data class PermissionStep(
    val permission: SchedulingPermission,
    @param:StringRes val rationale: Int,
    @param:StringRes val denied: Int,
)

@Immutable
data class PermissionOutcome(
    val granted: Boolean,
    /** Whether the user was actually prompted this run (vs already-granted or suppressed by flag). */
    val asked: Boolean,
)

@Immutable
data class PermissionResults(
    val outcomes: Map<SchedulingPermission, PermissionOutcome>,
) {
    fun outcome(permission: SchedulingPermission): PermissionOutcome? = outcomes[permission]

    fun granted(permission: SchedulingPermission): Boolean = outcomes[permission]?.granted == true

    /**
     * True when every requested REQUIRED permission is granted. The optional permissions
     * ([SchedulingPermission.isOptional]) degrade gracefully and never block.
     */
    fun requiredAllGranted(): Boolean = outcomes.all { (permission, outcome) -> permission.isOptional() || outcome.granted }
}

/**
 * Optional permissions: they degrade gracefully (never block enabling a feature), and so they also
 * offer/honor "don't ask again" even in the normal enable flow — unlike required permissions, which keep
 * asking until granted.
 */
fun SchedulingPermission.isOptional(): Boolean =
    this == SchedulingPermission.PhoneState ||
        this == SchedulingPermission.FullScreenIntent ||
        this == SchedulingPermission.BatteryOptimization

/** Reads the persisted "don't ask again" flag for a permission off [Settings]. */
fun Settings.isDontAskAgain(permission: SchedulingPermission): Boolean =
    when (permission) {
        SchedulingPermission.Notification -> dontAskPermissionNotifications
        SchedulingPermission.PhoneState -> dontAskPermissionPhoneState
        SchedulingPermission.ExactAlarm -> dontAskPermissionAlarm
        SchedulingPermission.FullScreenIntent -> dontAskPermissionFullScreenIntent
        SchedulingPermission.DndAccess -> dontAskPermissionDndAccess
        SchedulingPermission.BatteryOptimization -> dontAskPermissionBatteryOptimization
    }

/** Sets the persisted "don't ask again" flag for a permission. */
fun Settings.withDontAskAgain(permission: SchedulingPermission): Settings =
    when (permission) {
        SchedulingPermission.Notification -> copy(dontAskPermissionNotifications = true)
        SchedulingPermission.PhoneState -> copy(dontAskPermissionPhoneState = true)
        SchedulingPermission.ExactAlarm -> copy(dontAskPermissionAlarm = true)
        SchedulingPermission.FullScreenIntent -> copy(dontAskPermissionFullScreenIntent = true)
        SchedulingPermission.DndAccess -> copy(dontAskPermissionDndAccess = true)
        SchedulingPermission.BatteryOptimization -> copy(dontAskPermissionBatteryOptimization = true)
    }

/** Standard step lists. PhoneState (optional, call interruption) is requested for both adhan and reminders. */
object SchedulingPermissionSteps {
    val widget: List<PermissionStep> = listOf(
        PermissionStep(
            SchedulingPermission.Notification,
            R.string.notification_permission_rationale,
            R.string.notification_permission_denied_text,
        ),
        PermissionStep(
            SchedulingPermission.ExactAlarm,
            R.string.exact_alarm_permission_rationale,
            R.string.exact_alarm_permission_denied_text,
        ),
    )

    val adhan: List<PermissionStep> = listOf(
        PermissionStep(
            SchedulingPermission.Notification,
            R.string.adhan_notification_permission_rationale,
            R.string.adhan_notification_permission_denied_text,
        ),
        PermissionStep(
            SchedulingPermission.PhoneState,
            R.string.adhan_phone_state_permission_rationale,
            R.string.adhan_phone_state_permission_denied_text,
        ),
        PermissionStep(
            SchedulingPermission.ExactAlarm,
            R.string.adhan_exact_alarm_permission_rationale,
            R.string.adhan_exact_alarm_permission_denied_text,
        ),
        PermissionStep(
            SchedulingPermission.FullScreenIntent,
            R.string.adhan_full_screen_intent_permission_rationale,
            R.string.adhan_full_screen_intent_permission_denied_text,
        ),
    )

    val reminder: List<PermissionStep> = listOf(
        PermissionStep(
            SchedulingPermission.Notification,
            R.string.reminder_notification_permission_rationale,
            R.string.reminder_notification_permission_denied_text,
        ),
        PermissionStep(
            SchedulingPermission.PhoneState,
            R.string.reminder_phone_state_permission_rationale,
            R.string.reminder_phone_state_permission_denied_text,
        ),
        PermissionStep(
            SchedulingPermission.ExactAlarm,
            R.string.reminder_exact_alarm_permission_rationale,
            R.string.reminder_exact_alarm_permission_denied_text,
        ),
        PermissionStep(
            SchedulingPermission.FullScreenIntent,
            R.string.reminder_full_screen_intent_permission_rationale,
            R.string.reminder_full_screen_intent_permission_denied_text,
        ),
    )

    val dndBypass: List<PermissionStep> = listOf(
        PermissionStep(
            SchedulingPermission.DndAccess,
            R.string.dnd_permission_rationale,
            R.string.dnd_permission_denied_text,
        ),
    )
}

/**
 * Returns a trigger that walks an ordered list of [PermissionStep]s, requesting each in turn.
 *
 * Per step: already granted → skipped; suppressed via [isDontAskAgain] → skipped (required ones still
 * surface the settings snackbar); otherwise a rationale dialog with confirm / cancel / "don't ask
 * again". Confirm performs the real ask (runtime dialog for notification & phone state, settings
 * screen for exact alarm). "Don't ask again" persists the flag via [onDontAskAgain]. A required
 * permission that ends up denied surfaces the centralized settings snackbar.
 *
 * When the whole list is resolved, [onComplete] receives the per-permission [PermissionResults] so the
 * caller can revert a toggle, clean up, or reschedule as appropriate. PhoneState never affects the
 * required outcome — it degrades gracefully.
 */
@SuppressLint("InlinedApi") // POST_NOTIFICATIONS is an inlined constant; its paths are gated by SDK_INT.
@Composable
fun rememberSchedulingPermissionRequest(
    isDontAskAgain: (SchedulingPermission) -> Boolean,
    onDontAskAgain: (SchedulingPermission) -> Unit,
    // "Don't ask again" is offered only by the home re-check, not the normal enable flow.
    allowDontAskAgain: Boolean = false,
    onComplete: (PermissionResults) -> Unit = {},
): (List<PermissionStep>) -> Unit {
    val context = LocalContext.current
    val resources = LocalResources.current
    val activity = LocalActivity.current
    val scope = rememberCoroutineScope()
    val snackbarController = LocalSnackbarController.current
    val openSettingsLabel = stringResource(R.string.open_settings_label)

    val onCompleteState = rememberUpdatedState(onComplete)
    val isSuppressed = rememberUpdatedState(isDontAskAgain)
    val persistDontAsk = rememberUpdatedState(onDontAskAgain)

    val queue = remember { mutableStateOf<List<PermissionStep>>(emptyList()) }
    val outcomes = remember { mutableStateOf<Map<SchedulingPermission, PermissionOutcome>>(emptyMap()) }
    val dialogStep = remember { mutableStateOf<PermissionStep?>(null) }

    fun guideToSettings(
        @StringRes message: Int,
        openSettings: () -> Unit,
    ) {
        val text = resources.getString(message)
        scope.launch {
            if (snackbarController.show(text, actionLabel = openSettingsLabel) == SnackbarResult.ActionPerformed) {
                openSettings()
            }
        }
    }

    // Forward declaration via holder so finishStep -> process recursion can compile.
    val processRef = remember { mutableStateOf<() -> Unit>({}) }

    fun finishStep(
        step: PermissionStep,
        granted: Boolean,
        asked: Boolean,
    ) {
        outcomes.value = outcomes.value + (step.permission to PermissionOutcome(granted, asked))
        queue.value = queue.value.drop(1)
        processRef.value()
    }

    // Special-access settings (DND policy, exact-alarm, full-screen-intent) return no result, so we
    // re-read the granted state when the activity-result callback fires after the user returns.
    fun resolveSettingsStep(
        step: PermissionStep,
        isGranted: () -> Boolean,
        openSettings: () -> Unit,
    ) {
        val granted = isGranted()
        if (!granted) guideToSettings(step.denied, openSettings)
        finishStep(step, granted, asked = true)
    }

    val notificationLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            val step = queue.value.firstOrNull() ?: return@rememberLauncherForActivityResult
            if (!granted && activity != null &&
                !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.POST_NOTIFICATIONS)
            ) {
                // Permanently denied — the OS won't prompt again; the snackbar is the only recovery.
                guideToSettings(step.denied) { openAppNotificationSettings(context) }
            }
            finishStep(step, granted, asked = true)
        }

    val phoneStateLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            val step = queue.value.firstOrNull() ?: return@rememberLauncherForActivityResult
            finishStep(step, granted, asked = true) // optional: no snackbar, never blocks
        }

    val exactAlarmSettingsLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            val step = queue.value.firstOrNull() ?: return@rememberLauncherForActivityResult
            resolveSettingsStep(step, { canScheduleExactAlarms(context) }) { openExactAlarmSettings(context) }
        }

    val dndSettingsLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            val step = queue.value.firstOrNull() ?: return@rememberLauncherForActivityResult
            resolveSettingsStep(step, { dndAccessGranted(context) }) { openDndSettings(context) }
        }

    val fullScreenIntentSettingsLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            val step = queue.value.firstOrNull() ?: return@rememberLauncherForActivityResult
            resolveSettingsStep(step, { fullScreenIntentGranted(context) }) { openFullScreenIntentSettings(context) }
        }

    val batteryOptimizationLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            val step = queue.value.firstOrNull() ?: return@rememberLauncherForActivityResult
            resolveSettingsStep(step, { batteryOptimizationGranted(context) }) { openBatteryOptimizationSettings(context) }
        }

    fun ask(step: PermissionStep) {
        when (step.permission) {
            SchedulingPermission.Notification -> notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)

            SchedulingPermission.PhoneState -> phoneStateLauncher.launch(Manifest.permission.READ_PHONE_STATE)

            SchedulingPermission.DndAccess -> {
                try {
                    dndSettingsLauncher.launch(Intent(AndroidSettings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
                } catch (_: ActivityNotFoundException) {
                    Toast.makeText(context, R.string.open_settings_failed, Toast.LENGTH_LONG).show()
                    finishStep(step, granted = false, asked = true)
                }
            }

            SchedulingPermission.ExactAlarm -> {
                val intent = exactAlarmSettingsIntent(context)
                if (intent != null) {
                    try {
                        exactAlarmSettingsLauncher.launch(intent)
                    } catch (_: ActivityNotFoundException) {
                        Toast.makeText(context, R.string.open_settings_failed, Toast.LENGTH_LONG).show()
                        finishStep(step, granted = false, asked = true)
                    }
                } else {
                    finishStep(step, granted = true, asked = true) // < API 31: always allowed
                }
            }

            SchedulingPermission.FullScreenIntent -> {
                val intent = fullScreenIntentSettingsIntent(context)
                if (intent != null) {
                    try {
                        fullScreenIntentSettingsLauncher.launch(intent)
                    } catch (_: ActivityNotFoundException) {
                        Toast.makeText(context, R.string.open_settings_failed, Toast.LENGTH_LONG).show()
                        finishStep(step, granted = false, asked = true)
                    }
                } else {
                    finishStep(step, granted = true, asked = true) // < API 34: always allowed
                }
            }

            SchedulingPermission.BatteryOptimization -> {
                try {
                    batteryOptimizationLauncher.launch(batteryOptimizationIntent(context))
                } catch (_: ActivityNotFoundException) {
                    Toast.makeText(context, R.string.open_settings_failed, Toast.LENGTH_LONG).show()
                    finishStep(step, granted = false, asked = true)
                }
            }
        }
    }

    fun process() {
        val step = queue.value.firstOrNull()
        if (step == null) {
            onCompleteState.value(PermissionResults(outcomes.value))
            return
        }
        // The "don't ask again" flag is honored on the home re-check (allowDontAskAgain) and, always, for
        // the optional permissions. The normal enable flow keeps asking for required permissions.
        val respectFlag = allowDontAskAgain || step.permission.isOptional()
        when {
            isGranted(context, step.permission) -> finishStep(step, granted = true, asked = false)
            respectFlag && isSuppressed.value(step.permission) -> finishStep(step, granted = false, asked = false)
            else -> dialogStep.value = step
        }
    }
    processRef.value = { process() }

    dialogStep.value?.let { step ->
        PermissionRationaleDialog(
            title = stringResource(titleResFor(step.permission)),
            text = stringResource(step.rationale),
            confirmLabel = stringResource(confirmLabelResFor(step.permission)),
            onConfirm = {
                dialogStep.value = null
                ask(step)
            },
            onCancel = {
                dialogStep.value = null
                finishStep(step, granted = false, asked = true)
            },
            // "Don't ask again" is offered on the home re-check (allowDontAskAgain) and, always, for the
            // optional permissions — they never block, so users should be able to dismiss them for good even
            // in the normal enable flow. [process] honors the persisted flag under the same rule.
            onDontAskAgain = if (allowDontAskAgain || step.permission.isOptional()) {
                {
                    dialogStep.value = null
                    persistDontAsk.value(step.permission)
                    finishStep(step, granted = false, asked = true)
                }
            } else {
                null
            },
        )
    }

    return remember {
        { steps: List<PermissionStep> ->
            outcomes.value = emptyMap()
            queue.value = steps
            process()
        }
    }
}

/** Whether a scheduling permission is currently granted. */
fun isSchedulingPermissionGranted(
    context: Context,
    permission: SchedulingPermission,
): Boolean = isGranted(context, permission)

private fun isGranted(
    context: Context,
    permission: SchedulingPermission,
): Boolean =
    when (permission) {
        SchedulingPermission.Notification -> notificationGranted(context)

        SchedulingPermission.PhoneState ->
            context.checkSelfPermission(Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED

        SchedulingPermission.ExactAlarm -> canScheduleExactAlarms(context)

        SchedulingPermission.FullScreenIntent -> fullScreenIntentGranted(context)

        SchedulingPermission.DndAccess -> dndAccessGranted(context)

        SchedulingPermission.BatteryOptimization -> batteryOptimizationGranted(context)
    }

private fun batteryOptimizationGranted(context: Context): Boolean {
    val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    return pm.isIgnoringBatteryOptimizations(context.packageName)
}

private fun dndAccessGranted(context: Context): Boolean {
    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    return nm.isNotificationPolicyAccessGranted
}

private fun fullScreenIntentGranted(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return true
    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    return nm.canUseFullScreenIntent()
}

@SuppressLint("InlinedApi") // POST_NOTIFICATIONS constant is inlined; guarded by the SDK_INT check
private fun notificationGranted(context: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

private fun canScheduleExactAlarms(context: Context): Boolean {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    return Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()
}

@StringRes
private fun titleResFor(permission: SchedulingPermission): Int =
    when (permission) {
        SchedulingPermission.Notification -> R.string.notification_permission_title
        SchedulingPermission.PhoneState -> R.string.phone_state_permission_title
        SchedulingPermission.ExactAlarm -> R.string.exact_alarm_permission_title
        SchedulingPermission.FullScreenIntent -> R.string.full_screen_intent_permission_title
        SchedulingPermission.DndAccess -> R.string.dnd_permission_title
        SchedulingPermission.BatteryOptimization -> R.string.battery_optimization_permission_title
    }

@StringRes
private fun confirmLabelResFor(permission: SchedulingPermission): Int =
    when (permission) {
        SchedulingPermission.ExactAlarm,
        SchedulingPermission.FullScreenIntent,
        SchedulingPermission.DndAccess,
        -> R.string.open_settings_label

        else -> R.string.okay
    }

private fun openAppNotificationSettings(context: Context) {
    val intent = Intent(AndroidSettings.ACTION_APP_NOTIFICATION_SETTINGS)
        .putExtra(AndroidSettings.EXTRA_APP_PACKAGE, context.packageName)
        .apply { flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP }
    safeStart(context, intent)
}

private fun exactAlarmSettingsIntent(context: Context): Intent? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return null
    return Intent(
        AndroidSettings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
        "package:${context.packageName}".toUri(),
    ).apply { flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP }
}

private fun openExactAlarmSettings(context: Context) {
    safeStart(context, exactAlarmSettingsIntent(context) ?: return)
}

private fun fullScreenIntentSettingsIntent(context: Context): Intent? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return null
    return Intent(
        AndroidSettings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,
        "package:${context.packageName}".toUri(),
    ).apply { flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP }
}

private fun openFullScreenIntentSettings(context: Context) {
    safeStart(context, fullScreenIntentSettingsIntent(context) ?: return)
}

@SuppressLint("BatteryLife") // App is a prayer-alarm app; exemption is a documented, allowed use case.
private fun batteryOptimizationIntent(context: Context): Intent =
    Intent(
        AndroidSettings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
        "package:${context.packageName}".toUri(),
    ).apply { flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP }

// Recovery path when the user returns without granting: the app-wide battery-optimization list screen.
private fun openBatteryOptimizationSettings(context: Context) {
    safeStart(
        context,
        Intent(AndroidSettings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
            .apply { flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP },
    )
}

private fun openDndSettings(context: Context) {
    safeStart(
        context,
        Intent(AndroidSettings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
            .apply { flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP },
    )
}

private fun safeStart(
    context: Context,
    intent: Intent,
) {
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, R.string.open_settings_failed, Toast.LENGTH_LONG).show()
    }
}
