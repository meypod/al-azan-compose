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

    private class FakeSettingsRepository(
        fixApplied: Boolean = false,
    ) : SettingsRepository {
        private val state = MutableStateFlow(Settings(selectedLocale = "en", diyanetFixApplied = fixApplied))

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
        val fixApplied: Boolean,
    )

    private suspend fun migrate(settings: CalculationSettings): MigrationResult {
        val calculationSettingsRepository = FakeCalculationSettingsRepository(settings)
        val settingsRepository = FakeSettingsRepository()

        DiyanetParamsMigrationRunner(calculationSettingsRepository, settingsRepository).run()

        return MigrationResult(
            settings = calculationSettingsRepository.fetch(),
            updateCount = calculationSettingsRepository.updateCount,
            noticePending = settingsRepository.fetch().diyanetChangeNoticePending,
            fixApplied = settingsRepository.fetch().diyanetFixApplied,
        )
    }

    @Test
    fun `stored Diyanet parameters gain the takdir and the held declination`() =
        runTest {
            val result = migrate(CalculationSettings(parameters = preUpgradeTurkeyParams))

            assertEquals(CalculationMethod.TURKEY.parameters, result.settings.parameters)
            assertEquals(HighLatitudeRule.PROPORTIONAL_DEPRESSION, result.settings.parameters?.highLatitudeRule)
            assertEquals(false, result.settings.parameters?.interpolateDeclination)
        }

    @Test
    fun `a high latitude rule the user chose is replaced by the canonical one`() =
        runTest {
            val chosen = preUpgradeTurkeyParams.copy(highLatitudeRule = HighLatitudeRule.MIDDLE_OF_THE_NIGHT)

            val result = migrate(CalculationSettings(parameters = chosen))

            assertEquals(HighLatitudeRule.PROPORTIONAL_DEPRESSION, result.settings.parameters?.highLatitudeRule)
        }

    @Test
    fun `minute adjustments are cleared but the hijri day offset is kept`() =
        runTest {
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

    /** Angles were part of the workaround, so the migration restores the method's own. */
    @Test
    fun `edited angles are reset to the method's`() =
        runTest {
            val edited = preUpgradeTurkeyParams.copy(fajrAngle = 19.5, ishaAngle = 16.0)

            val result = migrate(CalculationSettings(parameters = edited))

            assertEquals(CalculationMethod.TURKEY.parameters, result.settings.parameters)
        }

    @Test
    fun `a rewritten user is flagged for the notice`() =
        runTest {
            val result = migrate(CalculationSettings(parameters = preUpgradeTurkeyParams))

            assertTrue(result.noticePending)
        }

    @Test
    fun `other methods are left untouched and not notified`() =
        runTest {
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
    fun `a fresh install with no parameters is not written to`() =
        runTest {
            val result = migrate(CalculationSettings())

            assertNull(result.settings.parameters)
            assertEquals(0, result.updateCount)
            assertFalse(result.noticePending)
        }

    /**
     * The flag is stamped even where nothing needed fixing, so a fresh install's backups say they carry
     * the fix and restore without being clobbered.
     */
    @Test
    fun `the flag is stamped even when there was nothing to fix`() =
        runTest {
            assertTrue(migrate(CalculationSettings()).fixApplied)
            assertTrue(migrate(CalculationSettings(parameters = CalculationMethod.MUSLIM_WORLD_LEAGUE.parameters)).fixApplied)
        }

    /** Settings that already carry the fix are never touched again, whatever they hold. */
    @Test
    fun `settings already carrying the fix are skipped`() =
        runTest {
            val calculationSettingsRepository = FakeCalculationSettingsRepository(
                CalculationSettings(
                    parameters = preUpgradeTurkeyParams,
                    calculationAdjustments = CalculationAdjustments(fajr = -3),
                ),
            )
            val settingsRepository = FakeSettingsRepository(fixApplied = true)

            DiyanetParamsMigrationRunner(calculationSettingsRepository, settingsRepository).run()

            assertEquals(0, calculationSettingsRepository.updateCount)
            assertFalse(settingsRepository.fetch().diyanetChangeNoticePending)
        }

    /**
     * The flag guards this, so a retry only happens after a failure that may already have written part of
     * its changes. It must land on the same result, not compound.
     */
    @Test
    fun `running twice lands on the same result`() =
        runTest {
            val calculationSettingsRepository = FakeCalculationSettingsRepository(
                CalculationSettings(
                    parameters = preUpgradeTurkeyParams,
                    calculationAdjustments = CalculationAdjustments(fajr = -3, hijriDate = 1),
                ),
            )
            val settingsRepository = FakeSettingsRepository()
            val runner = DiyanetParamsMigrationRunner(calculationSettingsRepository, settingsRepository)

            runner.run()
            val afterFirst = calculationSettingsRepository.fetch()
            runner.run()

            assertEquals(afterFirst, calculationSettingsRepository.fetch())
            assertTrue(settingsRepository.fetch().diyanetChangeNoticePending)
        }

    /**
     * Parameters that already look canonical are still rewritten: a first-launch migration from the old
     * app writes canonical parameters but keeps that app's minute adjustments, which were tuned against
     * the same wrong times. Skipping those users would leave the fix carrying their old offsets.
     */
    @Test
    fun `canonical parameters still get their adjustments cleared and the notice raised`() =
        runTest {
            val settings = CalculationSettings(
                parameters = CalculationMethod.TURKEY.parameters,
                calculationAdjustments = CalculationAdjustments(fajr = -3, isha = 4, hijriDate = 1),
            )

            val result = migrate(settings)

            assertEquals(CalculationAdjustments(hijriDate = 1), result.settings.calculationAdjustments)
            assertTrue(result.noticePending)
        }

    /**
     * The Europe variant only ever existed from 0.0.13 on, so it already carries the takdir and nobody
     * can have adjustments tuned against the old times for it.
     */
    @Test
    fun `the Europe variant is left alone`() =
        runTest {
            val europe = CalculationMethod.TURKEY_EUROPE.parameters

            assertEquals(HighLatitudeRule.PROPORTIONAL_DEPRESSION, europe.highLatitudeRule)
            assertFalse(europe.interpolateDeclination)

            val result = migrate(CalculationSettings(parameters = europe))
            assertEquals(0, result.updateCount)
            assertFalse(result.noticePending)
        }
}
