package com.kamui.rin

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.kamui.rin.fragment.LookupFragment
import com.kamui.rin.fragment.SettingsFragment

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val toolbar = findViewById<Toolbar>(R.id.searchToolbar)
        toolbar.bringToFront()
        setSupportActionBar(toolbar)
        loadFragment(LookupFragment())
        val btmNavView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        btmNavView.setOnNavigationItemSelectedListener { menuItem: MenuItem ->
            when (menuItem.itemId) {
                R.id.search_page -> {
                    loadFragment(LookupFragment())
                    return@setOnNavigationItemSelectedListener true
                }
                R.id.setting_page -> {
                    loadFragment(SettingsFragment())
                    return@setOnNavigationItemSelectedListener true
                }
            }
            true
        }
        btmNavView.selectedItemId = R.id.search_page
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