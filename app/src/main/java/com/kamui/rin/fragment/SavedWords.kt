package com.kamui.rin.fragment

import android.app.AlertDialog
import android.content.ContentResolver
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.*
import androidx.navigation.findNavController
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.kamui.rin.R
import com.kamui.rin.databinding.FragmentSavedWordsBinding
import com.kamui.rin.databinding.SavedWordsItemBinding
import com.kamui.rin.db.AppDatabase
import com.kamui.rin.db.SavedWord
import com.kamui.rin.util.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SavedWordsState(
    val words: List<SavedWord> = listOf()
)

class SavedWordsViewModel(private val database: AppDatabase) : ViewModel() {
    private val _uiState = MutableStateFlow(SavedWordsState())
    val uiState: StateFlow<SavedWordsState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            database.savedDao().getAllSaved().collect {
                _uiState.update { currentState ->
                    currentState.copy(
                        words = it
                    )
                }
            }
        }
    }

    fun deleteItem(index: Int) {
        _uiState.update { state ->
            viewModelScope.launch(Dispatchers.IO) {
                database.savedDao().deleteWord(state.words[index])
            }
            state.copy(
                words = state.words.subList(0, index) + state.words.subList(
                    index + 1,
                    state.words.size
                )
            )
        }
    }

    fun deleteAll() {
        _uiState.update { state ->
            viewModelScope.launch(Dispatchers.IO) {
                database.savedDao().deleteAllWords()
            }
            state.copy(words = listOf())
        }
    }

    fun saveWordsToFile(fileUri: Uri, resolver: ContentResolver?) {
        viewModelScope.launch(Dispatchers.IO) {
            val wordListString = uiState.value.words.joinToString("\n")
            resolver?.takePersistableUriPermission(fileUri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            val outputStream = resolver?.openOutputStream(fileUri, "wt")
            outputStream?.write(wordListString.toByteArray())
            outputStream?.close()
        }
    }
}

class SavedWordsViewModelFactory(private val database: AppDatabase) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return SavedWordsViewModel(database) as T
    }
}


class SavedWords : Fragment() {
    private var _binding: FragmentSavedWordsBinding? = null
    private val binding get() = _binding!!

    val wordsViewModel: SavedWordsViewModel by viewModels {
        SavedWordsViewModelFactory(AppDatabase.buildDatabase(binding.root.context))
    }

    private lateinit var settings: Settings

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settings = Settings(PreferenceManager.getDefaultSharedPreferences(requireContext()))
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSavedWordsBinding.inflate(inflater, container, false)

        binding.wordListRecycler.addItemDecoration(
            DividerItemDecoration(
                context,
                DividerItemDecoration.VERTICAL
            )
        )
        binding.wordListRecycler.layoutManager = LinearLayoutManager(context)
        val itemTouchHelper = ItemTouchHelper(ItemTouchCallback())
        itemTouchHelper.attachToRecyclerView(binding.wordListRecycler)

        binding.saveButton.setOnClickListener { saveToFile() }

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                wordsViewModel.uiState.collect {
                    binding.wordListRecycler.adapter = Adapter()
                }
            }
        }

        return binding.root
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.saved_words_action_bar, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.clearListButton -> {
                wordsViewModel.deleteAll()
                return true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }


    private fun saveToFile() {
        val fileUri = settings.savedWordsPath
        if (fileUri != null) {
            wordsViewModel.saveWordsToFile(fileUri, activity?.contentResolver)
        } else {
            val alertDialog = AlertDialog.Builder(activity)
                .setMessage("Specify a file in settings")
                .setPositiveButton("OK") { dialog: DialogInterface, _: Int -> dialog.dismiss() }
                .create()
            alertDialog.show()
        }
    }

    inner class ItemTouchCallback : ItemTouchHelper.Callback() {
        override fun getMovementFlags(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder
        ): Int {
            return makeMovementFlags(0, ItemTouchHelper.RIGHT)
        }

        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder
        ): Boolean {
            return false
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            val pos = viewHolder.absoluteAdapterPosition
            wordsViewModel.deleteItem(pos)
        }
    }

    inner class ViewHolder(val binding: SavedWordsItemBinding) :
        RecyclerView.ViewHolder(binding.root)

    inner class Adapter : RecyclerView.Adapter<SavedWords.ViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SavedWords.ViewHolder {
            val binding =
                SavedWordsItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(binding)
        }

        override fun getItemCount(): Int {
            return wordsViewModel.uiState.value.words.size
        }

        override fun onBindViewHolder(holder: SavedWords.ViewHolder, position: Int) {
            val kanji = wordsViewModel.uiState.value.words[position].kanji
            holder.binding.word.text = kanji
            holder.binding.root.setOnClickListener {
                val searchLink = Uri.parse("rin://search/${kanji}")
                it.findNavController().navigate(searchLink)
            }
        }
    }
}