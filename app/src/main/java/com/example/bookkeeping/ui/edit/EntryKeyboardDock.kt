package com.example.bookkeeping.ui.edit

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth

/** 保留金额键盘实例；输入法动画只影响布局测量，不触发整页逐帧组合。 */
@Composable
internal fun EntryKeyboardDock(
    imeVisible: Boolean,
    modifier: Modifier = Modifier,
    imeInsets: WindowInsets = WindowInsets.ime.exclude(WindowInsets.navigationBars),
    keyboard: @Composable () -> Unit
) {
    val guarded = if (imeVisible) Modifier.clearAndSetSemantics { }.pointerInput(Unit) {
        awaitPointerEventScope {
            while (true) awaitPointerEvent(PointerEventPass.Initial).changes.forEach { it.consume() }
        }
    } else Modifier
    Layout(content = keyboard, modifier = modifier.fillMaxWidth().clipToBounds().then(guarded)) { children, constraints ->
        val keypad = children.single().measure(constraints.copy(minHeight = 0, maxHeight = Constraints.Infinity))
        // 使用同一底部区域；小于键盘高度的动画帧不再引起分类区先扩后缩。
        val height = constraints.constrainHeight(maxOf(keypad.height, imeInsets.getBottom(this)))
        layout(constraints.constrainWidth(keypad.width), height) {
            keypad.placeRelative(0, height - keypad.height)
        }
    }
}
