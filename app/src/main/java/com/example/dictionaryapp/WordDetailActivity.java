package com.example.dictionaryapp;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import org.json.JSONArray;
import org.json.JSONException;

public class WordDetailActivity extends AppCompatActivity {

    private JSONArray jsonTags;

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_word_detail);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        Intent intent = getIntent();
        String kanji = intent.getStringExtra("word");
        String reading = intent.getStringExtra("reading");
        String meaning = intent.getStringExtra("meaning");
        String freq = intent.getStringExtra("freq");
        String pitch = intent.getStringExtra("pitch");
        String tags = intent.getStringExtra("tags");
        jsonTags = new JSONArray();
        try {
            jsonTags = new JSONArray(tags);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        TextView wordTextView = findViewById(R.id.wordTextView);
        TextView meaningTextView = findViewById(R.id.meaningTextView);
        TextView readingTextView = findViewById(R.id.secondaryTextCard);
        TextView pitchTextView = findViewById(R.id.pitchText);
        TextView freqChip = findViewById(R.id.freqChip);

        CardView pitchCard = findViewById(R.id.pitchCard);
        ChipGroup chipView = findViewById(R.id.chipLayout);
        for (int x = 0; x < jsonTags.length(); x++) {
            try {
                JSONArray current = jsonTags.getJSONArray(x);
                Chip chip = new Chip(WordDetailActivity.this);
                chip.setText(current.getString(0));
                chip.setChipStartPadding(30);
                chip.setOnClickListener(v -> {
                    Chip c = (Chip) v;
                    for (int y = 0; y < jsonTags.length(); y++) {
                        try {
                            if (jsonTags.getJSONArray(y).getString(0) == c.getText()) {
                                AlertDialog alertDialog = new AlertDialog.Builder(WordDetailActivity.this).create();
                                alertDialog.setMessage(jsonTags.getJSONArray(y).getString(1));
                                alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                                        (dialog, which) -> dialog.dismiss());
                                alertDialog.show();
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                });
                chipView.addView(chip);
            } catch (JSONException e) {
                e.printStackTrace();
            }

        }

        toolbar.setTitle(kanji);
        readingTextView.setText(reading);
        wordTextView.setText(kanji);
        meaningTextView.setText(meaning);
        pitchTextView.setText(pitch);
        freqChip.setText(freq);

        if (pitch == null) {
            pitchCard.setVisibility(View.GONE);
        }

        assert freq != null;
        if (freq.isEmpty()) {
            freqChip.setVisibility(View.GONE);
        }

        if (jsonTags.length() == 0) {
            chipView.setVisibility(View.GONE);
        }

        BottomNavigationView btmNavView = findViewById(R.id.bottom_navigation);
        btmNavView.setSelectedItemId(R.id.search_page);

        ImageButton closeBtn = findViewById(R.id.closeBtn);
        closeBtn.setOnClickListener(v -> finishAffinity());
        btmNavView.setOnNavigationItemSelectedListener(menuItem -> {
            switch (menuItem.getItemId()) {
//                    case R.id.word_page:
//                        Intent intent_word = new Intent(WordDetailActivity.this, WordsActivity.class);
//                        startActivity(intent_word);
//                        return true;
                case R.id.setting_page:
                    Intent intent_settings = new Intent(WordDetailActivity.this, SettingsActivity.class);
                    startActivity(intent_settings);
                    return true;
                case R.id.search_page:
                    Intent intent_search = new Intent(WordDetailActivity.this, MainActivity.class);
                    startActivity(intent_search);
                    return true;
            }
            return true;
        });
    }



}
