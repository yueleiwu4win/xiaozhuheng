package com.example.bookkeeping.data.repo

import androidx.room.withTransaction
import com.example.bookkeeping.data.db.AppDatabase
import com.example.bookkeeping.data.db.CategoryTotal
import com.example.bookkeeping.data.db.BillWithCategory
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.channels.awaitClose
import java.time.LocalDate
import java.time.YearMonth

data class SummaryPeriod(val month: YearMonth, val end: LocalDate, val previousStart: LocalDate, val previousEnd: LocalDate) {
    companion object {
        fun of(month: YearMonth, today: LocalDate): SummaryPeriod {
            require(month <= YearMonth.from(today))
            val previous = month.minusMonths(1)
            val end = if (month == YearMonth.from(today)) today else month.atEndOfMonth()
            val previousEnd = if (month == YearMonth.from(today)) previous.atDay(minOf(today.dayOfMonth, previous.lengthOfMonth())) else previous.atEndOfMonth()
            return SummaryPeriod(month, end, previous.atDay(1), previousEnd)
        }
    }
}

data class MonthlySummary(
    val period: SummaryPeriod, val expense: Long, val income: Long, val count: Long,
    val previousExpense: Long, val categories: List<CategoryTotal>, val largest: BillWithCategory?, val budget: Long?
) {
    val balance get() = income - expense
    fun changeText(money: (Long) -> String): String {
        val delta = expense - previousExpense
        if (previousExpense == 0L) return if (expense == 0L) "两期均无支出" else "上期无支出，本期支出 ${money(expense)}"
        if (delta == 0L) return "支出与上期持平"
        return "较上期${if (delta > 0) "增加" else "减少"} ${money(kotlin.math.abs(delta))}（${"%.1f".format(kotlin.math.abs(delta.toDouble()) / previousExpense * 100)}%）"
    }
    fun insight(): String = when {
        count == 0L -> "本月还没有账单，记下第一笔后再来查看。"
        expense == 0L -> "本月已记录收入，暂无支出记录。"
        else -> categories.take(2).let { top -> "支出主要集中在${top.joinToString("、") { it.name }}，合计占支出的 ${"%.0f".format(top.sumOf { it.totalCents }.toDouble() / expense * 100)}%。" }
    }
}

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class MonthlySummaryRepository(private val db: AppDatabase) {
    fun observe(month: YearMonth, today: LocalDate): Flow<MonthlySummary> {
        val period = SummaryPeriod.of(month, today)
        // Query flows invalidate on any bill/category edit, even when monthly totals stay unchanged.
        return callbackFlow {
            val observer = object : androidx.room.InvalidationTracker.Observer("bill", "category", "monthly_budget") {
                override fun onInvalidated(tables: Set<String>) { trySend(Unit) }
            }
            db.invalidationTracker.addObserver(observer)
            trySend(Unit)
            awaitClose { db.invalidationTracker.removeObserver(observer) }
        }.conflate()
            .mapLatest {
                db.withTransaction {
                    val dao = db.billDao()
                    val start = month.atDay(1).toString(); val end = period.end.toString()
                    MonthlySummary(period, dao.totalBetween("expense", start, end), dao.totalBetween("income", start, end),
                        dao.countBetween(start, end), dao.totalBetween("expense", period.previousStart.toString(), period.previousEnd.toString()),
                        dao.categoriesBetween(start, end),
                        dao.largestBetween(start, end), db.budgetDao().get(month.toString())?.amountCents)
                }
            }.flowOn(kotlinx.coroutines.Dispatchers.IO)
    }
}
