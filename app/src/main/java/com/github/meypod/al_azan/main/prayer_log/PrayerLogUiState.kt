package com.github.meypod.al_azan.main.prayer_log

import androidx.compose.runtime.Immutable
import com.github.meypod.al_azan.core.domain.model.adhan.Prayer
import com.github.meypod.al_azan.core.domain.model.prayer_log.DailyPrayerRecord
import com.github.meypod.al_azan.core.domain.model.settings.Settings
import com.github.meypod.al_azan.core.domain.util.toLocalDate
import kotlinx.datetime.LocalDate
import kotlin.time.Clock

@Immutable
data class PrayerLogUiState(
    val selectedDate: LocalDate = Clock.System.now().toLocalDate(),
    val currentRecord: DailyPrayerRecord? = null,
    val prayerTimes: Map<Prayer, String> = emptyMap(),
    val settings: Settings = Settings(selectedLocale = "en"),
    val hijriDateAdjustment: Int = 0,
    val prayerCheckInEnabled: Boolean = false,
    val recentRecords: List<DailyPrayerRecord> = emptyList(),
    val today: LocalDate = Clock.System.now().toLocalDate(),
)
