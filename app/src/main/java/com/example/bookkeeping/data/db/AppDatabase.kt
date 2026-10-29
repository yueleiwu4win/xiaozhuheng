package com.example.bookkeeping.data.db

import com.example.bookkeeping.data.db.entity.MonthlyBudget
import com.example.bookkeeping.data.db.dao.BudgetDao
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.bookkeeping.data.db.entity.BillEntity
import com.example.bookkeeping.data.db.entity.CategoryEntity
import com.example.bookkeeping.data.db.dao.BillDao
import com.example.bookkeeping.data.db.dao.CategoryDao

@Database(
    entities = [CategoryEntity::class, BillEntity::class, MonthlyBudget::class],
    version = 2,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun categoryDao(): CategoryDao
    abstract fun budgetDao(): BudgetDao
    abstract fun billDao(): BillDao

    companion object {
        const val NAME = "bookkeeping.db"
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `monthly_budget` (`month` TEXT NOT NULL, `amountCents` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`month`))")
            }
        }
    }
}
