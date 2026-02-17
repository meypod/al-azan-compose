package com.github.meypod.al_azan.intro.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.github.meypod.al_azan.R

@Composable
fun IntroSkipButton(onClick: () -> Unit = {}) {
    TextButton(
        onClick = onClick,
        modifier =
            Modifier
                .padding(8.dp),
    ) {
        Text(
            text = stringResource(R.string.skip),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
        )
    }
}
