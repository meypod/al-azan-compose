package com.github.meypod.al_azan.core.data.model.old

import com.github.meypod.al_azan.core.domain.model.adhan.Prayer
import com.github.meypod.al_azan.core.util.serialization.EmptyStringAsNullSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement

@Serializable
data class OldReminderSettings(
    val state: OldReminderSettingsState,
    val version: Int,
)

@Serializable
data class OldReminderSettingsState(
    @SerialName("REMINDERS") val reminders: List<OldReminder> = emptyList(),
)

@Serializable
data class OldReminder(
    val id: String,
    @Serializable(with = EmptyStringAsNullSerializer::class)  val label: String? = null,
    val enabled: Boolean = false,
    val prayer: Prayer,
    /** in milliseconds. negative to set before, positive to set after */
    val duration: Long,
    /** has a value of `-1` or `+1` */
    val durationModifier: Int,
    /** should reminder play sound and what sound ? */
    val sound: OldAudioEntry? = null,
    /** should reminder be set only once? */
    val once: Boolean? = null,
    val days: OldPrayerAlarmSettings? = null,
)


@Serializable(with = OldAudioEntrySerializer::class)
sealed interface OldAudioEntry {
  @Serializable
  data class OldResourceOldAudioEntry(
    val id: String,
    val filepath: Int,
    val label: String,
    val canDelete: Boolean = false,
    val loop: Boolean = false,
    val notif: Boolean = false,
  ) : OldAudioEntry

  @Serializable
  data class OldExternalOldAudioEntry(
    val id: String,
    val filepath: String?,
    val label: String,
    val canDelete: Boolean = false,
    val loop: Boolean = false,
    val notif: Boolean = false,
  ) : OldAudioEntry
}

internal object OldAudioEntrySerializer : KSerializer<OldAudioEntry> {
  override val descriptor: SerialDescriptor =
    OldAudioEntry.OldResourceOldAudioEntry.serializer().descriptor

  override fun deserialize(decoder: Decoder): OldAudioEntry {
    val jsonDecoder = decoder as? JsonDecoder
      ?: throw SerializationException("OldAudioEntrySerializer can be used only with JSON")

    val element: JsonElement = jsonDecoder.decodeJsonElement()

    // Try resource entry first (filepath as Int)
    try {
      return jsonDecoder.json.decodeFromJsonElement(
        OldAudioEntry.OldResourceOldAudioEntry.serializer(),
        element
      )
    } catch (_: Exception) {
    }

    // Then try external entry (filepath as String or nullable)
    try {
      return jsonDecoder.json.decodeFromJsonElement(
        OldAudioEntry.OldExternalOldAudioEntry.serializer(),
        element
      )
    } catch (e: Exception) {
      throw SerializationException("Cannot deserialize OldAudioEntry: ${e.message}")
    }
  }

  override fun serialize(encoder: Encoder, value: OldAudioEntry) {
    when (value) {
      is OldAudioEntry.OldResourceOldAudioEntry -> encoder.encodeSerializableValue(
        OldAudioEntry.OldResourceOldAudioEntry.serializer(), value
      )
      is OldAudioEntry.OldExternalOldAudioEntry -> encoder.encodeSerializableValue(
        OldAudioEntry.OldExternalOldAudioEntry.serializer(), value
      )
      else -> throw SerializationException("Unknown OldAudioEntry implementation: ${value::class}")
    }
  }
}

