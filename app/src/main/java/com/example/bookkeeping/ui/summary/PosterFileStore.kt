package com.example.bookkeeping.ui.summary

import java.io.File
import java.io.OutputStream
import java.util.UUID

/** A completed PNG is the source for preview, SAF save and sharing, including after recreation. */
internal class PosterFileStore(private val directory: File) {
    fun write(encode: (OutputStream) -> Boolean): String {
        check(directory.isDirectory || directory.mkdirs()) { "无法创建海报目录" }
        val name = "summary-${UUID.randomUUID()}.png"
        val target = File(directory, name)
        val temporary = File(directory, "$name.partial")
        try {
            temporary.outputStream().use { check(encode(it)) { "图片编码失败" } }
            check(temporary.length() > 0 && temporary.renameTo(target)) { "图片写入失败" }
            return name
        } finally { temporary.delete() }
    }

    fun resolve(name: String?): File {
        require(name != null && Regex("summary-[0-9a-f-]{36}\\.png").matches(name)) { "海报记录已失效，请重新生成" }
        return File(directory, name).also { check(it.isFile && it.length() > 0) { "海报文件已丢失，请重新生成" } }
    }

    fun copyTo(name: String?, openDestination: () -> OutputStream?) {
        // Validate and open the source first; never silently accept a missing restored poster.
        resolve(name).inputStream().use { source ->
            val destination = openDestination() ?: error("无法打开保存位置，请重试")
            destination.use { source.copyTo(it) }
        }
    }
}
