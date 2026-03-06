package com.github.meypod.al_azan.core.data.repository

import com.github.meypod.al_azan.core.domain.model.system.SystemChange
import com.github.meypod.al_azan.core.domain.repository.SystemChangeRepository
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class SystemChangeRepositoryImpl : SystemChangeRepository {
    private val _data = MutableSharedFlow<SystemChange>(replay = 0, extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    override val data: Flow<SystemChange> = _data.asSharedFlow()

    override fun tryEmit(type: SystemChange): Boolean = _data.tryEmit(type)
}
