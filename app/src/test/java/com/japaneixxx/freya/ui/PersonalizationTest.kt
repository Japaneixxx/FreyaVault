package com.japaneixxx.freya.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PersonalizationTest {
    @Test fun tabOrderAlwaysHasAllFiveTabsOnce() {
        assertEquals(listOf(0, 1, 2, 3, 4), parseTabOrder(""))
        assertEquals(listOf(3, 0, 1, 2, 4), parseTabOrder("3,0,3,9,x"))
        assertEquals(listOf(4, 3, 2, 1, 0), parseTabOrder(encodeTabOrder(listOf(4, 3, 2, 1, 0))))
    }

    @Test fun moveTabSwapsNeighboursAndStopsAtTheEnds() {
        val o = listOf(0, 1, 2, 3, 4)
        assertEquals(listOf(1, 0, 2, 3, 4), moveTab(o, 0, 1))
        assertEquals(o, moveTab(o, 0, -1))
        assertEquals(o, moveTab(o, 4, 1))
    }

    @Test fun shortcutIdsAreKnownDistinctAndLimited() {
        assertEquals(listOf("add", "agenda"), parseShortcutIds(DEFAULT_SHORTCUTS))
        assertEquals(listOf("add"), parseShortcutIds("add,lixo,add"))
        assertEquals(MAX_SHORTCUTS, parseShortcutIds("add,agenda,cartoes,orcamentos,extrato,sync").size)
    }

    @Test fun linkTargetFallsBackToHome() {
        assertEquals("home", linkTarget("desconhecido").id)
        assertEquals(3, linkTarget("agenda").link.tab)
        assertEquals(PlanSection.AGENDA, linkTarget("agenda").link.planSection)
    }

    @Test fun hslRoundTripKeepsTheColor() {
        listOf(0xFF3F5F8A.toInt(), 0xFFB0587A.toInt(), 0xFF7A8B3A.toInt(), 0xFF2E8B95.toInt()).forEach { c ->
            val hsl = argbToHsl(c)
            val back = hslToArgb(hsl[0], hsl[1], hsl[2])
            for (shift in listOf(16, 8, 0)) {
                assertTrue(Math.abs(((c shr shift) and 0xFF) - ((back shr shift) and 0xFF)) <= 1)
            }
        }
    }

    @Test fun accentTextIsReadableForEveryHueInLightAndDark() {
        for (hue in 0 until 360 step 15) {
            for (sat in listOf(0.2f, 0.6f, 0.9f)) {
                val picked = hslToArgb(hue.toFloat(), sat, 0.5f)
                val light = accentColors(picked, false)
                assertTrue("claro $hue/$sat", contrastRatio(light.primary, light.onPrimary) >= 4.5)
                assertTrue("claro sec $hue/$sat", contrastRatio(light.secondary, light.onSecondary) >= 4.5)
                val dark = accentColors(picked, true)
                assertTrue("escuro $hue/$sat", contrastRatio(dark.primary, dark.onPrimary) >= 4.5)
                assertTrue("escuro cont $hue/$sat", contrastRatio(dark.primaryContainer, dark.onPrimaryContainer) >= 4.5)
            }
        }
    }

    @Test fun densityKeepsDefaultAtOne() {
        assertEquals(1f, densityFactor(1), 0f)
        assertTrue(densityFactor(0) < 1f && densityFactor(2) > 1f)
    }
}
