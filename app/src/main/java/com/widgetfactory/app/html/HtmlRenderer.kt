package com.widgetfactory.app.html

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.http.SslError
import android.webkit.SslErrorHandler
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.google.accompanist.web.AccompanistWebViewClient
import com.google.accompanist.web.WebView
import com.google.accompanist.web.rememberWebViewState
import com.widgetfactory.app.log.Logger

private const val TAG = "HtmlRenderer"

/**
 * HTML 渲染降级层级。
 */
enum class HtmlDegradeLevel {
    /** 正常：HTML + 外部 CSS/JS + 内联 CSS/JS。 */
    NORMAL,
    /** 关闭外部 CSS。 */
    NO_EXTERNAL_CSS,
    /** 关闭外部 JS。 */
    NO_EXTERNAL_JS,
    /** 关闭内联 JS。 */
    NO_INLINE_JS,
    /** 显示原始 HTML 源码。 */
    SOURCE_ONLY
}

/**
 * HTML 渲染器：沙箱 WebView，支持多级降级。
 * - 普通 HTML：5 级降级链，每级写日志，下次加载从头重试。
 * - iframe 外部嵌入：简化降级，失败直接显示源码。
 */
@Composable
fun HtmlRenderer(
    html: String,
    baseUrl: String? = null,
    modifier: Modifier = Modifier
) {
    val isIframe = UrlUtils.containsIframe(html)
    // 降级层级，每次 html 变化时从头开始（NORMAL）
    var level by remember(html) { mutableStateOf(HtmlDegradeLevel.NORMAL) }

    // rememberWebViewState 的 url 参数非空，初始传空串，内容稍后通过 loadDataWithBaseURL 加载
    val state = rememberWebViewState(url = "")
    var loadVersion by remember { mutableIntStateOf(0) }

    WebView(
        state = state,
        modifier = modifier.fillMaxSize(),
        onCreated = { webView -> configureWebView(webView) },
        client = object : AccompanistWebViewClient() {
            override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                loadVersion++
            }

            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)
                loadVersion++ // 取消超时
            }

            override fun onReceivedError(
                view: WebView,
                request: WebResourceRequest,
                error: WebResourceError
            ) {
                super.onReceivedError(view, request, error)
                if (request.isForMainFrame) {
                    Logger.w(TAG, "主文档加载失败: ${error.description} level=$level")
                    level = nextLevel(level, isIframe)
                }
            }

            override fun onReceivedSslError(
                view: WebView,
                handler: SslErrorHandler,
                error: SslError
            ) {
                handler.cancel()
                Logger.w(TAG, "SSL 错误: ${error.url} level=$level")
                level = nextLevel(level, isIframe)
            }
        }
    )

    // 加载超时：10 秒未完成则触发降级
    LaunchedEffect(loadVersion, level) {
        val snapshot = loadVersion
        kotlinx.coroutines.delay(10_000)
        if (loadVersion == snapshot && state.isLoading) {
            Logger.w(TAG, "加载超时，触发降级 level=$level")
            level = nextLevel(level, isIframe)
        }
    }

    // 加载内容：html 或 level 变化时重新加载
    LaunchedEffect(html, level) {
        val content = if (level == HtmlDegradeLevel.SOURCE_ONLY) {
            buildSourceView(html)
        } else {
            applyDegradeToHtml(html, level)
        }
        state.loadDataWithBaseURL(baseUrl, content, "text/html", "UTF-8", null)
    }
}

/**
 * 计算下一级降级。
 * - 普通 HTML：NORMAL → NO_EXTERNAL_CSS → NO_EXTERNAL_JS → NO_INLINE_JS → SOURCE_ONLY
 * - iframe：直接降级到 SOURCE_ONLY
 */
private fun nextLevel(current: HtmlDegradeLevel, isIframe: Boolean): HtmlDegradeLevel {
    if (isIframe) return HtmlDegradeLevel.SOURCE_ONLY
    return when (current) {
        HtmlDegradeLevel.NORMAL -> HtmlDegradeLevel.NO_EXTERNAL_CSS
        HtmlDegradeLevel.NO_EXTERNAL_CSS -> HtmlDegradeLevel.NO_EXTERNAL_JS
        HtmlDegradeLevel.NO_EXTERNAL_JS -> HtmlDegradeLevel.NO_INLINE_JS
        HtmlDegradeLevel.NO_INLINE_JS -> HtmlDegradeLevel.SOURCE_ONLY
        HtmlDegradeLevel.SOURCE_ONLY -> HtmlDegradeLevel.SOURCE_ONLY
    }
}

@SuppressLint("SetJavaScriptEnabled")
private fun configureWebView(webView: WebView) {
    webView.settings.apply {
        // 允许 JS（否则播放器不可用）
        javaScriptEnabled = true
        // 限制文件访问与非必要权限
        allowFileAccess = false
        allowContentAccess = false
        allowFileAccessFromFileURLs = false
        allowUniversalAccessFromFileURLs = false
        // 混合内容策略：不允许混合内容
        mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
        // 第三方 Cookie 策略：不接受第三方 Cookie
        android.webkit.CookieManager.getInstance().setAcceptThirdPartyCookies(webView, false)
    }
}

/** 对 HTML 应用降级处理（移除对应外部/内联资源）。 */
private fun applyDegradeToHtml(html: String, level: HtmlDegradeLevel): String {
    var result = html
    when (level) {
        HtmlDegradeLevel.NO_EXTERNAL_CSS -> {
            result = result.replace(
                Regex("""<link[^>]*rel\s*=\s*["']stylesheet["'][^>]*>""", RegexOption.IGNORE_CASE),
                ""
            )
        }
        HtmlDegradeLevel.NO_EXTERNAL_JS -> {
            result = result.replace(
                Regex("""<link[^>]*rel\s*=\s*["']stylesheet["'][^>]*>""", RegexOption.IGNORE_CASE),
                ""
            )
            result = result.replace(
                Regex("""<script[^>]*src\s*=\s*["'][^"']+["'][^>]*>\s*</script>""", RegexOption.IGNORE_CASE),
                ""
            )
        }
        HtmlDegradeLevel.NO_INLINE_JS -> {
            result = result.replace(
                Regex("""<link[^>]*rel\s*=\s*["']stylesheet["'][^>]*>""", RegexOption.IGNORE_CASE),
                ""
            )
            result = result.replace(
                Regex("""<script[^>]*>\s*[\s\S]*?\s*</script>""", RegexOption.IGNORE_CASE),
                ""
            )
        }
        else -> {}
    }
    return result
}

/** 构建源码展示 HTML（转义原始内容）。 */
private fun buildSourceView(rawHtml: String): String {
    val escaped = rawHtml
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
    return "<html><body style='white-space:pre-wrap;font-family:monospace;font-size:12px;'>$escaped</body></html>"
}
