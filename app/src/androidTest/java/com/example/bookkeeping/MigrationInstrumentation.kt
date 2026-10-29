package com.example.bookkeeping

import android.app.Activity
import android.app.Instrumentation
import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import androidx.room.Room
import com.example.bookkeeping.data.db.AppDatabase
import com.example.bookkeeping.data.db.entity.MonthlyBudget
import com.example.bookkeeping.data.repo.BackupRepository
import kotlinx.coroutines.runBlocking
import org.json.JSONObject

/** Dependency-free device test runner. Uses a separate database, never user data.
 * adb shell am instrument -w com.example.bookkeeping.test/com.example.bookkeeping.MigrationInstrumentation
 */
class MigrationInstrumentation : Instrumentation() {
    override fun onCreate(arguments: Bundle?) { super.onCreate(arguments); start() }
    override fun onStart() {
        val name = "migration-verification.db"
        var room: AppDatabase? = null
        try {
            targetContext.deleteDatabase(name)
            val schema = JSONObject(context.assets.open("com.example.bookkeeping.data.db.AppDatabase/1.json").bufferedReader().use { it.readText() }).getJSONObject("database")
            SQLiteDatabase.openOrCreateDatabase(targetContext.getDatabasePath(name), null).use { db ->
                val entities = schema.getJSONArray("entities")
                for (i in 0 until entities.length()) {
                    val entity = entities.getJSONObject(i)
                    val table = entity.getString("tableName")
                    db.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", table))
                    val indices = entity.getJSONArray("indices")
                    for (j in 0 until indices.length()) db.execSQL(indices.getJSONObject(j).getString("createSql").replace("\${TABLE_NAME}", table))
                }
                db.execSQL("INSERT INTO category VALUES (1, '餐饮', 'expense', 'food', '#214F43', 0, 1, 0, 1)")
                db.execSQL("INSERT INTO bill (id,amount_cents,type,category_id,date,year,month,note,currency,created_at,updated_at) VALUES (1,12345,'expense',1,'2026-10-28',2026,10,'migration','CNY',1,1)")
                db.version = 1
            }
            val db = Room.databaseBuilder(targetContext, AppDatabase::class.java, name).addMigrations(AppDatabase.MIGRATION_1_2).build()
            room = db
            runBlocking {
                check(db.billDao().getById(1)?.amountCents == 12345L)
                check(db.categoryDao().listAll().single().name == "餐饮")
                db.budgetDao().put(MonthlyBudget("2026-10", 300000, 1))
                val categories = db.categoryDao().listAll()
                val bill = db.billDao().getById(1)!!
                // Force a foreign-key failure after deleting existing data; transaction must restore all three tables.
                check(runCatching { BackupRepository(db).restoreAll(categories, listOf(bill.copy(categoryId = 999)), emptyList()) }.isFailure)
                check(db.budgetDao().get("2026-10")?.amountCents == 300000L)
                check(db.billDao().getById(1)?.amountCents == 12345L)
                BackupRepository(db).restoreAll(categories, listOf(bill), listOf(MonthlyBudget("2026-09", 200000, 1)))
                check(db.budgetDao().all().single().month == "2026-09")
            }
            finish(Activity.RESULT_OK, Bundle().apply { putString("result", "PASS: Room v1-v2 migration, existing records, atomic restore and rollback") })
        } catch (e: Throwable) {
            finish(Activity.RESULT_CANCELED, Bundle().apply { putString("error", e.stackTraceToString()) })
        } finally { room?.close(); targetContext.deleteDatabase(name) }
    }
}
