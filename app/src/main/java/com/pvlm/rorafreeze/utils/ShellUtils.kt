package com.pvlm.rorafreeze.utils

import android.content.Context

object ShellUtils {

    fun isRootEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences("freeze_apps_prefs", Context.MODE_PRIVATE)
        return prefs.getBoolean("use_root", false)
    }

    fun isAvailable(context: Context): Boolean {
        return if (isRootEnabled(context)) {
            RootUtils.isRootAvailable()
        } else {
            ShizukuUtils.isShizukuAvailable()
        }
    }

    fun hasPermission(context: Context): Boolean {
        return if (isRootEnabled(context)) {
            RootUtils.isRootPermissionGranted()
        } else {
            ShizukuUtils.hasPermission()
        }
    }

    fun runCommand(context: Context, command: String) {
        if (isRootEnabled(context)) {
            RootUtils.runCommand(command)
        } else {
            if (!ShizukuUtils.isShizukuAvailable()) return
            if (!ShizukuUtils.hasPermission()) return
            ShizukuUtils.runCommand(command)
        }
    }

    fun runCommandWithOutput(context: Context, command: String): String? {
        return try {
            val process = newProcess(context, arrayOf("sh", "-c", command))
            process?.inputStream?.bufferedReader()?.use { it.readText() }?.trim()
        } catch (e: Exception) {
            null
        }
    }

    fun newProcess(context: Context, command: Array<String>): Process? {
        return if (isRootEnabled(context)) {
            RootUtils.newProcess(command)
        } else {
            if (!ShizukuUtils.isShizukuAvailable()) return null
            if (!ShizukuUtils.hasPermission()) return null
            ShizukuProcessHelper.newProcess(command)
        }
    }
}