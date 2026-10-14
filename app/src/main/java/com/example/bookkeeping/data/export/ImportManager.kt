package com.example.bookkeeping.data.export

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.example.bookkeeping.data.prefs.*
import com.example.bookkeeping.data.repo.BackupRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class ImportManager(
    private val context: Context,
    private val backupRepository: BackupRepository,
    private val settingsRepository: SettingsRepository
) {
    data class ImportResult(val ok: Boolean, val message: String, val billCount: Int = 0)

    suspend fun preview(uri: Uri): PreparedBackup = withContext(Dispatchers.IO) {
        val name = runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
        }.getOrNull() ?: "所选备份"
        val text = context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
            ?: error("无法读取所选文件")
        BackupParser.parse(text.removePrefix("\uFEFF"), name)
    }

    suspend fun restore(backup: PreparedBackup): ImportResult = withContext(Dispatchers.IO) {
        try {
            backupRepository.restoreAll(backup.categories, backup.bills)
        } catch (e: CancellationException) { throw e
        } catch (e: Exception) { return@withContext ImportResult(false, "恢复失败，原数据已保留：${e.message ?: "请重试"}") }
        // 数据库提交后，设置失败单独说明，避免误报整个恢复失败。
        val settingsNote = try {
            applySettings(backup.settings)
        } catch (e: CancellationException) { throw e
        } catch (_: Exception) { "；账单已恢复，部分设置恢复失败，请在设置中重新选择" }
        val skip = if (backup.skippedBills + backup.skippedCategories > 0) "，已跳过无效数据" else ""
        ImportResult(true, "已恢复 ${backup.bills.size} 笔账单、${backup.categories.size} 个分类$skip$settingsNote", backup.bills.size)
    }

    private suspend fun applySettings(settings: BackupSettings?): String {
        if (settings == null) return ""
        settings.currency?.let { settingsRepository.setCurrency(it) }
        settings.weekStartMonday?.let { settingsRepository.setWeekStartMonday(it) }
        settings.themeMode?.let { settingsRepository.setThemeMode(ThemeMode.from(it)) }
        settings.themeStyle?.let { settingsRepository.setThemeStyle(ThemeStyle.from(it)) }
        var note = ""
        settings.background?.let { spec ->
            val usable = when {
                spec == BACKGROUND_SPEC_DEFAULT -> true
                Regex("^gradient:[0-9A-Fa-f]{8}:[0-9A-Fa-f]{8}$").matches(spec) -> true
                spec == BACKGROUND_SPEC_CUSTOM_IMAGE -> File(context.filesDir, "backgrounds/custom.jpg").exists()
                else -> false
            }
            settingsRepository.setBackground(if (usable) spec else BACKGROUND_SPEC_DEFAULT)
            if (!usable) note = "；备份背景不可用，已使用默认背景"
        }
        return note
    }
}
