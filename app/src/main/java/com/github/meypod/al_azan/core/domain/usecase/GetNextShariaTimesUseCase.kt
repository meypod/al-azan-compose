package com.github.meypod.al_azan.core.domain.usecase

import androidx.compose.runtime.Immutable
import com.github.meypod.al_azan.core.domain.model.adhan.Prayer
import com.github.meypod.al_azan.core.domain.model.adhan.ShariaTimes
import com.github.meypod.al_azan.core.domain.model.alarm.AlarmSettings
import com.github.meypod.al_azan.core.domain.model.calculation.CalculationAdjustments
import com.github.meypod.al_azan.core.domain.model.calculation.CalculationLocationDetail
import com.github.meypod.al_azan.core.domain.utils.addDaysTimeZoneAware
import io.github.meypod.adhan_kotlin.CalculationParameters
import io.github.meypod.adhan_kotlin.data.DateComponents
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import javax.inject.Inject
import kotlin.time.DurationUnit
import kotlin.time.Instant
import kotlin.time.toDuration

@Immutable
data class ShariaTimeDetails(
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
            val nextPrayer = prevDayShariahTimes.nextPrayerForAlarm(instant, alarmSettings)
            if (nextPrayer != null) {
                return ShariaTimeDetails(
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
            shariahTimes = getShariaTimesUseCase(instant, calculationParameters, calculationAdjustments, arabicCalendar, locationDetail)
            nextPrayer = shariahTimes.nextPrayerForAlarm(instant, alarmSettings)
            if (nextPrayer == null) {
                instantToCheck = instantToCheck.plus(1.toDuration(DurationUnit.DAYS))
            } else {
                break
            }
        }

        if (nextPrayer == null || shariahTimes == null) {
            return null
        }

        return ShariaTimeDetails(
            forDate = DateComponents.from(instantToCheck),
            prayer = nextPrayer,
            prayerTime = shariahTimes.forPrayer(nextPrayer),
            notify = alarmSettings?.getNotifSettings(nextPrayer)?.shouldFireFor(instantToCheck) ?: false,
            sound = alarmSettings?.getSoundSettings(nextPrayer)?.shouldFireFor(instantToCheck) ?: false,
        )
    }
}
