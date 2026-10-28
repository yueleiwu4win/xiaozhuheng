package com.example.bookkeeping.ui.edit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bookkeeping.data.db.entity.CategoryEntity
import com.example.bookkeeping.data.prefs.SettingsRepository
import com.example.bookkeeping.data.repo.BillRepository
import com.example.bookkeeping.data.repo.BillType
import com.example.bookkeeping.data.repo.CategoryRepository
import com.example.bookkeeping.util.DateUtils
import java.time.LocalDate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class EditViewModel(
    savedStateHandle: SavedStateHandle,
    private val billRepository: BillRepository,
    private val categoryRepository: CategoryRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    val billId: Long = savedStateHandle.get<Long>("billId") ?: -1L
    val isEdit: Boolean get() = billId > 0
    val input = AmountInputState()
    private val _billType = MutableStateFlow(BillType.EXPENSE)
    val billType = _billType.asStateFlow()
    val allCategories = categoryRepository.cachedCategories
    private val _selectedCategory = MutableStateFlow<CategoryEntity?>(null)
    val selectedCategory = _selectedCategory.asStateFlow()
    private val _date = MutableStateFlow(LocalDate.now())
    val date = _date.asStateFlow()
    private val _note = MutableStateFlow("")
    val note = _note.asStateFlow()
    private val _datePickerVisible = MutableStateFlow(false)
    val datePickerVisible = _datePickerVisible.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()
    private val _saving = MutableStateFlow(false)
    val saving = _saving.asStateFlow()
    private val _ready = MutableStateFlow(false)
    val ready = _ready.asStateFlow()
    private val _loadFailed = MutableStateFlow(false)
    val loadFailed = _loadFailed.asStateFlow()
    private val _saveResult = MutableStateFlow<SaveResult?>(null)
    val saveResult = _saveResult.asStateFlow()
    private var baseline: EditDraft? = null
    private val lastByType = mutableMapOf<String, CategoryEntity>()
    val settings = settingsRepository.settings

    init {
        val cached = allCategories.value
        if (!isEdit && cached != null) {
            // 启动已准备好基础数据，新记账在构造时一次完成初始表单。
            _selectedCategory.value = chooseActiveCategory(cached, BillType.EXPENSE, settingsRepository.lastUsed.value.categoryId)
            baseline = currentDraft()
            _ready.value = true
        } else {
            viewModelScope.launch {
                try {
                    settingsRepository.awaitLoaded()
                    val all = categoryRepository.awaitReady()
                    if (isEdit) {
                        val b = billRepository.getById(billId) ?: error("账单不存在")
                        input.restore(b.bill.amountCents)
                        _billType.value = b.bill.type
                        _selectedCategory.value = b.category
                        _date.value = LocalDate.parse(b.bill.date)
                        _note.value = b.bill.note ?: ""
                    } else {
                        _selectedCategory.value = chooseActiveCategory(all, BillType.EXPENSE, settingsRepository.lastUsed.value.categoryId)
                    }
                    baseline = currentDraft()
                    _ready.value = true
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    _loadFailed.value = true
                    _error.value = e.message ?: "读取记录失败"
                }
            }
        }
        viewModelScope.launch {
            combine(allCategories.filterNotNull(), _saving, _ready) { all, saving, ready -> Triple(all, saving, ready) }
                .collect { (all, saving, ready) ->
                    if (ready && !saving) {
                        val current = _selectedCategory.value
                        val updated = all.firstOrNull { it.id == current?.id && it.type == _billType.value }
                        // 历史账单可保留归档分类；新账单只选择当前可用分类。
                        _selectedCategory.value = if (isEdit && updated != null) updated
                            else chooseActiveCategory(all, _billType.value, current?.id)
                    }
                }
        }
    }

    private fun currentDraft() = EditDraft(
        input.totalCents(), _selectedCategory.value?.id, _billType.value, _date.value, _note.value
    )

    fun hasUnsavedChanges(): Boolean = baseline?.let { !input.isComplete || currentDraft().differsFrom(it) } ?: false

    fun selectType(type: String) {
        if (!_ready.value || _saving.value || type !in listOf(BillType.EXPENSE, BillType.INCOME) || type == _billType.value) return
        _selectedCategory.value?.let { lastByType[it.type] = it }
        _billType.value = type
        _selectedCategory.value = chooseActiveCategory(allCategories.value.orEmpty(), type, lastByType[type]?.id)
    }

    fun selectCategory(c: CategoryEntity) {
        if (!_ready.value || _saving.value || c.type != _billType.value) return
        _selectedCategory.value = c
        lastByType[c.type] = c
    }

    fun setDate(d: LocalDate) {
        if (_saving.value) return
        if (DateUtils.isFuture(d)) { _error.value = "暂不支持记未来日期的账"; return }
        _date.value = d
    }
    fun setDatePickerVisible(visible: Boolean) { _datePickerVisible.value = visible }
    fun setNote(text: String) { if (!_saving.value) _note.value = text.take(50) }
    fun clearError() { _error.value = null }
    fun consumeSaveResult() { _saveResult.value = null }

    fun save(continueEntry: Boolean = false) {
        if (_saving.value || !_ready.value || _saveResult.value != null) return
        val draft = currentDraft()
        if (draft.categoryId == null) { _error.value = "请选择分类"; return }
        if (_selectedCategory.value?.type != draft.type) { _error.value = "请选择对应的收支分类"; return }
        input.validationError()?.let { _error.value = it; return }
        if (draft.amountCents <= 0) { _error.value = "请输入大于 0 的金额"; return }
        _saving.value = true
        val currency = settings.value.currency
        viewModelScope.launch {
            try {
                if (isEdit) {
                    billRepository.update(billId, draft.amountCents, draft.categoryId, draft.date, draft.type, draft.note, currency)
                } else {
                    billRepository.create(draft.amountCents, draft.categoryId, draft.date, draft.type, draft.note, currency)
                }
                // 分类偏好写入失败不应把已保存账单报告成失败，避免用户重复记账。
                if (!isEdit && draft.type == BillType.EXPENSE) {
                    try {
                        settingsRepository.setLastUsed(draft.categoryId, DateUtils.format(draft.date))
                    } catch (e: CancellationException) { throw e } catch (_: Exception) { }
                }
                if (continueEntry && !isEdit) {
                    input.reset()
                    _note.value = ""
                    baseline = currentDraft()
                    _saveResult.value = SaveResult.CONTINUE
                } else {
                    baseline = draft
                    _saveResult.value = SaveResult.CLOSE
                }
            } catch (e: CancellationException) { throw e
            } catch (e: Exception) { _error.value = e.message ?: "保存失败，请重试"
            } finally { _saving.value = false }
        }
    }
}
