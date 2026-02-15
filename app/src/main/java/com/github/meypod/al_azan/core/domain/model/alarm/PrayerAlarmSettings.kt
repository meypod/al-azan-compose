package com.github.meypod.al_azan.core.domain.model.alarm

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
import kotlinx.serialization.json.jsonPrimitive

@Serializable(with = PrayerAlarmSettingsSerializer::class)
sealed interface PrayerAlarmSettings {
    data class Bool(
        val value: Boolean,
    ) : PrayerAlarmSettings

    data class ByWeekDay(
        /** map of weekDayIndex -> enabled; missing keys = disabled */
        val days: Map<Int, Boolean> = emptyMap(),
    ) : PrayerAlarmSettings {
        fun get(weekDayIndex: Int) = days.getOrDefault(weekDayIndex, false)
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
                } else {
                    throw SerializationException("Unsupported primitive for PrayerAlarmSettings: $elem")
                }
            }

            is JsonObject -> {
                val map = elem.mapValues { (_, v) -> v.jsonPrimitive.booleanOrNull ?: false }
                    .mapKeys { it.key.toInt() }
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
                        value.days.mapKeys { it.key.toString() }.mapValues { JsonPrimitive(it.value) },
                    )
            }

        jsonEncoder.encodeSerializableValue(elementSerializer, outElem)
    }
}
