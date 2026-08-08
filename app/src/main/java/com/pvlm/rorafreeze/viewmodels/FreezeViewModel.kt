package com.pvlm.rorafreeze.viewmodels

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.util.Base64
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import com.google.gson.Gson
import com.pvlm.rorafreeze.domain.model.FreezeMode
import com.pvlm.rorafreeze.utils.FreezeManager
import com.pvlm.rorafreeze.utils.RootUtils
import com.pvlm.rorafreeze.utils.ShellUtils
import com.pvlm.rorafreeze.utils.ShizukuUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

class FreezeViewModel(app: Application) : AndroidViewModel(app) {

    private val viewModelScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val prefs
        get() = getApplication<Application>()
            .getSharedPreferences("freeze_apps_prefs", Context.MODE_PRIVATE)

    val isShizukuAvailable = mutableStateOf(false)

    val isShizukuPermissionGranted = mutableStateOf(false)

    val isRootAvailable = mutableStateOf(RootUtils.isRootAvailable())

    val isRootEnabled = mutableStateOf(prefs.getBoolean("use_root", false))

    val allowSystemApps = mutableStateOf(prefs.getBoolean("allow_system_apps", false))

    val gson = Gson()

    init {
        refreshPermissionState()
    }

    fun check(context: Context) {
        refreshPermissionState()
        if (isRootEnabled.value || isShizukuPermissionGranted.value) {
            ensureExportFolder(context)
        }
    }

    fun refreshPermissionState() {
        isShizukuAvailable.value = ShizukuUtils.isShizukuAvailable()
        isShizukuPermissionGranted.value = ShizukuUtils.hasPermission()
        isRootAvailable.value = RootUtils.isRootAvailable()
        if (!isRootAvailable.value && isRootEnabled.value) {
            isRootEnabled.value = false
            prefs.edit().putBoolean("use_root", false).apply()
        }
    }

    val freezeModes = mutableStateOf(loadModes())
    val activeModeId = mutableStateOf(prefs.getString("active_mode_id", null))

    private fun loadModes(): List<FreezeMode> {
        val json = prefs.getString("freeze_modes", null) ?: return emptyList()
        return try {
            gson.fromJson(json, Array<FreezeMode>::class.java).toList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun persistModes(modes: List<FreezeMode>) {
        prefs.edit().putString("freeze_modes", gson.toJson(modes)).apply()
        freezeModes.value = modes
    }

    fun addMode(name: String): FreezeMode? {
        if (name.isBlank()) return null
        val mode = FreezeMode(id = UUID.randomUUID().toString(), name = name.trim(), packageNames = emptyList())
        persistModes(freezeModes.value + mode)
        return mode
    }

    fun frozenCountInMode(context: Context, mode: FreezeMode): Int {
        return mode.packageNames.count { FreezeManager.isAppFrozen(context, it) }
    }

    fun updateModeApps(context: Context, modeId: String, packageNames: List<String>) {
        val previous = freezeModes.value.firstOrNull { it.id == modeId }?.packageNames ?: emptyList()
        val distinct = packageNames.distinct()
        val removed = previous.filterNot { it in distinct }
        val added = distinct.filterNot { it in previous }
        persistModes(
            freezeModes.value.map { if (it.id == modeId) it.copy(packageNames = distinct) else it }
        )
        if (activeModeId.value == modeId) {
            viewModelScope.launch {
                withContext(Dispatchers.IO) {
                    if (added.isNotEmpty()) FreezeManager.freezeApps(context, added)
                    if (removed.isNotEmpty()) FreezeManager.unfreezeApps(context, removed)
                }
            }
        }
    }

    fun renameMode(modeId: String, name: String) {
        if (name.isBlank()) return
        persistModes(
            freezeModes.value.map { if (it.id == modeId) it.copy(name = name.trim()) else it }
        )
    }

    fun deleteMode(context: Context, modeId: String) {
        val mode = freezeModes.value.firstOrNull { it.id == modeId }
        val remaining = freezeModes.value.filterNot { it.id == modeId }
        persistModes(remaining)
        if (activeModeId.value == modeId) {
            activeModeId.value = null
            prefs.edit().putString("active_mode_id", null).apply()
            mode?.packageNames?.let { packages ->
                viewModelScope.launch {
                    withContext(Dispatchers.IO) { FreezeManager.unfreezeApps(context, packages) }
                }
            }
        }
    }

    fun freezeAppFromMode(context: Context, packageName: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { FreezeManager.freezeApp(context, packageName) }
        }
    }

    fun unfreezeAppFromMode(context: Context, packageName: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { FreezeManager.unfreezeApp(context, packageName) }
        }
    }

    fun activateMode(context: Context, modeId: String) {
        viewModelScope.launch {
            val mode = freezeModes.value.firstOrNull { it.id == modeId }
            withContext(Dispatchers.IO) {
                val allPackages = freezeModes.value.flatMap { it.packageNames }.distinct()
                FreezeManager.unfreezeApps(context, allPackages)
                mode?.packageNames?.let { FreezeManager.freezeApps(context, it) }
            }
            activeModeId.value = modeId
            prefs.edit().putString("active_mode_id", modeId).apply()
        }
    }

    fun deactivateMode(context: Context, modeId: String) {
        viewModelScope.launch {
            val mode = freezeModes.value.firstOrNull { it.id == modeId }
            withContext(Dispatchers.IO) {
                mode?.packageNames?.let { FreezeManager.unfreezeApps(context, it) }
            }
            activeModeId.value = null
            prefs.edit().putString("active_mode_id", null).apply()
        }
    }

    fun launchAndUnfreezeApp(context: Context, packageName: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { FreezeManager.unfreezeApp(context, packageName) }
            val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
            launchIntent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (launchIntent != null) context.startActivity(launchIntent)
        }
    }

    fun openAppDetails(context: Context, packageName: String) {
        context.startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
    }

    fun setUseRoot(enabled: Boolean) {
        prefs.edit().putBoolean("use_root", enabled).apply()
        isRootEnabled.value = enabled
    }

    fun setAllowSystemApps(enabled: Boolean) {
        prefs.edit().putBoolean("allow_system_apps", enabled).apply()
        allowSystemApps.value = enabled
    }

    fun exportModes(stream: OutputStream) {
        try {
            stream.bufferedWriter().use { writer -> writer.write(gson.toJson(freezeModes.value)) }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun exportMode(stream: OutputStream, modeId: String) {
        try {
            val mode = freezeModes.value.firstOrNull { it.id == modeId } ?: return
            stream.bufferedWriter().use { writer -> writer.write(gson.toJson(listOf(mode))) }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object {
        const val EXPORT_DIR = "/storage/emulated/0/RoraFreeze"

        private fun safeFileName(name: String): String {
            val sanitized = name.replace(Regex("[^a-zA-Z0-9. _\\-]"), "_").trim().replace(' ', '_')
            return if (sanitized.isEmpty()) "freeze_mode" else sanitized
        }
    }

    fun exportModeToFolder(context: Context, modeId: String): Boolean {
        val mode = freezeModes.value.firstOrNull { it.id == modeId } ?: return false
        return exportToFolder(context, "${safeFileName(mode.name)}.prrf", gson.toJson(listOf(mode)))
    }

    fun exportModesToFolder(context: Context): Boolean {
        return exportToFolder(context, "rorafreeze_modes_backup.prrf", gson.toJson(freezeModes.value))
    }

    fun ensureExportFolder(context: Context) {
        ShellUtils.runCommand(context, "mkdir -p \"$EXPORT_DIR\"")
    }

    private fun exportToFolder(context: Context, fileName: String, json: String): Boolean {
        val base64 = Base64.encodeToString(json.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
        ensureExportFolder(context)
        val output = ShellUtils.runCommandWithOutput(
            context,
            "echo \"$base64\" | base64 -d > \"$EXPORT_DIR/$fileName\" && echo EXPORT_OK"
        )
        return output?.contains("EXPORT_OK") == true
    }

    fun importModes(context: Context, stream: InputStream) {
        try {
            val json = stream.bufferedReader().use { it.readText() }
            val modes = gson.fromJson(json, Array<FreezeMode>::class.java).toList()
            if (modes.isEmpty()) return
            val merged = (freezeModes.value + modes).distinctBy { it.name }.sortedBy { it.name }
            persistModes(merged)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
