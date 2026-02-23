package com.github.meypod.al_azan.intro.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.github.meypod.al_azan.R
import com.github.meypod.al_azan.core.presentation.AlAzanTheme

@Composable
fun IntroSubtitle(resId: Int) {
    Text(
        stringResource(resId),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onPrimaryContainer,
        fontWeight = FontWeight.Bold,
    )
}

@Preview
@Composable
private fun IntroSubtitlePreview() {
    AlAzanTheme {
        IntroSubtitle(R.string.location_title)
    }
}
