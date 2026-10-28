package com.example.bookkeeping.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import com.example.bookkeeping.ui.edit.AmountKeyboard
import com.example.bookkeeping.ui.edit.EntryNoteRow
import androidx.compose.ui.platform.LocalDensity
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.resources.Density
import com.example.bookkeeping.data.prefs.*
import com.example.bookkeeping.ui.common.LocalBackgroundSpec
import com.example.bookkeeping.ui.settings.ReadingSettingsContent
import com.example.bookkeeping.ui.settings.ReadingPreview
import com.example.bookkeeping.ui.theme.BookkeepingTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ReadingSettingsSnapshotTest {
    @get:Rule val paparazzi = Paparazzi(deviceConfig = DeviceConfig(screenWidth = 393, screenHeight = 850,
        xdpi = 160, ydpi = 160, density = Density.MEDIUM, softButtons = false, locale = "zh-rCN"))

    @Test fun panelTopWithLivePreview() = panel(0)
    @Test fun panelFontBackgroundAndEnhancement() = panel(5)
    @Test fun inkGoldPage() = panel(0, ThemeStyle.INK_GOLD, ThemeMode.DARK, draft = ReadingOptions())
    @Test fun inkGoldLowerOptions() = panel(3, ThemeStyle.INK_GOLD, ThemeMode.DARK)
    @Test fun celadonPage() = panel(0, ThemeStyle.CELADON)
    @Test fun classicPage() = panel(0, ThemeStyle.CLASSIC)
    @Test fun largerOptionsUseTwoColumns() = panel(2, ThemeStyle.INK_GOLD, ThemeMode.DARK, ReadingOptions(size = ReadingSize.LARGEST))
    @Test fun combinedLargeOptionsRemainReachable() {
        paparazzi.unsafeUpdateConfig(deviceConfig = DeviceConfig(screenWidth = 320, screenHeight = 850,
            xdpi = 160, ydpi = 160, density = Density.MEDIUM, softButtons = false, locale = "zh-rCN", fontScale = 2f))
        panel(2, ThemeStyle.INK_GOLD, ThemeMode.DARK, ReadingOptions(size = ReadingSize.LARGEST))
    }

    private fun panel(index: Int, theme: ThemeStyle = ThemeStyle.SONG, mode: ThemeMode = ThemeMode.LIGHT,
        reading: ReadingOptions = ReadingOptions(), draft: ReadingOptions = ReadingOptions.Clear) = paparazzi.snapshot {
        val settings = AppSettings(themeMode = mode, themeStyle = theme, background = BACKGROUND_SPEC_ABACUS, reading = reading)
        CompositionLocalProvider(LocalBackgroundSpec provides settings.background) {
            BookkeepingTheme(settings.themeMode, settings.themeStyle, settings.reading) {
                Surface(color = MaterialTheme.colorScheme.surface) {
                    ReadingSettingsContent(settings, draft, {}, {}, rememberLazyListState(index))
                }
            }
        }
    }

    @Test fun classicBillsWithLargeBoldTextAndLongAmount() = paparazzi.snapshot {
        BookkeepingTheme(ThemeMode.LIGHT, ThemeStyle.CLASSIC,
            ReadingOptions(ReadingSize.EXTRA_LARGE, ReadingWeight.BOLD, ReadingFont.SYSTEM, 45, true)) {
            Surface {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    val category = com.example.bookkeeping.data.db.entity.CategoryEntity(1, "餐饮", "expense", "restaurant", "#214F43", 0)
                    BillRow(com.example.bookkeeping.data.db.BillWithCategory(
                        com.example.bookkeeping.data.db.entity.BillEntity(1, 99999999999, "expense", 1, "2026-10-16", 2026, 10,
                            "和家人一起吃午餐", createdAt = 0, updatedAt = 0), category), "¥", com.example.bookkeeping.ui.theme.LocalAppColors.current.income, {})
                }
            }
        }
    }

    @Test fun combinedSystemAndAppLargeKeyboard() {
        paparazzi.unsafeUpdateConfig(deviceConfig = DeviceConfig(screenWidth = 320, screenHeight = 720,
            xdpi = 160, ydpi = 160, density = Density.MEDIUM, softButtons = false, locale = "zh-rCN", fontScale = 2f))
        paparazzi.snapshot {
            BookkeepingTheme(ThemeMode.LIGHT, ThemeStyle.SONG,
                ReadingOptions(ReadingSize.LARGEST, ReadingWeight.BOLD, ReadingFont.SERIF, 40, true)) {
                Surface {
                    Column(Modifier.fillMaxSize()) {
                        androidx.compose.material3.Text("记一笔", Modifier.padding(12.dp), style = MaterialTheme.typography.titleLarge)
                        FittedLedgerAmount("¥99,999,999.99", 36.sp, Modifier.fillMaxWidth().padding(16.dp))
                        Spacer(Modifier.weight(1f))
                        EntryNoteRow("超市购物，多个商品合计", true) {}
                        AmountKeyboard(true, LocalDate.of(2026, 10, 14), false, false, false, {}, {}, {}, {}, {}, {}, {})
                    }
                }
            }
        }
    }

    @Test fun previewDoesNotMultiplySavedFontScaleAgain() = paparazzi.snapshot {
        val settings = AppSettings(themeMode = ThemeMode.LIGHT, reading = ReadingOptions(size = ReadingSize.LARGEST))
        BookkeepingTheme(settings.themeMode, settings.themeStyle, settings.reading) {
            assertEquals(1.5f, LocalDensity.current.fontScale, 0.001f)
            BookkeepingTheme(settings.themeMode, settings.themeStyle, ReadingOptions.Clear) {
                assertEquals(1.15f, LocalDensity.current.fontScale, 0.001f)
            }
            ReadingPreview(settings, ReadingOptions.Clear)
        }
    }
}
