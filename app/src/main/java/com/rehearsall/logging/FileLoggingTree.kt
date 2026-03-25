package com.rehearsall.logging

import android.util.Log
import timber.log.Timber
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Timber tree that writes log messages to a local file for release builds.
 *
 * Logs only WARN level and above to keep file size manageable.
 * File is written to {filesDir}/logs/rehearsall.log with a 5MB cap —
 * when the file exceeds 5MB, it's truncated to the most recent half.
 */
class FileLoggingTree(filesDir: File) : Timber.Tree() {
    private val logFile: File
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
    private val maxFileSize = 5L * 1024 * 1024 // 5MB

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
        try {
            rotateIfNeeded()

            val timestamp = dateFormat.format(Date())
            val level =
                when (priority) {
                    Log.WARN -> "W"
                    Log.ERROR -> "E"
                    Log.ASSERT -> "A"
                    else -> "I"
                }

            FileWriter(logFile, true).use { writer ->
                writer.appendLine("$timestamp $level/$tag: $message")
                if (t != null) {
                    val pw = PrintWriter(writer)
                    t.printStackTrace(pw)
                    pw.flush()
                }
            }
        } catch (_: Exception) {
            // Logging should never crash the app
        }
    }

    private fun rotateIfNeeded() {
        if (logFile.exists() && logFile.length() > maxFileSize) {
            try {
                // Keep the most recent half of the log
                val content = logFile.readText()
                val midpoint = content.length / 2
                val keepFrom = content.indexOf('\n', midpoint)
                if (keepFrom > 0) {
                    logFile.writeText(content.substring(keepFrom + 1))
                }
            } catch (_: Exception) {
                // If rotation fails, just truncate
                logFile.writeText("")
            }
        }
    }
}
