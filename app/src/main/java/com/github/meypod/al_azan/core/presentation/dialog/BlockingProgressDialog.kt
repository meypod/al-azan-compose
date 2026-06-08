package com.github.meypod.al_azan.core.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.github.meypod.al_azan.R
import com.github.meypod.al_azan.core.presentation.AlAzanThemePreview

/**
 * A modal dialog showing a spinner next to [message]. Not dismissible (no back, no outside tap) so
 * the user can't interact while a blocking operation such as a restore is running.
 */
@Composable
fun BlockingProgressDialog(message: String) {
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
    ) {
        Surface(
            shape = MaterialTheme.shapes.large,
            tonalElevation = 6.dp,
        ) {
            Row(
                modifier = Modifier.padding(dimensionResource(R.dimen.page_padding)),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.element_padding)),
            ) {
                CircularProgressIndicator(Modifier.size(28.dp))
                Text(text = message, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@Preview
@Composable
private fun BlockingProgressDialogPreview() {
    AlAzanThemePreview {
        Scaffold {
            Column(Modifier.padding(it)) {
                BlockingProgressDialog(message = "Restoring…")
            }
        }
    }
}
