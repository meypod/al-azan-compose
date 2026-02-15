package com.github.meypod.al_azan.core.data.model.old

import com.github.meypod.al_azan.core.domain.model.alarm.AlarmSettings
import com.github.meypod.al_azan.core.domain.model.alarm.PrayerAlarmSettings
import com.github.meypod.al_azan.core.domain.model.alarm.VibrationMode
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonPrimitive

@Serializable
data class OldAlarmSettings(
  val state: OldAlarmSettingsState,
  val version: Int,
)

@Serializable
data class OldAlarmSettingsState(
  @SerialName("SHOW_NEXT_PRAYER_TIME") val showNextPrayerTime: Boolean = false,
  @SerialName("DONT_NOTIFY_UPCOMING") val dontNotifyUpcoming: Boolean = false,
  @SerialName("PRE_ALARM_MINUTES_BEFORE") val preAlarmMinutesBefore: Int = 60,
  @SerialName("DONT_TURN_ON_SCREEN") val dontTurnOnScreen: Boolean = false,
  @SerialName("VIBRATION_MODE") val vibrationMode: OldVibrationMode = OldVibrationMode.Once,
  //
  @SerialName("FAJR_SOUND") val fajrSound: OldPrayerAlarmSettings? = null,
  @SerialName("FAJR_NOTIFY") val fajrNotify: OldPrayerAlarmSettings? = null,
  @SerialName("SUNRISE_SOUND") val sunriseSound: OldPrayerAlarmSettings? = null,
  @SerialName("SUNRISE_NOTIFY") val sunriseNotify: OldPrayerAlarmSettings? = null,
  @SerialName("DHUHR_SOUND") val dhuhrSound: OldPrayerAlarmSettings? = null,
  @SerialName("DHUHR_NOTIFY") val dhuhrNotify: OldPrayerAlarmSettings? = null,
  @SerialName("ASR_SOUND") val asrSound: OldPrayerAlarmSettings? = null,
  @SerialName("ASR_NOTIFY") val asrNotify: OldPrayerAlarmSettings? = null,
  @SerialName("MAGHRIB_SOUND") val maghribSound: OldPrayerAlarmSettings? = null,
  @SerialName("MAGHRIB_NOTIFY") val maghribNotify: OldPrayerAlarmSettings? = null,
  @SerialName("ISHA_SOUND") val ishaSound: OldPrayerAlarmSettings? = null,
  @SerialName("ISHA_NOTIFY") val ishaNotify: OldPrayerAlarmSettings? = null,
  @SerialName("MIDNIGHT_SOUND") val midnightSound: OldPrayerAlarmSettings? = null,
  @SerialName("MIDNIGHT_NOTIFY") val midnightNotify: OldPrayerAlarmSettings? = null,
  @SerialName("TAHAJJUD_SOUND") val tahajjudSound: OldPrayerAlarmSettings? = null,
  @SerialName("TAHAJJUD_NOTIFY") val tahajjudNotify: OldPrayerAlarmSettings? = null,
  @SerialName("SUNSET_SOUND") val sunsetSound: OldPrayerAlarmSettings? = null,
  @SerialName("SUNSET_NOTIFY") val sunsetNotify: OldPrayerAlarmSettings? = null,
) {
  fun toAlarmSettings() =
    AlarmSettings(
      showNextPrayerTime = this.showNextPrayerTime,
      dontNotifyUpcoming = this.dontNotifyUpcoming,
      preAlarmMinutesBefore = this.preAlarmMinutesBefore,
      dontTurnOnScreen = this.dontTurnOnScreen,
      vibrationMode = this.vibrationMode.toVibrationMode(),
      //
      fajrSound = this.fajrSound.toPrayerAlarmSettings(),
      fajrNotify = this.fajrNotify.toPrayerAlarmSettings(),
      sunriseSound = this.sunriseSound.toPrayerAlarmSettings(),
      sunriseNotify = this.sunriseNotify.toPrayerAlarmSettings(),
      dhuhrSound = this.dhuhrSound.toPrayerAlarmSettings(),
      dhuhrNotify = this.dhuhrNotify.toPrayerAlarmSettings(),
      asrSound = this.asrSound.toPrayerAlarmSettings(),
      asrNotify = this.asrNotify.toPrayerAlarmSettings(),
      maghribSound = this.maghribSound.toPrayerAlarmSettings(),
      maghribNotify = this.maghribNotify.toPrayerAlarmSettings(),
      ishaSound = this.ishaSound.toPrayerAlarmSettings(),
      ishaNotify = this.ishaNotify.toPrayerAlarmSettings(),
      midnightSound = this.midnightSound.toPrayerAlarmSettings(),
      midnightNotify = this.midnightNotify.toPrayerAlarmSettings(),
      tahajjudSound = this.tahajjudSound.toPrayerAlarmSettings(),
      tahajjudNotify = this.tahajjudNotify.toPrayerAlarmSettings(),
      sunsetSound = this.sunsetSound.toPrayerAlarmSettings(),
      sunsetNotify = this.sunsetNotify.toPrayerAlarmSettings(),
    )
}

@Serializable(with = OldPrayerAlarmSettingsSerializer::class)
sealed interface OldPrayerAlarmSettings {
  data class Bool(
    val value: Boolean,
  ) : OldPrayerAlarmSettings

  data class ByWeekDay(
    /** map of weekDayIndex -> enabled; missing keys = unspecified */
    val days: Map<Int, Boolean> = emptyMap(),
  ) : OldPrayerAlarmSettings
}

fun OldPrayerAlarmSettings?.toPrayerAlarmSettings() =
  when (this) {
    is OldPrayerAlarmSettings.Bool -> PrayerAlarmSettings.Bool(this.value)
    is OldPrayerAlarmSettings.ByWeekDay -> PrayerAlarmSettings.ByWeekDay(this.days)
    null -> PrayerAlarmSettings.Bool(false)
  }

object OldPrayerAlarmSettingsSerializer : KSerializer<OldPrayerAlarmSettings?> {
  private val elementSerializer = JsonElement.serializer()

  override val descriptor: SerialDescriptor = elementSerializer.descriptor

  override fun deserialize(decoder: Decoder): OldPrayerAlarmSettings? {
    val jsonDecoder =
      decoder as? JsonDecoder
        ?: throw SerializationException(
          "OldPrayerAlarmSettings can be deserialized only by JSON",
        )
    return when (val elem = jsonDecoder.decodeSerializableValue(elementSerializer)) {
      is JsonNull -> {
        OldPrayerAlarmSettings.Bool(false)
      }

      is JsonPrimitive -> {
        if (elem.isString) {
          null
        } else if (elem.booleanOrNull != null) {
          OldPrayerAlarmSettings.Bool(elem.boolean)
        } else {
          throw SerializationException("Unsupported primitive for OldPrayerAlarmSettings: $elem")
        }
      }

      is JsonObject -> {
        val map =
          elem
            .mapValues { (_, v) -> v.jsonPrimitive.booleanOrNull ?: false }
            .mapKeys { it.key.toInt() }
        OldPrayerAlarmSettings.ByWeekDay(map)
      }

      else -> {
        throw SerializationException("Unsupported JSON for OldPrayerAlarmSettings: $elem")
      }
    }
  }

  override fun serialize(
    encoder: Encoder,
    value: OldPrayerAlarmSettings?,
  ) {
    val jsonEncoder =
      encoder as? JsonEncoder
        ?: throw SerializationException("OldPrayerAlarmSettings can be serialized only by JSON")

    val outElem: JsonElement =
      when (value) {
        is OldPrayerAlarmSettings.Bool -> {
          JsonPrimitive(value.value)
        }

        is OldPrayerAlarmSettings.ByWeekDay -> {
          JsonObject(
            value.days.mapKeys { it.key.toString() }.mapValues { JsonPrimitive(it.value) },
          )
        }

        null -> {
          JsonNull
        }
      }

    jsonEncoder.encodeSerializableValue(elementSerializer, outElem)
  }
}

@Serializable
object OldVibrationModeSerializer : KSerializer<OldVibrationMode> {
  private val elementSerializer = JsonElement.serializer()

  override val descriptor: SerialDescriptor = elementSerializer.descriptor

  override fun deserialize(decoder: Decoder): OldVibrationMode {
    val jsonDecoder =
      decoder as? JsonDecoder
        ?: throw SerializationException("OldVibrationMode can be deserialized only by JSON")

    return when (val elem = jsonDecoder.decodeSerializableValue(elementSerializer)) {
      is JsonPrimitive -> {
        if (elem.isString) {
          val name = elem.content
          try {
            OldVibrationMode.valueOf(name)
          } catch (e: Exception) {
            throw SerializationException("Unknown OldVibrationMode: $name")
          }
        } else {
          val num =
            elem.content.toIntOrNull()
              ?: throw SerializationException("Unsupported primitive for OldVibrationMode: $elem")
          OldVibrationMode.entries.getOrNull(num)
            ?: throw SerializationException("Invalid ordinal for OldVibrationMode: $num")
        }
      }

      else -> {
        throw SerializationException("Unsupported JSON for OldVibrationMode: $elem")
      }
    }
  }

  override fun serialize(
    encoder: Encoder,
    value: OldVibrationMode,
  ) {
    val jsonEncoder =
      encoder as? JsonEncoder
        ?: throw SerializationException("OldVibrationMode can be serialized only by JSON")

    val outElem: JsonElement = JsonPrimitive(value.name)

    jsonEncoder.encodeSerializableValue(elementSerializer, outElem)
  }
}

@Serializable(with = OldVibrationModeSerializer::class)
enum class OldVibrationMode {
  Off,
  Once,
  Continuous,
}

fun OldVibrationMode?.toVibrationMode() =
  when (this) {
    OldVibrationMode.Off -> VibrationMode.Off
    OldVibrationMode.Once -> VibrationMode.Once
    OldVibrationMode.Continuous -> VibrationMode.Continuous
    else -> VibrationMode.Once
  }
