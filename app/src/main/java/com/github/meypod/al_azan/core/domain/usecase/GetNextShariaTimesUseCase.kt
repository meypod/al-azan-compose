package com.github.meypod.al_azan.core.domain.usecase

import androidx.compose.runtime.Immutable
import com.github.meypod.al_azan.core.domain.model.adhan.Prayer
import com.github.meypod.al_azan.core.domain.model.adhan.ShariaTimes
import com.github.meypod.al_azan.core.domain.model.alarm.AlarmSettings
import com.github.meypod.al_azan.core.domain.model.calculation.CalculationAdjustments
import com.github.meypod.al_azan.core.domain.model.calculation.CalculationLocationDetail
import com.github.meypod.al_azan.core.domain.util.addDaysTimeZoneAware
import io.github.meypod.adhan_kotlin.CalculationParameters
import io.github.meypod.adhan_kotlin.data.DateComponents
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import javax.inject.Inject
import kotlin.time.Instant

@Immutable
data class ShariaTimeDetails(
    val forInstant: Instant,
    val forDate: DateComponents,
    val prayer: Prayer,
    val prayerTime: Instant,
    val notify: Boolean,
    val sound: Boolean,
)

private val possiblePrevDayPrayers = listOf(Prayer.Midnight, Prayer.Tahajjud)

class GetNextShariaTimesUseCase @Inject constructor(
    private val getShariaTimesUseCase: GetShariaTimesUseCase,
) {

    /**
     * Gets the next ShariaTimes after the passed [Instant]
     *
     * @param alarmSettings If not null, will return the first Shariah time according to this settings
     */
    operator fun invoke(
        instant: Instant,
        calculationParameters: CalculationParameters,
        calculationAdjustments: CalculationAdjustments,
        arabicCalendar: String,
        locationDetail: CalculationLocationDetail,
        alarmSettings: AlarmSettings? = null,
        excluding: Set<Prayer> = emptySet(),
    ): ShariaTimeDetails? {
        val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())

        // we need this check only for the first 6 hours of the day for tahajjud and midnight
        if (localDateTime.hour <= 6 && (alarmSettings?.shouldNotifyFor(instant, possiblePrevDayPrayers) ?: true)) {
            val prevDayInstant = addDaysTimeZoneAware(instant, -1)
            val prevDayShariahTimes = getShariaTimesUseCase(
                prevDayInstant,
                calculationParameters,
                calculationAdjustments,
                arabicCalendar,
                locationDetail,
            )
            val nextPrayer = prevDayShariahTimes.nextPrayerForAlarm(instant, alarmSettings, excluding)
            if (nextPrayer != null) {
                return ShariaTimeDetails(
                    forInstant = prevDayInstant,
                    forDate = DateComponents.from(prevDayInstant),
                    prayer = nextPrayer,
                    prayerTime = prevDayShariahTimes.forPrayer(nextPrayer),
                    notify = alarmSettings?.getNotifSettings(nextPrayer)?.shouldFireFor(instant) ?: false,
                    sound = alarmSettings?.getSoundSettings(nextPrayer)?.shouldFireFor(instant) ?: false,
                )
            }
        }

        var instantToCheck = instant
        var shariahTimes: ShariaTimes? = null
        var nextPrayer: Prayer? = null
        for (day in 1..7) {
            shariahTimes = getShariaTimesUseCase(instantToCheck, calculationParameters, calculationAdjustments, arabicCalendar, locationDetail)
            // `instant` (now) is earlier than every prayer on a future day, so on later days this returns
            // that day's first non-excluded prayer; on the current day it returns the next upcoming one.
            nextPrayer = shariahTimes.nextPrayerForAlarm(instant, alarmSettings, excluding)
            if (nextPrayer == null) {
                instantToCheck = addDaysTimeZoneAware(instantToCheck, 1)
            } else {
                break
            }
        }

        if (nextPrayer == null || shariahTimes == null) {
            return null
        }

        return ShariaTimeDetails(
            forInstant = instantToCheck,
            forDate = DateComponents.from(instantToCheck),
            prayer = nextPrayer,
            prayerTime = shariahTimes.forPrayer(nextPrayer),
            notify = alarmSettings?.getNotifSettings(nextPrayer)?.shouldFireFor(instantToCheck) ?: false,
            sound = alarmSettings?.getSoundSettings(nextPrayer)?.shouldFireFor(instantToCheck) ?: false,
        )
    }
}
