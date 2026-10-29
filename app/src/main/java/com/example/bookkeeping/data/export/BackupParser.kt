package com.example.bookkeeping.data.export

import com.example.bookkeeping.data.db.entity.BillEntity
import com.example.bookkeeping.data.db.entity.CategoryEntity
import com.example.bookkeeping.data.repo.BillType
import com.example.bookkeeping.data.repo.MAX_AMOUNT_CENTS
import org.json.JSONObject
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate

data class BackupSettings(val currency: String?, val weekStartMonday: Boolean?, val themeMode: String?, val background: String?, val themeStyle: String? = null, val reading: com.example.bookkeeping.data.prefs.ReadingOptions? = null)

data class PreparedBackup(
    val fileName: String, val version: Int, val exportedAt: Long?,
    val categories: List<CategoryEntity>, val bills: List<BillEntity>, val settings: BackupSettings?,
    val skippedCategories: Int, val skippedBills: Int,
    val budgets: List<com.example.bookkeeping.data.db.entity.MonthlyBudget> = emptyList()
) {
    val firstDate: String? = bills.minOfOrNull { it.date }
    val lastDate: String? = bills.maxOfOrNull { it.date }
}

/** 纯解析：预览和实际恢复使用同一份不可变数据，确认前不写入数据库。 */
object BackupParser {
    fun parse(text: String, fileName: String = "所选备份"): PreparedBackup {
        val root = JSONObject(text)
        val version = root.optInt("formatVersion", 0)
        require(version in 1..3 && root.optString("app") == "bookkeeping-android") { "不是有效的记账备份文件" }
        val catArray = root.optJSONArray("categories") ?: error("备份缺少分类数据")
        val cats = mutableListOf<CategoryEntity>()
        val categoryTypes = mutableMapOf<Long, String>()
        var skippedCats = 0
        val now = System.currentTimeMillis()
        for (i in 0 until catArray.length()) {
            val o = catArray.optJSONObject(i)
            val id = o?.optLong("id", 0) ?: 0L
            val name = o?.optString("name")?.trim().orEmpty()
            val type = o?.optString("type").orEmpty()
            if (o == null || id <= 0 || name.isEmpty() || type !in listOf(BillType.EXPENSE, BillType.INCOME) || id in categoryTypes) {
                skippedCats++; continue
            }
            categoryTypes[id] = type
            cats += CategoryEntity(
                id, name, type, o.optString("icon", "wallet"),
                o.optString("color").takeIf { Regex("^#(?:[0-9A-Fa-f]{6}|[0-9A-Fa-f]{8})$").matches(it) } ?: "#214F43",
                o.optInt("sortOrder", i + 1), o.optBoolean("isPreset", false), o.optBoolean("archived", false),
                o.optLong("createdAt", now).takeIf { it > 0L } ?: now
            )
        }
        require(cats.isNotEmpty()) { "备份中没有可用分类" }
        val billArray = root.optJSONArray("bills") ?: error("备份缺少账单数据")
        val bills = mutableListOf<BillEntity>()
        val usedIds = mutableSetOf<Long>()
        var skippedBills = 0
        for (i in 0 until billArray.length()) {
            val o = billArray.optJSONObject(i)
            if (o == null) { skippedBills++; continue }
            val type = o.optString("type")
            val categoryId = o.optLong("categoryId", 0L)
            val amount = runCatching {
                if (o.has("amountCents")) BigDecimal(o.get("amountCents").toString()).longValueExact()
                else BigDecimal(o.get("amount").toString()).movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact()
            }.getOrDefault(0L)
            val date = runCatching { LocalDate.parse(o.optString("date")) }.getOrNull()
            val id = o.optLong("id", 0L)
            if (type !in listOf(BillType.EXPENSE, BillType.INCOME) || categoryTypes[categoryId] != type ||
                amount !in 1..MAX_AMOUNT_CENTS || date == null || id < 0L || id > 0L && !usedIds.add(id)) {
                skippedBills++; continue
            }
            val created = o.optLong("createdAt", now).takeIf { it > 0L } ?: now
            bills += BillEntity(
                id, amount, type, categoryId, date.toString(), date.year, date.monthValue,
                if (o.isNull("note")) null else o.optString("note").trim().take(50).takeIf { it.isNotEmpty() },
                o.optString("currency", "CNY"), created, o.optLong("updatedAt", created).takeIf { it > 0L } ?: created
            )
        }
        // v1 备份允许省略账单 id；统一分配空闲 id，避免多个 0 或与显式 id 冲突。
        var nextId = 1L
        val normalized = bills.map { bill ->
            if (bill.id > 0L) bill else {
                while (nextId in usedIds) nextId++
                usedIds.add(nextId)
                bill.copy(id = nextId++)
            }
        }
        val settings = root.optJSONObject("settings")?.let {
            BackupSettings(
                it.optString("currency").trim().takeIf { c -> c.isNotEmpty() && c.length <= 8 },
                if (it.has("weekStartMonday")) it.optBoolean("weekStartMonday", true) else null,
                if (it.has("themeMode")) it.optString("themeMode") else null,
                if (it.has("background")) it.optString("background") else null,
                if (it.has("themeStyle")) it.optString("themeStyle") else null,
                it.optJSONObject("reading")?.toReadingOptions()
            )
        }
        val budgets = mutableListOf<com.example.bookkeeping.data.db.entity.MonthlyBudget>()
        if (version >= 3) {
            val a = root.getJSONArray("budgets")
            val months = mutableSetOf<String>()
            for (i in 0 until a.length()) {
                val o = a.getJSONObject(i)
                val month = o.getString("month")
                require(java.time.YearMonth.parse(month).toString() == month && months.add(month)) { "预算月份无效或重复" }
                val cents = BigDecimal(o.get("amountCents").toString()).longValueExact()
                require(cents in 1..MAX_AMOUNT_CENTS) { "预算金额无效" }
                budgets += com.example.bookkeeping.data.db.entity.MonthlyBudget(month, cents, o.getLong("updatedAt").also { require(it > 0) })
            }
        }
        return PreparedBackup(fileName, version, root.optLong("exportedAt", 0).takeIf { it > 0 },
            cats.toList(), normalized, settings, skippedCats, skippedBills, budgets)
    }
}
