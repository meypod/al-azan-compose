package com.github.meypod.al_azan.core.data.model

import com.github.meypod.al_azan.core.data.model.old.OldAdhanAudioEntry
import com.github.meypod.al_azan.core.data.model.old.OldAudioEntry
import com.github.meypod.al_azan.core.data.model.old.OldExportedSettings
import com.github.meypod.al_azan.core.data.model.old.OldPrayerAlarmSettings
import com.github.meypod.al_azan.core.data.model.old.OldRoundingMethod
import com.github.meypod.al_azan.core.data.model.old.OldThemeColors
import com.github.meypod.al_azan.core.data.model.old.OldVibrationMode
import com.github.meypod.al_azan.core.domain.model.adhan.AdhanKey
import com.github.meypod.al_azan.core.domain.model.adhan.Prayer
import com.github.meypod.al_azan.core.presentation.model.WidgetCityNamePos
import io.github.meypod.adhan_kotlin.MidnightMethod
import io.github.meypod.adhan_kotlin.model.Rounding
import junit.framework.TestCase.assertEquals
import kotlinx.serialization.json.Json
import org.junit.Assert.assertTrue
import org.junit.Test

class OldExportedSettingsSerializationTest {
    @Test
    fun parsesOldExportedSettingsJson() {
        val jsonUrl =
            javaClass.classLoader?.getResource("al_azan_settings.json")
                ?: throw IllegalStateException("Test resource not found: al_azan_settings.json")
        val json = jsonUrl.readText()

        val format = Json { ignoreUnknownKeys = true }
        val parsed = format.decodeFromString(OldExportedSettings.serializer(), json)

        // Settings storage
        assertEquals("fa", parsed.settingsStorage.state.selectedLocale)
        assertEquals(OldThemeColors.Default, parsed.settingsStorage.state.themeColor)
        assertEquals(true, parsed.settingsStorage.state.is24HourFormat)
        assertEquals(null, parsed.settingsStorage.state.numberingSystem)
        assertEquals(false, parsed.settingsStorage.state.highlightCurrentPrayer)
        assertEquals(null, parsed.settingsStorage.state.selectedLocaleForArabicCalendar)
        assertEquals("persian", parsed.settingsStorage.state.selectedSecondaryCalendar)
        assertEquals(true, parsed.settingsStorage.state.appIntroDone)
        assertEquals(4, parsed.settingsStorage.state.savedAdhanAudioEntries.size)
        // saved adhan entries ids and internals
        assertEquals(
            "masjid_an_nabawi",
            parsed.settingsStorage.state.savedAdhanAudioEntries[0].let {
                (it as OldAdhanAudioEntry.OldResourceAdhanAudioEntry).id
            },
        )
        assertEquals(
            true,
            (parsed.settingsStorage.state.savedAdhanAudioEntries[0] as OldAdhanAudioEntry.OldResourceAdhanAudioEntry)
                .internal,
        )
        assertEquals(
            "abdul_basit_abdus_samad",
            (parsed.settingsStorage.state.savedAdhanAudioEntries[1] as OldAdhanAudioEntry.OldResourceAdhanAudioEntry)
                .id,
        )
        assertEquals(
            "https://github.com/meypod/audio_files/raw/main/adhan/abdul_basit_abdus_samad.mp3",
            (parsed.settingsStorage.state.savedAdhanAudioEntries[1] as OldAdhanAudioEntry.OldResourceAdhanAudioEntry)
                .remoteUri,
        )
        val defaultAdhan = parsed.settingsStorage.state.selectedAdhanEntries[AdhanKey.Default]
        assertTrue(defaultAdhan is OldAdhanAudioEntry.OldResourceAdhanAudioEntry)
        assertEquals(
            "masjid_an_nabawi",
            (defaultAdhan as OldAdhanAudioEntry.OldResourceAdhanAudioEntry).id,
        )
        assertEquals(false, parsed.settingsStorage.state.isPlayingAudio)
        assertEquals(true, parsed.settingsStorage.state.devMode)
        assertEquals(true, parsed.settingsStorage.state.qiblaFinderUnderstood)
        assertEquals(true, parsed.settingsStorage.state.qiblaFinderOrientationLocked)
        assertEquals("1444 هـ", parsed.settingsStorage.state.ramadanRemindedYear)
        assertEquals(false, parsed.settingsStorage.state.ramadanReminderDontShow)

        // Calc settings
        assertEquals(35.6628073, parsed.calcSettingsStorage.state.locationLat)
        assertEquals(51.0620525, parsed.calcSettingsStorage.state.locationLong)
        assertEquals(
            35.69439,
            parsed.calcSettingsStorage.state.location
                ?.lat,
        )
        assertEquals("Jafari", parsed.calcSettingsStorage.state.calculationMethodKey)
        assertEquals("shafi", parsed.calcSettingsStorage.state.asrCalculation)
        assertEquals("general", parsed.calcSettingsStorage.state.shafaq)
        assertEquals("Unresolved", parsed.calcSettingsStorage.state.polarResolution)
        assertEquals(OldRoundingMethod.NEAREST, parsed.calcSettingsStorage.state.roundingMethod)
        assertEquals(-3, parsed.calcSettingsStorage.state.fajrAdjustment)
        assertEquals(0, parsed.calcSettingsStorage.state.sunriseAdjustment)

        // Alarm settings
        val alarmState = parsed.alarmSettingsStorage.state
        assertEquals(OldVibrationMode.Once, alarmState.vibrationMode)
        // FAJR
        assertTrue(alarmState.fajrSound is OldPrayerAlarmSettings.Bool)
        assertEquals(true, (alarmState.fajrSound as OldPrayerAlarmSettings.Bool).value)
        assertTrue(alarmState.fajrNotify is OldPrayerAlarmSettings.Bool)
        assertEquals(true, (alarmState.fajrNotify as OldPrayerAlarmSettings.Bool).value)
        // SUNRISE
        assertTrue(alarmState.sunriseSound is OldPrayerAlarmSettings.Bool)
        assertEquals(false, (alarmState.sunriseSound as OldPrayerAlarmSettings.Bool).value)
        assertTrue(alarmState.sunriseNotify is OldPrayerAlarmSettings.Bool)
        assertEquals(false, (alarmState.sunriseNotify as OldPrayerAlarmSettings.Bool).value)
        // DHUHR
        assertTrue(alarmState.dhuhrSound is OldPrayerAlarmSettings.Bool)
        assertEquals(true, (alarmState.dhuhrSound as OldPrayerAlarmSettings.Bool).value)
        assertTrue(alarmState.dhuhrNotify is OldPrayerAlarmSettings.Bool)
        assertEquals(true, (alarmState.dhuhrNotify as OldPrayerAlarmSettings.Bool).value)
        // ASR
        assertTrue(alarmState.asrSound is OldPrayerAlarmSettings.Bool)
        assertEquals(false, (alarmState.asrSound as OldPrayerAlarmSettings.Bool).value)
        assertTrue(alarmState.asrNotify is OldPrayerAlarmSettings.Bool)
        assertEquals(false, (alarmState.asrNotify as OldPrayerAlarmSettings.Bool).value)
        // MAGHRIB
        assertTrue(alarmState.maghribSound is OldPrayerAlarmSettings.Bool)
        assertEquals(true, (alarmState.maghribSound as OldPrayerAlarmSettings.Bool).value)
        assertTrue(alarmState.maghribNotify is OldPrayerAlarmSettings.Bool)
        assertEquals(true, (alarmState.maghribNotify as OldPrayerAlarmSettings.Bool).value)
        // ISHA
        assertTrue(alarmState.ishaSound is OldPrayerAlarmSettings.Bool)
        assertEquals(false, (alarmState.ishaSound as OldPrayerAlarmSettings.Bool).value)
        assertTrue(alarmState.ishaNotify is OldPrayerAlarmSettings.Bool)
        assertEquals(false, (alarmState.ishaNotify as OldPrayerAlarmSettings.Bool).value)
        // Counter storage
        assertEquals(7, parsed.counterStoreStorage.state.counters.size)
        val counters = parsed.counterStoreStorage.state.counters
        assertEquals("fajr", counters[0].id)
        assertEquals(4, counters[0].count)
        assertEquals(5, counters[0].lastCount)
        assertEquals("dhuhr", counters[1].id)
        assertEquals(2, counters[1].count)
        assertEquals("asr", counters[2].id)
        assertEquals(2, counters[2].count)
        assertEquals("maghrib", counters[3].id)
        assertEquals(2, counters[3].count)
        assertEquals("isha", counters[4].id)
        assertEquals(2, counters[4].count)
        assertEquals("fast", counters[5].id)
        assertEquals(0, counters[5].count)
        assertEquals("counter_1764365125916", counters[6].id)
        assertEquals("ذکر", counters[6].label)
        assertEquals(100, counters[6].lastCount)

        // Reminder storage
        assertEquals(1, parsed.reminderSettingsStorage.state.reminders.size)
        val reminder =
            parsed.reminderSettingsStorage.state.reminders
                .first()
        assertEquals("reminder_1745932214236", reminder.id)
        assertEquals(true, reminder.enabled)
        assertEquals(600000L, reminder.duration)
        assertEquals(1, reminder.durationModifier)
        assertEquals(Prayer.Maghrib, reminder.prayer)
        assertEquals("شکر", reminder.label)
        val reminderSound = reminder.sound
        assertTrue(reminderSound is OldAudioEntry.OldDefaultAudioEntry)
        if (reminderSound is OldAudioEntry.OldDefaultAudioEntry) {
            assertEquals("default", reminderSound.id)
        }

        // Favorite locations
        assertEquals(2, parsed.favoriteLocationsStorage.state.locations.size)
        val favs = parsed.favoriteLocationsStorage.state.locations
        assertEquals("favorite_city_1694315809709", favs[0].id)
        assertEquals("Tehran", favs[0].label)
        assertEquals(35.69439, favs[0].lat)
        assertEquals(51.42151, favs[0].long)
        assertEquals("IR", favs[0].country?.code)
        assertEquals("tehran", favs[0].city?.selectedName)

        // Delivered alarm timestamps
        assertEquals(
            false,
            parsed.settingsStorage.state.deliveredAlarmTimestamps
                .isEmpty(),
        )
        assertEquals(
            1765204380000L,
            parsed.settingsStorage.state.deliveredAlarmTimestamps["widget-update-notification"],
        )
    }

    @Test
    fun parsesOldExportedSettingsJson_v2() {
        val jsonUrl =
            javaClass.classLoader?.getResource("al_azan_settings_2.json")
                ?: throw IllegalStateException("Test resource not found: al_azan_settings_2.json")
        val json = jsonUrl.readText()

        val format = Json { ignoreUnknownKeys = true }
        val parsed = format.decodeFromString(OldExportedSettings.serializer(), json)
        // Settings storage — assert all visible fields
        val s = parsed.settingsStorage.state
        assertEquals("en", s.selectedLocale)
        assertEquals(OldThemeColors.Dark, s.themeColor)
        assertEquals("islamic-civil", s.selectedArabicCalendar)
        assertEquals(null, s.selectedLocaleForArabicCalendar)
        assertEquals("persian", s.selectedSecondaryCalendar)
        assertEquals(false, s.appInitialConfigDone)
        assertEquals(true, s.appIntroDone)
        assertEquals(4, s.savedAdhanAudioEntries.size)
        assertEquals(0, s.savedUserAudioEntries.size)
        // selected adhan entries: default, dhuhr (external), isha (default notification)
        val selDefault = s.selectedAdhanEntries[AdhanKey.Default]
        assertTrue(selDefault is OldAdhanAudioEntry.OldResourceAdhanAudioEntry)
        val selDhuhr = s.selectedAdhanEntries[AdhanKey.Dhuhr]
        assertTrue(selDhuhr is OldAdhanAudioEntry.OldExternalAdhanAudioEntry)
        assertEquals("11", (selDhuhr as OldAdhanAudioEntry.OldExternalAdhanAudioEntry).id)
        val selIsha = s.selectedAdhanEntries[AdhanKey.Isha]
        assertTrue(
            selIsha is OldAdhanAudioEntry.OldExternalAdhanAudioEntry ||
                selIsha is OldAdhanAudioEntry.OldResourceAdhanAudioEntry,
        )
        assertEquals(listOf(Prayer.Midnight, Prayer.Tahajjud), s.hiddenPrayers)
        assertEquals(70, s.adhanVolume)
        assertEquals(
            listOf(Prayer.Tahajjud, Prayer.Isha, Prayer.Asr, Prayer.Sunset),
            s.hiddenWidgetPrayers,
        )
        assertEquals(false, s.showWidget)
        assertEquals(true, s.showWidgetCountdown)
        assertEquals(true, s.adaptiveWidgets)
        assertEquals(WidgetCityNamePos.TopStart, s.widgetCityNamePos)
        assertEquals(true, s.is24HourFormat)
        assertEquals(null, s.numberingSystem)
        assertEquals(false, s.highlightCurrentPrayer)
        assertEquals(
            "6dc142c11b83c6bd4b84f826545993b55a97e8d711d6edac59362ae180108ba3",
            s.calcSettingsHash,
        )
        assertEquals(
            "637e9c131131658a0fbca3542a962e88534643b8c37c940a76a62ce9f5eba28d",
            s.alarmSettingsHash,
        )
        assertEquals(
            "97d6403543fb9f445c7a2fa11a4c4cd7d09f616d9f0da1eb654352757d0af23d",
            s.reminderSettingsHash,
        )
        assertEquals(emptyMap<String, Long?>(), s.deliveredAlarmTimestamps)
        assertEquals(false, s.isPlayingAudio)
        assertEquals(false, s.dontAskPermissionNotifications)
        assertEquals(false, s.dontAskPermissionAlarm)
        assertEquals(false, s.dontAskPermissionPhoneState)
        assertEquals(true, s.devMode)
        assertEquals(true, s.qiblaFinderUnderstood)
        assertEquals(true, s.qiblaFinderOrientationLocked)
        assertEquals(false, s.volumeButtonStopsAdhan)
        assertEquals(false, s.preferExternalAudioDevice)
        assertEquals(true, s.bypassDnd)
        assertEquals(false, s.counterHistoryVisible)
        assertEquals("1444 هـ", s.ramadanRemindedYear)
        assertEquals(false, s.ramadanReminderDontShow)
        assertEquals(true, s.advancedCustomAdhan)
        assertEquals(false, s.useDifferentAlarmType)
        assertEquals(false, s.hijriMonthlyView)

        // Counter storage
        val counters = parsed.counterStoreStorage.state.counters
        assertEquals(7, counters.size)
        assertEquals("fajr", counters[0].id)
        assertEquals(4, counters[0].count)
        assertEquals(5, counters[0].lastCount)
        assertEquals("dhuhr", counters[1].id)
        assertEquals(2, counters[1].count)
        assertEquals("asr", counters[2].id)
        assertEquals(2, counters[2].count)
        assertEquals("maghrib", counters[3].id)
        assertEquals(2, counters[3].count)
        assertEquals("isha", counters[4].id)
        assertEquals(2, counters[4].count)
        assertEquals("fast", counters[5].id)
        assertEquals(0, counters[5].count)
        assertEquals("counter_1764365125916", counters[6].id)
        assertEquals("ذکر", counters[6].label)
        assertEquals(100, counters[6].lastCount)

        // Reminder storage — two reminders
        val reminders = parsed.reminderSettingsStorage.state.reminders
        assertEquals(2, reminders.size)
        val r0 = reminders[0]
        assertEquals("reminder_1766664888207", r0.id)
        assertEquals(true, r0.enabled)
        assertEquals(900000L, r0.duration)
        assertEquals(-1, r0.durationModifier)
        assertEquals(Prayer.Fajr, r0.prayer)
        assertEquals(true, r0.once)
        // days map -> ByWeekDay
        assertTrue(r0.days is OldPrayerAlarmSettings.ByWeekDay)
        val daysMap0 = (r0.days as OldPrayerAlarmSettings.ByWeekDay).days
        assertEquals(true, daysMap0[0])
        assertEquals(true, daysMap0[1])
        assertEquals(true, daysMap0[5])
        assertEquals(true, daysMap0[6])
        val r0sound = r0.sound as OldAudioEntry.OldExternalAudioEntry
        assertEquals("70", r0sound.id)
        assertTrue(r0sound.filepath.startsWith("content://"))
        assertEquals("Cricket", r0sound.label)

        val r1 = reminders[1]
        assertEquals("reminder_1745932214236", r1.id)
        assertEquals(false, r1.enabled)
        assertEquals(600000L, r1.duration)
        assertEquals(1, r1.durationModifier)
        assertEquals(Prayer.Maghrib, r1.prayer)
        assertEquals("شکر", r1.label)
        assertTrue(
            r1.days == null || r1.days is OldPrayerAlarmSettings.Bool ||
                r1.days is OldPrayerAlarmSettings.ByWeekDay,
        )
        assertTrue(r1.sound is OldAudioEntry.OldDefaultAudioEntry)

        // Alarm settings
        val a = parsed.alarmSettingsStorage.state
        assertEquals(false, a.showNextPrayerTime)
        assertEquals(false, a.dontNotifyUpcoming)
        assertEquals(60, a.preAlarmMinutesBefore)
        assertEquals(false, a.dontTurnOnScreen)
        assertEquals(OldVibrationMode.Continuous, a.vibrationMode)
        // fajr/dhuhr/maghrib booleans
        assertTrue(a.fajrSound is OldPrayerAlarmSettings.Bool && a.fajrSound.value)
        assertTrue(a.fajrNotify is OldPrayerAlarmSettings.Bool && a.fajrNotify.value)
        assertTrue(a.dhuhrSound is OldPrayerAlarmSettings.Bool && a.dhuhrSound.value)
        assertTrue(a.dhuhrNotify is OldPrayerAlarmSettings.Bool && a.dhuhrNotify.value)
        assertTrue(a.maghribSound is OldPrayerAlarmSettings.Bool && a.maghribSound.value)
        assertTrue(a.maghribNotify is OldPrayerAlarmSettings.Bool && a.maghribNotify.value)
        // isha sound/notify are ByWeekDay with key 3 -> true
        assertTrue(a.ishaSound is OldPrayerAlarmSettings.ByWeekDay)
        val ishaMap = (a.ishaSound as OldPrayerAlarmSettings.ByWeekDay).days
        assertEquals(true, ishaMap[3])
        assertTrue(a.ishaNotify is OldPrayerAlarmSettings.ByWeekDay)
        val ishaNotifyMap = (a.ishaNotify as OldPrayerAlarmSettings.ByWeekDay).days
        assertEquals(true, ishaNotifyMap[3])
        // tahajjud notify map
        assertTrue(a.tahajjudNotify is OldPrayerAlarmSettings.ByWeekDay)
        val tahaMap = (a.tahajjudNotify as OldPrayerAlarmSettings.ByWeekDay).days
        assertEquals(true, tahaMap[3])

        // Calc settings — mapped fields
        val c = parsed.calcSettingsStorage.state
        assertEquals(35.6628073, c.locationLat)
        assertEquals(51.0620525, c.locationLong)
        assertEquals(35.69439, c.location?.lat)
        assertEquals("Jafari", c.calculationMethodKey)
        assertEquals("hanafi", c.asrCalculation)
        assertEquals("ahmer", c.shafaq)
        assertEquals("AqrabBalad", c.polarResolution)
        assertEquals(MidnightMethod.SunsetToSunrise, c.midnightMethod)
        // rounding method "none" is unsupported -> becomes null
        assertEquals(OldRoundingMethod.NEAREST, c.roundingMethod)
        assertEquals(-3, c.fajrAdjustment)

        // Favorite locations mapping (2 entries)
        val favs = parsed.favoriteLocationsStorage.state.locations
        assertEquals(2, favs.size)
        assertEquals("favorite_city_1694315809709", favs[0].id)
        assertEquals("Tehran", favs[0].label)
        assertEquals(35.69439, favs[0].lat)
        assertEquals(51.42151, favs[0].long)
        assertEquals("IR", favs[0].country?.code)
        assertEquals("tehran", favs[0].city?.selectedName)
    }
}
