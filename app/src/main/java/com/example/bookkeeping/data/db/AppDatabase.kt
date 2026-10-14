package com.example.bookkeeping.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.bookkeeping.data.db.entity.BillEntity
import com.example.bookkeeping.data.db.entity.CategoryEntity
import com.example.bookkeeping.data.db.dao.BillDao
import com.example.bookkeeping.data.db.dao.CategoryDao

@Database(
    entities = [CategoryEntity::class, BillEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun categoryDao(): CategoryDao
    abstract fun billDao(): BillDao

    companion object {
        const val NAME = "bookkeeping.db"
    }
}