package com.smartbasketball.app.util

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object AppLogger {
    const val TAG = "SmartBasketball"
    
    private const val LOG_FILE_NAME = "app_log.txt"
    private var logFile: File? = null
    private var writer: PrintWriter? = null
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())

    fun init(context: Context) {
        try {
            val logDir = context.filesDir
            val file = File(logDir, LOG_FILE_NAME)
            logFile = file
            
            // 每次启动清空日志文件
            PrintWriter(file).use { it.write("") }
            
            writer = PrintWriter(FileOutputStream(file, true), true)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to init logger: ${e.message}")
        }
    }

    private fun writeToFile(level: String, message: String) {
        try {
            synchronized(this) {
                writer?.let {
                    val timestamp = dateFormat.format(Date())
                    it.println("$timestamp $level $message")
                    it.flush()
                }
            }
        } catch (e: Exception) {
            // Ignore file write errors
        }
    }

    fun d(message: String, tag: String = TAG) {
        Log.d(tag, message)
        writeToFile("D", "$tag: $message")
    }

    fun i(message: String, tag: String = TAG) {
        Log.i(tag, message)
        writeToFile("I", "$tag: $message")
    }

    fun w(message: String, tag: String = TAG) {
        Log.w(tag, message)
        writeToFile("W", "$tag: $message")
    }

    fun w(message: String, throwable: Throwable, tag: String = TAG) {
        Log.w(tag, message, throwable)
        writeToFile("W", "$tag: $message, ${throwable.message}")
    }

    fun e(message: String, tag: String = TAG) {
        Log.e(tag, message)
        writeToFile("E", "$tag: $message")
    }

    fun e(message: String, throwable: Throwable, tag: String = TAG) {
        Log.e(tag, message, throwable)
        writeToFile("E", "$tag: $message, ${throwable.message}")
    }

    fun v(message: String, tag: String = TAG) {
        Log.v(tag, message)
        writeToFile("V", "$tag: $message")
    }

    fun json(tag: String = TAG, json: String) {
        val maxLogSize = 2000
        val length = json.length
        var start = 0
        while (start < length) {
            val end = minOf(start + maxLogSize, length)
            val part = json.substring(start, end)
            Log.d(tag, part)
            writeToFile("D", "$tag: $part")
            start = end
        }
    }

    fun performance(tag: String = TAG, operation: String, startTime: Long) {
        val duration = System.currentTimeMillis() - startTime
        val msg = "$operation took ${duration}ms"
        Log.d(tag, msg)
        writeToFile("D", "$tag: $msg")
    }

    inline fun <T> measureTime(tag: String = TAG, operation: String, block: () -> T): T {
        val startTime = System.currentTimeMillis()
        return block().also {
            performance(tag, operation, startTime)
        }
    }

    fun close() {
        try {
            writer?.close()
        } catch (e: Exception) {
            // Ignore
        }
    }
}

inline fun <T> T.logD(tag: String = "SmartBasketball"): T {
    AppLogger.d(this.toString(), tag)
    return this
}

inline fun <T> T.logI(tag: String = "SmartBasketball"): T {
    AppLogger.i(this.toString(), tag)
    return this
}

inline fun <T> T.logE(tag: String = "SmartBasketball"): T {
    AppLogger.e(this.toString(), tag)
    return this
}
