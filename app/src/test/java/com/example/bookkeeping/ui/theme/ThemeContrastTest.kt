package com.example.bookkeeping.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import com.example.bookkeeping.data.prefs.ThemeStyle
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/** 字体缩小、选中或切换明暗后，关键文字仍应能清楚辨认。 */
class ThemeContrastTest {
    @Test fun newThemesHaveReadableTextInBothBrightnessModes() {
        for (style in listOf(ThemeStyle.SONG, ThemeStyle.CELADON, ThemeStyle.INK_GOLD)) {
            val definition = ThemeRegistry.resolve(style)
            verifyTextPairs("${style.value}/light", definition.light)
            verifyTextPairs("${style.value}/dark", definition.dark)
        }
    }

    private fun verifyTextPairs(name: String, scheme: ColorScheme) {
        val pairs = listOf(
            "正文" to (scheme.onBackground to scheme.background),
            "面板正文" to (scheme.onSurface to scheme.surface),
            "辅助文字" to (scheme.onSurfaceVariant to scheme.background),
            "面板辅助文字" to (scheme.onSurfaceVariant to scheme.surface),
            "主操作文字" to (scheme.onPrimary to scheme.primary),
            "选中文字" to (scheme.onPrimaryContainer to scheme.primaryContainer),
            "概览辅助文字" to (scheme.onSurfaceVariant to scheme.primaryContainer),
            "次操作文字" to (scheme.onSecondaryContainer to scheme.secondaryContainer),
            "品牌操作" to (scheme.primary to scheme.background)
        )
        for ((label, colors) in pairs) {
            val ratio = contrast(colors.first, colors.second)
            assertTrue("$name $label contrast=$ratio, expected >=4.5", ratio >= 4.5)
        }
    }

    private fun contrast(foreground: Color, background: Color): Double {
        fun luminance(color: Color): Double {
            fun linear(value: Float): Double = if (value <= 0.04045f) value / 12.92
                else ((value + 0.055) / 1.055).pow(2.4)
            return 0.2126 * linear(color.red) + 0.7152 * linear(color.green) + 0.0722 * linear(color.blue)
        }
        val first = luminance(foreground)
        val second = luminance(background)
        return (max(first, second) + 0.05) / (min(first, second) + 0.05)
    }
}
