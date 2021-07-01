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
    var helper: DBHelper? = null
    var adapter: DictEntryAdapter? = null
    lateinit var pbar: ProgressBar
    lateinit var recyclerView: RecyclerView
    var results: MutableList<DictEntry>? = null
    private var sharedPreferences: SharedPreferences? = null
    var img: ImageView? = null
    var support: TextView? = null
    var notFoundView: TextView? = null

    @RequiresApi(api = Build.VERSION_CODES.N)
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
        helper = DBHelper(this, disabledDicts, shouldDeconj(), bilingualFirst(), readDeinflectJsonFile())
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
                if (adapter != null && s.length == 1) {
                    results!!.clear()
                    adapter!!.notifyDataSetChanged()
                }
                img!!.visibility = View.INVISIBLE
                support!!.visibility = View.INVISIBLE
                notFoundView!!.text = ""
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

    private val disabledDicts: List<String>
        get() {
            val jmdictEnabled = sharedPreferences!!.getBoolean("jmdictEnable", true)
            val kenkyuuEnable = sharedPreferences!!.getBoolean("kenkyuuEnable", true)
            val shinmeiEnable = sharedPreferences!!.getBoolean("shinmeiEnable", true)
            val daijirinEnable = sharedPreferences!!.getBoolean("daijirinEnable", true)
            val meikyoEnable = sharedPreferences!!.getBoolean("meikyoEnable", true)
            val disabledDicts: MutableList<String> = ArrayList()
            if (!jmdictEnabled) disabledDicts.add("JMdict (English)")
            if (!kenkyuuEnable) disabledDicts.add("研究社　新和英大辞典　第５版")
            if (!shinmeiEnable) disabledDicts.add("新明解国語辞典 第五版")
            if (!daijirinEnable) disabledDicts.add("三省堂　スーパー大辞林")
            if (!meikyoEnable) disabledDicts.add("明鏡国語辞典")
            return disabledDicts
        }

    private fun bilingualFirst(): Boolean {
        return sharedPreferences!!.getBoolean("showBilingualFirst", false)
    }

    private fun shouldDeconj(): Boolean {
        return sharedPreferences!!.getBoolean("deconjSettei", true)
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

    fun readDeinflectJsonFile(): String {
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
                results = helper?.lookup(query[0]) as MutableList<DictEntry>?
                runOnUiThread {
                    pbar.visibility = View.INVISIBLE
                    if (results!!.isEmpty()) {
                        notFoundView!!.text = getString(R.string.not_found)
                    }
                    recyclerView = findViewById(R.id.resultRecyclerView)
                    recyclerView.layoutManager = LinearLayoutManager(this@MainActivity)
                    adapter = DictEntryAdapter(this@MainActivity, results!!)
                    recyclerView.adapter = adapter
                }
            }
        }
    }

    override fun onBackPressed() {
        if (adapter != null && results != null && img!!.visibility != View.VISIBLE) {
            results!!.clear()
            adapter!!.notifyDataSetChanged()
            val animation = AnimationUtils.loadAnimation(this, android.R.anim.fade_in)
            img!!.startAnimation(animation)
            support!!.startAnimation(animation)
            img!!.visibility = View.VISIBLE
            support!!.visibility = View.VISIBLE
        } else {
            moveTaskToBack(true)
        }
    }
}