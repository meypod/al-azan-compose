package com.github.meypod.al_azan.main.counter

import androidx.compose.runtime.Immutable
import com.github.meypod.al_azan.core.domain.model.counter.Counter
import com.github.meypod.al_azan.core.domain.model.settings.NumberingSystem

@Immutable
data class CounterUiState(
    val counters: List<Counter> = emptyList(),
    val showLastChangeTime: Boolean = false,
    val numberingSystem: NumberingSystem = NumberingSystem.Default,
    val addDialog: AddCounterDraft? = null,
    val editDialog: EditCounterDraft? = null,
)

@Immutable
data class AddCounterDraft(
    val label: String = "",
    val labelError: Boolean = false,
)

@Immutable
data class EditCounterDraft(
    val id: String,
    val label: String,
    val count: Int,
    val isDefault: Boolean,
    val labelError: Boolean = false,
    val deleteConfirm: Boolean = false,
)
