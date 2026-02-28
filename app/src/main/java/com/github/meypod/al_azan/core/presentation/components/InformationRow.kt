package com.github.meypod.al_azan.core.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.github.meypod.al_azan.R

@Composable
fun InformationRow(
    modifier: Modifier = Modifier,
    iconDescription: String? = stringResource(R.string.information),
    content: @Composable () -> Unit,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.element_padding)),
    ) {
        Icon(
            painter = painterResource(R.drawable.info),
            contentDescription = iconDescription,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        ProvideTextStyle(MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)) {
            content()
        }
    }
}
