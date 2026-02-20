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
import com.github.meypod.al_azan.R
import com.github.meypod.al_azan.core.presentation.AlAzanTheme

@Composable
fun ACard(
    modifier: Modifier = Modifier,
    content: @Composable (paddingValues: PaddingValues) -> Unit,
) {
    Surface(modifier, shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceContainer) {
        content(PaddingValues(dimensionResource(R.dimen.card_padding)))
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
