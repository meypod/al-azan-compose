package com.github.meypod.al_azan.core.data.repository.old

import com.github.meypod.al_azan.core.data.model.old.OldReminderStore
import com.github.meypod.al_azan.core.data.model.old.toReminder
import com.github.meypod.al_azan.core.domain.model.reminder.Reminder
import com.github.meypod.al_azan.core.domain.repository.ReminderRepository
import com.github.meypod.al_azan.core.util.storage.MMKVDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class OldReminderRepositoryImpl(
    oldReminderStoreDatastore: MMKVDataStore<OldReminderStore>,
) : ReminderRepository {
    override val data: Flow<List<Reminder>> =
        oldReminderStoreDatastore.data.map {
            it.state.reminders.map { reminder -> reminder.toReminder() }
        }

    override suspend fun fetch(): List<Reminder> = data.first()

    override suspend fun update(transform: (t: List<Reminder>) -> List<Reminder>): Unit = throw RuntimeException("Unsupported operation")
}
