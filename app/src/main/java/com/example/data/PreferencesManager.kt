package com.example.data

import android.content.Context
import android.content.SharedPreferences

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("money_manager_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_APP_THEME = "key_app_theme"
        private const val KEY_IS_DARK_MODE = "key_is_dark_mode"
        
        private const val KEY_WIDGET_BACKGROUND_STYLE = "key_widget_bg_style"
        private const val KEY_WIDGET_OPACITY = "key_widget_opacity"
        private const val KEY_WIDGET_USE_THEME_COLOR = "key_widget_use_theme"

        // Tema aplikasi yang didukung
        const val THEME_TEAL = "TEAL"
        const val THEME_ORANGE = "ORANGE"
        const val THEME_BLUE = "BLUE"
        const val THEME_PURPLE = "PURPLE"
        const val THEME_GREEN = "GREEN"

        // Gaya latar belakang widget yang didukung
        const val WIDGET_STYLE_GLASS = "GLASS" // Glassmorphism translucent
        const val WIDGET_STYLE_THEME_GRADIENT = "GRADIENT" // Gradasi warna dari tema terpilih
        const val WIDGET_STYLE_SOLID_DARK = "SOLID_DARK" // Abu-abu gelap obsidian
        const val WIDGET_STYLE_SOLID_LIGHT = "SOLID_LIGHT" // Putih bersih modern
        const val WIDGET_STYLE_CUSTOM_IMAGE = "CUSTOM_IMAGE" // Gambar kustom dari galeri
    }

    var appTheme: String
        get() = prefs.getString(KEY_APP_THEME, THEME_TEAL) ?: THEME_TEAL
        set(value) = prefs.edit().putString(KEY_APP_THEME, value).apply()

    var isDarkMode: Boolean
        get() = prefs.getBoolean(KEY_IS_DARK_MODE, false)
        set(value) = prefs.edit().putBoolean(KEY_IS_DARK_MODE, value).apply()

    var widgetBackgroundStyle: String
        get() = prefs.getString(KEY_WIDGET_BACKGROUND_STYLE, WIDGET_STYLE_GLASS) ?: WIDGET_STYLE_GLASS
        set(value) = prefs.edit().putString(KEY_WIDGET_BACKGROUND_STYLE, value).apply()

    var widgetOpacity: Float
        get() = prefs.getFloat(KEY_WIDGET_OPACITY, 0.85f)
        set(value) = prefs.edit().putFloat(KEY_WIDGET_OPACITY, value).apply()

    var widgetUseThemeColor: Boolean
        get() = prefs.getBoolean(KEY_WIDGET_USE_THEME_COLOR, true)
        set(value) = prefs.edit().putBoolean(KEY_WIDGET_USE_THEME_COLOR, value).apply()
}
