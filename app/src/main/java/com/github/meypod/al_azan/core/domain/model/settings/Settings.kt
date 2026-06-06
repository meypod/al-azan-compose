package com.github.meypod.al_azan.core.domain.model.settings

import android.content.res.Resources
import androidx.annotation.RawRes
import androidx.annotation.StringRes
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.github.meypod.al_azan.R
import com.github.meypod.al_azan.core.domain.model.adhan.AdhanKey
import com.github.meypod.al_azan.core.domain.model.adhan.Prayer
import com.github.meypod.al_azan.core.util.serialization.EmptyStringAsNullSerializer
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.jsonObject

private enum class DefaultAdhanEntryId(
    val key: String,
) {
    MasjidAnNabawi("masjid_an_nabawi"),
    AbdulBasitAbduSamad("abdul_basit_abdus_samad"),
    RaghebMustafaGhalwash("ragheb_mustafa_ghalwash"),
    MoazenZade("moazen_zade"),
}

fun mapAdhanIdToEntry(key: String): AudioEntry.ResourceAudioEntry =
    when (key) {
        DefaultAdhanEntryId.MasjidAnNabawi.key -> AudioEntry.ResourceAudioEntry(
            DefaultAdhanEntryId.MasjidAnNabawi.key,
            R.raw.masjid_an_nabawi,
            R.string.masjid_an_nabawi,
        )

        DefaultAdhanEntryId.AbdulBasitAbduSamad.key -> AudioEntry.ResourceAudioEntry(
            DefaultAdhanEntryId.AbdulBasitAbduSamad.key,
            R.raw.abdul_basit_abdus_samad,
            R.string.abdul_basit_abdus_samad,
        )

        DefaultAdhanEntryId.RaghebMustafaGhalwash.key -> AudioEntry.ResourceAudioEntry(
            DefaultAdhanEntryId.RaghebMustafaGhalwash.key,
            R.raw.ragheb_mustafa_ghalwash,
            R.string.ragheb_mustafa_ghalwash,
        )

        DefaultAdhanEntryId.MoazenZade.key -> AudioEntry.ResourceAudioEntry(
            DefaultAdhanEntryId.MoazenZade.key,
            R.raw.moazen_zade,
            R.string.moazen_zade,
        )

        else -> mapAdhanIdToEntry(DefaultAdhanEntryId.MasjidAnNabawi.key)
    }

fun getDefaultAdhanEntries(): List<AudioEntry.ResourceAudioEntry> =
    listOf(
        mapAdhanIdToEntry(DefaultAdhanEntryId.MasjidAnNabawi.key),
        mapAdhanIdToEntry(DefaultAdhanEntryId.AbdulBasitAbduSamad.key),
        mapAdhanIdToEntry(DefaultAdhanEntryId.RaghebMustafaGhalwash.key),
        mapAdhanIdToEntry(DefaultAdhanEntryId.MoazenZade.key),
    )

@Serializable
data class Settings(
    val deliveredAlarmTimestamps: Map<String, Long?> = emptyMap(),
    val themeColor: ThemeColor = ThemeColor.Default,
    val is24HourFormat: Boolean = true,
    val numberingSystem: NumberingSystem = NumberingSystem.Default,
    val highlightCurrentPrayer: Boolean = false,
    val selectedLocale: String,
    val selectedArabicCalendar: String = "islamic",
    @Serializable(with = EmptyStringAsNullSerializer::class)
    val selectedLocaleForArabicCalendar: String? = null,
    val selectedSecondaryCalendar: SecondaryCalendar = SecondaryCalendar.Gregorian,
    val appInitialConfigDone: Boolean = false,
    val appIntroDone: Boolean = false,
    val savedAdhanAudioEntries: List<AudioEntry> = getDefaultAdhanEntries(),
    val savedUserAudioEntries: List<AudioEntry.ExternalAudioEntry> = emptyList(),
    val selectedAdhanEntries: Map<AdhanKey, AudioEntry> = mapOf(
        AdhanKey.Default to getDefaultAdhanEntries()[0],
    ),
    val lastAppFocusTimestamp: Int? = null,
    val hiddenPrayers: List<Prayer> = emptyList(),
    val adhanVolume: Int? = null,
    val volumeButtonStopsAdhan: Boolean = false,
    val preferExternalAudioDevice: Boolean = false,
    val bypassDnd: Boolean = false,
    val hiddenWidgetPrayers: List<Prayer> = listOf(Prayer.Sunset, Prayer.Midnight, Prayer.Tahajjud),
    val showWidget: Boolean = false,
    val showWidgetCountdown: Boolean = false,
    val adaptiveWidgets: Boolean = false,
    val widgetCityNamePos: WidgetCityNamePos = WidgetCityNamePos.None,
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
    @Serializable(with = EmptyStringAsNullSerializer::class)
    val locationIdBeforeTravel: String? = null,
    val travelModeLastUpdateMillis: Long? = null,
    val showHomeNextPrayerCountdown: Boolean = true,
    val countdownSkipNonPrayers: Boolean = false,
)

@Serializable
enum class NumberingSystem(
    val value: String,
) {
    @SerialName("")
    Default(""),

    @SerialName("latn")
    Latn("latn"),

    @SerialName("arab")
    Arab("arab"),

    @SerialName("arabext")
    Arabext("arabext"),
}

@Serializable
enum class SecondaryCalendar(
    val value: String,
) {
    @SerialName("gregorian")
    Gregorian("gregorian"),

    @SerialName("persian")
    Persian("persian"),

    @SerialName("ethiopic")
    Ethiopic("ethiopic"),

    @SerialName("buddhist")
    Buddhist("buddhist"),
}

@Serializable
enum class WidgetCityNamePos {
    @SerialName("")
    None,

    @SerialName("top_start")
    TopStart,

    @SerialName("top_end")
    TopEnd,
}

fun WidgetCityNamePos.i18n(resources: Resources): String =
    when (this) {
        WidgetCityNamePos.None -> resources.getString(R.string.calendar_none)
        WidgetCityNamePos.TopStart -> resources.getString(R.string.lunar_calendar)
        WidgetCityNamePos.TopEnd -> resources.getString(R.string.secondary_calendar)
    }

@Serializable
enum class ThemeColor {
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

    @Composable
    fun isDark(): Boolean =
        when (this) {
            ClassicDark, Dark -> true
            Dynamic -> isSystemInDarkTheme()
            else -> false
        }
}

@Serializable(with = AudioEntrySerializer::class)
sealed interface AudioEntry {
    val id: String

    @Serializable
    data class ResourceAudioEntry(
        override val id: String,
        @param:RawRes val resId: Int? = null,
        @param:StringRes val labelResId: Int = R.string.unknown,
    ) : AudioEntry {
        val canDelete: Boolean = false
    }

    @Serializable
    data class ExternalAudioEntry(
        override val id: String,
        @Serializable(with = EmptyStringAsNullSerializer::class) val filepath: String? = null,
        val label: String = "",
    ) : AudioEntry {
        val canDelete: Boolean = true
    }

    @Composable
    fun getLabel(): String =
        when (this) {
            is ResourceAudioEntry -> stringResource(this.labelResId)
            is ExternalAudioEntry -> this.label
        }
}

object AudioEntrySerializer :
    JsonContentPolymorphicSerializer<AudioEntry>(AudioEntry::class) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<AudioEntry> {
        val fp = element.jsonObject.getOrDefault("resId", JsonNull)
        if (fp == JsonNull) {
            AudioEntry.ExternalAudioEntry.serializer()
        } else {
            AudioEntry.ResourceAudioEntry.serializer()
        }
        return AudioEntry.ResourceAudioEntry.serializer()
    }
}
