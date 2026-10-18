package com.widgetfactory.app.action

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.wifi.WifiManager
import android.os.Build
import com.widgetfactory.app.model.ActionId
import com.widgetfactory.app.model.ActionParam
import com.widgetfactory.app.model.SystemOpId
import com.widgetfactory.app.ui.SnackbarManager

/**
 * 执行系统操作：按固定子项清单实现，每项声明权限。
 */
class SystemOpActionExecutor : ActionExecutor {
    override val actionId = ActionId.SYSTEM_OP

    override fun execute(
        context: Context,
        param: ActionParam,
        snackbarManager: SnackbarManager?
    ): Boolean {
        val op = param.systemOpId ?: run {
            snackbarManager?.show("未指定系统操作")
            return false
        }
        return try {
            when (op) {
                SystemOpId.VOLUME_UP -> adjustVolume(context, AudioManager.ADJUST_RAISE)
                SystemOpId.VOLUME_DOWN -> adjustVolume(context, AudioManager.ADJUST_LOWER)
                SystemOpId.TOGGLE_BLUETOOTH -> toggleBluetooth(context)
                SystemOpId.TOGGLE_WIFI -> toggleWifi(context)
                SystemOpId.EXPAND_NOTIFICATIONS -> expandNotifications(context)
                SystemOpId.LOCK_SCREEN -> lockScreen(context)
            }
            true
        } catch (e: Exception) {
            snackbarManager?.show("系统操作失败：${op.label}")
            false
        }
    }

    private fun adjustVolume(context: Context, direction: Int) {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        am.adjustStreamVolume(AudioManager.STREAM_MUSIC, direction, AudioManager.FLAG_SHOW_UI)
    }

    private fun toggleBluetooth(context: Context) {
        // BluetoothAdapter 在 API 33+ 需要 BLUETOOTH_CONNECT 权限
        val adapter = android.bluetooth.BluetoothAdapter.getDefaultAdapter()
        if (adapter != null) {
            if (adapter.isEnabled) adapter.disable() else adapter.enable()
        }
    }

    private fun toggleWifi(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // API 29+ 无法直接切换 Wi-Fi，跳转系统面板
            try {
                val intent = Intent(android.provider.Settings.Panel.ACTION_WIFI).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (_: Exception) {
                // 忽略
            }
        } else {
            @Suppress("DEPRECATION")
            val wifi = context.applicationContext
                .getSystemService(Context.WIFI_SERVICE) as WifiManager
            @Suppress("DEPRECATION")
            wifi.isWifiEnabled = !wifi.isWifiEnabled
        }
    }

    private fun expandNotifications(context: Context) {
        // 通过 StatusBarManagerService（反射）展开通知栏
        try {
            val statusBarService = context.getSystemService("statusbar")
            val statusBarManager = Class.forName("android.app.StatusBarManager")
            val expand = statusBarManager.getMethod("expandNotificationsPanel")
            expand.invoke(statusBarService)
        } catch (_: Exception) {
            // 部分设备不支持
        }
    }

    private fun lockScreen(context: Context) {
        // 需要设备管理员权限；此处尝试通过 DevicePolicyManager 锁屏
        // 若未授权则提示用户
        try {
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE)
                as android.app.admin.DevicePolicyManager
            if (dpm.isAdminActive(
                    android.content.ComponentName(
                        context,
                        com.widgetfactory.app.action.AdminReceiver::class.java
                    )
                )
            ) {
                dpm.lockNow()
            }
        } catch (_: Exception) {
            // 忽略
        }
    }
}

/** 设备管理员接收器（用于锁屏等系统操作，需用户在系统设置中激活）。 */
class AdminReceiver : android.app.admin.DeviceAdminReceiver()
