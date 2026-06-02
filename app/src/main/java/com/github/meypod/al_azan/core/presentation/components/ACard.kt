package com.github.meypod.al_azan.core.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.github.meypod.al_azan.R
import com.github.meypod.al_azan.core.presentation.AlAzanTheme

@Composable
fun ACard(
    modifier: Modifier = Modifier,
    tonalElevation: Dp = 0.dp,
    shadowElevation: Dp = 0.dp,
    compact: Boolean = false,
    paddingValues: PaddingValues = if (compact) {
        PaddingValues(
            vertical = dimensionResource(R.dimen.card_padding_vc),
            horizontal = dimensionResource(R.dimen.card_padding_hc),
        )
    } else {
        PaddingValues(
            vertical = dimensionResource(R.dimen.card_padding_v),
            horizontal = dimensionResource(R.dimen.card_padding_h),
        )
    },
    content: @Composable (paddingValues: PaddingValues) -> Unit,
) {
    Surface(
        modifier,
        tonalElevation = tonalElevation,
        shadowElevation = shadowElevation,
        shape = MaterialTheme.shapes.medium,
    ) {
        content(paddingValues)
    }
}

@Preview
@Composable
private fun CardPreview() {
    AlAzanTheme {
        ACard {
            Column(Modifier.padding(it)) {
                Text("a very long text to test the surface of this area and multi line")
                Text("a very long text to test the surface of this area and multi line")
                Text("a very long text to test the surface of this area and multi line")
            }
        }
    }
}
