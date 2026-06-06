package com.github.meypod.al_azan.main.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.github.meypod.al_azan.R
import com.github.meypod.al_azan.core.presentation.AlAzanTheme
import com.github.meypod.al_azan.core.presentation.util.dropShadow2

@Composable
fun ConfigHintCard(
    missingLocation: Boolean,
    missingCalculation: Boolean,
    onLocationClick: () -> Unit,
    onCalculationClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.dropShadow2(MaterialTheme.shapes.medium),
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 2.dp,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(dimensionResource(R.dimen.element_padding)),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.element_padding)),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.icon_padding)),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painterResource(R.drawable.info_variant_outline),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                    stringResource(R.string.prayer_times_unavailable_hint),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            if (missingLocation) {
                TextButton(onClick = onLocationClick, modifier = Modifier.fillMaxWidth()) {
                    Icon(
                        painterResource(R.drawable.map_marker),
                        contentDescription = null,
                        modifier = Modifier.padding(end = dimensionResource(R.dimen.icon_padding)),
                    )
                    Text(stringResource(R.string.set_location_hint))
                }
            }
            if (missingCalculation) {
                TextButton(onClick = onCalculationClick, modifier = Modifier.fillMaxWidth()) {
                    Icon(
                        painterResource(R.drawable.settings),
                        contentDescription = null,
                        modifier = Modifier.padding(end = dimensionResource(R.dimen.icon_padding)),
                    )
                    Text(stringResource(R.string.set_calculation_hint))
                }
            }
        }
    }
}

@Preview
@Composable
private fun ConfigHintCardPreview() {
    AlAzanTheme {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            ConfigHintCard(
                missingLocation = true,
                missingCalculation = true,
                onLocationClick = {},
                onCalculationClick = {},
            )
            ConfigHintCard(
                missingLocation = false,
                missingCalculation = true,
                onLocationClick = {},
                onCalculationClick = {},
            )
        }
    }
}
