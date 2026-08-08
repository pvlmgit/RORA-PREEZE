package com.pvlm.rorafreeze.utils

import android.content.Context
import android.content.pm.ApplicationInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.util.Log
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.createBitmap
import com.pvlm.rorafreeze.domain.model.NotificationApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object AppUtil {
    private const val TAG = "AppUtil"

    // Cache for app icons to prevent repeated system calls
    private val iconCache = mutableMapOf<String, Bitmap>()

    // Target size for app icons to balance quality and performance
    private const val ICON_SIZE = 64

    /**
     * Get all installed apps (not just launcher apps)
     */
    suspend fun getInstalledApps(context: Context): List<NotificationApp> =
        withContext(Dispatchers.IO) {
            try {
                val pm = context.packageManager

                // Get all installed applications
                val allApps = pm.getInstalledApplications(0)
                    .filter { appInfo ->
                        // Filter out our own app
                        !appInfo.packageName.contains("essentials")
                    }

                val apps = allApps.mapNotNull { appInfo ->
                    try {
                        val app = NotificationApp(
                            packageName = appInfo.packageName,
                            appName = pm.getApplicationLabel(appInfo).toString(),
                            isEnabled = appInfo.enabled,
                            icon = null,
                            isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
                            lastUpdated = System.currentTimeMillis()
                        )
                        app
                    } catch (e: Exception) {
                        Log.w(TAG, "Error loading app ${appInfo.packageName}: ${e.message}")
                        null
                    }
                }

                apps.sortedBy { it.appName.lowercase() }
            } catch (e: Exception) {
                Log.e(TAG, "Error getting installed apps: ${e.message}")
                emptyList()
            }
        }

    /**
     * Helper to load and scale an app icon to a lower resolution for better performance.
     */
    private fun getLowQualityIcon(context: Context, packageName: String): Bitmap {
        // Check cache first
        iconCache[packageName]?.let { return it }

        val drawable = try {
            context.packageManager.getApplicationIcon(packageName)
        } catch (e: Exception) {
            context.packageManager.defaultActivityIcon
        }

        val bitmap = when (drawable) {
            is BitmapDrawable -> {
                val b = drawable.bitmap
                if (b.width > ICON_SIZE || b.height > ICON_SIZE) {
                    Bitmap.createScaledBitmap(b, ICON_SIZE, ICON_SIZE, true)
                } else {
                    b
                }
            }

            else -> {
                val bmp = createBitmap(ICON_SIZE, ICON_SIZE)
                val canvas = Canvas(bmp)
                drawable.setBounds(0, 0, ICON_SIZE, ICON_SIZE)
                drawable.draw(canvas)
                bmp
            }
        }

        // Cache the result
        iconCache[packageName] = bitmap
        return bitmap
    }

    /**
     * Load and cache an app icon on a background thread (used for lazy grid icons).
     */
    suspend fun getAppIconAsync(context: Context, packageName: String): Bitmap =
        withContext(Dispatchers.IO) { getLowQualityIcon(context, packageName) }

    /**
     * A shared placeholder icon to show while a real icon is loading.
     */
    fun defaultAppIcon(context: Context): Bitmap {
        val drawable = context.packageManager.defaultActivityIcon
        return drawableToBitmap(drawable, ICON_SIZE)
    }

    fun drawableToBitmap(drawable: android.graphics.drawable.Drawable, size: Int? = null): Bitmap {
        if (drawable is BitmapDrawable && size == null) {
            return drawable.bitmap
        }

        val width = size ?: drawable.intrinsicWidth.coerceAtLeast(1)
        val height = size ?: drawable.intrinsicHeight.coerceAtLeast(1)

        val bitmap = Bitmap.createBitmap(
            width,
            height,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, width, height)
        drawable.draw(canvas)
        return bitmap
    }
}