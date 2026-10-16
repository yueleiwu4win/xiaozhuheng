package com.example.bookkeeping.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.SaveAlt
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.resources.Density
import com.example.bookkeeping.data.db.entity.CategoryEntity
import com.example.bookkeeping.data.prefs.ThemeMode
import com.example.bookkeeping.data.prefs.ThemeStyle
import com.example.bookkeeping.ui.common.*
import com.example.bookkeeping.ui.edit.AmountKeyboard
import com.example.bookkeeping.ui.edit.CategoryCell
import com.example.bookkeeping.ui.settings.SettingRow
import com.example.bookkeeping.ui.theme.BookkeepingTheme
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

/** 直接复用设置行、统计区块、分类和数字键盘；这是控件检查板，并非整页截图。 */
class LedgerControlsSnapshotTest {
    @get:Rule val paparazzi = Paparazzi(deviceConfig = DeviceConfig(screenWidth = 393, screenHeight = 850,
        xdpi = 160, ydpi = 160, density = Density.MEDIUM, softButtons = false, locale = "zh-rCN"))

    @Test fun songControls() = render(ThemeStyle.SONG, ThemeMode.LIGHT)
    @Test fun songDarkControls() = render(ThemeStyle.SONG, ThemeMode.DARK)
    @Test fun celadonControls() = render(ThemeStyle.CELADON, ThemeMode.LIGHT)
    @Test fun inkGoldControls() = render(ThemeStyle.INK_GOLD, ThemeMode.DARK)

    private fun render(style: ThemeStyle, mode: ThemeMode) = paparazzi.snapshot {
        BookkeepingTheme(mode, style) {
            Box(Modifier.fillMaxSize()) {
                AppBackground(Modifier.fillMaxSize())
                Column(Modifier.fillMaxSize()) {
                    Column(Modifier.weight(1f).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        SectionCard("外观与备份") {
                            SettingRow("界面风格", Icons.Outlined.Palette, value = style.label) {}
                            SettingRow("备份数据", Icons.Outlined.SaveAlt, subtitle = "保存账单、分类和设置") {}
                        }
                        SectionCard("当月支出") {
                            Text("¥2,023.71", style = themeAmountStyle(MaterialTheme.typography.displayMedium))
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                MetricCard("日均支出", "¥144.55", Modifier.weight(1f))
                                MetricCard("最大单笔", "¥860.00", Modifier.weight(1f))
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("餐饮" to "restaurant", "交通" to "directions_bus", "购物" to "shopping_bag", "书籍" to "menu_book").forEachIndexed { index, (name, icon) ->
                                Box(Modifier.weight(1f)) {
                                    CategoryCell(CategoryEntity(name = name, type = "expense", icon = icon, color = "#214F43", sortOrder = index), index == 0, true) {}
                                }
                            }
                        }
                    }
                    AmountKeyboard(true, LocalDate.now(), false, false, false, false, {}, {}, {}, {}, {}, {}, {})
                }
            }
        }
    }
}
