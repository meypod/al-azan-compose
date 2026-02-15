package com.github.meypod.al_azan.core.domain.model.settings

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
data class Settings(
    val deliveredAlarmTimestamps: Map<String, Long?> = emptyMap(),
    val themeColor: ThemeColors = ThemeColors.Default,
    val is24HourFormat: Boolean = true,
    @Serializable(with = EmptyStringAsNullSerializer::class)
    val numberingSystem: String? = null,
    val highlightCurrentPrayer: Boolean = false,
    val selectedLocale: String,
    @Serializable(with = EmptyStringAsNullSerializer::class)
    val selectedArabicCalendar: String? = null,
    @Serializable(with = EmptyStringAsNullSerializer::class)
    val selectedLocaleForArabicCalendar: String? = null,
    val selectedSecondaryCalendar: String = "gregory",
    val appInitialConfigDone: Boolean = false,
    val appIntroDone: Boolean = false,
    val savedAdhanAudioEntries: List<AdhanAudioEntry> = emptyList(),
    val savedUserAudioEntries: List<AdhanAudioEntry> = emptyList(),
    val selectedAdhanEntries: Map<AdhanKey, AdhanAudioEntry> = emptyMap(),
    val lastAppFocusTimestamp: Int? = null,
    val hiddenPrayers: List<Prayer> = emptyList(),
    val adhanVolume: Int? = null,
    val volumeButtonStopsAdhan: Boolean = false,
    val preferExternalAudioDevice: Boolean = false,
    val bypassDnd: Boolean = false,
    val hiddenWidgetPrayers: List<Prayer> = emptyList(),
    val showWidget: Boolean = false,
    val showWidgetCountdown: Boolean = false,
    val adaptiveWidgets: Boolean = false,
    val widgetCityNamePos: WidgetCityNamePos? = null,
    @Serializable(with = EmptyStringAsNullSerializer::class)
    val calcSettingsHash: String? = null,
    @Serializable(with = EmptyStringAsNullSerializer::class)
    val alarmSettingsHash: String? = null,
    @Serializable(with = EmptyStringAsNullSerializer::class)
    val reminderSettingsHash: String? = null,
    val isPlayingAudio: Boolean = false,
    val dontAskPermissionNotifications: Boolean = false,
    val dontAskPermissionAlarm: Boolean = false,
    val dontAskPermissionPhoneState: Boolean = false,
    val devMode: Boolean = false,
    val qiblaFinderUnderstood: Boolean = false,
    val qiblaFinderOrientationLocked: Boolean = true,
    val counterHistoryVisible: Boolean = false,
    val advancedCustomAdhan: Boolean = false,
    @Serializable(with = EmptyStringAsNullSerializer::class)
    val ramadanRemindedYear: String? = null,
    val ramadanReminderDontShow: Boolean = false,
    val useDifferentAlarmType: Boolean = false,
    val hijriMonthlyView: Boolean = false,
)

@Serializable
enum class ThemeColors {
    @SerialName("default")
    Default,

    @SerialName("light")
    Light,

    @SerialName("dark")
    Dark,

    @SerialName("classic_light")
    ClassicLight,

    @SerialName("classic_dark")
    ClassicDark,

    @SerialName("dynamic")
    Dynamic,

    ;

    fun isClassic(): Boolean =
        when (this) {
            ClassicLight, ClassicDark -> true
            else -> false
        }
}

@Serializable(with = AdhanAudioEntrySerializer::class)
sealed interface AdhanAudioEntry {
    @Serializable
    data class ResourceAdhanAudioEntry(
        val id: String,
        val filepath: Int? = null,
        val label: String = "",
        @Serializable(with = EmptyStringAsNullSerializer::class) val remoteUri: String? = null,
        val canDelete: Boolean = false,
        val internal: Boolean = false,
    ) : AdhanAudioEntry

    @Serializable
    data class ExternalAdhanAudioEntry(
        val id: String,
        @Serializable(with = EmptyStringAsNullSerializer::class) val filepath: String? = null,
        val label: String = "",
        @Serializable(with = EmptyStringAsNullSerializer::class) val remoteUri: String? = null,
        val canDelete: Boolean = false,
        val internal: Boolean = false,
    ) : AdhanAudioEntry
}

object AdhanAudioEntrySerializer :
    JsonContentPolymorphicSerializer<AdhanAudioEntry>(AdhanAudioEntry::class) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<AdhanAudioEntry> {
        val fp = element.jsonObject["filepath"]
        if (fp != null) {
            val prim = fp.jsonPrimitive
            return if (prim.isString) {
                AdhanAudioEntry.ExternalAdhanAudioEntry.serializer()
            } else {
                AdhanAudioEntry.ResourceAdhanAudioEntry.serializer()
            }
        }
        return AdhanAudioEntry.ResourceAdhanAudioEntry.serializer()
    }
}
