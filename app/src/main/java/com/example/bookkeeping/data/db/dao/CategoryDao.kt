package com.example.bookkeeping.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.bookkeeping.data.db.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {

    /** 记账选择区 / 筛选列表使用的启用分类，按排序。 */
    @Query("SELECT * FROM category WHERE type = :type AND is_archived = 0 ORDER BY sort_order ASC, id ASC")
    fun observeActive(type: String): Flow<List<CategoryEntity>>

    /** 管理页使用：启用在前，归档在后。 */
    @Query("SELECT * FROM category ORDER BY is_archived ASC, sort_order ASC, id ASC")
    fun observeAll(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM category WHERE id = :id")
    suspend fun getById(id: Long): CategoryEntity?

    @Query("SELECT COUNT(*) FROM category")
    suspend fun count(): Int

    /** 名称查重：包含已归档分类，避免出现两个同名分类。 */
    @Query("SELECT COUNT(*) FROM category WHERE name = :name AND type = :type")
    suspend fun countByName(name: String, type: String): Int

    @Query("SELECT COALESCE(MAX(sort_order), 0) FROM category WHERE type = :type")
    suspend fun maxSortOrder(type: String): Int

    @Insert
    suspend fun insert(entity: CategoryEntity): Long

    @Insert
    suspend fun insertAll(entities: List<CategoryEntity>)

    @Update
    suspend fun update(entity: CategoryEntity)

    /** 批量重排：单事务，避免逐条 UPDATE 触发 N 次列表刷新。 */
    @Transaction
    suspend fun reorderAll(entities: List<CategoryEntity>) {
        entities.forEach { updateSortOrder(it.id, it.sortOrder) }
    }

    @Query("UPDATE category SET sort_order = :sortOrder WHERE id = :id")
    suspend fun updateSortOrder(id: Long, sortOrder: Int)

    @Query("DELETE FROM category WHERE id = :id")
    suspend fun deleteById(id: Long)

    /** 清空分类（导入 / 清空数据时使用，配合事务）。 */
    @Query("DELETE FROM category")
    suspend fun deleteAllCategories()

    @Query("SELECT * FROM category")
    suspend fun listAll(): List<CategoryEntity>
}
