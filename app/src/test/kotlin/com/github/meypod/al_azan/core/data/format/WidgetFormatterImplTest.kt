package com.github.meypod.al_azan.core.data.format

import android.app.Application
import com.github.meypod.al_azan.core.domain.model.settings.NumberingSystem
import com.github.meypod.al_azan.core.domain.model.widget.HeaderDateFormat
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.time.Instant

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class WidgetFormatterImplTest {

    private val formatter = WidgetFormatterImpl()

    // Midday UTC, so the date is the same in any test machine's time zone.
    private val instant = Instant.parse("2026-08-17T12:00:00Z")

    private fun format(
        format: HeaderDateFormat,
        withDayName: Boolean,
    ) = formatter.formatDate(instant, "en-US", "gregorian", NumberingSystem.Default, withDayName, format)

    @Test
    fun `full spells out the month and weekday`() {
        assertEquals("August 17, 2026", format(HeaderDateFormat.Full, withDayName = false))
        assertEquals("Monday, August 17, 2026", format(HeaderDateFormat.Full, withDayName = true))
    }

    @Test
    fun `abbreviated shortens the month and weekday`() {
        assertEquals("Aug 17, 2026", format(HeaderDateFormat.Abbreviated, withDayName = false))
        assertEquals("Mon, Aug 17, 2026", format(HeaderDateFormat.Abbreviated, withDayName = true))
    }

    @Test
    fun `numeric writes the date in digits with a short weekday`() {
        assertEquals("8/17/2026", format(HeaderDateFormat.Numeric, withDayName = false))
        assertEquals("Mon, 8/17/2026", format(HeaderDateFormat.Numeric, withDayName = true))
    }
}
