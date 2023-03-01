package com.kamui.rin.ui.fragment

import android.content.Intent
import android.os.Bundle
import android.view.*
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.*
import androidx.navigation.fragment.navArgs
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.kamui.rin.adapter.DictEntryAdapter
import com.kamui.rin.R
import com.kamui.rin.databinding.FragmentLookupBinding
import com.kamui.rin.db.model.DictEntry
import com.kamui.rin.Settings
import com.kamui.rin.dict.Lookup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException

data class LookupState(
    val results: List<DictEntry> = listOf(),
    val currentlySearching: Boolean = false,
    val showStartPrompt: Boolean = true,
    val noResultsFound: Boolean = false,
    val lastQuery: String? = null
)

class LookupViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(LookupState())
    val uiState: StateFlow<LookupState> = _uiState.asStateFlow()

    fun lookupWord(query: String, helper: Lookup) {
        _uiState.update { state ->
            state.copy(
                currentlySearching = true,
                noResultsFound = false,
                showStartPrompt = false,
                lastQuery = query
            )
        }
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { state ->
                val dictEntries = helper.lookup(query)
                state.copy(
                    currentlySearching = false,
                    noResultsFound = dictEntries.isEmpty(),
                    results = dictEntries
                )
            }
        }
    }
}


class LookupFragment : Fragment() {
    val state: LookupViewModel by viewModels()
    private var _binding: FragmentLookupBinding? = null
    private val binding get() = _binding!!

    private val args: LookupFragmentArgs by navArgs()

    lateinit var helper: Lookup
    lateinit var adapter: DictEntryAdapter

    // for customizing search bar
    lateinit var actionBar: ActionBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                state.uiState.collect {
                    // new/changed entries
                    adapter = DictEntryAdapter(requireContext(), it.results)
                    binding.resultRecyclerView.layoutManager = LinearLayoutManager(requireContext())
                    binding.resultRecyclerView.adapter = adapter
                    adapter.notifyDataSetChanged()

                    if (it.lastQuery != null) actionBar.title = "Results for ${it.lastQuery}"

                    binding.homeGroup.visibility =
                        if (it.showStartPrompt) View.VISIBLE else View.GONE
                    binding.progressBar.visibility =
                        if (it.currentlySearching) View.VISIBLE else View.GONE
                    binding.noResultsFound.visibility =
                        if (it.noResultsFound) View.VISIBLE else View.GONE
                }
            }
        }

        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
        helper = Lookup(requireContext(), readDeinflectJsonFile(), Settings(sharedPreferences))
        _binding = FragmentLookupBinding.inflate(inflater, container, false)

        actionBar = (requireActivity() as AppCompatActivity).supportActionBar!!
        return binding.root
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.search_action_bar, menu)

        val searchAction = menu.findItem(R.id.action_search)
        val searchView = searchAction.actionView as SearchView

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                state.lookupWord(query, helper)
                return false
            }
            override fun onQueryTextChange(s: String): Boolean { return false }
        })

        // query intent
        handleIntent(requireActivity().intent)?.let {
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.setDisplayShowHomeEnabled(true)
            searchView.setQuery(it, true)
        }

        if (args.query != null) {
            searchView.setQuery(args.query, true)
        }
    }

    private fun handleIntent(intent: Intent): String? {
        return if (Intent.ACTION_PROCESS_TEXT == intent.action) {
            val keyword = intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)
            keyword.toString()
        } else {
            null
        }
    }

    private fun readDeinflectJsonFile(): String {
        return try {
            val stream = requireContext().assets.open("deinflect.json")
            val size = stream.available()
            val buffer = ByteArray(size)
            stream.read(buffer)
            stream.close()
            String(buffer)
        } catch (e: IOException) {
            ""
        }
    }
}