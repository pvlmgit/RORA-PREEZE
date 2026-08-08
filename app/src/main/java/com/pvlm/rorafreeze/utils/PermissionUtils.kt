package com.pvlm.rorafreeze.utils

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Process
import android.provider.Settings
import android.os.UserHandle

object PermissionUtils {

    fun hasUsageStatsPermission(context: Context): Boolean {
        return try {
            val appOpsManager = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                appOpsManager.unsafeCheckOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(),
                    context.packageName
                )
            } else {
                @Suppress("DEPRECATION")
                appOpsManager.checkOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(),
                    context.packageName
                )
            }
            mode == AppOpsManager.MODE_ALLOWED
        } catch (e: Exception) {
            false
        }
    }

    fun openUsageAccessSettings(context: Context) {
        context.startActivity(
            Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    fun hasNotificationListenerPermission(@Suppress("UNUSED_PARAMETER") context: Context): Boolean = true

    fun openNotificationListenerSettings(@Suppress("UNUSED_PARAMETER") context: Context) {}

    fun hasAccessibilityEnabled(@Suppress("UNUSED_PARAMETER") context: Context): Boolean = false

    fun openAccessibilitySettings(context: Context) {
        context.startActivity(
            Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    fun hasShizukuPermission(): Boolean = ShizukuUtils.hasPermission()

    fun requestShizukuPermission() = ShizukuUtils.requestPermission()
}