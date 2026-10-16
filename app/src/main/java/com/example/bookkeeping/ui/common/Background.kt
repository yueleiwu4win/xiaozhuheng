package com.example.bookkeeping.ui.common

import com.example.bookkeeping.ui.theme.LocalThemeTokens
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import com.example.bookkeeping.ui.theme.LocalAppDark
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import com.example.bookkeeping.ui.theme.HomeLayout
import com.example.bookkeeping.R
import com.example.bookkeeping.data.prefs.BACKGROUND_SPEC_CUSTOM_IMAGE
import com.example.bookkeeping.data.prefs.BACKGROUND_SPEC_DEFAULT
import com.example.bookkeeping.data.prefs.BACKGROUND_SPEC_ABACUS
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** 背景设置取值：MainActivity 从设置流中提供，各页面通过 [AppBackground] 读取。 */
val LocalBackgroundSpec = staticCompositionLocalOf<String> { BACKGROUND_SPEC_DEFAULT }

/** 自定义背景图文件名（与 BackgroundImageStore 保持一致）。 */
private const val CUSTOM_IMAGE_DIR = "backgrounds"
private const val CUSTOM_IMAGE_NAME = "custom.jpg"

/** 自定义背景图文件。 */
fun customBackgroundFile(context: android.content.Context): File =
    File(File(context.filesDir, CUSTOM_IMAGE_DIR), CUSTOM_IMAGE_NAME)

/** 预设渐变背景（浅色调；深色主题下自动叠加暗色蒙层保证可读性）。 */
data class BackgroundPreset(
    val key: String,
    val label: String,
    /** ARGB 十六进制（不带 0x），如 "FFD9EDFF" */
    val fromHex: String,
    val toHex: String
)

val BackgroundPresets: List<BackgroundPreset> = listOf(
    BackgroundPreset("sky", "晴空", "FFD9EDFF", "FFF7FBFF"),
    BackgroundPreset("mint", "薄荷", "FFDDF6EC", "FFF3FDF8"),
    BackgroundPreset("lavender", "薰衣", "FFE4E1F8", "FFF6F5FF"),
    BackgroundPreset("sand", "暖沙", "FFF9EDDA", "FFFEF9F0")
)

/** 预设对应的存储值，与 SettingsRepository.background 对应。 */
fun BackgroundPreset.spec(): String = "gradient:$fromHex:$toHex"

/**
 * 应用背景层：铺满父容器，绘制在页面内容之下。
 * - default：主题底色（与旧版观感一致）
 * - gradient：上下渐变
 * - image:custom：相册图片（Cover 铺满），叠加半透明蒙层保证文字可读
 */
@Composable
fun AppBackground(modifier: Modifier = Modifier) {
    val spec = LocalBackgroundSpec.current
    val bitmap = if (spec == BACKGROUND_SPEC_CUSTOM_IMAGE) {
        val file = customBackgroundFile(LocalContext.current)
        val image by produceState<ImageBitmap?>(null, spec, file.lastModified()) {
            value = withContext(Dispatchers.IO) { decodeSampled(file)?.asImageBitmap() }
        }
        image
    } else null
    AppBackgroundContent(spec, bitmap, modifier)
}

/** 解码与绘制分离，照片和主题背景共用真实页面绘制逻辑。 */
@Composable
internal fun AppBackgroundContent(spec: String, bitmap: ImageBitmap?, modifier: Modifier = Modifier) {
    val isDark = LocalAppDark.current

    Box(modifier = modifier) {
        when {
            spec == BACKGROUND_SPEC_DEFAULT -> {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                )
                val layout = LocalThemeTokens.current.homeLayout
                if (layout == HomeLayout.INK_GOLD && isDark) {
                    Image(painterResource(R.drawable.ink_gold_bookcloth), null,
                        Modifier.fillMaxSize(), contentScale = ContentScale.Crop, alpha = 0.42f)
                } else if (layout != HomeLayout.STANDARD) LedgerPaper(Modifier.fillMaxSize(), if (isDark) 0.035f else 0.075f)
                if (layout == HomeLayout.STANDARD) LocalThemeTokens.current.ornament?.let { resource ->
                    Image(
                        painterResource(resource), contentDescription = null,
                        modifier = Modifier.fillMaxSize(), contentScale = ContentScale.FillWidth,
                        alignment = androidx.compose.ui.Alignment.TopEnd,
                        alpha = if (isDark) 0.08f else 0.16f
                    )
                }
            }

            spec.startsWith("gradient:") -> {
                val parts = spec.removePrefix("gradient:").split(":")
                if (parts.size == 2) {
                    val from = Color(parts[0].toLong(16))
                    val to = Color(parts[1].toLong(16))
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(Brush.verticalGradient(listOf(from, to)))
                    )
                    if (isDark) {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.55f))
                        )
                    }
                }
            }

            spec == BACKGROUND_SPEC_CUSTOM_IMAGE || spec == BACKGROUND_SPEC_ABACUS -> {
                val img = bitmap
                if (spec == BACKGROUND_SPEC_ABACUS) {
                    Image(painterResource(R.drawable.background_abacus), null,
                        Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                } else if (img != null) {
                    Image(
                        bitmap = img,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background)
                    )
                }
                // 蒙层：浅色主题提亮、深色主题压暗，保证账单文字可读
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            if (isDark) Color.Black.copy(alpha = 0.55f)
                            else Color.White.copy(alpha = 0.35f)
                        )
                )
            }

            else -> {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                )
            }
        }
    }
}

/** 解码背景图片（最大边 1280px，采样解码，避免大图 OOM）。 */
private fun decodeSampled(file: File): Bitmap? {
    if (!file.exists()) return null
    return runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        var sample = 1
        val maxDim = 1280
        while (bounds.outWidth / sample > maxDim || bounds.outHeight / sample > maxDim) {
            sample *= 2
        }
        BitmapFactory.decodeFile(
            file.absolutePath,
            BitmapFactory.Options().apply { inSampleSize = sample }
        )
    }.getOrNull()
}
