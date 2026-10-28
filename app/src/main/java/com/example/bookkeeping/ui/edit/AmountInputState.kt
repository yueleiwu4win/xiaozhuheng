package com.example.bookkeeping.ui.edit

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.math.BigDecimal
import java.util.Locale

/** 仅支持加减的金额算式，逐项按整数分计算；保存时仍写入一个正数金额。 */
class AmountInputState {
    companion object {
        const val MAX_INTEGER_DIGITS = 8
        const val MAX_DECIMAL_DIGITS = 2
        const val MAX_CENTS = 9_999_999_999L
        private const val MAX_EXPRESSION_LENGTH = 160
    }
    private val _display = MutableStateFlow("0")
    val display = _display.asStateFlow()
    private var current = "0"
    private fun lastTermStart() = current.indexOfLast { it == '+' || it == '-' } + 1
    val hasCalculation: Boolean get() = current.any { it == '+' || it == '-' }
    val isComplete: Boolean get() = current.last() != '+' && current.last() != '-'

    fun digit(d: Char) {
        if (d !in '0'..'9' || current.length >= MAX_EXPRESSION_LENGTH) return
        val start = lastTermStart()
        val term = current.substring(start)
        if (!term.contains('.') && term.length >= MAX_INTEGER_DIGITS) return
        if (term.contains('.') && term.substringAfter('.').length >= MAX_DECIMAL_DIGITS) return
        current = if (term == "0") current.substring(0, start) + d else current + d
        publish()
    }
    fun dot() {
        if (current.length >= MAX_EXPRESSION_LENGTH - 1) return
        val term = current.substring(lastTermStart())
        if (term.contains('.')) return
        current += if (term.isEmpty()) "0." else "."
        publish()
    }
    fun operator(op: Char) {
        if (op != '+' && op != '-') return
        if (!isComplete) current = current.dropLast(1) + op
        else {
            if (current.length >= MAX_EXPRESSION_LENGTH - 1) return
            current += op
        }
        publish()
    }
    fun backspace() {
        current = current.dropLast(1).ifEmpty { "0" }
        publish()
    }
    /** 未完成的末尾运算符只用于预览；保存前必须通过validationError校验。 */
    fun totalCents(): Long {
        var sum = 0L
        var sign = 1
        var start = 0
        for (i in current.indices) {
            if (current[i] == '+' || current[i] == '-') {
                sum += sign * parseCents(current.substring(start, i))
                sign = if (current[i] == '+') 1 else -1
                start = i + 1
            }
        }
        return sum + sign * parseCents(current.substring(start))
    }
    fun validationError(): String? = when {
        !isComplete -> "请补全加减算式"
        totalCents() <= 0 -> "金额计算结果必须大于0"
        totalCents() > MAX_CENTS -> "金额不能超过99,999,999.99"
        else -> null
    }
    fun restore(cents: Long) {
        require(cents in 0..MAX_CENTS)
        current = String.format(Locale.US, "%d.%02d", cents / 100, cents % 100)
        publish()
    }
    fun reset() { current = "0"; publish() }
    private fun publish() { _display.value = current }
    private fun parseCents(s: String): Long = if (s.isEmpty()) 0 else BigDecimal(s).movePointRight(2).longValueExact()
}
