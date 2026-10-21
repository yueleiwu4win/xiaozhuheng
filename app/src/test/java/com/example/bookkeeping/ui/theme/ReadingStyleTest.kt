package com.example.bookkeeping.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.example.bookkeeping.data.prefs.*
import org.junit.Assert.*
import org.junit.Test

class ReadingStyleTest {
    @Test fun defaultsKeepExistingThemeStyles() {
        val style = TextStyle(fontFamily = LedgerTypeface, fontWeight = FontWeight.SemiBold)
        assertEquals(style, applyReadingStyle(style, ReadingOptions()))
    }

    @Test fun overridesAreIdempotentForRepeatedAmountStyleApplication() {
        for (weight in ReadingWeight.entries) for (font in ReadingFont.entries) {
            val options = ReadingOptions(weight = weight, font = font)
            val result = applyReadingStyle(TextStyle(fontFamily = LedgerTypeface), options)
            assertEquals(result, applyReadingStyle(result, options))
        }
    }

    @Test fun mediumKeepsHeadingsHeavierThanBodyAndSystemChoiceIsExplicit() {
        val options = ReadingOptions(weight = ReadingWeight.MEDIUM, font = ReadingFont.SYSTEM)
        val body = applyReadingStyle(TextStyle(fontWeight = FontWeight.Normal), options)
        val heading = applyReadingStyle(TextStyle(fontWeight = FontWeight.SemiBold), options)
        assertEquals(FontWeight.Medium, body.fontWeight)
        assertTrue(heading.fontWeight!!.weight > body.fontWeight!!.weight)
        assertEquals(FontFamily.SansSerif, body.fontFamily)
    }
}
