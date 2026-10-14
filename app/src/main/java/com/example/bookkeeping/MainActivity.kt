package com.example.bookkeeping

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.view.ViewTreeObserver
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.bookkeeping.data.prefs.ThemeMode
import com.example.bookkeeping.ui.common.LocalBackgroundSpec
import com.example.bookkeeping.ui.nav.MainNav
import com.example.bookkeeping.ui.theme.BookkeepingTheme
import com.example.bookkeeping.ui.theme.isDark

class MainActivity : ComponentActivity() {
    private var startupPreDrawListener: ViewTreeObserver.OnPreDrawListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val activityStarted = SystemClock.elapsedRealtime()
        val container = (application as BookkeepingApp).container
        // 只使用系统的图标启动窗口；不再叠加应用内全屏封面。
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            splashScreen.setOnExitAnimationListener { it.remove() }
        }

        var firstContentPrepared = false
        val minimumVisibleUntil = if (savedInstanceState == null) activityStarted + 600L else activityStarted
        val decor = window.decorView
        val preDrawListener = object : ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                if (firstContentPrepared && SystemClock.elapsedRealtime() >= minimumVisibleUntil) {
                    decor.viewTreeObserver.removeOnPreDrawListener(this)
                    startupPreDrawListener = null
                    decor.post {
                        if (container.startup.value.ready) {
                            reportFullyDrawn()
                            Log.i("Startup", "First usable frame after Activity.onCreate: ${SystemClock.elapsedRealtime() - activityStarted}ms")
                        }
                    }
                    return true
                }
                // 保持同一个启动窗口，等首屏真正组合完成后再放行第一帧。
                decor.postInvalidateOnAnimation()
                return false
            }
        }
        startupPreDrawListener = preDrawListener
        decor.viewTreeObserver.addOnPreDrawListener(preDrawListener)

        setContent {
            CompositionLocalProvider(LocalAppContainer provides container) {
                val settings by container.settingsRepository.settings.collectAsStateWithLifecycle()
                val startup by container.startup.collectAsStateWithLifecycle()
                val theme = if (startup.ready) settings.themeMode else ThemeMode.LIGHT
                val dark = theme.isDark()
                LaunchedEffect(dark) {
                    WindowCompat.getInsetsController(window, window.decorView).apply {
                        isAppearanceLightStatusBars = !dark
                        isAppearanceLightNavigationBars = !dark
                    }
                }
                CompositionLocalProvider(LocalBackgroundSpec provides settings.background) {
                    BookkeepingTheme(theme, settings.themeStyle) {
                        if (startup.ready) {
                            MainNav()
                            SideEffect { firstContentPrepared = true }
                        } else {
                            // 正常准备阶段被系统启动窗口遮住；读取失败时可放行重试界面。
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                val error = startup.error
                                if (error != null) {
                                    Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(error)
                                        TextButton(onClick = container::prepareStartup) { Text("重试") }
                                    }
                                } else CircularProgressIndicator()
                            }
                            SideEffect { firstContentPrepared = startup.error != null }
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        startupPreDrawListener?.let { listener ->
            val observer = window.decorView.viewTreeObserver
            if (observer.isAlive) observer.removeOnPreDrawListener(listener)
        }
        startupPreDrawListener = null
        super.onDestroy()
    }
}
