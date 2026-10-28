package com.example.bookkeeping.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.Image
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.resources.Density
import com.example.bookkeeping.data.prefs.*
import com.example.bookkeeping.data.repo.*
import com.example.bookkeeping.data.db.CategoryTotal
import com.example.bookkeeping.ui.budget.BudgetStrip
import com.example.bookkeeping.ui.summary.SummaryContent
import com.example.bookkeeping.ui.theme.BookkeepingTheme
import org.junit.Rule
import org.junit.Test
import java.time.*

class MonthlyToolsSnapshotTest {
    @get:Rule val paparazzi = Paparazzi(deviceConfig = DeviceConfig(screenWidth = 393, screenHeight = 852,
        xdpi = 160, ydpi = 160, density = Density.MEDIUM, softButtons = false, locale = "zh-rCN"))
    @Test fun classic() = capture(ThemeStyle.CLASSIC)
    @Test fun song() = capture(ThemeStyle.SONG)
    @Test fun celadon() = capture(ThemeStyle.CELADON)
    @Test fun inkGold() = capture(ThemeStyle.INK_GOLD)
    @Test fun classicLarge() = capture(ThemeStyle.CLASSIC, true)
    @Test fun songLarge() = capture(ThemeStyle.SONG, true)
    @Test fun celadonLarge() = capture(ThemeStyle.CELADON, true)
    @Test fun inkGoldLarge() = capture(ThemeStyle.INK_GOLD, true)
    @Test fun exportedPoster() = poster(false)
    @Test fun hiddenPoster() = poster(true)
    private fun poster(hidden: Boolean) = paparazzi.snapshot {
        BookkeepingTheme(ThemeMode.DARK, ThemeStyle.INK_GOLD) {
            val data = MonthlySummary(SummaryPeriod.of(YearMonth.of(2026, 10), LocalDate.of(2026, 10, 28)),
                234680, 580000, 29, 280000, listOf(CategoryTotal(1, "餐饮", "food", "#214F43", 234680, 29)), null, 300000)
            val colors = MaterialTheme.colorScheme
            val bitmap = com.example.bookkeeping.ui.summary.SummaryPoster.render(
                com.example.bookkeeping.ui.summary.SummaryPoster.lines(data, "¥", hidden),
                colors.background.toArgb(), colors.onBackground.toArgb(), colors.primary.toArgb(), android.graphics.Typeface.DEFAULT)
            Image(bitmap.asImageBitmap(), "真实导出海报", Modifier.fillMaxWidth().aspectRatio(bitmap.width.toFloat() / bitmap.height))
        }
    }
    private fun capture(style: ThemeStyle, large: Boolean = false) = paparazzi.snapshot {
        BookkeepingTheme(if (style == ThemeStyle.INK_GOLD) ThemeMode.DARK else ThemeMode.LIGHT, style,
            ReadingOptions(size = if (large) ReadingSize.LARGEST else ReadingSize.SYSTEM)) {
            Surface {
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    BudgetStrip(null, 0, "¥") {}
                    BudgetStrip(300000, 234680, "¥") {}
                    BudgetStrip(200000, 234680, "¥") {}
                    SummaryContent(MonthlySummary(SummaryPeriod.of(YearMonth.of(2026, 10), LocalDate.of(2026, 10, 28)),
                        234680, 580000, 29, 280000,
                        listOf(CategoryTotal(1, "餐饮", "food", "#214F43", 120000, 10)), null, 300000), "¥", false)
                }
            }
        }
    }
}
