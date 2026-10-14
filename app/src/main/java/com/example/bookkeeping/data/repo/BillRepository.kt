package com.example.bookkeeping.data.repo

import com.example.bookkeeping.data.db.BillWithCategory
import com.example.bookkeeping.data.db.CategoryCount
import com.example.bookkeeping.data.db.CategoryTotal
import com.example.bookkeeping.data.db.CategoryYearTotal
import com.example.bookkeeping.data.db.LabelTotal
import com.example.bookkeeping.data.db.dao.BillDao
import com.example.bookkeeping.data.db.entity.BillEntity
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.flow.Flow
import java.time.YearMonth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect

object BillType {
    const val EXPENSE = "expense"
    const val INCOME = "income"
}

private val DATE_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

/** 金额上限：99,999,999.99 元。 */
const val MAX_AMOUNT_CENTS = 9_999_999_999L

class BillRepository(private val dao: BillDao) {

    data class CachedMonth(val month: YearMonth, val bills: List<BillWithCategory>)
    private val _cachedMonth = MutableStateFlow<CachedMonth?>(null)
    val cachedMonth = _cachedMonth.asStateFlow()
    private var cacheMonth: YearMonth? = null
    private var cacheJob: Job? = null

    suspend fun primeCurrentMonthCache(scope: CoroutineScope) {
        val month = YearMonth.now()
        val rows = dao.observeMonthFiltered(month.year, month.monthValue, null, null, null).first()
        _cachedMonth.value = CachedMonth(month, rows)
        startCurrentMonthCache(scope)
    }

    fun startCurrentMonthCache(scope: CoroutineScope) {
        if (cacheJob?.isActive == true) return
        val month = YearMonth.now()
        cacheMonth = month
        cacheJob = scope.launch {
            dao.observeMonthFiltered(month.year, month.monthValue, null, null, null)
                .collect { _cachedMonth.value = CachedMonth(month, it) }
        }
    }

    suspend fun awaitCurrentMonth(): CachedMonth = cachedMonth.filterNotNull().first()

    fun monthSnapshot(month: YearMonth): List<BillWithCategory> =
        cachedMonth.value?.takeIf { it.month == month }?.bills.orEmpty()

    // ---------- 明细 ----------

    /** 某月全部账单（支出 + 收入），可叠加分类 / 收支类型 / 备注关键字筛选。 */
    fun observeMonthFiltered(
        year: Int,
        month: Int,
        categoryId: Long?,
        type: String?,
        keyword: String?
    ): Flow<List<BillWithCategory>> {
        val query = keyword?.trim()?.takeIf { it.isNotEmpty() }
        return if (cacheMonth == YearMonth.of(year, month) && categoryId == null && type == null && query == null) {
            cachedMonth.filterNotNull().map { it.bills }
        } else dao.observeMonthFiltered(year, month, categoryId, type, query)
    }

    /** 某月账单笔数（聚合查询，不读取明细行）。 */
    fun observeMonthCount(type: String, year: Int, month: Int): Flow<Long> =
        dao.observeMonthCount(type, year, month)

    /** 每个分类的账单数（分类管理页用，一条 GROUP BY）。 */
    fun observeCountsByCategory(): Flow<List<CategoryCount>> = dao.observeCountsByCategory()

    suspend fun getById(id: Long): BillWithCategory? = dao.getWithCategory(id)

    fun observeCategoryBills(categoryId: Long, type: String, year: Int, month: Int?) =
        dao.observeCategoryBills(categoryId, type, year, month)

    /** 新增账单。金额必须 > 0，分类必须存在（外键保证）。 */
    suspend fun create(
        amountCents: Long,
        categoryId: Long,
        date: LocalDate,
        type: String = BillType.EXPENSE,
        note: String? = null,
        currency: String = "CNY"
    ): BillEntity {
        require(amountCents > 0) { "金额必须大于 0" }
        require(amountCents <= MAX_AMOUNT_CENTS) { "金额过大，超出可记录范围" }
        val now = System.currentTimeMillis()
        val entity = BillEntity(
            amountCents = amountCents,
            type = type,
            categoryId = categoryId,
            date = date.format(DATE_FMT),
            year = date.year,
            month = date.monthValue,
            note = note?.trim()?.takeIf { it.isNotEmpty() },
            currency = currency,
            createdAt = now,
            updatedAt = now
        )
        dao.insert(entity)
        return entity
    }

    suspend fun update(
        id: Long,
        amountCents: Long,
        categoryId: Long,
        date: LocalDate,
        type: String,
        note: String?,
        currency: String
    ) {
        val old = dao.getById(id) ?: error("账单不存在")
        require(amountCents > 0) { "金额必须大于 0" }
        require(amountCents <= MAX_AMOUNT_CENTS) { "金额过大，超出可记录范围" }
        dao.update(
            old.copy(
                amountCents = amountCents,
                type = type,
                categoryId = categoryId,
                date = date.format(DATE_FMT),
                year = date.year,
                month = date.monthValue,
                note = note?.trim()?.takeIf { it.isNotEmpty() },
                currency = currency,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun delete(id: Long) {
        dao.deleteById(id)
    }

    suspend fun countByCategory(categoryId: Long): Int = dao.countByCategory(categoryId)

    // ---------- 统计 ----------

    fun observeYears(): Flow<List<Int>> = dao.observeYears()
    fun observeYearlyTotals(type: String): Flow<List<LabelTotal>> = dao.observeYearlyTotals(type)
    fun observeMonthlyTotals(year: Int, type: String): Flow<List<LabelTotal>> = dao.observeMonthlyTotals(year, type)

    /** 某年某月每日消费（label = 日）。 */
    fun observeDailyTotals(type: String, year: Int, month: Int): Flow<List<LabelTotal>> =
        dao.observeDailyTotals(type, year, month)
    fun observeCategoryTotals(type: String, year: Int, month: Int?): Flow<List<CategoryTotal>> =
        dao.observeCategoryTotals(type, year, month)

    fun observeCategoryYearTotals(type: String): Flow<List<CategoryYearTotal>> =
        dao.observeCategoryYearTotals(type)

    fun observeTotal(type: String, year: Int, month: Int): Flow<Long> = dao.observeTotal(type, year, month)
    fun observeMax(type: String, year: Int, month: Int): Flow<Long?> = dao.observeMax(type, year, month)
    fun observeCount(): Flow<Long> = dao.observeCount()

    // ---------- 导出 ----------

    suspend fun allWithCategoryForExport(): List<BillWithCategory> = dao.allWithCategory()
}
