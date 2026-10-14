package com.example.bookkeeping.ui.edit

import com.example.bookkeeping.data.db.entity.CategoryEntity

/** 从同一份分类快照选取默认项，避免列表与选中分类分别查询。 */
fun chooseActiveCategory(categories: List<CategoryEntity>, type: String, preferredId: Long?): CategoryEntity? {
    val available = categories.filter { !it.isArchived && it.type == type }
    return available.firstOrNull { it.id == preferredId } ?: available.firstOrNull()
}
