package com.github.meypod.al_azan.appfunctions

import android.app.Application
import com.github.meypod.al_azan.core.domain.model.calculation.CalculationAdjustments
import com.github.meypod.al_azan.core.domain.model.calculation.CalculationSettings
import com.github.meypod.al_azan.core.domain.model.settings.Settings
import com.github.meypod.al_azan.core.domain.repository.CalculationSettingsRepository
import com.github.meypod.al_azan.core.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class CalendarAppFunctionsTest {
    private class FakeSettingsRepository(
        settings: Settings,
    ) : SettingsRepository {
        private val state = MutableStateFlow(settings)

        override val data: Flow<Settings> get() = state

        override suspend fun fetch(): Settings = state.value

        override suspend fun update(transform: (t: Settings) -> Settings) {
            state.value = transform(state.value)
        }
    }

    private class FakeCalculationSettingsRepository(
        settings: CalculationSettings,
    ) : CalculationSettingsRepository {
        private val state = MutableStateFlow(settings)

        override val data: Flow<CalculationSettings> get() = state

        override suspend fun fetch(): CalculationSettings = state.value

        override suspend fun update(transform: (t: CalculationSettings) -> CalculationSettings) {
            state.value = transform(state.value)
        }
    }

    private fun functions(hijriAdjustment: Int = 0) =
        CalendarAppFunctions(
            // Persian app language, to show names stay English for the agent.
            FakeSettingsRepository(Settings(selectedLocale = "fa", selectedArabicCalendar = "islamic-umalqura")),
            FakeCalculationSettingsRepository(CalculationSettings(calculationAdjustments = CalculationAdjustments(hijriDate = hijriAdjustment))),
        )

    @Test
    fun `a Persian date converts to its Gregorian day`() = runTest {
        val result = functions().convertDate("persian", 1405, 8, 10, "gregorian")
        assertEquals(ConvertedDate("gregorian", 2026, 11, 1, "November", "November 1, 2026", "Sunday", "2026-11-01"), result)
    }

    @Test
    fun `a Gregorian date converts to Persian with English names`() = runTest {
        val result = functions().convertDate("gregorian", 2026, 11, 1, "persian")
        assertEquals(1405, result.year)
        assertEquals(8, result.month)
        assertEquals(10, result.day)
        assertEquals("Aban", result.monthName)
        assertEquals("2026-11-01", result.gregorianDate)
    }

    @Test
    fun `hijri follows the user's day adjustment`() = runTest {
        val adjusted = functions(hijriAdjustment = 1).convertDate("gregorian", 2026, 11, 1, "hijri")
        val nextDay = functions().convertDate("gregorian", 2026, 11, 2, "hijri")
        assertEquals(nextDay.copy(weekday = "Sunday", gregorianDate = "2026-11-01"), adjusted)
        // Going back with the same adjustment lands on the same civil day.
        val back = functions(hijriAdjustment = 1).convertDate("hijri", adjusted.year, adjusted.month, adjusted.day, "gregorian")
        assertEquals("2026-11-01", back.gregorianDate)
    }

    @Test
    fun `unknown calendars and missing days are rejected`() = runTest {
        val unknown = runCatching { functions().convertDate("julian", 2026, 1, 1, "gregorian") }.exceptionOrNull()
        assertTrue(unknown is IllegalArgumentException)
        // 1404 is a common Persian year, so Esfand has 29 days.
        val missing = runCatching { functions().convertDate("persian", 1404, 12, 30, "gregorian") }.exceptionOrNull()
        assertTrue(missing is IllegalArgumentException)
    }
}
