package com.kamui.rin.util

import android.content.SharedPreferences

class Settings(private val sharedPreferences: SharedPreferences) {
    val disabledDicts: List<String>
        get() {
            val dictMap = mapOf(
                "jmdictEnable" to "JMdict (English)",
                "kenkyuuEnable" to "研究社　新和英大辞典　第５版",
                "shinmeiEnable" to "新明解国語辞典 第五版",
                "daijirinEnable" to "三省堂　スーパー大辞林",
                "meikyoEnable" to "明鏡国語辞典"
            )
            val disabledDicts: MutableList<String> = ArrayList()
            for ((key, name) in dictMap) {
                val isEnabled = sharedPreferences.getBoolean(key, true)
                if (!isEnabled)
                    disabledDicts.add(name)
            }
            return disabledDicts
        }

    val bilingualFirst: Boolean
        get() {
            return sharedPreferences.getBoolean("showBilingualFirst", false)
        }

    val shouldDeconjugate: Boolean
        get() {
            return sharedPreferences.getBoolean("shouldDeconjugate", true)
        }

    val darkTheme: Boolean
        get() {
            return sharedPreferences.getBoolean("darkTheme", false)
        }
}