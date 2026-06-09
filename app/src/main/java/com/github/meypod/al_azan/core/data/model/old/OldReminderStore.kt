package com.github.meypod.al_azan.core.data.model.old

import com.github.meypod.al_azan.core.domain.model.adhan.Prayer
import com.github.meypod.al_azan.core.domain.model.reminder.Reminder
import com.github.meypod.al_azan.core.domain.model.reminder.ReminderAudioEntry
import com.github.meypod.al_azan.core.domain.model.settings.mapAdhanIdToEntryOrNull
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
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

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

        // Discriminate by the `filepath` field instead of trial-and-error decoding: a resource entry
        // has it as an Int, an external entry as a String, and the default entry omits it. This mirrors
        // OldAdhanAudioEntrySerializer and avoids the old chain throwing on an unexpected shape.
        val filepath = element.jsonObject["filepath"]
        val serializer =
            when {
                // The default entry stores `filepath` as JSON null (present, not absent).
                filepath == null || filepath is JsonNull -> OldAudioEntry.OldDefaultAudioEntry.serializer()

                filepath.jsonPrimitive.isString -> OldAudioEntry.OldExternalAudioEntry.serializer()

                else -> OldAudioEntry.OldResourceOldAudioEntry.serializer()
            }

        try {
            return jsonDecoder.json.decodeFromJsonElement(serializer, element)
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
        duration = this.duration.toInt(),
        durationModifier = this.durationModifier,
        sound = this.sound?.toReminderAudioEntry(),
        once = this.once,
        days = this.days?.toPrayerAlarmSettings(),
    )

fun OldAudioEntry.toReminderAudioEntry() =
    when (this) {
        is OldAudioEntry.OldResourceOldAudioEntry -> {
            // The old app's stored resource int is meaningless here — Android resource ids are not
            // stable across builds/apps. Re-resolve the current resource int from the stable string id;
            // an id this build no longer bundles falls back to the default notification sound.
            mapAdhanIdToEntryOrNull(this.id)?.resId?.let { resourceId ->
                ReminderAudioEntry.ResourceReminderAudioEntry(
                    id = this.id,
                    resourceId = resourceId,
                    label = this.label,
                    canDelete = this.canDelete,
                    loop = this.loop,
                    notif = this.notif,
                )
            } ?: ReminderAudioEntry.DefaultReminderAudioEntry
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
