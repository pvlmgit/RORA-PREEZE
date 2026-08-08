package com.pvlm.rorafreeze.utils

import android.view.View
import androidx.compose.runtime.mutableStateOf
import android.content.Context

/**
 * Centralized haptic feedback utility that can be toggled on/off app-wide.
 */
object HapticUtil {
    // Mutable state to track if in-app haptics are enabled
    val isAppHapticsEnabled = mutableStateOf(true)

    /**
     * Perform a UI interaction haptic feedback (light tick)
     * Only performs if app haptics are enabled
     */
    fun performUIHaptic(view: View) {
        if (!isAppHapticsEnabled.value) return
        view.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
    }

    /**
     * Perform a light tick haptic feedback
     */
    fun performLightHaptic(view: View) {
        if (!isAppHapticsEnabled.value) return
        view.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK)
    }

    /**
     * Perform a medium impact haptic feedback
     */
    fun performMediumHaptic(view: View) {
        if (!isAppHapticsEnabled.value) return
        view.performHapticFeedback(android.view.HapticFeedbackConstants.CONTEXT_CLICK)
    }

    /**
     * Perform a heavy/virtual key haptic feedback
     */
    fun performVirtualKeyHaptic(view: View) {
        if (!isAppHapticsEnabled.value) return
        view.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
    }

    fun performSliderHaptic(view: View) {
        if (!isAppHapticsEnabled.value) return
        view.performHapticFeedback(android.view.HapticFeedbackConstants.SEGMENT_FREQUENT_TICK)
    }

    /**
     * Load app haptic preference from SharedPreferences
     */
    fun loadAppHapticsEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences("freeze_apps_prefs", Context.MODE_PRIVATE)
        return prefs.getBoolean("app_haptics_enabled", true) // Default: enabled
    }

    /**
     * Save app haptic preference to SharedPreferences
     */
    fun saveAppHapticsEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences("freeze_apps_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("app_haptics_enabled", enabled).apply()
        isAppHapticsEnabled.value = enabled
    }

    /**
     * Initialize haptic setting from SharedPreferences
     */
    fun initialize(context: Context) {
        isAppHapticsEnabled.value = loadAppHapticsEnabled(context)
    }
}