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
        // The old app stored the offset as a positive magnitude in milliseconds (sign lives in
        // durationModifier); the new model is in minutes. Convert instead of copying the raw value.
        duration = (this.duration / 60_000L).toInt(),
        durationModifier = this.durationModifier,
        sound = this.sound?.toReminderAudioEntry(),
        once = this.once,
        days = this.days?.toPrayerAlarmSettings(),
    )

fun OldAudioEntry.toReminderAudioEntry(): ReminderAudioEntry {
    // The stable string id is authoritative. Any sound this build still bundles — the silent track or an
    // adhan voice — is re-resolved to its current resource int via mapAdhanIdToEntryOrNull, regardless of
    // how the old app stored it: as a bundled resource int, OR as an external entry whose filepath is a
    // now-stale download/asset path (e.g. an adhan voice downloaded by the old app). Only genuinely
    // external user files (custom picks, device ringtones) keep their stored path. An id this build no
    // longer bundles with no usable path falls back to the default notification sound.
    val id: String
    val label: String
    val canDelete: Boolean
    val loop: Boolean
    val externalPath: String?
    when (this) {
        is OldAudioEntry.OldResourceOldAudioEntry -> {
            id = this.id
            label = this.label
            canDelete = this.canDelete
            loop = this.loop
            externalPath = null
        }

        is OldAudioEntry.OldExternalAudioEntry -> {
            id = this.id
            label = this.label
            canDelete = this.canDelete
            loop = this.loop
            externalPath = this.filepath
        }

        is OldAudioEntry.OldDefaultAudioEntry -> {
            id = this.id
            label = this.label
            canDelete = this.canDelete
            loop = this.loop
            externalPath = null
        }
    }

    mapAdhanIdToEntryOrNull(id)?.resId?.let { resourceId ->
        return ReminderAudioEntry.ResourceReminderAudioEntry(
            id = id,
            resourceId = resourceId,
            label = label,
            canDelete = canDelete,
            loop = loop,
        )
    }

    return externalPath?.let { path ->
        ReminderAudioEntry.ExternalReminderAudioEntry(
            id = id,
            filepath = path,
            label = label,
            canDelete = canDelete,
            loop = loop,
        )
    } ?: ReminderAudioEntry.DefaultReminderAudioEntry
}
