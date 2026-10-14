package com.example.bookkeeping.ui.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bookkeeping.data.export.ExportManager
import com.example.bookkeeping.data.export.ImportManager
import com.example.bookkeeping.data.export.PreparedBackup
import com.example.bookkeeping.data.prefs.BACKGROUND_SPEC_CUSTOM_IMAGE
import com.example.bookkeeping.data.prefs.BACKGROUND_SPEC_DEFAULT
import com.example.bookkeeping.data.prefs.BackgroundImageStore
import com.example.bookkeeping.data.prefs.SettingsRepository
import com.example.bookkeeping.data.prefs.ThemeMode
import com.example.bookkeeping.data.repo.BackupRepository
import com.example.bookkeeping.data.repo.BillRepository
import com.example.bookkeeping.data.repo.CategoryRepository
import com.example.bookkeeping.data.seed.DefaultCategories
import com.example.bookkeeping.ui.common.BackgroundPreset
import com.example.bookkeeping.ui.common.BackgroundPresets
import com.example.bookkeeping.ui.common.spec
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    val exportManager: ExportManager,
    private val billRepository: BillRepository,
    private val categoryRepository: CategoryRepository,
    private val backupRepository: BackupRepository,
    private val importManager: ImportManager,
    private val backgroundImageStore: BackgroundImageStore,
    private val cacheDir: File
) : ViewModel() {

    data class ExportResult(val ok: Boolean, val message: String)

    val settings = settingsRepository.settings

    val billCount: StateFlow<Long> = billRepository.observeCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    private val _result = MutableStateFlow<ExportResult?>(null)
    val result: StateFlow<ExportResult?> = _result.asStateFlow()

    /** 导入 / 导出 / 清空中，用于禁用按钮防重复触发。 */
    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()
    private val _backupPreview = MutableStateFlow<PreparedBackup?>(null)
    val backupPreview = _backupPreview.asStateFlow()

    /** 缓存占用（字节）：导出临时文件 + 框架缓存 + 未使用的背景图。 */
    private val _cacheSizeBytes = MutableStateFlow(0L)
    val cacheSizeBytes: StateFlow<Long> = _cacheSizeBytes.asStateFlow()

    init {
        refreshCacheSize()
    }

    /** 重新统计占用（进入页面 / 清理后调用）。 */
    fun refreshCacheSize() {
        viewModelScope.launch {
            _cacheSizeBytes.value = withContext(Dispatchers.IO) {
                dirSize(cacheDir) + unusedBackgroundBytes()
            }
        }
    }

    /** 已保存但当前未使用的背景图（切回默认/预设后残留）占用。 */
    private suspend fun unusedBackgroundBytes(): Long = withContext(Dispatchers.IO) {
        val spec = settingsRepository.current().background
        if (spec == BACKGROUND_SPEC_CUSTOM_IMAGE) 0L else backgroundImageStore.sizeBytes()
    }

    /** 一键清理：清缓存目录，并删除已不再使用的背景图。账单数据不受影响。 */
    fun clearCache() {
        if (_busy.value) return
        _busy.value = true
        viewModelScope.launch {
            try {
                val removedBg = withContext(Dispatchers.IO) {
                    val spec = settingsRepository.current().background
                    cacheDir.listFiles()?.forEach { it.deleteRecursively() }
                    if (spec != BACKGROUND_SPEC_CUSTOM_IMAGE) {
                        val existed = backgroundImageStore.hasImage()
                        backgroundImageStore.clear()
                        existed
                    } else {
                        false
                    }
                }
                _result.value = ExportResult(
                    true,
                    if (removedBg) "缓存与未使用的背景图已清理" else "缓存已清理"
                )
                refreshCacheSize()
            } catch (e: Exception) {
                _result.value = ExportResult(false, "清理失败：${e.message ?: "未知错误"}")
            } finally {
                _busy.value = false
            }
        }
    }

    private fun dirSize(dir: File): Long {
        if (!dir.exists()) return 0L
        return dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    }

    fun setCurrency(code: String) {
        viewModelScope.launch { settingsRepository.setCurrency(code) }
    }

    fun setWeekStartMonday(value: Boolean) {
        viewModelScope.launch { settingsRepository.setWeekStartMonday(value) }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settingsRepository.setThemeMode(mode) }
    }

    /** 设置背景预设（key = "default" 或 BackgroundPreset.key）。 */
    fun setBackgroundPreset(key: String) {
        viewModelScope.launch {
            val spec = if (key == BACKGROUND_SPEC_DEFAULT) {
                BACKGROUND_SPEC_DEFAULT
            } else {
                BackgroundPresets.firstOrNull { it.key == key }?.spec() ?: BACKGROUND_SPEC_DEFAULT
            }
            settingsRepository.setBackground(spec)
            refreshCacheSize()
        }
    }

    /** 从相册 Uri 保存自定义背景图片并设为当前背景。 */
    fun setBackgroundImage(uri: Uri) {
        if (_busy.value) return
        _busy.value = true
        viewModelScope.launch {
            try {
                val ok = backgroundImageStore.saveFromUri(uri)
                if (ok) {
                    settingsRepository.setBackground(BACKGROUND_SPEC_CUSTOM_IMAGE)
                    _result.value = ExportResult(true, "背景图片已更新")
                } else {
                    _result.value = ExportResult(false, "图片读取失败，请换一张试试")
                }
                refreshCacheSize()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _result.value = ExportResult(false, "背景设置失败，请重试")
            } finally {
                _busy.value = false
            }
        }
    }

    fun defaultName(kind: ExportManager.Kind): String = exportManager.suggestFileName(kind)

    /** 导出到用户通过 SAF 选择的 Uri。 */
    fun exportToUri(kind: ExportManager.Kind, uri: Uri) {
        if (_busy.value) return
        _busy.value = true
        viewModelScope.launch {
            try {
                val n = exportManager.writeToUri(
                    kind,
                    uri,
                    categoryRepository.listAll(),
                    billRepository.allWithCategoryForExport(),
                    settingsRepository.current()
                )
                _result.value = ExportResult(true, "已导出 $n 条记录")
            } catch (e: Exception) {
                _result.value = ExportResult(false, "导出失败：${e.message ?: "未知错误"}")
            } finally {
                _busy.value = false
                refreshCacheSize()
            }
        }
    }

    /** 生成分享文件（写入应用缓存目录，随后由系统分享面板发出）。 */
    fun prepareShareFile(kind: ExportManager.Kind, onReady: (File) -> Unit) {
        if (_busy.value) return
        _busy.value = true
        viewModelScope.launch {
            try {
                val file = exportManager.writeShareFile(
                    kind,
                    categoryRepository.listAll(),
                    billRepository.allWithCategoryForExport(),
                    settingsRepository.current()
                )
                refreshCacheSize()
                onReady(file)
            } catch (e: Exception) {
                _result.value = ExportResult(false, "导出失败：${e.message ?: "未知错误"}")
            } finally {
                _busy.value = false
            }
        }
    }

    /** 先只读校验文件，不修改当前数据。 */
    fun previewBackup(uri: Uri) {
        if (_busy.value) return
        _busy.value = true
        _backupPreview.value = null
        _result.value = null
        viewModelScope.launch {
            try {
                _backupPreview.value = importManager.preview(uri)
            } catch (e: CancellationException) { throw e
            } catch (e: Exception) {
                _result.value = ExportResult(false, "备份无法读取：${e.message ?: "请检查文件格式"}")
            } finally { _busy.value = false }
        }
    }

    fun cancelBackupPreview() { if (!_busy.value) _backupPreview.value = null }

    /** 仅恢复用户已经查看并确认的那份数据，不重新读取可能变化的文件。 */
    fun restoreBackup() {
        if (_busy.value) return
        val backup = _backupPreview.value ?: return
        _busy.value = true
        viewModelScope.launch {
            try {
                val r = importManager.restore(backup)
                _result.value = ExportResult(r.ok, r.message)
                _backupPreview.value = null
            } finally {
                _busy.value = false
                refreshCacheSize()
            }
        }
    }

    /** 清空所有账单，并恢复内置分类（单事务）。 */
    fun clearAll() {
        if (_busy.value) return
        _busy.value = true
        viewModelScope.launch {
            try {
                backupRepository.clearAllData(DefaultCategories.toEntities())
                _result.value = ExportResult(true, "已清空全部数据")
            } catch (e: Exception) {
                _result.value = ExportResult(false, "操作失败：${e.message ?: "未知错误"}")
            } finally {
                _busy.value = false
            }
        }
    }

    fun consumeResult() {
        _result.value = null
    }

    /** 分享失败时给用户可见反馈（原先只打日志）。 */
    fun reportShareFailure(e: Exception) {
        _result.value = ExportResult(false, "分享失败：${e.message ?: "无法拉起分享面板"}")
    }
}
