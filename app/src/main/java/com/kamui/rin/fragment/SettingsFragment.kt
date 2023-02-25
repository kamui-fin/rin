package com.kamui.rin.fragment

import android.content.Intent
import android.os.Bundle
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import androidx.preference.SwitchPreference
import com.kamui.rin.R
import com.kamui.rin.util.Settings
import com.kamui.rin.util.setupTheme

class SettingsFragment : PreferenceFragmentCompat() {
    private lateinit var settings: Settings

    private val activityResultLauncher: ActivityResultLauncher<Intent> =
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
        settings = Settings(PreferenceManager.getDefaultSharedPreferences(context))
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        findPreference<SwitchPreference>("darkTheme")?.setOnPreferenceChangeListener { _, theme ->
            setupTheme(theme as Boolean)
            true
        }

        findPreference<Preference>("savedWordsPath")?.setOnPreferenceClickListener {
            chooseFile()
            true
        }

        updateSavedWordsPathLabel()
    }

    private fun updateSavedWordsPathLabel() {
        if (PreferenceManager.getDefaultSharedPreferences(context).getString("savedWordsPath", null) != null) {
            findPreference<Preference>("savedWordsPath")?.summary = "Change path"
        }
    }

    private fun chooseFile() {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
            .addCategory(Intent.CATEGORY_OPENABLE)
            .setType("text/plain")
            .putExtra(Intent.EXTRA_TITLE, "words.txt")
            .setFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        activityResultLauncher.launch(intent)
    }
}