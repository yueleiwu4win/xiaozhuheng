package com.example.bookkeeping.ui.edit

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class EditDraftTest {
    private val original = EditDraft(12345L, 1L, "expense", LocalDate.of(2026, 10, 13), "午餐")

    @Test fun unchangedPrefilledRecordCanLeaveWithoutWarning() {
        assertFalse(original.copy().differsFrom(original))
    }

    @Test fun eachEditableFieldRequiresUnsavedChangesProtection() {
        listOf(
            original.copy(amountCents = 12346L), original.copy(categoryId = 2L),
            original.copy(type = "income"), original.copy(date = original.date.minusDays(1)),
            original.copy(note = "晚餐")
        ).forEach { assertTrue(it.differsFrom(original)) }
    }

    @Test fun restoringOriginalInputRemovesWarning() {
        val input = AmountInputState()
        input.restore(original.amountCents)
        input.backspace()
        assertTrue(original.copy(amountCents = input.totalCents()).differsFrom(original))
        input.digit('5')
        assertFalse(original.copy(amountCents = input.totalCents()).differsFrom(original))
    }

    @Test fun amountResetIsReadyForNextEntry() {
        val input = AmountInputState()
        input.restore(original.amountCents)
        input.reset()
        assertEquals("0", input.display.value)
        assertEquals(0L, input.totalCents())
    }

    @Test fun decimalEntryAndBackspaceKeepIntegerCents() {
        val input = AmountInputState()
        input.digit('1'); input.digit('2'); input.dot(); input.digit('3'); input.digit('4'); input.digit('5')
        assertEquals(1234L, input.totalCents())
        input.backspace()
        assertEquals(1230L, input.totalCents())
    }
}
