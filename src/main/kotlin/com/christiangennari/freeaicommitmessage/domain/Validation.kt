package com.christiangennari.freeaicommitmessage.domain

object Validation {
    fun clampTemperature(temp: Double?): Double {
        if (temp == null || !temp.isFinite()) return 0.2
        return temp.coerceIn(0.0, 2.0)
    }

    fun clampMaxDiffCharacters(chars: Int): Int {
        return chars.coerceIn(1000, 100000)
    }

    fun clampTimeoutMs(ms: Long): Long {
        return ms.coerceIn(5000L, 600000L)
    }
}
