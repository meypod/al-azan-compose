package com.github.meypod.al_azan.core.presentation.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class WidgetCityNamePos {
  @SerialName("top_start")
  TopStart,
  @SerialName("top_end")
  TopEnd,
}
