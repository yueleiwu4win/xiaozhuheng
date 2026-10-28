package com.example.bookkeeping.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.resources.Density
import com.example.bookkeeping.data.prefs.*
import com.example.bookkeeping.ui.edit.EntryNoteRow
import com.example.bookkeeping.ui.theme.BookkeepingTheme
import org.junit.Rule
import org.junit.Test

/** 验证真实输入组件的已有备注、焦点与字数布局；不模拟或声称验证系统输入法。 */
class InlineNoteSnapshotTest {
    @get:Rule val paparazzi = Paparazzi(deviceConfig = DeviceConfig(screenWidth = 393, screenHeight = 500,
        xdpi = 160, ydpi = 160, density = Density.MEDIUM, softButtons = false, locale = "zh-rCN"))

    @Test fun filledInlineNote() = capture(false)
    @Test fun focusedInlineNoteShowsCounter() = capture(true)

    private fun capture(focused: Boolean) = paparazzi.snapshot {
        BookkeepingTheme(ThemeMode.DARK, ThemeStyle.INK_GOLD) {
            Surface {
                Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
                    var note by remember { mutableStateOf("超市购物，水果和日用品合计") }
                    val requester = remember { FocusRequester() }
                    EntryNoteRow(note, true, { note = it }, modifier = Modifier.focusRequester(requester))
                    if (focused) LaunchedEffect(Unit) { requester.requestFocus() }
                }
            }
        }
    }
}
