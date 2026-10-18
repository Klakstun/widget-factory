package com.widgetfactory.app.data

import android.content.Context
import com.widgetfactory.app.log.Logger
import com.widgetfactory.app.model.Component
import kotlinx.serialization.json.Json
import java.io.File

/**
 * 组件配置 JSON 存储。
 * 序列化格式为 JSON，存储于应用内部存储 files/configs/。
 * Widget 通过文件共享读取配置。
 */
object ComponentStore {
    private const val CONFIG_DIR = "configs"
    private const val TAG = "ComponentStore"

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private fun configDir(context: Context): File {
        val dir = File(context.filesDir, CONFIG_DIR)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun save(context: Context, component: Component) {
        try {
            val file = File(configDir(context), "${component.id}.json")
            file.writeText(json.encodeToString(Component.serializer(), component))
            Logger.i(TAG, "Saved component ${component.id}")
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to save component ${component.id}", e)
        }
    }

    fun load(context: Context, componentId: String): Component? {
        return try {
            val file = File(configDir(context), "$componentId.json")
            if (!file.exists()) return null
            json.decodeFromString(Component.serializer(), file.readText())
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to load component $componentId", e)
            null
        }
    }

    fun loadAll(context: Context): List<Component> {
        val dir = configDir(context)
        val files = dir.listFiles { f -> f.extension == "json" } ?: return emptyList()
        return files.mapNotNull { file ->
            try {
                json.decodeFromString(Component.serializer(), file.readText())
            } catch (e: Exception) {
                Logger.e(TAG, "Failed to parse ${file.name}", e)
                null
            }
        }.sortedByDescending { it.updatedAt }
    }

    fun delete(context: Context, componentId: String): Boolean {
        return try {
            val file = File(configDir(context), "$componentId.json")
            file.delete().also {
                Logger.i(TAG, "Deleted component $componentId: $it")
            }
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to delete component $componentId", e)
            false
        }
    }

    /** 将组件序列化为 JSON 字符串（供 Widget 通过 ContentProvider/Intent 传递）。 */
    fun toJson(component: Component): String =
        json.encodeToString(Component.serializer(), component)

    fun fromJson(jsonString: String): Component? =
        try {
            json.decodeFromString(Component.serializer(), jsonString)
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to parse component JSON", e)
            null
        }
}
