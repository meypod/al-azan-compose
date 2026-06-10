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
import com.github.meypod.al_azan.core.domain.model.alarm.SkippedAlarm
import com.github.meypod.al_azan.core.util.serialization.EmptyStringAsNullSerializer
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
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
    mapAdhanIdToEntryOrNull(key) ?: mapAdhanIdToEntry(DefaultAdhanEntryId.MasjidAnNabawi.key)

/**
 * Resolves a bundled-sound id to its current resource entry, or `null` for an unknown id. Callers that
 * migrate stored data use this to re-resolve the **current** resource int from the stable string id —
 * the raw resource int is not stable across builds, so a persisted int cannot be trusted.
 */
fun mapAdhanIdToEntryOrNull(key: String): AudioEntry.ResourceAudioEntry? =
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

        else -> null
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
    val dontAskPermissionFullScreenIntent: Boolean = false,
    val dontAskPermissionDndAccess: Boolean = false,
    val dontAskPermissionBatteryOptimization: Boolean = false,
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
    /**
     * Occurrences the user skipped from the Scheduled-alarms screen. Schedulers arm strictly after each
     * matching entry's fire time, so the *following* occurrence fires instead, and prune their own past
     * entries on every run. See [com.github.meypod.al_azan.core.domain.model.alarm.SkippedAlarm].
     */
    val skippedAlarms: List<SkippedAlarm> = emptyList(),
    /** Epoch millis until which alarms (adhan and reminders) are suppressed ("Dismiss & silent"). Null = not silenced. */
    val silencedUntilMillis: Long? = null,
    /**
     * Interruption filter captured before the "Dismiss & silent" DND window, restored when it ends.
     * Null = nothing to restore.
     */
    val dndRestoreFilter: Int? = null,
    /**
     * Always launch the full-screen alarm activity when an alarm sounds, instead of relying on the
     * notification's full-screen-intent. Some users prefer it, and some OEMs don't honor the full-screen
     * intent over the lock screen. Only takes effect when the full-screen alarm is enabled.
     */
    val forceLaunchAlarmActivity: Boolean = false,
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
        WidgetCityNamePos.None -> resources.getString(R.string.none)
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
    val loop: Boolean

    @Serializable
    data class ResourceAudioEntry(
        override val id: String,
        @param:RawRes val resId: Int? = null,
        @param:StringRes val labelResId: Int = R.string.unknown,
    ) : AudioEntry {
        override val loop: Boolean = false
        val canDelete: Boolean = false
    }

    @Serializable
    data class ExternalAudioEntry(
        override val id: String,
        @Serializable(with = EmptyStringAsNullSerializer::class) val filepath: String? = null,
        val label: String = "",
        override val loop: Boolean = false,
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

/**
 * Whether this entry can actually resolve to a playable sound. A [AudioEntry.ResourceAudioEntry] with
 * a null [AudioEntry.ResourceAudioEntry.resId], or an [AudioEntry.ExternalAudioEntry] with a null path,
 * is a broken/orphaned selection (a deleted sound, or data corrupted by an older build) — callers
 * should fall back to the global default rather than show "unknown" or play nothing.
 */
fun AudioEntry.isResolvable(): Boolean =
    when (this) {
        is AudioEntry.ResourceAudioEntry -> resId != null
        is AudioEntry.ExternalAudioEntry -> !filepath.isNullOrEmpty()
    }

object AudioEntrySerializer :
    JsonContentPolymorphicSerializer<AudioEntry>(AudioEntry::class) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<AudioEntry> {
        // Bundled sounds persist a non-null `resId`; external (device/user) sounds never do. The
        // discriminator must pick the matching deserializer — using ResourceAudioEntry for everything
        // strips the filepath/label off external entries and leaves them resolving to "unknown".
        val hasResId = element.jsonObject["resId"]?.takeIf { it != JsonNull } != null
        return if (hasResId) {
            ReResolvingResourceAudioEntrySerializer
        } else {
            AudioEntry.ExternalAudioEntry.serializer()
        }
    }
}

/**
 * Persisted [AudioEntry.ResourceAudioEntry.resId]/[AudioEntry.ResourceAudioEntry.labelResId] are raw
 * `R` ints, which are **not stable across builds** — a value saved by an older build can point to a
 * different resource or to none at all, crashing with `Resources$NotFoundException` when the label is
 * resolved. On read, re-resolve the entry from its stable string [AudioEntry.id]; an id no longer
 * bundled degrades to an unresolvable entry ("unknown" label, null sound) instead of crashing.
 */
private object ReResolvingResourceAudioEntrySerializer : KSerializer<AudioEntry.ResourceAudioEntry> {
    private val delegate = AudioEntry.ResourceAudioEntry.serializer()
    override val descriptor = delegate.descriptor

    override fun serialize(
        encoder: Encoder,
        value: AudioEntry.ResourceAudioEntry,
    ) = delegate.serialize(encoder, value)

    override fun deserialize(decoder: Decoder): AudioEntry.ResourceAudioEntry {
        val decoded = delegate.deserialize(decoder)
        return mapAdhanIdToEntryOrNull(decoded.id)
            ?: decoded.copy(resId = null, labelResId = R.string.unknown)
    }
}
