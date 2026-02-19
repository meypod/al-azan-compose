package com.github.meypod.al_azan.core.domain.repository

import com.github.meypod.al_azan.core.domain.model.reminder.Reminder
import kotlinx.coroutines.flow.Flow

interface ReminderRepository {
    val data: Flow<List<Reminder>>

    suspend fun fetch(): List<Reminder>

    suspend fun update(transform: (t: List<Reminder>) -> List<Reminder>)
}
