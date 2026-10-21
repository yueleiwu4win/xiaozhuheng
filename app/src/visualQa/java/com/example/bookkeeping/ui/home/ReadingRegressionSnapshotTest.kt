package com.example.bookkeeping.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.resources.Density
import com.example.bookkeeping.data.prefs.*
import com.example.bookkeeping.ui.chart.*
import com.example.bookkeeping.ui.theme.*
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ReadingRegressionSnapshotTest {
    @get:Rule val paparazzi = Paparazzi(deviceConfig = device(393, 1f))

    @Test fun chartsAt150Percent() = charts()
    @Test fun chartsAtCombined300Percent() {
        paparazzi.unsafeUpdateConfig(deviceConfig = device(320, 2f))
        charts()
    }
    private fun charts() = paparazzi.snapshot {
        BookkeepingTheme(ThemeMode.LIGHT, ThemeStyle.CLASSIC,
            ReadingOptions(size = ReadingSize.LARGEST, weight = ReadingWeight.BOLD)) {
            Surface {
                Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                    Text("年度支出", style = MaterialTheme.typography.titleMedium)
                    BarChart((1..12).map { ChartPoint("${it}月", it * 10000L) }, Modifier.fillMaxWidth().height(220.dp), highlightIndex = 11)
                    Text("每日趋势", style = MaterialTheme.typography.titleMedium)
                    LineChart(listOf(ChartSeries("支出", MaterialTheme.colorScheme.primary,
                        (1..31).map { ChartPoint("$it", it * 100L) })), Modifier.fillMaxWidth().height(220.dp))
                }
            }
        }
    }

    @Test fun serifResolvesRealMediumWeight() = paparazzi.snapshot {
        BookkeepingTheme(ThemeMode.LIGHT, ThemeStyle.SONG) {
            Surface {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    listOf(ReadingWeight.NORMAL to 400, ReadingWeight.MEDIUM to 500, ReadingWeight.BOLD to 700).forEach { (weight, expected) ->
                        val style = applyReadingStyle(TextStyle(fontSize = 24.sp), ReadingOptions(font = ReadingFont.SERIF, weight = weight))
                        assertNativeWeight(style, expected)
                        Text(weight.label, style = MaterialTheme.typography.labelLarge)
                        Text("餐饮 支出 ¥2,346.80\n自定义分类：鹤鸣清赏", style = style)
                        Spacer(Modifier.height(16.dp))
                    }
                    val themeStyle = applyReadingStyle(TextStyle(fontFamily = LedgerTypeface, fontWeight = FontWeight.Normal),
                        ReadingOptions(weight = ReadingWeight.MEDIUM))
                    assertNativeWeight(themeStyle, 500)
                }
            }
        }
    }

    @Composable private fun assertNativeWeight(style: TextStyle, expected: Int) {
        val face = LocalFontFamilyResolver.current.resolve(style.fontFamily, style.fontWeight!!).value as android.graphics.Typeface
        assertEquals(expected, face.weight)
    }

    companion object {
        private fun device(width: Int, fontScale: Float) = DeviceConfig(screenWidth = width, screenHeight = 850,
            xdpi = 160, ydpi = 160, density = Density.MEDIUM, softButtons = false, locale = "zh-rCN", fontScale = fontScale)
    }
}
