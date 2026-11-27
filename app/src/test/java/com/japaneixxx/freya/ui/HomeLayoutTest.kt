package com.japaneixxx.freya.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeLayoutTest {
    @Test fun emptyRawGivesDefaultWithAllBlocksPresent() {
        val l = parseHomeLayout("")
        assertEquals(HOME_BLOCKS.map { it.id }, l.map { it.id })
        assertFalse(l.first { it.id == "cards" }.visible)   // bloco novo começa desligado
        assertTrue(l.first { it.id == "agenda" }.visible)
    }

    @Test fun roundTripKeepsOrderAndVisibility() {
        val custom = moveHomeBlock(parseHomeLayout(""), 0, 1).map { if (it.id == "top") it.copy(visible = false) else it }
        val back = parseHomeLayout(encodeHomeLayout(custom))
        assertEquals(custom, back)
        assertEquals("alerts", back[0].id)
    }

    @Test fun unknownDuplicateAndMissingIdsAreHandled() {
        val l = parseHomeLayout("top:0,lixo:1,top:1,agenda:1")
        assertEquals(HOME_BLOCKS.size, l.size)
        assertEquals(listOf("top", "agenda"), l.take(2).map { it.id })
        assertFalse(l[0].visible)
    }

    @Test fun moveStopsAtTheEnds() {
        val l = parseHomeLayout("")
        assertEquals(l, moveHomeBlock(l, 0, -1))
        assertEquals(l, moveHomeBlock(l, l.lastIndex, 1))
    }
}
