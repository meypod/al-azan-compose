package com.github.meypod.al_azan.core.domain.util

import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import org.junit.Test

class StringUtilsTest {

    @Test
    fun `randomString returns string of correct length`() {
        assertEquals(10, randomString(10).length)
        assertEquals(1, randomString(1).length)
        assertEquals(0, randomString(0).length)
    }

    @Test
    fun `randomString only contains alphanumeric characters`() {
        val result = randomString(100)
        assertTrue(result.all { it in charPool })
    }

    @Test
    fun `randomString produces different results on repeated calls`() {
        val results = (1..10).map { randomString(16) }.toSet()
        assertTrue(results.size > 1)
    }
}
