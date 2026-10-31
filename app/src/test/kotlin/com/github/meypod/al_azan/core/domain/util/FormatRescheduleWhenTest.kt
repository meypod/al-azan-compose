package com.github.meypod.al_azan.core.domain.util

import android.app.Application
import com.github.meypod.al_azan.core.domain.model.settings.Settings
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class FormatRescheduleWhenTest {

    private val settings = Settings(selectedLocale = "en-US", is24HourFormat = true)
    private val resources get() = RuntimeEnvironment.getApplication().resources

    // Saturday 31 October 2026, 10:00 UTC: the same date in nearly every time zone.
    private val now = Instant.parse("2026-10-31T10:00:00Z")
    private fun whenText(fire: Instant) = settings.formatRescheduleWhen(fire.toEpochMilliseconds(), now.toEpochMilliseconds(), resources)

    @Test
    fun `within the week names the weekday`() {
        assertTrue(whenText(now + 2.days).endsWith(", Monday"))
    }

    /** A weekday alone pointed at the wrong week for monthly and long-interval reminders. */
    @Test
    fun `past the week names the date`() {
        assertTrue(whenText(now + 9.days).endsWith(", Mon, Nov 9"))
    }
}
