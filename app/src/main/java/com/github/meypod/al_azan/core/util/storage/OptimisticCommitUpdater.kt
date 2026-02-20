package com.github.meypod.al_azan.core.util.storage

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Helper to implement an optimistic update() with:
 * - state update performed atomically (lock-free CAS via StateFlow)
 * - commit (write to disk) serialized on a background worker
 * - rapid updates coalesced to the latest value
 */
internal class OptimisticCommitUpdater<T>(
    private val state: MutableStateFlow<T>,
    scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
    private val commit: suspend (newValue: T) -> Unit,
) {
    private val writeQueue = Channel<T>(capacity = Channel.CONFLATED)

    init {
        scope.launch {
            for (value in writeQueue) {
                try {
                    commit(value)
                } catch (t: Throwable) {
                    if (t is CancellationException) throw t
                    // Best-effort persistence: next update will enqueue again.
                }
            }
        }
    }

    fun update(
        transform: (T) -> T,
    ) {
        state.update(transform)
        writeQueue.trySend(state.value)
    }
}
