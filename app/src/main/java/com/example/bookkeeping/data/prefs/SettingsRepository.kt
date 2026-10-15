package com.example.bookkeeping.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.CancellationException

private val Context.dataStore by preferencesDataStore(name = "settings")

/** 背景设置取值：跟随主题底色。 */
const val BACKGROUND_SPEC_DEFAULT = "default"

/** 背景设置取值：相册自定义图片。 */
const val BACKGROUND_SPEC_CUSTOM_IMAGE = "image:custom"

/** 主题模式：system | light | dark */
enum class ThemeMode(val value: String) {
    SYSTEM("system"),
    LIGHT("light"),
    DARK("dark");

    companion object {
        fun from(value: String?): ThemeMode = entries.firstOrNull { it.value == value } ?: SYSTEM
    }
}

data class AppSettings(
    /** 币种代码（V1 单币种，仅用于符号展示，不做汇率换算） */
    val currency: String = "CNY",
    /** 周起始日是否为周一（预留给周维度统计） */
    val weekStartMonday: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val themeStyle: ThemeStyle = ThemeStyle.CLASSIC,
    /**
     * 背景设置：
     * - "default" 跟随主题底色
     * - "gradient:FFxxxxxx:FFxxxxxx" 预设渐变
     * - "image:custom" 相册图片（文件路径固定，见 BackgroundImageStore）
     */
    val background: String = BACKGROUND_SPEC_DEFAULT
)

/** 上次记账使用的分类与日期（连续快速记账用，跨重启保留）。 */
data class LastUsed(
    val categoryId: Long?,
    val date: String?
)

class SettingsRepository(private val context: Context, scope: CoroutineScope) {

    private val _settings = MutableStateFlow(AppSettings())
    val settings = _settings.asStateFlow()
    private val _lastUsed = MutableStateFlow(LastUsed(null, null))
    val lastUsed = _lastUsed.asStateFlow()
    private val loaded = CompletableDeferred<Unit>()

    /** 设置流；DataStore 文件损坏/读失败时回落默认值，避免启动崩溃。 */
    private val preferences = context.dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }

    init {
        scope.launch {
            try {
                preferences.collect { p ->
                    _settings.value = AppSettings(
                        currency = p[KEY_CURRENCY] ?: "CNY",
                        weekStartMonday = p[KEY_WEEK] ?: true,
                        themeMode = ThemeMode.from(p[KEY_THEME]),
                        themeStyle = ThemeStyle.from(p[KEY_STYLE]),
                        background = p[KEY_BG] ?: BACKGROUND_SPEC_DEFAULT
                    )
                    _lastUsed.value = LastUsed(p[KEY_LAST_CATEGORY], p[KEY_LAST_DATE])
                    loaded.complete(Unit)
                }
            } catch (e: CancellationException) {
                loaded.cancel(e)
                throw e
            } catch (e: Exception) {
                loaded.completeExceptionally(e)
            }
        }
    }

    suspend fun awaitLoaded() { loaded.await() }

    suspend fun current(): AppSettings {
        awaitLoaded()
        return settings.value
    }

    suspend fun setCurrency(code: String) {
        context.dataStore.edit { it[KEY_CURRENCY] = code }
    }

    suspend fun setWeekStartMonday(value: Boolean) {
        context.dataStore.edit { it[KEY_WEEK] = value }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[KEY_THEME] = mode.value }
    }

    suspend fun setThemeStyle(style: ThemeStyle, applyRecommendation: Boolean = true) {
        // 一次提交风格及推荐明暗，避免切换时先闪过另一套配色。
        context.dataStore.edit {
            it[KEY_STYLE] = style.value
            if (applyRecommendation) style.recommendedMode?.let { mode -> it[KEY_THEME] = mode.value }
        }
    }

    suspend fun setBackground(spec: String) {
        context.dataStore.edit { it[KEY_BG] = spec }
    }

    suspend fun setLastUsed(categoryId: Long, date: String) {
        context.dataStore.edit {
            it[KEY_LAST_CATEGORY] = categoryId
            it[KEY_LAST_DATE] = date
        }
        _lastUsed.value = LastUsed(categoryId, date)
    }

    private companion object {
        val KEY_CURRENCY = stringPreferencesKey("currency")
        val KEY_WEEK = booleanPreferencesKey("week_start_monday")
        val KEY_THEME = stringPreferencesKey("theme_mode")
        val KEY_STYLE = stringPreferencesKey("theme_style")
        val KEY_BG = stringPreferencesKey("background_spec")
        val KEY_LAST_CATEGORY = longPreferencesKey("last_category_id")
        val KEY_LAST_DATE = stringPreferencesKey("last_date")
    }
}
