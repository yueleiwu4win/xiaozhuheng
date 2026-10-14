package com.example.bookkeeping.ui.edit

import com.example.bookkeeping.data.db.entity.CategoryEntity
import org.junit.Assert.*
import org.junit.Test

class EditCategoriesTest {
    private val food = category(1, "expense")
    private val travel = category(2, "expense")
    private val salary = category(3, "income")
    private val archived = category(4, "expense").copy(isArchived = true)
    private val all = listOf(food, travel, salary, archived)

    @Test fun rememberedCategoryComesFromVisibleSnapshot() {
        assertEquals(travel, chooseActiveCategory(all, "expense", travel.id))
    }

    @Test fun archivedRememberedCategoryFallsBackToFirstActive() {
        assertEquals(food, chooseActiveCategory(all, "expense", archived.id))
    }

    @Test fun wrongTypeAndDeletedCategoryNeverBecomeDefault() {
        assertEquals(food, chooseActiveCategory(all, "expense", salary.id))
        assertEquals(food, chooseActiveCategory(all, "expense", 999L))
    }

    @Test fun switchingToIncomeUsesIncomeCategory() {
        assertEquals(salary, chooseActiveCategory(all, "income", travel.id))
    }

    @Test fun emptyOrArchivedOnlyListHasNoSelection() {
        assertNull(chooseActiveCategory(emptyList(), "expense", 1L))
        assertNull(chooseActiveCategory(listOf(archived), "expense", archived.id))
    }

    private fun category(id: Long, type: String) = CategoryEntity(
        id = id, name = "分类$id", type = type, icon = "food", color = "#214F43", sortOrder = id.toInt()
    )
}
