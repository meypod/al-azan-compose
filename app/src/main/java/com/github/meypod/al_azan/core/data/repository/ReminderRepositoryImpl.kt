package com.github.meypod.al_azan.core.data.repository

import com.github.meypod.al_azan.core.domain.model.reminder.Reminder
import com.github.meypod.al_azan.core.domain.repository.ReminderRepository
import com.github.meypod.al_azan.core.util.storage.MMKVDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class ReminderRepositoryImpl(
    private val reminderStoreDatastore: MMKVDataStore<List<Reminder>>,
) : ReminderRepository {
    override val data: Flow<List<Reminder>>
        get() = reminderStoreDatastore.data

    override suspend fun fetch(): List<Reminder> = data.first()

    override suspend fun update(transform: (t: List<Reminder>) -> List<Reminder>) {
        reminderStoreDatastore.update(transform)
    }
}
