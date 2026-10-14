package com.example.bookkeeping.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography as MaterialTypography
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bookkeeping.R
import com.example.bookkeeping.data.prefs.ThemeStyle

/** 页面只消费组件规则；新增主题在注册表中定义，不在每页判断主题名称。 */
@Immutable
data class ThemeTokens(
    val sectionCorner: Dp = 20.dp,
    val overviewCorner: Dp = 24.dp,
    val rowCorner: Dp = 16.dp,
    val categoryCorner: Dp = 16.dp,
    val keyCorner: Dp = 12.dp,
    val actionCorner: Dp = 28.dp,
    val chipCorner: Dp = 8.dp,
    val metricCorner: Dp = 18.dp,
    val pickerCorner: Dp = 10.dp,
    val rankCorner: Dp = 4.dp,
    val groupedSections: Boolean = true,
    val ledgerRows: Boolean = false,
    val outlinedIcons: Boolean = false,
    val mutedCategoryIcons: Boolean = false,
    val ornament: Int? = null
)

data class ThemeDefinition(
    val light: ColorScheme,
    val dark: ColorScheme,
    val typography: MaterialTypography,
    val shapes: Shapes,
    val tokens: ThemeTokens
)

private val SongHeadings = FontFamily(Font(R.font.song_headings, FontWeight.SemiBold))
private val SongTypography = Typography.copy(
    titleLarge = Typography.titleLarge.copy(fontFamily = SongHeadings, letterSpacing = 1.sp),
    titleMedium = Typography.titleMedium.copy(fontFamily = SongHeadings),
    headlineMedium = Typography.headlineMedium.copy(fontFamily = SongHeadings),
    labelSmall = Typography.labelSmall.copy(fontSize = 14.sp, lineHeight = 20.sp)
)

object ThemeRegistry {
    private val classic = ThemeDefinition(
        ClassicLightColors, ClassicDarkColors, Typography, Shapes(), ThemeTokens()
    )
    private val song = ThemeDefinition(
        light = ClassicLightColors.copy(
            primary = Color(0xFF214F43), secondary = Color(0xFF826B47),
            secondaryContainer = Color(0xFFE5EBE3), onSecondaryContainer = Color(0xFF214F43),
            outlineVariant = Color(0xFFDDDCD1)
        ),
        dark = ClassicDarkColors.copy(
            primary = Color(0xFFD6BE8E), onPrimary = Color(0xFF142B25),
            primaryContainer = Color(0xFF294C40), onPrimaryContainer = Color(0xFFF4F0E6),
            secondary = Color(0xFFBFD1C3), secondaryContainer = Color(0xFF294C40),
            onSecondaryContainer = Color(0xFFF4F0E6), background = Color(0xFF142B25),
            surface = Color(0xFF1B342C), surfaceVariant = Color(0xFF294239),
            surfaceContainer = Color(0xFF1B342C), surfaceContainerLow = Color(0xFF142B25),
            surfaceContainerLowest = Color(0xFF10231D), surfaceContainerHigh = Color(0xFF294239),
            surfaceContainerHighest = Color(0xFF294239), onBackground = Color(0xFFF4F0E6),
            onSurface = Color(0xFFF4F0E6), onSurfaceVariant = Color(0xFFBCC9BF),
            outline = Color(0xFF899E90), outlineVariant = Color(0xFF3D564B)
        ),
        typography = SongTypography,
        shapes = Shapes(
            extraSmall = RoundedCornerShape(4.dp), small = RoundedCornerShape(6.dp),
            medium = RoundedCornerShape(8.dp), large = RoundedCornerShape(12.dp),
            extraLarge = RoundedCornerShape(16.dp)
        ),
        tokens = ThemeTokens(
            sectionCorner = 0.dp, overviewCorner = 0.dp, rowCorner = 0.dp,
            categoryCorner = 10.dp, keyCorner = 6.dp, actionCorner = 8.dp,
            chipCorner = 6.dp, metricCorner = 8.dp, groupedSections = false,
            ledgerRows = true, outlinedIcons = true, mutedCategoryIcons = true,
            ornament = R.drawable.guofeng_bamboo
        )
    )

    fun resolve(style: ThemeStyle): ThemeDefinition = when (style) {
        ThemeStyle.CLASSIC -> classic
        ThemeStyle.SONG -> song
    }
}

val LocalThemeTokens = staticCompositionLocalOf { ThemeTokens() }
