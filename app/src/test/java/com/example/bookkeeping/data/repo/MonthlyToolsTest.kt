package com.example.bookkeeping.data.repo

import com.example.bookkeeping.data.db.CategoryTotal
import com.example.bookkeeping.ui.summary.SummaryPoster
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class MonthlyToolsTest {
    @Test fun budgetOnlyAcceptsPositiveExactCents() {
        assertEquals(123456L, parseBudgetAmount("1234.56"))
        assertEquals(9999999999L, parseBudgetAmount("99999999.99"))
        listOf("0", "-1", "1.001", "1e2", "100000000", "abc", "1+2", "").forEach { assertNull(it, parseBudgetAmount(it)) }
    }
    @Test fun currentMonthComparesSameDayAndClampsShortPreviousMonth() {
        val p = SummaryPeriod.of(YearMonth.of(2024, 3), LocalDate.of(2024, 3, 31))
        assertEquals(LocalDate.of(2024, 2, 29), p.previousEnd)
        assertEquals(LocalDate.of(2024, 3, 31), p.end)
    }
    @Test fun completedMonthUsesWholePreviousMonthAcrossYearBoundary() {
        val p = SummaryPeriod.of(YearMonth.of(2026, 1), LocalDate.of(2026, 10, 28))
        assertEquals(LocalDate.of(2025, 12, 1), p.previousStart)
        assertEquals(LocalDate.of(2025, 12, 31), p.previousEnd)
        assertEquals(LocalDate.of(2026, 1, 31), p.end)
    }
    @Test(expected = IllegalArgumentException::class) fun futureMonthIsRejected() {
        SummaryPeriod.of(YearMonth.of(2027, 1), LocalDate.of(2026, 10, 28))
    }
    private fun data(expense: Long = 123456, previous: Long = 0, count: Long = 1) = MonthlySummary(
        SummaryPeriod.of(YearMonth.of(2026, 10), LocalDate.of(2026, 10, 28)), expense, 300000, count, previous,
        if (expense == 0L) emptyList() else listOf(CategoryTotal(1, "餐饮", "food", "#214F43", expense, 1)), null, 500000)
    @Test fun zeroPreviousDoesNotGenerateInfinityOrMisleadingPercentage() {
        val text = data().changeText { it.toString() }
        assertTrue(text.contains("上期无支出")); assertFalse(text.contains("%"))
        assertEquals("两期均无支出", data(0).changeText { it.toString() })
    }
    @Test fun emptyAndIncomeOnlyMonthsHaveHonestInsights() {
        assertTrue(data(0, count = 0).insight().contains("还没有账单"))
        assertTrue(data(0).insight().contains("暂无支出"))
        assertTrue(data().insight().contains("100%"))
    }
    @Test fun hiddenPosterContainsNoRawAmounts() {
        val shown = SummaryPoster.lines(data(), "¥", false).joinToString()
        val hidden = SummaryPoster.lines(data(), "¥", true).joinToString()
        assertTrue(shown.contains("1,234.56"))
        listOf("1,234.56", "3,000.00", "5,000.00", "3,765.44").forEach { assertFalse(hidden.contains(it)) }
        assertTrue(hidden.contains("••••"))
    }
}
