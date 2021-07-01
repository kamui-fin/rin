package com.kamui.rin

import android.content.DialogInterface
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.cardview.widget.CardView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup

class WordDetailActivity : AppCompatActivity() {
    @RequiresApi(api = Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_word_detail)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.setDisplayShowHomeEnabled(true)
        toolbar.setNavigationOnClickListener { onBackPressed() }
        val intent = intent
        val kanji = intent.getStringExtra("word")
        val reading = intent.getStringExtra("reading")
        val meaning = intent.getStringExtra("meaning")
        val freq = intent.getStringExtra("freq")
        val pitch = intent.getStringExtra("pitch")
        val tags = intent.getSerializableExtra("tags") as Array<Tag>
        val wordTextView = findViewById<TextView>(R.id.wordTextView)
        val meaningTextView = findViewById<TextView>(R.id.meaningTextView)
        val readingTextView = findViewById<TextView>(R.id.secondaryTextCard)
        val pitchTextView = findViewById<TextView>(R.id.pitchText)
        val freqChip = findViewById<TextView>(R.id.freqChip)
        val pitchCard = findViewById<CardView>(R.id.pitchCard)
        val chipView = findViewById<ChipGroup>(R.id.chipLayout)
        for (tag in tags) {
            val chip = Chip(this@WordDetailActivity)
            chip.text = tag.name
            chip.chipStartPadding = 30f
            chip.setOnClickListener {
                val alertDialog = AlertDialog.Builder(this@WordDetailActivity).create()
                alertDialog.setMessage(tag.description)
                alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK"
                ) { dialog: DialogInterface, _: Int -> dialog.dismiss() }
                alertDialog.show()
            }
            chipView.addView(chip)
        }
        toolbar.title = kanji
        readingTextView.text = reading
        wordTextView.text = kanji
        meaningTextView.text = meaning
        pitchTextView.text = pitch
        freqChip.text = freq
        if (pitch == null) {
            pitchCard.visibility = View.GONE
        }
        if (freq!!.isEmpty()) {
            freqChip.visibility = View.GONE
        }
        if (tags.isEmpty()) {
            chipView.visibility = View.GONE
        }
        val btmNavView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        btmNavView.selectedItemId = R.id.search_page
        val closeBtn = findViewById<ImageButton>(R.id.closeBtn)
        closeBtn.setOnClickListener { finishAffinity() }
        btmNavView.setOnNavigationItemSelectedListener { menuItem: MenuItem ->
            when (menuItem.itemId) {
                R.id.setting_page -> {
                    val intentSettings = Intent(this@WordDetailActivity, SettingsActivity::class.java)
                    startActivity(intentSettings)
                    return@setOnNavigationItemSelectedListener true
                }
                R.id.search_page -> {
                    val intentSearch = Intent(this@WordDetailActivity, MainActivity::class.java)
                    startActivity(intentSearch)
                    return@setOnNavigationItemSelectedListener true
                }
            }
            true
        }
    }
}