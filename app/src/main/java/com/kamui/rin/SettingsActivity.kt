package com.kamui.rin

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

data class SettingsData(
    val disabledDicts: List<String>,
    val bilingualFirst: Boolean,
    val shouldDeconj: Boolean,
)

class SettingsActivity : AppCompatActivity() {
    private lateinit var btmNavView: BottomNavigationView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        btmNavView = findViewById(R.id.bottom_navigation)
        btmNavView.selectedItemId = R.id.setting_page
        btmNavView.setOnNavigationItemSelectedListener { menuItem: MenuItem ->
            when (menuItem.itemId) {
                R.id.search_page -> {
                    val intentSearch = Intent(this@SettingsActivity, MainActivity::class.java)
                    startActivity(intentSearch)
                    return@setOnNavigationItemSelectedListener true
                }
            }
            true
        }
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.settings_container, SettingsFragment())
            .commit()
    }

    override fun onBackPressed() {
        startActivity(Intent(this, MainActivity::class.java))
    }
}