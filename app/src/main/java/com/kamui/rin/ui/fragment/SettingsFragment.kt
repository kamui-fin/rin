package com.kamui.rin.ui.fragment

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.viewModels
import androidx.lifecycle.*
import androidx.navigation.Navigation
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import androidx.preference.SwitchPreference
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkRequest
import androidx.work.workDataOf
import com.kamui.rin.R
import com.kamui.rin.Settings
import com.kamui.rin.db.model.Dictionary
import com.kamui.rin.dict.worker.ImportDictionaryWorker
import com.kamui.rin.dict.worker.ImportFrequencyWorker
import com.kamui.rin.dict.worker.ImportPitchWorker
import com.kamui.rin.ui.setupTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsViewModel(private val context: Context) : ViewModel() {
    fun importFrequencyList(uri: Uri, lifecycleOwner: LifecycleOwner) {
        val importWork: WorkRequest =
            OneTimeWorkRequestBuilder<ImportFrequencyWorker>().setInputData(
                workDataOf(
                    "URI" to uri.toString()
                )
            ).build()
        WorkManager.getInstance(context).enqueue(importWork)
    }

    fun importPitchAccents(uri: Uri, lifecycleOwner: LifecycleOwner) {
        val importWork: WorkRequest =
            OneTimeWorkRequestBuilder<ImportPitchWorker>().setInputData(
                workDataOf(
                    "URI" to uri.toString()
                )
            ).build()
        WorkManager.getInstance(context).enqueue(importWork)
    }
}

class SettingsViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return SettingsViewModel(context) as T
    }
}

class SettingsFragment : PreferenceFragmentCompat() {
    private lateinit var settings: Settings
    private var dialog: AlertDialog? = null

    private lateinit var pickFreq: ActivityResultLauncher<Intent>
    private lateinit var pickPitch: ActivityResultLauncher<Intent>
    private lateinit var pickSavedWords: ActivityResultLauncher<Intent>

    private val viewModel: SettingsViewModel by viewModels {
        SettingsViewModelFactory(requireContext())
    }

    private fun pickFile(callback: (Uri) -> Unit): ActivityResultLauncher<Intent> {
        return registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {
            if (it.resultCode == AppCompatActivity.RESULT_OK) {
                val uri = Uri.parse(it.data?.dataString)
                if (uri != null) {
                    callback(uri)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settings = Settings(PreferenceManager.getDefaultSharedPreferences(requireContext()))
        pickFreq = pickFile { viewModel.importFrequencyList(it, viewLifecycleOwner) }
        pickPitch = pickFile { viewModel.importPitchAccents(it, viewLifecycleOwner) }
        pickSavedWords = pickFile {
            settings.setSavedWordsPath(it.path!!)
            updateSavedWordsPathLabel()
        }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        findPreference<SwitchPreference>("darkTheme")?.setOnPreferenceChangeListener { _, theme ->
            setupTheme(theme as Boolean)
            true
        }

        findPreference<Preference>("manageDicts")?.setOnPreferenceClickListener {
            Navigation.findNavController(requireView()).navigate(R.id.manage_dict_settings_fragment)
            true
        }

        findPreference<Preference>("importFreq")?.setOnPreferenceClickListener {
            pickFreq.launch(chooseYomichanFile())
            true
        }

        findPreference<Preference>("importPitch")?.setOnPreferenceClickListener {
            pickPitch.launch(chooseYomichanFile())
            true
        }

        findPreference<Preference>("savedWordsPath")?.setOnPreferenceClickListener {
            pickSavedWords.launch(chooseSavedWordsFile())
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

    private fun chooseSavedWordsFile(): Intent {
        return Intent(Intent.ACTION_CREATE_DOCUMENT)
            .addCategory(Intent.CATEGORY_OPENABLE)
            .setType("text/plain")
            .putExtra(Intent.EXTRA_TITLE, "words.txt")
            .setFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
    }

    private fun chooseYomichanFile(): Intent {
        return Intent(Intent.ACTION_OPEN_DOCUMENT)
            .addCategory(Intent.CATEGORY_OPENABLE)
            .setType("application/zip")
            .setFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
}