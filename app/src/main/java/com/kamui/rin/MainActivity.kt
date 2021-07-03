package com.kamui.rin

import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.kamui.rin.database.DBHelper
import com.kamui.rin.database.DictEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.IOException

class MainActivity : AppCompatActivity() {
    lateinit var helper: DBHelper
    lateinit var adapter: DictEntryAdapter
    lateinit var pbar: ProgressBar
    lateinit var recyclerView: RecyclerView
    lateinit var results: MutableList<DictEntry>
    private lateinit var sharedPreferences: SharedPreferences
    lateinit var img: ImageView
    lateinit var support: TextView
    lateinit var notFoundView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        pbar = findViewById(R.id.pBar)
        pbar.visibility = View.GONE
        support = findViewById(R.id.glassText)
        img = findViewById(R.id.glass)
        notFoundView = findViewById(R.id.noResultsFound)
        val toolbar = findViewById<Toolbar>(R.id.searchToolbar)
        toolbar.bringToFront()
        setSupportActionBar(toolbar)
        helper =
            DBHelper(this, readDeinflectJsonFile(), getSettingsData())
        val btmNavView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        btmNavView.setOnNavigationItemSelectedListener { menuItem: MenuItem ->
            when (menuItem.itemId) {
                R.id.setting_page -> {
                    val intentSettings = Intent(this@MainActivity, SettingsActivity::class.java)
                    startActivity(intentSettings)
                    return@setOnNavigationItemSelectedListener true
                }
            }
            true
        }
        btmNavView.selectedItemId = R.id.search_page
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.top_app_bar, menu)
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
        val intent = intent
        val value = handleIntent(intent)
        if (value != null) {
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
            supportActionBar!!.setDisplayShowHomeEnabled(true)
            myActionMenuItem.expandActionView()
            searchView.setQuery(value, true)
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun getDisabledDicts(): List<String> {
            val dictMap = mapOf(
                "jmdictEnable" to "JMdict (English)",
                "kenkyuuEnable" to "研究社　新和英大辞典　第５版",
                "shinmeiEnable" to "新明解国語辞典 第五版",
                "daijirinEnable" to "三省堂　スーパー大辞林",
                "meikyoEnable" to "明鏡国語辞典"
            )
            val disabledDicts: MutableList<String> = ArrayList()
            for ((k, v) in dictMap) {
                val isEnabled = sharedPreferences.getBoolean(k, true)
                if (!isEnabled)
                    disabledDicts.add(v)
            }
            return disabledDicts
        }

    private fun getSettingsData(): SettingsData {
        return SettingsData(
            getDisabledDicts(),
            sharedPreferences.getBoolean("showBilingualFirst", false),
            sharedPreferences.getBoolean("shouldDeconj", true)
        )
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
            val stream = assets.open("deinflect.json")
            val size = stream.available()
            val buffer = ByteArray(size)
            stream.read(buffer)
            stream.close()
            tContents = String(buffer)
        } catch (e: IOException) {
            // Handle exceptions here
        }
        return tContents
    }

    inner class LookupViewModel : ViewModel() {
        @RequiresApi(Build.VERSION_CODES.O)
        fun lookup(vararg query: String) {
            pbar = findViewById(R.id.pBar)
            pbar.bringToFront()
            pbar.visibility = View.VISIBLE
            viewModelScope.launch(Dispatchers.IO) {
                results = helper.lookup(query[0]) as MutableList<DictEntry>
                runOnUiThread {
                    pbar.visibility = View.INVISIBLE
                    if (results.isEmpty()) {
                        notFoundView.text = getString(R.string.not_found)
                    }
                    recyclerView = findViewById(R.id.resultRecyclerView)
                    recyclerView.layoutManager = LinearLayoutManager(this@MainActivity)
                    adapter = DictEntryAdapter(this@MainActivity, results)
                    recyclerView.adapter = adapter
                }
            }
        }
    }

    override fun onBackPressed() {
        if (img.visibility != View.VISIBLE) {
            results.clear()
            adapter.notifyDataSetChanged()
            val animation = AnimationUtils.loadAnimation(this, android.R.anim.fade_in)
            img.startAnimation(animation)
            support.startAnimation(animation)
            img.visibility = View.VISIBLE
            support.visibility = View.VISIBLE
        } else {
            moveTaskToBack(true)
        }
    }
}