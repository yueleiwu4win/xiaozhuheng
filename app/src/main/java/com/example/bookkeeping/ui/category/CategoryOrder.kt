package com.example.bookkeeping.ui.category

fun moveCategoryId(ids: List<Long>, from: Int, to: Int): List<Long> {
    if (from !in ids.indices || to !in ids.indices || from == to) return ids
    return ids.toMutableList().apply { add(to, removeAt(from)) }
}

fun isCompleteCategoryOrder(currentIds: List<Long>, orderedIds: List<Long>): Boolean =
    currentIds.size == orderedIds.size && orderedIds.distinct().size == orderedIds.size && currentIds.toSet() == orderedIds.toSet()
