package com.example.bookkeeping.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 账单表。
 * - 金额以「分」为单位存整数（amount_cents），全链路 Long 运算，展示层再格式化，避免浮点误差。
 * - date 为纯日期字符串 "yyyy-MM-dd"，精确到天，不受设备时区影响。
 * - year / month 为冗余列，配合索引让年度 / 月度聚合走覆盖索引。
 */
@Entity(
    tableName = "bill",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["date"], name = "idx_bill_date"),
        Index(value = ["year", "month"], name = "idx_bill_year_month"),
        Index(value = ["category_id", "date"], name = "idx_bill_category_date")
    ]
)
data class BillEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "amount_cents") val amountCents: Long,
    val type: String,
    @ColumnInfo(name = "category_id") val categoryId: Long,
    val date: String,
    val year: Int,
    val month: Int,
    val note: String? = null,
    val currency: String = "CNY",
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long
)