package com.github.meypod.al_azan.core.domain.model.reminder

import com.github.meypod.al_azan.core.domain.model.adhan.Prayer
import com.github.meypod.al_azan.core.domain.model.alarm.PrayerAlarmSettings
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement

@Serializable
data class Reminder(
    val id: String,
    val label: String = "",
    val enabled: Boolean = false,
    val prayer: Prayer,
    /** offset from the prayer time, in minutes */
    val duration: Int,
    val durationModifier: Int,
    val sound: ReminderAudioEntry? = null,
    val once: Boolean? = null,
    val days: PrayerAlarmSettings? = null,
)

@Serializable(with = ReminderAudioEntrySerializer::class)
sealed interface ReminderAudioEntry {
    @Serializable
    data class ResourceReminderAudioEntry(
        val id: String,
        val resourceId: Int,
        val label: String,
        val canDelete: Boolean = false,
        val loop: Boolean = false,
        val notif: Boolean = false,
    ) : ReminderAudioEntry

    @Serializable
    data class ExternalReminderAudioEntry(
        val id: String,
        val filepath: String,
        val label: String,
        val canDelete: Boolean = false,
        val loop: Boolean = false,
        val notif: Boolean = false,
    ) : ReminderAudioEntry

    @Serializable
    object DefaultReminderAudioEntry : ReminderAudioEntry {
        val id = "default"
        val canDelete = false
        val notif = true
        val loop = false
    }
}

internal object ReminderAudioEntrySerializer : KSerializer<ReminderAudioEntry> {
    override val descriptor: SerialDescriptor =
        ReminderAudioEntry.ResourceReminderAudioEntry.serializer().descriptor

    override fun deserialize(decoder: Decoder): ReminderAudioEntry {
        val jsonDecoder =
            decoder as? JsonDecoder
                ?: throw SerializationException("ReminderAudioEntrySerializer can be used only with JSON")

        val element: JsonElement = jsonDecoder.decodeJsonElement()

        try {
            return jsonDecoder.json.decodeFromJsonElement(
                ReminderAudioEntry.ResourceReminderAudioEntry.serializer(),
                element,
            )
        } catch (_: Exception) {
        }

        try {
            return jsonDecoder.json.decodeFromJsonElement(
                ReminderAudioEntry.ExternalReminderAudioEntry.serializer(),
                element,
            )
        } catch (e: Exception) {
            throw SerializationException("Cannot deserialize ReminderAudioEntry: ${e.message}")
        }
    }

    override fun serialize(
        encoder: Encoder,
        value: ReminderAudioEntry,
    ) {
        when (value) {
            is ReminderAudioEntry.ResourceReminderAudioEntry -> {
                encoder.encodeSerializableValue(
                    ReminderAudioEntry.ResourceReminderAudioEntry.serializer(),
                    value,
                )
            }

            is ReminderAudioEntry.ExternalReminderAudioEntry -> {
                encoder.encodeSerializableValue(
                    ReminderAudioEntry.ExternalReminderAudioEntry.serializer(),
                    value,
                )
            }

            is ReminderAudioEntry.DefaultReminderAudioEntry -> {
                encoder.encodeSerializableValue(
                    ReminderAudioEntry.DefaultReminderAudioEntry.serializer(),
                    value,
                )
            }
        }
    }
}
