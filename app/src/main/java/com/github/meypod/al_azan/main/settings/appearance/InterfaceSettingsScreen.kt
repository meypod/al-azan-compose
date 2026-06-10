package com.github.meypod.al_azan.main.settings.appearance

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.github.meypod.al_azan.R
import com.github.meypod.al_azan.core.domain.model.settings.NumberingSystem
import com.github.meypod.al_azan.core.domain.model.settings.SecondaryCalendar
import com.github.meypod.al_azan.core.domain.model.settings.SupportedLocales
import com.github.meypod.al_azan.core.domain.model.settings.ThemeColor
import com.github.meypod.al_azan.core.domain.util.formatWithUnicodeDigits
import com.github.meypod.al_azan.core.presentation.AlAzanThemePreview
import com.github.meypod.al_azan.core.presentation.DarkSurface
import com.github.meypod.al_azan.core.presentation.LightSecondaryContainer
import com.github.meypod.al_azan.core.presentation.components.ACard
import com.github.meypod.al_azan.core.presentation.components.BottomSelect
import com.github.meypod.al_azan.core.presentation.components.PrayerCheckboxTable
import com.github.meypod.al_azan.core.presentation.components.ScreenScaffold
import com.github.meypod.al_azan.core.presentation.components.SettingHeader
import com.github.meypod.al_azan.core.presentation.components.SettingLabel
import com.github.meypod.al_azan.core.presentation.components.SettingSwitch
import com.github.meypod.al_azan.core.presentation.navigation.NavigationController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InterfaceSettingsScreen(
    uiState: InterfaceSettingsUiState,
    onAction: (InterfaceSettingsUiAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    ScreenScaffold(
        title = stringResource(R.string.interface_settings),
        onBackClick = { NavigationController.navigateBack() },
        modifier = modifier,
    ) {
        LanguageCard(uiState, onAction)
        ThemesCard(uiState, onAction)
        PrayerVisibilityCard(uiState, onAction)
        CountdownCard(uiState, onAction)
        HighlightCurrentPrayerCard(uiState, onAction)
        TimeFormatCard(uiState, onAction)
        NumberingSystemCard(uiState, onAction)
        CalendarsCard(uiState, onAction)
    }
}

@Composable
private fun LanguageCard(
    uiState: InterfaceSettingsUiState,
    onAction: (InterfaceSettingsUiAction) -> Unit,
) {
    ACard { cardPadding ->
        Column(
            Modifier.padding(cardPadding),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.element_padding)),
        ) {
            SettingLabel(stringResource(R.string.language), fontWeight = FontWeight.Medium)
            BottomSelect(
                modifier = Modifier.fillMaxWidth(),
                options = SupportedLocales,
                optionKey = { it.value },
                optionLabel = { it.label },
                optionSearchTag = { it.tags },
                selectedKey = uiState.settings.selectedLocale,
                onSelect = { onAction(InterfaceSettingsUiAction.OnLanguageChange(it.value)) },
                searchable = true,
            )
        }
    }
}

@Composable
private fun ThemesCard(
    uiState: InterfaceSettingsUiState,
    onAction: (InterfaceSettingsUiAction) -> Unit,
) {
    ACard { cardPadding ->
        Column(
            Modifier.padding(cardPadding),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.element_padding)),
        ) {
            SettingLabel(stringResource(R.string.themes), fontWeight = FontWeight.Medium)
            ThemeGrid(uiState.settings.themeColor) { theme ->
                onAction(InterfaceSettingsUiAction.OnThemeChange(theme))
            }
        }
    }
}

@Composable
private fun PrayerVisibilityCard(
    uiState: InterfaceSettingsUiState,
    onAction: (InterfaceSettingsUiAction) -> Unit,
) {
    ACard { cardPadding ->
        PrayerCheckboxTable(
            title = stringResource(R.string.show_prayer_times_title),
            helpText = stringResource(R.string.show_prayer_times_help),
            leftColumn = stringResource(R.string.time_column),
            rightColumn = stringResource(R.string.show_column),
            isChecked = { it !in uiState.settings.hiddenPrayers },
            onToggle = { prayer, visible ->
                onAction(InterfaceSettingsUiAction.OnPrayerVisibilityChange(prayer, visible))
            },
            modifier = Modifier.padding(cardPadding),
        )
    }
}

@Composable
private fun CountdownCard(
    uiState: InterfaceSettingsUiState,
    onAction: (InterfaceSettingsUiAction) -> Unit,
) {
    ACard { cardPadding ->
        Column(
            Modifier.padding(cardPadding),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.element_padding)),
        ) {
            SettingSwitch(
                title = stringResource(R.string.countdown_timer),
                subtitle = stringResource(R.string.countdown_timer_help),
                checked = uiState.settings.showHomeNextPrayerCountdown,
                onCheckedChange = { onAction(InterfaceSettingsUiAction.OnCountdownTimerToggle(it)) },
            )
            SettingSwitch(
                title = stringResource(R.string.countdown_skip_non_prayers),
                subtitle = stringResource(R.string.countdown_skip_non_prayers_help),
                checked = uiState.settings.countdownSkipNonPrayers,
                enabled = uiState.settings.showHomeNextPrayerCountdown,
                onCheckedChange = { onAction(InterfaceSettingsUiAction.OnCountdownSkipNonPrayersToggle(it)) },
            )
        }
    }
}

@Composable
private fun HighlightCurrentPrayerCard(
    uiState: InterfaceSettingsUiState,
    onAction: (InterfaceSettingsUiAction) -> Unit,
) {
    ACard { cardPadding ->
        Column(Modifier.padding(cardPadding)) {
            SettingSwitch(
                title = stringResource(R.string.highlight_current_prayer_time),
                subtitle = stringResource(R.string.highlight_current_prayer_time_help),
                checked = uiState.settings.highlightCurrentPrayer,
                onCheckedChange = { onAction(InterfaceSettingsUiAction.OnHighlightCurrentPrayerToggle(it)) },
            )
        }
    }
}

@Composable
private fun TimeFormatCard(
    uiState: InterfaceSettingsUiState,
    onAction: (InterfaceSettingsUiAction) -> Unit,
) {
    ACard { cardPadding ->
        Column(
            Modifier.padding(cardPadding),
        ) {
            SettingSwitch(
                title = stringResource(R.string.use_24_hour_format),
                subtitle = null,
                checked = uiState.settings.is24HourFormat,
                onCheckedChange = { onAction(InterfaceSettingsUiAction.OnTimeFormatToggle(it)) },
            )
        }
    }
}

@Composable
private fun NumberingSystemCard(
    uiState: InterfaceSettingsUiState,
    onAction: (InterfaceSettingsUiAction) -> Unit,
) {
    val resources = LocalResources.current
    ACard { cardPadding ->
        Column(
            Modifier.padding(cardPadding),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.element_padding)),
        ) {
            SettingHeader(
                stringResource(R.string.numbering_system),
                stringResource(R.string.numbering_system_help),
            )
            BottomSelect(
                modifier = Modifier.fillMaxWidth(),
                options = NumberingSystem.entries,
                optionKey = { it.name },
                optionLabel = {
                    val label =
                        when (it) {
                            NumberingSystem.Default -> resources.getString(R.string.default_value)
                            NumberingSystem.Latn -> resources.getString(R.string.numbering_system_latn)
                            NumberingSystem.Arab -> resources.getString(R.string.numbering_system_arab)
                            NumberingSystem.Arabext -> resources.getString(R.string.numbering_system_arabext)
                        }
                    // 4,5,6 are the only digits that differ between Eastern Arabic and Persian,
                    // so the preview must show them to be useful.
                    if (it == NumberingSystem.Default) {
                        label
                    } else {
                        "$label (${formatWithUnicodeDigits("456", it)})"
                    }
                },
                selectedKey = uiState.settings.numberingSystem.name,
                onSelect = { onAction(InterfaceSettingsUiAction.OnNumberingSystemChange(it)) },
            )
        }
    }
}

@Composable
private fun CalendarsCard(
    uiState: InterfaceSettingsUiState,
    onAction: (InterfaceSettingsUiAction) -> Unit,
) {
    val resources = LocalResources.current
    ACard { cardPadding ->
        Column(
            Modifier.padding(cardPadding),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.element_padding)),
        ) {
            SettingHeader(
                stringResource(R.string.lunar_calendar_language),
                stringResource(R.string.lunar_calendar_language_help),
            )
            val langOptions = listOf<Pair<String?, String>>(
                null to stringResource(R.string.default_value),
            ) + SupportedLocales.map { it.value to it.label }
            BottomSelect(
                modifier = Modifier.fillMaxWidth(),
                options = langOptions,
                optionKey = { it.first ?: "default" },
                optionLabel = { it.second },
                selectedKey = uiState.settings.selectedLocaleForArabicCalendar ?: "default",
                onSelect = { onAction(InterfaceSettingsUiAction.OnLunarLanguageChange(it.first)) },
                searchable = true,
            )

            SettingHeader(
                stringResource(R.string.secondary_calendar),
                stringResource(R.string.secondary_calendar_help),
            )
            BottomSelect(
                modifier = Modifier.fillMaxWidth(),
                options = SecondaryCalendar.entries,
                optionKey = { it.name },
                optionLabel = {
                    when (it) {
                        SecondaryCalendar.Gregorian -> resources.getString(R.string.calendar_gregorian)
                        SecondaryCalendar.Persian -> resources.getString(R.string.calendar_persian)
                        SecondaryCalendar.Ethiopic -> resources.getString(R.string.calendar_ethiopic)
                        SecondaryCalendar.Buddhist -> resources.getString(R.string.calendar_buddhist)
                    }
                },
                selectedKey = uiState.settings.selectedSecondaryCalendar.name,
                onSelect = { onAction(InterfaceSettingsUiAction.OnSecondaryCalendarChange(it)) },
            )
        }
    }
}

@Composable
private fun ThemeGrid(
    selected: ThemeColor,
    onSelect: (ThemeColor) -> Unit,
) {
    val themeEntries = listOf(
        ThemeColor.Default to Triple(R.string.theme_system_default, Color.Transparent, false),
        ThemeColor.Light to Triple(R.string.theme_light, LightSecondaryContainer, false),
        ThemeColor.Dark to Triple(R.string.theme_dark, DarkSurface, false),
        ThemeColor.ClassicDark to Triple(R.string.theme_classic_black, Color.Black, false),
        ThemeColor.ClassicLight to Triple(R.string.theme_classic_light, Color.White, false),
        ThemeColor.Dynamic to Triple(R.string.theme_dynamic, Color(0xFFB3A3F5), false),
    )

    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.element_padding)),
    ) {
        themeEntries.forEach { (theme, meta) ->
            val isSelected = selected == theme
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .width(106.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent,
                        MaterialTheme.shapes.medium,
                    )
                    .selectable(
                        selected = isSelected,
                        role = Role.RadioButton,
                        onClick = { onSelect(theme) },
                    )
                    .padding(4.dp),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(MaterialTheme.shapes.medium)
                        .then(
                            if (theme == ThemeColor.Default) {
                                Modifier.drawBehind {
                                    drawRect(DarkSurface)
                                    val path = Path().apply {
                                        moveTo(size.width, 0f)
                                        lineTo(size.width, size.height)
                                        lineTo(0f, size.height)
                                        close()
                                    }
                                    drawPath(path, LightSecondaryContainer)
                                }
                            } else {
                                Modifier.background(meta.second)
                            },
                        )
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.medium),
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                    RadioButton(isSelected, onClick = null, modifier = Modifier.size(20.dp))
                    Box(
                        modifier = Modifier
                            .padding(start = dimensionResource((R.dimen.element_padding))),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        Text(
                            "", // for reserving space
                            style = MaterialTheme.typography.bodySmall,
                            minLines = 2,
                        )
                        Text(
                            stringResource(meta.first),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.width(IntrinsicSize.Min),
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun InterfaceSettingsPreview() {
    AlAzanThemePreview {
        InterfaceSettingsScreen(uiState = InterfaceSettingsUiState(), onAction = {})
    }
}

@Preview
@Composable
private fun LanguageCardPreview() {
    AlAzanThemePreview {
        LanguageCard(uiState = InterfaceSettingsUiState(), onAction = {})
    }
}

@Preview
@Composable
private fun ThemesCardPreview() {
    AlAzanThemePreview {
        ThemesCard(uiState = InterfaceSettingsUiState(), onAction = {})
    }
}

@Preview
@Composable
private fun PrayerVisibilityCardPreview() {
    AlAzanThemePreview {
        PrayerVisibilityCard(uiState = InterfaceSettingsUiState(), onAction = {})
    }
}

@Preview
@Composable
private fun CountdownCardPreview() {
    AlAzanThemePreview {
        CountdownCard(uiState = InterfaceSettingsUiState(), onAction = {})
    }
}

@Preview
@Composable
private fun HighlightCurrentPrayerCardPreview() {
    AlAzanThemePreview {
        HighlightCurrentPrayerCard(uiState = InterfaceSettingsUiState(), onAction = {})
    }
}

@Preview
@Composable
private fun TimeFormatCardPreview() {
    AlAzanThemePreview {
        TimeFormatCard(uiState = InterfaceSettingsUiState(), onAction = {})
    }
}

@Preview
@Composable
private fun NumberingSystemCardPreview() {
    AlAzanThemePreview {
        NumberingSystemCard(uiState = InterfaceSettingsUiState(), onAction = {})
    }
}

@Preview
@Composable
private fun CalendarsCardPreview() {
    AlAzanThemePreview {
        CalendarsCard(uiState = InterfaceSettingsUiState(), onAction = {})
    }
}
