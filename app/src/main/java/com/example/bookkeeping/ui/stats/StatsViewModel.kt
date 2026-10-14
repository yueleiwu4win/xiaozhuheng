package com.example.bookkeeping.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bookkeeping.data.prefs.SettingsRepository
import com.example.bookkeeping.data.repo.BillRepository
import com.example.bookkeeping.data.repo.BillType
import java.time.YearMonth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*

@OptIn(ExperimentalCoroutinesApi::class)
class StatsViewModel(private val billRepository: BillRepository, settingsRepository: SettingsRepository) : ViewModel() {
    private val _selection = MutableStateFlow(StatsSelection())
    val selection = _selection.asStateFlow()
    val settings = settingsRepository.settings
    val availableYears = combine(billRepository.observeYears(), _selection) { years, selected ->
        (years + selected.year + YearMonth.now().year).distinct().sortedDescending()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf(YearMonth.now().year))

    // 将查询条件随结果一起发出，UI 只展示与当前收支类型和期间匹配的结果。
    val monthly: StateFlow<MonthStats?> = _selection.flatMapLatest { selected ->
        val ym = selected.month
        val previous = ym.minusMonths(1)
        combine(
            billRepository.observeCategoryTotals(selected.type, ym.year, ym.monthValue),
            billRepository.observeDailyTotals(selected.type, ym.year, ym.monthValue),
            billRepository.observeTotal(selected.type, ym.year, ym.monthValue),
            billRepository.observeTotal(selected.type, previous.year, previous.monthValue),
            billRepository.observeMonthCount(selected.type, ym.year, ym.monthValue)
        ) { cats, days, total, prev, count ->
            MonthStats(selected, cats, days, total, prev, count, null)
        }.combine(billRepository.observeMax(selected.type, ym.year, ym.monthValue)) { stats, max ->
            stats.copy(maximum = max)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val annual: StateFlow<YearStats?> = _selection.map { it.year to it.type }.distinctUntilChanged().flatMapLatest { (year, type) ->
        combine(
            billRepository.observeMonthlyTotals(year, type),
            billRepository.observeCategoryTotals(type, year, null),
            billRepository.observeYearlyTotals(type),
            billRepository.observeCategoryYearTotals(type)
        ) { months, cats, years, catYears -> YearStats(year, type, completeMonths(months), cats, years, catYears) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun setType(type: String) {
        if (type == BillType.EXPENSE || type == BillType.INCOME) _selection.value = _selection.value.copy(type = type)
    }
    fun setYear(year: Int) { _selection.value = _selection.value.copy(year = year) }
    fun prevYear() { setYear((_selection.value.year - 1).coerceAtLeast(1)) }
    fun nextYear() { setYear((_selection.value.year + 1).coerceAtMost(YearMonth.now().year)) }
    fun setYearMonth(month: YearMonth) { _selection.value = _selection.value.copy(month = month, year = month.year) }
    fun prevMonth() { setYearMonth(_selection.value.month.minusMonths(1)) }
    fun nextMonth() {
        val next = _selection.value.month.plusMonths(1)
        if (next <= YearMonth.now()) setYearMonth(next)
    }
    fun goCurrentMonth() { setYearMonth(YearMonth.now()) }
}
