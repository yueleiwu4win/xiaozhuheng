package com.example.bookkeeping.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 分类表。
 * type: "expense" | "income"
 * icon: 内置图标 key（见 util/CategoryIcons.kt）
 * color: 十六进制颜色，如 "#FF7043"
 */
@Entity(
    tableName = "category",
    indices = [Index(value = ["type", "is_archived", "sort_order"], name = "idx_category_type_sort")]
)
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: String,
    val icon: String,
    val color: String,
    @ColumnInfo(name = "sort_order") val sortOrder: Int,
    @ColumnInfo(name = "is_preset") val isPreset: Boolean = false,
    @ColumnInfo(name = "is_archived") val isArchived: Boolean = false,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)