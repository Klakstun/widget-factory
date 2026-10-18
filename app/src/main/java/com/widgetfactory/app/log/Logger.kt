package com.widgetfactory.app.log

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 应用级日志系统。
 *
 * - 异步写入 getExternalFilesDir("logs")
 * - 分级：error / warn / info / debug
 * - 自动轮转（按大小，保留最近 N 个）
 * - 敏感信息（路径、URI）脱敏
 * - 支持导出（分享 Intent）
 */
object Logger {

    enum class Level(val priority: Int, val tag: String) {
        DEBUG(0, "D"),
        INFO(1, "I"),
        WARN(2, "W"),
        ERROR(3, "E")
    }

    private const val TAG = "Logger"
    private const val MAX_FILE_SIZE = 512 * 1024L // 512KB
    private const val MAX_FILES = 5

    private var context: Context? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
    private val fileDateFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())

    fun init(context: Context) {
        this.context = context.applicationContext
    }

    private fun logDir(): File? {
        val ctx = context ?: return null
        val dir = ctx.getExternalFilesDir("logs") ?: ctx.filesDir.resolve("logs")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private fun currentLogFile(): File? {
        val dir = logDir() ?: return null
        val today = fileDateFormat.format(Date())
        return File(dir, "log_$today.txt")
    }

    fun d(tag: String, msg: String) = log(Level.DEBUG, tag, msg)
    fun i(tag: String, msg: String) = log(Level.INFO, tag, msg)
    fun w(tag: String, msg: String) = log(Level.WARN, tag, msg)
    fun e(tag: String, msg: String, t: Throwable? = null) =
        log(Level.ERROR, tag, msg + (t?.let { "\n${it.stackTraceToString()}" } ?: ""))

    private fun log(level: Level, tag: String, message: String) {
        // 同步输出到 logcat
        when (level) {
            Level.DEBUG -> android.util.Log.d(tag, message)
            Level.INFO -> android.util.Log.i(tag, message)
            Level.WARN -> android.util.Log.w(tag, message)
            Level.ERROR -> android.util.Log.e(tag, message)
        }

        // 异步写入文件
        scope.launch {
            try {
                val file = currentLogFile() ?: return@launch
                rotateIfNeeded(file)
                val timestamp = dateFormat.format(Date())
                val masked = maskSensitive(message)
                file.appendText("[$timestamp] ${level.tag}/$tag: $masked\n")
            } catch (_: Exception) {
                // 忽略日志写入失败
            }
        }
    }

    private fun rotateIfNeeded(file: File) {
        if (file.exists() && file.length() > MAX_FILE_SIZE) {
            val dir = file.parentFile ?: return
            val files = dir.listFiles { f -> f.name.startsWith("log_") && f.name.endsWith(".txt") }
                ?.sortedBy { it.lastModified() }?.toMutableList() ?: return
            // 删除最旧的，保留最多 MAX_FILES 个
            while (files.size >= MAX_FILES) {
                val oldest = files.removeAt(0)
                oldest.delete()
            }
            val rotated = File(dir, "log_${fileDateFormat.format(Date())}_${System.currentTimeMillis()}.txt")
            file.renameTo(rotated)
        }
    }

    /**
     * 敏感信息脱敏：路径、URI、包名等。
     */
    private fun maskSensitive(input: String): String {
        var result = input
        // 隐藏 content:// URI 的具体路径
        result = result.replace(Regex("content://[^\\s]+"), "content://***")
        // 隐藏 file:// 路径
        result = result.replace(Regex("file://[^\\s]+"), "file://***")
        // 隐藏绝对路径（/data/..., /storage/...）
        result = result.replace(Regex("""/(data|storage|sdcard|mnt)/[^\s,)]+"""), "/$1/***")
        // 隐藏 http(s) URL 的查询参数
        result = result.replace(Regex("(https?://[^\\s?]+)\\?[^\\s]+"), "$1?***")
        return result
    }

    /**
     * 导出日志：通过分享 Intent 发送日志文件。
     */
    fun exportLogs(context: Context) {
        scope.launch {
            try {
                val dir = logDir() ?: return@launch
                val files = dir.listFiles { f -> f.extension == "txt" }?.toList() ?: emptyList()
                if (files.isEmpty()) {
                    android.util.Log.w(TAG, "无日志可导出")
                    return@launch
                }
                // 打包所有日志到一个 zip 或直接分享最新的
                val latest = files.maxByOrNull { it.lastModified() } ?: return@launch
                val uri: Uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    latest
                )
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                val chooser = Intent.createChooser(shareIntent, "导出日志").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(chooser)
            } catch (e: Exception) {
                android.util.Log.e(TAG, "导出日志失败", e)
            }
        }
    }

    /** 列出所有日志文件（供设置页显示）。 */
    fun listLogFiles(): List<File> {
        val dir = logDir() ?: return emptyList()
        return dir.listFiles { f -> f.extension == "txt" }?.sortedByDescending { it.lastModified() } ?: emptyList()
    }
}
