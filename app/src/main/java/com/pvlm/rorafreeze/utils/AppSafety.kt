package com.pvlm.rorafreeze.utils

import android.content.Context
import android.content.Intent
import com.pvlm.rorafreeze.domain.model.NotificationApp

/**
 * Classifies installed apps into safety buckets used by the picker.
 *
 * This is a CONSERVATIVE heuristic, not a guarantee:
 *  - CRITICAL   : core system components / the launcher / any system app we
 *                 cannot positively identify as safe. We never recommend disabling these.
 *  - RECOMMENDED: known bloatware that is safe and beneficial to freeze.
 *  - SAFE       : apps the user installed themselves (always re-enableable).
 */
enum class AppSafetyLevel {
    RECOMMENDED,
    SAFE,
    CRITICAL
}

object AppSafety {

    // Fragile system packages that should never be disabled.
    private val CRITICAL_PACKAGES = setOf(
        "com.android.systemui",
        "com.android.settings",
        "com.android.phone",
        "com.android.server.telecom",
        "com.android.incallui",
        "com.android.dialer",
        "com.google.android.dialer",
        "com.android.contacts",
        "com.google.android.contacts",
        "com.android.mms",
        "com.android.providers.contacts",
        "com.android.providers.telephony",
        "com.android.providers.media",
        "com.android.providers.media.module",
        "com.android.providers.settings",
        "com.android.providers.calendar",
        "com.android.launcher3",
        "com.android.launcher",
        "com.android.quickstep",
        "com.google.android.quickstep",
        "com.google.android.apps.nexuslauncher",
        "com.miui.home",
        "com.android.inputmethod.latin",
        "com.google.android.inputmethod.latin",
        "com.android.permissioncontroller",
        "com.android.packageinstaller",
        "com.google.android.packageinstaller",
        "com.android.documentsui",
        "com.android.externalstorage",
        "com.android.defcontainer",
        "com.android.backupconfirm",
        "com.android.provision",
        "com.android.emergency",
        "com.android.printspooler",
        "com.android.statementservice",
        "com.android.bluetooth",
        "com.android.bluetooth.services",
        "com.android.nfc",
        "com.android.wifi",
        "com.android.location.fused",
        "com.android.shell",
        "com.android.vpndialogs",
        "com.android.webview",
        "com.google.android.webview",
        "com.android.certinstaller",
        "com.android.cellbroadcastreceiver",
        "com.android.settings.intelligence",
        "com.google.android.settings.intelligence",
        "com.android.smspush",
        "com.android.ons",
        "com.google.android.gms",
        "com.google.android.gsf",
        "com.google.android.gsf.login",
        "com.android.inputdevices",
        "com.android.sharedstoragebackup",
        "moe.shizuku.manager",
        "moe.shizuku.privileged.api",
        "moe.shizuku.api"
    )

    // Common bloatware that is safe (and recommended) to freeze.
    private val RECOMMENDED_PACKAGES = setOf(
        "com.facebook.katana",
        "com.facebook.orca",
        "com.facebook.system",
        "com.facebook.appmanager",
        "com.facebook.services",
        "com.instagram.android",
        "com.google.android.youtube",
        "com.google.android.gm",
        "com.google.android.apps.maps",
        "com.netflix.mediaclient",
        "com.spotify.music",
        "com.zhiliaoapp.musically",
        "com.ss.android.ugc.aweme",
        "com.miui.cleanmaster",
        "com.miui.notes",
        "com.miui.video",
        "com.miui.videoplayer",
        "com.miui.player",
        "com.miui.music",
        "com.miui.gallery",
        "com.miui.bugreport",
        "com.miui.analytics",
        "com.miui.miservice",
        "com.miui.msa.global",
        "com.miui.systemAdSolution",
        "com.miui.weather2",
        "com.miui.touchassistant",
        "com.miui.yellowpage",
        "com.miui.freeform",
        "com.miui.compass",
        "com.miui.calculator",
        "com.miui.misound",
        "com.miui.ab",
        "com.miui.audiomonitor",
        "com.xiaomi.glgm",
        "com.xiaomi.discover",
        "com.xiaomi.joyose",
        "com.xiaomi.mipicks",
        "com.xiaomi.mico",
        "com.xiaomi.vipaccount",
        "com.xiaomi.scanner",
        "com.xiaomi.midrop",
        "com.mi.globalbrowser",
        "com.if.inn.photomaster",
        "com.duokan.phone.remotecontroller",
        "com.duokan.reader"
    )

    private var cachedLauncherPackage: String? = null

    fun getSafetyLevel(context: Context, app: NotificationApp): AppSafetyLevel {
        val packageName = app.packageName
        if (packageName == context.packageName) return AppSafetyLevel.CRITICAL
        if (packageName in RECOMMENDED_PACKAGES) return AppSafetyLevel.RECOMMENDED
        if (isCriticalPackage(context, packageName)) return AppSafetyLevel.CRITICAL
        // Only apps the user installed themselves are marked "safe to disable".
        if (!app.isSystemApp) return AppSafetyLevel.SAFE
        // UNKNOWN system app -> be conservative and do not recommend disabling.
        return AppSafetyLevel.CRITICAL
    }

    private fun isCriticalPackage(context: Context, packageName: String): Boolean {
        if (packageName in CRITICAL_PACKAGES) return true
        val home = defaultLauncher(context) ?: return false
        return packageName == home
    }

    private fun defaultLauncher(context: Context): String? {
        cachedLauncherPackage?.let { return it.ifEmpty { null } }
        return try {
            val home = context.packageManager.resolveActivity(
                Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME),
                0
            )
            cachedLauncherPackage = home?.activityInfo?.packageName ?: ""
            home?.activityInfo?.packageName
        } catch (_: Exception) {
            null
        }
    }
}