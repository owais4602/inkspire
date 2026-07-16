package dev.stupifranc.inkspire.model

import org.junit.Assert.assertEquals
import org.junit.Test

class BrushFamilyChoiceTest {

    @Test
    fun testParseBrushFamilyChoice() {
        // every current enum name round-trips
        BrushFamilyChoice.values().forEach {
            assertEquals(it, parseBrushFamilyChoice(it.name))
        }

        // PRESSURE_PEN -> PEN
        assertEquals(BrushFamilyChoice.PEN, parseBrushFamilyChoice("PRESSURE_PEN"))

        // GARBAGE/"" -> PEN, no throw
        assertEquals(BrushFamilyChoice.PEN, parseBrushFamilyChoice("GARBAGE"))
        assertEquals(BrushFamilyChoice.PEN, parseBrushFamilyChoice(""))
    }
}
