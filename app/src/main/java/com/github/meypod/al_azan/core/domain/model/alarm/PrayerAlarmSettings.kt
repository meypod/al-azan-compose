package com.github.meypod.al_azan.core.domain.model.alarm

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.TimeZone
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.KSerializer
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
import kotlinx.serialization.json.int
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlin.time.Instant

@Serializable(with = PrayerAlarmSettingsSerializer::class)
sealed interface PrayerAlarmSettings {
    data class Bool(
        val value: Boolean,
    ) : PrayerAlarmSettings

    data class ByWeekDay(
        /** map of weekDayIndex -> enabled; missing keys = disabled */
        val days: Map<DayOfWeek, Boolean> = emptyMap(),
    ) : PrayerAlarmSettings {
        fun get(dayOfWeek: DayOfWeek) = days.getOrDefault(dayOfWeek, false)
    }

    fun shouldFireFor(instant: Instant): Boolean =
        when (this) {
            is Bool -> this.value
            is ByWeekDay -> get(instant.toLocalDateTime(TimeZone.currentSystemDefault()).dayOfWeek)
        }

    /** Days currently enabled. Bool(true) means every day, Bool(false) means none. */
    fun selectedDays(): Set<DayOfWeek> =
        when (this) {
            is Bool -> if (value) ALL_DAYS else emptySet()
            is ByWeekDay -> days.filterValues { it }.keys
        }

    companion object {
        val ALL_DAYS: Set<DayOfWeek> = DayOfWeek.entries.toSet()

        /** Collapse a per-day selection to Bool when all/none are enabled, else ByWeekDay. */
        fun fromDays(days: Set<DayOfWeek>): PrayerAlarmSettings =
            when {
                days.isEmpty() -> Bool(false)
                days.containsAll(ALL_DAYS) -> Bool(true)
                else -> ByWeekDay(ALL_DAYS.associateWith { it in days })
            }
    }
}

object PrayerAlarmSettingsSerializer : KSerializer<PrayerAlarmSettings> {
    private val elementSerializer = JsonElement.serializer()

    override val descriptor: SerialDescriptor = elementSerializer.descriptor

    override fun deserialize(decoder: Decoder): PrayerAlarmSettings {
        val jsonDecoder =
            decoder as? JsonDecoder
                ?: throw SerializationException(
                    "PrayerAlarmSettings can be deserialized only by JSON",
                )
        return when (val elem = jsonDecoder.decodeSerializableValue(elementSerializer)) {
            is JsonNull -> PrayerAlarmSettings.Bool(false)

            is JsonPrimitive -> {
                if (elem.isString) {
                    PrayerAlarmSettings.Bool(false)
                } else if (elem.booleanOrNull != null) {
                    PrayerAlarmSettings.Bool(elem.boolean)
                } else if (elem.intOrNull != null) {
                    PrayerAlarmSettings.Bool(elem.int == 1)
                } else {
                    throw SerializationException("Unsupported primitive for PrayerAlarmSettings: $elem")
                }
            }

            is JsonObject -> {
                val map = elem.mapValues { (_, v) -> v.jsonPrimitive.booleanOrNull ?: false }
                    .mapKeys { DayOfWeek(it.key.toInt()) }
                PrayerAlarmSettings.ByWeekDay(map)
            }

            else -> throw SerializationException("Unsupported JSON for PrayerAlarmSettings: $elem")
        }
    }

    override fun serialize(
        encoder: Encoder,
        value: PrayerAlarmSettings,
    ) {
        val jsonEncoder =
            encoder as? JsonEncoder
                ?: throw SerializationException("PrayerAlarmSettings can be serialized only by JSON")

        val outElem: JsonElement =
            when (value) {
                is PrayerAlarmSettings.Bool -> JsonPrimitive(value.value)

                is PrayerAlarmSettings.ByWeekDay ->
                    JsonObject(
                        value.days.mapKeys { it.key.isoDayNumber.toString() }.mapValues { JsonPrimitive(it.value) },
                    )
            }

        jsonEncoder.encodeSerializableValue(elementSerializer, outElem)
    }
}
