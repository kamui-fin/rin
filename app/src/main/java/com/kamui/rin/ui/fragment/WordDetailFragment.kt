package com.kamui.rin.ui.fragment

import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.*
import androidx.navigation.fragment.navArgs
import com.google.android.material.chip.Chip
import com.kamui.rin.R
import com.kamui.rin.databinding.FragmentWordDetailBinding
import com.kamui.rin.db.AppDatabase
import com.kamui.rin.db.model.DictEntry
import com.kamui.rin.db.model.SavedWord
import com.kamui.rin.db.model.Tag
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.DecimalFormat

data class WordDetailState(
    val entry: DictEntry? = null,
    val frequency: Long? = null,
    val pitch: String? = null,
    val tags: List<Tag> = listOf(),
    val saved: Boolean = false
)

class WordDetailViewModel(private val database: AppDatabase, entryId: Long) : ViewModel() {
    private val _uiState = MutableStateFlow(WordDetailState())
    val uiState: StateFlow<WordDetailState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { currentState ->
                val entry = database.dictEntryDao().searchEntryById(entryId)
                val tags = database.dictEntryDao().getTagsForEntry(entryId)
                val freq = database.frequencyDao().getFrequencyForWord(entry.kanji)
                val pitch = database.pitchAccentDao().getPitchForWord(entry.kanji)
                currentState.copy(
                    entry = entry,
                    tags = tags,
                    saved = database.savedDao().existsWord(entry.kanji),
                    frequency = freq,
                    pitch = pitch
                )
            }
        }
    }

    fun removeWord(kanji: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { currentState ->
                database.savedDao().deleteWordByKanji(kanji)
                currentState.copy(saved = false)
            }
        }
    }

    fun saveWord(kanji: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { currentState ->
                database.savedDao().insertWord(SavedWord(kanji = kanji))
                currentState.copy(saved = true)
            }
        }
    }
}

class WordDetailViewModelFactory(private val database: AppDatabase, private val wordId: Long): ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return WordDetailViewModel(database, wordId) as T
    }
}

class WordDetailFragment : Fragment() {
    private var _binding: FragmentWordDetailBinding? = null
    private val binding get() = _binding!!
    private val args: WordDetailFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWordDetailBinding.inflate(inflater, container, false)

        val viewModel: WordDetailViewModel by viewModels {
            WordDetailViewModelFactory(AppDatabase.buildDatabase(binding.root.context), args.wordId)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect {
                    if (it.entry != null) {
                        // set title bar to word
                        val entry = it.entry
                        val tags = it.tags
                        (requireActivity() as AppCompatActivity).supportActionBar?.title = entry.kanji

                        // fill in UI elements
                        binding.secondaryTextCard.text = entry.reading
                        binding.wordTextView.text = entry.kanji
                        binding.meaningTextView.text = entry.meaning
                        binding.pitchText.text = it.pitch
                        binding.freqChip.text = formatFrequency(it.frequency)

                        tags.forEach { tag -> configureChip(tag) }

                        if (tags.isEmpty()) { binding.chipLayout.visibility = View.GONE }
                        if (it.pitch == null) { binding.pitchCard.visibility = View.GONE }
                        if (it.frequency == null) { binding.freqChip.visibility = View.GONE }

                        binding.copyButton.setOnClickListener {
                            val clipboard = activity?.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip: ClipData = ClipData.newPlainText("${entry.kanji} definition", entry.meaning)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Copied definition to clipboard", Toast.LENGTH_SHORT).show()
                        }

                        binding.saveWordButton.setOnClickListener { _ ->
                            if (it.saved) {
                                viewModel.removeWord(entry.kanji)
                            } else {
                                viewModel.saveWord(entry.kanji)
                            }
                        }

                        if (it.saved) {
                            binding.saveWordButton.setImageResource(R.drawable.ic_baseline_bookmark_added_24)
                        } else {
                            binding.saveWordButton.setImageResource(R.drawable.ic_baseline_bookmark_border_24)
                        }
                    }
                }
            }
        }

        return binding.root
    }

    private fun configureChip(tag: Tag) {
        val chip = Chip(context)
        chip.text = tag.name
        chip.setOnClickListener { showTagAlert(tag.notes) }
        binding.chipLayout.addView(chip)
    }

    private fun showTagAlert(tagNotes: String) {
        val alertDialog = AlertDialog.Builder(activity)
            .setMessage(tagNotes)
            .setPositiveButton("OK") { dialog: DialogInterface, _: Int -> dialog.dismiss() }
            .create()
        alertDialog.show()
    }

    private fun formatFrequency(frequency: Long?): String? {
        return frequency?.let {
            val formatter = DecimalFormat("#,###")
            return "Freq: ${formatter.format(frequency)}"
        }
    }
}