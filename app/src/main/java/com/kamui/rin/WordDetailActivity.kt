package com.kamui.rin

import android.content.DialogInterface
import android.os.Build
import android.os.Bundle
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
import com.kamui.rin.util.Tag

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

        val kanji = intent.getStringExtra("word")
        val reading = intent.getStringExtra("reading")
        val meaning = intent.getStringExtra("meaning")
        val freq = intent.getStringExtra("freq")
        val pitch = intent.getStringExtra("pitch")
        val tags = intent.getParcelableArrayListExtra<Tag>("tags")!!

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
                alertDialog.setButton(
                    AlertDialog.BUTTON_NEUTRAL, "OK"
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
        if (freq == null) {
            freqChip.visibility = View.GONE
        }
        if (tags.isEmpty()) {
            chipView.visibility = View.GONE
        }

        val closeBtn = findViewById<ImageButton>(R.id.closeBtn)
        closeBtn.setOnClickListener { finishAffinity() }
    }
}