package com.github.meypod.al_azan.main.prayer_log

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.meypod.al_azan.core.domain.model.adhan.Prayer
import com.github.meypod.al_azan.core.domain.model.adhan.timesOrNull
import com.github.meypod.al_azan.core.domain.model.prayer_log.LOGGABLE_DAILY_PRAYERS
import com.github.meypod.al_azan.core.domain.model.prayer_log.PrayerRecordStatus
import com.github.meypod.al_azan.core.domain.repository.AlarmSettingsRepository
import com.github.meypod.al_azan.core.domain.repository.CalculationSettingsRepository
import com.github.meypod.al_azan.core.domain.repository.FavoriteLocationsRepository
import com.github.meypod.al_azan.core.domain.repository.PrayerLogRepository
import com.github.meypod.al_azan.core.domain.repository.SettingsRepository
import com.github.meypod.al_azan.core.domain.usecase.GetShariaTimesUseCase
import com.github.meypod.al_azan.core.domain.util.formatTime
import com.github.meypod.al_azan.core.domain.util.toLocalDate
import com.github.meypod.al_azan.core.presentation.navigation.NavigationController
import com.github.meypod.al_azan.core.presentation.navigation.Route
import com.github.meypod.al_azan.prayer_checkin.PrayerCheckInFinalizer
import com.github.meypod.al_azan.prayer_checkin.PrayerStatusEditor
import com.github.meypod.al_azan.prayer_checkin.nextPrayerStatus
import com.github.meypod.al_azan.prayer_checkin.localNoonInstant
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import javax.inject.Inject
import kotlin.time.Clock

@HiltViewModel
class PrayerLogViewModel @Inject constructor(
    private val prayerLogRepository: PrayerLogRepository,
    private val settingsRepository: SettingsRepository,
    private val alarmSettingsRepository: AlarmSettingsRepository,
    private val calculationSettingsRepository: CalculationSettingsRepository,
    private val favoriteLocationsRepository: FavoriteLocationsRepository,
    private val getShariaTimesUseCase: GetShariaTimesUseCase,
    private val prayerCheckInFinalizer: PrayerCheckInFinalizer,
    private val prayerStatusEditor: PrayerStatusEditor,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PrayerLogUiState())
    val uiState: StateFlow<PrayerLogUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            prayerCheckInFinalizer.finalizePassedPrayers()
        }

        viewModelScope.launch {
            settingsRepository.data.collectLatest { s ->
                _uiState.update { it.copy(settings = s) }
                recomputeTimes()
            }
        }

        viewModelScope.launch {
            alarmSettingsRepository.data.collectLatest { alarm ->
                _uiState.update { it.copy(prayerCheckInEnabled = alarm.prayerCheckInEnabled) }
            }
        }

        viewModelScope.launch {
            calculationSettingsRepository.data.collectLatest { calc ->
                _uiState.update { it.copy(hijriDateAdjustment = calc.calculationAdjustments.hijriDate) }
                recomputeTimes()
            }
        }

        viewModelScope.launch {
            prayerLogRepository.data.collectLatest { records ->
                val date = _uiState.value.selectedDate
                val current = records.firstOrNull { it.date == date }
                val cutoff = Clock.System.now().toLocalDate().minus(DatePeriod(days = RECENT_HISTORY_DAYS))
                val nonEmpty = records
                    .filter { it.statuses.isNotEmpty() && it.date >= cutoff }
                    .sortedByDescending { it.date }
                _uiState.update {
                    it.copy(
                        currentRecord = current,
                        recentRecords = nonEmpty,
                    )
                }
            }
        }
    }

    fun onAction(action: PrayerLogUiAction) {
        when (action) {
            PrayerLogUiAction.OnPrevDayClick -> changeDate(_uiState.value.selectedDate.minus(DatePeriod(days = 1)))
            PrayerLogUiAction.OnNextDayClick -> changeDate(_uiState.value.selectedDate.plus(DatePeriod(days = 1)))
            PrayerLogUiAction.OnTodayClick -> changeDate(Clock.System.now().toLocalDate())
            is PrayerLogUiAction.OnDatePicked -> changeDate(action.date)
            is PrayerLogUiAction.OnSetPrayerStatus -> setPrayerStatus(action.prayer, action.status)
            is PrayerLogUiAction.OnTogglePrayerStatus -> togglePrayerStatus(action.prayer)
            PrayerLogUiAction.OnOpenCheckInSettingsClick ->
                NavigationController.navigateTo(Route.Main.Settings.SoundAndNotifications)
        }
    }

    private fun changeDate(newDate: LocalDate) {
        viewModelScope.launch {
            val records = prayerLogRepository.fetch()
            val current = records.firstOrNull { it.date == newDate }
            _uiState.update {
                it.copy(
                    selectedDate = newDate,
                    currentRecord = current,
                    today = Clock.System.now().toLocalDate(),
                )
            }
            recomputeTimes()
        }
    }

    private fun setPrayerStatus(
        prayer: Prayer,
        status: PrayerRecordStatus?,
    ) {
        val date = _uiState.value.selectedDate
        viewModelScope.launch {
            prayerStatusEditor.setStatus(date, prayer, status)
        }
    }

    private fun togglePrayerStatus(prayer: Prayer) {
        val nextStatus = nextPrayerStatus(_uiState.value.currentRecord?.statusFor(prayer))
        setPrayerStatus(prayer, nextStatus)
    }

    private suspend fun recomputeTimes() {
        val date = _uiState.value.selectedDate
        val calc = calculationSettingsRepository.fetch()
        val params = calc.parameters ?: return
        val location = favoriteLocationsRepository.fetch()
            .firstOrNull { it.id == calc.locationId }?.locationDetail ?: return
        val settings = settingsRepository.fetch()

        val times = getShariaTimesUseCase(
            instant = localNoonInstant(date),
            calculationParameters = params,
            calculationAdjustments = calc.calculationAdjustments,
            arabicCalendar = settings.selectedArabicCalendar,
            locationDetail = location,
            midnightMethod = calc.midnightMethod,
        ).timesOrNull ?: return

        val formattedMap = LOGGABLE_DAILY_PRAYERS.associateWith { prayer ->
            settings.formatTime(times.forPrayer(prayer).toEpochMilliseconds())
        }
        _uiState.update { it.copy(prayerTimes = formattedMap) }
    }
}

/** The recent-history card only lists this many days back; older days stay reachable via the date picker. */
private const val RECENT_HISTORY_DAYS = 30
