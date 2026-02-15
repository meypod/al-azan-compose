package com.github.meypod.al_azan.core.util.serialization

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

class FalsyAsNullSerializer<T>(
    private val delegate: KSerializer<T?>,
) : KSerializer<T?> {
    override val descriptor: SerialDescriptor = delegate.descriptor

    @OptIn(ExperimentalSerializationApi::class)
    override fun serialize(
        encoder: Encoder,
        value: T?,
    ) {
        if (value == null) {
            encoder.encodeNull()
            return
        }

        val isFalsy = when (value) {
            is String -> value.isEmpty()

            is Collection<*> -> value.isEmpty()

            is Map<*, *> -> value.isEmpty()

            is Boolean -> value == false

            is Number -> when (value) {
                is Float -> value == 0f
                is Double -> value == 0.0
                else -> value.toLong() == 0L
            }

            else -> false
        }

        if (isFalsy) {
            encoder.encodeNull()
        } else {
            delegate.serialize(encoder, value)
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    override fun deserialize(decoder: Decoder): T? {
        val decoded = try {
            decoder.decodeNullableSerializableValue(delegate)
        } catch (e: SerializationException) {
            return null
        }

        if (decoded == null) return null

        val isFalsy = when (decoded) {
            is String -> decoded.isEmpty()

            is Collection<*> -> decoded.isEmpty()

            is Map<*, *> -> decoded.isEmpty()

            is Boolean -> decoded == false

            is Number -> when (decoded) {
                is Float -> decoded == 0f
                is Double -> decoded == 0.0
                else -> decoded.toLong() == 0L
            }

            else -> false
        }

        return if (isFalsy) null else decoded
    }
}

/**
 * Factory to create a `FalsyAsNullSerializer` from a non-nullable `KSerializer<T>`.
 */
fun <T : Any> FalsyAsNullSerializer(delegate: KSerializer<T>): FalsyAsNullSerializer<T> = FalsyAsNullSerializer(delegate.nullable)
