package com.vian.vianlauncher

import android.os.Environment
import android.util.Log
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

object AppLogger {
    private val executor = Executors.newSingleThreadExecutor()
    private const val MAX_FILE_SIZE = 1024 * 1024L // 1MB

    fun d(tag: String, message: String) {
        Log.d(tag, message)
        writeToFile("DEBUG", tag, message)
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        Log.e(tag, message, throwable)
        val fullMessage = if (throwable != null) {
            "$message\n${throwable.stackTraceToString()}"
        } else {
            message
        }
        writeToFile("ERROR", tag, fullMessage)
    }

    private fun writeToFile(level: String, tag: String, message: String) {
        executor.execute {
            try {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!downloadsDir.exists()) downloadsDir.mkdirs()

                val logFile = File(downloadsDir, "vian_launcher_log.txt")
                
                if (logFile.exists() && logFile.length() > MAX_FILE_SIZE) {
                    truncateFile(logFile)
                }

                val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
                val timestamp = dateFormat.format(Date())
                val logLine = "[$timestamp] [$level] [$tag] $message\n"

                FileWriter(logFile, true).use { it.append(logLine) }
            } catch (e: Exception) {
                // Ignore file IO exceptions to prevent crashing
                Log.e("AppLogger", "Failed to write log", e)
            }
        }
    }

    private fun truncateFile(file: File) {
        try {
            val lines = file.readLines()
            val startLine = lines.size / 2
            val newLines = lines.subList(startLine, lines.size)
            FileWriter(file, false).use { writer ->
                newLines.forEach { line ->
                    writer.append(line).append("\n")
                }
            }
        } catch (e: Exception) {
            file.delete()
        }
    }
}
