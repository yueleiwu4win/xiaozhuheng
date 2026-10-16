package com.example.bookkeeping.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.resources.Density
import com.example.bookkeeping.data.db.BillWithCategory
import com.example.bookkeeping.data.db.entity.BillEntity
import com.example.bookkeeping.data.db.entity.CategoryEntity
import com.example.bookkeeping.data.prefs.*
import com.example.bookkeeping.ui.common.AppBackground
import com.example.bookkeeping.ui.common.LocalBackgroundSpec
import com.example.bookkeeping.ui.theme.*
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class SongReferenceSnapshotTest {
    @get:Rule val paparazzi = Paparazzi(deviceConfig = device(393, 850))

    @Test fun referenceRatio() {
        paparazzi.unsafeUpdateConfig(deviceConfig = device(390, 844))
        capture()
    }
    @Test fun dark() = capture(mode = ThemeMode.DARK)
    @Test fun builtinPhoto() = capture(builtin = true)
    @Test fun filtered() = capture(initialFilter = MonthFilter(1, "expense", "午餐"))
    @Test fun emptySearch() = capture(initialFilter = MonthFilter(keyword = "未找到"))
    @Test fun largeFont() {
        paparazzi.unsafeUpdateConfig(deviceConfig = device(320, 720, 2f))
        capture(large = true)
    }

    private fun capture(initialFilter: MonthFilter = MonthFilter(), large: Boolean = false,
        builtin: Boolean = false, mode: ThemeMode = ThemeMode.LIGHT) = paparazzi.snapshot {
        BookkeepingTheme(mode, ThemeStyle.SONG) {
            CompositionLocalProvider(LocalBackgroundSpec provides if (builtin) BACKGROUND_SPEC_ABACUS else BACKGROUND_SPEC_DEFAULT) {
                var filter by remember { mutableStateOf(initialFilter) }
                val selected = bills.filter { (filter.type == null || filter.type == it.bill.type) &&
                    (filter.categoryId == null || filter.categoryId == it.category.id) &&
                    (filter.keyword.isBlank() || it.bill.note.orEmpty().contains(filter.keyword)) }
                val rows = selected.groupBy { it.bill.date }.flatMap { (date, group) ->
                    listOf<HomeRow>(HomeRow.Header(date, summarizeBills(group))) + group.map { HomeRow.Item(it) }
                }
                Box(Modifier.fillMaxSize()) {
                    AppBackground(Modifier.fillMaxSize())
                    Scaffold(containerColor = Color.Transparent, bottomBar = {
                        Column {
                            SongCreateAction({})
                            LedgerNavigationBar(0) {}
                        }
                    }) { padding ->
                        Box(Modifier.fillMaxSize().padding(padding)) {
                            SongHomeContent(YearMonth.of(2026, 10), rows,
                                if (large) MonthSummary(9_999_999_999_999, 5, 0, 0) else MonthSummary(234680, 5, 580000, 1),
                                summarizeBills(selected), filter, categories.firstOrNull { it.id == filter.categoryId }?.name, "¥", filter.keyword.isNotBlank(),
                                onSearch = {}, onFilters = {}, searchContent = {
                                    androidx.compose.material3.OutlinedTextField(filter.keyword, { filter = filter.copy(keyword = it) },
                                        Modifier.fillMaxWidth().padding(horizontal = 16.dp), placeholder = { androidx.compose.material3.Text("搜索本月备注") }, singleLine = true)
                                }, onPrevious = {}, onNext = {},
                                onChooseMonth = {}, onCurrentMonth = {}, onType = { filter = filter.copy(type = it) },
                                onClearCategory = { filter = filter.copy(categoryId = null) },
                                onReset = { filter = MonthFilter() }, onCreate = {}, today = LocalDate.of(2026, 10, 16)) {
                                LedgerBillRow(it, "¥", LocalAppColors.current.income, {})
                            }
                        }
                    }
                }
            }
        }
    }

    private val categories = listOf("餐饮" to "restaurant", "交通" to "directions_bus", "购物" to "shopping_bag",
        "生活" to "home", "学习" to "menu_book", "娱乐" to "sports_esports", "其他" to "more_horiz").mapIndexed { index, pair ->
        CategoryEntity(index + 1L, pair.first, "expense", pair.second, "#214F43", index)
    }
    private fun bill(id: Long, day: Int, category: Int, cents: Long, note: String): BillWithCategory {
        val c = categories[category]
        return BillWithCategory(BillEntity(id, cents, "expense", c.id, "2026-10-$day", 2026, 10, note, createdAt = 0, updatedAt = 0), c)
    }
    private val bills = listOf(
        bill(1, 16, 0, 2800, "午餐"), bill(2, 16, 1, 600, "地铁"),
        bill(3, 15, 2, 8600, "日用品"), bill(4, 15, 4, 4900, "书籍"), bill(5, 15, 0, 2200, "咖啡")
    )

    private fun device(width: Int, height: Int, fontScale: Float = 1f) = DeviceConfig(screenWidth = width, screenHeight = height,
        xdpi = 160, ydpi = 160, density = Density.MEDIUM, softButtons = false, fontScale = fontScale, locale = "zh-rCN")
}
