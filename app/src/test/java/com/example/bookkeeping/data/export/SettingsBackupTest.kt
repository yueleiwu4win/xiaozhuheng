package com.example.bookkeeping.data.export

import com.example.bookkeeping.data.prefs.AppSettings
import com.example.bookkeeping.data.prefs.ThemeMode
import com.example.bookkeeping.data.prefs.ThemeStyle
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class SettingsBackupTest {
    @Test fun eachStyleAndBrightnessCombinationRoundTripsThroughActualBackupSettings() {
        for (style in ThemeStyle.entries) for (mode in ThemeMode.entries) {
            val original = AppSettings(themeStyle = style, themeMode = mode, background = "gradient:FFD9EDFF:FFF7FBFF")
            val categories = JSONArray().put(JSONObject().put("id", 1).put("name", "餐饮").put("type", "expense"))
            val root = JSONObject().put("app", "bookkeeping-android").put("formatVersion", 2)
                .put("categories", categories).put("bills", JSONArray()).put("settings", original.toBackupJson())
            val result = BackupParser.parse(root.toString()).settings!!
            assertEquals(style, ThemeStyle.from(result.themeStyle))
            assertEquals(mode, ThemeMode.from(result.themeMode))
            assertEquals(original.background, result.background)
            assertEquals(original.currency, result.currency)
        }
    }
}
