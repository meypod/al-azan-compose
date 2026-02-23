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
fun IntroTitle(resId: Int) {
    Text(
        text = stringResource(resId),
        style = MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.colorScheme.onPrimaryContainer,
        fontWeight = FontWeight.Bold
    )
}

@Preview
@Composable
private fun IntroTitlePreview() {
    AlAzanTheme {
        IntroTitle(R.string.location_title)
    }
}
