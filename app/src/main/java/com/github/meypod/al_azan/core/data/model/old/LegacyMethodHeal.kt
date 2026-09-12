package com.github.meypod.al_azan.core.data.model.old

import com.github.meypod.al_azan.core.domain.model.calculation.CalculationSettings
import io.github.meypod.adhan_kotlin.CalculationMethod
import io.github.meypod.adhan_kotlin.CalculationParameters
import io.github.meypod.adhan_kotlin.PrayerAdjustments

/**
 * Repairs calculation settings that the first-launch migration corrupted before the explicit legacy
 * key map landed.
 *
 * Until then the migration resolved the stored method with `CalculationMethod.valueOf(key.uppercase())`.
 * The legacy app persisted PascalCase keys, so every multi-word method missed: `MoonsightingCommittee`
 * uppercases to `MOONSIGHTINGCOMMITTEE`, which names no entry, and the lookup fell back to
 * [CalculationMethod.OTHER]. Because the parameters are then built from `method.parameters`, those users
 * were left with a Fajr and Isha angle of 0 and none of the method's built-in adjustments. The result is
 * not a slightly wrong time but a nonsensical one: a 0 angle asks for the moment the sun's centre crosses
 * the true horizon, so Fajr lands *after* sunrise and Isha *before* maghrib.
 *
 * `MuslimWorldLeague`, `MoonsightingCommittee`, `UmmAlQura` and `NorthAmerica` were all affected.
 *
 * The migration runs once, guarded by a flag that is set even when it throws, so fixing it forward left
 * everyone already migrated stuck with the corrupt values. This heals them, and it can do so exactly
 * rather than by guessing: the migration reads the legacy MMKV stores but never deletes them, so the
 * original key is still on disk. See [LegacyStorageKeys].
 */

/**
 * The corrupt signature, matched conservatively.
 *
 * Everything the broken branch could produce is here, and nothing else. A user who deliberately picked
 * "Custom" and left the angles alone looks identical, which is why [withLegacyMethodHealed] additionally
 * requires the legacy key to name a real method: `Custom` maps to [CalculationMethod.OTHER] by design and
 * must never be rewritten.
 */
private fun CalculationParameters.looksLikeFailedMethodLookup(): Boolean =
    method == CalculationMethod.OTHER &&
        fajrAngle == 0.0 &&
        ishaAngle == 0.0 &&
        ishaInterval == 0 &&
        maghribAngle == 0.0 &&
        methodAdjustments == PrayerAdjustments()

/**
 * Restores the method the user originally chose, or returns the settings untouched.
 *
 * Only the fields the failed lookup destroyed are replaced. Anything the user could have changed since —
 * madhab, high latitude rule, rounding, shafaq, polar resolution and their own minute adjustments — is
 * preserved, because a repair that also reverted deliberate choices would trade one silent surprise for
 * another.
 *
 * @param legacy the old app's stored calculation settings, or null when none remain on disk.
 */
fun CalculationSettings.withLegacyMethodHealed(
    legacy: OldCalculationSettingsState?,
): CalculationSettings {
    val current = parameters ?: return this
    if (!current.looksLikeFailedMethodLookup()) return this

    val recovered = legacy?.getCalculationParameters() ?: return this
    // `Custom` and any unrecognised key both resolve to OTHER. Neither is evidence of corruption, so
    // leave those users exactly as they are.
    if (recovered.method == CalculationMethod.OTHER) return this

    return copy(
        parameters = current.copy(
            method = recovered.method,
            fajrAngle = recovered.fajrAngle,
            ishaAngle = recovered.ishaAngle,
            ishaInterval = recovered.ishaInterval,
            maghribAngle = recovered.maghribAngle,
            methodAdjustments = recovered.methodAdjustments,
        ),
    )
}
