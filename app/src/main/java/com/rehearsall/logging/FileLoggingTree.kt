package com.rehearsall.logging

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

/**
 * Timber tree that writes log messages to a local file for release builds.
 *
 * Logs only WARN level and above to keep file size manageable.
 * File is written to {filesDir}/logs/rehearsall.log with a 5MB cap —
 * when the file exceeds 5MB, it's rotated to a .bak file and a fresh log is started.
 *
 * All writes are dispatched to a dedicated single-thread dispatcher so the calling
 * thread is never blocked by file I/O.
 */
class FileLoggingTree(filesDir: File) : Timber.Tree() {
    private val logFile: File
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
    private val maxFileSize = 5L * 1024 * 1024 // 5MB

    private val logDispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    private val logScope = CoroutineScope(logDispatcher + SupervisorJob())
    private var writer: BufferedWriter? = null
    private var writeCount = 0

    init {
        val logDir = File(filesDir, "logs")
        logDir.mkdirs()
        logFile = File(logDir, "rehearsall.log")
    }

    override fun isLoggable(
        tag: String?,
        priority: Int,
    ): Boolean {
        // Only log warnings and above in release builds
        return priority >= Log.WARN
    }

    override fun log(
        priority: Int,
        tag: String?,
        message: String,
        t: Throwable?,
    ) {
        val timestamp = dateFormat.format(Date())
        val level =
            when (priority) {
                Log.WARN -> "W"
                Log.ERROR -> "E"
                Log.ASSERT -> "A"
                else -> "I"
            }
        val line = "$timestamp $level/$tag: $message"

        logScope.launch {
            try {
                ensureWriter()
                writer?.appendLine(line)
                if (t != null) {
                    val pw = PrintWriter(writer!!, false)
                    t.printStackTrace(pw)
                    pw.flush()
                }
                writer?.flush()
                writeCount++
                if (writeCount % 100 == 0) rotateIfNeeded()
            } catch (_: Exception) {
                // Logging should never crash the app
            }
        }
    }

    private fun ensureWriter() {
        if (writer == null) {
            writer = BufferedWriter(FileWriter(logFile, true))
        }
    }

    private fun rotateIfNeeded() {
        if (logFile.exists() && logFile.length() > maxFileSize) {
            try {
                writer?.close()
                writer = null
                val backup = File(logFile.parent, "rehearsall.log.bak")
                backup.delete()
                logFile.renameTo(backup)
                ensureWriter()
            } catch (_: Exception) {
                // If rotation fails, just truncate
                try {
                    writer?.close()
                    writer = null
                    logFile.writeText("")
                    ensureWriter()
                } catch (_: Exception) {
                    // Give up silently
                }
            }
        }
    }
}
