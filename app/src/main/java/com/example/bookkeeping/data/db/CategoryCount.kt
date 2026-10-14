package com.example.bookkeeping.data.db

/** 每个分类的账单数（分类管理页用，一条 GROUP BY 查询取回）。 */
data class CategoryCount(
    val categoryId: Long,
    val cnt: Int
)
