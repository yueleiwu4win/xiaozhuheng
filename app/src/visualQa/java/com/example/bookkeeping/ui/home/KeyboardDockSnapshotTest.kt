package com.example.bookkeeping.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.resources.Density
import com.example.bookkeeping.data.prefs.*
import com.example.bookkeeping.ui.edit.*
import com.example.bookkeeping.ui.theme.BookkeepingTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

/** 使用注入的输入法高度验证真实底部布局；不把这些静态截图当作帧率测试。 */
@OptIn(ExperimentalLayoutApi::class)
class KeyboardDockSnapshotTest {
    @get:Rule val paparazzi = Paparazzi(deviceConfig = DeviceConfig(screenWidth = 393, screenHeight = 600,
        xdpi = 160, ydpi = 160, density = Density.MEDIUM, softButtons = false, locale = "zh-rCN"))

    @Test fun closed() = capture(0)
    @Test fun openingEarlyFrame() = capture(100)
    @Test fun fullyOpen() = capture(320)

    private fun capture(imeHeight: Int) {
        var measured = 0
        paparazzi.snapshot {
            BookkeepingTheme(ThemeMode.LIGHT, ThemeStyle.CLASSIC) {
                Surface {
                    Column(Modifier.fillMaxSize()) {
                        Text("键盘切换布局检查", Modifier.padding(16.dp))
                        Spacer(Modifier.weight(1f))
                        EntryNoteRow("超市购物，水果和日用品合计", true, {})
                        EntryKeyboardDock(imeHeight > 0, Modifier.onSizeChanged { measured = it.height }, WindowInsets(bottom = imeHeight)) {
                            Column { AmountKeyboard(true, LocalDate.of(2026, 10, 28), false, false, false, {}, {}, {}, {}, {}, {}, {}) }
                        }
                    }
                }
            }
        }
        assertEquals(maxOf(237, imeHeight), measured)
    }

    @Test fun animationFramesDoNotRecreateKeyboardOrRecomposeIt() {
        var created = 0
        var compositions = 0
        val frames = mutableStateOf(0)
        paparazzi.snapshot {
            val insets = remember { MutableWindowInsets(WindowInsets(bottom = 0)) }
            // 在组合后连续推进输入法高度，模拟同一组合生命周期中的开/关。
            LaunchedEffect(Unit) {
                listOf(80, 160, 320, 160, 80, 0).forEach { h ->
                    insets.insets = WindowInsets(bottom = h)
                    frames.value++
                    withFrameNanos { }
                }
            }
            EntryKeyboardDock(false, imeInsets = insets) {
                remember { created++; Any() }
                SideEffect { compositions++ }
                Surface(color = androidx.compose.ui.graphics.Color.White) {
                    Text("金额键盘驻留检查", Modifier.fillMaxWidth().height(237.dp), color = androidx.compose.ui.graphics.Color.Black)
                }
            }
        }
        assertTrue(frames.value > 0)
        assertEquals(1, created)
        assertEquals(1, compositions)
    }
}
