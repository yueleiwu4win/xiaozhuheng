package com.example.bookkeeping.ui.summary

import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayOutputStream

class PosterFileStoreTest {
    @get:Rule val temporary = TemporaryFolder()
    private val png = byteArrayOf(-119, 80, 78, 71, 13, 10, 26, 10, 1, 2, 3)

    @Test fun recreatedStoreSavesExactOriginalWithoutAnyBitmap() {
        val directory = temporary.newFolder()
        val savedName = PosterFileStore(directory).write { it.write(png); true }
        val restored = PosterFileStore(directory)
        val output = ByteArrayOutputStream()
        restored.copyTo(savedName) { output }
        assertArrayEquals(png, output.toByteArray())
        assertArrayEquals(png, restored.resolve(savedName).readBytes())
    }
    @Test fun missingRestoredSourceFailsBeforeDestinationIsOpened() {
        val store = PosterFileStore(temporary.newFolder())
        val name = store.write { it.write(png); true }
        store.resolve(name).delete()
        var opened = false
        assertTrue(runCatching { store.copyTo(name) { opened = true; ByteArrayOutputStream() } }.isFailure)
        assertFalse(opened)
    }
    @Test fun failedEncodingLeavesNoFileThatCouldBeShared() {
        val directory = temporary.newFolder()
        val store = PosterFileStore(directory)
        assertTrue(runCatching { store.write { it.write(png); false } }.isFailure)
        assertTrue(directory.listFiles()!!.isEmpty())
        assertTrue(runCatching { store.write { error("interrupted encoder") } }.isFailure)
        assertTrue(directory.listFiles()!!.isEmpty())
    }
    @Test fun invalidRestoredNamesCannotReadOtherFiles() {
        val store = PosterFileStore(temporary.newFolder())
        listOf(null, "", "../secret", "/other/file.png", "summary.png").forEach { name ->
            assertTrue(runCatching { store.resolve(name) }.isFailure)
        }
    }
}
