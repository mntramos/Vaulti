package com.vaulti.app.ui.theme

import android.content.Context
import android.content.SharedPreferences

enum class ThemeMode {
    SYSTEM, LIGHT, DARK
}

class ThemePreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("vaulti_prefs", Context.MODE_PRIVATE)

    var themeMode: ThemeMode
        get() = ThemeMode.entries[prefs.getInt("theme_mode", 0)]
        set(value) = prefs.edit().putInt("theme_mode", value.ordinal).apply()
}
