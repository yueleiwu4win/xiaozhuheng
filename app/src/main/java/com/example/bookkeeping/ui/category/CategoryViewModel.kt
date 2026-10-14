package com.example.bookkeeping.ui.category

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bookkeeping.data.repo.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class CategoryViewModel(private val categoryRepository: CategoryRepository, private val billRepository: BillRepository) : ViewModel() {
    val all = categoryRepository.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), categoryRepository.cachedCategories.value.orEmpty())
    val billCounts = billRepository.observeCountsByCategory().map { rows -> rows.associate { it.categoryId to it.cnt } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())
    private val _message = MutableStateFlow<String?>(null)
    val message = _message.asStateFlow()
    private val _busy = MutableStateFlow(false)
    val busy = _busy.asStateFlow()
    fun consumeMessage() { _message.value = null }

    private fun perform(onSuccess: () -> Unit = {}, onFailure: () -> Unit = {}, action: suspend () -> Unit) {
        if (_busy.value) return
        _busy.value = true
        _message.value = null
        viewModelScope.launch {
            try { action(); onSuccess()
            } catch (e: CancellationException) { throw e
            } catch (e: Exception) { _message.value = e.message?.takeUnless { it.contains("FOREIGN KEY", true) } ?: "操作失败，请重试"; onFailure()
            } finally { _busy.value = false }
        }
    }
    fun create(type: String, name: String, icon: String, color: String, onSuccess: () -> Unit) = perform(onSuccess) {
        require(type in listOf(BillType.EXPENSE, BillType.INCOME))
        categoryRepository.create(name, type, icon, color)
    }
    fun save(id: Long, name: String, icon: String, color: String, onSuccess: () -> Unit) = perform(onSuccess) {
        categoryRepository.update(id, name, icon, color)
    }
    fun toggleArchive(id: Long) = perform {
        val category = categoryRepository.getById(id) ?: error("分类不存在")
        categoryRepository.setArchived(id, !category.isArchived)
        _message.value = if (category.isArchived) "已恢复分类" else "已归档，历史账单保留"
    }
    fun delete(id: Long) = perform {
        val count = billRepository.countByCategory(id)
        val deleted = categoryRepository.delete(id, count)
        _message.value = if (deleted) "已删除分类" else "已归档，历史账单和统计保留"
    }
    fun reorder(type: String, orderedIds: List<Long>, onFailure: () -> Unit = {}) = perform(onFailure = onFailure) {
        val current = all.value.filter { it.type == type && !it.isArchived }
        require(isCompleteCategoryOrder(current.map { it.id }, orderedIds)) { "分类已变化，请重新排序" }
        val byId = current.associateBy { it.id }
        categoryRepository.reorder(orderedIds.map { byId.getValue(it) })
        _message.value = "排序已保存"
    }
}
