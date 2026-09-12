package com.github.meypod.al_azan.di

import com.github.meypod.al_azan.core.data.model.old.LegacyCalculationSettingsReader
import com.github.meypod.al_azan.core.data.model.old.OldCalculationSettingsState
import com.github.meypod.al_azan.core.domain.model.calculation.CalculationAdjustments
import com.github.meypod.al_azan.core.domain.model.calculation.CalculationSettings
import com.github.meypod.al_azan.core.domain.model.settings.Settings
import com.github.meypod.al_azan.core.domain.repository.CalculationSettingsRepository
import com.github.meypod.al_azan.core.domain.repository.SettingsRepository
import io.github.meypod.adhan_kotlin.CalculationMethod
import io.github.meypod.adhan_kotlin.CalculationParameters
import io.github.meypod.adhan_kotlin.MidnightMethod
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards the repair's run-once behaviour and, above all, its write order: the corrected settings must
 * land before the flag that says they are corrected, so an interrupted run repeats instead of stranding
 * someone on wrong times.
 */
class LegacyMethodHealRunnerTest {
    private class FakeCalculationSettingsRepository(
        initial: CalculationSettings,
    ) : CalculationSettingsRepository {
        private val state = MutableStateFlow(initial)
        var updateCount = 0
            private set

        override val data: Flow<CalculationSettings> get() = state

        override suspend fun fetch(): CalculationSettings = state.value

        override suspend fun update(transform: (t: CalculationSettings) -> CalculationSettings) {
            updateCount++
            state.value = transform(state.value)
        }
    }

    private class FakeSettingsRepository(
        healApplied: Boolean = false,
    ) : SettingsRepository {
        private val state =
            MutableStateFlow(Settings(selectedLocale = "en", legacyMethodHealApplied = healApplied))

        override val data: Flow<Settings> get() = state

        override suspend fun fetch(): Settings = state.value

        override suspend fun update(transform: (t: Settings) -> Settings) {
            state.value = transform(state.value)
        }
    }

    private fun legacyState(methodKey: String?) = OldCalculationSettingsState(
        calculationMethodKey = methodKey,
        midnightMethod = MidnightMethod.SunsetToFajr,
        fajrAdjustment = 0,
        sunriseAdjustment = 0,
        dhuhrAdjustment = 0,
        asrAdjustment = 0,
        sunsetAdjustment = 0,
        maghribAdjustment = 0,
        ishaAdjustment = 0,
        midnightAdjustment = 0,
        hijriDateAdjustment = 0,
    )

    private fun corruptSettings() = CalculationSettings(
        locationId = "default",
        parameters = CalculationParameters(),
        calculationAdjustments = CalculationAdjustments(),
        midnightMethod = MidnightMethod.SunsetToFajr,
    )

    private fun runnerFor(
        calc: FakeCalculationSettingsRepository,
        settings: FakeSettingsRepository,
        reader: LegacyCalculationSettingsReader,
    ) = LegacyMethodHealRunner(
        legacyCalculationSettingsReader = reader,
        calculationSettingsRepository = calc,
        settingsRepository = settings,
    )

    @Test
    fun repairsCorruptSettingsAndMarksItDone() = runTest {
        val calc = FakeCalculationSettingsRepository(corruptSettings())
        val settings = FakeSettingsRepository()

        runnerFor(calc, settings) { legacyState("MoonsightingCommittee") }.run()

        assertEquals(CalculationMethod.MOON_SIGHTING_COMMITTEE, calc.fetch().parameters!!.method)
        assertTrue(settings.fetch().legacyMethodHealApplied)
    }

    @Test
    fun doesNothingOnceTheFlagIsSet() = runTest {
        val calc = FakeCalculationSettingsRepository(corruptSettings())
        val settings = FakeSettingsRepository(healApplied = true)

        runnerFor(calc, settings) { legacyState("MoonsightingCommittee") }.run()

        assertEquals(0, calc.updateCount)
        assertEquals(CalculationMethod.OTHER, calc.fetch().parameters!!.method)
    }

    /** Nothing to repair still marks the pass done, so it does not re-read on every launch. */
    @Test
    fun marksDoneWithoutWritingWhenThereIsNothingToRepair() = runTest {
        val calc = FakeCalculationSettingsRepository(corruptSettings())
        val settings = FakeSettingsRepository()

        runnerFor(calc, settings) { legacyState("Custom") }.run()

        assertEquals(0, calc.updateCount)
        assertTrue(settings.fetch().legacyMethodHealApplied)
    }

    @Test
    fun toleratesLegacyDataBeingGone() = runTest {
        val calc = FakeCalculationSettingsRepository(corruptSettings())
        val settings = FakeSettingsRepository()

        runnerFor(calc, settings) { null }.run()

        assertEquals(0, calc.updateCount)
        assertTrue(settings.fetch().legacyMethodHealApplied)
    }

    /**
     * The safety property. If writing the corrected settings fails, the flag must stay unset so the next
     * launch tries again.
     */
    @Test
    fun leavesFlagUnsetWhenWritingTheRepairFails() = runTest {
        val failing = object : CalculationSettingsRepository {
            private val state = MutableStateFlow(corruptSettings())
            override val data: Flow<CalculationSettings> get() = state
            override suspend fun fetch(): CalculationSettings = state.value
            override suspend fun update(transform: (t: CalculationSettings) -> CalculationSettings) {
                throw IllegalStateException("storage unavailable")
            }
        }
        val settings = FakeSettingsRepository()

        runCatching {
            LegacyMethodHealRunner(
                legacyCalculationSettingsReader = { legacyState("MoonsightingCommittee") },
                calculationSettingsRepository = failing,
                settingsRepository = settings,
            ).run()
        }

        assertFalse(settings.fetch().legacyMethodHealApplied)
    }

    @Test
    fun isIdempotentAcrossTwoRuns() = runTest {
        val calc = FakeCalculationSettingsRepository(corruptSettings())
        val settings = FakeSettingsRepository()
        val runner = runnerFor(calc, settings) { legacyState("MuslimWorldLeague") }

        runner.run()
        val afterFirst = calc.fetch()
        runner.run()

        assertEquals(afterFirst, calc.fetch())
        assertEquals(1, calc.updateCount)
    }
}
