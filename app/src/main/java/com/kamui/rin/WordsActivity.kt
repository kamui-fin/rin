package com.kamui.rin

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

class WordsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_words)
        val btmNavView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        btmNavView.setOnNavigationItemSelectedListener(BottomNavigationView.OnNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.search_page -> {
                    val intentSearch = Intent(this@WordsActivity, MainActivity::class.java)
                    startActivity(intentSearch)
                    return@OnNavigationItemSelectedListener true
                }
                R.id.setting_page -> {
                    val intentSettings = Intent(this@WordsActivity, SettingsActivity::class.java)
                    startActivity(intentSettings)
                    return@OnNavigationItemSelectedListener true
                }
            }
            true
        })
    }
}