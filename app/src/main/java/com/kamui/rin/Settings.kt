package com.kamui.rin

import android.content.SharedPreferences
import android.net.Uri

class Settings(private val sharedPreferences: SharedPreferences) {
    fun disabledDicts(): List<Long> {
        return sharedPreferences.getStringSet("disabledDicts", setOf())!!.toList()
            .map { it.toLong() }
    }

    fun shouldDeconjugate(): Boolean {
        return sharedPreferences.getBoolean("shouldDeconjugate", true)
    }

    fun darkTheme(): Boolean {
        return sharedPreferences.getBoolean("darkTheme", false)
    }

    fun savedWordsPath(): Uri? {
        val uri = sharedPreferences.getString("savedWordsPath", null)
        return if (uri != null) {
            Uri.parse(uri)
        } else {
            null
        }
    }

    fun setSavedWordsPath(path: String) {
        sharedPreferences.edit().putString("savedWordsPath", path).apply()
    }
}