package com.example.bookkeeping.data.seed

import com.example.bookkeeping.data.db.entity.CategoryEntity

/**
 * 内置默认分类（对应 PRD 附录 A）。
 * 首次启动且分类表为空时写入，isPreset = true。
 * 内置分类不可硬删除（可改名 / 换图标 / 归档）。
 */
object DefaultCategories {

    data class Preset(
        val name: String,
        val icon: String,
        val color: String,
        val type: String
    )

    private val EXPENSE: List<Preset> = listOf(
        Preset("餐饮", "restaurant", "#FF7043", "expense"),
        Preset("交通", "directions_bus", "#42A5F5", "expense"),
        Preset("机票", "flight", "#4A90D9", "expense"),
        Preset("住宿", "hotel", "#AB47BC", "expense"),
        Preset("购物", "shopping_bag", "#EC407A", "expense"),
        Preset("娱乐", "sports_esports", "#FFA726", "expense"),
        Preset("医疗", "local_hospital", "#26A69A", "expense"),
        Preset("教育", "school", "#5C6BC0", "expense"),
        Preset("通讯", "phone_iphone", "#78909C", "expense"),
        Preset("居家", "home", "#8D6E63", "expense"),
        Preset("人情", "card_giftcard", "#EF5350", "expense"),
        Preset("旅行其他", "luggage", "#66BB6A", "expense"),
        Preset("其他", "more_horiz", "#90A4AE", "expense")
    )

    private val INCOME: List<Preset> = listOf(
        Preset("工资", "savings", "#26A69A", "income"),
        Preset("奖金", "emoji_events", "#FFA726", "income"),
        Preset("红包", "card_giftcard", "#EF5350", "income"),
        Preset("退款", "more_horiz", "#42A5F5", "income"),
        Preset("其他收入", "more_horiz", "#90A4AE", "income")
    )

    fun all(): List<Preset> = EXPENSE + INCOME

    fun toEntities(): List<CategoryEntity> =
        all().mapIndexed { index, p ->
            CategoryEntity(
                name = p.name,
                type = p.type,
                icon = p.icon,
                color = p.color,
                sortOrder = index + 1,
                isPreset = true
            )
        }
}