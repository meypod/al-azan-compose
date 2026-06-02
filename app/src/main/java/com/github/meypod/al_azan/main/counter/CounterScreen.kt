package com.github.meypod.al_azan.main.counter

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.github.meypod.al_azan.R
import com.github.meypod.al_azan.core.domain.model.counter.Counter
import com.github.meypod.al_azan.core.domain.model.settings.NumberingSystem
import com.github.meypod.al_azan.core.domain.util.DateDiff
import com.github.meypod.al_azan.core.domain.util.formatWithUnicodeDigits
import com.github.meypod.al_azan.core.domain.util.getDateDiff
import com.github.meypod.al_azan.core.presentation.AlAzanThemePreview
import com.github.meypod.al_azan.core.presentation.components.ACard
import com.github.meypod.al_azan.core.presentation.components.ReorderableLazyColumn
import com.github.meypod.al_azan.core.presentation.components.SettingSwitch
import com.github.meypod.al_azan.main.counter.components.AddCounterDialog
import com.github.meypod.al_azan.main.counter.components.EditCounterDialog
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CounterScreen(
    uiState: CounterUiState,
    onAction: (CounterUiAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(uiState.showLastChangeTime) {
        if (!uiState.showLastChangeTime) return@LaunchedEffect
        while (true) {
            now = System.currentTimeMillis()
            delay(30_000)
        }
    }
    Scaffold(
        modifier = modifier,
        topBar = {
            CenterAlignedTopAppBar(
                navigationIcon = {
                    IconButton(onClick = { onAction(CounterUiAction.OnBackClick) }) {
                        Icon(painterResource(R.drawable.arrow_back), null)
                    }
                },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(painterResource(R.drawable.counter), null)
                        Text(stringResource(R.string.qada_counter_title))
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onAction(CounterUiAction.OnAddClick) },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ) {
                Icon(painterResource(R.drawable.add), contentDescription = stringResource(R.string.add_counter))
            }
        },
    ) { paddingValues ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(dimensionResource(R.dimen.page_padding)),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.element_padding)),
        ) {
            ACard { cardPadding ->
                Box(Modifier.padding(cardPadding)) {
                    SettingSwitch(
                        title = stringResource(R.string.show_last_change_time),
                        subtitle = null,
                        checked = uiState.showLastChangeTime,
                        onCheckedChange = { onAction(CounterUiAction.OnShowLastChangeToggle(it)) },
                    )
                }
            }

            uiState.addDialog?.let { AddCounterDialog(draft = it, onAction = onAction) }
            uiState.editDialog?.let { EditCounterDialog(draft = it, onAction = onAction) }

            ReorderableLazyColumn(
                items = uiState.counters,
                key = { it.id },
                onMove = { from, to -> onAction(CounterUiAction.OnMove(from, to)) },
                modifier = Modifier.weight(1f).fillMaxWidth(),
                itemContent = { counter, isPlaceholder, itemModifier, dragHandleModifier ->
                    CounterRow(
                        counter = counter,
                        onAction = onAction,
                        showLastChange = uiState.showLastChangeTime,
                        numberingSystem = uiState.numberingSystem,
                        now = now,
                        modifier = itemModifier.graphicsLayer { alpha = if (isPlaceholder) 0f else 1f },
                        dragHandleModifier = dragHandleModifier,
                    )
                },
                verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.element_padding)),
                footerContent = { Spacer(Modifier.height(96.dp)) },
            )
        }
    }
}

@Composable
private fun CounterRow(
    counter: Counter,
    onAction: (CounterUiAction) -> Unit,
    showLastChange: Boolean,
    numberingSystem: NumberingSystem,
    now: Long,
    modifier: Modifier = Modifier,
    dragHandleModifier: Modifier = Modifier,
) {
    ACard(
        modifier = modifier.fillMaxWidth(),
        paddingValues = PaddingValues(horizontal = dimensionResource(R.dimen.card_padding_h)),
    ) { contentPadding ->
        Column(Modifier.padding(contentPadding)) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.element_padding)),
            ) {
                Icon(
                    painterResource(R.drawable.grip),
                    contentDescription = stringResource(R.string.drag_handle_description),
                    modifier = dragHandleModifier,
                )
                Surface(
                    onClick = { onAction(CounterUiAction.OnDecrement(counter.id)) },
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    shadowElevation = 2.dp,
                ) {
                    Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                        Icon(painterResource(R.drawable.minus), stringResource(R.string.decrement))
                    }
                }
                Column(
                    Modifier
                        .weight(1f)
                        .clickable { onAction(CounterUiAction.OnRowClick(counter.id)) }
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = counterDisplayLabel(counter),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = formatWithUnicodeDigits(counter.count.toString(), numberingSystem),
                        style = MaterialTheme.typography.titleLarge,
                    )
                    if (showLastChange && counter.lastModified != null && counter.lastCount != null) {
                        Text(
                            text = formatLastChange(counter, numberingSystem, now),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Surface(
                    onClick = { onAction(CounterUiAction.OnIncrement(counter.id)) },
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    shadowElevation = 2.dp,
                ) {
                    Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                        Icon(painterResource(R.drawable.add), stringResource(R.string.increment))
                    }
                }
            }
        }
    }
}

@Composable
private fun formatLastChange(
    counter: Counter,
    numberingSystem: NumberingSystem,
    now: Long,
): String {
    val diff = getDateDiff(now, counter.lastModified ?: 0L)
    val formatted = stringResource(
        R.string.counter_last_change,
        formatDateDiff(diff),
        counter.lastCount ?: 0,
        counter.count,
    )
    return formatWithUnicodeDigits(formatted, numberingSystem)
}

@Composable
private fun formatDateDiff(diff: DateDiff): String {
    if (diff.days == 0L && diff.hours == 0L && diff.minutes == 0L) {
        return stringResource(R.string.time_just_now)
    }
    val d = stringResource(R.string.time_unit_day_short)
    val h = stringResource(R.string.time_unit_hour_short)
    val m = stringResource(R.string.time_unit_minute_short)
    val tense = stringResource(if (diff.future) R.string.time_tense_later else R.string.time_tense_ago)
    return when (diff.days) {
        0L if diff.hours == 0L -> "${diff.minutes}$m $tense"
        0L -> "${diff.hours}$h ${diff.minutes}$m $tense"
        else -> "${diff.days}$d ${diff.hours}$h $tense"
    }
}

@Preview
@Composable
private fun CounterScreenPreview() {
    AlAzanThemePreview {
        CounterScreen(
            uiState = CounterUiState(
                counters = listOf(
                    Counter(id = "1", label = "Subhanallah", count = 33),
                    Counter(
                        id = "2",
                        label = "Alhamdulillah",
                        count = 27,
                        lastModified = System.currentTimeMillis(),
                        lastCount = 26,
                    ),
                ),
                showLastChangeTime = true,
            ),
            onAction = {},
        )
    }
}
