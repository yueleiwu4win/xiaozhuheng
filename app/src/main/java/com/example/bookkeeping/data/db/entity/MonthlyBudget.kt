package com.example.bookkeeping.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "monthly_budget")
data class MonthlyBudget(@PrimaryKey val month: String, val amountCents: Long, val updatedAt: Long)
