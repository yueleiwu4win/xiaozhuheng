package com.example.bookkeeping.data.repo

import com.example.bookkeeping.data.db.dao.BudgetDao
import com.example.bookkeeping.data.db.entity.MonthlyBudget
import java.math.BigDecimal
import java.time.YearMonth

fun parseBudgetAmount(text: String): Long? = runCatching {
    require(Regex("[0-9]{1,8}(\\.[0-9]{1,2})?").matches(text.trim()))
    BigDecimal(text.trim()).movePointRight(2).longValueExact().also { require(it in 1..MAX_AMOUNT_CENTS) }
}.getOrNull()

class BudgetRepository(private val dao: BudgetDao) {
    fun observe(month: YearMonth) = dao.observe(month.toString())
    suspend fun get(month: YearMonth) = dao.get(month.toString())
    suspend fun set(month: YearMonth, cents: Long) {
        require(cents in 1..MAX_AMOUNT_CENTS)
        dao.put(MonthlyBudget(month.toString(), cents, System.currentTimeMillis()))
    }
    suspend fun remove(month: YearMonth) = dao.delete(month.toString())
}
