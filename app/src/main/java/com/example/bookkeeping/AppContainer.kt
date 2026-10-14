package com.example.bookkeeping

import android.app.Application
import androidx.room.Room
import androidx.room.withTransaction
import android.os.SystemClock
import android.util.Log
import com.example.bookkeeping.data.db.AppDatabase
import com.example.bookkeeping.data.db.dao.BillDao
import com.example.bookkeeping.data.db.dao.CategoryDao
import com.example.bookkeeping.data.export.ExportManager
import com.example.bookkeeping.data.export.ImportManager
import com.example.bookkeeping.data.prefs.BackgroundImageStore
import com.example.bookkeeping.data.prefs.SettingsRepository
import com.example.bookkeeping.data.repo.BackupRepository
import com.example.bookkeeping.data.repo.BillRepository
import com.example.bookkeeping.data.repo.CategoryRepository
import com.example.bookkeeping.data.seed.DefaultCategories
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class StartupState(val ready: Boolean = false, val error: String? = null)

/** 轻量依赖容器（无 Hilt），由 [BookkeepingApp] 创建并持有。 */
class AppContainer(app: Application) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO + CoroutineExceptionHandler { _, error ->
        Log.e("Startup", "Background cache failed", error)
    })
    private val _startup = MutableStateFlow(StartupState())
    val startup = _startup.asStateFlow()
    private var startupJob: Job? = null

    val database: AppDatabase =
        Room.databaseBuilder(app, AppDatabase::class.java, AppDatabase.NAME).build()

    val categoryDao: CategoryDao = database.categoryDao()
    val billDao: BillDao = database.billDao()

    val categoryRepository = CategoryRepository(categoryDao)
    val billRepository = BillRepository(billDao)
    val backupRepository = BackupRepository(database)
    val settingsRepository = SettingsRepository(app, scope)
    val exportManager by lazy { ExportManager(app) }
    val importManager by lazy { ImportManager(app, backupRepository, settingsRepository) }
    val backgroundImageStore by lazy { BackgroundImageStore(app) }

    /** 封面只等待首屏必需数据；首装分类写入完成后才发布分类缓存。 */
    fun prepareStartup() {
        if (_startup.value.ready || startupJob?.isActive == true) return
        _startup.value = StartupState()
        startupJob = scope.launch {
            val started = SystemClock.elapsedRealtime()
            try {
                coroutineScope {
                    val settings = async { settingsRepository.awaitLoaded() }
                    database.withTransaction {
                        if (categoryDao.count() == 0) categoryDao.insertAll(DefaultCategories.toEntities())
                    }
                    // 首次查询在当前协程完成，失败可显示重试，而不是一直等空列表。
                    categoryRepository.primeCache(scope)
                    billRepository.primeCurrentMonthCache(scope)
                    settings.await()
                }
                Log.i("Startup", "Essential data ready in ${SystemClock.elapsedRealtime() - started}ms")
                _startup.value = StartupState(ready = true)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("Startup", "Startup preparation failed", e)
                _startup.value = StartupState(error = "暂时无法读取本地数据，请重试")
            }
        }
    }
}
