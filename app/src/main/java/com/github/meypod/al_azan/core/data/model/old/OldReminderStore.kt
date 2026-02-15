package com.github.meypod.al_azan.core.data.model.old

import com.github.meypod.al_azan.core.domain.model.adhan.Prayer
import com.github.meypod.al_azan.core.domain.model.reminder.Reminder
import com.github.meypod.al_azan.core.domain.model.reminder.ReminderAudioEntry
import com.github.meypod.al_azan.core.util.serialization.EmptyStringAsNullSerializer
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement

@Serializable
data class OldReminderStore(
  val state: OldReminderStoreState,
  val version: Int,
)

@Serializable
data class OldReminderStoreState(
  @SerialName("REMINDERS") val reminders: List<OldReminder> = emptyList(),
)

@Serializable
data class OldReminder(
  val id: String,
  @Serializable(with = EmptyStringAsNullSerializer::class) val label: String? = null,
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
  data class OldExternalAudioEntry(
    val id: String,
    val filepath: String,
    val label: String,
    val canDelete: Boolean = false,
    val loop: Boolean = false,
    val notif: Boolean = false,
  ) : OldAudioEntry

  @Serializable
  data class OldDefaultAudioEntry(
    val label: String,
  ) : OldAudioEntry {
    val id = "default"
    val filepath: String? = null
    val canDelete = false
    val loop = false
    val notif = true
  }
}

internal object OldAudioEntrySerializer : KSerializer<OldAudioEntry> {
  override val descriptor: SerialDescriptor =
    OldAudioEntry.OldResourceOldAudioEntry.serializer().descriptor

  override fun deserialize(decoder: Decoder): OldAudioEntry {
    val jsonDecoder =
      decoder as? JsonDecoder
        ?: throw SerializationException("OldAudioEntrySerializer can be used only with JSON")

    val element: JsonElement = jsonDecoder.decodeJsonElement()

    // Try resource entry first (filepath as Int)
    try {
      return jsonDecoder.json.decodeFromJsonElement(
        OldAudioEntry.OldResourceOldAudioEntry.serializer(),
        element,
      )
    } catch (_: Exception) {
    }

    // Then try external entry (filepath as String)
    try {
      return jsonDecoder.json.decodeFromJsonElement(
        OldAudioEntry.OldExternalAudioEntry.serializer(),
        element,
      )
    } catch (e: Exception) {
    }
    try {
      return jsonDecoder.json.decodeFromJsonElement(
        OldAudioEntry.OldDefaultAudioEntry.serializer(),
        element,
      )
    } catch (e: Exception) {
      throw SerializationException("Cannot deserialize OldAudioEntry: ${e.message}")
    }
  }

  override fun serialize(
    encoder: Encoder,
    value: OldAudioEntry,
  ) {
    when (value) {
      is OldAudioEntry.OldResourceOldAudioEntry -> {
        encoder.encodeSerializableValue(
          OldAudioEntry.OldResourceOldAudioEntry.serializer(),
          value,
        )
      }

      is OldAudioEntry.OldExternalAudioEntry -> {
        encoder.encodeSerializableValue(
          OldAudioEntry.OldExternalAudioEntry.serializer(),
          value,
        )
      }

      is OldAudioEntry.OldDefaultAudioEntry -> {
        encoder.encodeSerializableValue(
          OldAudioEntry.OldDefaultAudioEntry.serializer(),
          value,
        )
      }
    }
  }
}

fun OldReminder.toReminder() =
  Reminder(
    id = this.id,
    label = this.label ?: "",
    enabled = this.enabled,
    prayer = this.prayer,
    duration = this.duration,
    durationModifier = this.durationModifier,
    sound = this.sound?.toReminderAudioEntry(),
    once = this.once,
    days = this.days?.toPrayerAlarmSettings(),
  )

fun OldAudioEntry.toReminderAudioEntry() =
  when (this) {
    is OldAudioEntry.OldResourceOldAudioEntry -> {
      ReminderAudioEntry.ResourceReminderAudioEntry(
        id = this.id,
        resourceId = this.filepath,
        label = this.label,
        canDelete = this.canDelete,
        loop = this.loop,
        notif = this.notif,
      )
    }

    is OldAudioEntry.OldExternalAudioEntry -> {
      ReminderAudioEntry.ExternalReminderAudioEntry(
        id = this.id,
        filepath = this.filepath,
        label = this.label,
        canDelete = this.canDelete,
        loop = this.loop,
        notif = this.notif,
      )
    }

    is OldAudioEntry.OldDefaultAudioEntry -> {
      ReminderAudioEntry.DefaultReminderAudioEntry
    }
  }
