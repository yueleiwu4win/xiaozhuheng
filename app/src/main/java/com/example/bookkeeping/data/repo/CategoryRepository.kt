package com.example.bookkeeping.data.repo

import com.example.bookkeeping.data.db.dao.CategoryDao
import com.example.bookkeeping.data.db.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect

/**
 * 分类业务规则：
 * - 同类别下名称唯一（含已归档分类，避免出现两个同名分类）；
 * - 内置分类与有账单引用的分类不能硬删除，只能归档（历史统计不受影响）。
 */
class CategoryRepository(private val dao: CategoryDao) {

    // null 表示尚未读取；空列表表示已读取但没有分类，不能混为加载中。
    private val _cachedCategories = MutableStateFlow<List<CategoryEntity>?>(null)
    val cachedCategories = _cachedCategories.asStateFlow()
    private var cacheJob: Job? = null

    suspend fun primeCache(scope: CoroutineScope) {
        _cachedCategories.value = dao.observeAll().first()
        startObserving(scope)
    }

    fun startObserving(scope: CoroutineScope) {
        if (cacheJob?.isActive == true) return
        cacheJob = scope.launch {
            dao.observeAll().collect { _cachedCategories.value = it }
        }
    }

    suspend fun awaitReady(): List<CategoryEntity> = cachedCategories.filterNotNull().first()

    fun activeSnapshot(type: String): List<CategoryEntity> =
        cachedCategories.value.orEmpty().filter { !it.isArchived && it.type == type }

    fun observeActive(type: String): Flow<List<CategoryEntity>> = observeAll()
        .map { all -> all.filter { !it.isArchived && it.type == type } }.distinctUntilChanged()

    fun observeAll(): Flow<List<CategoryEntity>> = cachedCategories.filterNotNull()

    suspend fun getById(id: Long): CategoryEntity? = dao.getById(id)

    suspend fun listAll(): List<CategoryEntity> = dao.listAll()

    /** 新增分类；名称为空或重名时抛 IllegalArgumentException（文案可直接展示）。 */
    suspend fun create(name: String, type: String, icon: String, color: String): CategoryEntity {
        val trimmed = name.trim()
        require(trimmed.isNotEmpty()) { "名称不能为空" }
        require(dao.countByName(trimmed, type) == 0) { "已存在同名分类「$trimmed」" }
        val entity = CategoryEntity(
            name = trimmed,
            type = type,
            icon = icon,
            color = color,
            sortOrder = dao.maxSortOrder(type) + 1
        )
        dao.insert(entity)
        return entity
    }

    suspend fun rename(id: Long, name: String): CategoryEntity {
        val entity = dao.getById(id) ?: error("分类不存在")
        val trimmed = name.trim()
        require(trimmed.isNotEmpty()) { "名称不能为空" }
        require(dao.countByName(trimmed, entity.type) == 0 || trimmed == entity.name) {
            "已存在同名分类「$trimmed」"
        }
        val updated = entity.copy(name = trimmed)
        dao.update(updated)
        return updated
    }

    /**
     * 名称 + 图标 + 颜色一次性更新（避免"名字没改成、图标却改了"的半成功状态）。
     * 校验失败时抛异常且不写库。
     */
    suspend fun update(id: Long, name: String, icon: String, color: String): CategoryEntity {
        val entity = dao.getById(id) ?: error("分类不存在")
        val trimmed = name.trim()
        require(trimmed.isNotEmpty()) { "名称不能为空" }
        require(dao.countByName(trimmed, entity.type) == 0 || trimmed == entity.name) {
            "已存在同名分类「$trimmed」"
        }
        val updated = entity.copy(name = trimmed, icon = icon, color = color)
        dao.update(updated)
        return updated
    }

    suspend fun setArchived(id: Long, archived: Boolean) {
        val entity = dao.getById(id) ?: error("分类不存在")
        dao.update(entity.copy(isArchived = archived))
    }

    /**
     * 删除：内置分类或被账单引用时改为归档。
     * @return true = 已硬删除；false = 改为归档
     */
    suspend fun delete(id: Long, billCount: Int): Boolean {
        val entity = dao.getById(id) ?: error("分类不存在")
        if (entity.isPreset || billCount > 0) {
            dao.update(entity.copy(isArchived = true))
            return false
        }
        dao.deleteById(id)
        return true
    }

    /** 按新顺序重排（单事务批量写入）。 */
    suspend fun reorder(ordered: List<CategoryEntity>) {
        val updates = ordered.mapIndexedNotNull { index, entity ->
            val target = index + 1
            if (entity.sortOrder != target) entity.copy(sortOrder = target) else null
        }
        if (updates.isNotEmpty()) dao.reorderAll(updates)
    }
}
