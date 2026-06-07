package com.github.meypod.al_azan.main.home

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.github.meypod.al_azan.R

/**
 * On home load, shows the Ramadan-accuracy notice when [shouldShow] is true: the pre-calculated
 * calendar is near Ramadan's start/end. Mirrors [HomePermissionGate]'s one-shot pattern. Renders
 * nothing until the suspend check resolves. Tapping outside just hides it (re-shown next load);
 * only the explicit actions persist a choice.
 */
@Composable
fun RamadanNoticeGate(
    shouldShow: suspend () -> Boolean,
    onRemindNextYear: () -> Unit,
    onDontShowAgain: () -> Unit,
) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { visible = shouldShow() }

    if (visible) {
        RamadanNoticeDialog(
            onRemindNextYear = {
                visible = false
                onRemindNextYear()
            },
            onDontShowAgain = {
                visible = false
                onDontShowAgain()
            },
            onDismiss = { visible = false },
        )
    }
}

@Composable
private fun RamadanNoticeDialog(
    onRemindNextYear: () -> Unit,
    onDontShowAgain: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.ramadan_notice_title), style = MaterialTheme.typography.titleMedium) },
        text = { Text(text = stringResource(R.string.lunar_calendar_warning)) },
        confirmButton = {
            TextButton(onClick = onRemindNextYear) { Text(stringResource(R.string.ramadan_remind_next_year)) }
        },
        dismissButton = {
            TextButton(onClick = onDontShowAgain) {
                Text(
                    stringResource(R.string.ramadan_dont_show_again),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
    )
}
