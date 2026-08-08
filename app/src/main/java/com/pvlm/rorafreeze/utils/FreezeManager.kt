package com.pvlm.rorafreeze.utils

import android.content.Context
import android.os.Build
import android.os.IBinder
import android.util.Log
import org.lsposed.hiddenapibypass.HiddenApiBypass
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.SystemServiceHelper

object FreezeManager {
    private const val TAG = "FreezeManager"

    // Hidden API constants
    private const val COMPONENT_ENABLED_STATE_DEFAULT = 0
    private const val COMPONENT_ENABLED_STATE_ENABLED = 1
    private const val COMPONENT_ENABLED_STATE_DISABLED = 2
    private const val COMPONENT_ENABLED_STATE_DISABLED_USER = 3
    private const val COMPONENT_ENABLED_STATE_DISABLED_UNTIL_USED = 4

    /**
     * Freeze an application using Shizuku or Root (always disables the app).
     */
    fun freezeApp(context: Context, packageName: String): Boolean {
        return setApplicationEnabledSetting(
            context,
            packageName,
            COMPONENT_ENABLED_STATE_DISABLED_USER
        )
    }

    /**
     * Unfreeze an application using Shizuku or Root (always re-enables the app).
     */
    fun unfreezeApp(context: Context, packageName: String): Boolean {
        return setApplicationEnabledSetting(context, packageName, COMPONENT_ENABLED_STATE_ENABLED)
    }

    fun freezeApps(context: Context, packageNames: Collection<String>) {
        packageNames.forEach { freezeApp(context, it) }
    }

    fun unfreezeApps(context: Context, packageNames: Collection<String>) {
        packageNames.forEach { unfreezeApp(context, it) }
    }

    /**
     * Check if an application is currently frozen/disabled/suspended.
     */
    fun isAppFrozen(context: Context, packageName: String): Boolean {
        return try {
            val state = context.packageManager.getApplicationEnabledSetting(packageName)
            state == COMPONENT_ENABLED_STATE_DISABLED_USER || state == COMPONENT_ENABLED_STATE_DISABLED
        } catch (e: Exception) {
            false
        }
    }

    private fun getService(serviceName: String, stubClassName: String): Any? {
        return try {
            val binder = SystemServiceHelper.getSystemService(serviceName) ?: return null
            val stubClass = Class.forName(stubClassName)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                HiddenApiBypass.invoke(stubClass, null, "asInterface", ShizukuBinderWrapper(binder))
            } else {
                stubClass.getMethod("asInterface", IBinder::class.java)
                    .invoke(null, ShizukuBinderWrapper(binder))
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun getUserId(): Int {
        return try {
            val userHandle = android.os.Process.myUserHandle()
            val method = userHandle.javaClass.getMethod("getIdentifier")
            method.invoke(userHandle) as Int
        } catch (_: Exception) {
            0
        }
    }

    private fun setApplicationEnabledSetting(
        context: Context,
        packageName: String,
        newState: Int
    ): Boolean {
        // 1. Try Shizuku first
        if (ShizukuUtils.isShizukuAvailable() && ShizukuUtils.hasPermission()) {
            try {
                val pm = getService("package", "android.content.pm.IPackageManager\$Stub")
                if (pm != null) {
                    val userId = getUserId()
                    Log.d(
                        "FreezeManager",
                        "Shizuku: setting $packageName to $newState for user $userId"
                    )
                    HiddenApiBypass.invoke(
                        pm.javaClass, pm, "setApplicationEnabledSetting",
                        packageName, newState, 0, userId, "android"
                    )
                    return true
                }
            } catch (e: Exception) {
                Log.e("FreezeManager", "Shizuku call failed", e)
            }
        }

        // 2. Fallback to Shell (Root)
        if (!ShellUtils.hasPermission(context)) return false

        val cmd = when (newState) {
            COMPONENT_ENABLED_STATE_DISABLED_USER -> "pm disable-user --user 0 $packageName"
            COMPONENT_ENABLED_STATE_ENABLED -> "pm enable $packageName"
            else -> return false
        }

        return try {
            ShellUtils.runCommand(context, cmd)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}