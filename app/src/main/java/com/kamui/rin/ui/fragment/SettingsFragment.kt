package com.kamui.rin.ui.fragment

import android.content.Context
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
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import androidx.preference.SwitchPreference
import com.kamui.rin.R
import com.kamui.rin.dict.DictionaryManager
import com.kamui.rin.Settings
import com.kamui.rin.ui.setupTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.w3c.dom.Text

data class SettingsState(
    val importStatus: String? = null
)

class SettingsViewModel(private val context: Context) : ViewModel() {
    private val _uiState = MutableStateFlow(SettingsState())
    val uiState: StateFlow<SettingsState> = _uiState.asStateFlow()

    fun import(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val manager = DictionaryManager()
            manager.importYomichan(uri, context) {
                _uiState.update { currentState ->
                    currentState.copy(
                        importStatus = it
                    )
                }
            }
        }
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

    val viewModel: SettingsViewModel by viewModels {
        SettingsViewModelFactory(requireContext())
    }

    private val addDictionaryActivityResultLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {
            if (it.resultCode == AppCompatActivity.RESULT_OK) {
                val uri = it.data?.dataString
                if (uri != null) {
                    viewModel.import(Uri.parse(uri))
                }
            }
        }

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

        findPreference<Preference>("addDictionary")?.setOnPreferenceClickListener {
            addDictionary()
            true
        }
        updateSavedWordsPathLabel()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect {
                    if (it.importStatus != null) {
                        if (dialog == null) {
                            dialog = AlertDialog.Builder(requireContext()).setCancelable(false).setView(R.layout.layout_loading_dialog).create()
                            dialog!!.show()
                        }
                        dialog!!.findViewById<TextView>(R.id.statusText)?.text = it.importStatus
                    }
                }
            }
        }
    }

    private fun updateSavedWordsPathLabel() {
        if (PreferenceManager.getDefaultSharedPreferences(requireContext())
                .getString("savedWordsPath", null) != null
        ) {
            findPreference<Preference>("savedWordsPath")?.summary = "Change path"
        }
    }

    private fun addDictionary() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            .addCategory(Intent.CATEGORY_OPENABLE)
            .setType("application/zip")
            .setFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        addDictionaryActivityResultLauncher.launch(intent)
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