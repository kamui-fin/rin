package com.kamui.rin.util

data class SettingsData(
    val disabledDicts: List<String>,
    val bilingualFirst: Boolean,
    val shouldDeconj: Boolean,
)