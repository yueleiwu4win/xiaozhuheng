package com.example.bookkeeping.ui.home

import com.example.bookkeeping.data.db.BillWithCategory
import com.example.bookkeeping.data.db.entity.BillEntity
import com.example.bookkeeping.data.db.entity.CategoryEntity
import com.example.bookkeeping.data.repo.BillType
import org.junit.Assert.*
import org.junit.Test

class HomeModelsTest {
    private val expense = CategoryEntity(1L, "餐饮", BillType.EXPENSE, "restaurant", "#214F43", 0)
    private val income = CategoryEntity(2L, "工资", BillType.INCOME, "work", "#214F43", 0)
    private val categories = listOf(expense, income)

    private fun bill(cents: Long, category: CategoryEntity) = BillWithCategory(
        BillEntity(amountCents = cents, type = category.type, categoryId = category.id,
            date = "2026-10-13", year = 2026, month = 10, createdAt = 0L, updatedAt = 0L),
        category
    )

    @Test fun mixedRecordsHaveSeparateTotalsAndNegativeBalance() {
        val summary = summarizeBills(listOf(bill(101L, expense), bill(202L, expense), bill(100L, income)))
        assertEquals(303L, summary.expenseCents)
        assertEquals(2, summary.expenseCount)
        assertEquals(100L, summary.incomeCents)
        assertEquals(1, summary.incomeCount)
        assertEquals(-203L, summary.balanceCents)
    }

    @Test fun incomeOnlyDayDoesNotProduceExpenseCount() {
        val summary = summarizeBills(listOf(bill(12345L, income)))
        assertEquals(0, summary.expenseCount)
        assertEquals(0L, summary.expenseCents)
        assertEquals(12345L, summary.incomeCents)
    }

    @Test fun amountsBeyondIntRangeKeepCentsExact() {
        val summary = summarizeBills(listOf(bill(9_999_999_999L, expense), bill(1L, expense)))
        assertEquals(10_000_000_000L, summary.expenseCents)
    }

    @Test fun emptyMonthHasZeroTotals() {
        assertEquals(MonthSummary(), summarizeBills(emptyList()))
    }

    @Test fun changingTypeClearsIncompatibleCategoryAndPreservesSearch() {
        val result = MonthFilter(expense.id, BillType.EXPENSE, "午餐").withType(BillType.INCOME, categories)
        assertNull(result.categoryId)
        assertEquals(BillType.INCOME, result.type)
        assertEquals("午餐", result.keyword)
    }

    @Test fun changingTypePreservesCompatibleCategory() {
        val result = MonthFilter(income.id, null).withType(BillType.INCOME, categories)
        assertEquals(income.id, result.categoryId)
    }

    @Test fun selectingCategoryAlignsTypeInsteadOfReturningFalseEmptyResults() {
        val result = MonthFilter(type = BillType.EXPENSE, keyword = "奖金").withCategory(income)
        assertEquals(BillType.INCOME, result.type)
        assertEquals(income.id, result.categoryId)
        assertEquals("奖金", result.keyword)
    }

    @Test fun clearingCategoryKeepsSelectedType() {
        val result = MonthFilter(income.id, BillType.INCOME).withCategory(null)
        assertNull(result.categoryId)
        assertEquals(BillType.INCOME, result.type)
    }

    @Test fun whitespaceSearchIsNotAnActiveFilter() {
        assertFalse(MonthFilter(keyword = "   ").isActive)
        assertTrue(MonthFilter(keyword = "午餐").isActive)
        assertTrue(MonthFilter(type = BillType.INCOME).isActive)
    }

    @Test fun ledgerAllCategoriesClearsTypeButPreservesKeyword() {
        val result = MonthFilter(income.id, BillType.INCOME, "午餐").withLedgerCategory(null)
        assertNull(result.categoryId)
        assertNull(result.type)
        assertEquals("午餐", result.keyword)
    }

    @Test fun ledgerCategorySwitchCanCrossIncomeExpense() {
        val result = MonthFilter(income.id, BillType.INCOME).withLedgerCategory(expense)
        assertEquals(expense.id, result.categoryId)
        assertEquals(BillType.EXPENSE, result.type)
    }
}
