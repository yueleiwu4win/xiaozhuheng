package com.example.bookkeeping.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.resources.Density
import com.example.bookkeeping.data.db.BillWithCategory
import com.example.bookkeeping.data.db.entity.BillEntity
import com.example.bookkeeping.data.db.entity.CategoryEntity
import com.example.bookkeeping.data.prefs.ThemeMode
import com.example.bookkeeping.data.prefs.ThemeStyle
import com.example.bookkeeping.ui.common.AppBackground
import com.example.bookkeeping.ui.theme.*
import org.junit.Rule
import org.junit.Test
import java.time.YearMonth
import java.time.LocalDate

/** 使用生产 Compose 组件和 Android Layoutlib，输出可比对的原生渲染；不创建替代网页。 */
class LedgerHomeSnapshotTest {
    @get:Rule val paparazzi = Paparazzi(deviceConfig = DeviceConfig(screenWidth = 393, screenHeight = 850,
        xdpi = 160, ydpi = 160, density = Density.MEDIUM, softButtons = false, locale = "zh-rCN"))

    @Test fun celadon() = snapshot(ThemeStyle.CELADON, ThemeMode.LIGHT)
    @Test fun inkGold() = snapshot(ThemeStyle.INK_GOLD, ThemeMode.DARK)
    @Test fun celadonDark() = snapshot(ThemeStyle.CELADON, ThemeMode.DARK)
    @Test fun inkGoldLight() = snapshot(ThemeStyle.INK_GOLD, ThemeMode.LIGHT)
    @Test fun narrowLargeFont() {
        paparazzi.unsafeUpdateConfig(deviceConfig = DeviceConfig(screenWidth = 320, screenHeight = 720,
            xdpi = 160, ydpi = 160, density = Density.MEDIUM, softButtons = false, fontScale = 1.3f, locale = "zh-rCN"))
        snapshot(ThemeStyle.INK_GOLD, ThemeMode.DARK, large = true)
    }
    @Test fun celadonLargeFont() {
        paparazzi.unsafeUpdateConfig(deviceConfig = DeviceConfig(screenWidth = 320, screenHeight = 720,
            xdpi = 160, ydpi = 160, density = Density.MEDIUM, softButtons = false, fontScale = 2f, locale = "zh-rCN"))
        snapshot(ThemeStyle.CELADON, ThemeMode.LIGHT, large = true)
    }
    @Test fun celadonSystemBarViewport() {
        paparazzi.unsafeUpdateConfig(deviceConfig = DeviceConfig(screenWidth = 393, screenHeight = 802,
            xdpi = 160, ydpi = 160, density = Density.MEDIUM, softButtons = false, locale = "zh-rCN"))
        snapshot(ThemeStyle.CELADON, ThemeMode.LIGHT)
    }

    private fun snapshot(style: ThemeStyle, mode: ThemeMode, large: Boolean = false) = paparazzi.snapshot {
        BookkeepingTheme(mode, style) {
            val jade = style == ThemeStyle.CELADON
            Box(Modifier.fillMaxSize()) {
                AppBackground(Modifier.fillMaxSize())
                Scaffold(containerColor = Color.Transparent,
                    bottomBar = {
                        Column {
                            LedgerCreateButton({}, true, Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp))
                            LedgerNavigationBar(0) {}
                        }
                    }
                ) { padding ->
                    Box(Modifier.fillMaxSize().padding(padding)) { Page(large) }
                }
            }
        }
    }

    @Composable private fun Page(large: Boolean) {
        val bills = listOf(
            sample(1, "2026-10-14", "餐饮", "restaurant", 2800, "午餐"),
            sample(2, "2026-10-14", "交通", "directions_bus", 600, "地铁"),
            sample(3, "2026-10-14", "购物", "shopping_bag", 8600, "日用品"),
            sample(4, "2026-10-13", "书籍", "menu_book", 4900, "阅读")
        )
        val rows = bills.groupBy { it.bill.date }.flatMap { (date, group) ->
            listOf<HomeRow>(HomeRow.Header(date, summarizeBills(group))) + group.map { HomeRow.Item(it) }
        }
        LedgerHomeContent(YearMonth.of(2026, 10), rows,
            if (large) MonthSummary(9_999_999_999_999, 4, 0, 0) else MonthSummary(202371, 4, 800000, 1),
            MonthSummary(), MonthFilter(), null, "¥", false, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {},
            today = LocalDate.of(2026, 10, 14), categories = bills.map { it.category }) { item ->
            LedgerBillRow(item, "¥", LocalAppColors.current.income, {})
        }
    }

    private fun sample(id: Long, date: String, category: String, icon: String, amount: Long, note: String) = BillWithCategory(
        BillEntity(id = id, amountCents = amount, type = "expense", categoryId = id, date = date, year = 2026, month = 10,
            note = note, createdAt = 0, updatedAt = 0),
        CategoryEntity(id = id, name = category, type = "expense", icon = icon, color = "#214F43", sortOrder = 0))
}
