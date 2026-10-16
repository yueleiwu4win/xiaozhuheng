package com.example.bookkeeping.ui.home

import android.graphics.BitmapFactory
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.resources.Density
import com.example.bookkeeping.data.db.BillWithCategory
import com.example.bookkeeping.data.db.entity.BillEntity
import com.example.bookkeeping.data.db.entity.CategoryEntity
import com.example.bookkeeping.data.prefs.*
import com.example.bookkeeping.ui.common.AppBackground
import com.example.bookkeeping.ui.common.AppBackgroundContent
import com.example.bookkeeping.ui.common.LocalBackgroundSpec
import com.example.bookkeeping.ui.theme.*
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.time.LocalDate
import java.time.YearMonth

class InkGoldReferenceSnapshotTest {
    @get:Rule val paparazzi = Paparazzi(deviceConfig = device(393, 850))

    @Test fun referenceRatio() {
        paparazzi.unsafeUpdateConfig(deviceConfig = device(600, 900))
        capture()
    }
    @Test fun readingSystemBoldOnPhoto() = capture(builtin = true, reading = ReadingOptions(ReadingSize.EXTRA_LARGE, ReadingWeight.BOLD, ReadingFont.SYSTEM, 60, true))
    @Test fun phone() = capture()
    @Test fun photoBackground() = capture(photo = true)
    @Test fun builtinBackground() = capture(builtin = true)
    @Test fun builtinBackgroundLight() = capture(builtin = true, mode = ThemeMode.LIGHT)
    @Test fun categoryAndSearch() = capture(initialFilter = MonthFilter(1, "expense", "餐"))
    @Test fun emptySearch() = capture(initialFilter = MonthFilter(keyword = "未找到"))
    @Test fun largeFont() {
        paparazzi.unsafeUpdateConfig(deviceConfig = device(320, 720, 2f))
        capture(large = true)
    }

    private fun capture(photo: Boolean = false, initialFilter: MonthFilter = MonthFilter(), large: Boolean = false,
        builtin: Boolean = false, mode: ThemeMode = ThemeMode.DARK, reading: ReadingOptions = ReadingOptions()) = paparazzi.snapshot {
        BookkeepingTheme(mode, ThemeStyle.INK_GOLD, reading) {
            var filter by remember { mutableStateOf(initialFilter) }
            val selected = bills.filter { (filter.type == null || filter.type == it.bill.type) &&
                (filter.categoryId == null || filter.categoryId == it.category.id) &&
                (filter.keyword.isBlank() || it.bill.note.orEmpty().contains(filter.keyword)) }
            val rows = selected.groupBy { it.bill.date }.flatMap { (date, group) ->
                listOf<HomeRow>(HomeRow.Header(date, summarizeBills(group))) + group.map { HomeRow.Item(it) }
            }
            Box(Modifier.fillMaxSize()) {
                if (photo) {
                    val bitmap = remember { requireNotNull(BitmapFactory.decodeFile(File("../docs/design/guofeng/ink-gold-photo-fixture.png").absolutePath)).asImageBitmap() }
                    AppBackgroundContent(BACKGROUND_SPEC_CUSTOM_IMAGE, bitmap, Modifier.fillMaxSize())
                } else CompositionLocalProvider(LocalBackgroundSpec provides if (builtin) BACKGROUND_SPEC_ABACUS else BACKGROUND_SPEC_DEFAULT) {
                    AppBackground(Modifier.fillMaxSize())
                }
                Scaffold(containerColor = Color.Transparent, bottomBar = {
                    Column {
                        LedgerCreateButton({}, true, Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp))
                        LedgerNavigationBar(0) {}
                    }
                }) { padding ->
                    Box(Modifier.fillMaxSize().padding(padding).readingSurface()) {
                        InkGoldHomeContent(YearMonth.of(2024, 10), rows,
                            if (large) MonthSummary(9_999_999_999_999, 8, 0, 0) else MonthSummary(234680, 8, 580000, 1),
                            summarizeBills(selected), filter, categories, "¥",
                            onKeyword = { filter = filter.copy(keyword = it) }, onFilters = {}, onPrevious = {}, onNext = {},
                            onChooseMonth = {}, onCurrentMonth = {},
                            onCategory = { id -> filter = filter.withLedgerCategory(categories.firstOrNull { it.id == id }) },
                            onReset = { filter = MonthFilter() }, onCreate = {}, today = LocalDate.of(2024, 10, 12)) {
                            LedgerBillRow(it, "¥", LocalAppColors.current.income, {})
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
        return BillWithCategory(BillEntity(id, cents, "expense", c.id, "2024-10-$day", 2024, 10, note, createdAt = 0, updatedAt = 0), c)
    }
    private val bills = listOf(
        bill(1, 12, 0, 6800, "晚餐 · 家常菜"), bill(2, 12, 2, 5800, "书籍 · 墨绿笔记本"),
        bill(3, 12, 1, 600, "地铁"), bill(4, 12, 0, 2800, "咖啡"), bill(5, 12, 4, 4600, "会员 · 阅读平台"),
        bill(6, 11, 2, 8900, "生活用品"), bill(7, 11, 0, 2600, "午餐 · 面馆"), bill(8, 11, 1, 1360, "地铁")
    )

    private fun device(width: Int, height: Int, fontScale: Float = 1f) = DeviceConfig(screenWidth = width, screenHeight = height,
        xdpi = 160, ydpi = 160, density = Density.MEDIUM, softButtons = false, fontScale = fontScale, locale = "zh-rCN")
}
