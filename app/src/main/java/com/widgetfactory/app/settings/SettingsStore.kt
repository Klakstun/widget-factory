package com.widgetfactory.app.settings

import android.content.Context

/**
 * 设置存储：降级开关、主题配置等，持久化到 SharedPreferences。
 */
object SettingsStore {
    private const val PREFS = "app_settings"

    // 降级相关
    private const val KEY_FINE_RENDERING = "fine_rendering_enabled"
    private const val KEY_HTML_EXTERNAL_CSS = "html_external_css"
    private const val KEY_HTML_EXTERNAL_JS = "html_external_js"
    private const val KEY_HTML_INLINE_JS = "html_inline_js"

    // 主题
    private const val KEY_DYNAMIC_COLOR = "dynamic_color"
    private const val KEY_THEME_MODE = "theme_mode" // "system" | "light" | "dark"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun isFineRenderingEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_FINE_RENDERING, true)

    fun setFineRenderingEnabled(context: Context, enabled: Boolean) =
        prefs(context).edit().putBoolean(KEY_FINE_RENDERING, enabled).apply()

    fun isHtmlExternalCssEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_HTML_EXTERNAL_CSS, true)

    fun setHtmlExternalCssEnabled(context: Context, enabled: Boolean) =
        prefs(context).edit().putBoolean(KEY_HTML_EXTERNAL_CSS, enabled).apply()

    fun isHtmlExternalJsEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_HTML_EXTERNAL_JS, true)

    fun setHtmlExternalJsEnabled(context: Context, enabled: Boolean) =
        prefs(context).edit().putBoolean(KEY_HTML_EXTERNAL_JS, enabled).apply()

    fun isHtmlInlineJsEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_HTML_INLINE_JS, true)

    fun setHtmlInlineJsEnabled(context: Context, enabled: Boolean) =
        prefs(context).edit().putBoolean(KEY_HTML_INLINE_JS, enabled).apply()

    fun isDynamicColorEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_DYNAMIC_COLOR, true)

    fun setDynamicColorEnabled(context: Context, enabled: Boolean) =
        prefs(context).edit().putBoolean(KEY_DYNAMIC_COLOR, enabled).apply()

    fun getThemeMode(context: Context): String =
        prefs(context).getString(KEY_THEME_MODE, "system") ?: "system"

    fun setThemeMode(context: Context, mode: String) =
        prefs(context).edit().putString(KEY_THEME_MODE, mode).apply()
}
