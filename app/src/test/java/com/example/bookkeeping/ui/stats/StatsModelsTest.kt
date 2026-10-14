package com.example.bookkeeping.ui.stats

import com.example.bookkeeping.data.db.LabelTotal
import org.junit.Assert.*
import org.junit.Test

class StatsModelsTest {
    @Test fun missingMonthsAreZeroFilledInCalendarOrder() {
        val result = completeMonths(listOf(LabelTotal(12, 1200L, 2), LabelTotal(2, 200L, 1)))
        assertEquals((1..12).toList(), result.map { it.label })
        assertEquals(0L, result.first().totalCents)
        assertEquals(200L, result[1].totalCents)
        assertEquals(1200L, result.last().totalCents)
        assertEquals(1400L, result.sumOf { it.totalCents })
    }
    @Test fun changeFromZeroHasNoMisleadingPercentage() {
        assertNull(percentChange(100L, 0L))
        assertNull(percentChange(0L, 0L))
    }
    @Test fun incomeAndExpenseUseTheSameComparisonRules() {
        assertEquals("↑50.0%", percentChange(150L, 100L))
        assertEquals("↓50.0%", percentChange(50L, 100L))
        assertEquals("0.0%", percentChange(100L, 100L))
    }
}
