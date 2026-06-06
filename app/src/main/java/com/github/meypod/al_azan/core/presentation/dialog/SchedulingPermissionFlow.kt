package com.github.meypod.al_azan.core.presentation.dialog

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
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

    /** True when every requested REQUIRED permission (everything except [SchedulingPermission.PhoneState]) is granted. */
    fun requiredAllGranted(): Boolean =
        outcomes.all { (permission, outcome) -> permission == SchedulingPermission.PhoneState || outcome.granted }
}

/** Reads the persisted "don't ask again" flag for a permission off [Settings]. */
fun Settings.isDontAskAgain(permission: SchedulingPermission): Boolean =
    when (permission) {
        SchedulingPermission.Notification -> dontAskPermissionNotifications
        SchedulingPermission.PhoneState -> dontAskPermissionPhoneState
        SchedulingPermission.ExactAlarm -> dontAskPermissionAlarm
    }

/** Sets the persisted "don't ask again" flag for a permission. */
fun Settings.withDontAskAgain(permission: SchedulingPermission): Settings =
    when (permission) {
        SchedulingPermission.Notification -> copy(dontAskPermissionNotifications = true)
        SchedulingPermission.PhoneState -> copy(dontAskPermissionPhoneState = true)
        SchedulingPermission.ExactAlarm -> copy(dontAskPermissionAlarm = true)
    }

/** Standard step lists. PhoneState is only for adhan (call interruption). */
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
            val granted = canScheduleExactAlarms(context)
            if (!granted) guideToSettings(step.denied) { openExactAlarmSettings(context) }
            finishStep(step, granted, asked = true)
        }

    fun ask(step: PermissionStep) {
        when (step.permission) {
            SchedulingPermission.Notification -> notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)

            SchedulingPermission.PhoneState -> phoneStateLauncher.launch(Manifest.permission.READ_PHONE_STATE)

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
        }
    }

    fun process() {
        val step = queue.value.firstOrNull()
        if (step == null) {
            onCompleteState.value(PermissionResults(outcomes.value))
            return
        }
        // The "don't ask again" flag is honored only on the home re-check (allowDontAskAgain) and,
        // always, for the optional phone-state permission. The normal enable flow keeps asking.
        val respectFlag = allowDontAskAgain || step.permission == SchedulingPermission.PhoneState
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
            onDontAskAgain = if (allowDontAskAgain) {
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
    }

@StringRes
private fun confirmLabelResFor(permission: SchedulingPermission): Int =
    when (permission) {
        SchedulingPermission.ExactAlarm -> R.string.open_settings_label
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
