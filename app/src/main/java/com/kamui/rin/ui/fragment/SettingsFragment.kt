package com.kamui.rin.ui.fragment

import android.content.Intent
import android.os.Bundle
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.Navigation
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import androidx.preference.SwitchPreference
import com.kamui.rin.R
import com.kamui.rin.Settings
import com.kamui.rin.ui.setupTheme

class SettingsFragment : PreferenceFragmentCompat() {
    private lateinit var settings: Settings

    private val savedWordsActivityResultLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {
            if (it.resultCode == AppCompatActivity.RESULT_OK) {
                val uri = it.data?.dataString
                if (uri != null) {
                    settings.setSavedWordsPath(uri)
                    updateSavedWordsPathLabel()
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settings = Settings(PreferenceManager.getDefaultSharedPreferences(requireContext()))
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        findPreference<SwitchPreference>("darkTheme")?.setOnPreferenceChangeListener { _, theme ->
            setupTheme(theme as Boolean)
            true
        }

        findPreference<Preference>("savedWordsPath")?.setOnPreferenceClickListener {
            chooseSavedWordsFile()
            true
        }

        findPreference<Preference>("manageDicts")?.setOnPreferenceClickListener {
            Navigation.findNavController(requireView()).navigate(R.id.manage_dict_settings_fragment)
            true
        }

        updateSavedWordsPathLabel()
    }

    private fun updateSavedWordsPathLabel() {
        if (PreferenceManager.getDefaultSharedPreferences(requireContext())
                .getString("savedWordsPath", null) != null
        ) {
            findPreference<Preference>("savedWordsPath")?.summary = "Change path"
        }
    }

    private fun chooseSavedWordsFile() {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
            .addCategory(Intent.CATEGORY_OPENABLE)
            .setType("text/plain")
            .putExtra(Intent.EXTRA_TITLE, "words.txt")
            .setFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        savedWordsActivityResultLauncher.launch(intent)
    }
}