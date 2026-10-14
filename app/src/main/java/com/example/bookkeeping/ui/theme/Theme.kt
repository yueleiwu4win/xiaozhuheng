package com.example.bookkeeping.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.example.bookkeeping.data.prefs.ThemeMode

/** 语义色：跨页面统一取用，明暗各一套（避免各页面各写一份魔法色）。 */
@Immutable
data class AppColors(
    val income: Color,
    val swipeDelete: Color,
    val chartOther: Color
)

private val LightAppColors = AppColors(
    income = IncomeGreenLight,
    swipeDelete = SwipeDeleteOrange,
    chartOther = ChartOtherLight
)

private val DarkAppColors = AppColors(
    income = IncomeGreenDark,
    swipeDelete = SwipeDeleteOrange,
    chartOther = ChartOtherDark
)

/** 当前主题下的语义色，由 [BookkeepingTheme] 提供。 */
val LocalAppColors = staticCompositionLocalOf { LightAppColors }
val LocalAppDark = staticCompositionLocalOf { false }

private val LightColors = lightColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    onPrimaryContainer = PrimaryDark,
    primaryContainer = Color(0xFFE0ECE4),
    secondary = Primary,
    secondaryContainer = Color(0xFFE0ECE4),
    onSecondaryContainer = PrimaryDark,
    background = BackgroundLight,
    surface = SurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    surfaceContainer = SurfaceLight,
    surfaceContainerHigh = SurfaceVariantLight,
    surfaceContainerHighest = SurfaceVariantLight,
    surfaceContainerLow = BackgroundLight,
    surfaceContainerLowest = SurfaceLight,
    onBackground = OnSurfaceLight,
    onSurface = OnSurfaceLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    outline = OutlinedLight,
    outlineVariant = Color(0xFFE1E5DD),
    error = Danger
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF6FC2B2),
    onPrimary = Color(0xFF08302A),
    primaryContainer = PrimaryDark,
    // 显式指定，避免回落到 M3 基线浅紫（选中态配色不统一）
    onPrimaryContainer = Color(0xFFE3F2EE),
    onSecondaryContainer = Color(0xFFE3F2EE),
    secondary = Color(0xFF9BCBB4),
    secondaryContainer = Color(0xFF304D40),
    background = BackgroundDark,
    surface = SurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    surfaceContainer = SurfaceDark,
    surfaceContainerHigh = SurfaceVariantDark,
    surfaceContainerHighest = SurfaceVariantDark,
    surfaceContainerLow = BackgroundDark,
    surfaceContainerLowest = BackgroundDark,
    onBackground = OnSurfaceDark,
    onSurface = OnSurfaceDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    outline = OutlinedDark,
    outlineVariant = Color(0xFF3D4942),
    error = Color(0xFFFFB4AB)
)

/** 主题模式 → 是否深色（MainActivity 也用它同步系统栏图标）。 */
@Composable
fun ThemeMode.isDark(): Boolean = when (this) {
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
}

@Composable
fun BookkeepingTheme(
    themeMode: ThemeMode,
    content: @Composable () -> Unit
) {
    val dark = themeMode.isDark()
    CompositionLocalProvider(
        LocalAppColors provides if (dark) DarkAppColors else LightAppColors,
        LocalAppDark provides dark
    ) {
        MaterialTheme(
            colorScheme = if (dark) DarkColors else LightColors,
            typography = Typography,
            content = content
        )
    }
}
