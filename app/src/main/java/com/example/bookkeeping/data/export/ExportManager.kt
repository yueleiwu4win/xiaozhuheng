package com.example.bookkeeping.data.export

import android.content.Context
import androidx.room.withTransaction
import androidx.core.content.FileProvider
import com.example.bookkeeping.data.db.BillWithCategory
import com.example.bookkeeping.data.db.entity.BillEntity
import com.example.bookkeeping.data.db.entity.CategoryEntity
import com.example.bookkeeping.data.prefs.AppSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedWriter
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 数据导出：CSV（Excel 可直接打开）与 JSON（完整备份，formatVersion = 3，含预算）。
 * 全部为本地文件操作，不涉及网络。
 */
class ExportManager(private val context: Context, private val database: com.example.bookkeeping.data.db.AppDatabase) {

    enum class Kind { CSV, JSON }

    private val shareDir: File
        get() = File(context.cacheDir, "export").apply { mkdirs() }

    /** 写入用户通过 SAF 选择的 Uri。返回写出的账单行数。 */
    suspend fun writeToUri(
        kind: Kind,
        uri: android.net.Uri,
        categories: List<CategoryEntity>,
        bills: List<BillWithCategory>,
        settings: AppSettings
    ): Int = withContext(Dispatchers.IO) {
        when (kind) {
            Kind.CSV -> writeCsv(uri, bills)
            Kind.JSON -> writeJson(uri, categories, bills, settings)
        }
    }

    /** 写一份文件到应用缓存目录（用于系统分享面板）。 */
    suspend fun writeShareFile(
        kind: Kind,
        categories: List<CategoryEntity>,
        bills: List<BillWithCategory>,
        settings: AppSettings
    ): File = withContext(Dispatchers.IO) {
        // 顺手清理 24 小时前的分享临时文件（分享文件不能立刻删，又不能长期堆积）
        val cutoff = System.currentTimeMillis() - 24 * 3600_000L
        shareDir.listFiles()?.filter { it.isFile && it.lastModified() < cutoff }?.forEach { it.delete() }
        val file = File(shareDir, suggestFileName(kind))
        when (kind) {
            Kind.CSV -> {
                FileOutputStream(file).use { fos ->
                    fos.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())) // UTF-8 BOM
                    fos.flush()
                    val writer = BufferedWriter(OutputStreamWriter(fos, Charsets.UTF_8))
                    try {
                        writeCsvBody(writer, bills)
                    } finally {
                        writer.close()
                    }
                }
            }

            Kind.JSON -> {
                file.writeText(buildJson(categories, bills, settings), Charsets.UTF_8)
            }
        }
        file
    }

    /** 供 Compose 侧调用：缓存文件 → content:// 链接（只授予读取权限，不联网）。 */
    fun shareUriFor(file: File): android.net.Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

    fun mimeTypeOf(kind: Kind): String = when (kind) {
        Kind.CSV -> "text/csv"
        Kind.JSON -> "application/json"
    }

    fun shareTitleOf(kind: Kind, count: Int): String = when (kind) {
        Kind.CSV -> "记账数据（CSV，$count 条）"
        Kind.JSON -> "记账备份（JSON，$count 条）"
    }

    fun suggestFileName(kind: Kind): String {
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        return when (kind) {
            Kind.CSV -> "记账数据_${stamp}.csv"
            Kind.JSON -> "记账备份_${stamp}.json"
        }
    }

    /** 分享完成后清理临时文件（避免缓存目录长期堆积）。 */
    fun cleanupShareFile(file: File) {
        runCatching { file.delete() }
    }
    // ---------- CSV ----------

    private val CSV_HEADER = "日期,年,月,类别,金额,类型,币种,备注,记录时间"

    private suspend fun writeCsv(uri: android.net.Uri, bills: List<BillWithCategory>): Int {
        val stream: OutputStream? = context.contentResolver.openOutputStream(uri, "wt")
        stream?.use { out ->
            out.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())) // UTF-8 BOM，Excel 中文不乱码
            out.flush()
            val writer = BufferedWriter(OutputStreamWriter(out, Charsets.UTF_8))
            try {
                writeCsvBody(writer, bills)
            } finally {
                writer.close()
            }
        } ?: error("无法打开导出目标，请重试")
        return bills.size
    }

    private fun writeCsvBody(w: BufferedWriter, bills: List<BillWithCategory>) {
        w.write(CSV_HEADER)
        w.write("\r\n")
        val tsFmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        for (b in bills) {
            val bill = b.bill
            val fields: List<String> = listOf(
                bill.date,
                bill.year.toString(),
                bill.month.toString(),
                b.category.name,
                formatCents(bill.amountCents),
                if (bill.type == "expense") "支出" else "收入",
                bill.currency,
                bill.note.orEmpty(),
                tsFmt.format(Date(bill.createdAt))
            )
            w.write(fields.joinToString(",") { csvField(it) })
            w.write("\r\n")
        }
    }

    /**
     * RFC 4180：含逗号 / 引号 / 换行的字段用双引号包裹，内部引号翻倍。
     * 另外对 `= + - @` 及制表符开头的字段前置单引号，避免被 Excel / LibreOffice 当公式执行（CSV 注入）。
     */
    private fun csvField(value: String): String {
        val safe = if (value.isNotEmpty() && value[0] in charArrayOf('=', '+', '-', '@', '\t', '\r')) {
            "'$value"
        } else {
            value
        }
        return if (safe.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
            "\"${safe.replace("\"", "\"\"")}\""
        } else {
            safe
        }
    }

    // ---------- JSON ----------

    private suspend fun writeJson(
        uri: android.net.Uri,
        categories: List<CategoryEntity>,
        bills: List<BillWithCategory>,
        settings: AppSettings
    ): Int {
        val text = buildJson(categories, bills, settings)
        val stream: OutputStream? = context.contentResolver.openOutputStream(uri, "wt")
        stream?.use { out ->
            out.write(text.toByteArray(Charsets.UTF_8))
        } ?: error("无法打开导出目标，请重试")
        return bills.size
    }

    private suspend fun buildJson(
        categories: List<CategoryEntity>,
        bills: List<BillWithCategory>,
        settings: AppSettings
    ): String {
        val snapshot = database.withTransaction {
            Triple(database.categoryDao().listAll(), database.billDao().allWithCategory(), database.budgetDao().all())
        }
        val catArr = org.json.JSONArray()
        for (c in snapshot.first) {
            catArr.put(
                JSONObject()
                    .put("id", c.id)
                    .put("name", c.name)
                    .put("type", c.type)
                    .put("icon", c.icon)
                    .put("color", c.color)
                    .put("sortOrder", c.sortOrder)
                    .put("archived", c.isArchived)
                    .put("isPreset", c.isPreset)
                    .put("createdAt", c.createdAt)
            )
        }

        val billArr = org.json.JSONArray()
        var totalExpense = 0L
        var totalIncome = 0L
        for (b in snapshot.second) {
            val bill = b.bill
            if (bill.type == "expense") totalExpense += bill.amountCents else totalIncome += bill.amountCents
            billArr.put(
                JSONObject()
                    .put("id", bill.id)
                    // 整数分优先（无损）；amount 十进制保留，兼容旧版读取方
                    .put("amountCents", bill.amountCents)
                    .put("amount", bill.amountCents / 100.0)
                    .put("currency", bill.currency)
                    .put("type", bill.type)
                    .put("categoryId", bill.categoryId)
                    .put("categoryName", b.category.name)
                    .put("date", bill.date)
                    .put("note", bill.note)
                    .put("createdAt", bill.createdAt)
                    .put("updatedAt", bill.updatedAt)
            )
        }

        return JSONObject()
            .put("formatVersion", 3)
            .put("budgets", org.json.JSONArray().apply { snapshot.third.forEach { put(JSONObject().put("month", it.month).put("amountCents", it.amountCents).put("updatedAt", it.updatedAt)) } })
            .put("app", "bookkeeping-android")
            .put("exportedAt", Date().time)
            .put("settings", settings.toBackupJson())
            .put("categories", catArr)
            .put("bills", billArr)
            .put(
                "summary",
                JSONObject()
                    .put("billCount", snapshot.second.size)
                    .put("totalExpense", totalExpense / 100.0)
                    .put("totalIncome", totalIncome / 100.0)
            )
            .toString(2)
    }

    companion object {
        fun formatCents(cents: Long): String {
            val sign = if (cents < 0) "-" else ""
            val abs = Math.abs(cents)
            return String.format(Locale.US, "%s%d.%02d", sign, abs / 100, abs % 100)
        }
    }
}
