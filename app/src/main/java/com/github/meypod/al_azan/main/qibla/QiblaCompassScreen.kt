package com.github.meypod.al_azan.main.qibla

import android.app.Activity
import android.content.pm.ActivityInfo
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.github.meypod.al_azan.R
import com.github.meypod.al_azan.core.domain.model.calculation.CalculationLocationDetail
import com.github.meypod.al_azan.core.domain.model.compass.CompassAccuracy
import com.github.meypod.al_azan.core.domain.model.compass.CompassReading
import com.github.meypod.al_azan.core.presentation.AlAzanThemePreview
import com.github.meypod.al_azan.core.presentation.DarkSuccess
import com.github.meypod.al_azan.core.presentation.DarkWarning
import com.github.meypod.al_azan.core.presentation.LightSuccess
import com.github.meypod.al_azan.core.presentation.LightWarning
import com.github.meypod.al_azan.core.presentation.components.ScreenScaffold
import com.github.meypod.al_azan.core.presentation.dialog.rememberLocationAccessHelperDialogs
import com.github.meypod.al_azan.core.util.android.LocationUtils
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Stateful entry point: collects the hot compass [readings] stream (lifecycle-aware) into local UI
 * state, then renders the stateless [QiblaCompassScreen]. Keeping the [StateFlow] out of the
 * stateless screen keeps that screen preview-friendly and matches the project's MVI convention
 * (state in, events out).
 */
@Composable
fun QiblaCompassRoute(
    uiState: QiblaCompassUiState,
    readings: StateFlow<CompassReading?>,
    onAction: (QiblaCompassUiAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val heading = remember { mutableFloatStateOf(0f) }
    var accuracy by remember { mutableStateOf(CompassAccuracy.UNRELIABLE) }

    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(readings, lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            readings.collect { reading ->
                if (reading != null) {
                    heading.floatValue = reading.headingDegrees
                    accuracy = reading.accuracy
                }
            }
        }
    }

    QiblaCompassScreen(
        uiState = uiState,
        accuracy = accuracy,
        // Lambda, not a value: the dial reads it inside graphicsLayer (deferred to the draw phase),
        // so per-frame heading changes never trigger recomposition.
        headingProvider = { heading.floatValue },
        onAction = onAction,
        modifier = modifier,
    )
}

@Composable
fun QiblaCompassScreen(
    uiState: QiblaCompassUiState,
    accuracy: CompassAccuracy,
    headingProvider: () -> Float,
    onAction: (QiblaCompassUiAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    LockOrientationEffect(uiState.isOrientationLocked)

    // The screen owns the GPS fetch + its loading state; the helper only gates permission/service.
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isFetchingLocation by remember { mutableStateOf(false) }
    val triggerLocation = rememberLocationAccessHelperDialogs(
        onPermissionGranted = {
            scope.launch {
                isFetchingLocation = true
                try {
                    LocationUtils.requestCurrentLocation(context).getOrNull()?.let { location ->
                        onAction(
                            QiblaCompassUiAction.OnLocationFetched(
                                CalculationLocationDetail(lat = location.latitude, long = location.longitude),
                            ),
                        )
                    }
                } finally {
                    isFetchingLocation = false
                }
            }
        },
    )

    ScreenScaffold(
        title = stringResource(R.string.qibla_compass),
        onBackClick = { onAction(QiblaCompassUiAction.OnBackClick) },
        scrollable = false,
        modifier = modifier,
    ) {
        Box(Modifier.fillMaxSize()) {
            // Compass underneath: largest square that fits the available space (width-bound in
            // portrait, height-bound in landscape), centered.
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val availableWidth = maxWidth
                val availableHeight = maxHeight
                // In portrait, lift the dial up off the bottom so it clears the overlaid info bar;
                // landscape uses the full height.
                val portrait = availableHeight >= availableWidth
                val bottomPad = if (portrait) CompassButtonSize * 3 + 12.dp else 0.dp
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = bottomPad),
                    contentAlignment = Alignment.Center,
                ) {
                    val side = minOf(availableWidth, availableHeight - bottomPad)
                    Compass(
                        headingProvider = headingProvider,
                        qiblaDegrees = uiState.qiblaDegrees,
                        modifier = Modifier.size(side),
                    )
                }
            }

            // Controls overlaid on top of the compass.
            Column(Modifier.fillMaxSize()) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    CompassIconButton(
                        icon = if (uiState.isOrientationLocked) {
                            R.drawable.outline_screen_lock_rotation_24
                        } else {
                            R.drawable.outline_screen_rotation_24
                        },
                        contentDescription = stringResource(
                            if (uiState.isOrientationLocked) {
                                R.string.qibla_unlock_orientation
                            } else {
                                R.string.qibla_lock_orientation
                            },
                        ),
                        active = uiState.isOrientationLocked,
                        onClick = { onAction(QiblaCompassUiAction.OnToggleOrientationLock) },
                    )
                }

                Spacer(Modifier.weight(1f))

                InfoSection(
                    uiState = uiState,
                    accuracy = accuracy,
                    isFetchingLocation = isFetchingLocation,
                    onRefreshLocation = triggerLocation,
                )
            }
        }
    }
}

@Composable
private fun Compass(
    headingProvider: () -> Float,
    qiblaDegrees: Float?,
    modifier: Modifier = Modifier,
) {
    // Pick the dial by the resolved theme, not the system setting: the app's theme can be forced
    // Light/Dark independently of the OS, and may be a dynamic scheme. Surface luminance reflects
    // whatever scheme is actually rendered.
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val baseImage = if (isDark) R.drawable.compass_base_dark else R.drawable.compass_base_light
    Box(modifier, contentAlignment = Alignment.Center) {
        // Static circular surface providing the (rotation-independent) circular drop shadow.
        Box(
            Modifier
                .fillMaxSize()
                .shadow(elevation = 8.dp, shape = CircleShape, clip = false)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerLowest),
        )
        Image(
            painter = painterResource(baseImage),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { rotationZ = -headingProvider() },
        )
        if (qiblaDegrees != null) {
            Image(
                painter = painterResource(R.drawable.qibla_indicator),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { rotationZ = -headingProvider() + qiblaDegrees },
            )
        }
        HeadingDegreeText(headingProvider)
    }
}

@Composable
private fun CompassIconButton(
    icon: Int,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    active: Boolean = false,
    loading: Boolean = false,
) {
    Surface(
        onClick = onClick,
        enabled = !loading,
        shape = MaterialTheme.shapes.medium,
        color = if (active) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface,
        contentColor = if (active) {
            MaterialTheme.colorScheme.onSecondaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        shadowElevation = 2.dp,
        modifier = modifier.size(CompassButtonSize),
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (loading) {
                // Same footprint as the icon so the button doesn't resize.
                CircularProgressIndicator(
                    modifier = Modifier.size(CompassIconSize),
                    strokeWidth = 2.dp,
                    color = LocalContentColor.current,
                )
            } else {
                Icon(painterResource(icon), contentDescription)
            }
        }
    }
}

/** Isolated so only this node recomposes on each sensor frame. */
@Composable
private fun HeadingDegreeText(headingProvider: () -> Float) {
    Text(
        text = "${headingProvider().roundToInt()}°",
        style = MaterialTheme.typography.displayMedium,
    )
}

@Composable
private fun InfoSection(
    uiState: QiblaCompassUiState,
    accuracy: CompassAccuracy,
    isFetchingLocation: Boolean,
    onRefreshLocation: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(bottom = dimensionResource(R.dimen.element_padding_large)),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.element_padding)),
        horizontalAlignment = Alignment.Start,
    ) {
        val rowModifier = Modifier
            .fillMaxWidth()
            .height(CompassButtonSize)

        Row(
            modifier = rowModifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.element_padding)),
        ) {
            LabeledValue(
                label = stringResource(R.string.qibla_location_label),
                value = uiState.locationLabel.text(),
            )
            CompassIconButton(
                icon = R.drawable.outline_location_searching_24,
                contentDescription = stringResource(R.string.qibla_refresh_location),
                onClick = onRefreshLocation,
                loading = isFetchingLocation,
            )
        }

        uiState.qiblaDegrees?.let { qibla ->
            LabeledValue(
                label = stringResource(R.string.qibla_label),
                value = stringResource(R.string.qibla_degrees_from_north, qibla.roundToInt()),
                modifier = rowModifier,
            )
        }

        AccuracyRow(accuracy = accuracy, modifier = rowModifier)
    }
}

@Composable
private fun AccuracyRow(
    accuracy: CompassAccuracy,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = stringResource(R.string.qibla_accuracy_label),
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = stringResource(accuracy.labelRes()),
            color = accuracy.color(),
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun LabeledValue(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(text = label, fontWeight = FontWeight.Bold)
        Text(text = value)
    }
}

/** Shared size for the compass icon buttons; also drives the info rows' height. */
private val CompassButtonSize = 48.dp

/** Matches the default Material icon size so the loading spinner keeps the same footprint. */
private val CompassIconSize = 24.dp

@Composable
private fun QiblaLocationLabel.text(): String =
    when (this) {
        QiblaLocationLabel.None -> stringResource(R.string.qibla_no_location)

        QiblaLocationLabel.FromSettings -> stringResource(R.string.qibla_location_from_settings)

        is QiblaLocationLabel.Fetched -> String.format(
            Locale.US,
            "%.2f, %.2f",
            detail.lat,
            detail.long,
        )
    }

private fun CompassAccuracy.labelRes(): Int =
    when (this) {
        CompassAccuracy.HIGH -> R.string.compass_accuracy_high
        CompassAccuracy.MEDIUM -> R.string.compass_accuracy_medium
        CompassAccuracy.LOW -> R.string.compass_accuracy_low
        CompassAccuracy.UNRELIABLE -> R.string.compass_accuracy_unreliable
        CompassAccuracy.NO_SENSOR -> R.string.compass_sensor_unavailable
    }

@Composable
private fun CompassAccuracy.color(): Color {
    // Resolve light/dark by the actually-rendered scheme (theme can be forced or dynamic).
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    return when (this) {
        CompassAccuracy.HIGH -> if (isDark) DarkSuccess else LightSuccess

        CompassAccuracy.MEDIUM -> if (isDark) DarkWarning else LightWarning

        CompassAccuracy.LOW, CompassAccuracy.UNRELIABLE, CompassAccuracy.NO_SENSOR ->
            MaterialTheme.colorScheme.error
    }
}

@Composable
private fun LockOrientationEffect(locked: Boolean) {
    val context = LocalContext.current
    val activity = context as? Activity ?: return
    DisposableEffect(locked) {
        activity.requestedOrientation = if (locked) {
            ActivityInfo.SCREEN_ORIENTATION_LOCKED
        } else {
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
        onDispose {
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }
}

@Preview(name = "Qibla compass", showSystemUi = true)
@Composable
private fun QiblaCompassScreenPreview() {
    AlAzanThemePreview {
        QiblaCompassScreen(
            uiState = QiblaCompassUiState(
                qiblaDegrees = 218f,
                locationLabel = QiblaLocationLabel.FromSettings,
            ),
            accuracy = CompassAccuracy.HIGH,
            headingProvider = { 136f },
            onAction = {},
        )
    }
}

private class AccuracyPreviewProvider : PreviewParameterProvider<CompassAccuracy> {
    override val values = CompassAccuracy.entries.asSequence()
}

@Preview(name = "Accuracy light", showBackground = true)
@Preview(name = "Accuracy dark", showBackground = true, uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun AccuracyRowPreview(@PreviewParameter(AccuracyPreviewProvider::class) accuracy: CompassAccuracy) {
    AlAzanThemePreview {
        AccuracyRow(
            accuracy = accuracy,
            modifier = Modifier.padding(dimensionResource(R.dimen.page_padding)),
        )
    }
}
