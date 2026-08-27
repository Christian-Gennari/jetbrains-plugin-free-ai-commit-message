package com.christiangennari.freeaicommitmessage.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ValidationTest {

    @Test
    fun `test clamp temperature`() {
        assertEquals(0.2, Validation.clampTemperature(null))
        assertEquals(0.2, Validation.clampTemperature(Double.NaN))
        assertEquals(0.0, Validation.clampTemperature(-0.5))
        assertEquals(2.0, Validation.clampTemperature(3.5))
        assertEquals(0.7, Validation.clampTemperature(0.7))
    }

    @Test
    fun `test clamp max diff characters`() {
        assertEquals(1000, Validation.clampMaxDiffCharacters(500))
        assertEquals(100000, Validation.clampMaxDiffCharacters(200000))
        assertEquals(12000, Validation.clampMaxDiffCharacters(12000))
    }

    @Test
    fun `test clamp timeout ms`() {
        assertEquals(5000L, Validation.clampTimeoutMs(1000L))
        assertEquals(600000L, Validation.clampTimeoutMs(9999999L))
        assertEquals(120000L, Validation.clampTimeoutMs(120000L))
    }
}
