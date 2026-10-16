package com.example.bookkeeping.data.export

import com.example.bookkeeping.data.prefs.*
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class ReadingSettingsTest {
    private fun backup(settings: JSONObject) = JSONObject().put("app", "bookkeeping-android").put("formatVersion", 2)
        .put("categories", JSONArray().put(JSONObject().put("id", 1).put("name", "餐饮").put("type", "expense"))).put("bills", JSONArray()).put("settings", settings)

    @Test fun readingChoicesSurviveActualBackupParsing() {
        for (size in ReadingSize.entries) for (weight in ReadingWeight.entries) for (font in ReadingFont.entries) {
            val reading = ReadingOptions(size, weight, font, 35, true)
            val settings = AppSettings(themeStyle = ThemeStyle.INK_GOLD, background = BACKGROUND_SPEC_ABACUS, reading = reading)
            val result = BackupParser.parse(backup(settings.toBackupJson()).toString()).settings!!
            assertEquals(reading, result.reading)
            assertEquals("ink_gold", result.themeStyle)
            assertEquals(BACKGROUND_SPEC_ABACUS, result.background)
        }
    }

    @Test fun oldBackupLeavesExistingReadingPreferencesUntouched() {
        assertNull(BackupParser.parse(backup(JSONObject().put("themeMode", "light")).toString()).settings?.reading)
    }

    @Test fun unknownOptionsFallBackAndStrengthIsClamped() {
        val invalid = JSONObject().put("size", "future").put("weight", "future").put("font", "future").put("backgroundStrength", 500)
        assertEquals(ReadingOptions(), invalid.toReadingOptions())
        invalid.put("backgroundStrength", -25)
        assertEquals(0, invalid.toReadingOptions().backgroundStrength)
    }

    @Test fun malformedFieldsDoNotProduceUnsafeScaling() {
        val invalid = JSONObject().put("size", 100).put("backgroundStrength", "bad").put("enhanced", "bad")
        assertEquals(ReadingOptions(), invalid.toReadingOptions())
    }

    @Test fun clearPresetImprovesReadingWithoutDependingOnThemeOrImage() {
        assertEquals(ReadingFont.SYSTEM, ReadingOptions.Clear.font)
        assertEquals(ReadingWeight.MEDIUM, ReadingOptions.Clear.weight)
        assertTrue(ReadingOptions.Clear.size.scale > 1f)
        assertTrue(ReadingOptions.Clear.enhanced)
        assertTrue(ReadingOptions.Clear.backgroundStrength < 100)
    }
}
