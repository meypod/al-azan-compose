package com.github.meypod.al_azan.core.presentation.mapper

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.github.meypod.al_azan.R
import com.github.meypod.al_azan.core.domain.model.alarm.VibrationMode

@StringRes
fun VibrationMode.stringRes(): Int =
    when (this) {
        VibrationMode.Off -> R.string.vibration_off
        VibrationMode.Once -> R.string.vibration_once
        VibrationMode.Continuous -> R.string.vibration_continuous
    }

@Composable
fun VibrationMode.localized(): String = stringResource(stringRes())
