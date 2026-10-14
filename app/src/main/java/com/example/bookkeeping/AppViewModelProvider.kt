package com.example.bookkeeping

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.bookkeeping.ui.category.CategoryViewModel
import com.example.bookkeeping.ui.edit.EditViewModel
import com.example.bookkeeping.ui.home.HomeViewModel
import com.example.bookkeeping.ui.settings.SettingsViewModel
import com.example.bookkeeping.ui.stats.StatsViewModel
import com.example.bookkeeping.ui.stats.CategoryBillsViewModel

/** 轻量 DI：无 Hilt，ViewModel 通过本工厂从 AppContainer 取依赖。 */
object AppViewModelProvider {

    val Factory = viewModelFactory {
        initializer {
            HomeViewModel(
                app().container.billRepository,
                app().container.categoryRepository,
                app().container.settingsRepository
            )
        }
        initializer {
            EditViewModel(
                createSavedStateHandle(),
                app().container.billRepository,
                app().container.categoryRepository,
                app().container.settingsRepository
            )
        }
        initializer {
            StatsViewModel(
                app().container.billRepository,
                app().container.settingsRepository
            )
        }
        initializer {
            CategoryBillsViewModel(createSavedStateHandle(), app().container.billRepository,
                app().container.categoryRepository, app().container.settingsRepository)
        }
        initializer {
            CategoryViewModel(
                app().container.categoryRepository,
                app().container.billRepository
            )
        }
        initializer {
            SettingsViewModel(
                app().container.settingsRepository,
                app().container.exportManager,
                app().container.billRepository,
                app().container.categoryRepository,
                app().container.backupRepository,
                app().container.importManager,
                app().container.backgroundImageStore,
                app().cacheDir
            )
        }
    }

    private fun CreationExtras.app(): BookkeepingApp =
        this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as BookkeepingApp
}

/** 供 Compose 层访问应用容器。 */
val LocalAppContainer = staticCompositionLocalOf<AppContainer> {
    error("LocalAppContainer not provided")
}
