package com.example.bookkeeping.data.db

/** 统计查询返回的轻量 POJO（非实体，不参与 Room 映射持久化）。 */

/** 按年 / 按月聚合：label = 年份或月份。 */
data class LabelTotal(
    val label: Int,
    val totalCents: Long,
    val cnt: Int
)

/** 某年（或某月）内各类别总额，按总额降序。 */
data class CategoryTotal(
    val categoryId: Long,
    val name: String,
    val icon: String,
    val color: String,
    val totalCents: Long,
    val cnt: Int
)

/** 每个类别的年度总额（用于「类别逐年对比」多折线图）。 */
data class CategoryYearTotal(
    val categoryId: Long,
    val name: String,
    val color: String,
    val year: Int,
    val totalCents: Long
)