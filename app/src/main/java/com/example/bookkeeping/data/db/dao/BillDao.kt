package com.example.bookkeeping.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.bookkeeping.data.db.BillWithCategory
import com.example.bookkeeping.data.db.CategoryCount
import com.example.bookkeeping.data.db.CategoryTotal
import com.example.bookkeeping.data.db.CategoryYearTotal
import com.example.bookkeeping.data.db.LabelTotal
import com.example.bookkeeping.data.db.entity.BillEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BillDao {

    // ---------- 明细列表 ----------

    /**
     * 某月账单（支出 + 收入混排），可叠加：分类、收支类型、备注关键字筛选。
     * 按日期倒序、同一天按创建时间倒序。
     */
    @Transaction
    @Query(
        "SELECT * FROM bill WHERE year = :year AND month = :month " +
            "AND (:categoryId IS NULL OR category_id = :categoryId) " +
            "AND (:type IS NULL OR type = :type) " +
            "AND (:keyword IS NULL OR note LIKE '%' || :keyword || '%') " +
            "ORDER BY date DESC, created_at DESC, id DESC"
    )
    fun observeMonthFiltered(
        year: Int,
        month: Int,
        categoryId: Long?,
        type: String?,
        keyword: String?
    ): Flow<List<BillWithCategory>>

    @Transaction
    @Query("SELECT * FROM bill WHERE category_id = :categoryId AND type = :type AND year = :year " +
        "AND (:month IS NULL OR month = :month) ORDER BY date DESC, created_at DESC, id DESC")
    fun observeCategoryBills(categoryId: Long, type: String, year: Int, month: Int?): Flow<List<BillWithCategory>>

    /** 某月账单笔数（聚合查询，避免把整月明细读进内存）。 */
    @Query("SELECT COUNT(*) FROM bill WHERE type = :type AND year = :year AND month = :month")
    fun observeMonthCount(type: String, year: Int, month: Int): Flow<Long>

    @Query("SELECT * FROM bill WHERE id = :id")
    suspend fun getById(id: Long): BillEntity?

    @Transaction
    @Query("SELECT * FROM bill WHERE id = :id")
    suspend fun getWithCategory(id: Long): BillWithCategory?

    @Insert
    suspend fun insert(entity: BillEntity): Long

    @Insert
    suspend fun insertAll(entities: List<BillEntity>)

    @Update
    suspend fun update(entity: BillEntity)

    @Query("DELETE FROM bill WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM bill")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM bill WHERE category_id = :categoryId")
    suspend fun countByCategory(categoryId: Long): Int

    /** 每个分类的账单数（一条 GROUP BY，替代逐分类 COUNT 的 N+1）。 */
    @Query("SELECT category_id AS categoryId, COUNT(*) AS cnt FROM bill GROUP BY category_id")
    fun observeCountsByCategory(): Flow<List<CategoryCount>>

    // ---------- 统计 ----------

    @Query("SELECT DISTINCT year FROM bill ORDER BY year DESC")
    fun observeYears(): Flow<List<Int>>

    /** R1 年度消费趋势：每年总额。 */
    @Query(
        "SELECT year AS label, IFNULL(SUM(amount_cents), 0) AS totalCents, COUNT(*) AS cnt " +
            "FROM bill WHERE type = :type GROUP BY year ORDER BY year ASC"
    )
    fun observeYearlyTotals(type: String): Flow<List<LabelTotal>>

    /** R2 某年 12 个月消费（缺月补 0 在应用层处理）。 */
    @Query(
        "SELECT month AS label, IFNULL(SUM(amount_cents), 0) AS totalCents, COUNT(*) AS cnt " +
            "FROM bill WHERE type = :type AND year = :year GROUP BY month ORDER BY month ASC"
    )
    fun observeMonthlyTotals(year: Int, type: String): Flow<List<LabelTotal>>

    /** 某年某月每日消费（label = 日，1-31），用于月度每日消费折线图。 */
    @Query(
        "SELECT CAST(strftime('%d', date) AS INTEGER) AS label, " +
            "IFNULL(SUM(amount_cents), 0) AS totalCents, COUNT(*) AS cnt " +
            "FROM bill WHERE type = :type AND year = :year AND month = :month " +
            "GROUP BY date ORDER BY date ASC"
    )
    fun observeDailyTotals(type: String, year: Int, month: Int): Flow<List<LabelTotal>>

    /** R3 某年 / 某月各类别总额（month 传 null 表示整年），按总额降序。 */
    @Query(
        "SELECT b.category_id AS categoryId, c.name AS name, c.icon AS icon, c.color AS color, " +
            "IFNULL(SUM(b.amount_cents), 0) AS totalCents, COUNT(*) AS cnt " +
            "FROM bill b JOIN category c ON c.id = b.category_id " +
            "WHERE b.type = :type AND b.year = :year AND (:month IS NULL OR b.month = :month) " +
            "GROUP BY b.category_id, c.name, c.icon, c.color ORDER BY totalCents DESC"
    )
    fun observeCategoryTotals(type: String, year: Int, month: Int?): Flow<List<CategoryTotal>>

    /** R4 各类别逐年总额（用于多折线对比）。 */
    @Query(
        "SELECT b.category_id AS categoryId, b.year AS year, c.name AS name, c.color AS color, " +
            "IFNULL(SUM(b.amount_cents), 0) AS totalCents " +
            "FROM bill b JOIN category c ON c.id = b.category_id " +
            "WHERE b.type = :type GROUP BY b.category_id, b.year, c.name, c.color " +
            "ORDER BY c.name ASC, b.year ASC"
    )
    fun observeCategoryYearTotals(type: String): Flow<List<CategoryYearTotal>>

    /** 指定年月总额（用于同比 / 环比）。 */
    @Query(
        "SELECT IFNULL(SUM(amount_cents), 0) FROM bill WHERE type = :type AND year = :year AND month = :month"
    )
    fun observeTotal(type: String, year: Int, month: Int): Flow<Long>

    /** 指定年月最大单笔。 */
    @Query(
        "SELECT MAX(amount_cents) FROM bill WHERE type = :type AND year = :year AND month = :month"
    )
    fun observeMax(type: String, year: Int, month: Int): Flow<Long?>

    @Query("SELECT COUNT(*) FROM bill")
    fun observeCount(): Flow<Long>

    @Query("SELECT IFNULL(SUM(amount_cents), 0) FROM bill WHERE type = :type AND date BETWEEN :start AND :end")
    suspend fun totalBetween(type: String, start: String, end: String): Long
    @Query("SELECT COUNT(*) FROM bill WHERE date BETWEEN :start AND :end")
    suspend fun countBetween(start: String, end: String): Long
    @Transaction
    @Query("SELECT * FROM bill WHERE type = 'expense' AND date BETWEEN :start AND :end ORDER BY amount_cents DESC, date DESC, id DESC LIMIT 1")
    suspend fun largestBetween(start: String, end: String): BillWithCategory?

    @Query("SELECT b.category_id AS categoryId, c.name AS name, c.icon AS icon, c.color AS color, SUM(b.amount_cents) AS totalCents, COUNT(*) AS cnt FROM bill b JOIN category c ON c.id = b.category_id WHERE b.type = 'expense' AND b.date BETWEEN :start AND :end GROUP BY b.category_id ORDER BY totalCents DESC, b.category_id ASC")
    suspend fun categoriesBetween(start: String, end: String): List<CategoryTotal>

    // ---------- 导出 ----------

    @Transaction
    @Query("SELECT * FROM bill ORDER BY date ASC, created_at ASC, id ASC")
    suspend fun allWithCategory(): List<BillWithCategory>
}
