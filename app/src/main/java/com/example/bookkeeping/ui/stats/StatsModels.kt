package com.example.bookkeeping.ui.stats

import com.example.bookkeeping.data.db.CategoryTotal
import com.example.bookkeeping.data.db.CategoryYearTotal
import com.example.bookkeeping.data.db.LabelTotal
import com.example.bookkeeping.data.repo.BillType
import java.time.YearMonth
import java.util.Locale

data class StatsSelection(val month: YearMonth = YearMonth.now(), val year: Int = month.year, val type: String = BillType.EXPENSE)

data class MonthStats(
    val selection: StatsSelection, val categories: List<CategoryTotal>, val daily: List<LabelTotal>,
    val total: Long, val previous: Long, val count: Long, val maximum: Long?
)

data class YearStats(
    val year: Int, val type: String, val months: List<LabelTotal>, val categories: List<CategoryTotal>,
    val years: List<LabelTotal>, val categoryYears: List<CategoryYearTotal>
)

fun percentChange(current: Long, previous: Long): String? {
    if (previous <= 0L) return null
    val percent = (current.toDouble() - previous.toDouble()) * 100 / previous
    return when {
        percent > 0 -> String.format(Locale.CHINA, "↑%.1f%%", percent)
        percent < 0 -> String.format(Locale.CHINA, "↓%.1f%%", -percent)
        else -> "0.0%"
    }
}

fun completeMonths(rows: List<LabelTotal>): List<LabelTotal> {
    val totals = rows.associateBy { it.label }
    return (1..12).map { totals[it] ?: LabelTotal(it, 0, 0) }
}
