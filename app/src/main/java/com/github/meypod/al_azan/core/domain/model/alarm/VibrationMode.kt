package com.github.meypod.al_azan.core.domain.model.alarm

import kotlinx.serialization.Serializable

@Serializable
enum class VibrationMode {
    Off,
    Once,
    Continuous,
}
