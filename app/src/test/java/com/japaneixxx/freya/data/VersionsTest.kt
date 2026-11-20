package com.japaneixxx.freya.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VersionsTest {
    @Test fun betaWhenTheLastNumberHasTwoOrMoreDigits() {
        listOf("1.8.41", "1.8.43", "1.8.45", "1.8.46", "1.8.51", "1.9.10").forEach { assertTrue("$it é beta", Versions.isBeta(it)) }
    }

    @Test fun stableWhenTheLastNumberHasOneDigit() {
        listOf("1.8.5", "1.8.4", "1.9.0", "1.9.1", "2.0.0").forEach { assertFalse("$it é estável", Versions.isBeta(it)) }
    }

    @Test fun suffixAndMalformedVersionsAreHandled() {
        assertTrue(Versions.isBeta("1.8.51-debug")); assertFalse(Versions.isBeta("1.9.0-debug"))
        assertFalse(Versions.isBeta("1.9")); assertFalse(Versions.isBeta("abc"))
    }

    @Test fun comparesNumericallyNotAsText() {
        assertTrue(Versions.isNewer("1.8.51", "1.8.5"))   // 51 > 5
        assertTrue(Versions.isNewer("1.9.0", "1.8.51"))   // minor sobe
        assertTrue(Versions.isNewer("1.10.0", "1.9.0"))
        assertFalse(Versions.isNewer("1.8.5", "1.8.51"))
        assertFalse(Versions.isNewer("1.9.0", "1.9.0"))
        assertTrue(Versions.isNewer("2.0", "1.9.9"))
    }
}
