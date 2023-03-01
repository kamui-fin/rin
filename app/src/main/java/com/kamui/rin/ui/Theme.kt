package com.kamui.rin.ui

import androidx.appcompat.app.AppCompatDelegate

fun setupTheme(isDarkTheme: Boolean) {
    val theme = if (isDarkTheme) {
        AppCompatDelegate.MODE_NIGHT_YES
    } else {
        AppCompatDelegate.MODE_NIGHT_NO
    }
    AppCompatDelegate.setDefaultNightMode(theme)
}

