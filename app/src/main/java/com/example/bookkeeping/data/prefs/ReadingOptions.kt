package com.example.bookkeeping.data.prefs

enum class ReadingSize(val value: String, val label: String, val scale: Float) {
    SYSTEM("system", "跟随系统", 1f), LARGE("large", "较大 · 115%", 1.15f),
    EXTRA_LARGE("extra_large", "特大 · 130%", 1.3f), LARGEST("largest", "超大 · 150%", 1.5f);
    companion object { fun from(value: String?) = entries.firstOrNull { it.value == value } ?: SYSTEM }
}

enum class ReadingWeight(val value: String, val label: String) {
    THEME("theme", "跟随主题"), NORMAL("normal", "常规"), MEDIUM("medium", "适中"), BOLD("bold", "加粗");
    companion object { fun from(value: String?) = entries.firstOrNull { it.value == value } ?: THEME }
}

enum class ReadingFont(val value: String, val label: String) {
    THEME("theme", "跟随主题"), SYSTEM("system", "系统字体"), SERIF("serif", "宋体");
    companion object { fun from(value: String?) = entries.firstOrNull { it.value == value } ?: THEME }
}

/** 与界面主题独立保存；背景强度100保留旧版观感，0只保留主题底色。 */
data class ReadingOptions(
    val size: ReadingSize = ReadingSize.SYSTEM,
    val weight: ReadingWeight = ReadingWeight.THEME,
    val font: ReadingFont = ReadingFont.THEME,
    val backgroundStrength: Int = 100,
    val enhanced: Boolean = false
) {
    fun normalized() = copy(backgroundStrength = backgroundStrength.coerceIn(0, 100))
    companion object {
        val Clear = ReadingOptions(ReadingSize.LARGE, ReadingWeight.MEDIUM, ReadingFont.SYSTEM, 45, true)
    }
}
