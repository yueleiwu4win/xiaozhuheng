package com.example.bookkeeping.data.prefs

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * 自定义背景图片的本地存储：从相册 Uri 解码、压缩后保存到应用私有目录。
 * 文件路径固定（filesDir/backgrounds/custom.jpg），设置项只需记录 spec 字符串。
 *
 * - API 28+ 用 [ImageDecoder]：自动按 EXIF 方向解码（竖拍照片不会侧躺）
 * - 低版本回落到 BitmapFactory + 采样
 * - 先写临时文件再改名，避免压缩中断留下半张图
 */
class BackgroundImageStore(private val context: Context) {

    val file: File
        get() = File(File(context.filesDir, "backgrounds"), "custom.jpg")

    fun hasImage(): Boolean = file.exists()

    fun sizeBytes(): Long = if (file.exists()) file.length() else 0L

    /** 从相册 Uri 解码并压缩保存（最大边 1080px，JPEG 85%）。成功返回 true。 */
    suspend fun saveFromUri(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        val bitmap = runCatching { decode(uri) }.getOrNull()
        if (bitmap == null) {
            Log.w(TAG, "decode background failed: $uri")
            return@withContext false
        }
        val tmp = File(file.parentFile, "${file.name}.tmp")
        val ok = runCatching {
            file.parentFile?.mkdirs()
            FileOutputStream(tmp).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
            }
            tmp.renameTo(file)
        }.getOrElse {
            Log.w(TAG, "save background failed", it)
            false
        }
        tmp.delete()
        bitmap.recycle()
        ok
    }

    private fun decode(uri: Uri): Bitmap? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            return ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                // 软件分配，保证后续 compress 可用
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                val w = info.size.width
                val h = info.size.height
                val maxDim = 1080
                val longest = maxOf(w, h)
                if (longest > maxDim) {
                    val scale = maxDim.toFloat() / longest
                    decoder.setTargetSize(
                        (w * scale).toInt().coerceAtLeast(1),
                        (h * scale).toInt().coerceAtLeast(1)
                    )
                }
            }
        }
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            ?: return null
        var sample = 1
        val maxDim = 1080
        while (bounds.outWidth / sample > maxDim || bounds.outHeight / sample > maxDim) {
            sample *= 2
        }
        return context.contentResolver.openInputStream(uri)?.use { input ->
            BitmapFactory.decodeStream(input, null, BitmapFactory.Options().apply { inSampleSize = sample })
        }
    }

    fun clear() {
        file.delete()
    }

    private companion object {
        const val TAG = "BackgroundImageStore"
    }
}
