package com.example.bookkeeping.data.export

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class BackupParserTest {
    private fun budget(month: String = "2026-10", amount: Any = 500000L) = JSONObject().put("month", month).put("amountCents", amount).put("updatedAt", 1L)
    @Test fun version3KeepsBudgetsAndLegacyHasNone() {
        val parsed = BackupParser.parse(backup(version = 3).put("budgets", JSONArray().put(budget())).toString())
        assertEquals(500000L, parsed.budgets.single().amountCents)
        assertEquals("2026-10", parsed.budgets.single().month)
        assertTrue(BackupParser.parse(backup().toString()).budgets.isEmpty())
    }
    @Test fun invalidBudgetNeverPartiallyRestoresBackup() {
        listOf(budget("2026-13"), budget(amount = "0.5"), budget(amount = -1), budget(amount = "999999999999999999999")).forEach {
            assertTrue(runCatching { BackupParser.parse(backup(version = 3).put("budgets", JSONArray().put(it)).toString()) }.isFailure)
        }
        assertTrue(runCatching { BackupParser.parse(backup(version = 3).put("budgets", JSONArray().put(budget()).put(budget())).toString()) }.isFailure)
        assertTrue(runCatching { BackupParser.parse(backup(version = 3).toString()) }.isFailure)
    }
    @Test fun newThemeSettingSurvivesBackupParsingWithoutChangingBills() {
        val root = backup().put("settings", JSONObject().put("themeStyle", "song").put("themeMode", "dark"))
        val parsed = BackupParser.parse(root.toString())
        assertEquals("song", parsed.settings?.themeStyle)
        assertEquals("dark", parsed.settings?.themeMode)
        assertEquals(1234L, parsed.bills.single().amountCents)
    }

    @Test fun oldBackupDoesNotOverwriteStyleWithMissingField() {
        val root = backup().put("settings", JSONObject().put("themeMode", "light"))
        assertNull(BackupParser.parse(root.toString()).settings?.themeStyle)
    }

    private fun category(id: Long = 1L, type: String = "expense") = JSONObject()
        .put("id", id).put("name", "餐饮").put("type", type).put("icon", "wallet").put("color", "#214F43")
    private fun bill(id: Long? = 1L, type: String = "expense", categoryId: Long = 1L) = JSONObject()
        .apply { id?.let { put("id", it) } }.put("type", type).put("categoryId", categoryId)
        .put("amountCents", 1234L).put("date", "2026-10-13")
    private fun backup(bills: List<JSONObject> = listOf(bill()), categories: List<JSONObject> = listOf(category()), version: Int = 2) = JSONObject()
        .put("app", "bookkeeping-android").put("formatVersion", version)
        .put("categories", JSONArray(categories)).put("bills", JSONArray(bills))

    @Test fun validBackupKeepsIntegerCentsAndDateRange() {
        val root = backup(listOf(bill().put("amountCents", 9_999_999_999L), bill(2L).put("date", "2026-09-01")))
        val parsed = BackupParser.parse(root.toString(), "我的备份.json")
        assertEquals("我的备份.json", parsed.fileName)
        assertEquals(9_999_999_999L, parsed.bills.first().amountCents)
        assertEquals("2026-09-01", parsed.firstDate)
        assertEquals("2026-10-13", parsed.lastDate)
        assertEquals(0, parsed.skippedBills)
    }

    @Test fun legacyDecimalAmountsAndMissingIdsRemainCompatible() {
        val rows = listOf(bill(null).apply { remove("amountCents"); put("amount", 12.34) }, bill(null), bill(1L))
        val parsed = BackupParser.parse(backup(rows, version = 1).toString())
        assertEquals(1234L, parsed.bills.first().amountCents)
        assertEquals(3, parsed.bills.map { it.id }.distinct().size)
        assertTrue(parsed.bills.all { it.id > 0L })
        assertNull(parsed.settings)
    }

    @Test fun mismatchedCategoryTypeAndMissingCategoryAreExcluded() {
        val rows = listOf(bill(type = "income"), bill(2L, categoryId = 99L), bill(3L))
        val parsed = BackupParser.parse(backup(rows).toString())
        assertEquals(2, parsed.skippedBills)
        assertEquals(listOf(3L), parsed.bills.map { it.id })
    }

    @Test fun impossibleDateIsNotSilentlyNormalized() {
        val parsed = BackupParser.parse(backup(listOf(bill().put("date", "2026-02-30"))).toString())
        assertEquals(1, parsed.skippedBills)
        assertTrue(parsed.bills.isEmpty())
    }

    @Test fun fractionalCentsAreNotTruncatedIntoValidMoney() {
        val parsed = BackupParser.parse(backup(listOf(bill().put("amountCents", 1.5))).toString())
        assertEquals(1, parsed.skippedBills)
        assertTrue(parsed.bills.isEmpty())
    }

    @Test fun duplicateBillsAreReportedAndKeepOnlyOneRecord() {
        val parsed = BackupParser.parse(backup(listOf(bill(), bill())).toString())
        assertEquals(1, parsed.bills.size)
        assertEquals(1, parsed.skippedBills)
    }

    @Test fun invalidCategoriesAreReportedBeforeConfirmation() {
        val parsed = BackupParser.parse(backup(categories = listOf(category(), category(), category(2L).put("name", ""))).toString())
        assertEquals(1, parsed.categories.size)
        assertEquals(2, parsed.skippedCategories)
    }

    @Test fun nullNotesRemainNull() {
        val parsed = BackupParser.parse(backup(listOf(bill().put("note", JSONObject.NULL))).toString())
        assertNull(parsed.bills.single().note)
    }

    @Test fun emptyBackupIsValidForExplicitEmptyRestoreConfirmation() {
        val parsed = BackupParser.parse(backup(emptyList()).toString())
        assertTrue(parsed.bills.isEmpty())
        assertNull(parsed.firstDate)
    }

    @Test fun confirmationUsesParsedSnapshotEvenIfOriginalJsonChanges() {
        val root = backup().put("settings", JSONObject().put("currency", "USD").put("themeMode", "dark"))
        val parsed = BackupParser.parse(root.toString())
        root.getJSONArray("bills").getJSONObject(0).put("amountCents", 5000L)
        assertEquals(1234L, parsed.bills.single().amountCents)
        assertEquals("USD", parsed.settings?.currency)
    }

    @Test(expected = IllegalArgumentException::class) fun otherApplicationsCannotBeRestored() {
        BackupParser.parse(backup().put("app", "other").toString())
    }

    @Test(expected = IllegalArgumentException::class) fun futureVersionsAreRejected() {
        BackupParser.parse(backup(version = 4).toString())
    }
}
