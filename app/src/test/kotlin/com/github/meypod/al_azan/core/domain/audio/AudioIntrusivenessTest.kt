package com.github.meypod.al_azan.core.domain.audio

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioIntrusivenessTest {
    @Test
    fun loopingSoundIsAlwaysIntrusive() {
        assertTrue(isIntrusiveAudio(loop = true, durationMs = 1))
    }

    @Test
    fun shortNonLoopingSoundIsSoft() {
        assertFalse(isIntrusiveAudio(loop = false, durationMs = INTRUSIVE_MIN_DURATION_MS - 1))
    }

    @Test
    fun soundAtTheThresholdIsIntrusive() {
        assertTrue(isIntrusiveAudio(loop = false, durationMs = INTRUSIVE_MIN_DURATION_MS))
    }

    /**
     * Regression: a null duration means the length couldn't be read, not that the sound is short.
     * Treating it as soft downgraded the alarm to a plain notification — no full-screen screen and,
     * before the notification gained its own controls, no way to stop it at all.
     */
    @Test
    fun unreadableDurationIsIntrusive() {
        assertTrue(isIntrusiveAudio(loop = false, durationMs = null))
    }
}
