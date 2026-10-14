package com.example.bookkeeping.ui.edit

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale

/**
 * 金额键盘输入状态机（纯逻辑，便于测试）。
 *
 * 只负责单一正数金额的输入；支出 / 收入由页面的类型切换决定，
 * 金额一律按正数存储，展示层不显示负号。
 */
class AmountInputState {

    companion object {
        const val MAX_INTEGER_DIGITS = 8 // 8 位整数 = 上限 99,999,999.99
        const val MAX_DECIMAL_DIGITS = 2
    }

    private val _display = MutableStateFlow("0")
    val display: StateFlow<String> = _display.asStateFlow()

    private var current: String = "0"

    fun digit(d: Char) {
        if (!d.isDigit()) return
        val intPart = if (current.contains('.')) current.substringBefore('.') else current
        if (intPart.length >= MAX_INTEGER_DIGITS && !current.contains('.')) return
        if (current.contains('.')) {
            val decimals = current.substringAfter('.').length
            if (decimals >= MAX_DECIMAL_DIGITS) return
        }
        current = if (current == "0") d.toString() else current + d
        publish()
    }

    fun dot() {
        if (current.contains('.')) return
        current += "."
        publish()
    }

    fun backspace() {
        if (current.length <= 1) {
            current = "0"
        } else {
            current = current.dropLast(1)
        }
        publish()
    }

    /** 当前金额（分）。 */
    fun totalCents(): Long = parseCents(current)

    /** 编辑模式：恢复一个已有金额。 */
    fun restore(cents: Long) {
        current = String.format(Locale.US, "%d.%02d", cents / 100, cents % 100)
        publish()
    }

    /** 保存成功后重置（分类与日期由调用方保留，便于连续记账）。 */
    fun reset() {
        current = "0"
        publish()
    }

    private fun publish() {
        _display.value = current
    }

    private fun parseCents(s: String): Long {
        if (s.isEmpty() || s == "0") return 0
        val normalized = if (s.endsWith(".")) s + "0" else s
        return try {
            BigDecimal(normalized)
                .movePointRight(2)
                .setScale(0, RoundingMode.HALF_UP)
                .toLong()
        } catch (e: NumberFormatException) {
            0
        }
    }
}