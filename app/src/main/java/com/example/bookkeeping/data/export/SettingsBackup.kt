package com.example.bookkeeping.data.export

import com.example.bookkeeping.data.prefs.*
import org.json.JSONObject

/** 使用可选附加字段扩展 v2 备份，旧读取端可忽略主题风格。 */
fun AppSettings.toBackupJson(): JSONObject = JSONObject()
    .put("currency", currency)
    .put("weekStartMonday", weekStartMonday)
    .put("themeMode", themeMode.value)
    .put("themeStyle", themeStyle.value)
    .put("background", background)
    .put("reading", reading.toBackupJson())

fun ReadingOptions.toBackupJson(): JSONObject = JSONObject()
    .put("size", size.value).put("weight", weight.value).put("font", font.value)
    .put("backgroundStrength", backgroundStrength.coerceIn(0, 100)).put("enhanced", enhanced)

internal fun JSONObject.toReadingOptions() = ReadingOptions(
    ReadingSize.from(optString("size")), ReadingWeight.from(optString("weight")), ReadingFont.from(optString("font")),
    optInt("backgroundStrength", 100), optBoolean("enhanced", false)
).normalized()
