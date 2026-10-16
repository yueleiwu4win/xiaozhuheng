package com.example.bookkeeping.ui.theme

import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import com.example.bookkeeping.R
import com.example.bookkeeping.data.prefs.*

val LocalReadingOptions = staticCompositionLocalOf { ReadingOptions() }
internal val LocalReadingBaseDensity = staticCompositionLocalOf<Density?> { null }

// 自定义字重使用完整正文汉字区及合成字重，避免挑到短标题子集后动态分类缺字。
private val CompleteSerif = FontFamily(Font(R.font.ledger_text, FontWeight.Normal))

internal fun applyReadingStyle(base: TextStyle, options: ReadingOptions): TextStyle {
    val original = base.fontWeight ?: FontWeight.Normal
    val heading = original.weight >= 600
    val weight = when (options.weight) {
        ReadingWeight.THEME -> original
        ReadingWeight.NORMAL -> if (heading) FontWeight.SemiBold else FontWeight.Normal
        ReadingWeight.MEDIUM -> FontWeight(maxOf(original.weight, 500))
        ReadingWeight.BOLD -> FontWeight(maxOf(original.weight, 700))
    }
    val family = when (options.font) {
        ReadingFont.SYSTEM -> FontFamily.SansSerif
        ReadingFont.SERIF -> CompleteSerif
        ReadingFont.THEME -> if (options.weight != ReadingWeight.THEME && (base.fontFamily == LedgerTypeface || base.fontFamily == SongHeadings)) CompleteSerif else base.fontFamily
    }
    if (options.font == ReadingFont.THEME && options.weight == ReadingWeight.THEME) return base
    return base.copy(fontFamily = family, fontWeight = weight, fontSynthesis = FontSynthesis.All)
}

@Composable
fun readingTextStyle(base: TextStyle): TextStyle = applyReadingStyle(base, LocalReadingOptions.current)

internal fun Typography.withReading(options: ReadingOptions): Typography {
    fun TextStyle.read() = applyReadingStyle(this, options)
    return copy(displayLarge = displayLarge.read(), displayMedium = displayMedium.read(), displaySmall = displaySmall.read(),
        headlineLarge = headlineLarge.read(), headlineMedium = headlineMedium.read(), headlineSmall = headlineSmall.read(),
        titleLarge = titleLarge.read(), titleMedium = titleMedium.read(), titleSmall = titleSmall.read(),
        bodyLarge = bodyLarge.read(), bodyMedium = bodyMedium.read(), bodySmall = bodySmall.read(),
        labelLarge = labelLarge.read(), labelMedium = labelMedium.read(), labelSmall = labelSmall.read())
}

/** 给实际操作区域加主题纸色衬底，背景仍在导航及边缘保留。 */
@Composable
fun Modifier.readingSurface(): Modifier = if (LocalReadingOptions.current.enhanced)
    background(MaterialTheme.colorScheme.background.copy(alpha = 0.88f)) else this

@Composable
fun readingArtworkAlpha(): Float = LocalReadingOptions.current.let {
    it.backgroundStrength.coerceIn(0, 100) / 100f * if (it.enhanced) 0.3f else 1f
}
