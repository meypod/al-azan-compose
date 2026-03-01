package com.github.meypod.al_azan.core.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import com.github.meypod.al_azan.R
import com.github.meypod.al_azan.core.presentation.AlAzanTheme

@Composable
fun ScreenLinkButton(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onClick: () -> Unit,
) {
    ListItem(
        modifier = Modifier
            .clip(MaterialTheme.shapes.large)
            .then(modifier)
            .clickable(role = Role.Button, onClick = onClick),
        headlineContent = { SettingLabel(title) },
        supportingContent = {
            if (!subtitle.isNullOrEmpty()) {
                Text(subtitle, color = MaterialTheme.colorScheme.primary)
            }
        },
        trailingContent = {
            Icon(
                painterResource(R.drawable.baseline_navigate_next_24),
                null,
            )
        },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    )
}

@Preview(showBackground = true)
@Composable
private fun ScreenLinkButtonPreview() {
    AlAzanTheme {
        ScreenLinkButton("Adjustments") {}
    }
}
