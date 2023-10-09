package com.kamui.rin.ui.fragment

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkRequest
import androidx.work.workDataOf
import com.kamui.rin.R
import com.kamui.rin.Settings
import com.kamui.rin.databinding.DictionaryBinding
import com.kamui.rin.databinding.FragmentManageDictsBinding
import com.kamui.rin.db.AppDatabase
import com.kamui.rin.db.model.Dictionary
import com.kamui.rin.dict.worker.DeleteDictionaryWorker
import com.kamui.rin.dict.worker.ImportDictionaryWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ManageDictSettingsState(
    val dictionaries: List<Dictionary> = listOf()
)

class ManageDictSettingsViewModel(private val context: Context) : ViewModel() {
    private val _uiState = MutableStateFlow(ManageDictSettingsState())
    val uiState: StateFlow<ManageDictSettingsState> = _uiState.asStateFlow()
    private val dictDao = AppDatabase.buildDatabase(context).dictionaryDao()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { currentState ->
                currentState.copy(
                    dictionaries = dictDao.getAllDictionaries().sortedDescending()
                )
            }
        }
    }

    fun delete(dict: Dictionary, lifecycleOwner: LifecycleOwner) {
        val deleteWork: WorkRequest =
            OneTimeWorkRequestBuilder<DeleteDictionaryWorker>().setInputData(
                workDataOf(
                    "DICT_ID" to dict.dictId
                )
            ).build()
        WorkManager.getInstance(context).enqueue(deleteWork)

        WorkManager.getInstance(context).getWorkInfoByIdLiveData(deleteWork.id)
            .observe(lifecycleOwner) { result: WorkInfo ->
                if (result.state == WorkInfo.State.SUCCEEDED) {
                    viewModelScope.launch(Dispatchers.IO) {
                        _uiState.update { currentState ->
                            val newDictionaries =
                                currentState.dictionaries.filter { it.dictId != dict.dictId }
                            currentState.copy(
                                dictionaries = newDictionaries
                            )
                        }
                    }
                }
            }
    }

    fun import(uri: Uri, lifecycleOwner: LifecycleOwner) {
        val importWork: WorkRequest =
            OneTimeWorkRequestBuilder<ImportDictionaryWorker>().setInputData(
                workDataOf(
                    "URI" to uri.toString()
                )
            ).build()
        WorkManager.getInstance(context).enqueue(importWork)

        WorkManager.getInstance(context).getWorkInfoByIdLiveData(importWork.id)
            .observe(lifecycleOwner, Observer { result: WorkInfo ->
                if (result.state == WorkInfo.State.SUCCEEDED) {
                    val data = result.outputData
                    val dictId = data.getLong("DICT_ID", -1)
                    val dictTitle = data.getString("DICT_TITLE") ?: "Untitled Dictionary"
                    if (dictId.toInt() != -1) {
                        val dict = Dictionary(dictId, dictTitle)
                        viewModelScope.launch(Dispatchers.IO) {
                            _uiState.update { currentState ->
                                currentState.copy(
                                    dictionaries = currentState.dictionaries + listOfNotNull(
                                        dict
                                    )
                                )
                            }
                        }
                    }
                }
            })
    }

    fun setOrder(dict: Dictionary, order: Int) {
        val newDictionary = dict.copy(order = order)
        viewModelScope.launch(Dispatchers.IO) {
            dictDao.updateDictionary(newDictionary)
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

}

class ManageDictSettingsViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ManageDictSettingsViewModel(context) as T
    }
}

class ManageDictsFragment : androidx.fragment.app.Fragment() {
    private var _binding: FragmentManageDictsBinding? = null
    private val binding get() = _binding!!

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
                    Toast.makeText(activity, "Starting dictionary import", Toast.LENGTH_LONG).show()
                    viewModel.import(Uri.parse(uri), viewLifecycleOwner)
                }
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect {
                    binding.dictRecyclerList.adapter = Adapter()
                }
            }
        }

        _binding = FragmentManageDictsBinding.inflate(inflater, container, false)
        binding.dictRecyclerList.addItemDecoration(
            DividerItemDecoration(
                context, DividerItemDecoration.VERTICAL
            )
        )
        binding.dictRecyclerList.layoutManager = LinearLayoutManager(context)
        binding.importButton.setOnClickListener { addDictionary() }
        return binding.root
    }

    private fun addDictionary() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE)
            .setType("application/zip")
            .setFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        addDictionaryActivityResultLauncher.launch(intent)
    }

    inner class ViewHolder(val binding: DictionaryBinding) : RecyclerView.ViewHolder(binding.root)

    inner class Adapter : RecyclerView.Adapter<ManageDictsFragment.ViewHolder>() {
        override fun onCreateViewHolder(
            parent: ViewGroup, viewType: Int
        ): ManageDictsFragment.ViewHolder {
            val binding =
                DictionaryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(binding)
        }

        override fun getItemCount(): Int {
            return viewModel.uiState.value.dictionaries.size
        }

        private fun createPopupMenu(holder: ViewHolder, dict: Dictionary): PopupMenu {
            val popup = PopupMenu(requireContext(), holder.binding.moreActionsButton)
            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.delete -> {
                        viewModel.delete(dict, viewLifecycleOwner)
                        Toast.makeText(activity, "Deleting ${dict.name}", Toast.LENGTH_LONG).show()
                    }

                    R.id.setOrder -> {
                        android.app.AlertDialog.Builder(requireContext()).apply {
                            val view = layoutInflater.inflate(R.layout.dict_priority_dialog, null)
                            setView(view)
                            setTitle("Dictionary Priority")
                            val input = view.findViewById<EditText>(R.id.dictOrder)
                            input.setText(dict.order.toString())
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
            return popup
        }

        override fun onBindViewHolder(holder: ManageDictsFragment.ViewHolder, position: Int) {
            val settings = Settings(PreferenceManager.getDefaultSharedPreferences(requireContext()))
            val dict = viewModel.uiState.value.dictionaries[position]
            holder.binding.dictionaryName.text = dict.name
            holder.binding.order.text = dict.order.toString()
            holder.binding.toggleActive.isChecked = settings.isDictActive(dict.dictId)

            holder.binding.toggleActive.setOnCheckedChangeListener { _, isChecked ->
                val idString = dict.dictId.toString()
                val origSet = settings.disabledDictSet()
                val newSet = mutableSetOf<String>()
                newSet.addAll(origSet)
                if (isChecked) {
                    newSet.remove(idString)
                } else {
                    newSet.add(idString)
                }
                settings.updateDisabledDicts(newSet)
            }

            val popup = createPopupMenu(holder, dict)

            val inflater = popup.menuInflater
            inflater.inflate(R.menu.dictionary_manage_actions, popup.menu)
            holder.binding.moreActionsButton.setOnClickListener {
                popup.show()
            }

        }
    }
}
