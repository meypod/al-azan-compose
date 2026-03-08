package com.github.meypod.al_azan.core.presentation.dialog

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.github.meypod.al_azan.R
import com.github.meypod.al_azan.core.domain.model.calculation.CalculationLocationDetail
import com.github.meypod.al_azan.core.presentation.components.CtaDialog
import com.github.meypod.al_azan.core.util.android.LocationUtils
import kotlinx.coroutines.launch

@Composable
fun rememberLocationAccessHelperDialogs(
    onLocation: ((CalculationLocationDetail) -> Unit)? = null,
    onPermissionGranted: (() -> Unit)? = null,
): () -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val showLocationDisabledDialog = remember { mutableStateOf(false) }
    val showPermissionDeniedDialog = remember { mutableStateOf(false) }
    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted || context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            showPermissionDeniedDialog.value = false
            onPermissionGranted?.invoke()
            if (onLocation != null) {
                scope.launch {
                    val result = LocationUtils.requestCurrentLocation(context).getOrNull()
                    if (result != null) {
                        onLocation(
                            CalculationLocationDetail(
                                lat = result.latitude,
                                long = result.longitude,
                            ),
                        )
                    }
                }
            }
        } else {
            showPermissionDeniedDialog.value = true
        }
    }

    val activityLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (LocationUtils.isLocationEnabled(context)) {
            showLocationDisabledDialog.value = false
            permLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    val triggerLocation = remember {
        {
            if (!LocationUtils.isLocationEnabled(context)) {
                showLocationDisabledDialog.value = true
            } else {
                permLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }

    if (showLocationDisabledDialog.value) {
        CtaDialog(
            title = stringResource(R.string.location_service_required_title),
            text = stringResource(R.string.location_service_required_text),
            confirmLabel = stringResource(R.string.open_settings_label),
            dismissLabel = stringResource(R.string.okay),
            onConfirm = {
                try {
                    activityLauncher.launch(
                        Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).apply {
                            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        },
                    )
                } catch (_: ActivityNotFoundException) {
                    Toast.makeText(
                        context,
                        R.string.open_settings_failed,
                        Toast.LENGTH_LONG,
                    ).show()
                }
            },
            onDismissRequest = {
                showLocationDisabledDialog.value = false
            },
        )
    }

    if (showPermissionDeniedDialog.value) {
        CtaDialog(
            title = stringResource(R.string.permission_denied),
            text = stringResource(R.string.location_permission_denied_text),
            confirmLabel = stringResource(R.string.open_settings_label),
            dismissLabel = stringResource(R.string.okay),
            onConfirm = {
                try {
                    activityLauncher.launch(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        },
                    )
                    Toast.makeText(
                        context,
                        R.string.open_permissions_guidance,
                        Toast.LENGTH_LONG,
                    ).show()
                } catch (_: ActivityNotFoundException) {
                    Toast.makeText(
                        context,
                        R.string.open_settings_failed,
                        Toast.LENGTH_LONG,
                    ).show()
                }
            },
            onDismissRequest = {
                showPermissionDeniedDialog.value = false
            },
        )
    }

    return triggerLocation
}
