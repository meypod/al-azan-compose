package com.github.meypod.al_azan.di

import com.github.meypod.al_azan.core.domain.model.calculation.CalculationAdjustments
import com.github.meypod.al_azan.core.domain.model.calculation.CalculationSettings
import com.github.meypod.al_azan.core.domain.model.settings.Settings
import com.github.meypod.al_azan.core.domain.repository.CalculationSettingsRepository
import com.github.meypod.al_azan.core.domain.repository.SettingsRepository
import io.github.meypod.adhan_kotlin.CalculationMethod
import io.github.meypod.adhan_kotlin.HighLatitudeRule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards the one-time upgrade of pre-adhan-0.0.13 Diyanet settings: stored parameters are a snapshot
 * taken when the method was picked, so without this the takdir and the non-interpolated declination
 * never reach users who already had Diyanet selected.
 */
class DiyanetParamsMigrationRunnerTest {
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

    private class FakeSettingsRepository : SettingsRepository {
        private val state = MutableStateFlow(Settings(selectedLocale = "en"))

        override val data: Flow<Settings> get() = state

        override suspend fun fetch(): Settings = state.value

        override suspend fun update(transform: (t: Settings) -> Settings) {
            state.value = transform(state.value)
        }
    }

    /** The parameters adhan 0.0.12 produced for [CalculationMethod.TURKEY]. */
    private val preUpgradeTurkeyParams = CalculationMethod.TURKEY.parameters.copy(
        highLatitudeRule = null,
        interpolateDeclination = true,
    )

    private class MigrationResult(
        val settings: CalculationSettings,
        val updateCount: Int,
        val noticePending: Boolean,
    )

    private suspend fun migrate(settings: CalculationSettings): MigrationResult {
        val calculationSettingsRepository = FakeCalculationSettingsRepository(settings)
        val settingsRepository = FakeSettingsRepository()

        DiyanetParamsMigrationRunner(calculationSettingsRepository, settingsRepository).run()

        return MigrationResult(
            settings = calculationSettingsRepository.fetch(),
            updateCount = calculationSettingsRepository.updateCount,
            noticePending = settingsRepository.fetch().diyanetChangeNoticePending,
        )
    }

    @Test
    fun `stored Diyanet parameters gain the takdir and the held declination`() = runTest {
        val result = migrate(CalculationSettings(parameters = preUpgradeTurkeyParams))

        assertEquals(CalculationMethod.TURKEY.parameters, result.settings.parameters)
        assertEquals(HighLatitudeRule.PROPORTIONAL_DEPRESSION, result.settings.parameters?.highLatitudeRule)
        assertEquals(false, result.settings.parameters?.interpolateDeclination)
    }

    @Test
    fun `a high latitude rule the user chose is replaced by the canonical one`() = runTest {
        val chosen = preUpgradeTurkeyParams.copy(highLatitudeRule = HighLatitudeRule.MIDDLE_OF_THE_NIGHT)

        val result = migrate(CalculationSettings(parameters = chosen))

        assertEquals(HighLatitudeRule.PROPORTIONAL_DEPRESSION, result.settings.parameters?.highLatitudeRule)
    }

    @Test
    fun `minute adjustments are cleared but the hijri day offset is kept`() = runTest {
        val settings = CalculationSettings(
            parameters = preUpgradeTurkeyParams,
            calculationAdjustments = CalculationAdjustments(
                fajr = -3,
                sunrise = 2,
                dhuhr = 1,
                asr = -1,
                maghrib = 4,
                sunset = 4,
                isha = -6,
                midnight = 5,
                tahajjud = -5,
                hijriDate = 1,
            ),
        )

        val result = migrate(settings)

        assertEquals(CalculationAdjustments(hijriDate = 1), result.settings.calculationAdjustments)
    }

    @Test
    fun `edited angles survive the migration`() = runTest {
        val edited = preUpgradeTurkeyParams.copy(fajrAngle = 19.5, ishaAngle = 16.0)

        val result = migrate(CalculationSettings(parameters = edited))

        val params = result.settings.parameters!!
        assertEquals(19.5, params.fajrAngle, 0.0)
        assertEquals(16.0, params.ishaAngle, 0.0)
        assertEquals(HighLatitudeRule.PROPORTIONAL_DEPRESSION, params.highLatitudeRule)
    }

    @Test
    fun `a rewritten user is flagged for the notice`() = runTest {
        val result = migrate(CalculationSettings(parameters = preUpgradeTurkeyParams))

        assertTrue(result.noticePending)
    }

    @Test
    fun `other methods are left untouched and not notified`() = runTest {
        val settings = CalculationSettings(
            parameters = CalculationMethod.MUSLIM_WORLD_LEAGUE.parameters,
            calculationAdjustments = CalculationAdjustments(fajr = -3),
        )

        val result = migrate(settings)

        assertSame(settings, result.settings)
        assertEquals(0, result.updateCount)
        assertFalse(result.noticePending)
    }

    @Test
    fun `a fresh install with no parameters is not written to`() = runTest {
        val result = migrate(CalculationSettings())

        assertNull(result.settings.parameters)
        assertEquals(0, result.updateCount)
        assertFalse(result.noticePending)
    }

    @Test
    fun `running twice writes only once`() = runTest {
        val calculationSettingsRepository = FakeCalculationSettingsRepository(
            CalculationSettings(parameters = preUpgradeTurkeyParams),
        )
        val runner = DiyanetParamsMigrationRunner(calculationSettingsRepository, FakeSettingsRepository())

        runner.run()
        runner.run()

        assertEquals(1, calculationSettingsRepository.updateCount)
    }

    /**
     * The Europe variant only ever existed from 0.0.13 on, so it is already correct when stored and
     * must not be rewritten — but it must carry the takdir, which is what the migration restores for
     * [CalculationMethod.TURKEY].
     */
    @Test
    fun `the Europe variant already ships the takdir`() = runTest {
        val europe = CalculationMethod.TURKEY_EUROPE.parameters

        assertEquals(HighLatitudeRule.PROPORTIONAL_DEPRESSION, europe.highLatitudeRule)
        assertFalse(europe.interpolateDeclination)

        val result = migrate(CalculationSettings(parameters = europe))
        assertEquals(0, result.updateCount)
    }
}
