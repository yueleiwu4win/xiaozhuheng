package com.example.bookkeeping.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bookkeeping.data.db.BillWithCategory
import com.example.bookkeeping.data.db.entity.CategoryEntity
import com.example.bookkeeping.data.prefs.SettingsRepository
import com.example.bookkeeping.data.repo.BillRepository
import com.example.bookkeeping.data.repo.BillType
import com.example.bookkeeping.data.repo.CategoryRepository
import java.time.YearMonth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(
    private val billRepository: BillRepository,
    private val categoryRepository: CategoryRepository,
    settingsRepository: SettingsRepository,
    private val budgetRepository: com.example.bookkeeping.data.repo.BudgetRepository
) : ViewModel() {

    private val _yearMonth = MutableStateFlow(YearMonth.now())
    val yearMonth: StateFlow<YearMonth> = _yearMonth.asStateFlow()

    private val _filter = MutableStateFlow(MonthFilter())
    val filter: StateFlow<MonthFilter> = _filter.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val _deleting = MutableStateFlow(false)
    val deleting: StateFlow<Boolean> = _deleting.asStateFlow()

    /** 筛选区展示支出 + 收入两类启用分类。 */
    val categories: StateFlow<List<CategoryEntity>> =
        combine(
            categoryRepository.observeActive(BillType.EXPENSE),
            categoryRepository.observeActive(BillType.INCOME)
        ) { expense, income -> expense + income }
            .stateIn(viewModelScope, SharingStarted.Eagerly,
                categoryRepository.activeSnapshot(BillType.EXPENSE) + categoryRepository.activeSnapshot(BillType.INCOME))

    /** 当月账单（支出 + 收入混排，可叠加分类 / 收支 / 关键字筛选）。 */
    val bills: StateFlow<List<BillWithCategory>> =
        combine(_yearMonth, _filter) { ym, f -> ym to f }
            .flatMapLatest { (ym, f) ->
                billRepository.observeMonthFiltered(
                    ym.year, ym.monthValue, f.categoryId, f.type, f.keyword
                )
            }
            .stateIn(viewModelScope, SharingStarted.Eagerly, billRepository.monthSnapshot(_yearMonth.value))

    // 月度概览独立于筛选结果；筛选时另显示结果合计。
    val monthSummary: StateFlow<MonthSummary> = _yearMonth
        .flatMapLatest { ym ->
            billRepository.observeMonthFiltered(ym.year, ym.monthValue, null, null, null)
        }
        .map(::summarizeBills)
        .stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            summarizeBills(billRepository.monthSnapshot(_yearMonth.value))
        )

    val budget = _yearMonth.flatMapLatest { ym -> budgetRepository.observe(ym).map { ym to it } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val settings = settingsRepository.settings

    fun prevMonth() {
        _yearMonth.value = _yearMonth.value.minusMonths(1)
    }

    fun nextMonth() {
        _yearMonth.value = _yearMonth.value.plusMonths(1)
    }

    /** 月份选择器确认后设置到某月。 */
    fun setYearMonth(ym: YearMonth) {
        _yearMonth.value = ym
    }

    fun setCategoryFilter(categoryId: Long?) {
        val category = categories.value.firstOrNull { it.id == categoryId }
        _filter.value = _filter.value.withCategory(category)
    }

    fun setTypeFilter(type: String?) {
        _filter.value = _filter.value.withType(type, categories.value)
    }

    fun setLedgerCategoryFilter(categoryId: Long?) {
        _filter.value = _filter.value.withLedgerCategory(categories.value.firstOrNull { it.id == categoryId })
    }

    fun setKeyword(keyword: String) {
        _filter.value = _filter.value.copy(keyword = keyword)
    }

    fun resetFilters() {
        _filter.value = MonthFilter()
    }

    /** 二次确认后删除；成功静默更新，编辑入口通过回调返回原列表。 */
    fun deleteBill(id: Long, onDeleted: () -> Unit = {}) {
        if (_deleting.value) return
        _deleting.value = true
        viewModelScope.launch {
            try {
                billRepository.getById(id) ?: error("账单不存在")
                billRepository.delete(id)
                onDeleted()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _message.value = "删除失败：${e.message ?: "未知错误"}"
            } finally {
                _deleting.value = false
            }
        }
    }

    fun consumeMessage() {
        _message.value = null
    }

}
