package com.example.bookkeeping.ui.category

import org.junit.Assert.*
import org.junit.Test

class CategoryOrderTest {
    @Test fun draggingBothDirectionsKeepsEveryCategoryExactlyOnce() {
        val ids = listOf(1L, 2L, 3L, 4L)
        assertEquals(listOf(2L, 3L, 4L, 1L), moveCategoryId(ids, 0, 3))
        assertEquals(listOf(4L, 1L, 2L, 3L), moveCategoryId(ids, 3, 0))
        assertEquals(ids, moveCategoryId(moveCategoryId(ids, 0, 3), 3, 0))
    }
    @Test fun offscreenOrInvalidDragTargetsKeepOrderUnchanged() {
        val ids = listOf(1L, 2L)
        assertEquals(ids, moveCategoryId(ids, -1, 1))
        assertEquals(ids, moveCategoryId(ids, 0, 2))
    }
    @Test fun reorderCannotDropDuplicateOrIntroduceOtherGroupIds() {
        val ids = listOf(1L, 2L, 3L)
        assertTrue(isCompleteCategoryOrder(ids, listOf(3L, 1L, 2L)))
        assertFalse(isCompleteCategoryOrder(ids, listOf(1L, 2L)))
        assertFalse(isCompleteCategoryOrder(ids, listOf(1L, 2L, 2L)))
        assertFalse(isCompleteCategoryOrder(ids, listOf(1L, 2L, 9L)))
    }
}
