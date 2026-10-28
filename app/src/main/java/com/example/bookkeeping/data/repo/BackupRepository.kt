package com.example.bookkeeping.data.repo

import androidx.room.withTransaction
import com.example.bookkeeping.data.db.AppDatabase
import com.example.bookkeeping.data.db.entity.BillEntity
import com.example.bookkeeping.data.db.entity.CategoryEntity

/**
 * 破坏性批量写库（导入备份 / 清空数据）统一走这里，保证**单事务原子性**：
 * 中途异常或进程被杀时整体回滚，不会出现"账单删光了、分类只写了一半"的半套数据。
 *
 * 顺序固定：先删账单（解除 RESTRICT 外键引用）→ 再整体替换分类 → 最后写入账单。
 */
class BackupRepository(private val db: AppDatabase) {

    suspend fun restoreAll(categories: List<CategoryEntity>, bills: List<BillEntity>, budgets: List<com.example.bookkeeping.data.db.entity.MonthlyBudget> = emptyList()) {
        require(categories.map { it.id }.toSet().size == categories.size) { "备份中的分类 id 有重复" }
        require(bills.map { it.id }.toSet().size == bills.size) { "备份中的账单 id 有重复" }
        db.withTransaction {
            db.budgetDao().deleteAll()
            db.billDao().deleteAll()
            db.categoryDao().deleteAllCategories()
            db.categoryDao().insertAll(categories)
            db.billDao().insertAll(bills)
            budgets.forEach { db.budgetDao().put(it) }
        }
    }

    /** 清空全部账单并重建内置分类（单事务）。 */
    suspend fun clearAllData(defaultCategories: List<CategoryEntity>) {
        db.withTransaction {
            db.budgetDao().deleteAll()
            db.billDao().deleteAll()
            db.categoryDao().deleteAllCategories()
            db.categoryDao().insertAll(defaultCategories)
        }
    }
}
