package com.github.meypod.al_azan.core.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.github.meypod.al_azan.R
import com.github.meypod.al_azan.core.domain.model.settings.ThemeColor
import com.github.meypod.al_azan.core.presentation.AlAzanTheme

@Composable
fun InformationCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    ACard(modifier) { cardPadding ->
        InformationRow(Modifier.padding(cardPadding), content = content)
    }
}

@Composable
private fun PreviewContent() {
    InformationCard {
        Column {
            Text("a very long text to test the surface of this area and multi line")
            Text("a very long text to test the surface of this area and multi line")
            Text("a very long text to test the surface of this area and multi line")
        }
    }
}

@Preview
@Composable
private fun InformationCardPreview() {
    AlAzanTheme {
        PreviewContent()
    }
}

@Preview
@Composable
private fun InformationCardDarkPreview() {
    AlAzanTheme(themeColor = ThemeColor.Dark) {
        PreviewContent()
    }
}
