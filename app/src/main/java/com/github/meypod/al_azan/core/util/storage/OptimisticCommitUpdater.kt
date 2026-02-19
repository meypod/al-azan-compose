package com.github.meypod.al_azan.core.util.storage

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicLong

/**
 * Helper to implement a minimal-lock update() with:
 * - transform() computed outside the lock
 * - commit (write to disk) serialized under a short critical section
 * - automatic retry if state changed during transform()
 */
internal class OptimisticCommitUpdater<T>(
    private val state: MutableStateFlow<T>,
) {
    private val commitMutex = Mutex()
    private val stateVersion = AtomicLong(0)

    suspend fun update(
        transform: (T) -> T,
        commit: suspend (newValue: T) -> Unit,
    ) {
        while (true) {
            val startVersion = stateVersion.get()
            val currentValue = state.value
            val newValue = transform(currentValue)

            val committed =
                commitMutex.withLock {
                    if (stateVersion.get() != startVersion) {
                        return@withLock false
                    }
                    commit(newValue)
                    state.value = newValue
                    stateVersion.incrementAndGet()
                    true
                }

            if (committed) return
        }
    }
}
