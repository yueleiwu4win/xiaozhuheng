package com.example.bookkeeping.ui.edit

import java.time.LocalDate

/** 比较用户实际修改的字段，自动预填分类不会触发退出确认。 */
data class EditDraft(
    val amountCents: Long,
    val categoryId: Long?,
    val type: String,
    val date: LocalDate,
    val note: String
) {
    fun differsFrom(original: EditDraft): Boolean = this != original
}

enum class SaveResult { CLOSE, CONTINUE }
