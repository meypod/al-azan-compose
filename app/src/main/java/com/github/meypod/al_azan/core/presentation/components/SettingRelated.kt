package com.github.meypod.al_azan.core.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import com.github.meypod.al_azan.R
import com.github.meypod.al_azan.core.presentation.AlAzanTheme

@Composable
fun RowScope.SettingLabel(
    text: String,
    fontWeight: FontWeight? = null,
) {
    SettingLabel(text = text, fontWeight = fontWeight, modifier = Modifier.weight(1f))
}

@Composable
fun SettingLabel(
    text: String,
    modifier: Modifier = Modifier,
    fontWeight: FontWeight? = null,
) {
    Text(text = text, fontWeight = fontWeight, modifier = modifier)
}

@Composable
fun SettingHelp(text: String) {
    Text(
        text = text,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 13.sp,
        lineHeight = 18.sp,
    )
}

@Composable
fun SettingHelp(text: AnnotatedString) {
    Text(
        text = text,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 13.sp,
        lineHeight = 18.sp,
    )
}

@Composable
fun SettingHeader(
    title: String,
    subtitle: String,
) {
    Column {
        SettingLabel(title, modifier = Modifier.semantics { heading() })
        SettingHelp(subtitle)
    }
}

@Composable
fun SettingHeader(
    title: String,
    subtitle: AnnotatedString,
) {
    Column {
        SettingLabel(title, modifier = Modifier.semantics { heading() })
        SettingHelp(subtitle)
    }
}

@Composable
fun SettingSwitch(
    title: String,
    subtitle: String?,
    modifier: Modifier = Modifier,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
) {
    val disabledAlpha = 0.38f
    val titleColor = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = disabledAlpha)
    val subtitleColor = MaterialTheme.colorScheme.onSurfaceVariant.let {
        if (enabled) it else it.copy(alpha = disabledAlpha)
    }
    Row(
        modifier
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = titleColor)
            subtitle?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = subtitleColor,
                )
            }
        }
        Spacer(Modifier.padding(start = dimensionResource(R.dimen.element_padding)))
        Switch(
            checked,
            onCheckedChange,
            enabled = enabled,
            // Lone Switch announces only on/off; give TalkBack the setting's name.
            modifier = Modifier.semantics { contentDescription = title },
        )
    }
}

@Composable
fun SettingLinkButton(
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
private fun SettingLinkButtonPreview() {
    AlAzanTheme {
        SettingLinkButton("Adjustments") {}
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingHeaderPreview() {
    AlAzanTheme {
        ACard { cardPadding ->
            Column(Modifier.padding(cardPadding)) {
                SettingHeader("This is a title", "this is a subtitle")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingSwitchPreview() {
    AlAzanTheme {
        ACard { cardPadding ->
            Column(Modifier.padding(cardPadding)) {
                SettingSwitch("This is a title", "this is a subtitle", checked = false) {}
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingSwitchWithoutSubtitlePreview() {
    AlAzanTheme {
        ACard { cardPadding ->
            Column(Modifier.padding(cardPadding)) {
                SettingSwitch("This is a title", null, checked = false) {}
            }
        }
    }
}
