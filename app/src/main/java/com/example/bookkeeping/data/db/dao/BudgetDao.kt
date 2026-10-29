package com.example.bookkeeping.data.db.dao

import androidx.room.*
import com.example.bookkeeping.data.db.entity.MonthlyBudget
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {
    @Query("SELECT * FROM monthly_budget WHERE month = :month")
    fun observe(month: String): Flow<MonthlyBudget?>
    @Query("SELECT * FROM monthly_budget WHERE month = :month")
    suspend fun get(month: String): MonthlyBudget?
    @Query("SELECT * FROM monthly_budget ORDER BY month")
    suspend fun all(): List<MonthlyBudget>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(budget: MonthlyBudget)
    @Query("DELETE FROM monthly_budget WHERE month = :month")
    suspend fun delete(month: String)
    @Query("DELETE FROM monthly_budget")
    suspend fun deleteAll()
}
