package com.kamui.rin.fragment

import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.view.*
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.addCallback
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.kamui.rin.DictEntryAdapter
import com.kamui.rin.R
import com.kamui.rin.db.DBHelper
import com.kamui.rin.db.DictEntry
import com.kamui.rin.util.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.IOException

class LookupFragment : Fragment() {
    lateinit var helper: DBHelper
    lateinit var pbar: ProgressBar
    lateinit var recyclerView: RecyclerView
    lateinit var img: ImageView
    lateinit var support: TextView
    lateinit var notFoundView: TextView
    lateinit var adapter: DictEntryAdapter
    private lateinit var sharedPreferences: SharedPreferences
    var results: MutableList<DictEntry> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        adapter = DictEntryAdapter(requireContext(), results)
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
        helper = DBHelper(requireContext(), readDeinflectJsonFile(), Settings(sharedPreferences))
        val view = inflater.inflate(R.layout.fragment_lookup, container, false)
        pbar = view.findViewById(R.id.pBar)
        pbar.visibility = View.GONE
        support = view.findViewById(R.id.glassText)
        img = view.findViewById(R.id.glass)
        notFoundView = view.findViewById(R.id.noResultsFound)
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            if (img.visibility != View.VISIBLE) {
                results.clear()
                adapter.notifyDataSetChanged()
                val animation = AnimationUtils.loadAnimation(requireContext(), android.R.anim.fade_in)
                img.startAnimation(animation)
                support.startAnimation(animation)
                img.visibility = View.VISIBLE
                support.visibility = View.VISIBLE
            } else {
                requireActivity().moveTaskToBack(true)
            }
        }

        return view
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.top_app_bar, menu)
        val myActionMenuItem = menu.findItem(R.id.action_search)
        val searchView = myActionMenuItem.actionView as SearchView
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            @RequiresApi(api = Build.VERSION_CODES.O)
            override fun onQueryTextSubmit(query: String): Boolean {
                val vm = LookupViewModel()
                vm.lookup(query)
                if (!searchView.isIconified) {
                    searchView.isIconified = true
                }
                myActionMenuItem.collapseActionView()
                return false
            }

            override fun onQueryTextChange(s: String): Boolean {
                if (s.length == 1) {
                    results.clear()
                    adapter.notifyDataSetChanged()
                }
                img.visibility = View.INVISIBLE
                support.visibility = View.INVISIBLE
                notFoundView.text = ""
                return false
            }
        })
        val intent = requireActivity().intent
        val value = handleIntent(intent)
        if (value != null) {
            (requireActivity() as AppCompatActivity).supportActionBar!!.setDisplayHomeAsUpEnabled(true)
            (requireActivity() as AppCompatActivity).supportActionBar!!.setDisplayShowHomeEnabled(true)
            myActionMenuItem.expandActionView()
            searchView.setQuery(value, true)
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun handleIntent(intent: Intent): String? {
        return if (Intent.ACTION_PROCESS_TEXT == intent.action) {
            val keyword = intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)
            keyword.toString()
        } else {
            null
        }
    }

    private fun readDeinflectJsonFile(): String {
        var tContents = ""
        try {
            val stream = requireContext().assets.open("deinflect.json")
            val size = stream.available()
            val buffer = ByteArray(size)
            stream.read(buffer)
            stream.close()
            tContents = String(buffer)
        } catch (e: IOException) {
        }
        return tContents
    }

    inner class LookupViewModel : ViewModel() {
        @RequiresApi(Build.VERSION_CODES.O)
        fun lookup(vararg query: String) {
            pbar = requireActivity().findViewById(R.id.pBar)
            pbar.bringToFront()
            pbar.visibility = View.VISIBLE
            viewModelScope.launch(Dispatchers.IO) {
                results = helper.lookup(query[0]) as MutableList<DictEntry>
                requireActivity().runOnUiThread {
                    pbar.visibility = View.INVISIBLE
                    if (results.isEmpty()) {
                        notFoundView.text = getString(R.string.not_found)
                    }
                    recyclerView = requireActivity().findViewById(R.id.resultRecyclerView)
                    recyclerView.layoutManager = LinearLayoutManager(requireContext())
                    adapter = DictEntryAdapter(requireContext(), results)
                    recyclerView.adapter = adapter
                    adapter.notifyDataSetChanged()
                }
            }
        }
    }
}