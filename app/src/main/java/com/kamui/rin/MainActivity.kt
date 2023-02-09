package com.kamui.rin

import android.content.res.Configuration
import android.os.Bundle
import android.view.MenuItem
import androidx.activity.addCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.preference.PreferenceManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.kamui.rin.fragment.LookupFragment
import com.kamui.rin.fragment.SettingsFragment
import com.kamui.rin.util.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NavigationState(
    val pageId: Int = R.id.search_page
)

class NavigationViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(NavigationState())
    val uiState: StateFlow<NavigationState> = _uiState.asStateFlow()

    fun setPage(page: Int) {
        _uiState.update { currentState ->
            currentState.copy(pageId = page)
        }
    }
}

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        if (Settings(PreferenceManager.getDefaultSharedPreferences(applicationContext)).darkTheme) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val viewModel: NavigationViewModel by viewModels()

        val toolbar = findViewById<Toolbar>(R.id.searchToolbar)
        toolbar.bringToFront()
        setSupportActionBar(toolbar)

        loadFragment(LookupFragment())
        val btmNavView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        btmNavView.setOnNavigationItemSelectedListener { menuItem: MenuItem ->
            when (menuItem.itemId) {
                R.id.search_page -> {
                    viewModel.setPage(R.id.search_page)
                    loadFragment(LookupFragment())
                    return@setOnNavigationItemSelectedListener true
                }
                R.id.setting_page -> {
                    viewModel.setPage(R.id.setting_page)
                    loadFragment(SettingsFragment())
                    return@setOnNavigationItemSelectedListener true
                }
            }
            true
        }
        onBackPressedDispatcher.addCallback(this) {
            finish()
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect {
                    // Update UI elements
                    btmNavView.selectedItemId = it.pageId
                }
            }
        }

    }

    private fun loadFragment(fragment: Fragment) {
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.container, fragment)
        transaction.addToBackStack(null)
        transaction.commit()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}