package com.example.bookkeeping.data.db

import androidx.room.Embedded
import androidx.room.Relation
import com.example.bookkeeping.data.db.entity.BillEntity
import com.example.bookkeeping.data.db.entity.CategoryEntity

/** 账单 + 所属分类（JOIN 结果）。 */
data class BillWithCategory(
    @Embedded val bill: BillEntity,
    @Relation(parentColumn = "category_id", entityColumn = "id") val category: CategoryEntity
)