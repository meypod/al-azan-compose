package com.github.meypod.al_azan.main.settings.calculation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.github.meypod.al_azan.R
import com.github.meypod.al_azan.core.domain.model.settings.ThemeColor
import com.github.meypod.al_azan.core.presentation.AlAzanTheme
import com.github.meypod.al_azan.core.presentation.components.ACard

@Composable
fun ParamAdjustBox(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.element_padding)),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold,
            textDecoration = TextDecoration.Underline,
        )
    }
}

@Preview
@Composable
private fun ParamAdjustBoxPreview() {
    Column(Modifier.padding(10.dp)) {
        AlAzanTheme(ThemeColor.Light) {
            ACard {
                ParamAdjustBox(stringResource(R.string.fajr_angle), "16", Modifier.padding(it))
            }
        }
        AlAzanTheme(ThemeColor.Dark) {
            ACard {
                ParamAdjustBox(stringResource(R.string.fajr_angle), "16", Modifier.padding(it))
            }
        }
    }
}
