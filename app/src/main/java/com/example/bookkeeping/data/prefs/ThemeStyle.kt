package com.example.bookkeeping.data.prefs

/** 稳定标识独立于明暗模式；旧安装/未知主题自动回到经典。 */
enum class ThemeStyle(val value: String, val label: String, val description: String, val recommendedMode: ThemeMode? = null) {
    CLASSIC("classic", "经典", "暖白墨绿 · 圆角卡片"),
    SONG("song", "绢本山水", "浅绢山水 · 棕墨书写 · 朱印点睛", ThemeMode.LIGHT),
    CELADON("celadon", "青玉雅集", "淡玉账页 · 圆润按钮 · 清雅留白", ThemeMode.LIGHT),
    INK_GOLD("ink_gold", "墨金藏书", "墨绿底色 · 暖白文字 · 淡金点睛", ThemeMode.DARK);

    companion object {
        fun from(value: String?): ThemeStyle = entries.firstOrNull { it.value == value } ?: CLASSIC
    }
}
