package com.kamui.rin.ui.fragment

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.PopupMenu
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.*
import androidx.preference.*
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.kamui.rin.R
import com.kamui.rin.databinding.DictionaryBinding
import com.kamui.rin.databinding.FragmentManageDictsBinding
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
            val dicts =
                AppDatabase.buildDatabase(context).dictionaryDao().getAllDictionaries()
            _uiState.update { currentState ->
                currentState.copy(
                    dictionaries = dicts.sortedDescending()
                )
            }
        }
    }

    fun delete(dict: Dictionary) {
        viewModelScope.launch(Dispatchers.IO) {
            val manager = DictionaryManager(context)
            manager.deleteDictionary(dict) { status, data ->
                _uiState.update { currentState ->
                    val newDictionaries =
                        if (data != null) currentState.dictionaries.filter { it.dictId != data }
                        else currentState.dictionaries
                    currentState.copy(
                        importStatus = status,
                        dictionaries = newDictionaries
                    )
                }
            }
        }
    }

    fun setOrder(dict: Dictionary, order: Int) {
        val newDictionary = dict.copy(order = order)
        viewModelScope.launch(Dispatchers.IO) {
            AppDatabase.buildDatabase(context).dictionaryDao().updateDictionary(newDictionary)
            _uiState.update { currentState ->
                currentState.copy(
                    dictionaries = currentState.dictionaries.map {
                        if (it.dictId == dict.dictId) newDictionary
                        else it
                    }.sortedDescending()
                )
            }
        }
    }

    fun import(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val manager = DictionaryManager(context)
            manager.importYomichanDictionary(uri) { status, data ->
                _uiState.update { currentState ->
                    currentState.copy(
                        importStatus = status,
                        dictionaries = currentState.dictionaries + listOfNotNull(data)
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

class ManageDictsFragment : Fragment() {
    private var _binding: FragmentManageDictsBinding? = null
    private val binding get() = _binding!!
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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect {

                    if (it.importStatus != null) {
                        if (dialog == null || !dialog!!.isShowing) {
                            dialog = AlertDialog.Builder(requireContext()).setCancelable(false)
                                .setView(R.layout.layout_loading_dialog).create()
                            dialog!!.show()
                        } else if (it.importStatus == "Done") {
                            dialog!!.hide()
                        }
                        dialog!!.findViewById<TextView>(R.id.statusText)?.text = it.importStatus
                    }

                    binding.dictRecyclerList.adapter = Adapter()

                }
            }
        }

        _binding = FragmentManageDictsBinding.inflate(inflater, container, false)
        binding.dictRecyclerList.addItemDecoration(
            DividerItemDecoration(
                context,
                DividerItemDecoration.VERTICAL
            )
        )
        binding.dictRecyclerList.layoutManager = LinearLayoutManager(context)
        binding.importButton.setOnClickListener { addDictionary() }
        return binding.root
    }

    private fun addDictionary() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            .addCategory(Intent.CATEGORY_OPENABLE)
            .setType("application/zip")
            .setFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        addDictionaryActivityResultLauncher.launch(intent)
    }

    inner class ViewHolder(val binding: DictionaryBinding) :
        RecyclerView.ViewHolder(binding.root)

    inner class Adapter : RecyclerView.Adapter<ManageDictsFragment.ViewHolder>() {
        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): ManageDictsFragment.ViewHolder {
            val binding =
                DictionaryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(binding)
        }

        override fun getItemCount(): Int {
            return viewModel.uiState.value.dictionaries.size
        }

        override fun onBindViewHolder(holder: ManageDictsFragment.ViewHolder, position: Int) {
            val settings = PreferenceManager.getDefaultSharedPreferences(requireContext())
            val dict = viewModel.uiState.value.dictionaries[position]
            val idString = dict.dictId.toString()
            holder.binding.dictionaryName.text = dict.name
            holder.binding.toggleActive.isChecked =
                !settings.getStringSet("disabledDicts", setOf())!!.contains(idString)
            holder.binding.toggleActive.setOnCheckedChangeListener { _, isChecked ->
                val origSet = settings.getStringSet("disabledDicts", setOf())!!
                val newSet = mutableSetOf<String>()
                newSet.addAll(origSet)
                if (isChecked) {
                    newSet.remove(idString)
                } else {
                    newSet.add(idString)
                }
                settings.edit().putStringSet("disabledDicts", newSet).apply()
            }
            val popup = PopupMenu(requireContext(), holder.binding.moreActionsButton)
            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.delete -> {
                        viewModel.delete(dict)
                    }
                    R.id.setOrder -> {
                        AlertDialog.Builder(requireContext()).apply {
                            setTitle("Dictionary Priority")

                            val input = EditText(requireContext())
                            input.inputType = InputType.TYPE_CLASS_NUMBER
                            input.setText(dict.order.toString())
                            setView(input)

                            setPositiveButton("OK") { _, _ ->
                                val order = input.text.toString().toInt()
                                viewModel.setOrder(dict, order)
                            }
                            setNegativeButton("Cancel") { _, _ -> }
                        }.show()
                    }
                }
                false
            }
            val inflater = popup.menuInflater
            inflater.inflate(R.menu.dictionary_manage_actions, popup.menu)
            holder.binding.moreActionsButton.setOnClickListener {
                popup.show()
            }
        }
    }
}
