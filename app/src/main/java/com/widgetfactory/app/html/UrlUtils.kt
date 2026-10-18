package com.widgetfactory.app.html

import android.net.Uri

/**
 * URL 规范化工具。
 * - 协议相对 URL（//）补全为 https:
 * - 相对路径基于设定 baseUrl 解析
 */
object UrlUtils {

    /**
     * 将可能是协议相对或相对路径的 URL 规范化为绝对 URL。
     * @param input 原始 URL（可能是 //example.com/a 或 /a/b 或 https://...）
     * @param baseUrl 基础 URL，用于解析相对路径
     */
    fun normalize(input: String, baseUrl: String? = null): String {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return trimmed

        // 已是绝对 URL
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return trimmed
        }

        // 协议相对 URL（//）→ 补全 https
        if (trimmed.startsWith("//")) {
            return "https:$trimmed"
        }

        // 相对路径 → 基于 baseUrl 解析
        if (baseUrl != null) {
            return try {
                val base = Uri.parse(baseUrl)
                val baseScheme = base.scheme ?: "https"
                val baseHost = base.host ?: ""
                if (trimmed.startsWith("/")) {
                    // 绝对路径
                    "$baseScheme://$baseHost$trimmed"
                } else {
                    // 相对路径
                    val basePath = base.path?.let { p ->
                        if (p.endsWith("/")) p else p.substringBeforeLast('/') + "/"
                    } ?: "/"
                    "$baseScheme://$baseHost$basePath$trimmed"
                }
            } catch (_: Exception) {
                trimmed
            }
        }

        return trimmed
    }

    /** 从 HTML 中提取第一个 iframe 的 src。 */
    fun extractIframeSrc(html: String): String? {
        val regex = Regex("""<iframe[^>]*src\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE)
        return regex.find(html)?.groupValues?.getOrNull(1)
    }

    /** 判断 HTML 是否包含 iframe。 */
    fun containsIframe(html: String): Boolean {
        return Regex("""<iframe""", RegexOption.IGNORE_CASE).containsMatchIn(html)
    }
}
