package com.example.magneticsecretactions.settings

import android.content.Context

enum class ActionChoice { NOTIFICATION, FLASHLIGHT, OPEN_URL, OPEN_APP, MEDIA_PLAY_PAUSE }

class AppSettings(context: Context) {
    private val preferences = context.getSharedPreferences("magnetic_settings", Context.MODE_PRIVATE)

    var action: ActionChoice
        get() = runCatching {
            ActionChoice.valueOf(preferences.getString(KEY_ACTION, ActionChoice.NOTIFICATION.name)!!)
        }.getOrDefault(ActionChoice.NOTIFICATION)
        set(value) { preferences.edit().putString(KEY_ACTION, value.name).apply() }

    fun actionFor(pattern: String): ActionChoice = runCatching {
        ActionChoice.valueOf(preferences.getString("$KEY_PATTERN$pattern", action.name)!!)
    }.getOrDefault(action)

    fun setActionFor(pattern: String, value: ActionChoice) {
        preferences.edit().putString("$KEY_PATTERN$pattern", value.name).apply()
    }

    var url: String
        get() = preferences.getString(KEY_URL, "https://example.com") ?: "https://example.com"
        set(value) { preferences.edit().putString(KEY_URL, value).apply() }

    var appPackage: String
        get() = preferences.getString(KEY_APP_PACKAGE, "") ?: ""
        set(value) { preferences.edit().putString(KEY_APP_PACKAGE, value).apply() }

    var baseline: Float?
        get() = if (preferences.contains(KEY_BASELINE)) preferences.getFloat(KEY_BASELINE, 0f) else null
        set(value) {
            preferences.edit().apply {
                if (value == null) remove(KEY_BASELINE) else putFloat(KEY_BASELINE, value)
            }.apply()
        }

    var threshold: Float
        get() = preferences.getFloat(KEY_THRESHOLD, 10f)
        set(value) { preferences.edit().putFloat(KEY_THRESHOLD, value).apply() }

    var sensitivity: Float
        get() = preferences.getFloat(KEY_SENSITIVITY, 1f)
        set(value) { preferences.edit().putFloat(KEY_SENSITIVITY, value.coerceIn(0.25f, 4f)).apply() }

    var patternTimeoutMs: Long
        get() = preferences.getLong(KEY_PATTERN_TIMEOUT, 700L)
        set(value) { preferences.edit().putLong(KEY_PATTERN_TIMEOUT, value.coerceIn(400L, 1500L)).apply() }

    companion object {
        private const val KEY_ACTION = "action"
        private const val KEY_BASELINE = "baseline"
        private const val KEY_THRESHOLD = "threshold"
        private const val KEY_PATTERN = "pattern_"
        private const val KEY_URL = "url"
        private const val KEY_APP_PACKAGE = "app_package"
        private const val KEY_SENSITIVITY = "sensitivity"
        private const val KEY_PATTERN_TIMEOUT = "pattern_timeout"
    }
}
