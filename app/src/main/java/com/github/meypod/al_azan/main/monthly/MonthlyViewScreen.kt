package com.github.meypod.al_azan.main.monthly

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FabPosition
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.github.meypod.al_azan.R
import com.github.meypod.al_azan.core.presentation.AlAzanThemePreview
import com.github.meypod.al_azan.core.presentation.DarkOnTertiaryContainer
import com.github.meypod.al_azan.core.presentation.DarkTertiaryContainer
import com.github.meypod.al_azan.core.presentation.LightOnTertiaryContainer
import com.github.meypod.al_azan.core.presentation.LightTertiaryContainer
import com.github.meypod.al_azan.core.presentation.components.ACard
import com.github.meypod.al_azan.core.presentation.components.ScreenScaffold
import com.github.meypod.al_azan.core.presentation.navigation.NavigationController
import com.github.meypod.al_azan.core.presentation.util.dropShadow2
import com.github.meypod.al_azan.core.presentation.util.swipeNavigate

// TODO
@Composable
fun MonthlyViewScreen(
    uiState: MonthlyViewUiState,
    onAction: (MonthlyViewUiAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    ScreenScaffold(
        title = stringResource(R.string.monthly_view_title),
        onBackClick = { NavigationController.navigateBack() },
        modifier = modifier,
        floatingActionButton = {
            AnimatedVisibility(
                modifier = Modifier.graphicsLayer { clip = false },
                visible = !uiState.isCurrentMonth,
                enter = slideInVertically(
                    initialOffsetY = { it / 2 },
                    animationSpec = tween(durationMillis = 150),
                ),
                exit = slideOutVertically(
                    targetOffsetY = { it * 3 },
                    animationSpec = tween(durationMillis = 200),
                ),
            ) {
                val buttonShape = MaterialTheme.shapes.extraLarge
                // classic themes use the high-contrast scheme; keep this button on the
                // normal tertiary tones so it looks the same across themes
                val dark = uiState.themeColor.isDark()
                val containerColor = if (uiState.themeColor.isClassic()) {
                    if (dark) DarkTertiaryContainer else LightTertiaryContainer
                } else {
                    MaterialTheme.colorScheme.tertiaryContainer
                }
                val contentColor = if (uiState.themeColor.isClassic()) {
                    if (dark) DarkOnTertiaryContainer else LightOnTertiaryContainer
                } else {
                    MaterialTheme.colorScheme.onTertiaryContainer
                }
                ExtendedFloatingActionButton(
                    onClick = { onAction(MonthlyViewUiAction.OnShowThisMonthClick) },
                    shape = buttonShape,
                    modifier = Modifier
                        .widthIn(min = 160.dp)
                        .dropShadow2(buttonShape),
                    containerColor = containerColor,
                    contentColor = contentColor,
                ) {
                    Text(
                        stringResource(R.string.show_this_month),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        },
        floatingActionButtonPosition = FabPosition.Center,
    ) {
        ACard { cardPadding ->
            Column(
                Modifier
                    .padding(cardPadding)
                    .swipeNavigate(
                        onNext = { onAction(MonthlyViewUiAction.OnNextMonthClick) },
                        onPrev = { onAction(MonthlyViewUiAction.OnPrevMonthClick) },
                    ),
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    OutlinedButton(onClick = { onAction(MonthlyViewUiAction.OnPrevMonthClick) }) {
                        Icon(painterResource(R.drawable.arrow_back), null)
                        Text(stringResource(R.string.prev_month), modifier = Modifier.padding(start = 6.dp))
                    }
                    OutlinedButton(onClick = { onAction(MonthlyViewUiAction.OnNextMonthClick) }) {
                        Text(stringResource(R.string.next_month), modifier = Modifier.padding(end = 6.dp))
                        Icon(painterResource(R.drawable.arrow_forward), null)
                    }
                }
                MonthLabelButton(
                    label = uiState.monthLabel,
                    calendarMode = uiState.calendarMode,
                    onClick = { onAction(MonthlyViewUiAction.OnToggleCalendarClick) },
                )

                HeaderRow()
                HorizontalDivider()
                uiState.rows.forEach { row ->
                    DayRow(row)
                    if (row.isToday) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.primary)
                    } else {
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthLabelButton(
    label: String,
    calendarMode: MonthlyCalendarMode,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val contentColor = when (calendarMode) {
        MonthlyCalendarMode.SECONDARY -> MaterialTheme.colorScheme.tertiary
        MonthlyCalendarMode.LUNAR -> MaterialTheme.colorScheme.secondary
    }
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(1.dp, contentColor),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = contentColor),
    ) {
        Text(label, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun HeaderRow() {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        listOf(R.string.date_column, R.string.fajr, R.string.dhuhr, R.string.asr, R.string.maghrib, R.string.isha).forEach {
            Text(
                stringResource(it),
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DayRow(row: MonthlyDayRow) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        listOf(row.day, row.fajr, row.dhuhr, row.asr, row.maghrib, row.isha).forEach {
            Text(
                it,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (row.isToday) FontWeight.ExtraBold else FontWeight.Normal,
                color = if (row.isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun previewRows() = (1..31).map { MonthlyDayRow(it.toString(), "03:59", "03:59", "03:59", "03:59", "03:59", isToday = it == 15) }

@Preview(showBackground = true)
@Composable
private fun MonthLabelButtonSecondaryPreview() {
    AlAzanThemePreview {
        MonthLabelButton(
            label = "1403, Khordad",
            calendarMode = MonthlyCalendarMode.SECONDARY,
            onClick = {},
            modifier = Modifier.padding(10.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MonthLabelButtonLunarPreview() {
    AlAzanThemePreview {
        MonthLabelButton(
            label = "1445, Dhu al-Qadah",
            calendarMode = MonthlyCalendarMode.LUNAR,
            onClick = {},
            modifier = Modifier.padding(10.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun HeaderRowPreview() {
    AlAzanThemePreview {
        HeaderRow()
    }
}

@Preview(showBackground = true)
@Composable
private fun DayRowPreview() {
    AlAzanThemePreview {
        Column {
            DayRow(MonthlyDayRow("15", "03:59", "13:00", "16:30", "20:15", "21:45", isToday = true))
            DayRow(MonthlyDayRow("16", "04:00", "13:00", "16:30", "20:16", "21:46"))
        }
    }
}

@Preview(heightDp = 600)
@Composable
private fun MonthlyViewPreview() {
    AlAzanThemePreview {
        MonthlyViewScreen(
            uiState = MonthlyViewUiState(
                monthLabel = "1403, Khordad",
                rows = previewRows(),
            ),
            onAction = {},
        )
    }
}
