package com.example.bookkeeping.data.prefs

/** 稳定标识独立于明暗模式；旧安装/未知主题自动回到经典。 */
enum class ThemeStyle(val value: String, val label: String, val description: String) {
    CLASSIC("classic", "经典", "暖白墨绿 · 圆角卡片"),
    SONG("song", "宋式书账", "纸墨留白 · 清秀宋体 · 账页细线");

    companion object {
        fun from(value: String?): ThemeStyle = entries.firstOrNull { it.value == value } ?: CLASSIC
    }
}
