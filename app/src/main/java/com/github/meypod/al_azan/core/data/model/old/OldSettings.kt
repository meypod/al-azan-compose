package com.github.meypod.al_azan.core.data.model.old

import com.github.meypod.al_azan.core.domain.model.adhan.AdhanKey
import com.github.meypod.al_azan.core.domain.model.adhan.Prayer
import com.github.meypod.al_azan.core.presentation.model.WidgetCityNamePos
import com.github.meypod.al_azan.core.util.serialization.EmptyStringAsNullSerializer
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@Serializable
data class OldSettings(
    val state: OldSettingsState,
    val version: Int,
)

@Serializable
data class OldSettingsState(
    @SerialName("DELIVERED_ALARM_TIMESTAMPS")
    val deliveredAlarmTimestamps: Map<String, Long?> = emptyMap(),
    //
    @SerialName("THEME_COLOR") val themeColor: OldThemeColors = OldThemeColors.Default,
    //
    @SerialName("IS_24_HOUR_FORMAT") val is24HourFormat: Boolean = true,
    @SerialName("NUMBERING_SYSTEM")
    @Serializable(with = EmptyStringAsNullSerializer::class)
    val numberingSystem: String? = null,
    @SerialName("HIGHLIGHT_CURRENT_PRAYER") val highlightCurrentPrayer: Boolean = false,
    //
    @SerialName("SELECTED_LOCALE") val selectedLocale: String,
    @SerialName("SELECTED_ARABIC_CALENDAR")
    @Serializable(with = EmptyStringAsNullSerializer::class)
    val selectedArabicCalendar: String? = null,
    @SerialName("SELECTED_LOCALE_FOR_ARABIC_CALENDAR")
    @Serializable(with = EmptyStringAsNullSerializer::class)
    val selectedLocaleForArabicCalendar: String? = null,
    @SerialName("SELECTED_SECONDARY_CALENDAR") val selectedSecondaryCalendar: String = "gregory",
    @SerialName("APP_INITIAL_CONFIG_DONE") val appInitialConfigDone: Boolean = false,
    @SerialName("APP_INTRO_DONE") val appIntroDone: Boolean = false,
    @SerialName("SAVED_ADHAN_AUDIO_ENTRIES")
    val savedAdhanAudioEntries: List<OldAdhanAudioEntry> = emptyList(),
    @SerialName("SAVED_USER_AUDIO_ENTRIES")
    val savedUserAudioEntries: List<OldAdhanAudioEntry> = emptyList(),
    @SerialName("SELECTED_ADHAN_ENTRIES")
    val selectedAdhanEntries: Map<AdhanKey, OldAdhanAudioEntry> = emptyMap(),
    //
    @SerialName("LAST_APP_FOCUS_TIMESTAMP") val lastAppFocusTimestamp: Int? = null,
    @SerialName("HIDDEN_PRAYERS") val hiddenPrayers: List<Prayer> = emptyList(),
    @SerialName("ADHAN_VOLUME") val adhanVolume: Int? = null,
    //
    @SerialName("VOLUME_BUTTON_STOPS_ADHAN") val volumeButtonStopsAdhan: Boolean = false,
    @SerialName("PREFER_EXTERNAL_AUDIO_DEVICE") val preferExternalAudioDevice: Boolean = false,
    @SerialName("BYPASS_DND") val bypassDnd: Boolean = false,
    //
    @SerialName("HIDDEN_WIDGET_PRAYERS") val hiddenWidgetPrayers: List<Prayer> = emptyList(),
    @SerialName("SHOW_WIDGET") val showWidget: Boolean = false,
    @SerialName("SHOW_WIDGET_COUNTDOWN") val showWidgetCountdown: Boolean = false,
    @SerialName("ADAPTIVE_WIDGETS") val adaptiveWidgets: Boolean = false,
    @SerialName("WIDGET_CITY_NAME_POS") val widgetCityNamePos: WidgetCityNamePos? = null,
    //
    @SerialName("CALC_SETTINGS_HASH")
    @Serializable(with = EmptyStringAsNullSerializer::class)
    val calcSettingsHash: String? = null,
    @SerialName("ALARM_SETTINGS_HASH")
    @Serializable(with = EmptyStringAsNullSerializer::class)
    val alarmSettingsHash: String? = null,
    @SerialName("REMINDER_SETTINGS_HASH")
    @Serializable(with = EmptyStringAsNullSerializer::class)
    val reminderSettingsHash: String? = null,
    @SerialName("IS_PLAYING_AUDIO") val isPlayingAudio: Boolean = false,
    //
    @SerialName("DONT_ASK_PERMISSION_NOTIFICATIONS")
    val dontAskPermissionNotifications: Boolean = false,
    @SerialName("DONT_ASK_PERMISSION_ALARM") val dontAskPermissionAlarm: Boolean = false,
    @SerialName("DONT_ASK_PERMISSION_PHONE_STATE") val dontAskPermissionPhoneState: Boolean = false,
    //
    @SerialName("DEV_MODE") val devMode: Boolean = false,
    //
    @SerialName("QIBLA_FINDER_UNDERSTOOD") val qiblaFinderUnderstood: Boolean = false,
    @SerialName("QIBLA_FINDER_ORIENTATION_LOCKED") val qiblaFinderOrientationLocked: Boolean = true,
    @SerialName("COUNTER_HISTORY_VISIBLE") val counterHistoryVisible: Boolean = false,
    @SerialName("ADVANCED_CUSTOM_ADHAN") val advancedCustomAdhan: Boolean = false,
    //
    @SerialName("RAMADAN_REMINDED_YEAR")
    @Serializable(with = EmptyStringAsNullSerializer::class)
    val ramadanRemindedYear: String? = null,
    @SerialName("RAMADAN_REMINDER_DONT_SHOW") val ramadanReminderDontShow: Boolean = false,
    //
    @SerialName("USE_DIFFERENT_ALARM_TYPE") val useDifferentAlarmType: Boolean = false,
    @SerialName("HIJRI_MONTHLY_VIEW") val hijriMonthlyView: Boolean = false,
)

@Serializable
enum class OldThemeColors {
  @SerialName("default") Default,
  @SerialName("light") Light,
  @SerialName("dark") Dark,
}

@Serializable(with = OldAdhanAudioEntrySerializer::class)
sealed interface OldAdhanAudioEntry {
  @Serializable
  data class OldResourceAdhanAudioEntry(
      val id: String,
      val filepath: Int? = null,
      val label: String = "",
      @Serializable(with = EmptyStringAsNullSerializer::class) val remoteUri: String? = null,
      val canDelete: Boolean = false,
      val internal: Boolean = false,
  ) : OldAdhanAudioEntry

  @Serializable
  data class OldExternalAdhanAudioEntry(
      val id: String,
      @Serializable(with = EmptyStringAsNullSerializer::class) val filepath: String? = null,
      val label: String = "",
      @Serializable(with = EmptyStringAsNullSerializer::class) val remoteUri: String? = null,
      val canDelete: Boolean = false,
      val internal: Boolean = false,
  ) : OldAdhanAudioEntry
}

object OldAdhanAudioEntrySerializer :
    JsonContentPolymorphicSerializer<OldAdhanAudioEntry>(OldAdhanAudioEntry::class) {
  override fun selectDeserializer(
      element: JsonElement
  ): DeserializationStrategy<out OldAdhanAudioEntry> {
    val fp = element.jsonObject["filepath"]
    if (fp != null) {
      val prim = fp.jsonPrimitive
      return if (prim.isString) OldAdhanAudioEntry.OldExternalAdhanAudioEntry.serializer()
      else OldAdhanAudioEntry.OldResourceAdhanAudioEntry.serializer()
    }
    return OldAdhanAudioEntry.OldResourceAdhanAudioEntry.serializer()
  }
}
