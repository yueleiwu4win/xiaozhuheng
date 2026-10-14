package com.example.bookkeeping.ui.home

import com.example.bookkeeping.data.db.BillWithCategory
import com.example.bookkeeping.data.db.entity.CategoryEntity
import com.example.bookkeeping.data.repo.BillType

data class MonthSummary(
    val expenseCents: Long = 0,
    val expenseCount: Int = 0,
    val incomeCents: Long = 0,
    val incomeCount: Int = 0
) {
    val balanceCents: Long get() = incomeCents - expenseCents
}

fun summarizeBills(bills: List<BillWithCategory>): MonthSummary {
    var expense = 0L
    var income = 0L
    var expenseCount = 0
    var incomeCount = 0
    bills.forEach {
        if (it.bill.type == BillType.INCOME) {
            income += it.bill.amountCents
            incomeCount++
        } else {
            expense += it.bill.amountCents
            expenseCount++
        }
    }
    return MonthSummary(expense, expenseCount, income, incomeCount)
}

data class MonthFilter(
    val categoryId: Long? = null,
    val type: String? = null,
    val keyword: String = ""
) {
    val isActive: Boolean get() = type != null || categoryId != null || keyword.isNotBlank()

    fun withType(nextType: String?, categories: List<CategoryEntity>): MonthFilter {
        val category = categories.firstOrNull { it.id == categoryId }
        return copy(
            type = nextType,
            categoryId = categoryId.takeIf { category != null && (nextType == null || category.type == nextType) }
        )
    }

    fun withCategory(category: CategoryEntity?): MonthFilter =
        copy(categoryId = category?.id, type = category?.type ?: type)
}
