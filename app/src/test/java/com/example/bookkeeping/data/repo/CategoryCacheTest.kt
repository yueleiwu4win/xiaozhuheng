package com.example.bookkeeping.data.repo

import com.example.bookkeeping.data.db.dao.CategoryDao
import com.example.bookkeeping.data.db.entity.CategoryEntity
import java.lang.reflect.Proxy
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Test

class CategoryCacheTest {
    @Test fun emptyLoadedDatabaseIsDifferentFromUnloadedCache() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val repository = repository(MutableStateFlow(emptyList()))
            assertNull(repository.cachedCategories.value)
            repository.primeCache(scope)
            assertEquals(emptyList<CategoryEntity>(), repository.awaitReady())
        } finally { scope.cancel() }
    }

    @Test fun activeReadersShareCacheAndKeepDatabaseOrder() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val rows = listOf(category(2), category(1), category(3, "income"), category(4).copy(isArchived = true))
            val repository = repository(MutableStateFlow(rows))
            repository.primeCache(scope)
            assertEquals(listOf(2L, 1L), repository.observeActive("expense").first().map { it.id })
            assertEquals(listOf(3L), repository.activeSnapshot("income").map { it.id })
        } finally { scope.cancel() }
    }

    @Test fun editArchiveAndBackupReplacementUpdateExistingCache() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val changes = MutableStateFlow(listOf(category(1)))
            val repository = repository(changes)
            repository.primeCache(scope)
            changes.value = listOf(category(1).copy(name = "新名称"))
            assertEquals("新名称", repository.activeSnapshot("expense").single().name)
            changes.value = listOf(category(1).copy(isArchived = true))
            assertTrue(repository.activeSnapshot("expense").isEmpty())
            changes.value = listOf(category(9, "income"))
            assertEquals(9L, repository.cachedCategories.value!!.single().id)
        } finally { scope.cancel() }
    }

    private fun repository(changes: MutableStateFlow<List<CategoryEntity>>): CategoryRepository {
        val dao = Proxy.newProxyInstance(CategoryDao::class.java.classLoader, arrayOf(CategoryDao::class.java)) { _, method, _ ->
            if (method.name == "observeAll") changes else error("Unexpected database query: ${method.name}")
        } as CategoryDao
        return CategoryRepository(dao)
    }

    private fun category(id: Long, type: String = "expense") = CategoryEntity(
        id = id, name = "分类$id", type = type, icon = "food", color = "#214F43", sortOrder = id.toInt()
    )
}
