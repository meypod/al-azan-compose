package com.github.meypod.al_azan.main.reminder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.meypod.al_azan.core.domain.audio.AudioPreviewPlayer
import com.github.meypod.al_azan.core.domain.model.adhan.Prayer
import com.github.meypod.al_azan.core.domain.model.adhan.ShariaTimes
import com.github.meypod.al_azan.core.domain.model.adhan.timesOrNull
import com.github.meypod.al_azan.core.domain.model.alarm.SkippedAlarm
import com.github.meypod.al_azan.core.domain.model.audio.DeviceRingtone
import com.github.meypod.al_azan.core.domain.model.reminder.MonthlyCalendar
import com.github.meypod.al_azan.core.domain.model.reminder.Reminder
import com.github.meypod.al_azan.core.domain.model.reminder.ReminderAudioEntry
import com.github.meypod.al_azan.core.domain.model.reminder.ReminderInterval
import com.github.meypod.al_azan.core.domain.model.reminder.ReminderMonthly
import com.github.meypod.al_azan.core.domain.model.reminder.nextOccurrence
import com.github.meypod.al_azan.core.domain.model.reminder.nextTrigger
import com.github.meypod.al_azan.core.domain.model.reminder.reminderCalendars
import com.github.meypod.al_azan.core.domain.model.settings.AudioEntry
import com.github.meypod.al_azan.core.domain.repository.CalculationSettingsRepository
import com.github.meypod.al_azan.core.domain.repository.FavoriteLocationsRepository
import com.github.meypod.al_azan.core.domain.repository.ReminderRepository
import com.github.meypod.al_azan.core.domain.repository.RingtoneRepository
import com.github.meypod.al_azan.core.domain.repository.SettingsRepository
import com.github.meypod.al_azan.core.domain.usecase.GetShariaTimesUseCase
import com.github.meypod.al_azan.core.domain.util.calendarDayOf
import com.github.meypod.al_azan.core.domain.util.calendarMaxMonthLength
import com.github.meypod.al_azan.core.domain.util.toLocalDate
import com.github.meypod.al_azan.core.presentation.dialog.SchedulingPermission
import com.github.meypod.al_azan.core.presentation.dialog.withDontAskAgain
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import java.util.UUID
import javax.inject.Inject
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/** The fields that decide when a reminder fires; a change here makes its old skips meaningless. */
private fun Reminder.schedule() = listOf(prayer, duration, durationModifier, once, days, interval, monthly, onceDate)

/** What moves a draft's fire time; the one-off check reruns only when it changes. */
private data class OnceCheckKey(
    val prayer: Prayer,
    val duration: Int,
    val modifier: ReminderTimeModifier,
    val repeat: ReminderRepeat,
)

/** Id for checking a draft; it only matters for matching skips, which draft checks never pass. */
private const val DRAFT_ID = "draft"

@HiltViewModel
class ReminderViewModel @Inject constructor(
    private val reminderRepository: ReminderRepository,
    private val settingsRepository: SettingsRepository,
    private val calculationSettingsRepository: CalculationSettingsRepository,
    private val favoriteLocationsRepository: FavoriteLocationsRepository,
    private val getShariaTimesUseCase: GetShariaTimesUseCase,
    private val ringtoneRepository: RingtoneRepository,
    private val audioPreviewPlayer: AudioPreviewPlayer,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ReminderUiState())

    /**
     * Bumped when the screen resumes: the expiry wait uses uptime, which stops in deep sleep, and the
     * clock or time zone may have changed meanwhile.
     */
    private val expiryCheckRequests = MutableStateFlow(0)
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            reminderRepository.data.collect { list ->
                _uiState.update { it.copy(reminders = list) }
            }
        }
        viewModelScope.launch {
            settingsRepository.data.collect { settings ->
                val sounds = settings.savedUserAudioEntries.mapNotNull { it.toReminderAudioEntry() }
                _uiState.update {
                    it.copy(userSounds = sounds, adhanEntries = settings.savedAdhanAudioEntries, settings = settings)
                }
            }
        }
        viewModelScope.launch {
            calculationSettingsRepository.data.collect { calc ->
                _uiState.update { it.copy(hijriDateAdjustment = calc.calculationAdjustments.hijriDate) }
            }
        }
        viewModelScope.launch {
            val ringtones = ringtoneRepository.getDeviceRingtones().map { it.toReminderAudioEntry() }
            _uiState.update { it.copy(deviceSounds = ringtones) }
        }
        viewModelScope.launch {
            // Latest only: each draft edit supersedes the previous check.
            // Only what moves the fire time; labels, sounds and vibration don't.
            uiState.map { s -> s.editDraft?.let { d -> OnceCheckKey(d.prayer, d.duration, d.modifier, d.repeat) } }
                .distinctUntilChanged()
                .collectLatest { checkOnceDate(_uiState.value.editDraft) }
        }
        viewModelScope.launch {
            combine(
                reminderRepository.data,
                settingsRepository.data,
                calculationSettingsRepository.data,
                favoriteLocationsRepository.data,
                expiryCheckRequests,
            ) { reminders, _, _, _, _ -> reminders }.collectLatest(::trackExpiredOnceReminders)
        }
        viewModelScope.launch {
            audioPreviewPlayer.playingId.collect { id ->
                _uiState.update { it.copy(playingSoundId = id) }
            }
        }
    }

    fun onAction(action: ReminderUiAction) {
        when (action) {
            ReminderUiAction.OnAddClick -> onAddClick()
            is ReminderUiAction.OnToggleEnabled -> onToggleEnabled(action)
            is ReminderUiAction.OnSetEnabled -> onSetEnabled(action)
            is ReminderUiAction.OnItemClick -> onItemClick(action)
            is ReminderUiAction.OnItemLongPress -> onItemLongPress(action)
            is ReminderUiAction.OnSelectionToggle -> onSelectionToggle(action)
            ReminderUiAction.OnSelectAllToggle -> onSelectAllToggle()
            ReminderUiAction.OnExitSelectionMode -> onExitSelectionMode()
            ReminderUiAction.OnBulkTurnOn -> onBulkTurnOn()
            ReminderUiAction.OnBulkTurnOff -> onBulkTurnOff()
            ReminderUiAction.OnBulkDelete -> onBulkDelete()
            is ReminderUiAction.OnContextMenuOpen -> onContextMenuOpen(action)
            ReminderUiAction.OnContextMenuDismiss -> onContextMenuDismiss()
            is ReminderUiAction.OnEditClick -> onEditClick(action)
            is ReminderUiAction.OnDeleteClick -> onDeleteClick(action)
            is ReminderUiAction.OnDuplicateClick -> onDuplicateClick(action)
            ReminderUiAction.OnConfirmDelete -> onConfirmDelete()
            ReminderUiAction.OnCancelDelete -> onCancelDelete()
            ReminderUiAction.OnDraftDismiss -> onDraftDismiss()
            ReminderUiAction.OnDraftSave -> onDraftSave()
            is ReminderUiAction.OnDraftLabelChange -> onDraftLabelChange(action)
            is ReminderUiAction.OnDraftDurationChange -> onDraftDurationChange(action)
            is ReminderUiAction.OnDraftModifierChange -> onDraftModifierChange(action)
            is ReminderUiAction.OnDraftPrayerChange -> onDraftPrayerChange(action)
            is ReminderUiAction.OnDraftVibrationChange -> onDraftVibrationChange(action)
            is ReminderUiAction.OnDraftSoundChange -> onDraftSoundChange(action)
            is ReminderUiAction.OnAddSoundFile -> onAddSoundFile(action)
            is ReminderUiAction.OnPreviewSound -> onPreviewSound(action)
            ReminderUiAction.OnStopPreview -> onStopPreview()
            is ReminderUiAction.OnDraftDayToggle -> onDraftDayToggle(action)
            is ReminderUiAction.OnDraftRepeatChange -> onDraftRepeatChange(action.option)
            is ReminderUiAction.OnDraftIntervalStartChange -> onDraftIntervalStartChange(action.date)
            ReminderUiAction.OnResume -> expiryCheckRequests.update { it + 1 }
            is ReminderUiAction.OnDraftOnceDateChange -> updateDraft { d ->
                if (d.repeat is ReminderRepeat.Once) d.copy(repeat = ReminderRepeat.Once(action.date, picked = true)) else d
            }
            is ReminderUiAction.OnDraftMonthlyCalendarChange -> onDraftMonthlyCalendarChange(action.calendar)
            is ReminderUiAction.OnDraftMonthlyDayChange -> updateMonthly { it.copy(day = action.day) }
            is ReminderUiAction.OnPermissionDontAskAgain -> onPermissionDontAskAgain(action.permission)
        }
    }

    private fun onPermissionDontAskAgain(permission: SchedulingPermission) {
        viewModelScope.launch {
            settingsRepository.update { it.withDontAskAgain(permission) }
        }
    }

    private fun onAddClick() {
        _uiState.update { it.copy(editDraft = ReminderEditDraft()) }
    }

    private fun onToggleEnabled(action: ReminderUiAction.OnToggleEnabled) {
        viewModelScope.launch {
            reminderRepository.update { list -> list.map { if (it.id == action.id) it.copy(enabled = action.enabled) else it } }
        }
    }

    private fun onSetEnabled(action: ReminderUiAction.OnSetEnabled) {
        viewModelScope.launch {
            reminderRepository.update { list -> list.map { if (it.id in action.ids) it.copy(enabled = action.enabled) else it } }
        }
    }

    private fun onItemClick(action: ReminderUiAction.OnItemClick) {
        if (_uiState.value.selectionMode) {
            toggleSelection(action.id)
        } else {
            val r = _uiState.value.reminders.firstOrNull { it.id == action.id } ?: return
            _uiState.update { it.copy(editDraft = r.toDraft()) }
        }
    }

    private fun onItemLongPress(action: ReminderUiAction.OnItemLongPress) {
        _uiState.update {
            it.copy(
                selectionMode = true,
                selectedIds = it.selectedIds + action.id,
            )
        }
    }

    private fun onSelectionToggle(action: ReminderUiAction.OnSelectionToggle) {
        toggleSelection(action.id)
    }

    private fun onSelectAllToggle() {
        _uiState.update {
            if (it.allSelected()) it.copy(selectedIds = emptySet()) else it.copy(selectedIds = it.reminders.map(Reminder::id).toSet())
        }
    }

    private fun onExitSelectionMode() {
        _uiState.update { it.copy(selectionMode = false, selectedIds = emptySet()) }
    }

    private fun onBulkTurnOn() {
        val ids = _uiState.value.selectedIds
        viewModelScope.launch {
            reminderRepository.update { list -> list.map { if (it.id in ids) it.copy(enabled = true) else it } }
        }
        _uiState.update { it.copy(selectionMode = false, selectedIds = emptySet()) }
    }

    private fun onBulkTurnOff() {
        val ids = _uiState.value.selectedIds
        viewModelScope.launch {
            reminderRepository.update { list -> list.map { if (it.id in ids) it.copy(enabled = false) else it } }
        }
        _uiState.update { it.copy(selectionMode = false, selectedIds = emptySet()) }
    }

    private fun onBulkDelete() {
        _uiState.update { it.copy(deletingBulk = true) }
    }

    private fun onContextMenuOpen(action: ReminderUiAction.OnContextMenuOpen) {
        _uiState.update { it.copy(contextMenuId = action.id) }
    }

    private fun onContextMenuDismiss() {
        _uiState.update { it.copy(contextMenuId = null) }
    }

    private fun onEditClick(action: ReminderUiAction.OnEditClick) {
        val r = _uiState.value.reminders.firstOrNull { it.id == action.id } ?: return
        _uiState.update { it.copy(editDraft = r.toDraft(), contextMenuId = null) }
    }

    private fun onDeleteClick(action: ReminderUiAction.OnDeleteClick) {
        _uiState.update { it.copy(deletingReminderId = action.id, contextMenuId = null, editDraft = null) }
    }

    private fun onDuplicateClick(action: ReminderUiAction.OnDuplicateClick) {
        val source = _uiState.value.reminders.firstOrNull { it.id == action.id } ?: return
        val copy = source.copy(
            id = UUID.randomUUID().toString(),
            label = source.label + " (copy)",
        )
        viewModelScope.launch {
            reminderRepository.update { list -> list + copy }
        }
        _uiState.update { it.copy(editDraft = null) }
    }

    private fun onCancelDelete() {
        _uiState.update { it.copy(deletingReminderId = null, deletingBulk = false) }
    }

    private fun onDraftDismiss() {
        audioPreviewPlayer.stop()
        _uiState.update { it.copy(editDraft = null) }
    }

    private fun onDraftSave() {
        audioPreviewPlayer.stop()
        saveDraft()
    }

    private fun onDraftLabelChange(action: ReminderUiAction.OnDraftLabelChange) = updateDraft { it.copy(label = action.value) }

    private fun onDraftDurationChange(action: ReminderUiAction.OnDraftDurationChange) = updateDraft { it.copy(duration = action.value) }

    private fun onDraftModifierChange(action: ReminderUiAction.OnDraftModifierChange) = updateDraft { it.copy(modifier = action.value) }

    private fun onDraftPrayerChange(action: ReminderUiAction.OnDraftPrayerChange) = updateDraft { it.copy(prayer = action.value) }

    private fun onDraftVibrationChange(action: ReminderUiAction.OnDraftVibrationChange) = updateDraft { it.copy(vibration = action.value) }

    private fun onDraftSoundChange(action: ReminderUiAction.OnDraftSoundChange) = updateDraft { it.copy(sound = action.value) }

    private fun onAddSoundFile(action: ReminderUiAction.OnAddSoundFile) {
        val id = "audio_" + UUID.randomUUID().toString()
        // Persist to the shared user sounds so it also shows up in the muezzin picker.
        viewModelScope.launch {
            settingsRepository.update {
                it.copy(
                    savedUserAudioEntries = it.savedUserAudioEntries + AudioEntry.ExternalAudioEntry(
                        id = id,
                        filepath = action.filepath,
                        label = action.label,
                    ),
                )
            }
        }
        updateDraft {
            it.copy(
                sound = ReminderAudioEntry.ExternalReminderAudioEntry(
                    id = id,
                    filepath = action.filepath,
                    label = action.label,
                ),
            )
        }
    }

    private fun onPreviewSound(action: ReminderUiAction.OnPreviewSound) = audioPreviewPlayer.play(action.value)

    private fun onStopPreview() = audioPreviewPlayer.stop()

    private fun onDraftDayToggle(action: ReminderUiAction.OnDraftDayToggle) =
        updateDraft { d ->
            val newDays = if (action.day in d.days) d.days - action.day else d.days + action.day
            // The last day stays selected: a weekly reminder with no day would never fire.
            if (newDays.isEmpty()) d else d.copy(days = newDays)
        }

    private fun onDraftRepeatChange(option: RepeatOption) =
        updateDraft { d ->
            // The current mode's value joins the remembered ones, so switching back restores it.
            val memory = d.otherRepeats.filterNot { it::class == d.repeat::class } + d.repeat
            val repeat = when (option) {
                RepeatOption.Once -> memory.filterIsInstance<ReminderRepeat.Once>().lastOrNull() ?: ReminderRepeat.Once()
                RepeatOption.Weekdays -> ReminderRepeat.Weekdays
                is RepeatOption.EveryDays -> {
                    // Changing the length keeps the chosen start date; a new interval starts today.
                    val start = memory.filterIsInstance<ReminderRepeat.Every>().lastOrNull()?.interval?.startDate ?: today()
                    ReminderRepeat.Every(ReminderInterval(everyDays = option.days, startDate = start))
                }

                RepeatOption.Monthly -> memory.filterIsInstance<ReminderRepeat.Monthly>().lastOrNull() ?: run {
                    // The user's secondary calendar, starting on today's day so the first reminder is
                    // at most a month away.
                    val calendar = MonthlyCalendar.Civil(_uiState.value.settings.selectedSecondaryCalendar)
                    ReminderRepeat.Monthly(ReminderMonthly(calendar, calendarDayOf(today(), calendars().systemFor(calendar)).day))
                }
            }
            d.copy(repeat = repeat, otherRepeats = memory.filterNot { it::class == repeat::class })
        }

    private fun onDraftIntervalStartChange(date: LocalDate) =
        updateDraft { d ->
            val repeat = d.repeat as? ReminderRepeat.Every ?: return@updateDraft d
            d.copy(repeat = ReminderRepeat.Every(repeat.interval.copy(startDate = date)))
        }

    private fun onDraftMonthlyCalendarChange(calendar: MonthlyCalendar) =
        updateMonthly { monthly ->
            // A day the new calendar never has (the 31st in a Hijri calendar) would never fire.
            val longest = calendarMaxMonthLength(calendars().systemFor(calendar))
            monthly.copy(calendar = calendar, day = monthly.day.coerceAtMost(longest))
        }

    private fun updateMonthly(transform: (ReminderMonthly) -> ReminderMonthly) =
        updateDraft { d ->
            val repeat = d.repeat as? ReminderRepeat.Monthly ?: return@updateDraft d
            d.copy(repeat = ReminderRepeat.Monthly(transform(repeat.monthly)))
        }

    /**
     * Fills a one-off's missing date with its next occurrence (new drafts, and older one-offs saved
     * without a date, so saving keeps when they fire), then reports when the chosen date would fire.
     */
    private suspend fun checkOnceDate(draft: ReminderEditDraft?) {
        val once = draft?.repeat as? ReminderRepeat.Once
        if (once == null) {
            _uiState.update { it.copy(onceSchedule = OnceSchedule.Unknown) }
            return
        }
        val now = Clock.System.now()
        val timesFor = timesFor() ?: return
        // Judged by what the editor shows: weekdays and skips it doesn't show don't count. A one-off
        // saved without a date keeps firing as stored until it is edited (see the scheduler).
        if (!once.picked) {
            // Not chosen by the user: keep the date on the next occurrence as prayer or offset change.
            val nextDay = draft.copy(repeat = ReminderRepeat.Once()).toReminder(DRAFT_ID)
                .nextOccurrence(now, emptyList(), calendars(), timesFor)?.prayerDay
            if (nextDay != null && nextDay != once.date) {
                // Re-runs this check with the new date.
                updateDraft { d -> if (d.repeat == once) d.copy(repeat = ReminderRepeat.Once(nextDay)) else d }
                return
            }
        }
        val fireAt = draft.toReminder(DRAFT_ID).nextTrigger(now, emptyList(), calendars(), timesFor)
        _uiState.update { it.copy(onceSchedule = fireAt?.let { at -> OnceSchedule.At(at.toEpochMilliseconds()) } ?: OnceSchedule.Passed) }
    }

    /**
     * Marks dated one-offs whose time has gone by as expired. Rather than polling, it sleeps until the soonest
     * pending one is due and checks again; any reminder or settings change restarts it.
     */
    private suspend fun trackExpiredOnceReminders(reminders: List<Reminder>) {
        val dated = reminders.filter { it.once == true && it.onceDate != null }
        while (true) {
            val timesFor = timesFor() ?: return
            val calendars = settingsRepository.data.first().reminderCalendars(calculationSettingsRepository.data.first().calculationAdjustments.hijriDate)
            val now = Clock.System.now()
            val next = dated.associate { it.id to it.nextTrigger(now, emptyList(), calendars, timesFor) }
            _uiState.update { it.copy(expiredReminderIds = next.filterValues { at -> at == null }.keys) }
            val soonest = next.values.filterNotNull().minOrNull() ?: return
            delay(soonest - now + 1.seconds)
        }
    }

    /** Prayer times of the day containing an instant, for the selected location; null when unset. */
    private suspend fun timesFor(): ((Instant) -> ShariaTimes?)? {
        val settings = settingsRepository.data.first()
        val calc = calculationSettingsRepository.data.first()
        val parameters = calc.parameters ?: return null
        val location = favoriteLocationsRepository.data.first().firstOrNull { it.id == calc.locationId }?.locationDetail ?: return null
        return { instant ->
            getShariaTimesUseCase(
                instant = instant,
                calculationParameters = parameters,
                calculationAdjustments = calc.calculationAdjustments,
                arabicCalendar = settings.selectedArabicCalendar,
                midnightMethod = calc.midnightMethod,
                locationDetail = location,
            ).timesOrNull
        }
    }

    private fun calendars() = _uiState.value.let { it.settings.reminderCalendars(it.hijriDateAdjustment) }

    private fun today(): LocalDate = Clock.System.now().toLocalDate()

    private fun toggleSelection(id: String) {
        _uiState.update {
            val next = if (id in it.selectedIds) it.selectedIds - id else it.selectedIds + id
            it.copy(selectedIds = next, selectionMode = next.isNotEmpty())
        }
    }

    private fun updateDraft(transform: (ReminderEditDraft) -> ReminderEditDraft) {
        _uiState.update { s -> s.copy(editDraft = s.editDraft?.let(transform)) }
    }

    private fun saveDraft() {
        val draft = _uiState.value.editDraft ?: return
        val id = draft.id ?: UUID.randomUUID().toString()
        val reminder = draft.toReminder(id)
        val previous = _uiState.value.reminders.firstOrNull { it.id == id }
        viewModelScope.launch {
            // Skips were for the old schedule; a changed rule must not inherit them (a dated one-off on
            // a skipped day would silently never fire).
            if (previous != null && previous.schedule() != reminder.schedule()) {
                settingsRepository.update { s ->
                    s.copy(skippedOccurrences = s.skippedOccurrences.filterNot { it is SkippedAlarm.Reminder && it.reminderId == id })
                }
            }
            reminderRepository.update { list ->
                val existing = list.indexOfFirst { it.id == id }
                if (existing >= 0) list.mapIndexed { i, r -> if (i == existing) reminder else r } else list + reminder
            }
        }
        _uiState.update { it.copy(editDraft = null) }
    }

    override fun onCleared() {
        audioPreviewPlayer.release()
    }

    private fun AudioEntry.ExternalAudioEntry.toReminderAudioEntry(): ReminderAudioEntry? =
        filepath?.let {
            ReminderAudioEntry.ExternalReminderAudioEntry(id = id, filepath = it, label = label)
        }

    private fun DeviceRingtone.toReminderAudioEntry(): ReminderAudioEntry =
        ReminderAudioEntry.ExternalReminderAudioEntry(
            id = id,
            filepath = uri,
            label = label,
            canDelete = false,
            loop = loop,
        )

    private fun onConfirmDelete() {
        val state = _uiState.value
        val idsToDelete = when {
            state.deletingReminderId != null -> setOf(state.deletingReminderId)
            state.deletingBulk -> state.selectedIds
            else -> emptySet()
        }
        if (idsToDelete.isNotEmpty()) {
            viewModelScope.launch {
                reminderRepository.update { list -> list.filterNot { it.id in idsToDelete } }
            }
        }
        _uiState.update { it.copy(deletingReminderId = null, deletingBulk = false, selectedIds = emptySet(), selectionMode = false) }
    }
}
