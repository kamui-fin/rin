package com.kamui.rin.fragment

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import com.kamui.rin.R

class SettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
    }
}