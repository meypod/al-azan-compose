package com.github.meypod.al_azan.core.domain.repository

import androidx.compose.runtime.Stable
import com.github.meypod.al_azan.core.domain.model.settings.Settings
import com.github.meypod.al_azan.core.domain.model.system.SystemChange
import kotlinx.coroutines.flow.Flow

/**
 * a simple repository for letting other parts of application know about a system change, like time & timezone change, to refresh ui when necessary
 */
@Stable
interface SystemChangeRepository {
    val data: Flow<SystemChange>

    fun tryEmit(type: SystemChange): Boolean
}
