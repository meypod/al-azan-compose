package com.github.meypod.al_azan.appfunctions

import androidx.appfunctions.AppFunctionContext
import androidx.appfunctions.AppFunctionSerializable
import androidx.appfunctions.service.AppFunction
import com.github.meypod.al_azan.core.domain.model.adhan.SHARIA_TIMES_IN_ORDER
import com.github.meypod.al_azan.core.domain.repository.CalculationSettingsRepository
import com.github.meypod.al_azan.core.domain.repository.FavoriteLocationsRepository
import com.github.meypod.al_azan.core.domain.repository.SettingsRepository
import com.github.meypod.al_azan.core.domain.usecase.GetShariaTimesUseCase
import jakarta.inject.Inject
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import kotlin.time.toJavaInstant
import kotlin.time.toKotlinInstant

/**
 * A single prayer or sun-event time for a day.
 */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class PrayerTimeEntry(
    /** Name of the prayer or sun event: Fajr, Sunrise, Dhuhr, Asr, Sunset, Maghrib, Isha, Midnight or Tahajjud. */
    val name: String,
    /** Local clock time in 24-hour HH:mm format, e.g. 05:23. */
    val time: String,
    /** Full ISO-8601 UTC timestamp of the event, e.g. 2026-06-20T01:53:00Z. */
    val isoTimestamp: String,
)

/**
 * Islamic prayer (adhan) times for a single calendar date.
 */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class PrayerTimesResult(
    /** The Gregorian date the times are for, in yyyy-MM-dd format. */
    val date: String,
    /** Human-readable name of the location the times were calculated for. */
    val location: String,
    /** IANA timezone id used to compute the local clock times, e.g. Asia/Tehran. */
    val timeZone: String,
    /** Ordered list of prayer and sun-event times for the day. */
    val prayerTimes: List<PrayerTimeEntry>,
)

/**
 * Exposes the app's prayer-time calculation to on-device AI agents via AppFunctions.
 */
class PrayerTimesAppFunctions @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val calculationSettingsRepository: CalculationSettingsRepository,
    private val favoriteLocationsRepository: FavoriteLocationsRepository,
    private val getShariaTimesUseCase: GetShariaTimesUseCase,
) {
    /**
     * Returns the Islamic prayer times (adhan times) for a given Gregorian date,
     * computed for the user's configured location and calculation method.
     *
     * Use this to answer questions such as "what time is Fajr tomorrow" or "when is
     * Maghrib on 2026-06-20". The result includes Fajr, Sunrise, Dhuhr, Asr, Sunset,
     * Maghrib, Isha, Midnight and Tahajjud, each with a local clock time and a full
     * UTC timestamp.
     *
     * @param date The Gregorian date to compute prayer times for, in ISO yyyy-MM-dd
     *   format, for example 2026-06-20.
     * @return The prayer times for that date. Fails if the user has not yet configured
     *   a location and calculation method, or if the date is malformed.
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun getPrayerTimes(
        appFunctionContext: AppFunctionContext,
        date: String,
    ): PrayerTimesResult {
        val localDate = try {
            LocalDate.parse(date)
        } catch (e: DateTimeParseException) {
            throw IllegalArgumentException("Invalid date '$date'. Expected ISO format yyyy-MM-dd.", e)
        }

        val calcSettings = calculationSettingsRepository.fetch()
        val parameters = calcSettings.parameters
            ?: throw IllegalStateException("No calculation method configured in the app yet.")
        val location = favoriteLocationsRepository.fetch()
            .firstOrNull { it.id == calcSettings.locationId }
            ?: throw IllegalStateException("No location configured in the app yet.")
        val settings = settingsRepository.fetch()

        val zone = ZoneId.systemDefault()
        // Anchor on local noon so the day is unambiguous across DST transitions; the
        // use case normalizes to the start of the local day internally.
        val instant = localDate.atTime(12, 0).atZone(zone).toInstant().toKotlinInstant()

        val shariaTimes = getShariaTimesUseCase(
            instant = instant,
            calculationParameters = parameters,
            calculationAdjustments = calcSettings.calculationAdjustments,
            arabicCalendar = settings.selectedArabicCalendar,
            locationDetail = location.locationDetail,
        )

        val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
        val entries = SHARIA_TIMES_IN_ORDER.map { prayer ->
            val time = shariaTimes.forPrayer(prayer)
            PrayerTimeEntry(
                name = prayer.name,
                time = time.toJavaInstant().atZone(zone).format(timeFormatter),
                isoTimestamp = time.toString(),
            )
        }

        return PrayerTimesResult(
            date = localDate.toString(),
            location = location.locationDetail.toDisplayString(),
            timeZone = zone.id,
            prayerTimes = entries,
        )
    }
}
