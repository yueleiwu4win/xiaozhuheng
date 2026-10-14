package com.example.bookkeeping.util

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

object DateUtils {

    val WEEKDAYS = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")

    fun today(): LocalDate = LocalDate.now()

    fun parse(date: String): LocalDate = LocalDate.parse(date)

    fun format(date: LocalDate): String = date.toString() // ISO yyyy-MM-dd

    /** 列表分组标题：「今天」/「昨天」/「6月14日 周六」/「2024年6月14日 周六」。 */
    fun groupLabel(dateStr: String): String {
        val d = parse(dateStr)
        val now = today()
        val base = when (d) {
            now -> "今天"
            now.minusDays(1) -> "昨天"
            else -> {
                val prefix = if (d.year == now.year) "" else "${d.year}年"
                "${prefix}${d.monthValue}月${d.dayOfMonth}日"
            }
        }
        val weekday = WEEKDAYS[d.dayOfWeek.value - 1]
        return "$base $weekday"
    }

    fun weekdayLabel(date: LocalDate): String = WEEKDAYS[date.dayOfWeek.value - 1]

    fun monthName(year: Int, month: Int): String = "${year}年${month}月"

    fun daysInMonth(year: Int, month: Int): Int = YearMonth.of(year, month).lengthOfMonth()

    /** 本月已过天数（当月为今天，历史月为整月）。 */
    fun daysPassed(year: Int, month: Int): Int {
        val now = today()
        return if (year == now.year && month == now.monthValue) now.dayOfMonth
        else daysInMonth(year, month)
    }

    fun isFuture(date: LocalDate): Boolean = date.isAfter(today())
}