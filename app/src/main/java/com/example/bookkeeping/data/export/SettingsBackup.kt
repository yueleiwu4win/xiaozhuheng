package com.example.bookkeeping.data.export

import com.example.bookkeeping.data.prefs.AppSettings
import org.json.JSONObject

/** 使用可选附加字段扩展 v2 备份，旧读取端可忽略主题风格。 */
fun AppSettings.toBackupJson(): JSONObject = JSONObject()
    .put("currency", currency)
    .put("weekStartMonday", weekStartMonday)
    .put("themeMode", themeMode.value)
    .put("themeStyle", themeStyle.value)
    .put("background", background)
