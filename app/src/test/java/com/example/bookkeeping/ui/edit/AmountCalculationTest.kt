package com.example.bookkeeping.ui.edit

import org.junit.Assert.*
import org.junit.Test

class AmountCalculationTest {
    private fun input(text: String) = AmountInputState().also { state ->
        text.forEach { when (it) { '+' , '-' -> state.operator(it); '.' -> state.dot(); else -> state.digit(it) } }
    }
    @Test fun shoppingTotalsUseExactCents() {
        val state = input("12.30+8.20+0.10-1.60")
        assertEquals(1900L, state.totalCents())
        assertNull(state.validationError())
        assertEquals(30L, input("0.1+0.2").totalCents())
    }
    @Test fun operatorsAreLeftAssociativeAndReplacePendingOperator() {
        assertEquals(500L, input("10-8+3").totalCents())
        assertEquals("10-2", input("10+-2").display.value)
        assertEquals(800L, input("10+-2").totalCents())
    }
    @Test fun decimalAndIntegerLimitsApplyToEachOperand() {
        val state = input("123456789+1.239")
        assertEquals("12345678+1.23", state.display.value)
        assertEquals(1234567923L, state.totalCents())
        assertEquals("1+0.5", input("1+.5").display.value)
        assertEquals("1+2", input("1+0002").display.value)
    }
    @Test fun trailingOperatorCannotSaveAndBackspaceCanUndoIt() {
        val state = input("12+3-")
        assertEquals(1500L, state.totalCents())
        assertNotNull(state.validationError())
        state.backspace()
        assertEquals("12+3", state.display.value)
        assertNull(state.validationError())
        state.backspace(); state.backspace()
        assertEquals("12", state.display.value)
    }
    @Test fun zeroNegativeAndOverflowResultsCannotSave() {
        listOf("1-1", "1-2", "99999999.99+0.01").forEach { assertNotNull(input(it).validationError()) }
        assertNull(input("99999999.99+1-1").validationError())
        assertEquals(150L, input("1-2+2.5").totalCents())
    }
    @Test fun editingAndContinueEntryResetCalculation() {
        val state = input("12+3")
        state.restore(12345)
        assertEquals("123.45", state.display.value)
        assertFalse(state.hasCalculation)
        state.operator('+'); state.digit('5')
        assertEquals(12845L, state.totalCents())
        state.reset()
        assertEquals("0", state.display.value)
        assertTrue(state.isComplete)
    }
    @Test fun expressionLengthIsBoundedAndAlwaysParseable() {
        val state = AmountInputState()
        repeat(300) { state.operator('+'); state.digit('9'); state.totalCents() }
        assertTrue(state.display.value.length <= 160)
        assertTrue(state.totalCents() > 0L)
    }
}
