package com.kamui.rin.ui.fragment

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
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
import androidx.preference.*
import com.kamui.rin.R
import com.kamui.rin.dict.DictionaryManager
import com.kamui.rin.db.AppDatabase
import com.kamui.rin.db.model.Dictionary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ManageDictSettingsState(
    val importStatus: String? = null,
    val dictionaries: List<Dictionary> = listOf()
)

class ManageDictSettingsViewModel(private val context: Context) : ViewModel() {
    private val _uiState = MutableStateFlow(ManageDictSettingsState())
    val uiState: StateFlow<ManageDictSettingsState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val dicts = AppDatabase.buildDatabase(context).dictionaryDao().getAllDictionaries()
            _uiState.update { currentState ->
                currentState.copy(
                    dictionaries = dicts
                )
            }
        }
    }


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

class ManageDictSettingsViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ManageDictSettingsViewModel(context) as T
    }
}

class ManageDictSettingsFragment : PreferenceFragmentCompat() {
    private var dialog: AlertDialog? = null

    private val viewModel: ManageDictSettingsViewModel by viewModels {
        ManageDictSettingsViewModelFactory(requireContext())
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

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.manage_dicts, rootKey)
        findPreference<Preference>("addDictionary")?.setOnPreferenceClickListener {
            addDictionary()
            true
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect {
                    val settings = PreferenceManager.getDefaultSharedPreferences(requireContext())

                    if (it.importStatus != null) {
                        if (dialog == null) {
                            dialog = AlertDialog.Builder(requireContext()).setCancelable(false).setView(R.layout.layout_loading_dialog).create()
                            dialog!!.show()
                        } else if (it.importStatus == "Done") {
                            dialog!!.hide()
                        }
                        dialog!!.findViewById<TextView>(R.id.statusText)?.text = it.importStatus
                    }

                    val dictPrefs = it.dictionaries.map {
                        val pref = SwitchPreference(requireContext())
                        val dictId = it.dictId.toString()
                        pref.setOnPreferenceChangeListener { _, newValue ->
                            val dictEnabled = newValue as Boolean
                            val origSet = settings.getStringSet("disabledDicts", setOf())!!
                            val newSet = mutableSetOf<String>()
                            newSet.addAll(origSet)
                            if (dictEnabled) {
                                newSet.remove(dictId)
                            } else {
                                newSet.add(dictId)
                            }
                            settings.edit().putStringSet("disabledDicts", newSet).apply()
                            true
                        }
                        pref.isChecked = !settings.getStringSet("disabledDicts", setOf())!!.contains(dictId)
                        pref.title = it.name
                        pref
                    }

                    val prefCategoryDicts = findPreference<PreferenceScreen>("root")!!
                    val addPreference = prefCategoryDicts.findPreference<Preference>("addDictionary")!!
                    prefCategoryDicts.removeAll()
                    dictPrefs.forEach { prefCategoryDicts.addPreference(it)}
                    prefCategoryDicts.addPreference(addPreference)
                }
            }
        }
    }

    private fun addDictionary() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            .addCategory(Intent.CATEGORY_OPENABLE)
            .setType("application/zip")
            .setFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        addDictionaryActivityResultLauncher.launch(intent)
    }
}
