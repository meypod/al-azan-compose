package com.github.meypod.al_azan.core.presentation.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.github.meypod.al_azan.R

/**
 * Icon + text note. [iconRes] and [contentColor] default to the informational look; override both to
 * turn the same row into a warning (see the "display over other apps" notice in the adhan settings).
 */
@Composable
fun InformationRow(
    modifier: Modifier = Modifier,
    iconDescription: String? = stringResource(R.string.information),
    verticalAlignment: Alignment.Vertical = Alignment.Top,
    @DrawableRes iconRes: Int = R.drawable.info,
    contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    content: @Composable () -> Unit,
) {
    Row(
        modifier = modifier,
        verticalAlignment = verticalAlignment,
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.element_padding)),
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = iconDescription,
            tint = contentColor,
        )
        // Via LocalContentColor rather than baked into the text style: a style color would also win
        // inside nested components that set their own (a Button's label would come out this color too).
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            ProvideTextStyle(MaterialTheme.typography.bodyMedium) {
                content()
            }
        }
    }
}
