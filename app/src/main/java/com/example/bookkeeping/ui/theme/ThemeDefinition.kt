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
enum class OverviewStyle { CARD, LEDGER, JADE }
enum class SelectionStyle { CLASSIC, OUTLINED, PILL, BOOKMARK }
enum class HomeActionPlacement { FLOATING, DOCKED }
enum class HomeLayout { STANDARD, CELADON, INK_GOLD, SONG }

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
    val ornament: Int? = null,
    val overviewStyle: OverviewStyle = OverviewStyle.CARD,
    val filterStyle: SelectionStyle = SelectionStyle.CLASSIC,
    val navigationStyle: SelectionStyle = SelectionStyle.CLASSIC,
    val homeActionPlacement: HomeActionPlacement = HomeActionPlacement.FLOATING,
    val portraitBrand: Boolean = false,
    val abacusBrand: Boolean = false,
    val monochromeCategoryIcons: Boolean = false,
    val neutralHeadline: Boolean = false,
    val homeLayout: HomeLayout = HomeLayout.STANDARD
)

data class ThemeDefinition(
    val light: ColorScheme,
    val dark: ColorScheme,
    val typography: MaterialTypography,
    val shapes: Shapes,
    val tokens: ThemeTokens
)

private val SongHeadings = FontFamily(Font(R.font.song_headings, FontWeight.SemiBold))
val LedgerTypeface = FontFamily(
    Font(R.font.ledger_text, FontWeight.Normal),
    Font(R.font.song_headings, FontWeight.SemiBold)
)
private val SongTypography = Typography.copy(
    titleLarge = Typography.titleLarge.copy(fontFamily = SongHeadings, letterSpacing = 1.sp),
    titleMedium = Typography.titleMedium.copy(fontFamily = SongHeadings),
    headlineMedium = Typography.headlineMedium.copy(fontFamily = SongHeadings),
    labelSmall = Typography.labelSmall.copy(fontSize = 14.sp, lineHeight = 20.sp)
)

private val LedgerTypography = SongTypography.copy(
    bodyLarge = Typography.bodyLarge.copy(fontFamily = LedgerTypeface),
    bodyMedium = Typography.bodyMedium.copy(fontFamily = LedgerTypeface),
    bodySmall = Typography.bodySmall.copy(fontFamily = LedgerTypeface),
    labelLarge = Typography.labelLarge.copy(fontFamily = LedgerTypeface),
    labelMedium = Typography.labelMedium.copy(fontFamily = LedgerTypeface),
    labelSmall = Typography.labelSmall.copy(fontFamily = LedgerTypeface)
)

object ThemeRegistry {
    private val classic = ThemeDefinition(
        ClassicLightColors, ClassicDarkColors, Typography, Shapes(), ThemeTokens()
    )
    private val song = ThemeDefinition(
        light = ClassicLightColors.copy(
            primary = Color(0xFF70543D), onPrimary = Color(0xFFFFFAF1),
            primaryContainer = Color(0xFFE9DDC8), onPrimaryContainer = Color(0xFF44372A),
            secondary = Color(0xFF826547), secondaryContainer = Color(0xFFECE2D1),
            onSecondaryContainer = Color(0xFF44372A), background = Color(0xFFF6EFE2),
            surface = Color(0xFFF9F2E6), surfaceVariant = Color(0xFFECE2D1),
            surfaceContainer = Color(0xFFF6EEDF), surfaceContainerLow = Color(0xFFF6EFE2),
            surfaceContainerLowest = Color(0xFFFCF7EE), surfaceContainerHigh = Color(0xFFECE2D1),
            surfaceContainerHighest = Color(0xFFE6D9C4), onBackground = Color(0xFF302D28),
            onSurface = Color(0xFF302D28), onSurfaceVariant = Color(0xFF665D50),
            outline = Color(0xFF9B8972), outlineVariant = Color(0xFFD2C3AD),
            tertiary = Color(0xFF8D4937), tertiaryContainer = Color(0xFFF1DDD3),
            onTertiaryContainer = Color(0xFF5F3024)
        ),
        dark = ClassicDarkColors.copy(
            primary = Color(0xFFD6BA91), onPrimary = Color(0xFF30271D),
            primaryContainer = Color(0xFF493D2F), onPrimaryContainer = Color(0xFFF4EAD9),
            secondary = Color(0xFFD0BCA1), secondaryContainer = Color(0xFF493D2F),
            onSecondaryContainer = Color(0xFFF4EAD9), background = Color(0xFF24221D),
            surface = Color(0xFF2D2A23), surfaceVariant = Color(0xFF413B31),
            surfaceContainer = Color(0xFF2D2A23), surfaceContainerLow = Color(0xFF24221D),
            surfaceContainerLowest = Color(0xFF1F1D19), surfaceContainerHigh = Color(0xFF413B31),
            surfaceContainerHighest = Color(0xFF493F32), onBackground = Color(0xFFF1E8D8),
            onSurface = Color(0xFFF1E8D8), onSurfaceVariant = Color(0xFFD0C2AE),
            outline = Color(0xFFAA9780), outlineVariant = Color(0xFF665B49),
            tertiary = Color(0xFFE4AA92), tertiaryContainer = Color(0xFF59382E),
            onTertiaryContainer = Color(0xFFFFE2D6)
        ),
        typography = LedgerTypography,
        shapes = Shapes(
            extraSmall = RoundedCornerShape(4.dp), small = RoundedCornerShape(6.dp),
            medium = RoundedCornerShape(8.dp), large = RoundedCornerShape(12.dp),
            extraLarge = RoundedCornerShape(16.dp)
        ),
        tokens = ThemeTokens(
            sectionCorner = 0.dp, overviewCorner = 0.dp, rowCorner = 0.dp,
            categoryCorner = 5.dp, keyCorner = 5.dp, actionCorner = 5.dp,
            chipCorner = 6.dp, metricCorner = 8.dp, groupedSections = false,
            ledgerRows = true, outlinedIcons = true, mutedCategoryIcons = true,
            monochromeCategoryIcons = true, neutralHeadline = true,
            homeLayout = HomeLayout.SONG, homeActionPlacement = HomeActionPlacement.DOCKED,
            overviewStyle = OverviewStyle.LEDGER,
            filterStyle = SelectionStyle.BOOKMARK,
            navigationStyle = SelectionStyle.BOOKMARK
        )
    )

    private val celadon = ThemeDefinition(
        light = ClassicLightColors.copy(
            primary = Color(0xFF214F43), onPrimary = Color(0xFFFFFDF9),
            primaryContainer = Color(0xFFE1EBE2), onPrimaryContainer = Color(0xFF214F43),
            secondary = Color(0xFF49675B), secondaryContainer = Color(0xFFE8EEE6),
            onSecondaryContainer = Color(0xFF294D40), background = Color(0xFFF8F6F0),
            surface = Color(0xFFFFFDF9), surfaceVariant = Color(0xFFEBEFE7),
            surfaceContainer = Color(0xFFFFFDF9), surfaceContainerLow = Color(0xFFF8F6F0),
            surfaceContainerLowest = Color(0xFFFFFDF9), surfaceContainerHigh = Color(0xFFEBEFE7),
            surfaceContainerHighest = Color(0xFFE1EBE2), onSurface = Color(0xFF233D33),
            onBackground = Color(0xFF233D33), onSurfaceVariant = Color(0xFF5E6C61),
            outline = Color(0xFF89998C), outlineVariant = Color(0xFFD3DED3)
        ),
        dark = ClassicDarkColors.copy(
            primary = Color(0xFFB3D4BF), onPrimary = Color(0xFF173D30),
            primaryContainer = Color(0xFF2C4A3B), onPrimaryContainer = Color(0xFFE4EFE6),
            secondary = Color(0xFFC1D3C5), secondaryContainer = Color(0xFF304B3D),
            onSecondaryContainer = Color(0xFFE4EFE6), background = Color(0xFF16241D),
            surface = Color(0xFF203329), surfaceVariant = Color(0xFF2B4033),
            surfaceContainer = Color(0xFF203329), surfaceContainerLow = Color(0xFF16241D),
            surfaceContainerLowest = Color(0xFF122018), surfaceContainerHigh = Color(0xFF2B4033),
            surfaceContainerHighest = Color(0xFF304B3D), onSurface = Color(0xFFE4EFE6),
            onBackground = Color(0xFFE4EFE6), onSurfaceVariant = Color(0xFFBBCABD),
            outline = Color(0xFF8DA593), outlineVariant = Color(0xFF465B4B)
        ),
        typography = LedgerTypography,
        shapes = Shapes(
            extraSmall = RoundedCornerShape(8.dp), small = RoundedCornerShape(12.dp),
            medium = RoundedCornerShape(16.dp), large = RoundedCornerShape(20.dp),
            extraLarge = RoundedCornerShape(24.dp)
        ),
        tokens = ThemeTokens(
            sectionCorner = 20.dp, overviewCorner = 24.dp, rowCorner = 0.dp,
            categoryCorner = 18.dp, keyCorner = 14.dp, actionCorner = 28.dp,
            chipCorner = 24.dp, metricCorner = 16.dp, groupedSections = true,
            ledgerRows = true, outlinedIcons = true, mutedCategoryIcons = true,
            ornament = R.drawable.guofeng_bamboo, overviewStyle = OverviewStyle.JADE,
            filterStyle = SelectionStyle.PILL, navigationStyle = SelectionStyle.PILL,
            homeActionPlacement = HomeActionPlacement.DOCKED,
            portraitBrand = true, monochromeCategoryIcons = true, homeLayout = HomeLayout.CELADON
        )
    )

    private val inkGold = ThemeDefinition(
        light = ClassicLightColors.copy(
            primary = Color(0xFF74562B), onPrimary = Color(0xFFFFFDF7),
            primaryContainer = Color(0xFFECE2CA), onPrimaryContainer = Color(0xFF4D3B21),
            secondary = Color(0xFF3B5B4C), secondaryContainer = Color(0xFFE6EADF),
            onSecondaryContainer = Color(0xFF294B3B), background = Color(0xFFF5F0E4),
            surface = Color(0xFFFCF8EE), surfaceVariant = Color(0xFFECE7D8),
            surfaceContainer = Color(0xFFFCF8EE), surfaceContainerLow = Color(0xFFF5F0E4),
            surfaceContainerLowest = Color(0xFFFFFCF4), surfaceContainerHigh = Color(0xFFECE7D8),
            surfaceContainerHighest = Color(0xFFE7DEC9), onSurface = Color(0xFF233A2E),
            onBackground = Color(0xFF233A2E), onSurfaceVariant = Color(0xFF686552),
            outline = Color(0xFF95896D), outlineVariant = Color(0xFFD9D1BB)
        ),
        dark = song.dark.copy(
            primary = Color(0xFFE0C18A), onPrimary = Color(0xFF142B25),
            primaryContainer = Color(0xFF294B3B), onPrimaryContainer = Color(0xFFF4F0E6),
            secondary = Color(0xFFC7B68E), secondaryContainer = Color(0xFF294239),
            onSecondaryContainer = Color(0xFFF4F0E6), background = Color(0xFF0B2D24),
            surface = Color(0xFF1D352D), surfaceContainer = Color(0xFF1D352D),
            onSurfaceVariant = Color(0xFFE0E5DB),
            outlineVariant = Color(0x6695A496)
        ),
        typography = LedgerTypography,
        shapes = Shapes(
            extraSmall = RoundedCornerShape(4.dp), small = RoundedCornerShape(6.dp),
            medium = RoundedCornerShape(10.dp), large = RoundedCornerShape(12.dp),
            extraLarge = RoundedCornerShape(16.dp)
        ),
        tokens = ThemeTokens(
            sectionCorner = 0.dp, overviewCorner = 0.dp, rowCorner = 0.dp,
            categoryCorner = 10.dp, keyCorner = 8.dp, actionCorner = 12.dp,
            chipCorner = 4.dp, metricCorner = 10.dp, groupedSections = false,
            ledgerRows = true, outlinedIcons = true, mutedCategoryIcons = true,
            ornament = R.drawable.guofeng_bamboo, overviewStyle = OverviewStyle.LEDGER,
            filterStyle = SelectionStyle.BOOKMARK, navigationStyle = SelectionStyle.BOOKMARK,
            homeActionPlacement = HomeActionPlacement.DOCKED,
            abacusBrand = true, monochromeCategoryIcons = true, neutralHeadline = true, homeLayout = HomeLayout.INK_GOLD
        )
    )

    fun resolve(style: ThemeStyle): ThemeDefinition = when (style) {
        ThemeStyle.CLASSIC -> classic
        ThemeStyle.SONG -> song
        ThemeStyle.CELADON -> celadon
        ThemeStyle.INK_GOLD -> inkGold
    }
}

val LocalThemeTokens = staticCompositionLocalOf { ThemeTokens() }
