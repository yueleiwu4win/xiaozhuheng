package com.example.bookkeeping.ui.chart

import org.junit.Assert.*
import org.junit.Test

class AxisLabelsTest {
    private fun assertReadable(result: List<AxisLabelPosition>, width: Float, gap: Float) {
        assertTrue(result.all { it.left >= 0f && it.left + it.width <= width })
        result.zipWithNext().forEach { (a, b) -> assertTrue(b.left >= a.left + a.width + gap) }
    }

    @Test fun decemberStaysVisibleWithoutCollidingWithNovember() {
        val result = placeAxisLabels((0..11).map { 48f + 24f * it }, List(12) { 32f }, 350f, 6f)
        assertReadable(result, 350f, 6f)
        assertEquals(11, result.last().index)
        assertFalse(result.any { it.index == 10 })
    }

    @Test fun narrowChartsAndLargeTextStayInsideEdges() {
        for (count in listOf(1, 2, 12, 31)) for (fontScale in listOf(1f, 1.5f, 3f)) {
            val centers = (0 until count).map { if (count == 1) 100f else it * 200f / (count - 1) }
            val result = placeAxisLabels(centers, List(count) { 28f * fontScale }, 200f, 6f)
            assertReadable(result, 200f, 6f)
            assertEquals(count - 1, result.last().index)
        }
    }

    @Test fun emptyOrUnrenderableLabelsAreSafe() {
        assertTrue(placeAxisLabels(emptyList(), emptyList(), 200f, 6f).isEmpty())
        assertTrue(placeAxisLabels(listOf(10f), listOf(500f), 200f, 6f).isEmpty())
    }
}
