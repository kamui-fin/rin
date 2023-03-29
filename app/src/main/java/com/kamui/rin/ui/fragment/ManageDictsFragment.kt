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
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.viewModels
import androidx.lifecycle.*
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.kamui.rin.R
import com.kamui.rin.Settings
import com.kamui.rin.databinding.DictionaryBinding
import com.kamui.rin.databinding.FragmentManageDictsBinding
import com.kamui.rin.db.AppDatabase
import com.kamui.rin.db.model.Dictionary
import com.kamui.rin.dict.DataStatus
import com.kamui.rin.dict.DictionaryManager
import com.kamui.rin.dict.StateData
import com.kamui.rin.dict.StateLiveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ManageDictSettingsState(
    val dictionaries: List<Dictionary> = listOf()
)

class ManageDictSettingsViewModel(private val context: Context) : ViewModel() {
    private val _uiState = MutableStateFlow(ManageDictSettingsState())
    val uiState: StateFlow<ManageDictSettingsState> = _uiState.asStateFlow()
    private val dictDao = AppDatabase.buildDatabase(context).dictionaryDao()

    private val manager = DictionaryManager(context)
    var importUpdates: MutableLiveData<StateData<Unit>> = MutableLiveData()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { currentState ->
                currentState.copy(
                    dictionaries = dictDao.getAllDictionaries().sortedDescending()
                )
            }
        }
    }

    fun delete(dict: Dictionary) {
        viewModelScope.launch(Dispatchers.IO) {
            val updates = StateLiveData<Long>(importUpdates)
            withContext(Dispatchers.Main) {
                updates.observeForever { state ->
                    if (state.status == DataStatus.SUCCESS) {
                        _uiState.update { currentState ->
                            val newDictionaries =
                                if (state.data != null) currentState.dictionaries.filter { it.dictId != state.data }
                                else currentState.dictionaries
                            currentState.copy(
                                dictionaries = newDictionaries
                            )
                        }
                    }
                }
            }
            manager.deleteDictionary(dict, updates)
        }
    }

    fun import(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val updates = StateLiveData<Dictionary>(importUpdates)
            withContext(Dispatchers.Main) {
                updates.observeForever { state ->
                    if (state.status == DataStatus.SUCCESS) {
                        _uiState.update { currentState ->
                            currentState.copy(
                                dictionaries = currentState.dictionaries + listOfNotNull(state.data)
                            )
                        }
                    }
                }
            }
            manager.importYomichanDictionary(uri, updates)
        }
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
    private var dialog: android.app.AlertDialog? = null

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
                viewModel.importUpdates.observe(requireActivity(), Observer { progress ->
                    when (progress.status) {
                        DataStatus.LOADING -> {
                            if (dialog == null || !dialog!!.isShowing) {
                                dialog = android.app.AlertDialog.Builder(requireContext())
                                    .setCancelable(false)
                                    .setView(R.layout.layout_loading_dialog).create()
                            }
                            dialog?.show()
                            dialog!!.findViewById<ProgressBar>(R.id.progressBar2)!!.isIndeterminate = true
                            dialog?.findViewById<TextView>(R.id.statusText)?.text =
                                progress.progressData
                        }
                        DataStatus.SUCCESS, DataStatus.COMPLETE -> {
                            dialog?.hide()
                        }
                        DataStatus.ERROR -> {
                            dialog?.hide()
                            android.app.AlertDialog.Builder(requireContext())
                                .setMessage(
                                    progress.error?.message ?: "An unexpected error has occured."
                                )
                                .setCancelable(false)
                                .setPositiveButton(
                                    "OK"
                                ) { dialog, _ ->
                                    dialog.dismiss()
                                }.setTitle("Error")
                                .create().show()
                        }
                    }
                })
                viewModel.uiState.collect {
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

        private fun createPopupMenu(holder: ViewHolder, dict: Dictionary): PopupMenu {
            val popup = PopupMenu(requireContext(), holder.binding.moreActionsButton)
            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.delete -> {
                        viewModel.delete(dict)
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
