package com.kamui.rin.fragment

import android.app.AlertDialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.*
import androidx.navigation.fragment.navArgs
import com.google.android.material.chip.Chip
import com.kamui.rin.R
import com.kamui.rin.databinding.FragmentWordDetailBinding
import com.kamui.rin.db.AppDatabase
import com.kamui.rin.db.DictEntry
import com.kamui.rin.db.getTagsFromSplit
import com.kamui.rin.util.Tag
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.DecimalFormat

data class WordDetailState(
    val entry: DictEntry? = null
)

class WordDetailViewModel(database: AppDatabase, wordId: Int) : ViewModel() {
    private val _uiState = MutableStateFlow(WordDetailState())
    val uiState: StateFlow<WordDetailState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { currentState ->
                currentState.copy(
                    entry = database.dictDao().searchEntryById(wordId)
                )
            }
        }
    }
}

class WordDetailViewModelFactory(private val database: AppDatabase, private val wordId: Int): ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return WordDetailViewModel(database, wordId) as T
    }
}

class WordDetailFragment : Fragment() {
    private var _binding: FragmentWordDetailBinding? = null
    private val binding get() = _binding!!
    private val args: WordDetailFragmentArgs by navArgs()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

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
                    val (entry) = it
                    if (entry != null) {
                        // set title bar to word
                        (requireActivity() as AppCompatActivity).supportActionBar?.title = entry.kanji

                        // fill in UI elements
                        binding.secondaryTextCard.text = entry.reading
                        binding.wordTextView.text = entry.kanji
                        binding.meaningTextView.text = entry.meaning
                        binding.pitchText.text = entry.pitchAccent
                        binding.freqChip.text = formatFrequency(entry.freq)
                        getTagsFromSplit(entry.tags, binding.root.context).forEach { tag -> configureChip(tag) }

                        if (entry.pitchAccent == null) { binding.pitchCard.visibility = View.GONE }
                        if (entry.freq == null) { binding.freqChip.visibility = View.GONE }
                        if (entry.tags.isEmpty()) { binding.chipLayout.visibility = View.GONE }
                    }
                }
            }
        }

        return binding.root
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.detail_action_bar, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_close) {
            activity?.finishAffinity()
        } else {
            return super.onOptionsItemSelected(item)
        }
        return true
    }

    private fun configureChip(tag: Tag) {
        val chip = Chip(context)
        chip.text = tag.name
        chip.setOnClickListener { showTagAlert(tag) }
        binding.chipLayout.addView(chip)
    }

    private fun showTagAlert(tag: Tag) {
        val alertDialog = AlertDialog.Builder(activity)
            .setMessage(tag.description)
            .setPositiveButton("OK") { dialog: DialogInterface, _: Int -> dialog.dismiss() }
            .create()
        alertDialog.show()
    }

    private fun formatFrequency(frequency: Int?): String? {
        return frequency?.let {
            val formatter = DecimalFormat("#,###")
            return "Freq: ${formatter.format(frequency)}"
        }
    }
}